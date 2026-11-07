package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import java.util.Set;

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

    @Test void supremeWizardUsesFifteenLifetimeWins() {
        assertFalse(WizardsAchievementRules.supremeWizard(14));
        assertTrue(WizardsAchievementRules.supremeWizard(15));
    }

    @Test void kitAchievementsRequireTheKitAndTheWinningConditions() {
        assertTrue(WizardsAchievementRules.spellKillsAllCategories(true,
                Set.of(WizardsSpell.Category.ATTACK, WizardsSpell.Category.SUPPORT, WizardsSpell.Category.MISC)));
        assertFalse(WizardsAchievementRules.spellKillsAllCategories(false,
                Set.of(WizardsSpell.Category.ATTACK, WizardsSpell.Category.SUPPORT, WizardsSpell.Category.MISC)));
        assertTrue(WizardsAchievementRules.mageBonusSpellElimination(WizardsKit.MAGE, true));
        assertFalse(WizardsAchievementRules.mageBonusSpellElimination(WizardsKit.WIZARD, true));
        assertTrue(WizardsAchievementRules.sorcererUsedEveryStartingWand(
                WizardsKit.SORCERER, true, Set.of(0, 1, 2)));
        assertFalse(WizardsAchievementRules.sorcererUsedEveryStartingWand(
                WizardsKit.SORCERER, true, Set.of(0, 1)));
        assertTrue(WizardsAchievementRules.mysticManaRegenerationVictory(WizardsKit.MYSTIC, true, 100.0F));
        assertFalse(WizardsAchievementRules.mysticManaRegenerationVictory(WizardsKit.MYSTIC, true, 99.9F));
        assertTrue(WizardsAchievementRules.witchDoctorManaSpendVictory(WizardsKit.WITCH_DOCTOR, true, 150.0F));
        assertFalse(WizardsAchievementRules.witchDoctorManaSpendVictory(WizardsKit.WITCH_DOCTOR, true, 149.0F));
    }

    @Test void spellAchievementsRequireDistinctTargetsOrAWinningEndgameSpellElimination() {
        assertTrue(WizardsAchievementRules.threeKillsFromOneCast(3));
        assertFalse(WizardsAchievementRules.threeKillsFromOneCast(2));
        assertTrue(WizardsAchievementRules.spellbookCurator(true, true));
        assertFalse(WizardsAchievementRules.spellbookCurator(false, true));
        assertTrue(WizardsAchievementRules.finalWord(true, true));
        assertFalse(WizardsAchievementRules.finalWord(true, false));
    }
}
