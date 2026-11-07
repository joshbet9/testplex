package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RunnerAchievementRulesTest {
    @Test void longLeaperCountsOnlyTheRequiredFiniteLeapDistance() {
        assertFalse(RunnerAchievementRules.longLeaper(49.99D));
        assertTrue(RunnerAchievementRules.longLeaper(50.0D));
        assertFalse(RunnerAchievementRules.longLeaper(Double.NaN));
        assertFalse(RunnerAchievementRules.longLeaper(Double.POSITIVE_INFINITY));
    }

    @Test void coldFrontRequiresSevenDistinctPlayers() {
        Set<UUID> targets = new java.util.HashSet<>();
        for (int i = 0; i < 6; i++) targets.add(new UUID(0L, i));
        assertFalse(RunnerAchievementRules.coldFront(targets));
        targets.add(new UUID(0L, 6L));
        targets.add(new UUID(0L, 6L));
        assertTrue(RunnerAchievementRules.coldFront(targets));
    }

    @Test void switcherooRequiresTheSwapToHappenInTheFinalDuel() {
        assertFalse(RunnerAchievementRules.switcheroo(false));
        assertTrue(RunnerAchievementRules.switcheroo(true));
    }

    @Test void deadeyeRequiresAWinningFinalFallCausedByLongRangeArrowCutFloor() {
        assertTrue(RunnerAchievementRules.deadeye(true, true, 25.0D));
        assertFalse(RunnerAchievementRules.deadeye(false, true, 30.0D));
        assertFalse(RunnerAchievementRules.deadeye(true, false, 30.0D));
        assertFalse(RunnerAchievementRules.deadeye(true, true, 24.99D));
    }
}
