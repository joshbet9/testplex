package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyWarsIslandSpawnResolverTest {
    @Test
    void fillsMissingMapStartsOnlyWithDistinctTargetsInTheLargestIslandGaps() {
        Location center = new Location(null, 0.0D, 0.0D, 0.0D);
        List<Location> authored = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            double angle = Math.PI * 2.0D * index / 12.0D;
            authored.add(new Location(null, Math.cos(angle) * 80.0D, 75.0D, Math.sin(angle) * 80.0D));
        }

        List<Location> inferredTargets = SkyWarsIslandSpawnResolver.additionalIslandTargets(
                authored, center, 4);

        assertEquals(4, inferredTargets.size());
        for (Location target : inferredTargets) {
            assertEquals(80.0D, Math.hypot(target.getX(), target.getZ()), 0.001D);
            assertTrue(authored.stream().allMatch(start -> horizontalDistanceSquared(start, target)
                    >= 18.0D * 18.0D));
        }
        for (int left = 0; left < inferredTargets.size(); left++) {
            for (int right = left + 1; right < inferredTargets.size(); right++) {
                assertTrue(horizontalDistanceSquared(inferredTargets.get(left), inferredTargets.get(right))
                    >= 18.0D * 18.0D);
            }
        }
    }

    private static double horizontalDistanceSquared(Location left, Location right) {
        double dx = left.getX() - right.getX();
        double dz = left.getZ() - right.getZ();
        return dx * dx + dz * dz;
    }
}
