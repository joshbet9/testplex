package me.monstermaze.game;

import au.com.testplex.arcadeports.GameDebugRuntimeOverrides;

/** Profile-backed values for the existing in-process Monster Maze game engine. */
public final class MazeDebugRules {
    public static final float MOB_MOVE_SPEED = 0.8F;
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

    public static double doubleValue(MazeMode mode, String field, double fallback) {
        return GameDebugRuntimeOverrides.doubleValue(key(mode, field), fallback);
    }
}
