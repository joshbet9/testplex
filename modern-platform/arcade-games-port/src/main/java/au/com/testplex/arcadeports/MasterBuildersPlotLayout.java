package au.com.testplex.arcadeports;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/** Finds the already-built clay plot floors in an imported Master Builders map. */
final class MasterBuildersPlotLayout {
    private static final int MIN_PLOT_BLOCKS = 12;
    private static final int MIN_EDGE = 4;
    private static final int MAX_EDGE = 64;

    private MasterBuildersPlotLayout() { }

    static List<Location> findExistingClayPlots(World world, SourceMapCatalog.MapData map, int maximumPlots) {
        if (world == null || map == null || !map.hasBounds() || maximumPlots < 1) return List.of();
        int minX = map.minX(), maxX = map.maxX(), minZ = map.minZ(), maxZ = map.maxZ();
        if (maxX < minX || maxZ < minZ) return List.of();
        long columns = (long) (maxX - minX + 1) * (maxZ - minZ + 1);
        if (columns < 1 || columns > 1_000_000L) return List.of();

        int centerY = map.spawnLocations().isEmpty() ? 64
                : map.spawnLocations().stream().mapToInt(Location::getBlockY).sorted()
                        .skip(map.spawnLocations().size() / 2L).findFirst().orElse(65) - 1;
        int lowY = Math.max(world.getMinHeight() + 1, centerY - 12);
        int highY = Math.min(world.getMaxHeight() - 3, centerY + 12);
        List<Plot> candidates = new ArrayList<>();
        for (int y = lowY; y <= highY; y++) {
            Set<Long> remaining = new HashSet<>();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block floor = world.getBlockAt(x, y, z);
                    if (isClay(floor.getType()) && floor.getRelative(0, 1, 0).isPassable()) {
                        remaining.add(pack(x, z));
                    }
                }
            }
            while (!remaining.isEmpty()) {
                long first = remaining.iterator().next();
                ArrayDeque<Long> queue = new ArrayDeque<>();
                queue.add(first);
                remaining.remove(first);
                int minComponentX = Integer.MAX_VALUE, maxComponentX = Integer.MIN_VALUE;
                int minComponentZ = Integer.MAX_VALUE, maxComponentZ = Integer.MIN_VALUE, count = 0;
                while (!queue.isEmpty()) {
                    long point = queue.removeFirst();
                    int x = x(point), z = z(point);
                    count++;
                    minComponentX = Math.min(minComponentX, x);
                    maxComponentX = Math.max(maxComponentX, x);
                    minComponentZ = Math.min(minComponentZ, z);
                    maxComponentZ = Math.max(maxComponentZ, z);
                    add(remaining, queue, x - 1, z);
                    add(remaining, queue, x + 1, z);
                    add(remaining, queue, x, z - 1);
                    add(remaining, queue, x, z + 1);
                }
                int width = maxComponentX - minComponentX + 1;
                int depth = maxComponentZ - minComponentZ + 1;
                if (count < MIN_PLOT_BLOCKS || width < MIN_EDGE || depth < MIN_EDGE
                        || width > MAX_EDGE || depth > MAX_EDGE
                        || Math.max(width, depth) > 2 * Math.min(width, depth)) continue;
                int centerX = (minComponentX + maxComponentX) / 2;
                int centerZ = (minComponentZ + maxComponentZ) / 2;
                Location spawn = safeCenter(world, y, centerX, centerZ,
                        minComponentX, maxComponentX, minComponentZ, maxComponentZ);
                if (spawn != null) candidates.add(new Plot(spawn, count));
            }
        }

        candidates.sort(Comparator.comparingInt(Plot::blocks).reversed()
                .thenComparingInt(plot -> plot.spawn().getBlockX())
                .thenComparingInt(plot -> plot.spawn().getBlockZ()));
        List<Location> result = new ArrayList<>();
        for (Plot plot : candidates) {
            boolean duplicate = result.stream().anyMatch(existing ->
                    Math.abs(existing.getBlockY() - plot.spawn().getBlockY()) <= 2
                            && Math.abs(existing.getBlockX() - plot.spawn().getBlockX()) <= 2
                            && Math.abs(existing.getBlockZ() - plot.spawn().getBlockZ()) <= 2);
            if (duplicate) continue;
            result.add(plot.spawn());
            if (result.size() == maximumPlots) break;
        }
        result.sort(Comparator.comparingInt(Location::getBlockX).thenComparingInt(Location::getBlockZ));
        return List.copyOf(result);
    }

    private static Location safeCenter(World world, int floorY, int centerX, int centerZ,
            int minX, int maxX, int minZ, int maxZ) {
        int maxRadius = Math.max(maxX - minX, maxZ - minZ);
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = Math.max(minX, centerX - radius); x <= Math.min(maxX, centerX + radius); x++) {
                for (int z = Math.max(minZ, centerZ - radius); z <= Math.min(maxZ, centerZ + radius); z++) {
                    if (Math.max(Math.abs(x - centerX), Math.abs(z - centerZ)) != radius) continue;
                    Block floor = world.getBlockAt(x, floorY, z);
                    if (floor.getType().isSolid() && floor.getRelative(0, 1, 0).isPassable()
                            && floor.getRelative(0, 2, 0).isPassable()) {
                        return new Location(world, x + 0.5D, floorY + 1.0D, z + 0.5D);
                    }
                }
            }
        }
        return null;
    }

    private static void add(Set<Long> remaining, ArrayDeque<Long> queue, int x, int z) {
        long key = pack(x, z);
        if (remaining.remove(key)) queue.addLast(key);
    }

    private static long pack(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }
    private static int x(long packed) { return (int) (packed >> 32); }
    private static int z(long packed) { return (int) packed; }

    private static boolean isClay(Material material) {
        return material == Material.CLAY || material == Material.TERRACOTTA
                || material.name().endsWith("_TERRACOTTA");
    }

    private record Plot(Location spawn, int blocks) { }
}
