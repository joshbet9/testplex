package au.com.testplex.arcadeports;

/** Pure trigger thresholds for One in the Quiver achievements. */
public final class QuiverAchievementRules {
    private QuiverAchievementRules() { }

    /** The high-arc flag alone is not sufficient; the arrow must resolve into a valid kill. */
    public static boolean shouldAwardHeads(boolean highArcArrow, boolean confirmedKill) {
        return highArcArrow && confirmedKill;
    }

    /** One separate two-kill streak is counted once, when a life streak first reaches two. */
    public static boolean countsSeparateTwoKillStreak(int currentLifeKillStreak) {
        return currentLifeKillStreak == 2;
    }

    public static boolean hasTenTwoKillStreaks(int separateTwoKillStreaks) {
        return separateTwoKillStreaks >= 10;
    }
}
