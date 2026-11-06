package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroesOfGwenDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void storeHealthPotionsUseProfileBackedHealingAndRestoreSourceDefaults() {
        assertEquals(4, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(8, ArcadePortRules.heroesOfGwenHealthPotionHealHp(true));
        assertEquals(20.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(16.0D, 20.0D, true));

        GameDebugRuntimeOverrides.activate(Map.of(
                "HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP", 7,
                "HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP", 13));
        assertEquals(7, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(20.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(9.0D, 20.0D, true));
        assertEquals(16.0D, ArcadePortRules.heroesOfGwenHealthPotionResult(9.0D, 16.0D, true));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(4, ArcadePortRules.heroesOfGwenHealthPotionHealHp(false));
        assertEquals(8, ArcadePortRules.heroesOfGwenHealthPotionHealHp(true));
    }

    @Test
    void bothPotionHealingValuesAreAvailableWithSafeRangesInTheHeroesDebugMenu() {
        GameDebugVariableCatalog.Variable small = GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP");
        GameDebugVariableCatalog.Variable large = GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP");
        assertNotNull(small);
        assertNotNull(large);
        assertEquals(0.0D, small.minimum());
        assertEquals(40.0D, small.maximum());
        assertEquals(0.0D, large.minimum());
        assertEquals(40.0D, large.maximum());
    }

    @Test
    void objectiveBossAndHeroDamageRulesChangeFromProfilesAndRestoreDefaults() {
        assertEquals(4.0D, ArcadePortRules.heroesOfGwenWitherEarthquakeDamage(4.0D), 0.0001D);
        assertEquals(15.0D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(2), 0.0001D);
        assertEquals(7.5D, ArcadePortRules.heroesOfGwenRowenaBowDamage(2), 0.0001D);
        assertTrue(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(10.0D));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("HEROES_OF_GWEN_WITHER_EARTHQUAKE_RADIUS", 4.0D),
                Map.entry("HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE", 8.0D),
                Map.entry("HEROES_OF_GWEN_DEVON_TNT_BOW_BASE_DAMAGE", 10.0D),
                Map.entry("HEROES_OF_GWEN_DEVON_TNT_BOW_POWER_DAMAGE_PER_LEVEL", 0.5D),
                Map.entry("HEROES_OF_GWEN_ROWENA_BOW_BASE_DAMAGE", 8.0D),
                Map.entry("HEROES_OF_GWEN_ROWENA_BOW_POWER_DAMAGE_PER_LEVEL", 0.5D),
                Map.entry("HEROES_OF_GWEN_ANATH_METEOR_RADIUS", 4.0D)));

        assertEquals(8.0D, ArcadePortRules.heroesOfGwenWitherEarthquakeDamage(2.0D), 0.0001D);
        assertEquals(30.0D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(2), 0.0001D);
        assertEquals(16.0D, ArcadePortRules.heroesOfGwenRowenaBowDamage(2), 0.0001D);
        assertFalse(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(4.1D));

        GameDebugRuntimeOverrides.clear();
        assertEquals(4.0D, ArcadePortRules.heroesOfGwenWitherEarthquakeDamage(4.0D), 0.0001D);
        assertEquals(15.0D, ArcadePortRules.heroesOfGwenDevonTntArrowDamage(2), 0.0001D);
        assertEquals(7.5D, ArcadePortRules.heroesOfGwenRowenaBowDamage(2), 0.0001D);
        assertTrue(ArcadePortRules.heroesOfGwenAnathMeteorContainsDistance(10.0D));
    }

    @Test
    void teamScaledTowersAndOvertimeThresholdsUseTheSelectedProfile() {
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_TOWER_REFERENCE_TEAM_SIZE"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_TOWER_DAMAGE"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN,
                "HEROES_OF_GWEN_TOWER_UPDATE_INTERVAL_MILLIS"));
        assertEquals(500.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(true, 9));
        assertEquals(1_000.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(false, 9));
        assertEquals(223.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(true, 4));
        assertEquals(3.0D, ArcadePortRules.heroesOfGwenTowerDamage());
        assertEquals(2.6D, ArcadePortRules.heroesOfGwenTowerHeal());
        assertEquals(100.0D, ArcadePortRules.heroesOfGwenTowerRangeSquared());
        assertFalse(ArcadePortRules.heroesOfGwenOvertimeHasStarted(1_000L, 900_999L));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("HEROES_OF_GWEN_TOWER_REFERENCE_TEAM_SIZE", 5),
                Map.entry("HEROES_OF_GWEN_OUTER_TOWER_HEALTH", 600),
                Map.entry("HEROES_OF_GWEN_INNER_TOWER_HEALTH", 1_200),
                Map.entry("HEROES_OF_GWEN_TOWER_DAMAGE", 5.0D),
                Map.entry("HEROES_OF_GWEN_TOWER_HEAL", 4.0D),
                Map.entry("HEROES_OF_GWEN_TOWER_RANGE_SQUARED", 144.0D),
                Map.entry("HEROES_OF_GWEN_OVERTIME_DELAY_MILLIS", 60_000L),
                Map.entry("HEROES_OF_GWEN_OVERTIME_PATH_ADVANCE_DISTANCE_SQUARED", 25.0D)));
        assertEquals(480.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(true, 4));
        assertEquals(960.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(false, 4));
        assertEquals(5.0D, ArcadePortRules.heroesOfGwenTowerDamage());
        assertEquals(4.0D, ArcadePortRules.heroesOfGwenTowerHeal());
        assertEquals(144.0D, ArcadePortRules.heroesOfGwenTowerRangeSquared());
        assertTrue(ArcadePortRules.heroesOfGwenOvertimeHasStarted(1_000L, 61_000L));
        assertEquals(3, ArcadePortRules.heroesOfGwenOvertimeNextTargetIndex(2, 8, 24.0D));
        assertEquals(2, ArcadePortRules.heroesOfGwenOvertimeNextTargetIndex(2, 8, 25.0D));

        GameDebugRuntimeOverrides.clear();
        assertEquals(500.0D, ArcadePortRules.heroesOfGwenTowerMaximumHealth(true, 9));
        assertEquals(3.0D, ArcadePortRules.heroesOfGwenTowerDamage());
        assertFalse(ArcadePortRules.heroesOfGwenOvertimeHasStarted(1_000L, 61_000L));
    }

    @Test
    void captureAndMinionControlsReachGameplayRulesAndRestoreSourceDefaults() {
        assertEquals(1_000L, ArcadePortRules.heroesOfGwenCaptureUpdateIntervalMillis());
        assertEquals(25.0D, ArcadePortRules.heroesOfGwenCapturePointRangeSquared());
        assertEquals(10.0D, ArcadePortRules.heroesOfGwenMinionHealth());
        assertEquals(0.2D, ArcadePortRules.heroesOfGwenMinionDamage(false));
        assertEquals(0.4D, ArcadePortRules.heroesOfGwenMinionDamage(true));
        assertEquals(100.0D, ArcadePortRules.heroesOfGwenMinionTargetRangeSquared());
        assertEquals(500L, ArcadePortRules.heroesOfGwenMinionCombatIntervalMillis());

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("HEROES_OF_GWEN_CAPTURE_UPDATE_INTERVAL_MILLIS", 250L),
                Map.entry("HEROES_OF_GWEN_CAPTURE_POINT_RANGE_SQUARED", 49.0D),
                Map.entry("HEROES_OF_GWEN_MINION_HEALTH", 30.0D),
                Map.entry("HEROES_OF_GWEN_MINION_DAMAGE", 1.5D),
                Map.entry("HEROES_OF_GWEN_MINION_TOWER_DAMAGE", 2.5D),
                Map.entry("HEROES_OF_GWEN_MINION_SOURCE_TARGET_RANGE_SQUARED", 225.0D),
                Map.entry("HEROES_OF_GWEN_MINION_COMBAT_INTERVAL_MILLIS", 100L)));
        assertEquals(250L, ArcadePortRules.heroesOfGwenCaptureUpdateIntervalMillis());
        assertEquals(49.0D, ArcadePortRules.heroesOfGwenCapturePointRangeSquared());
        assertEquals(30.0D, ArcadePortRules.heroesOfGwenMinionHealth());
        assertEquals(1.5D, ArcadePortRules.heroesOfGwenMinionDamage(false));
        assertEquals(2.5D, ArcadePortRules.heroesOfGwenMinionDamage(true));
        assertEquals(225.0D, ArcadePortRules.heroesOfGwenMinionTargetRangeSquared());
        assertEquals(100L, ArcadePortRules.heroesOfGwenMinionCombatIntervalMillis());

        for (String key : new String[] {
                "HEROES_OF_GWEN_CAPTURE_UPDATE_INTERVAL_MILLIS",
                "HEROES_OF_GWEN_CAPTURE_POINT_RANGE_SQUARED",
                "HEROES_OF_GWEN_MINION_HEALTH",
                "HEROES_OF_GWEN_MINION_DAMAGE",
                "HEROES_OF_GWEN_MINION_TOWER_DAMAGE",
                "HEROES_OF_GWEN_MINION_SOURCE_TARGET_RANGE_SQUARED",
                "HEROES_OF_GWEN_MINION_COMBAT_INTERVAL_MILLIS"}) {
            GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.HEROES_OF_GWEN, key);
            assertNotNull(variable, key);
            assertTrue(variable.maximum() > variable.minimum(), key);
        }

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(1_000L, ArcadePortRules.heroesOfGwenCaptureUpdateIntervalMillis());
        assertEquals(25.0D, ArcadePortRules.heroesOfGwenCapturePointRangeSquared());
        assertEquals(10.0D, ArcadePortRules.heroesOfGwenMinionHealth());
        assertEquals(0.2D, ArcadePortRules.heroesOfGwenMinionDamage(false));
        assertEquals(0.4D, ArcadePortRules.heroesOfGwenMinionDamage(true));
        assertEquals(100.0D, ArcadePortRules.heroesOfGwenMinionTargetRangeSquared());
        assertEquals(500L, ArcadePortRules.heroesOfGwenMinionCombatIntervalMillis());
    }
}
