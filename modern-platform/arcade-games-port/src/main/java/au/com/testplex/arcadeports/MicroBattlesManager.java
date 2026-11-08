package au.com.testplex.arcadeports;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/** Micro Battles' per-player world-border cue, invoked from the existing match tick. */
final class MicroBattlesManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.MICRO_BATTLES; }

    /** A world-shared overhead warning for players close to the shrinking border. */
    void updateBorderWarnings(SessionState session, List<Player> alive, Predicate<Player> showForPlayer) {
        WorldBorder border = session.world.getWorldBorder();
        Location center = border.getCenter();
        Set<UUID> showing = new HashSet<>();
        for (Player player : alive) {
            if (!showForPlayer.test(player)) {
                removeBorderWarning(session, player.getUniqueId());
                continue;
            }
            Location location = player.getLocation();
            boolean nearBorder = ArcadePortRules.shouldShowMicroBattlesBorderParticles(
                    location.getX(), location.getZ(), center.getX(), center.getZ(), border.getSize(),
                    GameDebugRuntimeOverrides.doubleValue("MICRO_BATTLES_BORDER_PARTICLE_DISTANCE",
                            ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_DISTANCE));
            if (!nearBorder) continue;

            UUID playerId = player.getUniqueId();
            showing.add(playerId);
            TextDisplay warning = session.microBorderWarnings.get(playerId);
            Location aboveHead = location.clone().add(0.0D, 2.35D, 0.0D);
            if (warning == null || !warning.isValid()) {
                warning = session.world.spawn(aboveHead, TextDisplay.class);
                warning.setText(ChatColor.GOLD + "" + ChatColor.BOLD + "⚠ BORDER");
                warning.setBillboard(Display.Billboard.CENTER);
                warning.setAlignment(TextDisplay.TextAlignment.CENTER);
                warning.setSeeThrough(true);
                warning.setShadowed(true);
                warning.setDefaultBackground(false);
                warning.setLineWidth(120);
                warning.setPersistent(false);
                warning.setGravity(false);
                warning.setInvulnerable(true);
                warning.setSilent(true);
                warning.setViewRange(1.2F);
                session.microBorderWarnings.put(playerId, warning);
            } else {
                // Follow every game tick so the label stays directly above a moving player.
                warning.teleport(aboveHead);
            }
        }

        for (UUID playerId : new HashSet<>(session.microBorderWarnings.keySet())) {
            if (!showing.contains(playerId)) removeBorderWarning(session, playerId);
        }
    }

    void removeBorderWarning(SessionState session, UUID playerId) {
        if (session == null || playerId == null) return;
        TextDisplay warning = session.microBorderWarnings.remove(playerId);
        if (warning != null && warning.isValid()) warning.remove();
    }

    void removeAllBorderWarnings(SessionState session) {
        if (session == null) return;
        for (UUID playerId : new HashSet<>(session.microBorderWarnings.keySet())) {
            removeBorderWarning(session, playerId);
        }
    }

    void updateBorderParticles(SessionState session, List<Player> alive, Predicate<Player> showForPlayer) {
        if (session == null || session.type != ArcadePortGame.MICRO_BATTLES
                || session.world == null || alive == null || showForPlayer == null) return;
        WorldBorder border = session.world.getWorldBorder();
        Location center = border.getCenter();
        double half = border.getSize() / 2.0D;
        for (Player player : alive) {
            if (!showForPlayer.test(player)) continue;
            Location location = player.getLocation();
            double x = location.getX(), z = location.getZ();
            if (!ArcadePortRules.shouldShowMicroBattlesBorderParticles(
                    x, z, center.getX(), center.getZ(), border.getSize(),
                    GameDebugRuntimeOverrides.doubleValue("MICRO_BATTLES_BORDER_PARTICLE_DISTANCE",
                            ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_DISTANCE))) continue;
            double xFaceDistance = half - Math.abs(x - center.getX());
            double zFaceDistance = half - Math.abs(z - center.getZ());
            boolean xFace = xFaceDistance <= zFaceDistance;
            double edgeX = center.getX() + (x >= center.getX() ? half - 0.06D : -half + 0.06D);
            double edgeZ = center.getZ() + (z >= center.getZ() ? half - 0.06D : -half + 0.06D);
            double along = xFace ? Math.max(center.getZ() - half + 0.08D,
                    Math.min(center.getZ() + half - 0.08D, z))
                    : Math.max(center.getX() - half + 0.08D, Math.min(center.getX() + half - 0.08D, x));
            Color colour = ArcadePortRules.microBattlesParticleColour(
                    session.microBorderColours.getOrDefault(player.getUniqueId(),
                            ArcadePortRules.MICRO_BATTLES_DEFAULT_COSMETIC_COLOUR));
            Particle.DustOptions dust = new Particle.DustOptions(colour, 1.2F);
            // Three points make the border obvious across the player's full height, not just on the floor.
            int verticalPoints = GameDebugRuntimeOverrides.intValue("MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS",
                    ArcadePortRules.MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS);
            for (int point = 0; point < verticalPoints; point++) {
                double height = verticalPoints == 1 ? 0.95D : 0.15D + 1.6D * point / (verticalPoints - 1);
                Location cue = xFace
                        ? new Location(session.world, edgeX, location.getY() + height, along)
                        : new Location(session.world, along, location.getY() + height, edgeZ);
                session.world.spawnParticle(Particle.DUST, cue, 2, 0.015D, 0.025D, 0.015D, 0.0D, dust);
            }
        }
    }
}
