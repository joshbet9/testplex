package au.com.testplex.arcadeports;

/** Keeps minion waypoint handoff and movement tolerance aligned with the source Moba AI. */
final class HeroesOfGwenMinionMovementRules {
    private static final double WAYPOINT_REACHED_DISTANCE_SQUARED = 4.0D;

    private HeroesOfGwenMinionMovementRules() { }

    static boolean shouldAdvanceWaypoint(double distanceSquared) {
        return distanceSquared < WAYPOINT_REACHED_DISTANCE_SQUARED;
    }

    static boolean shouldMoveTowardWaypoint(double distanceSquared) {
        return distanceSquared >= WAYPOINT_REACHED_DISTANCE_SQUARED;
    }
}
