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

    /** Source Sharpshooter unlocks on the eighth confirmed arrow hit only if none missed. */
    public static boolean shouldAwardSharpshooter(int arrowsShot, int arrowsHit) {
        return arrowsShot == 8 && arrowsHit == 8;
    }

    public static boolean shouldAwardDoubleTake(boolean leaper, boolean arrowShotDuringDoubleJump) {
        return leaper && arrowShotDuringDoubleJump;
    }

    public static boolean shouldAwardIronResolve(boolean brawler, int meleeKills) {
        return brawler && meleeKills >= 10;
    }

    public static boolean shouldAwardSlamDunk(boolean slamShot, int killsFromOnePound) {
        return slamShot && killsFromOnePound >= 2;
    }

    public static boolean shouldAwardNinjaVanish(boolean ninja, boolean vanishWasActive, int killsDuringVanish) {
        return ninja && vanishWasActive && killsDuringVanish >= 3;
    }

    public static boolean shouldAwardReboundRemarkable(boolean enchanter, int reboundArrowKills) {
        return enchanter && reboundArrowKills >= 3;
    }

    public static boolean shouldAwardQuiverJuggler(boolean won, int bowKills, int meleeKills) {
        return won && bowKills >= 10 && meleeKills >= 10;
    }

    public static boolean shouldAwardPhoenixRising(boolean died, boolean won, int winningLifeStreak) {
        return died && won && winningLifeStreak >= 15;
    }
}
