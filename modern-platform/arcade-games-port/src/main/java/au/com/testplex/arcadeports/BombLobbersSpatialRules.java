package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

/** Legacy Bomb Lobbers team-boundary and TNT-hit spatial calculations. */
final class BombLobbersSpatialRules {
    private BombLobbersSpatialRules() { }

    static void cacheBombLobbersTeamSpawnCentres(SessionState session, SourceMapCatalog.MapData sourceMap) {
        session.bombTeamSpawnCentres.clear();

        // Reborn calculates each side from every configured team spawn, not just the
        // subset of spawn markers occupied by the current lobby. Keep that boundary stable
        // when the test server has a small party or only one player on either team.
        if (sourceMap != null) {
            boolean hasAllTeamMarkers = true;
            for (int team = 0; team < 2; team++) {
                List<Location> markers = sourceMap.teamSpawnLocationsForTeam(team, 2);
                if (markers.isEmpty()) {
                    hasAllTeamMarkers = false;
                    break;
                }
                double x = 0.0D, y = 0.0D, z = 0.0D;
                for (Location marker : markers) {
                    x += marker.getX();
                    y += marker.getY();
                    z += marker.getZ();
                }
                int count = markers.size();
                session.bombTeamSpawnCentres.put(team, new Location(session.world,
                        x / count, y / count, z / count));
            }
            if (hasAllTeamMarkers) return;
            session.bombTeamSpawnCentres.clear();
        }

        // Fallback for a malformed or incomplete source map: derive centres from occupied spawns.
        Map<Integer, double[]> totals = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        for (UUID id : session.players.keySet()) {
            int team = session.teams.getOrDefault(id, -1);
            Location spawn = session.lastLocations.get(id);
            if (team < 0 || spawn == null || spawn.getWorld() != session.world) continue;
            double[] total = totals.computeIfAbsent(team, ignored -> new double[3]);
            total[0] += spawn.getX();
            total[1] += spawn.getY();
            total[2] += spawn.getZ();
            counts.merge(team, 1, Integer::sum);
        }
        for (Map.Entry<Integer, double[]> entry : totals.entrySet()) {
            int count = counts.getOrDefault(entry.getKey(), 0);
            if (count <= 0) continue;
            double[] total = entry.getValue();
            session.bombTeamSpawnCentres.put(entry.getKey(), new Location(session.world,
                    total[0] / count, total[1] / count, total[2] / count));
        }
    }

    static int nearestBombLobbersSpawnTeam(SessionState session, Location location) {
        if (session == null || location == null || location.getWorld() != session.world
                || session.bombTeamSpawnCentres.isEmpty()) return -1;
        int nearestTeam = -1;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Map.Entry<Integer, Location> entry : session.bombTeamSpawnCentres.entrySet()) {
            double distanceSquared = location.distanceSquared(entry.getValue());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearestTeam = entry.getKey();
            }
        }
        return nearestTeam;
    }

    static double bombLobbersDistanceToBoundingBox(Location blast, Player target) {
        org.bukkit.util.BoundingBox box = target.getBoundingBox();
        double dx = blast.getX() < box.getMinX() ? box.getMinX() - blast.getX()
                : blast.getX() > box.getMaxX() ? blast.getX() - box.getMaxX() : 0.0D;
        double dy = blast.getY() < box.getMinY() ? box.getMinY() - blast.getY()
                : blast.getY() > box.getMaxY() ? blast.getY() - box.getMaxY() : 0.0D;
        double dz = blast.getZ() < box.getMinZ() ? box.getMinZ() - blast.getZ()
                : blast.getZ() > box.getMaxZ() ? blast.getZ() - box.getMaxZ() : 0.0D;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static double bombLobbersBlastExposure(Location blast, Player target) {
        if (blast == null || target == null || blast.getWorld() == null
                || target.getWorld() != blast.getWorld()) return 0.0D;
        org.bukkit.util.BoundingBox box = target.getBoundingBox();
        double xStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxX() - box.getMinX()));
        double yStep = ArcadePortRules.bombLobbersExposureSampleStep(box.getHeight());
        double zStep = ArcadePortRules.bombLobbersExposureSampleStep((box.getMaxZ() - box.getMinZ()));
        if (!Double.isFinite(xStep) || !Double.isFinite(yStep) || !Double.isFinite(zStep)) return 0.0D;
        double xOffset = ArcadePortRules.bombLobbersExposureSampleOffset(xStep);
        double zOffset = ArcadePortRules.bombLobbersExposureSampleOffset(zStep);
        int visible = 0;
        int total = 0;
        for (float fx = 0.0F; fx <= 1.0F; fx = (float) (fx + xStep)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float) (fy + yStep)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float) (fz + zStep)) {
                    Location sample = new Location(blast.getWorld(),
                            box.getMinX() + (box.getMaxX() - box.getMinX()) * fx + xOffset,
                            box.getMinY() + box.getHeight() * fy,
                            box.getMinZ() + (box.getMaxZ() - box.getMinZ()) * fz + zOffset);
                    Vector ray = sample.toVector().subtract(blast.toVector());
                    double distance = ray.length();
                    total++;
                    if (distance <= 1.0E-6D
                            || blast.getWorld().rayTraceBlocks(blast, ray.normalize(), distance,
                                    org.bukkit.FluidCollisionMode.NEVER, true) == null) visible++;
                }
            }
        }
        return total == 0 ? 0.0D : visible / (double) total;
    }

    static int bombLobbersOnlineMatchPlayers(SessionState session) {
        if (session == null) return 0;
        return (int) session.players.values().stream()
                .filter(player -> player != null && player.isOnline() && player.getWorld() == session.world)
                .count();
    }

    static boolean isBombTouchingAnyBlock(TNTPrimed bomb) {
        Block center = bomb.getLocation().getBlock();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Block nearby = center.getRelative(dx, dy, dz);
                    // Reborn primes on any non-air neighbour, including liquids and passable blocks.
                    if (nearby.getType() != Material.AIR) return true;
                }
            }
        }
        return false;
    }

}
