package au.com.testplex.arcadeports;

import org.bukkit.Material;

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
    public static final long RUNNER_LEAP_COOLDOWN_MILLIS = 8_000L;
    public static final long RUNNER_QUICKSHOT_COOLDOWN_MILLIS = 6_000L;
    public static final long RUNNER_FROSTBALL_INTERVAL_MILLIS = 500L;
    public static final int RUNNER_MAX_FROSTBALLS = 16;
    public static final int RUNNER_FROSTBALL_SLOW_TICKS = 40;
    public static final int RUNNER_FROSTBALL_SLOW_AMPLIFIER = 2;
    public static final long BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS = 50_000L;
    public static final long BACON_MIN_KNOCKBACK_INTERVAL_MILLIS = 5_000L;
    public static final long BACON_HUNGER_INTERVAL_MILLIS = 1_000L;
    public static final int BACON_HUNGER_DRAIN_PER_INTERVAL = 1;
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
    public static final long SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS = 4_000L;
    public static final int SPLEEF_HUNGER_DRAIN_PER_INTERVAL = 2;
    public static final long SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS = 50L;
    public static final long SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS = 2_000L;
    public static final int SPLEEF_ARCHER_MAX_ARROWS = 2;
    public static final int SPLEEF_SNOWBALLER_MAX_SNOWBALLS = 16;
    public static final long SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS = 6_000L;
    public static final long SPLEEF_SMASHER_COOLDOWN_MILLIS = 250L;
    public static final long TURF_WARS_INITIAL_BUILD_PHASE_MILLIS = 40_000L;
    public static final long TURF_WARS_BUILD_PHASE_MILLIS = 20_000L;
    public static final long TURF_WARS_COMBAT_PHASE_MILLIS = 90_000L;
    public static final long MICRO_BATTLES_CHEAT_WINDOW_MILLIS = 4_000L;
    public static final long MICRO_BATTLES_BARRIER_DURATION_MILLIS = 10_000L;
    public static final int MICRO_BATTLES_CRUMBLE_BLOCKS_PER_TICK = 3;
    public static final int MICRO_BATTLES_CRUMBLE_SCAN_INTERVAL_TICKS = 2;
    public static final int DRAGONS_MAX_DRAGONS = 5;
    public static final long DRAGONS_SPAWN_INTERVAL_MILLIS = 500L;
    public static final long DRAGONS_CONTACT_COOLDOWN_MILLIS = 2_000L;
    public static final long DRAGONS_SPARKLER_INTERVAL_MILLIS = 25_000L;
    public static final int DRAGONS_MAX_SPARKLERS = 2;
    public static final int DRAGONS_SPARKLER_LIFETIME_TICKS = 100;
    public static final double DRAGONS_SPARKLER_RANGE_SQUARED = 48.0D * 48.0D;
    public static final int DRAGONS_MARKSMAN_MAX_ARROWS = 4;
    public static final long DRAGONS_MARKSMAN_ARROW_INTERVAL_MILLIS = 4_000L;
    public static final int DRAGONS_BARRAGE_MAX_ARROWS = 6;
    public static final long DRAGONS_BARRAGE_INTERVAL_MILLIS = 200L;
    public static final long DRAGONS_BARRAGE_INITIAL_CHARGE_MILLIS = 1_000L;
    public static final long DRAGONS_COWARD_LEAP_COOLDOWN_MILLIS = 8_000L;
    public static final long MICRO_BATTLES_FLETCHER_INTERVAL_MILLIS = 20_000L;
    public static final int MICRO_BATTLES_FLETCHER_MAX_ARROWS = 3;

    private ArcadePortRules() {
    }

    public static String normalizeDragonsKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("dragons-")) kit = kit.substring("dragons-".length());
        return switch (kit) {
            case "coward" -> "coward";
            case "marksman", "markman" -> "marksman";
            case "pyrotechnic", "pyro" -> "pyrotechnic";
            default -> null;
        };
    }

    public static boolean shouldSpawnTestDragon(int currentCount, long now, long nextSpawnAt) {
        return currentCount < DRAGONS_MAX_DRAGONS && now >= nextSpawnAt;
    }

    public static boolean shouldReplenishDragonSparkler(long now, long nextSpawnAt, int currentCount) {
        return now >= nextSpawnAt && currentCount < DRAGONS_MAX_SPARKLERS;
    }

    public static boolean shouldExpireDragonSparkler(int livedTicks, double y, boolean inLiquid) {
        return livedTicks > DRAGONS_SPARKLER_LIFETIME_TICKS || y < 4.0D || inLiquid;
    }

    public static double dragonFallDamageAfterReduction(double damage) {
        return Math.max(0.0D, damage - 1.0D);
    }

    public static String normalizeMicroBattlesKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (kit) {
            case "archer" -> "archer";
            case "worker" -> "worker";
            case "fighter" -> "fighter";
            default -> null;
        };
    }

    public static double microBattlesDamageAfterIronSkin(double damage, boolean fighter) {
        if (!fighter || damage <= 1.0D) return damage;
        return Math.max(0.0D, damage * 0.5D);
    }

    public static boolean shouldReplenishMicroArrow(long nowMillis, long nextArrowAt,
            int namedArrows, int maximum) {
        return nowMillis >= nextArrowAt && namedArrows < Math.max(0, maximum);
    }

    public static boolean shouldRemoveMicroBarrier(long elapsedMillis) {
        return elapsedMillis >= MICRO_BATTLES_BARRIER_DURATION_MILLIS;
    }

    /** Only the kits retained from the selected source ruleset are exposed. */
    public static String normalizeSpleefKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "snowballer" -> "snowballer";
            case "brawler" -> "brawler";
            case "archer" -> "archer";
            default -> null;
        };
    }

    /** Legacy Spleef wool/clay data transitions: green -> yellow -> red -> broken. */
    public static int nextSpleefSlowDamageData(int legacyData) {
        return switch (legacyData) {
            case 5, 13 -> 4;
            case 4 -> 14;
            default -> -1;
        };
    }

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

    /** Legacy 1.8 armour points absorb 4% of incoming damage per point, up to 20 points. */
    public static double bombLobbersDamageAfterArmor(double rawDamage, double armorPoints) {
        if (!Double.isFinite(rawDamage) || rawDamage <= 0.0D) return 0.0D;
        double armor = Double.isFinite(armorPoints) ? Math.max(0.0D, Math.min(20.0D, armorPoints)) : 0.0D;
        return rawDamage * (1.0D - armor / 25.0D);
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

    public static String normalizeRunnerKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("runnerkit-")) kit = kit.substring("runnerkit-".length());
        else if (kit.startsWith("runner-")) kit = kit.substring("runner-".length());
        return switch (kit) {
            case "leaper" -> "leaper";
            case "archer" -> "archer";
            case "frosty" -> "frosty";
            default -> null;
        };
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

    /** Match the team-index order used by SourceMapCatalog: red, blue, green, yellow. */
    public static String sheepQuestTeamPenMarker(int team) {
        return switch (team) {
            case 0 -> "RED";
            case 1 -> "BLUE";
            case 2 -> "GREEN";
            case 3 -> "YELLOW";
            default -> "UNASSIGNED";
        };
    }

    /** WorldConfig pen markers describe the floor; the sheep's feet block is one block above it. */
    public static boolean sheepQuestInsidePenMarker(int sheepBlockX, int sheepFeetY, int sheepBlockZ,
            int markerX, int markerFloorY, int markerZ) {
        return sheepBlockX == markerX && sheepFeetY == markerFloorY + 1 && sheepBlockZ == markerZ;
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


    public static final long PAINTBALL_WATER_BOMB_COOLDOWN_MILLIS = 250L;
    public static final double PAINTBALL_SAVE_ME_RADIUS = 3.0D;

    public static boolean shouldPaintballWaterBombAffectPlayer(double distanceSquared, boolean sameTeam) {
        return sameTeam && distanceSquared >= 0.0D
                && distanceSquared <= PAINTBALL_SAVE_ME_RADIUS * PAINTBALL_SAVE_ME_RADIUS;
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

    /** Test a player against a world border whose centre may not be at world origin. */
    public static boolean isOutsideWorldBorder(double playerX, double playerZ,
            double centerX, double centerZ, double borderSize) {
        if (!Double.isFinite(playerX) || !Double.isFinite(playerZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(borderSize) || borderSize <= 0.0D) return true;
        double halfSize = borderSize / 2.0D;
        return Math.abs(playerX - centerX) > halfSize || Math.abs(playerZ - centerZ) > halfSize;
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

 
    /** Legacy Dragon Escape runs its Hell course at night; other courses keep the normal day start. */
    /**
     * Preserve the original per-game world-water damage settings. Dragon Escape
     * only enables this penalty on its Pirate course.
     */
    public static int sourceWorldWaterDamage(ArcadePortGame game, String mapName) {
        if (game == null) return 0;
        return switch (game) {
            case BOMB_LOBBERS -> 5;
            case DRAGONS, RUNNER, SPLEEF -> 4;
            case DRAGON_ESCAPE -> mapName != null
                    && mapName.toLowerCase(java.util.Locale.ROOT).contains("pirate") ? 2 : 0;
            default -> 0;
        };
    }

    /** Map/game-specific time overrides; all unconfigured source games inherit Game.WorldTimeSet=12000. */
    public static long sourceWorldTime(ArcadePortGame game, String mapName) {
        if (game == null) return 12_000L;
        return switch (game) {
            case BOMB_LOBBERS -> mapName != null && mapName.equalsIgnoreCase("Intergalactic")
                    ? 18_000L : 6_000L;
            case SHEEP_QUEST -> 2_000L;
            case DRAGON_ESCAPE -> mapName != null
                    && mapName.toLowerCase(java.util.Locale.ROOT).contains("hell") ? 16_000L : 12_000L;
            default -> 12_000L;
        };
    }

    public static long dragonEscapeWorldTime(String mapName) {
        return sourceWorldTime(ArcadePortGame.DRAGON_ESCAPE, mapName);
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
    public static final long QUIVER_GROUND_POUND_COOLDOWN_MILLIS = 10_000L;
    public static final long QUIVER_NINJA_VANISH_MILLIS = 1_200L;
    public static final double QUIVER_REBOUND_MAX_DISTANCE_SQUARED = 1.0D;

    public static String normalizeQuiverKit(String requested) {
        if (requested == null) return null;
        String kit = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (kit.startsWith("quiver-")) kit = kit.substring("quiver-".length());
        return switch (kit) {
            case "leaper" -> "leaper";
            case "brawler" -> "brawler";
            case "enchanter" -> "enchanter";
            case "slam-shot", "slamshot", "slam-shooter" -> "slam-shot";
            case "ninja" -> "ninja";
            default -> null;
        };
    }

    /** One in the Quiver refunds its single arrow only when the hit secures a kill. */
    public static boolean quiverRegrantsArrowAfterHit(boolean targetEliminated) {
        return targetEliminated;
    }

    /** Reborn's Ground Pound scales horizontal knockback from 0 at range to 0.8 at point-blank. */
    public static double quiverGroundPoundHorizontalMultiplier(double distance) {
        if (!Double.isFinite(distance)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, (8.0D - distance) / 8.0D));
    }

    public static double quiverMeleeDamage(String kit) {
        String value = normalizeQuiverKit(kit);
        if (value == null) value = "leaper";
        return switch (value) {
            case "brawler" -> 7.0D;
            case "slam-shot" -> 5.5D;
            case "ninja" -> 4.0D;
            default -> 5.0D;
        };
    }

    public static boolean shouldRemoveQuiverInvisibilityOnHit(boolean melee, boolean ninja) {
        return melee && ninja;
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

    /** Original Death Tag lights switch between gold blocks and white wool each second. */
    public static Material deathTagNextLightMaterial(Material current) {
        return current == Material.GOLD_BLOCK ? Material.WHITE_WOOL : Material.GOLD_BLOCK;
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

    /**
     * Reborn scales the starting Gladiators arenas to the lobby size: fewer players
     * start farther inward (stronger tier); larger lobbies use outer tiers first.
     * Tier index is GREEN=0, YELLOW=1, ORANGE=2, RED=3.
     */
    public static int gladiatorsStartingTierForPlayers(int players) {
        if (players <= 2) return 3;
        if (players <= 4) return 2;
        if (players <= 8) return 1;
        return 0;
    }

    /** Original Gladiators opens with green outer arenas, then moves inward by tier. */
    public static String gladiatorsArenaMarkerGroup(int round) {
        return switch (Math.max(0, Math.min(3, round))) {
            case 1 -> "YELLOW";
            case 2 -> "ORANGE";
            case 3 -> "RED";
            default -> "GREEN";
        };
    }

    /** Initial line sits immediately to the left of the X-axis midpoint (red territory). */
    public static int turfWarsInitialFrontline(int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return -1;
        return (int) Math.floor((minX + (double) maxXExclusive) / 2.0D) - 1;
    }

    /** Source-map bounds are exclusive at the maximum coordinate. */
    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return false;
        if (teamId == 0) return frontline >= maxXExclusive - 1;
        if (teamId == 1) return frontline <= minX;
        return false;
    }

    /** Compare territory against the source map's starting line before applying a kill tiebreak. */
    public static int turfWarsTimeoutLeader(int frontline, int initialFrontline, int redKills, int blueKills) {
        if (frontline > initialFrontline) return 0;
        if (frontline < initialFrontline) return 1;
        if (redKills != blueKills) return redKills > blueKills ? 0 : 1;
        return -1;
    }

    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int halfSize) {
        if (halfSize < 1) return false;
        if (teamId == 0) return frontline >= halfSize - 1;
        if (teamId == 1) return frontline <= -halfSize;
        return false;
    }

    /**
     * Select a team winner for a timed elimination-game test. Remaining players
     * are the primary standing; aggregate kills break ties. A perfect tie has
     * no objective winner and may fall back to the game's normal score rule.
     */
    public static int teamWithBestTimeoutStanding(int teamZeroAlive, int teamOneAlive,
            int teamZeroKills, int teamOneKills) {
        if (teamZeroAlive != teamOneAlive) return teamZeroAlive > teamOneAlive ? 0 : 1;
        if (teamZeroKills != teamOneKills) return teamZeroKills > teamOneKills ? 0 : 1;
        return -1;
    }

    /**
     * Turf Wars is won by the captured frontline, not by an individual kill
     * count. Only when the line is unchanged do team kills provide a tiebreak.
     */
    public static int turfWarsTimeoutLeader(int frontline, int redKills, int blueKills) {
        if (frontline > -1) return 0;
        if (frontline < -1) return 1;
        if (redKills != blueKills) return redKills > blueKills ? 0 : 1;
        return -1;
    }
}
