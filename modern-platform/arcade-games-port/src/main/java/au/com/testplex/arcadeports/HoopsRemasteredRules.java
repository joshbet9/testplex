package au.com.testplex.arcadeports;

import java.util.Locale;

/** Deterministic scoring, timing and lobby rules for Hoops Remastered. */
public final class HoopsRemasteredRules {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 16;
    public static final int TARGET_SCORE = 21;
    public static final long REGULATION_MILLIS = 5 * 60_000L;
    public static final long SHOT_CLOCK_MILLIS = 24_000L;
    public static final long HOPS_COOLDOWN_MILLIS = 8_000L;
    public static final long OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS = 14_000L;
    public static final long ASSIST_WINDOW_MILLIS = 8_000L;
    public static final long CLUTCH_WINDOW_MILLIS = 60_000L;
    public static final long BUZZER_WINDOW_MILLIS = 3_000L;
    public static final double THREE_POINT_DISTANCE = 12.5D;
    public static final double STEAL_RADIUS = 2.6D;
    public static final double BLOCK_RADIUS = 2.5D;
    public static final long BLOCK_COOLDOWN_MILLIS = 700L;
    public static final double SHOT_AIM_TOLERANCE = 2.25D;
    public static final double LAYUP_DISTANCE = 6.0D;
    public static final double DUNK_DISTANCE = 4.5D;
    public static final int COMEBACK_SCORE_GAP = 10;
    public static final long STEAL_COOLDOWN_MILLIS = 3_000L;
    public static final double BALL_GRAVITY = 0.045D;
    public static final double BALL_WALL_BOUNCE = 0.45D;
    public static final double BALL_FLOOR_BOUNCE = 0.48D;
    public static final double BALL_FLOOR_BOUNCE_FRICTION = 0.82D;
    public static final double BALL_FLOOR_FRICTION = 0.75D;
    public static final double BALL_MOVEMENT_FRICTION = 0.65D;
    public static final double POSSESSION_MOVEMENT_DAMPING = 0.65D;
    public static final int BALL_MAX_BOUNCES = 7;
    public static final long BALL_LOOSE_LIMIT_MILLIS = 12_000L;
    public static final long BALL_RECLAIM_MILLIS = 15_000L;
    public static final double PASS_SPEED = 1.35D;
    public static final double PASS_VERTICAL_SPEED = 0.22D;
    public static final double PASS_FLIGHT_TICKS_PER_BLOCK = 1.45D;
    public static final int PASS_MIN_FLIGHT_TICKS = 4;
    public static final int PASS_MAX_FLIGHT_TICKS = 20;
    public static final double PASS_MIN_DISTANCE = 2.0D;
    public static final double PASS_MAX_DISTANCE = 24.0D;
    public static final double PASS_AIM_DOT = 0.45D;
    public static final double BLOCK_KNOCKBACK_HORIZONTAL = 0.6D;
    public static final double BLOCK_KNOCKBACK_VERTICAL = 0.35D;
    public static final double HOPS_HORIZONTAL = 0.48D;
    public static final double HOPS_VERTICAL = 0.78D;
    public static final double SHOT_DUNK_FLIGHT_TICKS_PER_BLOCK = 1.35D;
    public static final double SHOT_LAYUP_FLIGHT_TICKS_PER_BLOCK = 1.2D;
    public static final double SHOT_NORMAL_FLIGHT_TICKS_PER_BLOCK = 1.05D;
    public static final int SHOT_DUNK_MIN_FLIGHT_TICKS = 5;
    public static final int SHOT_DUNK_MAX_FLIGHT_TICKS = 8;
    public static final int SHOT_LAYUP_MIN_FLIGHT_TICKS = 7;
    public static final int SHOT_LAYUP_MAX_FLIGHT_TICKS = 11;
    public static final int SHOT_NORMAL_MIN_FLIGHT_TICKS = 8;
    public static final int SHOT_NORMAL_MAX_FLIGHT_TICKS = 22;
    public static final double BALL_CATCH_RADIUS_SQUARED = 2.25D;
    public static final double SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED = 2.25D;
    public static final double BLOCK_AIM_DOT = 0.25D;
    public static final double STEAL_AIM_DOT = 0.20D;

    private HoopsRemasteredRules() { }

    public static boolean supportsPlayerCount(int players) {
        return players >= GameDebugRuntimeOverrides.intValue("HOOPS_MIN_PLAYERS", MIN_PLAYERS)
                && players <= GameDebugRuntimeOverrides.intValue("HOOPS_MAX_PLAYERS", MAX_PLAYERS);
    }

    public static int scoreValue(double releaseDistance) {
        return Double.isFinite(releaseDistance) && releaseDistance >= GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_THREE_POINT_DISTANCE", THREE_POINT_DISTANCE) ? 3 : 2;
    }

    /** A team wins immediately when its score reaches or passes 21. */
    public static boolean isAimedAtHoop(boolean inFront, double missDistance) {
        return inFront && Double.isFinite(missDistance) && missDistance <= GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_SHOT_AIM_TOLERANCE", SHOT_AIM_TOLERANCE);
    }

    public static boolean isLayupEligible(boolean grounded, double hoopDistance, boolean aimedAtHoop) {
        return grounded && aimedAtHoop && Double.isFinite(hoopDistance)
                && hoopDistance <= GameDebugRuntimeOverrides.doubleValue("HOOPS_LAYUP_DISTANCE", LAYUP_DISTANCE);
    }

