package au.com.testplex.arcadeports;

/**
 * Fast, free-for-all rules for the dedicated Mixed Arcade SkyWars variant.
 * These values intentionally do not affect the legacy Solo or Team modes.
 */
public final class SkyWarsMixedArcadeRules {
    private SkyWarsMixedArcadeRules() {}

    public static final int MAX_PLAYERS = 16;
    public static final long PREPARE_MILLIS = 5_000L;
    public static final long ROUND_MILLIS = 8 * 60_000L;
    public static final long CRUMBLE_DELAY_MILLIS = 60_000L;
    public static final long BORDER_SHRINK_MILLIS = 45_000L;
    public static final double FINAL_BORDER_SIZE = 32.0D;
    public static final long TNT_GENERATOR_INTERVAL_MILLIS = 15_000L;
    public static final int TNT_PER_DROP = 2;
    public static final long TNT_THROW_FUSE_TICKS = 40L;

    public static boolean hasRoom(int queuedPlayers) {
        return queuedPlayers < MAX_PLAYERS;
    }

    public static int acceptedPlayers(int requestedPlayers) {
        return Math.max(0, Math.min(MAX_PLAYERS, requestedPlayers));
    }

    public static long roundEndsAt(long startMillis) {
        return startMillis + PREPARE_MILLIS + ROUND_MILLIS;
    }

    public static long liveStartsAt(long startMillis) {
        return startMillis + PREPARE_MILLIS;
    }

    public static long crumbleStartsAt(long startMillis) {
        return liveStartsAt(startMillis) + CRUMBLE_DELAY_MILLIS;
    }

    public static long firstTntAt(long startMillis) {
        return liveStartsAt(startMillis) + TNT_GENERATOR_INTERVAL_MILLIS;
    }
}
