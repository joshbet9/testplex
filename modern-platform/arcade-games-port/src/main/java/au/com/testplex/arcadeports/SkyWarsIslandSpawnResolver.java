package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Resolves missing SkyWars island starts only when real, separate island terrain exists. */
final class SkyWarsIslandSpawnResolver {
    private static final double MIN_ISLAND_SEPARATION_SQUARED = 18.0D * 18.0D;
    private static final int MAX_SEARCH_RADIUS = 12;

    private SkyWarsIslandSpawnResolver() { }

    static List<Location> resolve(World world, SourceMapCatalog.MapData map, int required) {
        if (world == null || map == null || required < 1) return List.of();
        List<Location> starts = map.spawnLocations();
        if (starts.isEmpty()) starts = map.dataLocations("GREEN");
        List<Location> resolved = new ArrayList<>();
        for (Location start : starts) {
            if (start != null) resolved.add(new Location(world, start.getX(), start.getY(), start.getZ(),
                    start.getYaw(), start.getPitch()));
        }
        if (resolved.size() >= required || !map.hasBounds()) return List.copyOf(resolved);

        Location center = new Location(world, map.centerX(), 0.0D, map.centerZ());
        List<Location> targets = additionalIslandTargets(resolved, center, required - resolved.size());
        for (Location target : targets) {
            Location safe = nearestSafeIslandSurface(world, target, resolved);
            if (safe == null) safe = createSupplementalIsland(world, map, target, resolved);
            if (safe != null) resolved.add(safe);
            if (resolved.size() >= required) break;
        }
        return List.copyOf(resolved);
    }

