package au.com.testplex.arcadeports;

import java.util.Set;

/** Small, deterministic predicates for Runner's kit achievements. */
final class RunnerAchievementRules {
    static final double LONG_LEAPER_DISTANCE_BLOCKS = 50.0D;
    static final double DEADEYE_RANGE_BLOCKS = 25.0D;
    static final int COLD_FRONT_TARGETS = 7;

    private RunnerAchievementRules() { }

    static boolean longLeaper(double leapDistance) {
        return Double.isFinite(leapDistance) && leapDistance >= LONG_LEAPER_DISTANCE_BLOCKS;
    }

    static boolean coldFront(Set<?> distinctTargets) {
        return distinctTargets != null && distinctTargets.size() >= COLD_FRONT_TARGETS;
    }

    static boolean switcheroo(boolean finalOneVersusOne) {
        return finalOneVersusOne;
    }

    static boolean deadeye(boolean finalOpponentEliminated, boolean arrowMarkedFloorCausedFall,
                           double shotDistance) {
        return finalOpponentEliminated && arrowMarkedFloorCausedFall
                && Double.isFinite(shotDistance) && shotDistance >= DEADEYE_RANGE_BLOCKS;
    }
}
