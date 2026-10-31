package au.com.testplex.arcadeports;

import java.util.Locale;

/** Deterministic scoring, timing and lobby rules for Hoops Remastered. */
public final class HoopsRemasteredRules {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 16;
    public static final int TARGET_SCORE = 21;
    public static final int WIN_BY = 2;
    public static final long REGULATION_MILLIS = 5 * 60_000L;
    public static final long SHOT_CLOCK_MILLIS = 24_000L;
    public static final long HOPS_COOLDOWN_MILLIS = 8_000L;
    public static final long OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS = 14_000L;
    public static final long ASSIST_WINDOW_MILLIS = 8_000L;
    public static final long CLUTCH_WINDOW_MILLIS = 60_000L;
    public static final long BUZZER_WINDOW_MILLIS = 3_000L;
    public static final double THREE_POINT_DISTANCE = 12.5D;
    public static final double STEAL_RADIUS = 2.6D;
    public static final double BLOCK_RADIUS = 2.2D;

    private HoopsRemasteredRules() { }

    public static boolean supportsPlayerCount(int players) {
        return players >= MIN_PLAYERS && players <= MAX_PLAYERS;
    }

    public static int scoreValue(double releaseDistance) {
        return Double.isFinite(releaseDistance) && releaseDistance >= THREE_POINT_DISTANCE ? 3 : 2;
    }

    public static boolean isWinningScore(int red, int blue, boolean overtime) {
        if (red < 0 || blue < 0) return false;
        if (overtime) return red != blue;
        return Math.max(red, blue) >= TARGET_SCORE && Math.abs(red - blue) >= WIN_BY;
    }

    public static int leadingTeam(int red, int blue) {
        if (red == blue) return -1;
        return red > blue ? 0 : 1;
    }

    public static boolean isBuzzerBeater(long remainingMillis) {
        return remainingMillis >= 0L && remainingMillis <= BUZZER_WINDOW_MILLIS;
    }

    public static boolean isClutchTime(long remainingMillis) {
        return remainingMillis >= 0L && remainingMillis <= CLUTCH_WINDOW_MILLIS;
    }

    public static boolean isHopsReady(long now, long readyAt) {
        return now >= readyAt;
    }

    public static boolean isAssistEligible(long now, long lastPassAt) {
        return lastPassAt > 0L && now >= lastPassAt && now - lastPassAt <= ASSIST_WINDOW_MILLIS;
    }

    public static boolean isComebackEligible(int ownScore, int opponentScore) {
        return opponentScore - ownScore >= 10;
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
