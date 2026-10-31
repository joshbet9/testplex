package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;

import java.util.Set;

/**
 * Deterministic Plexball rules shared by the live game and unit tests.
 *
 * Gameplay outcomes are server-owned. Paper's ping estimate is used only as
 * a bounded compensation hint; it is not a network RTT measurement or an
 * anti-cheat signal.
 */
public final class PlexballRules {
    public static final int MAX_PLAYERS = 8;
    public static final int MAX_SPECTATORS = 16;
    public static final int MIN_PLAYERS = 2;
    public static final int MATCH_LIMIT_TICKS = 20 * 180;
    public static final int NORMAL_BATTING_TICKS = 20 * 10;
    public static final int SUDDEN_DEATH_BATTING_TICKS = 20 * 5;
    public static final int MAX_REWIND_TICKS = 5;
    public static final int PLAYER_HISTORY_TICKS = 16;
    public static final double HOME_RUN_METRES = 100.0D;
    public static final double FIELD_HALF_WIDTH_PER_DEPTH = 0.65D;
    public static final double FAIR_ANGLE_DEGREES = 34.0D;
    public static final double BALL_GRAVITY_PER_TICK = 0.030D;
    public static final int BASE_CATCH_WINDOW_TICKS = 8;
    public static final double BASE_CATCH_RADIUS = 1.25D;

    private PlexballRules() { }

    /**
     * Convert Paper's coarse ping estimate into a cautious bounded rewind.
     * This estimate is not guaranteed to equal network RTT. It only offsets
     * ordinary input delay and must never be used to judge or punish a player.
     */
    /** Spectators are admitted only during an active match, outside the player roster. */
    public static boolean canJoinAsSpectator(boolean matchActive, boolean alreadyPlaying,
                                             int currentSpectators) {
        return matchActive && !alreadyPlaying && currentSpectators >= 0
                && currentSpectators < MAX_SPECTATORS;
    }

    public static boolean hasValidPlayerCount(int count) {
        return count >= MIN_PLAYERS && count <= MAX_PLAYERS;
    }

    public static int latencyRewindTicks(int pingEstimateMillis) {
        if (pingEstimateMillis <= 0) return 0;
        return Math.max(0, Math.min(MAX_REWIND_TICKS,
                (int) Math.round(pingEstimateMillis / 100.0D)));
    }

    /**
     * Catch input may only be evaluated against ball positions already recorded
     * by the server. A player event can run before the current tick's ball update.
     */
    public static long catchEvaluationThroughTick(long serverTick, long latestBallHistoryTick) {
        return Math.min(serverTick, latestBallHistoryTick);
    }

    /** Distance, in ticks, from the centre of the current randomly phased beat. */
    public static int timingDistanceTicks(long inputTick, long startTick, int cycleTicks, int phaseOffset) {
        if (cycleTicks < 4) throw new IllegalArgumentException("Timing cycle must be at least four ticks.");
        int phase = Math.floorMod((int) (inputTick - startTick - phaseOffset), cycleTicks);
        int centre = cycleTicks / 2;
        int direct = Math.abs(phase - centre);
        return Math.min(direct, cycleTicks - direct);
    }

    /**
     * A broad, graded timing curve. The perfect band is deliberately wider than
     * a single tick, then the server-side rewind removes most ordinary RTT bias.
     */
    public static double timingMultiplier(int distanceTicks) {
        if (distanceTicks <= 2) return 1.00D;
        if (distanceTicks <= 4) return 0.94D;
        if (distanceTicks <= 6) return 0.84D;
        if (distanceTicks <= 9) return 0.70D;
        if (distanceTicks <= 13) return 0.54D;
        return 0.38D;
    }

    /** Fair cone is centred on the field's -Z direction (Minecraft yaw 180). */
    public static boolean isFairYaw(double yaw) {
        double delta = (yaw - 180.0D) % 360.0D;
        if (delta > 180.0D) delta -= 360.0D;
        if (delta < -180.0D) delta += 360.0D;
        return Math.abs(delta) <= FAIR_ANGLE_DEGREES;
    }

    public static double fieldHalfWidthAtZ(double z) {
        return Math.max(2.0D, -z * FIELD_HALF_WIDTH_PER_DEPTH + 2.0D);
    }

    public static boolean isInsideField(double x, double z) {
        return z <= 0.0D && z >= -HOME_RUN_METRES
                && Math.abs(x) <= fieldHalfWidthAtZ(z);
    }

    public static double horizontalDistance(Vector first, Vector second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Squared distance from a point to a swept ball segment. */
    public static double distanceSquaredToSegment(Vector point, Vector start, Vector end) {
        Vector segment = end.clone().subtract(start);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 1.0E-9D) return point.distanceSquared(start);
        double t = point.clone().subtract(start).dot(segment) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vector closest = start.clone().add(segment.multiply(t));
        return point.distanceSquared(closest);
    }

    /**
     * A sudden-death round is complete only after every finalist has had a
     * recorded turn. A timeout during a partial round must not eliminate
     * finalists who have not yet batted.
     */
    public static <T> boolean hasCompletedRound(Set<T> finalists, Set<T> completedTurns) {
        return finalists != null && !finalists.isEmpty()
                && completedTurns != null && completedTurns.containsAll(finalists);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
