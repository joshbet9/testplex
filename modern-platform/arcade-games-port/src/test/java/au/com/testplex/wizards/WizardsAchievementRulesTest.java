package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WizardsAchievementRulesTest {
    @Test void noSpellWinRequiresZeroSuccessfulCasts() {
        assertTrue(WizardsAchievementRules.noSpellVictory(true, 0));
        assertFalse(WizardsAchievementRules.noSpellVictory(true, 1));
        assertFalse(WizardsAchievementRules.noSpellVictory(false, 0));
    }

    @Test void maximumLevelRequiresAtLeastOneRealCast() {
        assertTrue(WizardsAchievementRules.maxLevelOnlyVictory(true, 2, true));
        assertFalse(WizardsAchievementRules.maxLevelOnlyVictory(true, 0, true));
        assertFalse(WizardsAchievementRules.maxLevelOnlyVictory(true, 3, false));
    }

    @Test void dingDongIsDefeatAfterSixKills() {
        assertTrue(WizardsAchievementRules.sixKillDefeat(false, 6));
        assertFalse(WizardsAchievementRules.sixKillDefeat(false, 5));
        assertFalse(WizardsAchievementRules.sixKillDefeat(true, 9));
    }

    @Test void oneTrickRequiresExactlyOneCategory() {
        assertTrue(WizardsAchievementRules.singleFamilyVictory(true, 1));
        assertFalse(WizardsAchievementRules.singleFamilyVictory(true, 0));
        assertFalse(WizardsAchievementRules.singleFamilyVictory(true, 2));
    }
}
