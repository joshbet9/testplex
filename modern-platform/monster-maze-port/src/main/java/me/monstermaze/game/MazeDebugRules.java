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
}
