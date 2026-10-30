package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MilkCowRulesTest {
    @Test void cowTeamStartsAtTwentyPercentAndLeavesAFarmerInTheGame() {
        assertEquals(0, MilkCowRules.cowCountForPlayers(0));
        assertEquals(0, MilkCowRules.cowCountForPlayers(1));
        assertEquals(1, MilkCowRules.cowCountForPlayers(2));
        assertEquals(1, MilkCowRules.cowCountForPlayers(5));
        assertEquals(2, MilkCowRules.cowCountForPlayers(10));
        assertEquals(4, MilkCowRules.cowCountForPlayers(20));
    }

    @Test void milkScoreStopsAtZeroAndWinsAtFifteen() {
        assertEquals(0, MilkCowRules.scoreAfterChange(1, -2));
        assertEquals(6, MilkCowRules.scoreAfterChange(5, 1));
        assertFalse(MilkCowRules.hasWon(14));
        assertTrue(MilkCowRules.hasWon(15));
    }
}
