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
        double minSpeed = WitherAssaultRules.flightMinSpeed();
        double maxSpeed = Math.max(minSpeed, WitherAssaultRules.flightMaxSpeed());
        double hoverSpeed = clamp(WitherAssaultRules.flightSpawnHoverSpeed(), minSpeed, maxSpeed);
        double altitudeSpeed = clamp(WitherAssaultRules.flightBaseSpeed()
                        + (y - yLimit) * WitherAssaultRules.flightSpeedPerYBlock(),
                minSpeed, maxSpeed);
        double distance = Math.abs(y - spawnY);
        double blendStart = WitherAssaultRules.flightBlendStartBlocks();
        double blendEnd = WitherAssaultRules.flightBlendEndBlocks();
        double linearBlend = clamp((distance - blendStart)
                / (blendEnd - blendStart), 0.0D, 1.0D);
        double easedBlend = linearBlend * linearBlend * (3.0D - 2.0D * linearBlend);
        return clamp(hoverSpeed + (altitudeSpeed - hoverSpeed) * easedBlend, minSpeed, maxSpeed);
    }

    /** Move toward the target without a one-tick fly-speed jump. */
    public static double approachSpeed(double currentSpeed, double targetSpeed) {
        if (!Double.isFinite(currentSpeed)) currentSpeed = SPAWN_HOVER_SPEED;
        if (!Double.isFinite(targetSpeed)) targetSpeed = SPAWN_HOVER_SPEED;
        double minSpeed = WitherAssaultRules.flightMinSpeed();
        double maxSpeed = Math.max(minSpeed, WitherAssaultRules.flightMaxSpeed());
        double current = clamp(currentSpeed, minSpeed, maxSpeed);
        double target = clamp(targetSpeed, minSpeed, maxSpeed);
        double maxChange = WitherAssaultRules.flightMaxSpeedChangePerTick();
        double delta = clamp(target - current,
                -maxChange, maxChange);
        return clamp(current + delta, minSpeed, maxSpeed);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
