package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DragonEscapeDebugRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void scoreCadenceAndDisruptorRechargeUseProfileValuesAndReset() {
        assertEquals(2_000L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(8_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGON_ESCAPE_SCORE_UPDATE_MILLIS", 125L,
                "DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS", 2_000L));
        assertEquals(1_625L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(2_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(2_000L, ArcadePortRules.nextDragonEscapeScoreUpdateAt(1_500L));
        assertEquals(8_000L, ArcadePortRules.dragonEscapeNextTntChargeAt(0L));
    }

    @Test void dragonEscapeControlsHavePracticalRanges() {
        assertVariable("DRAGON_ESCAPE_SCORE_UPDATE_MILLIS", 1.0D, 120_000.0D);
        assertVariable("DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS", 0.0D, 120_000.0D);
        assertVariable("DRAGON_ESCAPE_CHASE_SPEED", 0.0D, 2.0D);
        assertVariable("DRAGON_ESCAPE_FINISH_RADIUS", 0.0D, 32.0D);
        assertVariable("DRAGON_ESCAPE_DEFEATER_WINS", 1.0D, 1_000.0D);
        assertVariable("DRAGON_ESCAPE_MAP_WINS_FOR_ACHIEVEMENT", 1.0D, 1_000.0D);
        assertVariable("DRAGON_ESCAPE_STRATS_GAP_BLOCKS", 0.0D, 64.0D);
        assertVariable("DRAGON_ESCAPE_RAGEBAITER_KILLS", 1.0D, 1_000.0D);
        assertVariable("DRAGON_ESCAPE_RAGEBAITER_KILL_WINDOW_MILLIS", 0.0D, 120_000.0D);
        assertVariable("DRAGON_ESCAPE_JUMPER_LEAP_USES", 0.0D, 64.0D);
        assertVariable("DRAGON_ESCAPE_DISRUPTOR_LEAP_USES", 0.0D, 64.0D);
        assertVariable("DRAGON_ESCAPE_WARPER_LEAP_USES", 0.0D, 64.0D);
        assertVariable("DRAGON_ESCAPE_LEAP_FACING_Y_BONUS", 0.0D, 2.0D);
        assertVariable("DRAGON_ESCAPE_LEAP_GROUNDED_Y_BONUS", 0.0D, 2.0D);
    }

    @Test void chaseDiggerRouteAndLeapControlsChangeTheirGameplayRules() {
        assertTrue(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(100.0D));
        assertFalse(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(121.0D));
        assertTrue(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(5_000L));
        assertEquals(4, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(0.4D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.0D, true), 1.0e-9);

        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        java.util.List<Location> sourceMarkers = java.util.List.of(
                new Location(null, 2.5D, 0.0D, 0.0D), new Location(null, 10.0D, 0.0D, 0.0D));
        assertEquals(1, SourceMapCatalog.MapData.legacyNearestRouteMarkers(start, sourceMarkers).size());

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGON_ESCAPE_BLOCK_RADIUS", 12.0D,
                "DRAGON_ESCAPE_DIGGER_OPENING_LOCKOUT_MILLIS", 1_000L,
                "DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE", 2.0D,
                "DRAGON_ESCAPE_JUMPER_LEAP_USES", 7,
                "DRAGON_ESCAPE_LEAP_FACING_Y_BONUS", 0.5D,
                "DRAGON_ESCAPE_LEAP_GROUNDED_Y_BONUS", 0.1D));
        assertTrue(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(121.0D));
        assertFalse(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(5_000L));
        assertEquals(7, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(0.6D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.0D, true), 1.0e-9);
        assertEquals(2, SourceMapCatalog.MapData.legacyNearestRouteMarkers(
                start, sourceMarkers, ArcadePortRules.dragonEscapeDuplicateWaypointDistance()).size());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertFalse(ArcadePortRules.isDragonEscapeBlockInsideDestructionRadius(121.0D));
        assertTrue(ArcadePortRules.isDragonEscapeDiggerOpeningLocked(5_000L));
        assertEquals(4, ArcadePortRules.dragonEscapeLeapUses("jumper"));
        assertEquals(0.4D, ArcadePortRules.dragonEscapeLeapVerticalVelocity(0.0D, true), 1.0e-9);
    }

    @Test void achievementAndAbilityThresholdsUseProfileValuesAndReset() {
        assertEquals(50, ArcadePortRules.dragonEscapeDefeaterWins());
        assertEquals(5, ArcadePortRules.dragonEscapeMapWinsForAchievement());
        assertEquals(40.0D, ArcadePortRules.dragonEscapeStratsGapBlocks());
        assertEquals(3, ArcadePortRules.dragonEscapeRagebaiterKills());
        assertEquals(12_000L, ArcadePortRules.dragonEscapeRagebaiterKillWindowMillis());
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeDefeater(49));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeDefeater(50));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeMapMaster(4));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeMapMaster(5));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeStrats(39.99D));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeStrats(40.0D));
        assertFalse(ArcadePortRules.dragonEscapeRagebaiterKillWithinWindow(12_001L));
        assertTrue(ArcadePortRules.dragonEscapeRagebaiterKillWithinWindow(12_000L));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeRagebaiter(2));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeRagebaiter(3));

        GameDebugRuntimeOverrides.activate(Map.of(
                "DRAGON_ESCAPE_DEFEATER_WINS", 2,
                "DRAGON_ESCAPE_MAP_WINS_FOR_ACHIEVEMENT", 1,
                "DRAGON_ESCAPE_STRATS_GAP_BLOCKS", 8.0D,
                "DRAGON_ESCAPE_RAGEBAITER_KILLS", 1,
                "DRAGON_ESCAPE_RAGEBAITER_KILL_WINDOW_MILLIS", 500L));
        assertEquals(2, ArcadePortRules.dragonEscapeDefeaterWins());
        assertEquals(1, ArcadePortRules.dragonEscapeMapWinsForAchievement());
        assertEquals(8.0D, ArcadePortRules.dragonEscapeStratsGapBlocks());
        assertEquals(1, ArcadePortRules.dragonEscapeRagebaiterKills());
        assertEquals(500L, ArcadePortRules.dragonEscapeRagebaiterKillWindowMillis());
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeDefeater(2));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeDefeater(1));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeMapMaster(1));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeStrats(8.0D));
        assertTrue(ArcadePortRules.dragonEscapeRagebaiterKillWithinWindow(500L));
        assertFalse(ArcadePortRules.dragonEscapeRagebaiterKillWithinWindow(501L));
        assertTrue(ArcadePortRules.shouldAwardDragonEscapeRagebaiter(1));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(50, ArcadePortRules.dragonEscapeDefeaterWins());
        assertEquals(5, ArcadePortRules.dragonEscapeMapWinsForAchievement());
        assertEquals(40.0D, ArcadePortRules.dragonEscapeStratsGapBlocks());
        assertEquals(3, ArcadePortRules.dragonEscapeRagebaiterKills());
        assertEquals(12_000L, ArcadePortRules.dragonEscapeRagebaiterKillWindowMillis());
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeDefeater(2));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeMapMaster(1));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeStrats(8.0D));
        assertFalse(ArcadePortRules.dragonEscapeRagebaiterKillWithinWindow(12_001L));
        assertFalse(ArcadePortRules.shouldAwardDragonEscapeRagebaiter(1));
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.DRAGON_ESCAPE, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
