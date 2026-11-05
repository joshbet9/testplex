package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DragonsDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void survivalAndWinStreakAchievementConditionsCanBeTunedAndReset() {
        assertFalse(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(60_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonsWinStreak(2));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGONS_SURVIVE_FIVE_MINUTES_MILLIS", 30_000D,
                "DRAGONS_WIN_STREAK_ACHIEVEMENT", 2D));
        assertTrue(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(60_000L));
        assertTrue(ArcadePortRules.shouldAwardDragonsWinStreak(2));

        GameDebugRuntimeOverrides.clear();
        assertFalse(ArcadePortRules.shouldAwardDragonsSurviveFiveMinutes(60_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonsWinStreak(2));
    }
}
