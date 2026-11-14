package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Shared 1.8-style above-hotbar progress display for timed Arcade abilities. */
public final class AbilityCooldownProgress implements Listener {
    private static final Map<JavaPlugin, AbilityCooldownProgress> INSTANCES = new WeakHashMap<>();
    private static final int BAR_SEGMENTS = 24;

    private final JavaPlugin plugin;
    private final Map<UUID, Map<String, Cooldown>> active = new HashMap<>();
    @SuppressWarnings("unused")
    private final BukkitTask task;

    public static synchronized AbilityCooldownProgress forPlugin(JavaPlugin plugin) {
        return INSTANCES.computeIfAbsent(plugin, AbilityCooldownProgress::new);
    }

    private AbilityCooldownProgress(JavaPlugin plugin) {
        this.plugin = plugin;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Returns whether an ability cooldown should currently own the player's action bar. */
    public boolean isActive(Player player) {
        if (player == null) return false;
        Map<String, Cooldown> cooldowns = active.get(player.getUniqueId());
        if (cooldowns == null) return false;
        long now = System.currentTimeMillis();
        for (Cooldown cooldown : cooldowns.values()) {
            if (now < cooldown.startedAt + cooldown.durationMillis) return true;
        }
        return false;
    }

    public void start(Player player, String name, long durationMillis) {
        start(player, name, durationMillis, null);
    }

    public void start(Player player, String name, long durationMillis, ArcadePortGame cosmeticGame) {
        if (player == null || !player.isOnline() || durationMillis <= 0L) return;
        String key = (cosmeticGame == null ? "" : cosmeticGame.id() + ":") + name;
        active.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                .put(key, new Cooldown(key, name, System.currentTimeMillis(), durationMillis));
        if (plugin instanceof ArcadeMechanicsModernPlugin arcadePlugin) {
            ArcadePortManager manager = arcadePlugin.getArcadePortManager();
            if (manager != null) {
                if (cosmeticGame == null) manager.showAbilityActivationCosmetic(player, name);
                else manager.showAbilityActivationCosmetic(player, cosmeticGame, name);
            }
        }
    }

    public void stop(Player player) {
        if (player != null) active.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        stop(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
    }

    private void update() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Map<String, Cooldown>>> iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Map<String, Cooldown>> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            Map<String, Cooldown> cooldowns = entry.getValue();
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }
            Cooldown cooldown = null;
            Cooldown lastCompleted = null;
            Iterator<Cooldown> cooldownIterator = cooldowns.values().iterator();
            while (cooldownIterator.hasNext()) {
                Cooldown candidate = cooldownIterator.next();
                long readyAt = candidate.startedAt + candidate.durationMillis;
                if (now >= readyAt) {
                    cooldownIterator.remove();
                    if (lastCompleted == null
                            || readyAt > lastCompleted.startedAt + lastCompleted.durationMillis) {
                        lastCompleted = candidate;
                    }
                } else if (cooldown == null
                        || readyAt < cooldown.startedAt + cooldown.durationMillis) {
                    cooldown = candidate;
                }
            }
            if (cooldown == null) {
                iterator.remove();
                if (lastCompleted != null) {
                    player.sendActionBar(ChatColor.GREEN + ChatColor.BOLD.toString()
                            + lastCompleted.name + " Recharged");
                }
                continue;
            }
            long elapsed = Math.max(0L, now - cooldown.startedAt);
            double progress = Math.min(1.0D, (double) elapsed / cooldown.durationMillis);
            long remainingMillis = Math.max(1L, cooldown.durationMillis - elapsed);
            long remainingTenths = (remainingMillis + 99L) / 100L;
            int filled = (int) Math.floor(progress * BAR_SEGMENTS);
            StringBuilder display = new StringBuilder(ChatColor.GOLD + ChatColor.BOLD.toString()
                    + cooldown.name + ChatColor.RESET + " ");
            for (int segment = 0; segment < BAR_SEGMENTS; segment++) {
                display.append(segment < filled ? ChatColor.GREEN : ChatColor.RED).append('\u258c');
            }
            display.append(ChatColor.RESET).append(' ')
                    .append(remainingTenths / 10L).append('.')
                    .append(remainingTenths % 10L).append(" seconds");
            player.sendActionBar(display.toString());
        }
    }

    static Cooldown nextCooldown(Iterable<Cooldown> cooldowns, long now) {
        if (cooldowns == null) return null;
        Cooldown next = null;
        for (Cooldown candidate : cooldowns) {
            long readyAt = candidate.startedAt + candidate.durationMillis;
            if (now < readyAt && (next == null || readyAt < next.startedAt + next.durationMillis)) {
                next = candidate;
            }
        }
        return next;
    }

    record Cooldown(String key, String name, long startedAt, long durationMillis) { }
}