    /** Add a compact, lootable island only when the source map has no terrain in a required gap. */
    private static Location createSupplementalIsland(World world, SourceMapCatalog.MapData map,
            Location target, List<Location> existing) {
        int centerX = target.getBlockX();
        int centerZ = target.getBlockZ();
        int topY = nearestStartY(existing, centerX, centerZ) - 1;
        if (topY <= world.getMinHeight() || topY >= world.getMaxHeight() - 3) return null;
        if (!map.hasBounds() || centerX - 4 < map.minX() || centerX + 4 > map.maxX()
                || centerZ - 4 < map.minZ() || centerZ + 4 > map.maxZ()) return null;

        Location spawn = new Location(world, centerX + 0.5D, topY + 1.0D, centerZ + 0.5D);
        if (existing.stream().anyMatch(start -> start != null
                && horizontalDistanceSquared(start, spawn.getX(), spawn.getZ())
                < MIN_ISLAND_SEPARATION_SQUARED)) return null;

        world.getChunkAt(centerX >> 4, centerZ >> 4).load();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                world.getBlockAt(centerX + dx, topY, centerZ + dz).setType(Material.GRASS_BLOCK, false);
                world.getBlockAt(centerX + dx, topY - 1, centerZ + dz).setType(Material.DIRT, false);
            }
        }
        for (int layer = 1; layer <= 4; layer++) {
            int radius = 4 - layer;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    world.getBlockAt(centerX + dx, topY - 1 - layer, centerZ + dz)
                            .setType(Material.STONE, false);
                }
            }
        }

        org.bukkit.block.Block chestBlock = world.getBlockAt(centerX + 3, topY + 1, centerZ);
        chestBlock.setType(Material.CHEST, false);
        if (chestBlock.getState() instanceof org.bukkit.block.Chest chest) {
            SkyWarsChestLoot.populateSupplementalIslandChest(chest);
        }
        return spawn;
    }

    /** Place inferred targets in the widest gaps between authored islands. */
    static List<Location> additionalIslandTargets(List<Location> starts, Location center, int count) {
        if (starts == null || starts.size() < 2 || center == null || count <= 0) return List.of();
        List<AngularStart> angular = new ArrayList<>();
        for (Location start : starts) {
            if (start == null || start.getWorld() != center.getWorld()) continue;
            double dx = start.getX() - center.getX();
            double dz = start.getZ() - center.getZ();
            double angle = Math.atan2(dz, dx);
            if (angle < 0.0D) angle += Math.PI * 2.0D;
            angular.add(new AngularStart(angle, Math.hypot(dx, dz)));
        }
        if (angular.size() < 2) return List.of();
        angular.sort(Comparator.comparingDouble(AngularStart::angle));
        List<AngularGap> gaps = new ArrayList<>();
        for (int i = 0; i < angular.size(); i++) {
            AngularStart left = angular.get(i);
            AngularStart right = angular.get((i + 1) % angular.size());
            double rightAngle = right.angle() + (i + 1 == angular.size() ? Math.PI * 2.0D : 0.0D);
            gaps.add(new AngularGap(rightAngle - left.angle(), left.angle(), rightAngle,
                    (left.radius() + right.radius()) / 2.0D));
        }
        gaps.sort(Comparator.comparingDouble(AngularGap::width).reversed());

        List<Location> targets = new ArrayList<>();
        for (AngularGap gap : gaps) {
            if (targets.size() >= count) break;
            double angle = (gap.leftAngle() + gap.rightAngle()) / 2.0D;
            double x = center.getX() + Math.cos(angle) * gap.radius();
            double z = center.getZ() + Math.sin(angle) * gap.radius();
            Location target = new Location(center.getWorld(), x, 0.0D, z);
            if (starts.stream().anyMatch(start -> start != null
                    && horizontalDistanceSquared(start, target.getX(), target.getZ())
                    < MIN_ISLAND_SEPARATION_SQUARED)) continue;
            targets.add(target);
        }
        return List.copyOf(targets);
    }

    private static Location nearestSafeIslandSurface(World world, Location target, List<Location> existing) {
        int targetX = target.getBlockX();
        int targetZ = target.getBlockZ();
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS; radius++) {
            List<Location> candidates = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = targetX + dx;
                    int z = targetZ + dz;
                    if (x < target.getBlockX() - MAX_SEARCH_RADIUS || x > target.getBlockX() + MAX_SEARCH_RADIUS
                            || z < target.getBlockZ() - MAX_SEARCH_RADIUS || z > target.getBlockZ() + MAX_SEARCH_RADIUS) continue;
                    world.getChunkAt(x >> 4, z >> 4).load();
                    int y = world.getHighestBlockYAt(x, z);
                    if (y < world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) continue;
                    Block floor = world.getBlockAt(x, y, z);
                    if (!isSafeFloor(floor.getType()) || !world.getBlockAt(x, y + 1, z).isPassable()
                            || !world.getBlockAt(x, y + 2, z).isPassable()) continue;
                    if (Math.abs(y - nearestStartY(existing, x, z)) > 24) continue;
                    if (supportCount(world, x, y, z) < 6) continue;
                    Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
                    if (existing.stream().anyMatch(start -> start != null
                            && start.distanceSquared(candidate) < MIN_ISLAND_SEPARATION_SQUARED)) continue;
                    candidates.add(candidate);
                }
            }
            if (!candidates.isEmpty()) return candidates.stream()
                    .min(Comparator.comparingDouble(candidate -> candidate.distanceSquared(target))).orElse(null);
        }
        return null;
    }

    private static int nearestStartY(List<Location> starts, int x, int z) {
        return starts.stream().filter(start -> start != null)
                .min(Comparator.comparingDouble(start -> horizontalDistanceSquared(start, x, z)))
                .map(Location::getBlockY).orElse(0);
    }

    private static double horizontalDistanceSquared(Location location, int x, int z) {
        return horizontalDistanceSquared(location, (double) x, (double) z);
    }

    private static double horizontalDistanceSquared(Location location, double x, double z) {
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static boolean isSafeFloor(Material material) {
        return material.isSolid() && material != Material.MAGMA_BLOCK && material != Material.CACTUS
                && material != Material.CAMPFIRE && material != Material.SOUL_CAMPFIRE
                && material != Material.FIRE && material != Material.SOUL_FIRE
                && material != Material.POWDER_SNOW;
    }

    private static int supportCount(World world, int x, int y, int z) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (isSafeFloor(world.getBlockAt(x + dx, y, z + dz).getType())) count++;
            }
        }
        return count;
    }

    private record AngularStart(double angle, double radius) { }
    private record AngularGap(double width, double leftAngle, double rightAngle, double radius) { }
}
