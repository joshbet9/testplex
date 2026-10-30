package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurfWarsMixedVariantTest {
    @Test
    void mixedArcadeHalvesEveryTurfWarsPhaseWithoutChangingStandardTimings() {
        assertEquals(40_000L, ArcadePortRules.turfWarsInitialBuildPhaseMillis(false));
        assertEquals(20_000L, ArcadePortRules.turfWarsBuildPhaseMillis(false));
        assertEquals(90_000L, ArcadePortRules.turfWarsCombatPhaseMillis(false));

        assertEquals(20_000L, ArcadePortRules.turfWarsInitialBuildPhaseMillis(true));
        assertEquals(10_000L, ArcadePortRules.turfWarsBuildPhaseMillis(true));
        assertEquals(45_000L, ArcadePortRules.turfWarsCombatPhaseMillis(true));
    }

    @Test
    void standardKillsMoveOneThenTwoThenThreeLinesAcrossCombatRounds() {
        assertEquals(1, ArcadePortRules.turfWarsLinesPerKill(1, false));
        assertEquals(2, ArcadePortRules.turfWarsLinesPerKill(2, false));
        assertEquals(3, ArcadePortRules.turfWarsLinesPerKill(3, false));
        assertEquals(4, ArcadePortRules.turfWarsLinesPerKill(4, false));
    }

    @Test
    void mixedArcadeDoublesTheProgressionForEveryCombatRound() {
        assertEquals(2, ArcadePortRules.turfWarsLinesPerKill(1, true));
        assertEquals(4, ArcadePortRules.turfWarsLinesPerKill(2, true));
        assertEquals(6, ArcadePortRules.turfWarsLinesPerKill(3, true));
        assertEquals(8, ArcadePortRules.turfWarsLinesPerKill(4, true));
    }

    @Test
    void legacyFrontlineHelperKeepsItsSimpleFixedStepCompatibilityContract() {
        assertEquals(0, ArcadePortRules.advanceTurfFrontline(-1, 0, false));
        assertEquals(-2, ArcadePortRules.advanceTurfFrontline(-1, 1, false));
        assertEquals(1, ArcadePortRules.advanceTurfFrontline(-1, 0, true));
        assertEquals(-3, ArcadePortRules.advanceTurfFrontline(-1, 1, true));
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 7, true));
    }

    @Test
    void turfWarsKitSelectionNormalizesTheThreeSourceKits() {
        assertEquals("marksman", ArcadePortRules.normalizeTurfWarsKit("Marksman"));
        assertEquals("infiltrator", ArcadePortRules.normalizeTurfWarsKit("turf-wars-infiltrator"));
        assertEquals("shredder", ArcadePortRules.normalizeTurfWarsKit("TURFWARS_SHREDDER"));
        assertNull(ArcadePortRules.normalizeTurfWarsKit("sniper"));
        assertNull(ArcadePortRules.normalizeTurfWarsKit(null));
    }

    @Test
    void constructorAndFletcherPerksUseTheOriginalKitCapsAndIntervals() {
        assertEquals(4_000L, ArcadePortRules.TURF_WARS_CONSTRUCTOR_INTERVAL_MILLIS);
        assertEquals(6, ArcadePortRules.TURF_WARS_STARTING_WOOL);
        assertEquals(24, ArcadePortRules.TURF_WARS_BUILD_PHASE_WOOL);
        assertEquals(8, ArcadePortRules.turfWarsConstructorMax("marksman"));
        assertEquals(4, ArcadePortRules.turfWarsConstructorMax("infiltrator"));
        assertEquals(6, ArcadePortRules.turfWarsConstructorMax("shredder"));
        assertEquals(2, ArcadePortRules.turfWarsArrowMax("marksman"));
        assertEquals(1, ArcadePortRules.turfWarsArrowMax("infiltrator"));
        assertEquals(2, ArcadePortRules.turfWarsArrowMax("shredder"));
        assertEquals(2_000L, ArcadePortRules.turfWarsArrowIntervalMillis("marksman"));
        assertEquals(8_000L, ArcadePortRules.turfWarsArrowIntervalMillis("infiltrator"));
        assertEquals(4_000L, ArcadePortRules.turfWarsArrowIntervalMillis("shredder"));
    }

    @Test
    void shredderBarrageUsesFiveChargesWithOneSecondInitialAndQuarterSecondRefill() {
        assertEquals(5, ArcadePortRules.TURF_WARS_BARRAGE_MAX_CHARGE);
        assertEquals(1_000L, ArcadePortRules.TURF_WARS_BARRAGE_FIRST_CHARGE_MILLIS);
        assertEquals(250L, ArcadePortRules.TURF_WARS_BARRAGE_CHARGE_INTERVAL_MILLIS);
    }

    @Test
    void arrowsAreOneHitExceptForShredderAndSwordMeleeIsStronger() {
        assertEquals(500.0D, ArcadePortRules.turfWarsHitDamage("marksman", true, false), 0.00001D);
        assertEquals(500.0D, ArcadePortRules.turfWarsHitDamage("infiltrator", true, false), 0.00001D);
        assertEquals(9.0D, ArcadePortRules.turfWarsHitDamage("shredder", true, false), 0.00001D);
        assertEquals(6.0D, ArcadePortRules.turfWarsHitDamage("marksman", false, false), 0.00001D);
        assertEquals(12.0D, ArcadePortRules.turfWarsHitDamage("infiltrator", false, true), 0.00001D);
    }

    @Test
    void markerLineMatchingSupportsXOnlyZOnlyAndDiagonalMaps() {
        assertTrue(ArcadePortRules.turfWarsTileOnMarkerLine(120, 15, 120, 0, 1, 0));
        assertFalse(ArcadePortRules.turfWarsTileOnMarkerLine(119, 15, 120, 0, 1, 0));
        assertTrue(ArcadePortRules.turfWarsTileOnMarkerLine(15, -40, 0, -40, 0, 1));
        assertFalse(ArcadePortRules.turfWarsTileOnMarkerLine(15, -39, 0, -40, 0, 1));
        assertTrue(ArcadePortRules.turfWarsTileOnMarkerLine(90, 90, 90, -10, 1, 1));
        assertTrue(ArcadePortRules.turfWarsTileOnMarkerLine(90, 90, -10, 90, 1, 1));
        assertFalse(ArcadePortRules.turfWarsTileOnMarkerLine(89, 89, 90, -10, 1, 1));
    }

    @Test
    void markerMovementFollowsTheSourceAxisAndRemainingLinesChooseTheLeader() {
        assertEquals(4, ArcadePortRules.turfWarsMarkerDelta(0, -1, 4));
        assertEquals(-4, ArcadePortRules.turfWarsMarkerDelta(1, -1, 4));
        assertEquals(-4, ArcadePortRules.turfWarsMarkerDelta(0, 1, 4));
        assertEquals(4, ArcadePortRules.turfWarsMarkerDelta(1, 1, 4));
        assertEquals(12, ArcadePortRules.turfWarsRemainingLines(40, 52));
        assertEquals(0, ArcadePortRules.turfWarsLeadingTeam(3, 5, 0, 99));
        assertEquals(1, ArcadePortRules.turfWarsLeadingTeam(6, 2, 99, 0));
        assertEquals(0, ArcadePortRules.turfWarsLeadingTeam(4, 4, 8, 7));
        assertEquals(-1, ArcadePortRules.turfWarsLeadingTeam(4, 4, 8, 8));
    }

    @Test
    void comebackTrackerStartsAtFiveRemainingLinesAndNotSix() {
        assertFalse(ArcadePortRules.turfWarsShouldTrackComeback(6));
        assertTrue(ArcadePortRules.turfWarsShouldTrackComeback(5));
        assertTrue(ArcadePortRules.turfWarsShouldTrackComeback(0));
    }

    @Test
    void territoryAndSpawnBoundaryIntervalsMatchTheSourcePerks() {
        assertEquals(2_000L, ArcadePortRules.TURF_WARS_TERRITORY_KNOCKBACK_COOLDOWN_MILLIS);
        assertEquals(2_000L, ArcadePortRules.TURF_WARS_SPAWN_DAMAGE_COOLDOWN_MILLIS);
        assertEquals(5_000L, ArcadePortRules.TURF_WARS_INFIlTRATOR_SLOW_INTERVAL_MILLIS);
        assertEquals(2_500L, ArcadePortRules.TURF_WARS_INFIlTRATOR_KILL_SLOW_INTERVAL_MILLIS);
        assertEquals(2.0D, ArcadePortRules.turfWarsSpawnBoundaryDamage(true), 0.00001D);
        assertEquals(500.0D, ArcadePortRules.turfWarsSpawnBoundaryDamage(false), 0.00001D);
        assertEquals(900_000L, ArcadePortRules.TURF_WARS_GAME_TIMEOUT_MILLIS);
    }

    @Test
    void everyNewlyCapturedColumnIsRepaintedForBothTeams() {
        assertEquals(List.of(0), ArcadePortRules.turfWarsCapturedColumns(-1, 0, 0, -20, 20));
        assertEquals(List.of(0, 1), ArcadePortRules.turfWarsCapturedColumns(-1, 1, 0, -20, 20));
        assertEquals(List.of(0, 1), ArcadePortRules.turfWarsCapturedColumns(1, -1, 1, -20, 20));
        assertEquals(List.of(119, 120), ArcadePortRules.turfWarsCapturedColumns(120, 118, 1, 100, 140));
    }

    @Test
    void capturedColumnsRespectInclusiveMinimumAndExclusiveMaximumBounds() {
        assertEquals(List.of(138, 139),
                ArcadePortRules.turfWarsCapturedColumns(137, 139, 0, 100, 140));
        assertEquals(List.of(100, 101),
                ArcadePortRules.turfWarsCapturedColumns(101, 99, 1, 100, 140));
        assertEquals(List.of(),
                ArcadePortRules.turfWarsCapturedColumns(139, 141, 0, 100, 140));
    }

    @Test
    void mapTimeoutUsesTerritoryThenKillsAsTieBreak() {
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(120, 119, 0, 50));
        assertEquals(1, ArcadePortRules.turfWarsTimeoutLeader(118, 119, 50, 0));
        assertEquals(0, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 3));
        assertEquals(-1, ArcadePortRules.turfWarsTimeoutLeader(119, 119, 4, 4));
    }
}
