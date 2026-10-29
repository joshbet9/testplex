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
    /** Only discard effectively coincident markers; close BLACK markers are valid route steps. */
    public static final double DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE = 0.75D;
    public static final long RUNNER_BLOCK_DECAY_MILLIS = 600L;
    public static final long RUNNER_BLOCK_LIFETIME_MILLIS = 1_200L;
    public static final long RUNNER_LEAP_COOLDOWN_MILLIS = 8_000L;
    public static final long RUNNER_QUICKSHOT_COOLDOWN_MILLIS = 6_000L;
    public static final long RUNNER_FROSTBALL_INTERVAL_MILLIS = 500L;
    public static final int RUNNER_MAX_FROSTBALLS = 16;
    public static final int RUNNER_FROSTBALL_SLOW_TICKS = 40;
    public static final int RUNNER_FROSTBALL_SLOW_AMPLIFIER = 2;
    /** Selected community balance value for the King of Bacon win achievement. */
    public static final int BACON_BRAWL_KING_OF_BACON_WINS = 15;
    /** Pigs In Blankets is earned by six kills in a single game. */
    public static final int BACON_BRAWL_PIGS_IN_BLANKETS_KILLS = 6;

    public static boolean shouldAwardBaconPigsInBlankets(int killsThisGame) {
        return killsThisGame >= BACON_BRAWL_PIGS_IN_BLANKETS_KILLS;
    }

    public static boolean shouldAwardBaconKingOfBacon(int wins) {
        return wins >= BACON_BRAWL_KING_OF_BACON_WINS;
    }

    /** Bacon Blast calls the source explosion with block damage disabled. */
    public static boolean baconBlastShouldBreakBlock(org.bukkit.Material material, double distanceSquared) {
        return false;
    }

    /** Recent hit credit keeps knock-off kills attached to the attacker who caused the fall. */
    public static final long BACON_FALL_KILL_CREDIT_WINDOW_MILLIS = 5_000L;

    /** Hazards that end a Bacon Brawl knock-off on the embedded arena, including its lava floor. */
    public static boolean isBaconArenaKnockoutReason(String reason) {
        if (reason == null) return false;
        String value = reason.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').trim();
        return value.equals("fell from the arena")
                || value.equals("was lost to the water")
                || value.equals("lava")
                || value.equals("fire")
                || value.equals("hot floor")
                || value.equals("magma block")
                || value.equals("drowning")
                || value.equals("void");
    }

    public static boolean shouldCreditBaconFallKill(long nowMillis, long lastHitAtMillis,
            boolean isArenaFallOrWaterKnockout) {
        return isArenaFallOrWaterKnockout && lastHitAtMillis > 0L
                && nowMillis >= lastHitAtMillis
                && nowMillis - lastHitAtMillis <= BACON_FALL_KILL_CREDIT_WINDOW_MILLIS;
    }

    /** Original Bacon Brawl is decided by kills at the two-minute mark. */
    public static final long BACON_BRAWL_ROUND_MILLIS = 120_000L;
    public static final long BACON_BRAWL_RESPAWN_DELAY_TICKS = 20L;
    public static final long BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS = 50_000L;
    public static final long BACON_MIN_KNOCKBACK_INTERVAL_MILLIS = 5_000L;
    public static final long BACON_SLOW_UPDATE_INTERVAL_MILLIS = 4_000L;
    public static final long BACON_HUNGER_INTERVAL_MILLIS = 4_000L;
    public static final int BACON_HUNGER_DRAIN_PER_INTERVAL = 2;
    public static final float BACON_SATURATION_ON_FOOD_CHANGE = 5.0F;
    public static final double BACON_STARVATION_DAMAGE = 4.0D;
    /** Ordinary Bacon Brawl hits use knockback; health loss is reserved for arena/environment hazards. */
    public static final boolean BACON_BRAWL_HITS_DAMAGE_HEALTH = false;
    /** Body Slam applies a second knockback component in addition to the game-wide knockback. */
    public static final double BACON_BODY_SLAM_EXTRA_KNOCKBACK = 2.0D;
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

    public static String normalizeDragonEscapeKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (value.startsWith("dragon-escape-")) value = value.substring("dragon-escape-".length());
        return switch (value) {
            case "jumper" -> "jumper";
            case "disruptor" -> "disruptor";
            case "warper" -> "warper";
            case "digger" -> "digger";
            default -> null;
        };
    }

    /** A Disruptor mine starts its own recharge window when it is thrown. */
    public static long dragonEscapeNextTntChargeAt(long thrownAtMillis) {
        return thrownAtMillis + DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS;
    }

    public static int dragonEscapeLeapUses(String kit) {
        return switch (normalizeDragonEscapeKit(kit) == null ? "" : normalizeDragonEscapeKit(kit)) {
            case "jumper" -> 4;
            case "disruptor" -> 3;
            case "warper" -> 2;
            default -> 0;
        };
    }

    public static final int DRAGON_ESCAPE_DIGGER_MAX_USES = 6;
    public static final int DRAGON_ESCAPE_DISRUPTOR_MAX_TNT = 2;
    public static final long DRAGON_ESCAPE_DISRUPTOR_TNT_INTERVAL_MILLIS = 10_000L;

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

    /** Map MAX_Y is exclusive; scanning is complete as soon as the next Y reaches it. */
    public static boolean isMicroCrumbleScanComplete(int nextY, int maxYExclusive) {
        return nextY >= maxYExclusive;
    }

    /** The source Micro Battles glass barrier is two blocks high, starting at the island spawn Y. */
    public static int microBattlesBarrierTopY(int islandSpawnY) {
        return islandSpawnY + 1;
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
    /** Blocks which are part of the supported source Spleef floor palette. */
    public static boolean isSpleefFloorMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        if (name.endsWith("_WOOL") || name.endsWith("_TERRACOTTA") || material == Material.TERRACOTTA) {
            return true;
        }
        return switch (material) {
            case SNOW_BLOCK, CLAY, STONE, COBBLESTONE, STONE_BRICKS, MOSSY_STONE_BRICKS,
                    CRACKED_STONE_BRICKS, GRASS_BLOCK, DIRT, SPRUCE_PLANKS, OAK_PLANKS, BIRCH_PLANKS -> true;
            default -> false;
        };
    }

    /** Apply the source's green/lime -> yellow -> red -> broken material sequence. */
    public static Material nextSpleefSlowDamageMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        String family = name.endsWith("_WOOL") ? "_WOOL"
                : name.endsWith("_TERRACOTTA") ? "_TERRACOTTA" : "";
        if (family.isEmpty()) return null;
        String colour = name.substring(0, name.length() - family.length());
        return switch (colour) {
            case "LIME", "GREEN" -> family.equals("_WOOL") ? Material.YELLOW_WOOL : Material.YELLOW_TERRACOTTA;
            case "YELLOW" -> family.equals("_WOOL") ? Material.RED_WOOL : Material.RED_TERRACOTTA;
            default -> null;
        };
    }

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

    /** Bomb Lobbers TNT damages opponents only; this remains true if the thrower disconnects. */
    public static boolean areBombLobbersEnemies(int ownerTeam, int targetTeam) {
        return ownerTeam >= 0 && targetTeam >= 0 && ownerTeam != targetTeam;
    }

    /**
     * Legacy Minecraft armour reduction with zero toughness:
     * effective armour = min(20, max(armour / 5, armour - damage / 2)).
     */
    public static double bombLobbersDamageAfterArmor(double rawDamage, double armorPoints) {
        if (!Double.isFinite(rawDamage) || rawDamage <= 0.0D) return 0.0D;
        double armor = Double.isFinite(armorPoints) ? Math.max(0.0D, Math.min(20.0D, armorPoints)) : 0.0D;
        double effectiveArmor = Math.min(20.0D, Math.max(armor / 5.0D, armor - rawDamage / 2.0D));
        return rawDamage * (1.0D - effectiveArmor / 25.0D);
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

    /** Match Reborn's backstab cone: compare the target's backward look with the attacker direction. */
    /** Pig's source passive is +250% knockback, i.e. 3.5 times the normal amount. */
    public static final double BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER = 3.5D;

    public static double baconBrawlKnockbackMultiplier(boolean backstab) {
        return backstab ? BACON_PIG_BACKSTAB_KNOCKBACK_MULTIPLIER : 1.0D;
    }

    public static boolean isBaconBrawlBackstab(double targetLookX, double targetLookZ,
            double attackerFromTargetX, double attackerFromTargetZ) {
        double lookLength = Math.hypot(targetLookX, targetLookZ);
        double attackerLength = Math.hypot(attackerFromTargetX, attackerFromTargetZ);
        if (!Double.isFinite(lookLength) || !Double.isFinite(attackerLength)
                || lookLength < 1.0E-6D || attackerLength < 1.0E-6D) return false;
        double backX = -targetLookX / lookLength;
        double backZ = -targetLookZ / lookLength;
        double fromX = attackerFromTargetX / attackerLength;
        double fromZ = attackerFromTargetZ / attackerLength;
        return Math.hypot(backX - fromX, backZ - fromZ) < 0.8D;
    }

    /** Canonical kit IDs mirror the official names; old test aliases remain accepted. */
    public static String normalizeBaconBrawlKit(String requested) {
        if (requested == null) return null;
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return switch (value) {
            case "el-muchacho-pigo", "muchacho", "body-slam", "pigo" -> "el-muchacho-pigo";
            case "mama", "mama-pig", "mama-piggles" -> "mama-piggles";
            case "pig", "sneaky-pig", "sheep", "sheep-pig" -> "pig";
            default -> null;
        };
    }

    /** Source kits activate only when an iron axe is right-clicked. Body Slam and Bacon Blast
     * defer to interactive blocks; Pig's cloak perk runs on any right-click with its axe. */
    public static boolean shouldActivateBaconBrawlAbility(boolean rightClick, org.bukkit.Material held,
            boolean clickedUsableBlock, String kit) {
        String normalized = normalizeBaconBrawlKit(kit);
        if (!rightClick || held != org.bukkit.Material.IRON_AXE || normalized == null) return false;
        return "pig".equals(normalized) || !clickedUsableBlock;
    }

    /**
     * Return the unique highest-kill player's index, or -1 for empty/tied scores.
     * The original game is a kill race, so deaths and respawns do not remove a
     * player's accumulated kills from the final comparison.
     */
    public static int baconBrawlUniqueLeaderIndex(java.util.List<Integer> kills) {
        if (kills == null || kills.isEmpty()) return -1;
        int bestScore = Integer.MIN_VALUE;
        int bestIndex = -1;
        boolean tied = false;
        for (int i = 0; i < kills.size(); i++) {
            int score = kills.get(i) == null ? 0 : kills.get(i);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
                tied = false;
            } else if (score == bestScore) {
                tied = true;
            }
        }
        return tied ? -1 : bestIndex;
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

    public static final double SHEEP_QUEST_PLAYER_HIT_DAMAGE = 4.0D;
    public static final int SHEEP_QUEST_INITIAL_SHEEP = 4;
    public static final long SHEEP_QUEST_ROUND_MILLIS = 300_000L;
    public static final int SHEEP_QUEST_MAX_CARRY = 3;
    public static final long SHEEP_QUEST_SPAWN_INTERVAL_MILLIS = 20_000L;

    public static boolean canCarryAnotherSheep(int carriedCount) {
        return carriedCount >= 0 && carriedCount < SHEEP_QUEST_MAX_CARRY;
    }

    /** Source Sheep Quest slows carriers progressively, up to the three-sheep limit. */
    public static int sheepQuestCarrySlownessAmplifier(int carriedCount) {
        if (carriedCount <= 0) return -1;
        return Math.min(SHEEP_QUEST_MAX_CARRY - 1, carriedCount - 1);
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

    public static final int PAINTBALL_WINNER_SCORE = 10;

    public static boolean isPaintballWinnerScoreReached(int score) {
        return score >= PAINTBALL_WINNER_SCORE;
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

    public static final int BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS = 2;
    public static final long BACON_BRAWL_ENDGAME_ESCALATION_DELAY_MILLIS = 20_000L;
    /** At most two source escalations; make the remaining escalation faster in a two-player endgame. */
    public static long nextBaconKnockbackInterval(int completedEscalations, int alivePlayers) {
        if (completedEscalations <= 0) return BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
        if (completedEscalations >= BACON_BRAWL_MAX_KNOCKBACK_ESCALATIONS) return 0L;
        return alivePlayers <= 2 ? BACON_BRAWL_ENDGAME_ESCALATION_DELAY_MILLIS
                : BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS;
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
    /** Keep route progress monotonic when a course crosses near one of its earlier waypoints. */
    /** For source maps with vertical bounds, let players fall just below the mapped volume before elimination. */
    public static double sourceMapVoidThresholdY(boolean hasVerticalBounds, int mapMinY) {
        return hasVerticalBounds ? Math.max(0.0D, mapMinY - 1.0D) : 58.0D;
    }

    /** Fall limits below the minimum authored play surface for known low-altitude Dragon Escape maps. */
    public static double dragonEscapeVoidThresholdY(String mapName) {
        String name = mapName == null ? "" : mapName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("hell")) return 24.0D;
        if (name.contains("pirate")) return 0.0D;
        return 58.0D;
    }

    /** Avoid rechecking the same destruction sphere every server tick. */
    public static boolean shouldProcessDragonEscapeDestruction(double distanceSquared, boolean hasPreviousCenter) {
        return !hasPreviousCenter || !Double.isFinite(distanceSquared) || distanceSquared >= 0.25D;
    }

    /** The straight X/Z crumble strip belongs only to the generated fallback course. */
    public static boolean shouldCrumbleDragonEscapeFallbackTrack(boolean hasAuthoredRoute, int playerZ) {
        int crumbleZ = playerZ - 5;
        return !hasAuthoredRoute && crumbleZ > -24 && crumbleZ < 78;
    }

    public static int monotonicDragonEscapeProgress(int previous, int candidate, int waypointCount) {
        if (waypointCount <= 0) return -1;
        return Math.min(waypointCount - 1, Math.max(0, Math.max(previous, candidate)));
    }

    public static boolean isDuplicateDragonEscapeWaypoint(double distance) {
        return isDuplicateDragonEscapeWaypoint(distance, DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE);
    }

    public static boolean isDuplicateDragonEscapeWaypoint(double distance, double threshold) {
        return Double.isFinite(distance) && Double.isFinite(threshold)
                && distance >= 0.0D && threshold >= 0.0D && distance < threshold;
    }

    public static boolean shouldDragonEscapePlayerBeCaught(double dragonProgress, double playerProgress) {
        return dragonProgress > playerProgress;
    }

    public static final long QUIVER_RESPAWN_DELAY_TICKS = 20L;
    public static final long QUIVER_GROUND_POUND_COOLDOWN_MILLIS = 10_000L;
    public static final long QUIVER_NINJA_VANISH_MILLIS = 1_200L;
    /** Practical multi-block target-search range for the source's "nearby enemies" chain arrow. */
    public static final double QUIVER_REBOUND_MAX_DISTANCE_SQUARED = 64.0D;
    public static final int QUIVER_MAX_ARROW_BOUNCES = 2;

    public static boolean isQuiverReboundTargetInRange(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= QUIVER_REBOUND_MAX_DISTANCE_SQUARED;
    }

    public static boolean shouldQuiverArrowBounce(int remainingBounces) {
        return remainingBounces > 0;
    }

    public static int quiverArrowBouncesAfterBounce(int remainingBounces) {
        return Math.max(0, remainingBounces - 1);
    }

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

    /** Leaper flight can be re-enabled only on landing, never during the launch frame. */
    public static boolean shouldRearmQuiverLeap(boolean onGround, boolean awaitingLanding, double verticalVelocity) {
        if (!onGround) return false;
        if (!awaitingLanding) return true;
        return Double.isFinite(verticalVelocity) && verticalVelocity <= 0.01D;
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

    /** Surviving runners have the Death Tag objective at timeout; chaser scores are only a fallback when none remain. */
    /** The chaser side wins only after there are no living runners left. */
    public static boolean deathTagChasersWinRound(int runnersRemaining) {
        return runnersRemaining <= 0;
    }

    public static boolean shouldDeathTagRunnerWinTimeout(int runnersRemaining) {
        return runnersRemaining > 0;
    }

    public static boolean shouldFinishDeathTagRound(int runnersRemaining, int initialPlayers) {
        return initialPlayers > 2 ? runnersRemaining <= 1 : runnersRemaining <= 0;
    }

    /** Generated island-ring removal must never run against an archived source map. */
    public static boolean shouldRemoveGeneratedMicroBarrier(boolean sourceMapLoaded, int trackedBarrierCount) {
        return !sourceMapLoaded && trackedBarrierCount <= 0;
    }

    /** Clamp flying dragon targets inside the authored X/Z map bounds with a one-block safety margin. */
    public static double clampDragonSkyTargetCoordinate(double coordinate, int minInclusive, int maxExclusive) {
        if (!Double.isFinite(coordinate) || maxExclusive <= minInclusive) {
            return (minInclusive + (double) maxExclusive) / 2.0D;
        }
        double low = minInclusive + 1.0D;
        double high = maxExclusive - 1.001D;
        if (high < low) return (minInclusive + (double) maxExclusive) / 2.0D;
        return Math.max(low, Math.min(high, coordinate));
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

    /** Turf floor is the block below players' source spawn feet location. */
    public static int turfWarsFloorYFromSpawn(double spawnY) {
        if (!Double.isFinite(spawnY)) return 63;
        return (int) Math.floor(spawnY) - 1;
    }

    /** Initial line sits immediately to the left of the X-axis midpoint (red territory). */
    public static int turfWarsInitialFrontline(int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return -1;
        return (int) Math.ceil((minX + (double) maxXExclusive) / 2.0D) - 1;
    }

    /** Source-map build bounds use an inclusive minimum and exclusive maximum. */
    public static boolean isTurfWarsBuildWithinBounds(int x, int z,
            int minX, int maxXExclusive, int minZ, int maxZExclusive) {
        return maxXExclusive > minX && maxZExclusive > minZ
                && x >= minX && x < maxXExclusive && z >= minZ && z < maxZExclusive;
    }

    /** Only the half of the arena on the team's side of the current line may be built on. */
    public static boolean canTurfWarsBuildOnTerritory(int teamId, int x, int frontline) {
        if (teamId == 0) return x <= frontline;
        if (teamId == 1) return x > frontline;
        return false;
    }

    /** Source-map bounds are exclusive at the maximum coordinate. */
    public static boolean isTurfWarsWinningLine(int frontline, int teamId, int minX, int maxXExclusive) {
        if (maxXExclusive <= minX) return false;
        if (teamId == 0) return frontline >= maxXExclusive - 1;
        if (teamId == 1) return frontline < minX;
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
        if (teamId == 1) return frontline < -halfSize;
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
