package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoopsRemasteredAchievementRulesTest {
    @AfterEach void resetDebugOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void hopsDunkRequiresAnUninterruptedAirborneHopsSequence() {
        assertTrue(HoopsRemasteredAchievementRules.isHopsDunk(true, true));
        assertFalse(HoopsRemasteredAchievementRules.isHopsDunk(true, false));
        assertFalse(HoopsRemasteredAchievementRules.isHopsDunk(false, true));
    }

    @Test void alleyOopRequiresAnAirbornePassAndDunkAfterHops() {
        assertTrue(HoopsRemasteredAchievementRules.isAlleyOopDunk(true, true, true));
        assertFalse(HoopsRemasteredAchievementRules.isAlleyOopDunk(true, true, false));
        assertFalse(HoopsRemasteredAchievementRules.isAlleyOopDunk(true, false, true));
        assertFalse(HoopsRemasteredAchievementRules.isAlleyOopDunk(false, true, true));
    }

    @Test void stealScoreRequiresTheThiefToScoreBeforePossessionIsLost() {
        assertTrue(HoopsRemasteredAchievementRules.isStealScore(true, true));
        assertFalse(HoopsRemasteredAchievementRules.isStealScore(false, true));
        assertFalse(HoopsRemasteredAchievementRules.isStealScore(true, false));
    }

    @Test void blockedShotRecoveryMustBelongToTheBlockersTeam() {
        assertTrue(HoopsRemasteredAchievementRules.isBlockedShotRecovery(true, 0, 0));
        assertFalse(HoopsRemasteredAchievementRules.isBlockedShotRecovery(true, 0, 1));
        assertFalse(HoopsRemasteredAchievementRules.isBlockedShotRecovery(false, 0, 0));
        assertFalse(HoopsRemasteredAchievementRules.isBlockedShotRecovery(true, -1, 0));
    }

    @Test void logoLauncherRequiresTheWinningBasketToBeWorthThree() {
        assertTrue(HoopsRemasteredAchievementRules.isGameWinningThree(true, 3));
        assertFalse(HoopsRemasteredAchievementRules.isGameWinningThree(false, 3));
        assertFalse(HoopsRemasteredAchievementRules.isGameWinningThree(true, 2));
    }

    @Test void buzzerBeaterPasserMustHaveMadeAValidAssistForTheScoringTeam() {
        assertTrue(HoopsRemasteredAchievementRules.isValidBuzzerAssist(10_000L, 2_000L, 0, 0));
        assertFalse(HoopsRemasteredAchievementRules.isValidBuzzerAssist(10_001L, 2_000L, 0, 0));
        assertFalse(HoopsRemasteredAchievementRules.isValidBuzzerAssist(10_000L, 2_000L, 1, 0));
    }
}
