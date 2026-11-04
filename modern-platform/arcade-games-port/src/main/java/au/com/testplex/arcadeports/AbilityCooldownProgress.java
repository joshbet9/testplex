package au.com.testplex.arcadeports;

import java.util.HashMap;
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
    private final Map<UUID, Cooldown> active = new HashMap<>();
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

    public void start(Player player, String name, long durationMillis) {
        if (player == null || !player.isOnline() || durationMillis <= 0L) return;
        active.put(player.getUniqueId(), new Cooldown(name, System.currentTimeMillis(), durationMillis));
        if (plugin instanceof ArcadeMechanicsModernPlugin arcadePlugin) {
            ArcadePortManager manager = arcadePlugin.getArcadePortManager();
            if (manager != null) manager.showAbilityActivationCosmetic(player);
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
        active.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            Cooldown cooldown = entry.getValue();
            if (player == null || !player.isOnline()) return true;
            long elapsed = Math.max(0L, now - cooldown.startedAt);
            double progress = Math.min(1.0D, (double) elapsed / cooldown.durationMillis);
            if (progress >= 1.0D) {
                player.sendActionBar(ChatColor.GREEN + ChatColor.BOLD.toString() + cooldown.name + " Recharged");
                return true;
            }
            long remaining = Math.max(1L, (cooldown.durationMillis - elapsed + 999L) / 1_000L);
            int filled = (int) Math.floor(progress * BAR_SEGMENTS);
            StringBuilder display = new StringBuilder(ChatColor.GOLD + ChatColor.BOLD.toString()
                    + cooldown.name + ChatColor.RESET + " ");
            for (int segment = 0; segment < BAR_SEGMENTS; segment++) {
                display.append(segment < filled ? ChatColor.GREEN : ChatColor.RED).append('\u258c');
            }
            display.append(ChatColor.RESET).append(' ').append(remaining).append('s');
            player.sendActionBar(display.toString());
            return false;
        });
    }

    private record Cooldown(String name, long startedAt, long durationMillis) { }
}
