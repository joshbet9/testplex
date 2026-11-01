package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RunnerParityRulesTest {
    @Test
    void stepsAchievementMeansStrictlyOverFourHundredBlocks() {
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(400.0D));
        assertTrue(ArcadePortRules.shouldAwardRunnerSteps(400.01D));
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(Double.NaN));
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(Double.POSITIVE_INFINITY));
    }

    @Test
    void hopAchievementRequiresOneHundredJumps() {
        assertFalse(ArcadePortRules.shouldAwardRunnerHopToIt(99));
        assertTrue(ArcadePortRules.shouldAwardRunnerHopToIt(100));
    }

    @Test
    void flashRequiresAWinWithinNinetySeconds() {
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(90_001L, true));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(90_000L, true));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(1L, true));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(90_000L, false));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(-1L, true));
    }

    @Test
    void movementSegmentsIgnoreTeleportsAndLargeCorrections() {
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(0.0D));
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(2.01D));
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(Double.NaN));
        assertTrue(ArcadePortRules.shouldCountRunnerWalkSegment(0.42D));
        assertTrue(ArcadePortRules.shouldCountRunnerWalkSegment(2.0D));
    }

    @Test
    void selectedTrailColourIsRetainedWhenItDoesNotConflict() {
        assertEquals("purple", ArcadePortRules.resolveRunnerTrailColour("purple", Map.of("cyan", 20)));
    }

    @Test
    void unusedTrailColourIsPreferredWhenSelectedColourConflicts() {
        assertEquals("white", ArcadePortRules.resolveRunnerTrailColour("cyan", Map.of("cyan", 12)));
    }

    @Test
    void leastRepresentedAlternativeWinsWhenEveryColourAppearsOnTheMap() {
        assertEquals("green", ArcadePortRules.resolveRunnerTrailColour("cyan",
                Map.ofEntries(
                        Map.entry("cyan", 10), Map.entry("white", 6), Map.entry("orange", 5),
                        Map.entry("magenta", 7), Map.entry("light-blue", 4), Map.entry("yellow", 8),
                        Map.entry("lime", 9), Map.entry("pink", 7), Map.entry("gray", 5),
                        Map.entry("light-gray", 6), Map.entry("purple", 3), Map.entry("blue", 4),
                        Map.entry("brown", 2), Map.entry("green", 1), Map.entry("black", 3))));
    }

    @Test
    void redAndUnknownColoursAreNeverSelected() {
        assertFalse(ArcadePortRules.runnerTrailColours().contains("red"));
        assertEquals("cyan", ArcadePortRules.resolveRunnerTrailColour("red", Map.of()));
        assertEquals("cyan", ArcadePortRules.resolveRunnerTrailColour("unknown", Map.of()));
    }

    @Test
    void sourceBlockDecayAndFallingBlockLifetimeMatchReborn() {
        assertEquals(600L, ArcadePortRules.RUNNER_BLOCK_DECAY_MILLIS);
        assertEquals(1_200L, ArcadePortRules.RUNNER_BLOCK_LIFETIME_MILLIS);
        assertFalse(ArcadePortRules.shouldDropRunnerBlock(599L));
        assertTrue(ArcadePortRules.shouldDropRunnerBlock(600L));
    }

    @Test
    void sourceKitCooldownsAndFrostyRechargeMatchReborn() {
        assertEquals(8_000L, ArcadePortRules.RUNNER_LEAP_COOLDOWN_MILLIS);
        assertEquals(6_000L, ArcadePortRules.RUNNER_QUICKSHOT_COOLDOWN_MILLIS);
        assertEquals(500L, ArcadePortRules.RUNNER_FROSTBALL_INTERVAL_MILLIS);
        assertEquals(16, ArcadePortRules.RUNNER_MAX_FROSTBALLS);
        assertEquals(40, ArcadePortRules.RUNNER_FROSTBALL_SLOW_TICKS);
        assertEquals(2, ArcadePortRules.RUNNER_FROSTBALL_SLOW_AMPLIFIER);
    }

    @Test
    void sourceKitAliasesNormalizeToTheThreeLegacyKits() {
        assertEquals("leaper", ArcadePortRules.normalizeRunnerKit("runnerkit-leaper"));
        assertEquals("archer", ArcadePortRules.normalizeRunnerKit("Runner-Archer"));
        assertEquals("frosty", ArcadePortRules.normalizeRunnerKit("frosty"));
        assertEquals(null, ArcadePortRules.normalizeRunnerKit("tank"));
        assertEquals(null, ArcadePortRules.normalizeRunnerKit(null));
    }

    @Test
    void onlyEligibleExposedBlocksAreMarkedForTheRunnerTrail() {
        assertTrue(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(false, false, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, true, false, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, true, true, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, false, false));
        assertFalse(ArcadePortRules.shouldMarkRunnerBlock(true, false, false, true, true));
    }

    @Test
    void fallingBlockCollisionOnlyHitsPlayersAtOrBelowTheBlock() {
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(10.0D, 10.0D));
        assertTrue(ArcadePortRules.shouldRunnerFallingBlockHit(9.0D, 10.0D));
        assertFalse(ArcadePortRules.shouldRunnerFallingBlockHit(10.51D, 10.0D));
    }
}
