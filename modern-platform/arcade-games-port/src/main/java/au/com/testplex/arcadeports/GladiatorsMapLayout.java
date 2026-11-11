package au.com.testplex.arcadeports;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Layout helpers for authored and inferred Gladiators arena spawn markers. */
final class GladiatorsMapLayout {
    private static final List<String> TIERS = List.of("GREEN", "YELLOW", "ORANGE", "RED");
    private static final int[] ARENAS_PER_TIER = {8, 4, 2, 1};

    private GladiatorsMapLayout() {}

    /**
     * Pair terrain-checked surface locations into the 8/4/2/1 bracket arena tiers.
     * Returned markers are explicitly inferred test metadata when the source archive
     * does not contain its original WorldConfig.dat.
     */
    static Map<String, List<Location>> inferArenaMarkers(
            List<Location> safeSurfaceStarts, double mapCenterX, double mapCenterZ) {
        if (safeSurfaceStarts == null || safeSurfaceStarts.size() < 30) return Map.of();
        List<Location> remaining = new ArrayList<>();
        safeSurfaceStarts.stream().filter(location -> location != null)
                .map(Location::clone).forEach(remaining::add);
        List<ArenaSpawns> arenas = new ArrayList<>(15);
        while (arenas.size() < 15) {
            PairChoice choice = nearestSeparatedPair(remaining, arenas, 18.0D);
            if (choice == null) return Map.of();
            Location second = remaining.remove(choice.secondIndex());
            Location first = remaining.remove(choice.firstIndex());
            Location center = first.clone().add(second.toVector().subtract(first.toVector()).multiply(0.5D));
            arenas.add(new ArenaSpawns(center, first, second));
        }

        arenas.sort(Comparator.comparingDouble(arena -> -horizontalDistanceSquared(
                arena.center(), mapCenterX, mapCenterZ)));
        Map<String, List<Location>> markers = new LinkedHashMap<>();
        List<List<ArenaSpawns>> tierArenas = new ArrayList<>();
        int next = 0;
        for (int tier = 0; tier < TIERS.size(); tier++) {
            List<Location> centers = new ArrayList<>();
            List<ArenaSpawns> currentTier = new ArrayList<>();
            for (int i = 0; i < ARENAS_PER_TIER[tier]; i++) {
                ArenaSpawns arena = arenas.get(next++);
                currentTier.add(arena);
                centers.add(arena.center().clone());
                markers.computeIfAbsent("BLACK", ignored -> new ArrayList<>()).add(arena.first().clone());
                markers.get("BLACK").add(arena.second().clone());
            }
            markers.put(TIERS.get(tier), centers);
            tierArenas.add(currentTier);
        }
        List<Location> guidePoints = new ArrayList<>();
        for (int tier = 0; tier < tierArenas.size() - 1; tier++) {
            List<ArenaSpawns> nextTier = tierArenas.get(tier + 1);
            for (ArenaSpawns arena : tierArenas.get(tier)) {
                ArenaSpawns nextArena = nextTier.stream().min(Comparator.comparingDouble(
                        candidate -> distanceSquared(candidate.center(), arena.center()))).orElse(null);
                if (nextArena == null) continue;
                Location guide = arena.center().clone().add(nextArena.center().toVector()
                        .subtract(arena.center().toVector()).multiply(0.5D));
                guidePoints.add(guide);
            }
        }
        markers.put("PINK", guidePoints);
        markers.replaceAll((name, locations) -> List.copyOf(locations));
        return Map.copyOf(markers);
    }

    /** Return the two black spawn markers whose nearest arena centre is {@code center}. */
    static List<Location> spawnsForArena(
            List<Location> centers, List<Location> blackMarkers, Location center) {
        if (centers == null || blackMarkers == null || center == null || centers.isEmpty()) return List.of();
        List<Location> assigned = blackMarkers.stream().filter(marker -> marker != null
                        && marker.getWorld() == center.getWorld())
                .filter(marker -> nearestCenter(centers, marker) == centerIndex(centers, center))
                .sorted(Comparator.comparingDouble(marker -> distanceSquared(marker, center)))
                .limit(2).map(Location::clone).toList();
        return assigned.size() == 2 ? assigned : List.of();
    }

    static boolean hasTwoSpawnsPerArena(List<Location> centers, List<Location> blackMarkers) {
        if (centers == null || centers.isEmpty() || blackMarkers == null) return false;
        return centers.stream().allMatch(center -> spawnsForArena(centers, blackMarkers, center).size() == 2);
    }

    static boolean hasStableSpawnSurface(int walkableNeighbours) {
        return walkableNeighbours >= 6;
    }
    private static int nearestCenter(List<Location> centers, Location marker) {
        int best = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int i = 0; i < centers.size(); i++) {
            Location candidate = centers.get(i);
            if (candidate == null || candidate.getWorld() != marker.getWorld()) continue;
            double distance = distanceSquared(candidate, marker);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static int centerIndex(List<Location> centers, Location center) {
        for (int i = 0; i < centers.size(); i++) {
            Location candidate = centers.get(i);
            if (candidate == center || candidate != null && candidate.getWorld() == center.getWorld()
                    && candidate.getBlockX() == center.getBlockX()
                    && candidate.getBlockY() == center.getBlockY()
                    && candidate.getBlockZ() == center.getBlockZ()) return i;
        }
        return -1;
    }

    private static PairChoice nearestSeparatedPair(List<Location> locations,
            List<ArenaSpawns> existing, double minimumCenterSpacing) {
        PairChoice best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        double minCenterDistanceSquared = minimumCenterSpacing * minimumCenterSpacing;
        for (int i = 0; i < locations.size(); i++) {
            Location first = locations.get(i);
            for (int j = i + 1; j < locations.size(); j++) {
                Location second = locations.get(j);
                if (first.getWorld() != second.getWorld()) continue;
                double spawnDistance = distanceSquared(first, second);
                if (spawnDistance < 64.0D || spawnDistance > 1_600.0D || spawnDistance >= bestDistance) continue;
                Location center = first.clone().add(second.toVector().subtract(first.toVector()).multiply(0.5D));
                boolean overlaps = existing.stream().anyMatch(arena -> arena.center().getWorld() == center.getWorld()
                        && distanceSquared(arena.center(), center) < minCenterDistanceSquared);
                if (overlaps) continue;
                best = new PairChoice(i, j);
                bestDistance = spawnDistance;
            }
        }
        return best;
    }

    private static double horizontalDistanceSquared(Location location, double x, double z) {
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static double distanceSquared(Location first, Location second) {
        double dx = first.getX() - second.getX();
        double dy = first.getY() - second.getY();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record ArenaSpawns(Location center, Location first, Location second) {}
    private record PairChoice(int firstIndex, int secondIndex) {}
}
