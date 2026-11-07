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

    @Test
    void sharpshooterRequiresEightShotsAndEightConfirmedHits() {
        assertFalse(QuiverAchievementRules.shouldAwardSharpshooter(7, 7));
        assertTrue(QuiverAchievementRules.shouldAwardSharpshooter(8, 8));
        assertFalse(QuiverAchievementRules.shouldAwardSharpshooter(9, 8));
        assertFalse(QuiverAchievementRules.shouldAwardSharpshooter(8, 7));
    }

    @Test
    void approvedKitAchievementsRequireTheirSpecificGameplayConditions() {
        assertFalse(QuiverAchievementRules.shouldAwardDoubleTake(false, true));
        assertTrue(QuiverAchievementRules.shouldAwardDoubleTake(true, true));
        assertFalse(QuiverAchievementRules.shouldAwardIronResolve(true, 9));
        assertTrue(QuiverAchievementRules.shouldAwardIronResolve(true, 10));
        assertFalse(QuiverAchievementRules.shouldAwardSlamDunk(true, 1));
        assertTrue(QuiverAchievementRules.shouldAwardSlamDunk(true, 2));
        assertFalse(QuiverAchievementRules.shouldAwardNinjaVanish(true, false, 3));
        assertTrue(QuiverAchievementRules.shouldAwardNinjaVanish(true, true, 3));
        assertFalse(QuiverAchievementRules.shouldAwardReboundRemarkable(true, 2));
        assertTrue(QuiverAchievementRules.shouldAwardReboundRemarkable(true, 3));
        assertFalse(QuiverAchievementRules.shouldAwardQuiverJuggler(false, 10, 10));
        assertTrue(QuiverAchievementRules.shouldAwardQuiverJuggler(true, 10, 10));
        assertFalse(QuiverAchievementRules.shouldAwardPhoenixRising(false, true, 20));
        assertFalse(QuiverAchievementRules.shouldAwardPhoenixRising(true, true, 14));
        assertTrue(QuiverAchievementRules.shouldAwardPhoenixRising(true, true, 15));
    }
}
