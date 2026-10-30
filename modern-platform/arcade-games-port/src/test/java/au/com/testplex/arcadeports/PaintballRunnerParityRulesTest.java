package au.com.testplex.arcadeports;

import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaintballRunnerParityRulesTest {
    @Test
    void paintballTeamOrderMatchesFrostAquaThenNetherRed() {
        assertFalse(ArcadePortRules.isPaintballRedTeam(0));
        assertTrue(ArcadePortRules.isPaintballRedTeam(1));
        assertEquals(Color.AQUA, ArcadePortRules.paintballTeamColor(0));
        assertEquals(Color.RED, ArcadePortRules.paintballTeamColor(1));
    }

    @Test
    void paintballImpactRecoloursWoolAndHardClayForTheCorrectTeam() {
        assertEquals(Material.LIGHT_BLUE_WOOL,
                ArcadePortRules.paintballPaintedBlock(Material.WHITE_WOOL, 0));
        assertEquals(Material.RED_WOOL,
                ArcadePortRules.paintballPaintedBlock(Material.WHITE_WOOL, 1));
        assertEquals(Material.LIGHT_BLUE_TERRACOTTA,
                ArcadePortRules.paintballPaintedBlock(Material.TERRACOTTA, 0));
        assertEquals(Material.RED_TERRACOTTA,
                ArcadePortRules.paintballPaintedBlock(Material.TERRACOTTA, 1));
    }

    @Test
    void paintballImpactDoesNotChangeUnpaintableBlocks() {
        assertNull(ArcadePortRules.paintballPaintedBlock(Material.STONE, 0));
        assertNull(ArcadePortRules.paintballPaintedBlock(Material.GLASS, 1));
        assertNull(ArcadePortRules.paintballPaintedBlock(Material.BLUE_GLAZED_TERRACOTTA, 1));
        assertNull(ArcadePortRules.paintballPaintedBlock(null, 0));
    }

    @Test
    void paintballSourceTrackerThresholdsAreExact() {
        assertFalse(ArcadePortRules.shouldAwardPaintballKillingSpree(3));
        assertTrue(ArcadePortRules.shouldAwardPaintballKillingSpree(4));
        assertFalse(ArcadePortRules.shouldAwardPaintballLastStand(2));
        assertTrue(ArcadePortRules.shouldAwardPaintballLastStand(3));
        assertFalse(ArcadePortRules.shouldAwardPaintballSpeedrunner(30_000L, true));
        assertTrue(ArcadePortRules.shouldAwardPaintballSpeedrunner(29_999L, true));
        assertFalse(ArcadePortRules.shouldAwardPaintballSpeedrunner(10_000L, false));
    }

    @Test
    void paintballFlawlessVictoryRequiresFourStartersAndEveryTeammateToSurvive() {
        assertFalse(ArcadePortRules.shouldAwardPaintballFlawlessVictory(3, 3, true));
        assertFalse(ArcadePortRules.shouldAwardPaintballFlawlessVictory(4, 3, true));
        assertFalse(ArcadePortRules.shouldAwardPaintballFlawlessVictory(4, 4, false));
        assertTrue(ArcadePortRules.shouldAwardPaintballFlawlessVictory(4, 4, true));
    }

    @Test
    void runnerStepsAchievementIsStrictlyOverFourHundredHorizontalBlocks() {
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(400.0D));
        assertTrue(ArcadePortRules.shouldAwardRunnerSteps(400.01D));
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(Double.NaN));
        assertFalse(ArcadePortRules.shouldAwardRunnerSteps(Double.POSITIVE_INFINITY));
    }

    @Test
    void runnerMovementSegmentsRejectTeleportsAndLargePositionCorrections() {
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(0.0D));
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(2.01D));
        assertFalse(ArcadePortRules.shouldCountRunnerWalkSegment(Double.NaN));
        assertTrue(ArcadePortRules.shouldCountRunnerWalkSegment(0.42D));
        assertTrue(ArcadePortRules.shouldCountRunnerWalkSegment(2.0D));
    }

    @Test
    void runnerHopAchievementRequiresOneHundredJumps() {
        assertFalse(ArcadePortRules.shouldAwardRunnerHopToIt(99));
        assertTrue(ArcadePortRules.shouldAwardRunnerHopToIt(100));
    }

    @Test
    void runnerFlashRequiresAWinWithinNinetySeconds() {
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(90_001L, true));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(90_000L, true));
        assertTrue(ArcadePortRules.shouldAwardRunnerFlash(1L, true));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(90_000L, false));
        assertFalse(ArcadePortRules.shouldAwardRunnerFlash(-1L, true));
    }

    @Test
    void runnerTrailColourSelectionAvoidsMapPaletteConflicts() {
        assertEquals("purple", ArcadePortRules.resolveRunnerTrailColour("purple", Map.of("cyan", 20)));
        assertEquals("white", ArcadePortRules.resolveRunnerTrailColour("cyan", Map.of("cyan", 12)));
        assertFalse(ArcadePortRules.runnerTrailColours().contains("red"));
        assertEquals("cyan", ArcadePortRules.resolveRunnerTrailColour("red", Map.of()));
    }
}
