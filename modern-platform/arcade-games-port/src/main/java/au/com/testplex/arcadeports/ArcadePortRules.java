package au.com.testplex.arcadeports;

/**
 * Small deterministic rules used by the mechanics-first Arcade prototypes.
 * Keeping timing and hazard classification here makes them testable without
 * needing a live Paper server.
 */
public final class ArcadePortRules {
    public static final int EVOLUTION_WIN_LEVEL = 6;
    public static final double DRAGON_ESCAPE_FINISH_X = 0.5D;
    public static final double DRAGON_ESCAPE_FINISH_Y = 64.0D;
    public static final double DRAGON_ESCAPE_FINISH_Z = 77.5D;
    public static final double DRAGON_ESCAPE_CHASE_SPEED = 0.2D;
    public static final double DRAGON_ESCAPE_BLOCK_RADIUS = 10.0D;
    public static final long RUNNER_BLOCK_DECAY_MILLIS = 600L;
    public static final long RUNNER_BLOCK_LIFETIME_MILLIS = 1_200L;
    public static final long BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS = 50_000L;
    public static final long BACON_MIN_KNOCKBACK_INTERVAL_MILLIS = 5_000L;
    public static final long BACON_HUNGER_INTERVAL_MILLIS = 1_000L;
    public static final int BACON_HUNGER_DRAIN_PER_INTERVAL = 2;
    public static final double BACON_STARVATION_DAMAGE = 4.0D;
    public static final double BACON_STANDARD_HIT_DAMAGE = 6.0D;
    public static final double BACON_BODY_SLAM_RECOIL_DAMAGE = 2.0D;
    public static final long BACON_BODY_SLAM_COOLDOWN_MILLIS = 8_000L;
    public static final long BACON_BLAST_COOLDOWN_MILLIS = 3_000L;
    public static final long BACON_CLOAK_COOLDOWN_MILLIS = 10_000L;
    public static final long BACON_CLOAK_DURATION_MILLIS = 5_000L;
    public static final long BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS = 3_000L;
    public static final long BOMB_LOBBERS_MAX_FLIGHT_MILLIS = 8_000L;
    public static final int BOMB_LOBBERS_FUSE_TICKS = 60;
    public static final int BOMB_LOBBERS_MAX_CARRIED = 3;
    public static final long TURF_WARS_BUILD_PHASE_MILLIS = 20_000L;
    public static final long TURF_WARS_COMBAT_PHASE_MILLIS = 90_000L;
    public static final long MICRO_BATTLES_CHEAT_WINDOW_MILLIS = 4_000L;
    public static final long MICRO_BATTLES_BARRIER_DURATION_MILLIS = 10_000L;

    private ArcadePortRules() {
    }

    public static boolean shouldRemoveMicroBarrier(long elapsedMillis) {
        return elapsedMillis >= MICRO_BATTLES_BARRIER_DURATION_MILLIS;
    }

