package au.com.testplex.wizards;

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
}
