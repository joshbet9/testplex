package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertBounded("RUNNER_MAX_FROSTBALLS", 0.0D, 64.0D);
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
