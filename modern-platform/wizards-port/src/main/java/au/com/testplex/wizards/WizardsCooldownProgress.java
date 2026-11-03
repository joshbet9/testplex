package au.com.testplex.wizards;

import java.util.HashMap;
import java.util.Map;
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

/** Wizards spell recharge display matching the shared Arcade 1.8-style bar. */
final class WizardsCooldownProgress implements Listener {
    private final Map<UUID, Cooldown> active = new HashMap<>();
    @SuppressWarnings("unused")
    private final BukkitTask task;

    WizardsCooldownProgress(JavaPlugin plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 1L, 1L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void start(Player player, String name, long durationMillis) {
        if (player == null || !player.isOnline() || durationMillis <= 0L) return;
        active.put(player.getUniqueId(), new Cooldown(name, System.currentTimeMillis(), durationMillis));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        active.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
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
            int filled = (int) Math.floor(progress * 24);
            StringBuilder display = new StringBuilder(ChatColor.GOLD + ChatColor.BOLD.toString()
                    + cooldown.name + ChatColor.RESET + " ");
            for (int segment = 0; segment < 24; segment++) {
                display.append(segment < filled ? ChatColor.GREEN : ChatColor.RED).append('█');
            }
            display.append(ChatColor.RESET).append(' ').append(remaining).append('s');
            player.sendActionBar(display.toString());
            return false;
        });
    }

    private record Cooldown(String name, long startedAt, long durationMillis) { }
}