    /** Only the kits retained from the selected source ruleset are exposed. */
    public static String normalizeBombLobbersKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "jumper" -> "jumper";
            case "armorer", "armourer" -> "armorer";
            case "pitcher" -> "pitcher";
            case "waller" -> "waller";
            default -> null;
        };
    }

    public static double bombLobbersPitchVelocity(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 1.75D;
            case 3 -> 2.25D;
            default -> 2.0D;
        };
    }

    public static long bombLobbersCraftmanIntervalMillis(long liveMillis) {
        if (liveMillis <= 20_000L) return 8_000L;
        if (liveMillis <= 40_000L) return 7_000L;
        if (liveMillis <= 60_000L) return 6_000L;
        if (liveMillis <= 620_000L) return 5_000L;
        return 4_000L;
    }

    public static boolean shouldPrimeBombLobbersBomb(long ageMillis, boolean touchingSolidBlock) {
        return ageMillis >= BOMB_LOBBERS_MAX_FLIGHT_MILLIS
                || (ageMillis >= BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS && touchingSolidBlock);
    }

    public static String normalizeBaconBrawlKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "pig" -> "pig";
            case "mama", "mama-pig" -> "mama-pig";
            case "sheep", "sheep-pig" -> "sheep-pig";
            default -> null;
        };
    }

    public static double initialBaconKnockback(int playerCount) {
        return playerCount == 2 ? 1.8D : 1.5D;
    }

    public static double nextBaconKnockback(double current) {
        return current * 1.2D;
    }

    public static boolean shouldDropRunnerBlock(long elapsedMillis) {
        return elapsedMillis >= RUNNER_BLOCK_DECAY_MILLIS;
    }

    /** Reborn only destabilises exposed, non-liquid solid blocks once per trail cycle. */
    public static boolean shouldMarkRunnerBlock(boolean solid, boolean bedrock, boolean liquid,
            boolean aboveIsAir, boolean alreadyMarked) {
        return solid && !bedrock && !liquid && aboveIsAir && !alreadyMarked;
    }

    /** Falling blocks only hurt a player when the player is not above the block. */
    public static boolean shouldRunnerFallingBlockHit(double playerY, double fallingBlockY) {
        return playerY <= fallingBlockY + 0.5D;
    }

    public static int spleefProjectileClearRadius(boolean arrow, double velocity) {
        if (!arrow) return 1;
        double safeVelocity = Math.max(0.0D, velocity);
        return Math.max(1, Math.min(3, (int) Math.ceil(0.5D + safeVelocity / 1.6D)));
    }

    public static final long EVOLUTION_RESPAWN_TICKS = 80L;
    public static final long EVOLUTION_FORM_CHANGE_TICKS = 44L;
    public static final long EVOLUTION_TOTAL_ANIMATION_TICKS = 140L;

    public static int nextEvolutionLevel(int currentLevel, boolean wonEvolve) {
        int safeLevel = Math.max(0, Math.min(EVOLUTION_WIN_LEVEL, currentLevel));
        return wonEvolve ? Math.min(EVOLUTION_WIN_LEVEL, safeLevel + 1) : safeLevel;
    }

    public static double evolutionMaxHealth(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 0 -> 40.0D; case 1 -> 36.0D; case 2 -> 32.0D;
            case 3 -> 28.0D; case 4 -> 22.0D; default -> 16.0D;
        };
    }

    public static double evolutionMeleeDamage(int level) {
        return switch (Math.max(0, Math.min(5, level))) {
            case 0 -> 6.0D; case 1, 2 -> 5.0D; case 3, 4 -> 4.0D; default -> 3.0D;
        };
    }

    public static double evolutionChargePerTick(int killsThisLife) {
        int extraKills = Math.max(0, killsThisLife - 1);
        return Math.min(0.022D, 0.012D + 0.003D * extraKills);
    }

    public static boolean isEvolutionWinnerLevel(int level) {
        return level >= EVOLUTION_WIN_LEVEL;
    }

    public static int sheepQuestLeadingTeam(int redSheep, int blueSheep) {
        if (redSheep > blueSheep) return 0;
        if (blueSheep > redSheep) return 1;
        return -1;
    }

    public static int sheepQuestLeadingTeam(int[] sheepCounts) {
        if (sheepCounts == null || sheepCounts.length == 0) return -1;
        int bestTeam = -1;
        int bestCount = Integer.MIN_VALUE;
        boolean tied = false;
        for (int team = 0; team < sheepCounts.length; team++) {
            if (sheepCounts[team] > bestCount) {
                bestCount = sheepCounts[team];
                bestTeam = team;
                tied = false;
            } else if (sheepCounts[team] == bestCount) {
                tied = true;
            }
        }
        return tied ? -1 : bestTeam;
    }

    public static boolean shouldEnterSheepQuestOvertime(int redSheep, int blueSheep) {
        return redSheep == blueSheep;
    }
    public static final double SHEEP_QUEST_PLAYER_HIT_DAMAGE = 4.0D;
    public static final int SHEEP_QUEST_INITIAL_SHEEP = 4;
    public static final int SHEEP_QUEST_MAX_CARRY = 3;
    public static final long SHEEP_QUEST_SPAWN_INTERVAL_MILLIS = 20_000L;

    public static boolean canCarryAnotherSheep(int carriedCount) {
        return carriedCount >= 0 && carriedCount < SHEEP_QUEST_MAX_CARRY;
    }


    public static int paintballPiecesAfterHit(int remainingPieces, int paintAmount) {
        return Math.max(0, Math.max(0, remainingPieces) - Math.max(0, paintAmount));
    }

    public static boolean isPaintballPlayerOut(int remainingPieces) {
        return remainingPieces <= 0;
    }

    public static double paintballHealthAfterHit(int remainingPieces) {
        return Math.max(2.0D, Math.max(0, remainingPieces) * 5.0D + 1.0D);
    }

    /** Mirrors Reborn's per-kit armour-paint counts and the Sniper scope damage gauge. */
    public static int paintballPaintAmount(String kit, boolean arrow, double scopeCharge) {
        String safeKit = kit == null ? "rifle" : kit.toLowerCase(java.util.Locale.ROOT);
        if ("rifle".equals(safeKit)) return 3;
        if ("sniper".equals(safeKit) && arrow) return paintballSniperPaintDamage(scopeCharge);
        return 1;
    }

    public static int paintballSniperPaintDamage(double scopeCharge) {
        double charge = Math.max(0.0D, Math.min(0.999D, scopeCharge));
        return 1 + (int) Math.floor(charge * 10.0D / 3.0D);
    }

    public static boolean isBombDirectHit(double distanceSquared) {
        return distanceSquared >= 0.0D && distanceSquared <= 1.0D;
    }

    public static final long GLADIATOR_ROUND_COUNTDOWN_MILLIS = 5_000L;
    public static final long GLADIATOR_POISON_DELAY_MILLIS = 60_000L;
    public static final long GLADIATOR_POISON_INTERVAL_MILLIS = 1_000L;
    public static final double GLADIATOR_POISON_DAMAGE = 1.0D;

    public static boolean canGladiatorsFight(long nowMillis, long fightStartsAt,
            int attackerPair, int targetPair, boolean pairResolved) {
        return nowMillis >= fightStartsAt && attackerPair >= 0
                && attackerPair == targetPair && !pairResolved;
    }

    public static boolean shouldGladiatorPoison(long nowMillis, long roundStartedAt,
            boolean pairResolved, int playersStillFighting) {
        return nowMillis >= roundStartedAt + GLADIATOR_POISON_DELAY_MILLIS
                && !pairResolved && playersStillFighting >= 2;
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
        return Math.max(0L, currentMillis - 5_000L);
    }

    public static boolean shouldPunishMicroIslandExit(long elapsedMillis, double ownDistanceSquared,
            double nearestOtherDistanceSquared) {
        return elapsedMillis >= 0L && elapsedMillis < MICRO_BATTLES_CHEAT_WINDOW_MILLIS
                && nearestOtherDistanceSquared < ownDistanceSquared;
    }

    public static double microBattlesHitDamage(boolean projectile) {
        return projectile ? 4.0D : 6.0D;
    }

    public static int microBattlesMinimumFoodLevel(int foodLevel) {
        return Math.max(2, Math.min(20, foodLevel));
    }

    public static boolean isWorldDragonHazard(ArcadePortGame game, boolean trackedEntity, boolean playerOwned) {
        return game == ArcadePortGame.DRAGONS && trackedEntity && !playerOwned;
    }

    public static boolean shouldApplyDragonContactHit(long nowMillis, long nextAllowedMillis) {
        return nowMillis >= nextAllowedMillis;
    }

 
    /** The source game finishes near its final map waypoint, not anywhere on a Z plane. */
    public static boolean isDragonEscapeFinishReached(double x, double y, double z) {
        double dx = x - DRAGON_ESCAPE_FINISH_X;
        double dy = y - DRAGON_ESCAPE_FINISH_Y;
        double dz = z - DRAGON_ESCAPE_FINISH_Z;
        return dx * dx + dy * dy + dz * dz < 9.0D;
    }

    /** Reborn kills runners once Douglas's route progress has overtaken theirs. */
    public static boolean shouldDragonEscapePlayerBeCaught(double dragonProgress, double playerProgress) {
        return dragonProgress > playerProgress;
    }

    public static final long QUIVER_RESPAWN_DELAY_TICKS = 20L;

    public static boolean isQuiverWinnerScoreReached(int kills) {
        return kills >= 20;
    }

    public static boolean shouldFinishWhenNobodyIsAlive(int alivePlayers, int respawningPlayers) {
        return alivePlayers <= 0 && respawningPlayers <= 0;
    }

    public static int requiredDeathTagChasers(long elapsedMillis) {
        return elapsedMillis < 0L ? 1 : 1 + (int) (elapsedMillis / 30_000L);
    }

    public static boolean shouldSpreadDeathTag(boolean attackerTagged, boolean targetTagged) {
        return attackerTagged && !targetTagged;
    }

    public static boolean deathTagHitIsLethal(double currentHealth, double hitDamage) {
        return hitDamage > 0.0D && currentHealth <= hitDamage;
    }

    public static double deathTagChaserDamage(boolean alphaChaser) {
        return alphaChaser ? 6.0D : 4.0D;
    }

    public static double deathTagRunnerDamage(String runnerKit, boolean projectile) {
        String kit = runnerKit == null ? "basher" : runnerKit.toLowerCase(java.util.Locale.ROOT);
        if ("null".equals(kit)) return 0.0D;
        if (projectile) return "archer".equals(kit) ? 4.0D : 0.0D;
        return "basher".equals(kit) || "traitor".equals(kit) ? 6.0D : 0.0D;
    }

    /** Iron Skin in Reborn subtracts a flat 2 or 4 damage from attacks greater than 1. */
    public static double deathTagDamageAfterIronSkin(double damage, boolean alphaChaser) {
        if (damage <= 1.0D) return damage;
        return Math.max(0.0D, damage - (alphaChaser ? 4.0D : 2.0D));
    }

    public static double deathTagKnockbackMultiplier(boolean alphaChaser) {
        return alphaChaser ? 0.5D : 1.0D;
    }

    /** Reborn's runner speed threshold is strictly greater than 25% chasers. */
    public static boolean deathTagRunnersReceiveSpeed(int chasers, int totalPlayers) {
        return totalPlayers > 0 && chasers >= 0 && (double) chasers / (double) totalPlayers > 0.25D;
    }

    public static boolean shouldFinishDeathTagRound(int runnersRemaining, int initialPlayers) {
        return initialPlayers > 2 ? runnersRemaining <= 1 : runnersRemaining <= 0;
    }

    public static double turfWarsHitDamage(boolean projectile, boolean ironSword) {
        if (projectile) return 9.0D;
        return ironSword ? 12.0D : 6.0D;
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
