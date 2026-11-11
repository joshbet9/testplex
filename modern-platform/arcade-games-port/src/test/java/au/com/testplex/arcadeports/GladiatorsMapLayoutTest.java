package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GladiatorsMapLayoutTest {
    @Test void inferredSpawnsRequireAnInteriorWalkableSurfaceInsteadOfAThinBorderTop() {
        assertFalse(GladiatorsMapLayout.hasStableSpawnSurface(5));
        assertTrue(GladiatorsMapLayout.hasStableSpawnSurface(6));
        assertTrue(GladiatorsMapLayout.hasStableSpawnSurface(8));
    }

    @Test void inferredLayoutBuildsAllFifteenBracketArenasFromThirtySafeStarts() {
        List<Location> safeStarts = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            double centerX = 40 + (index % 5) * 70;
            double centerZ = 40 + (index / 5) * 70;
            safeStarts.add(new Location(null, centerX - 8, 64, centerZ));
            safeStarts.add(new Location(null, centerX + 8, 64, centerZ));
        }

        Map<String, List<Location>> markers = GladiatorsMapLayout.inferArenaMarkers(safeStarts, 180, 110);

        assertEquals(8, markers.get("GREEN").size());
        assertEquals(4, markers.get("YELLOW").size());
        assertEquals(2, markers.get("ORANGE").size());
        assertEquals(1, markers.get("RED").size());
        assertEquals(30, markers.get("BLACK").size());
        double nearestRoundOne = markers.get("GREEN").stream()
                .mapToDouble(location -> horizontalDistanceSquared(location, 180, 110)).min().orElseThrow();
        double farthestRoundTwo = markers.get("YELLOW").stream()
                .mapToDouble(location -> horizontalDistanceSquared(location, 180, 110)).max().orElseThrow();
        double nearestRoundTwo = markers.get("YELLOW").stream()
                .mapToDouble(location -> horizontalDistanceSquared(location, 180, 110)).min().orElseThrow();
        double farthestRoundThree = markers.get("ORANGE").stream()
                .mapToDouble(location -> horizontalDistanceSquared(location, 180, 110)).max().orElseThrow();
        double nearestRoundThree = markers.get("ORANGE").stream()
                .mapToDouble(location -> horizontalDistanceSquared(location, 180, 110)).min().orElseThrow();
        double grandFinal = horizontalDistanceSquared(markers.get("RED").getFirst(), 180, 110);
        assertTrue(nearestRoundOne >= farthestRoundTwo,
                "The outermost arenas must be assigned to winners' round one");
        assertTrue(nearestRoundTwo >= farthestRoundThree,
                "The next outer tier must be assigned to round two");
        assertTrue(nearestRoundThree >= grandFinal,
                "The innermost arena must be assigned to the final");
        List<Location> allCenters = new ArrayList<>();
        for (String tier : List.of("GREEN", "YELLOW", "ORANGE", "RED")) {
            allCenters.addAll(markers.get(tier));
        }
        assertTrue(GladiatorsMapLayout.hasTwoSpawnsPerArena(allCenters, markers.get("BLACK")));
        assertEquals(14, markers.get("PINK").size());
        Set<String> assignedSpawnPositions = new HashSet<>();
        for (Location center : allCenters) {
            List<Location> arenaSpawns = GladiatorsMapLayout.spawnsForArena(
                    allCenters, markers.get("BLACK"), center);
            assertEquals(2, arenaSpawns.size());
            for (Location spawn : arenaSpawns) {
                assertTrue(assignedSpawnPositions.add(spawn.getBlockX() + ":" + spawn.getBlockY()
                        + ":" + spawn.getBlockZ()), "A duel spawn must belong to only one arena");
            }
        }
        assertEquals(30, assignedSpawnPositions.size());
    }

    private static double horizontalDistanceSquared(Location location, double x, double z) {
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        return dx * dx + dz * dz;
    }

    @Test void inferredLayoutRejectsInsufficientOrCrowdedTerrainStarts() {
        List<Location> insufficient = List.of(
                new Location(null, 0, 64, 0), new Location(null, 16, 64, 0));
        assertTrue(GladiatorsMapLayout.inferArenaMarkers(insufficient, 0, 0).isEmpty());

        List<Location> crowded = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            crowded.add(new Location(null, index % 3, 64, index / 3));
        }
        assertTrue(GladiatorsMapLayout.inferArenaMarkers(crowded, 0, 0).isEmpty());
    }
}
