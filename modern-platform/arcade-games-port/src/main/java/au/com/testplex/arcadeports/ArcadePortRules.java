package au.com.testplex.arcadeports;

/**
 * Small deterministic rules used by the mechanics-first Arcade prototypes.
 * Keeping timing and hazard classification here makes them testable without
 * needing a live Paper server.
 */
public final class ArcadePortRules {
    private static final int MICRO_BATTLES_BORDER_START_TICK = 600; // 30 seconds
    private static final int MICRO_BATTLES_BORDER_INTERVAL_TICKS = 400; // 20 seconds
    private static final double MICRO_BATTLES_BORDER_STEP = 6.0D;
    private static final double MICRO_BATTLES_BORDER_MINIMUM = 10.0D;

    private ArcadePortRules() {
    }

    public static boolean shouldShrinkMicroBattlesBorder(int elapsedTicks) {
        return elapsedTicks >= MICRO_BATTLES_BORDER_START_TICK
                && (elapsedTicks - MICRO_BATTLES_BORDER_START_TICK) % MICRO_BATTLES_BORDER_INTERVAL_TICKS == 0;
    }

    public static double nextMicroBattlesBorderSize(double currentSize) {
        return Math.max(MICRO_BATTLES_BORDER_MINIMUM, currentSize - MICRO_BATTLES_BORDER_STEP);
    }

    public static boolean isWorldDragonHazard(ArcadePortGame game, boolean trackedEntity, boolean playerOwned) {
        return game == ArcadePortGame.DRAGONS && trackedEntity && !playerOwned;
    }

    public static boolean isQuiverWinnerScoreReached(int kills) {
        return kills >= 20;
    }
}
