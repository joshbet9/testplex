package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpleefAchievementRulesTest {
    @Test
    void squareClearsMayBeMultipleButEveryConnectedPatchMustBeFilledSquare() {
        assertTrue(SpleefAchievementRules.onlySquareAreas(square(0, 0, 2)));
        Set<SpleefAchievementRules.BlockPosition> separate = square(0, 0, 2);
        separate.addAll(square(5, 5, 3));
        assertTrue(SpleefAchievementRules.onlySquareAreas(separate));

        Set<SpleefAchievementRules.BlockPosition> lShape = new HashSet<>(square(0, 0, 2));
        lShape.remove(new SpleefAchievementRules.BlockPosition(1, 1));
        assertFalse(SpleefAchievementRules.onlySquareAreas(lShape));
        assertFalse(SpleefAchievementRules.onlySquareAreas(Set.of(new SpleefAchievementRules.BlockPosition(0, 0))));
        assertFalse(SpleefAchievementRules.onlySquareAreas(Set.of()));
    }

    private static Set<SpleefAchievementRules.BlockPosition> square(int x, int z, int side) {
        Set<SpleefAchievementRules.BlockPosition> blocks = new HashSet<>();
        for (int dx = 0; dx < side; dx++) for (int dz = 0; dz < side; dz++) {
            blocks.add(new SpleefAchievementRules.BlockPosition(x + dx, z + dz));
        }
        return blocks;
    }

    @Test
    void sinkholeRequiresFiveCreditedEliminations() {
        assertFalse(SpleefAchievementRules.sinkhole(4));
        assertTrue(SpleefAchievementRules.sinkhole(5));
    }
}
