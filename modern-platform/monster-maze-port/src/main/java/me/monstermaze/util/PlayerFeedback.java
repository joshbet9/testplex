package me.monstermaze.util;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.event.MonsterBumpPlayerEvent;
import me.monstermaze.game.GameManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Short-lived modern-only floating health feedback above players in Monster Maze. */
public final class PlayerFeedback implements Listener {
    private final MonsterMazePlugin plugin;
    private final GameManager game;
    private final Set<TextDisplay> activeDisplays = new HashSet<TextDisplay>();
    /** Keep per-viewer visibility state to avoid re-sending show/hide packets every tick. */
    private final Map<TextDisplay, Set<UUID>> visibleTo = new HashMap<TextDisplay, Set<UUID>>();

    public PlayerFeedback(MonsterMazePlugin plugin, GameManager game) {
        this.plugin = plugin;
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, plugin.getHostPlugin());
    }

    @EventHandler
    public void onMonsterBump(MonsterBumpPlayerEvent event) {
        if (!game.isLive()) return;
        show(event.getPlayer(), "-2 ♥", NamedTextColor.RED);
    }

    /** First arrival receives two hearts; every later Safe Pad arrival receives one. */
    public void showPadReward(Player player, boolean first) {
        show(player, first ? "+2 ♥" : "+1 ♥", NamedTextColor.GREEN);
    }

    private void show(final Player player, String value, NamedTextColor colour) {
        if (player == null || !player.isOnline() || !game.isLive()) return;

        Location origin = player.getLocation().clone().add(0.0D, 2.25D, 0.0D);
        final TextDisplay display = player.getWorld().spawn(origin, TextDisplay.class);
        display.text(Component.text(value, colour).decorate(TextDecoration.BOLD));
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setDefaultBackground(false);
        display.setShadowed(true);
        // Ordinary text depth/shadow gives sharper glyphs than see-through holograms.
        display.setSeeThrough(false);
        display.setLineWidth(100);
        display.setViewRange(2.0f);
        display.setTeleportDuration(2);
        display.setPersistent(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setGravity(false);
        display.setVisibleByDefault(false);
        activeDisplays.add(display);

        showToAllowedViewers(player, display);

        new BukkitRunnable() {
            private int age;

            @Override
            public void run() {
                if (!game.isLive() || !player.isOnline() || !display.isValid() || age >= 24) {
                    display.remove();
                    activeDisplays.remove(display);
                    visibleTo.remove(display);
                    cancel();
                    return;
                }

                showToAllowedViewers(player, display);
                Location next = player.getLocation().clone().add(0.0D, 2.25D + age * 0.025D, 0.0D);
                display.teleport(next);
                // Interpolate two-tick movement and avoid 24 opacity metadata packets.
                // The brief popup disappears cleanly when its lifetime ends.
                age += 2;
            }
        }.runTaskTimer(plugin.getHostPlugin(), 1L, 2L);
    }

    private void showToAllowedViewers(Player subject, TextDisplay display) {
        Set<UUID> shown = visibleTo.computeIfAbsent(display, ignored -> new HashSet<UUID>());
        Set<UUID> online = new HashSet<UUID>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID id = viewer.getUniqueId();
            online.add(id);
            boolean allowed = viewer.getWorld() == subject.getWorld()
                    && !game.getKitManager().isHiddenFrom(viewer, subject);
            if (!shouldUpdateVisibility(allowed, shown.contains(id))) continue;
            if (allowed) {
                viewer.showEntity(plugin.getHostPlugin(), display);
                shown.add(id);
            } else {
                viewer.hideEntity(plugin.getHostPlugin(), display);
                shown.remove(id);
            }
        }
        shown.retainAll(online);
    }

    /** Only emit a visibility packet when the desired state actually changes. */
    static boolean shouldUpdateVisibility(boolean shouldShow, boolean currentlyShown) {
        return shouldShow != currentlyShown;
    }

    /** Clear any outstanding displays when the round is stopped. */
    public void clear() {
        for (TextDisplay display : new HashSet<TextDisplay>(activeDisplays)) {
            if (display != null && display.isValid()) display.remove();
        }
        activeDisplays.clear();
        visibleTo.clear();
    }
}
