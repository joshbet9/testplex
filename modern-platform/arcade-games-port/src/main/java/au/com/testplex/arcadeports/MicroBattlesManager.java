package au.com.testplex.arcadeports;

import java.util.List;
import java.util.function.Predicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

/** Micro Battles' per-player world-border cue, invoked from the existing match tick. */
final class MicroBattlesManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.MICRO_BATTLES; }

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
