package au.com.testplex.arcadeports;

/** Pure progression thresholds for Plexball achievements and career milestones. */
public final class PlexballProgressionRules {
    public static final double SWEET_SPOT_METRES = 75.0D;
    public static final double WARNING_TRACK_METRES = 95.0D;
    public static final double PERFECT_TIMING_MULTIPLIER = 0.99D;
    public static final int PERFECT_CONTACTS_REQUIRED = 3;
    public static final int STICKY_GLOVES_CATCHES = 3;
    public static final int GOLDEN_GLOVE_CATCHES = 5;
    public static final double ALL_ROUNDER_METRES = 75.0D;
    public static final int CLUTCH_HOME_RUNS_REQUIRED = 2;
    public static final int CHAMPION_WIN_STREAK = 5;

    private PlexballProgressionRules() { }

    public static long distanceToTenths(double metres) {
        if (!Double.isFinite(metres) || metres <= 0.0D) return 0L;
        return Math.round(metres * 10.0D);
    }

    public static boolean shouldAwardSweetSpot(double distanceMetres) {
        return Double.isFinite(distanceMetres) && distanceMetres >= SWEET_SPOT_METRES;
    }

    public static boolean shouldAwardWarningTrack(double distanceMetres) {
        return Double.isFinite(distanceMetres) && distanceMetres >= WARNING_TRACK_METRES;
    }

    public static boolean isPerfectContact(double timingMultiplier) {
        return Double.isFinite(timingMultiplier) && timingMultiplier >= PERFECT_TIMING_MULTIPLIER;
    }

    public static boolean shouldAwardPerfectContact(long careerPerfectContacts) {
        return careerPerfectContacts >= PERFECT_CONTACTS_REQUIRED;
    }

    public static boolean shouldAwardStickyGloves(int catchesInMatch) {
        return catchesInMatch >= STICKY_GLOVES_CATCHES;
    }

    public static boolean shouldAwardGoldenGlove(int catchesInMatch) {
        return catchesInMatch >= GOLDEN_GLOVE_CATCHES;
    }

    public static boolean shouldAwardAllRounder(double bestDistanceMetres, int catchesInMatch) {
        return Double.isFinite(bestDistanceMetres) && bestDistanceMetres >= ALL_ROUNDER_METRES
                && catchesInMatch >= 1;
    }

    public static boolean shouldAwardClutchPerformer(boolean wonMatch, int suddenDeathHomeRuns) {
        return wonMatch && suddenDeathHomeRuns >= CLUTCH_HOME_RUNS_REQUIRED;
    }

    public static boolean shouldAwardChampion(int currentWinStreak) {
        return currentWinStreak >= CHAMPION_WIN_STREAK;
    }

    public static int milestoneProgressPercent(long current, long target) {
        if (target <= 0L) return 100;
        if (current <= 0L) return 0;
        if (current >= target) return 100;
        return (int) Math.min(100L, current * 100L / target);
    }
}
