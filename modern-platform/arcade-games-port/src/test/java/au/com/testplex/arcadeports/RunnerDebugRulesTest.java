package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RunnerDebugRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void movementAbilityTuningChangesProductionValuesAndResetRestoresSourceDefaults() {
        assertEquals(1.2D, ArcadePortRules.runnerLeapHorizontalSpeed());
        assertEquals(1.2D, ArcadePortRules.runnerLeapVerticalSpeed());
        assertEquals(1.2D, ArcadePortRules.runnerQuickshotProjectileSpeed());
        assertEquals(750L, ArcadePortRules.runnerSwitchGraceMillis());

        GameDebugRuntimeOverrides.activate(Map.of(
                "RUNNER_LEAP_HORIZONTAL_SPEED", 2.0D,
                "RUNNER_LEAP_VERTICAL_SPEED", 1.8D,
                "RUNNER_QUICKSHOT_PROJECTILE_SPEED", 2.4D,
                "RUNNER_SWITCH_GRACE_MILLIS", 1_500L));
        assertEquals(2.0D, ArcadePortRules.runnerLeapHorizontalSpeed());
        assertEquals(1.8D, ArcadePortRules.runnerLeapVerticalSpeed());
        assertEquals(2.4D, ArcadePortRules.runnerQuickshotProjectileSpeed());
        assertEquals(1_500L, ArcadePortRules.runnerSwitchGraceMillis());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(1.2D, ArcadePortRules.runnerLeapHorizontalSpeed());
        assertEquals(1.2D, ArcadePortRules.runnerLeapVerticalSpeed());
        assertEquals(1.2D, ArcadePortRules.runnerQuickshotProjectileSpeed());
        assertEquals(750L, ArcadePortRules.runnerSwitchGraceMillis());
    }

    @Test void runnerCombatAndMovementSettingsAreExposedWithUsefulRanges() {
        assertBounded("RUNNER_BLOCK_DECAY_MILLIS", 0.0D, 60_000.0D);
        assertBounded("RUNNER_LEAP_HORIZONTAL_SPEED", 0.0D, 4.0D);
        assertBounded("RUNNER_LEAP_VERTICAL_SPEED", 0.0D, 4.0D);
        assertBounded("RUNNER_QUICKSHOT_PROJECTILE_SPEED", 0.0D, 4.0D);
        assertBounded("RUNNER_SWITCH_GRACE_MILLIS", 0.0D, 5_000.0D);
        assertBounded("RUNNER_FROSTBALL_SLOW_TICKS", 0.0D, 1_200.0D);
        assertBounded("RUNNER_FROSTBALL_INTERVAL_MILLIS", 0.0D, 10_000.0D);
        assertBounded("RUNNER_FROSTBALL_SLOW_AMPLIFIER", 0.0D, 10.0D);
        assertBounded("RUNNER_MAX_FROSTBALLS", 0.0D, 64.0D);
        assertBounded("RUNNER_LEAP_COOLDOWN_MILLIS", 0.0D, 120_000.0D);
        assertBounded("RUNNER_QUICKSHOT_COOLDOWN_MILLIS", 0.0D, 120_000.0D);
        assertBounded("RUNNER_STEPS_ACHIEVEMENT_BLOCKS", 0.0D, 100_000.0D);
        assertBounded("RUNNER_HOP_ACHIEVEMENT_JUMPS", 0.0D, 1_000.0D);
        assertBounded("RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS", 0.0D, 600_000.0D);
        assertBounded("RUNNER_FROSTBALL_ASSISTS_ACHIEVEMENT_COUNT", 1.0D, 100.0D);
        assertBounded("RUNNER_ARCHER_ASSISTS_ACHIEVEMENT_COUNT", 1.0D, 100.0D);
        assertBounded("RUNNER_FAILED_UPWARDS_MAX_HEALTH", 1.0D, 20.0D);
        assertBounded("RUNNER_SPECIAL_PLAY_MIN_FALL_DISTANCE", 0.0D, 100.0D);
        assertBounded("RUNNER_SPECIAL_PLAY_GROUND_DISTANCE", 0.0D, 16.0D);
        assertBounded("RUNNER_SPECIAL_PLAY_MAX_DOWNWARD_VELOCITY", 0.0D, 5.0D);
        assertBounded("RUNNER_DIVE_EARLY_WINDOW_MILLIS", 0.0D, 600_000.0D);
        assertBounded("RUNNER_DIVE_DROP_BLOCKS", 0.0D, 64.0D);
    }

    @Test void runnerAchievementHelpersUseProfileThresholdsAndRestoreSourceConditions() {
        assertEquals(5, ArcadePortRules.runnerFrostballAssistAchievementCount());
        assertEquals(5, ArcadePortRules.runnerArcherAssistAchievementCount());
        assertEquals(6.0D, ArcadePortRules.runnerFailedUpwardsMaxHealth());
        assertFalse(ArcadePortRules.isRunnerSpecialPlayFall(19.0D, -1.0D, true));
        assertTrue(ArcadePortRules.isRunnerSpecialPlayFall(20.0D, -0.26D, true));
        assertFalse(ArcadePortRules.isRunnerSpecialPlayFall(20.0D, -0.25D, true));
        assertFalse(ArcadePortRules.isRunnerSpecialPlayFall(20.0D, -1.0D, false));
        assertTrue(ArcadePortRules.isRunnerEarlyDive(3_000L, 97.9D, 100.0D));
        assertFalse(ArcadePortRules.isRunnerEarlyDive(3_001L, 97.9D, 100.0D));
        assertFalse(ArcadePortRules.isRunnerEarlyDive(1_000L, 98.0D, 100.0D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "RUNNER_FROSTBALL_ASSISTS_ACHIEVEMENT_COUNT", 2,
                "RUNNER_ARCHER_ASSISTS_ACHIEVEMENT_COUNT", 3,
                "RUNNER_FAILED_UPWARDS_MAX_HEALTH", 10.0D,
                "RUNNER_SPECIAL_PLAY_MIN_FALL_DISTANCE", 5.0D,
                "RUNNER_SPECIAL_PLAY_MAX_DOWNWARD_VELOCITY", 0.1D,
                "RUNNER_DIVE_EARLY_WINDOW_MILLIS", 5_000L,
                "RUNNER_DIVE_DROP_BLOCKS", 1.0D));
        assertEquals(2, ArcadePortRules.runnerFrostballAssistAchievementCount());
        assertEquals(3, ArcadePortRules.runnerArcherAssistAchievementCount());
        assertEquals(10.0D, ArcadePortRules.runnerFailedUpwardsMaxHealth());
        assertTrue(ArcadePortRules.isRunnerSpecialPlayFall(5.0D, -0.11D, true));
        assertTrue(ArcadePortRules.isRunnerEarlyDive(5_000L, 98.9D, 100.0D));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(5, ArcadePortRules.runnerFrostballAssistAchievementCount());
        assertEquals(6.0D, ArcadePortRules.runnerFailedUpwardsMaxHealth());
        assertFalse(ArcadePortRules.isRunnerSpecialPlayFall(5.0D, -0.11D, true));
    }

    @Test void achievementThresholdsChangeTheirActualAwardPredicatesAndReset() {
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(400.0D));
        assertTrue(ArcadePortRules.shouldAwardRunnerSteps(400.1D));
        assertFalse(ArcadePortRules.shouldAwardRunnerHopToIt(99));
        assertTrue(ArcadePortRules.shouldAwardRunnerHopToIt(100));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(90_000L, true));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(90_001L, true));

        GameDebugRuntimeOverrides.activate(Map.of(
                "RUNNER_STEPS_ACHIEVEMENT_BLOCKS", 100.0D,
                "RUNNER_HOP_ACHIEVEMENT_JUMPS", 20,
                "RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS", 30_000L));
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(100.0D));
        assertTrue(ArcadePortRules.shouldAwardRunnerSteps(100.1D));
        assertFalse(ArcadePortRules.shouldAwardRunnerHopToIt(19));
        assertTrue(ArcadePortRules.shouldAwardRunnerHopToIt(20));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(30_000L, true));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(30_001L, true));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(20_000L, false));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(400.0D));
        assertTrue(ArcadePortRules.shouldAwardRunnerHopToIt(100));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(90_000L, true));
    }

    private static void assertBounded(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.RUNNER, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
        assertNotEquals(variable.defaultValue(), GameDebugVariableCatalog.clamp(variable,
                variable.defaultValue() == minimum ? maximum : minimum), key);
    }
}
