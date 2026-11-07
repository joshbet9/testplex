package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlexballAchievementRulesTest {
    @Test
    void kitAchievementsRequireTheirOwnActionAndAWin() {
        assertFalse(PlexballAchievementRules.walkOffPowerDive(false, true));
        assertFalse(PlexballAchievementRules.walkOffPowerDive(true, false));
        assertTrue(PlexballAchievementRules.walkOffPowerDive(true, true));

        assertFalse(PlexballAchievementRules.sureHands(true, false, 3));
        assertFalse(PlexballAchievementRules.sureHands(true, true, 2));
        assertTrue(PlexballAchievementRules.sureHands(true, true, 3));

        assertFalse(PlexballAchievementRules.bendItIn(true, true, 2));
        assertTrue(PlexballAchievementRules.bendItIn(true, true, 3));
        assertFalse(PlexballAchievementRules.firstToHome(true, true, 1));
        assertTrue(PlexballAchievementRules.firstToHome(true, true, 2));
        assertFalse(PlexballAchievementRules.highPoint(false, true, 3));
        assertTrue(PlexballAchievementRules.highPoint(true, true, 3));
    }
}
