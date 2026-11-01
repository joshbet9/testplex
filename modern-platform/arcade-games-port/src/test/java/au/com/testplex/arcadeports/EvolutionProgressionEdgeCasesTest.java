package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionProgressionEdgeCasesTest {
    @Test
    void overkillCounterClampsNegativeInputAndSaturatesAtTwo() {
        assertEquals(1, ArcadePortRules.nextEvolutionOverkillKillCount(-5));
        assertEquals(1, ArcadePortRules.nextEvolutionOverkillKillCount(0));
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(1));
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(2));
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(999));
    }

    @Test
    void overkillRequiresTwoKillsBeforeTheEvolution() {
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(-1));
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(0));
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(1));
        assertTrue(ArcadePortRules.evolutionOverkillRequirementMet(2));
        assertTrue(ArcadePortRules.evolutionOverkillRequirementMet(3));
    }

    @Test
    void pityReductionsCannotMoveTheDeadlineBeforeNow() {
        long now = 100_000L;
        long deadline = 180_000L;

        assertEquals(165_000L, ArcadePortRules.reduceEvolutionPityDeadline(deadline, now, 15_000L));
        assertEquals(deadline, ArcadePortRules.reduceEvolutionPityDeadline(deadline, now, -5_000L));
        assertEquals(now, ArcadePortRules.reduceEvolutionPityDeadline(deadline, now, 999_999L));
        assertEquals(now, ArcadePortRules.reduceEvolutionPityDeadline(now - 1L, now, 15_000L));
    }

    @Test
    void pityRemainingAndDisplayClampAtZeroAndRoundUpToWholeSeconds() {
        assertEquals(12_345L, ArcadePortRules.evolutionPityRemainingMillis(112_345L, 100_000L));
        assertEquals(0L, ArcadePortRules.evolutionPityRemainingMillis(99_999L, 100_000L));
        assertEquals("1:30", ArcadePortRules.formatEvolutionPityRemaining(90_000L));
        assertEquals("0:01", ArcadePortRules.formatEvolutionPityRemaining(1L));
        assertEquals("0:00", ArcadePortRules.formatEvolutionPityRemaining(-1L));
    }

    @Test
    void evolutionLevelCannotAdvancePastTheWinningStageOrDropBelowZero() {
        assertEquals(1, ArcadePortRules.nextEvolutionLevel(-4, true));
        assertEquals(3, ArcadePortRules.nextEvolutionLevel(3, false));
        assertEquals(4, ArcadePortRules.nextEvolutionLevel(3, true));
        assertEquals(4, ArcadePortRules.nextEvolutionLevel(4, true));
        assertEquals(5, ArcadePortRules.nextEvolutionLevel(99, false));
    }
}
