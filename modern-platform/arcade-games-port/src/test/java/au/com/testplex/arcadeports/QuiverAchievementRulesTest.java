package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuiverAchievementRulesTest {
    @Test
    void headsRequiresBothAHighArcArrowAndAConfirmedKill() {
        assertTrue(QuiverAchievementRules.shouldAwardHeads(true, true));
        assertFalse(QuiverAchievementRules.shouldAwardHeads(true, false));
        assertFalse(QuiverAchievementRules.shouldAwardHeads(false, true));
        assertFalse(QuiverAchievementRules.shouldAwardHeads(false, false));
    }

    @Test
    void aSeparateTwoKillStreakIsCountedOnlyWhenTheLifeStreakReachesTwo() {
        assertFalse(QuiverAchievementRules.countsSeparateTwoKillStreak(0));
        assertFalse(QuiverAchievementRules.countsSeparateTwoKillStreak(1));
        assertTrue(QuiverAchievementRules.countsSeparateTwoKillStreak(2));
        assertFalse(QuiverAchievementRules.countsSeparateTwoKillStreak(3));
    }

    @Test
    void rhythmHeavenRequiresTenSeparateTwoKillStreaksInTheWinningSession() {
        assertFalse(QuiverAchievementRules.hasTenTwoKillStreaks(9));
        assertTrue(QuiverAchievementRules.hasTenTwoKillStreaks(10));
        assertTrue(QuiverAchievementRules.hasTenTwoKillStreaks(11));
    }
}
