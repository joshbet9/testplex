package au.com.testplex.arcadeports;

/** Pure progression thresholds for Plexball achievements and career milestones. */
public final class PlexballProgressionRules {
    public static final double SWEET_SPOT_METRES = 50.0D;
    public static final double WARNING_TRACK_METRES = 90.0D;
    public static final double PERFECT_TIMING_MULTIPLIER = 0.99D;
    public static final int GOLDEN_GLOVE_CATCHES = 3;

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

    public static boolean shouldAwardPerfectContact(double timingMultiplier) {
        return Double.isFinite(timingMultiplier) && timingMultiplier >= PERFECT_TIMING_MULTIPLIER;
    }

    public static boolean shouldAwardGoldenGlove(int catchesInMatch) {
        return catchesInMatch >= GOLDEN_GLOVE_CATCHES;
    }

    public static boolean shouldAwardAllRounder(double bestDistanceMetres, int catchesInMatch) {
        return shouldAwardSweetSpot(bestDistanceMetres) && catchesInMatch >= 1;
    }

    public static boolean shouldAwardClutchPerformer(boolean wonMatch, int suddenDeathHomeRuns) {
        return wonMatch && suddenDeathHomeRuns >= 1;
    }

    public static int milestoneProgressPercent(long current, long target) {
        if (target <= 0L) return 100;
        if (current <= 0L) return 0;
        if (current >= target) return 100;
        return (int) Math.min(100L, current * 100L / target);
    }
}
