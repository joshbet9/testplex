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
    public static final int MAX_PLAYERS = 16;
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
    public static final int BALL_MAX_FLIGHT_TICKS = 85;
    public static final double BALL_RADIUS = 0.24D;
    public static final double BATTING_POWER_MULTIPLIER = 1.0D;
    public static final double CURVE_ACCELERATION = 0.0035D;
    public static final double CURVE_ARTIST_SIDE_SPEED = 0.25D;
    public static final double SLUGGER_DIVE_SPEED = 0.62D;
    public static final double SPRINTER_BURST_SPEED = 0.78D;
    public static final double SKYGUARD_LEAP_SPEED = 0.48D;
    public static final int BASE_CATCH_WINDOW_TICKS = 8;
    public static final double BASE_CATCH_RADIUS = 1.25D;
    public static final int INTRO_TICKS = 40;
    public static final int TURN_BREAK_TICKS = 12;
    public static final int TIMING_CYCLE_MIN_TICKS = 30;
    public static final int TIMING_CYCLE_MAX_TICKS = 42;
    public static final int TECHNICIAN_TIMING_BONUS_TICKS = 2;
    public static final double BATTING_BASE_HORIZONTAL_SPEED = 3.0D;
    public static final double BATTING_CONTACT_HORIZONTAL_SCALE = 1.1D;
    public static final double BATTING_BASE_VERTICAL_SPEED = 0.47D;
    public static final double BATTING_CONTACT_VERTICAL_SCALE = 0.25D;
    public static final double TIMING_POWER_BASE = 0.65D;
    public static final double TIMING_POWER_SCALE = 0.35D;
    public static final double SLUGGER_BATTING_POWER = 1.07D;
    public static final double TECHNICIAN_BATTING_POWER = 1.00D;
    public static final double CURVE_ARTIST_BATTING_POWER = 0.97D;
    public static final double SPRINTER_BATTING_POWER = 1.00D;
    public static final double SKYGUARD_BATTING_POWER = 0.99D;
    public static final int TIMING_PERFECT_MAX_TICKS = 2;
    public static final int TIMING_GREAT_MAX_TICKS = 4;
    public static final int TIMING_GOOD_MAX_TICKS = 6;
    public static final int TIMING_FAIR_MAX_TICKS = 9;
    public static final int TIMING_WEAK_MAX_TICKS = 13;
    public static final double TIMING_PERFECT_POWER = 1.00D;
    public static final double TIMING_GREAT_POWER = 0.94D;
    public static final double TIMING_GOOD_POWER = 0.84D;
    public static final double TIMING_FAIR_POWER = 0.70D;
    public static final double TIMING_WEAK_POWER = 0.54D;
    public static final double TIMING_POOR_POWER = 0.38D;

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
                && currentSpectators < GameDebugRuntimeOverrides.intValue("PLEXBALL_MAX_SPECTATORS", MAX_SPECTATORS);
    }

    public static boolean hasValidPlayerCount(int count) {
        return count >= GameDebugRuntimeOverrides.intValue("PLEXBALL_MIN_PLAYERS", MIN_PLAYERS)
                && count <= GameDebugRuntimeOverrides.intValue("PLEXBALL_MAX_PLAYERS", MAX_PLAYERS);
    }

    public static int latencyRewindTicks(int pingEstimateMillis) {
        if (pingEstimateMillis <= 0) return 0;
        return Math.max(0, Math.min(GameDebugRuntimeOverrides.intValue("PLEXBALL_MAX_REWIND_TICKS", MAX_REWIND_TICKS),
                (int) Math.round(pingEstimateMillis / 100.0D)));
    }

    public static int ballMaxFlightTicks() {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(
                "PLEXBALL_BALL_MAX_FLIGHT_TICKS", BALL_MAX_FLIGHT_TICKS));
    }

    public static double ballRadius() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_BALL_RADIUS", BALL_RADIUS);
    }

    public static double battingPower(double kitPower) {
        return kitPower * GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_BATTING_POWER_MULTIPLIER", BATTING_POWER_MULTIPLIER);
    }

    public static int introTicks() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue("PLEXBALL_INTRO_TICKS", INTRO_TICKS));
    }

    public static int turnBreakTicks() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue("PLEXBALL_TURN_BREAK_TICKS", TURN_BREAK_TICKS));
    }

    public static int timingCycleMinTicks() {
        return Math.max(4, GameDebugRuntimeOverrides.intValue(
                "PLEXBALL_TIMING_CYCLE_MIN_TICKS", TIMING_CYCLE_MIN_TICKS));
    }

    public static int timingCycleMaxTicks() {
        return Math.max(timingCycleMinTicks(), GameDebugRuntimeOverrides.intValue(
                "PLEXBALL_TIMING_CYCLE_MAX_TICKS", TIMING_CYCLE_MAX_TICKS));
    }

    public static int technicianTimingBonusTicks() {
        return Math.max(0, GameDebugRuntimeOverrides.intValue(
                "PLEXBALL_TECHNICIAN_TIMING_BONUS_TICKS", TECHNICIAN_TIMING_BONUS_TICKS));
    }

    public static double kitBattingPower(String kit, double sourceDefault) {
        return switch (kit == null ? "" : kit.toLowerCase(java.util.Locale.ROOT)) {
            case "slugger" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SLUGGER_BATTING_POWER", sourceDefault);
            case "technician" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TECHNICIAN_BATTING_POWER", sourceDefault);
            case "curve-artist" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_CURVE_ARTIST_BATTING_POWER", sourceDefault);
            case "sprinter" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SPRINTER_BATTING_POWER", sourceDefault);
            case "skyguard" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SKYGUARD_BATTING_POWER", sourceDefault);
            default -> sourceDefault;
        };
    }

    public static double battingBaseHorizontalSpeed() {
        return GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_BATTING_BASE_HORIZONTAL_SPEED", BATTING_BASE_HORIZONTAL_SPEED);
    }

    public static double battingContactHorizontalScale() {
        return GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_BATTING_CONTACT_HORIZONTAL_SCALE", BATTING_CONTACT_HORIZONTAL_SCALE);
    }

    public static double battingBaseVerticalSpeed() {
        return GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_BATTING_BASE_VERTICAL_SPEED", BATTING_BASE_VERTICAL_SPEED);
    }

    public static double battingContactVerticalScale() {
        return GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_BATTING_CONTACT_VERTICAL_SCALE", BATTING_CONTACT_VERTICAL_SCALE);
    }

    public static double timingPowerBase() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_POWER_BASE", TIMING_POWER_BASE);
    }

    public static double timingPowerScale() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_POWER_SCALE", TIMING_POWER_SCALE);
    }

    public static double curveAcceleration() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_CURVE_ACCELERATION", CURVE_ACCELERATION);
    }

    public static double curveArtistSideSpeed() {
        return GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_CURVE_ARTIST_SIDE_SPEED", CURVE_ARTIST_SIDE_SPEED);
    }

    public static double sluggerDiveSpeed() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SLUGGER_DIVE_SPEED", SLUGGER_DIVE_SPEED);
    }

    public static double sprinterBurstSpeed() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SPRINTER_BURST_SPEED", SPRINTER_BURST_SPEED);
    }

    public static double skyguardLeapSpeed() {
        return GameDebugRuntimeOverrides.doubleValue("PLEXBALL_SKYGUARD_LEAP_SPEED", SKYGUARD_LEAP_SPEED);
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
        if (distanceTicks <= timingThreshold("PERFECT", TIMING_PERFECT_MAX_TICKS)) return timingPower("PERFECT", TIMING_PERFECT_POWER);
        if (distanceTicks <= timingThreshold("GREAT", TIMING_GREAT_MAX_TICKS)) return timingPower("GREAT", TIMING_GREAT_POWER);
        if (distanceTicks <= timingThreshold("GOOD", TIMING_GOOD_MAX_TICKS)) return timingPower("GOOD", TIMING_GOOD_POWER);
        if (distanceTicks <= timingThreshold("FAIR", TIMING_FAIR_MAX_TICKS)) return timingPower("FAIR", TIMING_FAIR_POWER);
        if (distanceTicks <= timingThreshold("WEAK", TIMING_WEAK_MAX_TICKS)) return timingPower("WEAK", TIMING_WEAK_POWER);
        return timingPower("POOR", TIMING_POOR_POWER);
    }

    private static int timingThreshold(String tier, int sourceDefault) {
        int value = switch (tier) {
            case "PERFECT" -> GameDebugRuntimeOverrides.intValue("PLEXBALL_TIMING_PERFECT_MAX_TICKS", sourceDefault);
            case "GREAT" -> GameDebugRuntimeOverrides.intValue("PLEXBALL_TIMING_GREAT_MAX_TICKS", sourceDefault);
            case "GOOD" -> GameDebugRuntimeOverrides.intValue("PLEXBALL_TIMING_GOOD_MAX_TICKS", sourceDefault);
            case "FAIR" -> GameDebugRuntimeOverrides.intValue("PLEXBALL_TIMING_FAIR_MAX_TICKS", sourceDefault);
            case "WEAK" -> GameDebugRuntimeOverrides.intValue("PLEXBALL_TIMING_WEAK_MAX_TICKS", sourceDefault);
            default -> sourceDefault;
        };
        return Math.max(0, value);
    }

    private static double timingPower(String tier, double sourceDefault) {
        double value = switch (tier) {
            case "PERFECT" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_PERFECT_POWER", sourceDefault);
            case "GREAT" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_GREAT_POWER", sourceDefault);
            case "GOOD" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_GOOD_POWER", sourceDefault);
            case "FAIR" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_FAIR_POWER", sourceDefault);
            case "WEAK" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_WEAK_POWER", sourceDefault);
            case "POOR" -> GameDebugRuntimeOverrides.doubleValue("PLEXBALL_TIMING_POOR_POWER", sourceDefault);
            default -> sourceDefault;
        };
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    /** Fair cone is centred on the field's -Z direction (Minecraft yaw 180). */
    public static boolean isFairYaw(double yaw) {
        double delta = (yaw - 180.0D) % 360.0D;
        if (delta > 180.0D) delta -= 360.0D;
        if (delta < -180.0D) delta += 360.0D;
        return Math.abs(delta) <= GameDebugRuntimeOverrides.doubleValue("PLEXBALL_FAIR_ANGLE_DEGREES", FAIR_ANGLE_DEGREES);
    }

    public static double fieldHalfWidthAtZ(double z) {
        return Math.max(2.0D, -z * GameDebugRuntimeOverrides.doubleValue(
                "PLEXBALL_FIELD_HALF_WIDTH_PER_DEPTH", FIELD_HALF_WIDTH_PER_DEPTH) + 2.0D);
    }

    public static boolean isInsideField(double x, double z) {
        return z <= 0.0D && z >= -GameDebugRuntimeOverrides.doubleValue("PLEXBALL_HOME_RUN_METRES", HOME_RUN_METRES)
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
