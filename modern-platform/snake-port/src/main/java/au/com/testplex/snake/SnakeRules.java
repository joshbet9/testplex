package au.com.testplex.snake;

/** Small deterministic rules shared by the Snake loop and its contract tests. */
public final class SnakeRules {
    private static final int[] ROULETTE_SECONDS = {1, 3, 5, 10, 15, 20, 30};

    public static final int MAX_HAZARDS = 4;
    public static final long FIRST_HAZARD_DELAY_MILLIS = 10_000L;
    public static final long HAZARD_INTERVAL_MILLIS = 15_000L;
    public static final int HAZARD_STEERING_INTERVAL_TICKS = 4;
    public static final double HAZARD_STEERING_SPEED = 0.12D;
    public static final double HAZARD_CONTACT_RADIUS = 2.1D;

    /** Legacy Snake leaves an item box available if the player is still rolling an item. */
    public static boolean shouldConsumeHazardOnContact(boolean rouletteActive) {
        return !rouletteActive;
    }

    private SnakeRules() {}

    public static int rouletteDurationSecondsForHit(int hitCount) {
        if (hitCount <= 1) return ROULETTE_SECONDS[0];
        return ROULETTE_SECONDS[Math.min(hitCount, ROULETTE_SECONDS.length) - 1];
    }

    public static double movementSpeed(int tailLength, boolean speedBoost) {
        double growth = Math.min(0.16D, Math.max(0, tailLength) / 1800.0D);
        double speed = 0.18D + growth;
        return speedBoost ? speed * 1.5D : speed;
    }

    public static double clampTurnRadians(double requestedRadians) {
        double max = Math.toRadians(8.0D);
        return Math.max(-max, Math.min(max, requestedRadians));
    }

    public static float wrapYaw(float angle) {
        while (angle <= -180.0F) angle += 360.0F;
        while (angle > 180.0F) angle -= 360.0F;
        return angle;
    }

    public static double wrapRadians(double angle) {
        while (angle <= -Math.PI) angle += 2.0D * Math.PI;
        while (angle > Math.PI) angle -= 2.0D * Math.PI;
        return angle;
    }
}
