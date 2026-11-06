package me.monstermaze.game;

import au.com.testplex.arcadeports.GameDebugRuntimeOverrides;
import java.util.UUID;

/** Profile-backed values for the existing in-process Monster Maze game engine. */
public final class MazeDebugRules {
    public static final float MOB_MOVE_SPEED = 0.8F;
    public static final int STAGE_TIMER_INITIAL_SECONDS = 60;
    public static final double STAGE_TIMER_DECREASE_PER_STAGE_SECONDS = 2.0D;
    public static final int STAGE_TIMER_MIN_SECONDS = 15;
    public static final int INITIAL_MONSTER_COUNT = 150;
    public static final int ADDITIONAL_MONSTERS_PER_SAFE_PAD = 15;
    public static final double MONSTER_HEALTH = 4.0D;
    public static final long PLAYER_BUMP_COOLDOWN_MILLIS = 1_000L;
    public static final double BUMP_RADIUS_BLOCKS = 1.0D;
    public static final double BUMP_DAMAGE = 4.0D;
    public static final double BUMP_KNOCKBACK_STRENGTH = 1.0D;
    public static final double BUMP_KNOCKBACK_Y_ADD = 0.75D;
    public static final double BUMP_KNOCKBACK_Y_MAX = 1.2D;
    public static final double BODY_RUSH_STRENGTH = 1.0D;
    public static final double BODY_RUSH_Y_ADD = 0.8D;
    public static final double BODY_RUSH_Y_MAX = 2.0D;
    public static final int SAFE_PAD_DECAY_STAGES = 11;
    public static final double SAFE_PAD_CAPTURE_RADIUS_BLOCKS = 2.0D;
    public static final long BODY_RUSH_DURATION_MILLIS = 10_000L;
    public static final int CRYO_COOLDOWN_MILLIS = 30_000;
    public static final long CRYO_FREEZE_MILLIS = 3_000L;
    public static final int CRYO_RADIUS_BLOCKS = 6;
    public static final double JUMP_STRENGTH = 0.42D;
    public static final int JUMPER_MAX_JUMPS = 3;
    public static final long JUMPER_JUMP_COOLDOWN_MILLIS = 750L;
    public static final int SLOWBALL_MAX_SNOWBALLS = 16;
    public static final long SLOWBALL_REGEN_INTERVAL_MILLIS = 2_000L;
    public static final int SLOWBALL_SLOW_TICKS = 40;
    public static final int SLOWBALL_SLOW_AMPLIFIER = 1;
    public static final double BODY_BUILDER_HEALTH_PER_PAD = 2.0D;
    public static final double BODY_BUILDER_MAX_HEALTH = 30.0D;
    public static final int BODY_RUSH_ACTIVATION_CHARGES = 2;
    public static final long BODY_RUSH_DURATION_REDUCTION_MILLIS = 2_000L;
    public static final int BODY_RUSH_ACHIEVEMENT_CONTACTS = 10;
    public static final double REPULSOR_RADIUS_BLOCKS = 6.0D;
    public static final double REPULSOR_LAUNCH_STRENGTH = 1.0D;
    public static final double REPULSOR_LAUNCH_Y_ADD = 0.8D;
    public static final double REPULSOR_LAUNCH_Y_MAX = 2.0D;
    public static final long REPULSOR_GROUND_CLEANUP_DELAY_MILLIS = 500L;
    public static final long REPULSOR_MAX_LIFETIME_MILLIS = 1_500L;

    private MazeDebugRules() { }

    public static String key(MazeMode mode, String field) {
        String prefix = switch (mode == null ? MazeMode.ORIGINAL : mode) {
            case ORIGINAL -> "MONSTER_MAZE_";
            case SPEED -> "MONSTER_MAZE_SPEED_";
            case MODERN -> "MONSTER_MAZE_MODERN_";
            case CLASSIC -> "MONSTER_MAZE_MODERN_";
        };
        return prefix + field;
    }

    public static float floatValue(MazeMode mode, String field, float fallback) {
        return GameDebugRuntimeOverrides.floatValue(key(mode, field), fallback);
    }

    public static long longValue(MazeMode mode, String field, long fallback) {
        return GameDebugRuntimeOverrides.longValue(key(mode, field), fallback);
    }

    public static int intValue(MazeMode mode, String field, int fallback) {
        return GameDebugRuntimeOverrides.intValue(key(mode, field), fallback);
    }

    public static boolean kitEnabled(UUID player, String kit) {
        return GameDebugRuntimeOverrides.booleanValueFor(player, "kit-" + kit, true);
    }

    public static double doubleValue(MazeMode mode, String field, double fallback) {
        return GameDebugRuntimeOverrides.doubleValue(key(mode, field), fallback);
    }

    public static boolean isBumpHitWithinRange(MazeMode mode, double horizontalDistanceSquared,
            double distanceSquared) {
        double radius = Math.max(0.1D, doubleValue(mode, "BUMP_RADIUS_BLOCKS", BUMP_RADIUS_BLOCKS));
        double radiusSquared = radius * radius;
        return Double.isFinite(horizontalDistanceSquared) && Double.isFinite(distanceSquared)
                && horizontalDistanceSquared >= 0.0D && distanceSquared >= 0.0D
                && horizontalDistanceSquared < radiusSquared && distanceSquared < radiusSquared;
    }