    public static boolean isDunkEligible(boolean airborne, boolean rising,
            double hoopDistance, boolean aimedAtHoop) {
        return airborne && rising && aimedAtHoop && Double.isFinite(hoopDistance)
                && hoopDistance <= GameDebugRuntimeOverrides.doubleValue("HOOPS_DUNK_DISTANCE", DUNK_DISTANCE);
    }

    public static final double RIM_SCORING_RADIUS = 1.15D;

    /**
     * Tests the actual segment/rim-plane intersection, not just the ball's
     * end-of-tick location. This prevents fast shots from skipping over the
     * scoring cylinder between server ticks.
     */
    public static boolean crossesHoopPlane(double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ, double hoopX, double hoopY, double radius) {
        double[] values = {fromX, fromY, fromZ, toX, toY, toZ, hoopX, hoopY, radius};
        for (double value : values) if (!Double.isFinite(value)) return false;
        if (radius <= 0.0D || fromY <= hoopY || toY > hoopY || fromY <= toY) return false;
        double fraction = (fromY - hoopY) / (fromY - toY);
        if (fraction < 0.0D || fraction > 1.0D) return false;
        double crossingX = fromX + (toX - fromX) * fraction;
        double crossingZ = fromZ + (toZ - fromZ) * fraction;
        double dx = crossingX - hoopX;
        return dx * dx + crossingZ * crossingZ <= radius * radius;
    }

    public static boolean hasReachedTargetScore(int red, int blue) {
        int targetScore = GameDebugRuntimeOverrides.intValue("HOOPS_TARGET_SCORE", TARGET_SCORE);
        return red >= targetScore || blue >= targetScore;
    }

    /** Regulation is tied when the five-minute buzzer sounds. */
    public static boolean isTiedAtTimeLimit(int red, int blue) {
        return red == blue;
    }

    /** A basket wins either by reaching 21 in regulation or by being scored in overtime. */
    public static boolean isWinningBasket(int red, int blue, boolean overtime) {
        return overtime || hasReachedTargetScore(red, blue);
    }

    public static int leadingTeam(int red, int blue) {
        if (red == blue) return -1;
        return red > blue ? 0 : 1;
    }

    public static boolean isBuzzerBeater(long remainingMillis) {
        return remainingMillis >= 0L && remainingMillis <= GameDebugRuntimeOverrides.longValue(
                "HOOPS_BUZZER_WINDOW_MILLIS", BUZZER_WINDOW_MILLIS);
    }

    public static boolean isClutchTime(long remainingMillis) {
        return remainingMillis >= 0L && remainingMillis <= GameDebugRuntimeOverrides.longValue(
                "HOOPS_CLUTCH_WINDOW_MILLIS", CLUTCH_WINDOW_MILLIS);
    }

    public static boolean isWithinBallCatchRadius(double distanceSquared) {
        double radiusSquared = GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_BALL_CATCH_RADIUS_SQUARED", BALL_CATCH_RADIUS_SQUARED);
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D && distanceSquared <= radiusSquared;
    }

    public static boolean isShooterInsideLaunchProtection(double distanceSquared) {
        double protectionSquared = GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED", SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED);
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D && distanceSquared < protectionSquared;
    }

    public static boolean isFacingBallForBlock(double facingDot) {
        return Double.isFinite(facingDot) && facingDot >= GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_BLOCK_AIM_DOT", BLOCK_AIM_DOT);
    }

    public static boolean isFacingHolderForSteal(double facingDot) {
        return Double.isFinite(facingDot) && facingDot >= GameDebugRuntimeOverrides.doubleValue(
                "HOOPS_STEAL_AIM_DOT", STEAL_AIM_DOT);
    }

    public static boolean isHopsReady(long now, long readyAt) {
        return now >= readyAt;
    }

    public static boolean isAssistEligible(long now, long lastPassAt) {
        return lastPassAt > 0L && now >= lastPassAt && now - lastPassAt <= GameDebugRuntimeOverrides.longValue(
                "HOOPS_ASSIST_WINDOW_MILLIS", ASSIST_WINDOW_MILLIS);
    }

    public static boolean isComebackEligible(int ownScore, int opponentScore) {
        return opponentScore - ownScore >= GameDebugRuntimeOverrides.intValue(
                "HOOPS_COMEBACK_SCORE_GAP", COMEBACK_SCORE_GAP);
    }

    public static boolean isDoubleDouble(int points, int assists, int rebounds, int blocks, int steals) {
        return categoriesAtLeastThree(points, assists, rebounds, blocks, steals) >= 2;
    }

    public static boolean isTripleDouble(int points, int assists, int rebounds, int blocks, int steals) {
        return categoriesAtLeastThree(points, assists, rebounds, blocks, steals) >= 3;
    }

    public static boolean isQuadrupleDouble(int points, int assists, int rebounds, int blocks, int steals) {
        return categoriesAtLeastThree(points, assists, rebounds, blocks, steals) >= 4;
    }

    public static boolean isQuintupleDouble(int points, int assists, int rebounds, int blocks, int steals) {
        return categoriesAtLeastThree(points, assists, rebounds, blocks, steals) == 5;
    }

    private static int categoriesAtLeastThree(int... values) {
        int count = 0;
        for (int value : values) if (value >= 3) count++;
        return count;
    }

    public static String formatClock(long remainingMillis) {
        long seconds = Math.max(0L, remainingMillis + 999L) / 1_000L;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    public static int teamForSpawnIndex(int index) {
        return Math.floorMod(index, 2);
    }

    public static double spawnX(int team) {
        return team == 0 ? -10.0D : 10.0D;
    }

    public static double spawnZ(int playerWithinTeam) {
        return -8.0D + Math.floorMod(playerWithinTeam, 8) * (16.0D / 7.0D);
    }
}
