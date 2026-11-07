package au.com.testplex.wizards;

import java.util.Set;

/** Match-only conditions; no persistent or bot state is consulted here. */
public final class WizardsAchievementRules {
    private WizardsAchievementRules() { }

    public static boolean noSpellVictory(boolean won, int successfulCasts) {
        return won && successfulCasts == 0;
    }

    public static boolean maxLevelOnlyVictory(boolean won, int successfulCasts, boolean allCastsAtMaximum) {
        return won && successfulCasts > 0 && allCastsAtMaximum;
    }

    public static boolean sixKillDefeat(boolean won, int kills) {
        return !won && kills >= 6;
    }

    public static boolean singleFamilyVictory(boolean won, int familiesUsed) {
        return won && familiesUsed == 1;
    }

    public static boolean supremeWizard(int lifetimeWinsAfterRound) {
        return lifetimeWinsAfterRound >= 15;
    }

    public static boolean spellKillsAllCategories(boolean won, Set<WizardsSpell.Category> categories) {
        return won && categories != null && categories.containsAll(Set.of(
                WizardsSpell.Category.ATTACK, WizardsSpell.Category.SUPPORT, WizardsSpell.Category.MISC));
    }

    public static boolean mageBonusSpellElimination(WizardsKit kit, boolean bonusSpellFinalHit) {
        return kit == WizardsKit.MAGE && bonusSpellFinalHit;
    }

    public static boolean sorcererUsedEveryStartingWand(WizardsKit kit, boolean won, Set<Integer> wandSlots) {
        return kit == WizardsKit.SORCERER && won && wandSlots != null
                && wandSlots.containsAll(Set.of(0, 1, 2));
    }

    public static boolean mysticManaRegenerationVictory(WizardsKit kit, boolean won, float manaRegenerated) {
        return kit == WizardsKit.MYSTIC && won && manaRegenerated >= 100.0F;
    }

    public static boolean witchDoctorManaSpendVictory(WizardsKit kit, boolean won, float manaSpent) {
        return kit == WizardsKit.WITCH_DOCTOR && won && manaSpent >= 150.0F;
    }

    public static boolean spellbookCurator(boolean won, boolean completeCategoryBound) {
        return won && completeCategoryBound;
    }

    public static boolean finalWord(boolean won, boolean spellEliminationDuringEndgame) {
        return won && spellEliminationDuringEndgame;
    }

    public static boolean threeKillsFromOneCast(int distinctPlayersKilled) {
        return distinctPlayersKilled >= 3;
    }
}
