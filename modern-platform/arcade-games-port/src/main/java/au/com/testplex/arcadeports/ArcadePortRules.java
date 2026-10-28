package au.com.testplex.arcadeports;

/**
 * Small deterministic rules used by the mechanics-first Arcade prototypes.
 * Keeping timing and hazard classification here makes them testable without
 * needing a live Paper server.
 */
public final class ArcadePortRules {
    public static final int EVOLUTION_WIN_LEVEL = 6;
    public static final double DRAGON_ESCAPE_FINISH_Z = 77.0D;
    public static final long DRAGON_ESCAPE_HAZARD_INTERVAL_MILLIS = 6_000L;
    public static final long RUNNER_BLOCK_DECAY_MILLIS = 600L;
    public static final long RUNNER_BLOCK_LIFETIME_MILLIS = 1_200L;
    public static final long BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS = 50_000L;
    public static final long BACON_MIN_KNOCKBACK_INTERVAL_MILLIS = 5_000L;
    public static final long TURF_WARS_BUILD_PHASE_MILLIS = 20_000L;
    public static final long TURF_WARS_COMBAT_PHASE_MILLIS = 90_000L;
    public static final long MICRO_BATTLES_CHEAT_WINDOW_MILLIS = 4_000L;
    public static final long MICRO_BATTLES_BARRIER_DURATION_MILLIS = 10_000L;

    private ArcadePortRules() {
    }

    public static boolean shouldRemoveMicroBarrier(long elapsedMillis) {
        return elapsedMillis >= MICRO_BATTLES_BARRIER_DURATION_MILLIS;
    }

    public static double nextBaconKnockback(double current) {
        return current * 1.2D;
    }

    public static boolean shouldDropRunnerBlock(long elapsedMillis) {
        return elapsedMillis >= RUNNER_BLOCK_DECAY_MILLIS;
    }

    public static int spleefProjectileClearRadius(boolean arrow, double velocity) {
        if (!arrow) return 1;
        double safeVelocity = Math.max(0.0D, velocity);
        return Math.max(1, Math.min(3, (int) Math.ceil(0.5D + safeVelocity / 1.6D)));
    }

    public static int nextEvolutionLevel(int currentLevel, boolean wonDuel) {
        int safeLevel = Math.max(0, Math.min(EVOLUTION_WIN_LEVEL, currentLevel));
        return wonDuel ? Math.min(EVOLUTION_WIN_LEVEL, safeLevel + 1) : safeLevel;
    }

    public static boolean isEvolutionWinnerLevel(int level) {
        return level >= EVOLUTION_WIN_LEVEL;
    }

    public static int sheepQuestLeadingTeam(int redSheep, int blueSheep) {
        if (redSheep > blueSheep) return 0;
        if (blueSheep > redSheep) return 1;
        return -1;
    }

    public static int paintballPiecesAfterHit(int remainingPieces, int paintAmount) {
        return Math.max(0, Math.max(0, remainingPieces) - Math.max(0, paintAmount));
    }

    public static boolean isPaintballPlayerOut(int remainingPieces) {
        return remainingPieces <= 0;
    }

    public static int gladiatorsTierForRound(int round) {
        return Math.max(0, Math.min(3, round));
    }

    public static int gladiatorsPairCount(int players) {
        return Math.max(0, (Math.max(0, players) + 1) / 2);
    }

    public static boolean allGladiatorPairsResolved(int pairCount, int resolvedPairs) {
        return pairCount > 0 && resolvedPairs >= pairCount;
    }

    public static long nextBaconKnockbackInterval(long currentMillis) {
        return Math.max(BACON_MIN_KNOCKBACK_INTERVAL_MILLIS, currentMillis - 5_000L);
    }

    public static boolean shouldPunishMicroIslandExit(long elapsedMillis, double ownDistanceSquared,
            double nearestOtherDistanceSquared) {
        return elapsedMillis >= 0L && elapsedMillis < MICRO_BATTLES_CHEAT_WINDOW_MILLIS
                && nearestOtherDistanceSquared < ownDistanceSquared;
    }

    public static boolean isWorldDragonHazard(ArcadePortGame game, boolean trackedEntity, boolean playerOwned) {
        return game == ArcadePortGame.DRAGONS && trackedEntity && !playerOwned;
    }

    public static boolean shouldApplyDragonContactHit(long nowMillis, long nextAllowedMillis) {
        return nowMillis >= nextAllowedMillis;
    }

    public static boolean shouldLaunchDragonEscapeHazard(long nowMillis, long nextLaunchMillis) {
        return nowMillis >= nextLaunchMillis;
    }

    public static boolean isDragonEscapeFinishReached(double z) {
        return z >= DRAGON_ESCAPE_FINISH_Z;
    }

    public static boolean isQuiverWinnerScoreReached(int kills) {
        return kills >= 20;
    }

    public static boolean shouldFinishWhenNobodyIsAlive(int alivePlayers, int respawningPlayers) {
        return alivePlayers <= 0 && respawningPlayers <= 0;
    }

    public static int requiredDeathTagChasers(long elapsedMillis) {
        return elapsedMillis < 0L ? 1 : 1 + (int) (elapsedMillis / 30_000L);
    }

    public static int advanceTurfFrontline(int currentFrontline, int teamId) {
        if (teamId == 0) return currentFrontline + 1;
        if (teamId == 1) return currentFrontline - 1;
        return currentFrontline;
    }

    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int halfSize) {
        if (halfSize < 1) return false;
        if (teamId == 0) return frontline >= halfSize - 1;
        if (teamId == 1) return frontline <= -halfSize;
        return false;
    }
}
