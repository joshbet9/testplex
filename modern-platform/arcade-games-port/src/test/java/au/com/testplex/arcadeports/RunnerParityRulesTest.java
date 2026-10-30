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
}
