package au.com.testplex.arcadeports;

/**
 * Smooth altitude-to-flight-speed curve for Wither Assault.
 *
 * The original mode used a hard four-block spawn-height cutoff and then a steep
 * linear speed formula. This keeps the original curve away from the spawn while
 * easing into it near the launch point and rate-limiting each live update.
 */
public final class WitherAssaultMovementRules {
    public static final double MIN_FLY_SPEED = 0.01D;
    public static final double SPAWN_HOVER_SPEED = 0.016D;
    public static final double MAX_FLY_SPEED = 1.0D;
    public static final double BASE_FLY_SPEED = 0.09D;
    public static final double SPEED_PER_Y_BLOCK = 0.006D;
    public static final double BLEND_START_BLOCKS = 2.0D;
    public static final double BLEND_END_BLOCKS = 10.0D;
    public static final double MAX_SPEED_CHANGE_PER_TICK = 0.0035D;

    private WitherAssaultMovementRules() { }

    /**
     * Calculate the desired speed at a Y level. The spawn-speed plateau blends
     * into the altitude curve using smoothstep, so both value and gradient stay
     * continuous at the beginning and end of the transition.
     */
    public static double targetFlySpeed(double y, double spawnY, double yLimit) {
        if (!Double.isFinite(y) || !Double.isFinite(spawnY) || !Double.isFinite(yLimit)) {
            return SPAWN_HOVER_SPEED;
        }
        double altitudeSpeed = clamp(
                BASE_FLY_SPEED + (y - yLimit) * SPEED_PER_Y_BLOCK,
                MIN_FLY_SPEED, MAX_FLY_SPEED);
        double distance = Math.abs(y - spawnY);
        double linearBlend = clamp((distance - BLEND_START_BLOCKS)
                / (BLEND_END_BLOCKS - BLEND_START_BLOCKS), 0.0D, 1.0D);
        double easedBlend = linearBlend * linearBlend * (3.0D - 2.0D * linearBlend);
        return clamp(SPAWN_HOVER_SPEED
                + (altitudeSpeed - SPAWN_HOVER_SPEED) * easedBlend,
                MIN_FLY_SPEED, MAX_FLY_SPEED);
    }

    /** Move toward the target without a one-tick fly-speed jump. */
    public static double approachSpeed(double currentSpeed, double targetSpeed) {
        if (!Double.isFinite(currentSpeed)) currentSpeed = SPAWN_HOVER_SPEED;
        if (!Double.isFinite(targetSpeed)) targetSpeed = SPAWN_HOVER_SPEED;
        double current = clamp(currentSpeed, MIN_FLY_SPEED, MAX_FLY_SPEED);
        double target = clamp(targetSpeed, MIN_FLY_SPEED, MAX_FLY_SPEED);
        double delta = clamp(target - current,
                -MAX_SPEED_CHANGE_PER_TICK, MAX_SPEED_CHANGE_PER_TICK);
        return clamp(current + delta, MIN_FLY_SPEED, MAX_FLY_SPEED);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
