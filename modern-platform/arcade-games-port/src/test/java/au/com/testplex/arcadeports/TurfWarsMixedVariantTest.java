package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void mixedArcadeKillsMoveTheFrontlineTwoColumnsInEitherDirection() {
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 0, false));
        assertEquals(-2, ArcadePortRules.advanceTurfFrontline(-1, 1, false));
        assertEquals(1, ArcadePortRules.advanceTurfFrontline(-1, 0, true));
        assertEquals(-3, ArcadePortRules.advanceTurfFrontline(-1, 1, true));
        assertEquals(-1, ArcadePortRules.advanceTurfFrontline(-1, 7, true));
    }

    @Test
    void everyNewlyCapturedColumnIsRepaintedForBothTeams() {
        assertEquals(List.of(0), ArcadePortRules.turfWarsCapturedColumns(-1, 0, 0, -20, 20));
        assertEquals(List.of(0, 1), ArcadePortRules.turfWarsCapturedColumns(-1, 1, 0, -20, 20));
        assertEquals(List.of(0, 1), ArcadePortRules.turfWarsCapturedColumns(1, -1, 1, -20, 20));
        assertEquals(List.of(119, 120), ArcadePortRules.turfWarsCapturedColumns(120, 118, 1, 100, 140));
    }

    @Test
    void claimedColumnsRespectInclusiveMinimumAndExclusiveMaximumBounds() {
        assertEquals(List.of(138, 139),
                ArcadePortRules.turfWarsCapturedColumns(137, 139, 0, 100, 140));
        assertEquals(List.of(100, 101),
                ArcadePortRules.turfWarsCapturedColumns(101, 99, 1, 100, 140));
        assertEquals(List.of(),
                ArcadePortRules.turfWarsCapturedColumns(139, 141, 0, 100, 140));
    }
}