    public static boolean isBumpCooldownReady(MazeMode mode, long nowMillis, long lastBumpMillis) {
        long cooldown = Math.max(0L, longValue(mode, "PLAYER_BUMP_COOLDOWN_MILLIS", PLAYER_BUMP_COOLDOWN_MILLIS));
        return nowMillis >= 0L && (lastBumpMillis < 0L || nowMillis >= lastBumpMillis
                && nowMillis - lastBumpMillis >= cooldown);
    }

    public static double bumpDamage(MazeMode mode) {
        return Math.max(0.0D, doubleValue(mode, "BUMP_DAMAGE", BUMP_DAMAGE));
    }

    public static int jumperMaxJumps(MazeMode mode) {
        int defaultJumps = mode == null || mode == MazeMode.ORIGINAL ? 5 : JUMPER_MAX_JUMPS;
        return Math.max(0, intValue(mode, "JUMPER_MAX_JUMPS", defaultJumps));
    }

    public static long jumperCooldownMillis(MazeMode mode) {
        return Math.max(0L, longValue(mode, "JUMPER_JUMP_COOLDOWN_MILLIS", JUMPER_JUMP_COOLDOWN_MILLIS));
    }

    public static int slowballMaxSnowballs(MazeMode mode) {
        return Math.max(1, intValue(mode, "SLOWBALL_MAX_SNOWBALLS", SLOWBALL_MAX_SNOWBALLS));
    }

    public static long slowballRegenIntervalMillis(MazeMode mode) {
        return Math.max(1L, longValue(mode, "SLOWBALL_REGEN_INTERVAL_MILLIS", SLOWBALL_REGEN_INTERVAL_MILLIS));
    }

    public static int slowballSlowTicks(MazeMode mode) {
        return Math.max(0, intValue(mode, "SLOWBALL_SLOW_TICKS", SLOWBALL_SLOW_TICKS));
    }

    public static int slowballSlowAmplifier(MazeMode mode) {
        return Math.max(0, intValue(mode, "SLOWBALL_SLOW_AMPLIFIER", SLOWBALL_SLOW_AMPLIFIER));
    }

    public static double bodyBuilderHealthPerPad(MazeMode mode) {
        return Math.max(0.0D, doubleValue(mode, "BODY_BUILDER_HEALTH_PER_PAD", BODY_BUILDER_HEALTH_PER_PAD));
    }

    public static double bodyBuilderMaxHealth(MazeMode mode) {
        return Math.max(20.0D, doubleValue(mode, "BODY_BUILDER_MAX_HEALTH", BODY_BUILDER_MAX_HEALTH));
    }

    public static double bodyBuilderMaxHealthAfterPad(MazeMode mode, double currentMaxHealth) {
        if (!Double.isFinite(currentMaxHealth)) return bodyBuilderMaxHealth(mode);
        double cap = bodyBuilderMaxHealth(mode);
        return currentMaxHealth >= cap ? currentMaxHealth
                : Math.min(cap, Math.max(20.0D, currentMaxHealth) + bodyBuilderHealthPerPad(mode));
    }

    public static int bodyRushActivationCharges(MazeMode mode) {
        return Math.max(1, intValue(mode, "BODY_RUSH_ACTIVATION_CHARGES", BODY_RUSH_ACTIVATION_CHARGES));
    }

    public static long bodyRushDurationReductionMillis(MazeMode mode) {
        return Math.max(0L, longValue(mode, "BODY_RUSH_DURATION_REDUCTION_MILLIS",
                BODY_RUSH_DURATION_REDUCTION_MILLIS));
    }

    public static int bodyRushAchievementContacts(MazeMode mode) {
        return Math.max(1, intValue(mode, "BODY_RUSH_ACHIEVEMENT_CONTACTS", BODY_RUSH_ACHIEVEMENT_CONTACTS));
    }

    public static boolean shouldAwardBodyRushAchievement(MazeMode mode, int contacts) {
        return contacts >= bodyRushAchievementContacts(mode);
    }

    public static long bodyRushUntilAfterContact(MazeMode mode, long activeUntilMillis) {
        long reduction = bodyRushDurationReductionMillis(mode);
        return activeUntilMillis < Long.MIN_VALUE + reduction ? Long.MIN_VALUE : activeUntilMillis - reduction;
    }

    public static double repulsorRadiusBlocks(MazeMode mode) {
        return Math.max(0.1D, doubleValue(mode, "REPULSOR_RADIUS_BLOCKS", REPULSOR_RADIUS_BLOCKS));
    }

    public static double repulsorLaunchStrength(MazeMode mode) {
        return Math.max(0.0D, doubleValue(mode, "REPULSOR_LAUNCH_STRENGTH", REPULSOR_LAUNCH_STRENGTH));
    }

    public static double repulsorLaunchYAdd(MazeMode mode) {
        return Math.max(0.0D, doubleValue(mode, "REPULSOR_LAUNCH_Y_ADD", REPULSOR_LAUNCH_Y_ADD));
    }

    public static double repulsorLaunchYMax(MazeMode mode) {
        return Math.max(0.0D, doubleValue(mode, "REPULSOR_LAUNCH_Y_MAX", REPULSOR_LAUNCH_Y_MAX));
    }

    public static long repulsorGroundCleanupDelayMillis(MazeMode mode) {
        return Math.max(0L, longValue(mode, "REPULSOR_GROUND_CLEANUP_DELAY_MILLIS",
                REPULSOR_GROUND_CLEANUP_DELAY_MILLIS));
    }

    public static long repulsorMaxLifetimeMillis(MazeMode mode) {
        return Math.max(1L, longValue(mode, "REPULSOR_MAX_LIFETIME_MILLIS", REPULSOR_MAX_LIFETIME_MILLIS));
    }
}
