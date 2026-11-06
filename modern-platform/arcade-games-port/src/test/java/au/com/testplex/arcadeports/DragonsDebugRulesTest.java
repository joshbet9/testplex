package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import org.bukkit.util.Vector;

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

    @Test
    void spawnAndDodgemsConditionsUseProfileValuesAndReset() {
        assertTrue(ArcadePortRules.shouldSpawnTestDragon(4, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(5, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(0, 9_999L, 10_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonsDodgems(59_999L));
        assertTrue(ArcadePortRules.shouldAwardDragonsDodgems(60_000L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGONS_MAX_DRAGONS", 1,
                "DRAGONS_DODGEMS_QUIET_MILLIS", 1_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(1, 10_000L, 10_000L));
        assertTrue(ArcadePortRules.shouldSpawnTestDragon(0, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonsDodgems(999L));
        assertTrue(ArcadePortRules.shouldAwardDragonsDodgems(1_000L));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertTrue(ArcadePortRules.shouldSpawnTestDragon(4, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldSpawnTestDragon(5, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonsDodgems(1_000L));
    }

    @Test
    void movementCombatAndKitVariablesChangeRealRuleOutputsAndReset() {
        assertEquals(new Vector(0.0D, 0.4D, 1.2D),
                ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 0.0D, 1.0D), true));
        assertEquals(new Vector(1.0D, 0.8D, 0.0D),
                ArcadePortRules.dragonsContactKnockback(new Vector(1.0D, 0.0D, 0.0D), true));
        assertEquals(0.14D, ArcadePortRules.dragonsTamerVerticalVelocity(1.0D), 1.0e-9);
        assertTrue(ArcadePortRules.shouldRetargetDragon(9.0D, 100.0D, true));
        assertFalse(ArcadePortRules.shouldRetargetDragon(20.0D, 49.0D, true));
        assertEquals(new Vector(0.0D, 0.12D, 0.72D),
                ArcadePortRules.dragonsGliderVelocity(new Vector(0.0D, 0.0D, 1.0D)));
        assertEquals(new Vector(0.0D, 0.04D, 0.32D),
                ArcadePortRules.autonomousDragonFlight(new Vector(0.0D, 0.0D, 1.0D), 0.32D));
        assertFalse(ArcadePortRules.shouldDragonSparklerAttract(3.99D, false));
        assertTrue(ArcadePortRules.shouldDragonSparklerAttract(4.0D, false));
        assertEquals(new Vector(0.0D, 0.0D, 3.0D),
                ArcadePortRules.dragonsBarrageArrowVelocity(new Vector(0.0D, 0.0D, 1.0D), new Vector()));
        assertEquals(-0.05D, ArcadePortRules.dragonsBarrageJitterComponent(0.0D), 1.0e-9);
        assertEquals(0.5D, ArcadePortRules.dragonsFrostInitialLift());
        assertEquals(0.2D, ArcadePortRules.dragonsFrostPathStep());
        assertEquals(1.0D, ArcadePortRules.dragonFallDamageAfterReduction(2.0D));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("DRAGONS_COWARD_LEAP_HORIZONTAL_SPEED", 2.0D),
                Map.entry("DRAGONS_COWARD_LEAP_FACING_Y_BONUS", 0.5D),
                Map.entry("DRAGONS_COWARD_LEAP_MAX_Y", 0.6D),
                Map.entry("DRAGONS_COWARD_LEAP_GROUNDED_Y_BONUS", 0.1D),
                Map.entry("DRAGONS_CONTACT_HORIZONTAL_KNOCKBACK", 2.0D),
                Map.entry("DRAGONS_CONTACT_VERTICAL_BONUS", 1.0D),
                Map.entry("DRAGONS_CONTACT_MAX_Y", 0.5D),
                Map.entry("DRAGONS_CONTACT_GROUNDED_Y_BONUS", 0.1D),
                Map.entry("DRAGONS_TAMER_VERTICAL_FLIGHT_FACTOR", 0.2D),
                Map.entry("DRAGONS_TAMER_VERTICAL_FLIGHT_CAP", 0.5D),
                Map.entry("DRAGONS_RETARGET_ALTITUDE_Y", 30.0D),
                Map.entry("DRAGONS_RETARGET_DISTANCE_SQUARED", 50.0D),
                Map.entry("DRAGONS_GLIDER_HORIZONTAL_SPEED", 1.0D),
                Map.entry("DRAGONS_GLIDER_VERTICAL_BASE", 0.2D),
                Map.entry("DRAGONS_GLIDER_MIN_VERTICAL", -0.5D),
                Map.entry("DRAGONS_GLIDER_MAX_VERTICAL", 0.5D),
                Map.entry("DRAGONS_FLIGHT_VERTICAL_BIAS", 0.2D),
                Map.entry("DRAGONS_FLIGHT_MIN_VERTICAL_SPEED", 0.0D),
                Map.entry("DRAGONS_FLIGHT_MAX_VERTICAL_SPEED", 0.3D),
                Map.entry("DRAGONS_SPARKLER_MIN_ATTRACT_Y", 1.0D),
                Map.entry("DRAGONS_BARRAGE_ARROW_SPEED", 5.0D),
                Map.entry("DRAGONS_BARRAGE_ARROW_SPREAD", 0.4D),
                Map.entry("DRAGONS_FROST_LAUNCH_Y", 0.8D),
                Map.entry("DRAGONS_FROST_PATH_STEP", 0.4D),
                Map.entry("DRAGONS_FALL_DAMAGE_REDUCTION", 0.5D)));
        assertEquals(new Vector(0.0D, 0.6D, 2.0D),
                ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 0.0D, 1.0D), true));
        assertEquals(new Vector(2.0D, 0.6D, 0.0D),
                ArcadePortRules.dragonsContactKnockback(new Vector(1.0D, 0.0D, 0.0D), true));
        assertEquals(0.2D, ArcadePortRules.dragonsTamerVerticalVelocity(1.0D), 1.0e-9);
        assertTrue(ArcadePortRules.shouldRetargetDragon(40.0D, 49.0D, true));
        assertEquals(new Vector(0.0D, 0.5D, 0.0D),
                ArcadePortRules.dragonsGliderVelocity(new Vector(0.0D, 1.0D, 0.0D)));
        assertEquals(new Vector(0.0D, 0.2D, 0.32D),
                ArcadePortRules.autonomousDragonFlight(new Vector(0.0D, 0.0D, 1.0D), 0.32D));
        assertTrue(ArcadePortRules.shouldDragonSparklerAttract(1.0D, false));
        assertEquals(new Vector(1.0D, 0.0D, 5.0D),
                ArcadePortRules.dragonsBarrageArrowVelocity(new Vector(0.0D, 0.0D, 1.0D), new Vector(0.2D, 0.0D, 0.0D)));
        assertEquals(0.2D, ArcadePortRules.dragonsBarrageJitterComponent(1.0D), 1.0e-9);
        assertEquals(0.8D, ArcadePortRules.dragonsFrostInitialLift());
        assertEquals(0.4D, ArcadePortRules.dragonsFrostPathStep());
        assertEquals(1.5D, ArcadePortRules.dragonFallDamageAfterReduction(2.0D));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(new Vector(0.0D, 0.4D, 1.2D),
                ArcadePortRules.dragonsLeapVelocity(new Vector(0.0D, 0.0D, 1.0D), true));
        assertEquals(new Vector(1.0D, 0.8D, 0.0D),
                ArcadePortRules.dragonsContactKnockback(new Vector(1.0D, 0.0D, 0.0D), true));
        assertEquals(0.14D, ArcadePortRules.dragonsTamerVerticalVelocity(1.0D), 1.0e-9);
        assertFalse(ArcadePortRules.shouldRetargetDragon(40.0D, 49.0D, true));
        assertEquals(new Vector(0.0D, 0.12D, 0.72D),
                ArcadePortRules.dragonsGliderVelocity(new Vector(0.0D, 0.0D, 1.0D)));
        assertEquals(new Vector(0.0D, 0.04D, 0.32D),
                ArcadePortRules.autonomousDragonFlight(new Vector(0.0D, 0.0D, 1.0D), 0.32D));
        assertFalse(ArcadePortRules.shouldDragonSparklerAttract(1.0D, false));
        assertEquals(1.0D, ArcadePortRules.dragonFallDamageAfterReduction(2.0D));
    }

    @Test
    void exposedDragonMechanicsHaveBoundedProfileControls() {
        assertVariable("DRAGONS_MAX_DRAGONS", 0.0D, 64.0D);
        assertVariable("DRAGONS_DODGEMS_QUIET_MILLIS", 0.0D, 120_000.0D);
        assertVariable("DRAGONS_COWARD_LEAP_HORIZONTAL_SPEED", 0.0D, 8.0D);
        assertVariable("DRAGONS_CONTACT_HORIZONTAL_KNOCKBACK", 0.0D, 8.0D);
        assertVariable("DRAGONS_TAMER_VERTICAL_FLIGHT_CAP", 0.0D, 8.0D);
        assertVariable("DRAGONS_RETARGET_DISTANCE_SQUARED", 0.0D, 4_096.0D);
        assertVariable("DRAGONS_GLIDER_HORIZONTAL_SPEED", 0.0D, 8.0D);
        assertVariable("DRAGONS_FROST_PATH_STEP", 0.01D, 4.0D);
        assertVariable("DRAGONS_BARRAGE_ARROW_SPEED", 0.0D, 8.0D);
        assertVariable("DRAGONS_FLIGHT_MIN_VERTICAL_SPEED", -1.0D, 1.0D);
        assertVariable("DRAGONS_FALL_DAMAGE_REDUCTION", 0.0D, 40.0D);
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.DRAGONS, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
