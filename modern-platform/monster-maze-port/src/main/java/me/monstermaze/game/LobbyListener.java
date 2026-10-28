package me.monstermaze.game;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.world.VoidWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Keeps Monster Maze's world/lobby flow inside the unified modern Arcade platform. */
public final class LobbyListener implements Listener {
    private final MonsterMazePlugin plugin;
    private final GameManager game;

    public LobbyListener(MonsterMazePlugin plugin, GameManager game, VoidWorldManager voids) {
        this.plugin = plugin;
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (plugin.isSoloMode()) new SoloIntegrityListener(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (plugin.isSoloMode() && player.isOp()) player.setOp(false);
        // Do not pull all new joins into a private selector world. The common Game Panel
        // starts the game; late joiners are moved into spectator mode only during a live round.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !game.isLive()) return;
            Location center = game.getCenter();
            if (center == null) return;
            player.teleport(center.clone().add(0, 15, 0));
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (game.isLive()) return;
        Location center = game.getCenter();
        if (center == null || event.getPlayer().getWorld() != center.getWorld()) return;
        Location lobby = game.getLobbySpawn();
        if (lobby != null) event.setRespawnLocation(lobby);
    }
}
