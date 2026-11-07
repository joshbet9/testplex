package au.com.testplex.arcadeports;

import org.bukkit.Material;

/** Pure, regression-tested rules extracted from the Mineplex Reborn Sneaky Assassins source. */
final class SneakyAssassinsRules {
    public static final long POWERUP_KILL_WINDOW_MILLIS = 1_000L;
    public static final long FIRST_POWERUP_DELAY_MILLIS = 12_000L;
    public static final long NEXT_POWERUP_DELAY_MILLIS = 7_000L;
    public static final int MAX_ACTIVE_POWERUPS = 2;
    public static final int MASTER_ASSASSIN_POWERUPS_REQUIRED = 4;
    public static final int REVEAL_RADIUS_BLOCKS = 12;
    public static final int SMOKE_RADIUS_BLOCKS = 5;
    public static final long GLOBAL_REVEAL_CYCLE_MILLIS = 60_000L;
    public static final long REVEALER_DURATION_MILLIS = 5_000L;
    public static final long SMOKE_CLOAK_DURATION_MILLIS = 1_200L;
    public static final long BRIBE_COOLDOWN_MILLIS = 8_000L;
    public static final int SOURCE_NPC_COUNT = 200;
    public static final long NPC_CULL_INTERVAL_MILLIS = 4_000L;
    public static final int NPC_PATROL_ANCHOR_DISTANCE_BLOCKS = 16;
    public static final int NPC_PATROL_ORDER_CHANCE_DENOMINATOR = 40;
    public static final long NPC_STUCK_RECOVERY_MILLIS = 800L;
    public static final int MAX_POWERUP_PROGRESS = 5;
    public static final long GLOBAL_REVEAL_DURATION_MILLIS = 4_000L;
    public static final int SMOKE_BLINDNESS_DURATION_MILLIS = 3_000;
    public static final double POWERUP_PICKUP_RADIUS_BLOCKS = 1.0D;
    public static final double WOOD_SWORD_DAMAGE = 4.0D;
    public static final double STONE_SWORD_DAMAGE = 4.5D;
    public static final double GOLD_SWORD_DAMAGE = 5.0D;
    public static final double IRON_SWORD_DAMAGE = 6.0D;
    public static final double DIAMOND_SWORD_DAMAGE = 7.0D;
    public static final double PROJECTILE_DAMAGE_MIN = 2.0D;
    public static final double PROJECTILE_DAMAGE_PER_SPEED = 2.0D;
    public static final int POWERUP_BONUS_HEALTH = 2;
    public static final int POWERUP_MAX_HEALTH = 40;
    public static final int KILL_BONUS_HEALTH = 2;
    public static final int MASTERS_MASTER_POWERUPS_REQUIRED = 4;
    public static final long NPC_ATTACK_COOLDOWN_MILLIS = 1_000L;
    public static final double NPC_ATTACK_RANGE_BLOCKS = 1.5D;
    public static final double NPC_ATTACK_DAMAGE = 1.0D;
    public static final double NPC_BRIBE_MOVE_SPEED = 0.8D;
    public static final double NPC_PATROL_MOVE_SPEED = 0.7D;
    public static final double NPC_RECOVERY_HORIZONTAL = 0.3D;
    public static final double NPC_RECOVERY_VERTICAL = 0.3D;
    public static final double NPC_MOTION_FACTOR = 0.18D;
    public static final double NPC_MIN_VERTICAL_SPEED = -0.05D;
    public static final double NPC_MAX_VERTICAL_SPEED = 0.12D;
    public static final int REVEALER_PROJECTILE_LIFETIME_TICKS = 120;
    public static final double REVEALER_PROJECTILE_SPEED = 1.2D;
    public static final double REVEALER_PROJECTILE_VERTICAL_SPEED = 0.2D;
    public static final int NPC_PATROL_WAYPOINT_ATTEMPTS = 5;
    public static final int NPC_SPAWN_SEARCH_ATTEMPTS = 80;
    public static final int PROSECUTOR_DISTINCT_TARGETS_REQUIRED = 5;
    public static final int DUELS_OTHER_MASTERS_REQUIRED = 2;
    public static final int SO_SO_SNEAKY_WINS_REQUIRED = 15;
    public static final int MASTER_ASSASSIN_ACHIEVEMENTS_REQUIRED = 10;
    public static final int INCOMPETENCE_NPCS_REQUIRED = 200;
    public static final int I_SEE_YOU_REVEALS_REQUIRED = 50;
    public static final int SMOKE_SCREEN_ESCAPES_REQUIRED = 3;
    public static final int DEADEYE_BOW_KILLS_REQUIRED = 3;
    public static final int BASE_SMOKE_BOMBS = 1;
    public static final int ESCAPE_ARTIST_EXTRA_SMOKE_BOMBS = 2;
    public static final int ASSASSIN_STARTING_ARROWS = 32;
    public static final int BRIBER_STARTING_EMERALDS = 4;
    public static final int REVEALER_STARTING_DIAMONDS = 3;
    public static final int POWERUP_SMOKE_BOMBS = 1;
    public static final int KILL_BONUS_SMOKE_BOMBS = 1;
    private SneakyAssassinsRules() { }

    static boolean shouldAwardYouNeedDirections(boolean masterAssassin, boolean directPlayerAttack,
            boolean actualMasterCompass, boolean lethalHit) {
        return masterAssassin && directPlayerAttack && actualMasterCompass && lethalHit;
    }

    static boolean shouldAwardNotSoSneaky(String kit, boolean dealtMeleeDamage) {
        return "assassin".equals(kit) && !dealtMeleeDamage;
    }

    static boolean shouldAwardIdiot(String kit, boolean usedSmokeBomb) {
        return "escape-artist".equals(kit) && !usedSmokeBomb;
    }

    static boolean shouldAwardTheDuelsDuelsDuel(boolean winnerIsMasterAssassin, int otherMasterAssassins) {
        return winnerIsMasterAssassin && otherMasterAssassins >= GameDebugRuntimeOverrides.intValue(
                "SNEAKY_ASSASSINS_DUELS_OTHER_MASTERS_REQUIRED", DUELS_OTHER_MASTERS_REQUIRED);
    }

    static boolean shouldAwardProsecutor(int distinctTargets) {
        return distinctTargets >= GameDebugRuntimeOverrides.intValue(
                "SNEAKY_ASSASSINS_PROSECUTOR_DISTINCT_TARGETS_REQUIRED", PROSECUTOR_DISTINCT_TARGETS_REQUIRED);
    }

    static boolean shouldAwardSmokeScreen(String kit, int smokeEscapes) {
        return "escape-artist".equals(kit) && smokeEscapes >= SMOKE_SCREEN_ESCAPES_REQUIRED;
    }

    static boolean smokePursuitWasEscaped(boolean pursuerStillEligible, boolean pursuerStillNearby) {
        return pursuerStillEligible && !pursuerStillNearby;
    }

    static boolean shouldAwardDeadeye(String kit, int bowKills) {
        return "assassin".equals(kit) && bowKills >= DEADEYE_BOW_KILLS_REQUIRED;
    }

    static boolean shouldAwardPaidInFull(boolean winner, boolean finalEliminationByBribedVillager) {
        return winner && finalEliminationByBribedVillager;
    }

    static String achievementForLifetimeStat(String stat, int value) {
        if (stat == null) return null;
        return switch (stat) {
            case "master-assassin" -> value >= MASTER_ASSASSIN_ACHIEVEMENTS_REQUIRED ? "master-assassin" : null;
            case "incompetence" -> value >= INCOMPETENCE_NPCS_REQUIRED ? "incompetence" : null;
            case "i-see-you" -> value >= I_SEE_YOU_REVEALS_REQUIRED ? "i-see-you" : null;
            default -> null;
        };
    }

    static boolean isPowerUpKillWindow(long nowMillis, long collectedAtMillis) {
        return collectedAtMillis > 0L && nowMillis >= collectedAtMillis
                && nowMillis - collectedAtMillis <= GameDebugRuntimeOverrides.longValue(
                        "SNEAKY_ASSASSINS_POWERUP_KILL_WINDOW_MILLIS", POWERUP_KILL_WINDOW_MILLIS);
    }

    static long firstPowerUpDelayMillis() { return value("FIRST_POWERUP_DELAY_MILLIS", FIRST_POWERUP_DELAY_MILLIS); }
    static long nextPowerUpDelayMillis() { return value("NEXT_POWERUP_DELAY_MILLIS", NEXT_POWERUP_DELAY_MILLIS); }
    static int maxActivePowerUps() { return GameDebugRuntimeOverrides.intValue("SNEAKY_ASSASSINS_MAX_ACTIVE_POWERUPS", MAX_ACTIVE_POWERUPS); }
    static int npcPatrolOrderChanceDenominator() { return Math.max(1, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_NPC_PATROL_ORDER_CHANCE_DENOMINATOR", NPC_PATROL_ORDER_CHANCE_DENOMINATOR)); }

    static double swordDamage(Material weapon) {
        if (weapon == null) return 0.0D;
        return switch (weapon) {
            case WOODEN_SWORD -> decimal("WOOD_SWORD_DAMAGE", WOOD_SWORD_DAMAGE);
            case STONE_SWORD -> decimal("STONE_SWORD_DAMAGE", STONE_SWORD_DAMAGE);
            case GOLDEN_SWORD -> decimal("GOLD_SWORD_DAMAGE", GOLD_SWORD_DAMAGE);
            case IRON_SWORD -> decimal("IRON_SWORD_DAMAGE", IRON_SWORD_DAMAGE);
            case DIAMOND_SWORD -> decimal("DIAMOND_SWORD_DAMAGE", DIAMOND_SWORD_DAMAGE);
            default -> 0.0D;
        };
    }

    static Material nextWeapon(Material weapon) {
        if (weapon == null) return null;
        return switch (weapon) {
            case WOODEN_SWORD -> Material.STONE_SWORD;
            case STONE_SWORD -> Material.GOLDEN_SWORD;
            case GOLDEN_SWORD -> Material.IRON_SWORD;
            case IRON_SWORD -> Material.DIAMOND_SWORD;
            default -> null;
        };
    }

    static int armorTier(Material armor) {
        if (armor == null) return -1;
        String name = armor.name();
        if (name.startsWith("LEATHER_")) return 0;
        if (name.startsWith("CHAINMAIL_")) return 1;
        if (name.startsWith("IRON_")) return 2;
        return -1;
    }

    static Material nextArmor(Material armor) {
        if (armor == null) return null;
        return switch (armor) {
            case LEATHER_BOOTS -> Material.CHAINMAIL_BOOTS;
            case LEATHER_LEGGINGS -> Material.CHAINMAIL_LEGGINGS;
            case LEATHER_CHESTPLATE -> Material.CHAINMAIL_CHESTPLATE;
            case LEATHER_HELMET -> Material.CHAINMAIL_HELMET;
            case CHAINMAIL_BOOTS -> Material.IRON_BOOTS;
            case CHAINMAIL_LEGGINGS -> Material.IRON_LEGGINGS;
            case CHAINMAIL_CHESTPLATE -> Material.IRON_CHESTPLATE;
            case CHAINMAIL_HELMET -> Material.IRON_HELMET;
            default -> null;
        };
    }

    static int nextPowerUpCount(int current) {
        return Math.max(0, Math.min(maxPowerUpProgress(), current + 1));
    }

    static int maxPowerUpProgress() { return Math.max(1, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_MAX_POWERUP_PROGRESS", MAX_POWERUP_PROGRESS)); }
    static int powerUpBonusHealth() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_POWERUP_BONUS_HEALTH", POWERUP_BONUS_HEALTH)); }
    static int powerUpMaxHealth() { return Math.max(1, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_POWERUP_MAX_HEALTH", POWERUP_MAX_HEALTH)); }
    static int killBonusHealth() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_KILL_BONUS_HEALTH", KILL_BONUS_HEALTH)); }
    static double powerUpPickupRadiusSquared() {
        double radius = decimal("POWERUP_PICKUP_RADIUS_BLOCKS", POWERUP_PICKUP_RADIUS_BLOCKS);
        return radius * radius;
    }
    static double projectileDamage(double velocity) {
        return Math.max(decimal("PROJECTILE_DAMAGE_MIN", PROJECTILE_DAMAGE_MIN),
                velocity * decimal("PROJECTILE_DAMAGE_PER_SPEED", PROJECTILE_DAMAGE_PER_SPEED));
    }
    static long globalRevealDurationMillis() { return value("GLOBAL_REVEAL_DURATION_MILLIS", GLOBAL_REVEAL_DURATION_MILLIS); }
    static int smokeBlindnessDurationMillis() { return GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_SMOKE_BLINDNESS_DURATION_MILLIS", SMOKE_BLINDNESS_DURATION_MILLIS); }
    static double npcAttackRangeSquared() {
        double range = decimal("NPC_ATTACK_RANGE_BLOCKS", NPC_ATTACK_RANGE_BLOCKS);
        return range * range;
    }
    static boolean npcAttackInRange(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0.0D
                && distanceSquared <= npcAttackRangeSquared();
    }
    static boolean npcAttackReady(long now, long readyAt) { return now >= readyAt; }
    static long nextNpcAttackAt(long now) { return now + npcAttackCooldownMillis(); }
    static double npcAttackDamage() { return decimal("NPC_ATTACK_DAMAGE", NPC_ATTACK_DAMAGE); }
    static long npcAttackCooldownMillis() { return value("NPC_ATTACK_COOLDOWN_MILLIS", NPC_ATTACK_COOLDOWN_MILLIS); }
    static double npcBribeMoveSpeed() { return decimal("NPC_BRIBE_MOVE_SPEED", NPC_BRIBE_MOVE_SPEED); }
    static double npcPatrolMoveSpeed() { return decimal("NPC_PATROL_MOVE_SPEED", NPC_PATROL_MOVE_SPEED); }
    static double npcRecoveryHorizontal() { return decimal("NPC_RECOVERY_HORIZONTAL", NPC_RECOVERY_HORIZONTAL); }
    static double npcRecoveryVertical() { return decimal("NPC_RECOVERY_VERTICAL", NPC_RECOVERY_VERTICAL); }
    static double npcMotionFactor() { return decimal("NPC_MOTION_FACTOR", NPC_MOTION_FACTOR); }
    static double npcMinVerticalSpeed() { return decimal("NPC_MIN_VERTICAL_SPEED", NPC_MIN_VERTICAL_SPEED); }
    static double npcMaxVerticalSpeed() { return decimal("NPC_MAX_VERTICAL_SPEED", NPC_MAX_VERTICAL_SPEED); }
    static int npcPatrolWaypointAttempts() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_NPC_PATROL_WAYPOINT_ATTEMPTS", NPC_PATROL_WAYPOINT_ATTEMPTS)); }
    static int npcSpawnSearchAttempts() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_NPC_SPAWN_SEARCH_ATTEMPTS", NPC_SPAWN_SEARCH_ATTEMPTS)); }
    static int baseSmokeBombs() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_BASE_SMOKE_BOMBS", BASE_SMOKE_BOMBS)); }
    static int escapeArtistExtraSmokeBombs() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_ESCAPE_ARTIST_EXTRA_SMOKE_BOMBS", ESCAPE_ARTIST_EXTRA_SMOKE_BOMBS)); }
    static int assassinStartingArrows() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_ASSASSIN_STARTING_ARROWS", ASSASSIN_STARTING_ARROWS)); }
    static int briberStartingEmeralds() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_BRIBER_STARTING_EMERALDS", BRIBER_STARTING_EMERALDS)); }
    static int revealerStartingDiamonds() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_REVEALER_STARTING_DIAMONDS", REVEALER_STARTING_DIAMONDS)); }
    static int powerUpSmokeBombs() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_POWERUP_SMOKE_BOMBS", POWERUP_SMOKE_BOMBS)); }
    static int killBonusSmokeBombs() { return Math.max(0, GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_KILL_BONUS_SMOKE_BOMBS", KILL_BONUS_SMOKE_BOMBS)); }
    static int revealerProjectileLifetimeTicks() { return GameDebugRuntimeOverrides.intValue(
            "SNEAKY_ASSASSINS_REVEALER_PROJECTILE_LIFETIME_TICKS", REVEALER_PROJECTILE_LIFETIME_TICKS); }
    static double revealerProjectileSpeed() { return decimal("REVEALER_PROJECTILE_SPEED", REVEALER_PROJECTILE_SPEED); }
    static double revealerProjectileVerticalSpeed() { return decimal(
            "REVEALER_PROJECTILE_VERTICAL_SPEED", REVEALER_PROJECTILE_VERTICAL_SPEED); }

    static boolean becomesMasterAssassin(int currentPowerUps) {
        return currentPowerUps == GameDebugRuntimeOverrides.intValue(
                "SNEAKY_ASSASSINS_MASTER_ASSASSIN_POWERUPS_REQUIRED", MASTER_ASSASSIN_POWERUPS_REQUIRED);
    }

    static boolean isSmokeBombKit(String kit) {
        return "escape-artist".equals(kit) || "assassin".equals(kit)
                || "briber".equals(kit) || "revealer".equals(kit);
    }

    static double revealRadiusSquared() {
        double radius = GameDebugRuntimeOverrides.intValue("SNEAKY_ASSASSINS_REVEAL_RADIUS_BLOCKS", REVEAL_RADIUS_BLOCKS);
        return radius * radius;
    }

    static double smokeRadiusSquared() {
        double radius = GameDebugRuntimeOverrides.intValue("SNEAKY_ASSASSINS_SMOKE_RADIUS_BLOCKS", SMOKE_RADIUS_BLOCKS);
        return radius * radius;
    }

    static long globalRevealCycleMillis() {
        return value("GLOBAL_REVEAL_CYCLE_MILLIS", GLOBAL_REVEAL_CYCLE_MILLIS);
    }

    static long revealerDurationMillis() {
        return value("REVEALER_DURATION_MILLIS", REVEALER_DURATION_MILLIS);
    }

    static long smokeCloakDurationMillis() {
        return value("SMOKE_CLOAK_DURATION_MILLIS", SMOKE_CLOAK_DURATION_MILLIS);
    }

    static long bribeCooldownMillis() {
        return value("BRIBE_COOLDOWN_MILLIS", BRIBE_COOLDOWN_MILLIS);
    }

    static int sourceNpcCount() {
        return GameDebugRuntimeOverrides.intValue("SNEAKY_ASSASSINS_SOURCE_NPC_COUNT", SOURCE_NPC_COUNT);
    }

    /** Source NpcManager removes one random town NPC every four seconds while Live. */
    static long npcCullIntervalMillis() {
        return value("NPC_CULL_INTERVAL_MILLIS", NPC_CULL_INTERVAL_MILLIS);
    }

    static boolean shouldCullTownNpc(boolean live, long nowMillis, long nextCullAtMillis, int npcCount) {
        return live && npcCount > 0 && nowMillis >= nextCullAtMillis;
    }

    /** Invalid or missing entities are pruned from the runtime's UUID index. */
    static boolean shouldPruneTownNpc(boolean entityExists, boolean validVillager) {
        return !entityExists || !validVillager;
    }
    /** Reborn considers only town spawn points within sixteen blocks on each horizontal axis. */
    static boolean isNpcPatrolAnchorNearby(double npcX, double npcZ, double anchorX, double anchorZ) {
        return Double.isFinite(npcX) && Double.isFinite(npcZ)
                && Double.isFinite(anchorX) && Double.isFinite(anchorZ)
                && Math.abs(npcX - anchorX) <= GameDebugRuntimeOverrides.intValue(
                        "SNEAKY_ASSASSINS_NPC_PATROL_ANCHOR_DISTANCE_BLOCKS", NPC_PATROL_ANCHOR_DISTANCE_BLOCKS)
                && Math.abs(npcZ - anchorZ) <= GameDebugRuntimeOverrides.intValue(
                        "SNEAKY_ASSASSINS_NPC_PATROL_ANCHOR_DISTANCE_BLOCKS", NPC_PATROL_ANCHOR_DISTANCE_BLOCKS);
    }

    /** Reborn picks one independent patrol waypoint attempt with probability 1/40 each tick. */
    static boolean shouldIssueNpcPatrolOrder(int randomRoll) {
        return randomRoll >= 0 && randomRoll < npcPatrolOrderChanceDenominator();
    }

    /** A generated-town patrol destination needs support, two clear blocks and no liquid floor. */
    static boolean isSafeNpcPatrolDestination(boolean solidFloor, boolean liquidFloor,
            boolean feetPassable, boolean headPassable) {
        return solidFloor && !liquidFloor && feetPassable && headPassable;
    }

    /** Reborn applies a recovery jump when a bribed villager cannot move for 800 ms. */
    static boolean shouldRecoverBribedNpc(long nowMillis, long lastMovementAtMillis) {
        return lastMovementAtMillis > 0L && nowMillis >= lastMovementAtMillis
                && nowMillis - lastMovementAtMillis >= value("NPC_STUCK_RECOVERY_MILLIS", NPC_STUCK_RECOVERY_MILLIS);
    }

    static boolean debuggableRule(String name) {
        return switch (name) {
            case "POWERUP_KILL_WINDOW_MILLIS", "FIRST_POWERUP_DELAY_MILLIS", "NEXT_POWERUP_DELAY_MILLIS",
                    "MAX_ACTIVE_POWERUPS", "MASTER_ASSASSIN_POWERUPS_REQUIRED", "REVEAL_RADIUS_BLOCKS",
                    "SMOKE_RADIUS_BLOCKS", "GLOBAL_REVEAL_CYCLE_MILLIS", "REVEALER_DURATION_MILLIS",
                    "SMOKE_CLOAK_DURATION_MILLIS", "BRIBE_COOLDOWN_MILLIS", "SOURCE_NPC_COUNT",
                    "NPC_CULL_INTERVAL_MILLIS", "NPC_PATROL_ANCHOR_DISTANCE_BLOCKS",
                    "NPC_PATROL_ORDER_CHANCE_DENOMINATOR", "NPC_STUCK_RECOVERY_MILLIS",
                    "MAX_POWERUP_PROGRESS", "GLOBAL_REVEAL_DURATION_MILLIS", "SMOKE_BLINDNESS_DURATION_MILLIS",
                    "POWERUP_PICKUP_RADIUS_BLOCKS", "WOOD_SWORD_DAMAGE", "STONE_SWORD_DAMAGE",
                    "GOLD_SWORD_DAMAGE", "IRON_SWORD_DAMAGE", "DIAMOND_SWORD_DAMAGE",
                    "PROJECTILE_DAMAGE_MIN", "PROJECTILE_DAMAGE_PER_SPEED", "POWERUP_BONUS_HEALTH",
                    "POWERUP_MAX_HEALTH", "KILL_BONUS_HEALTH",
                    "MASTERS_MASTER_POWERUPS_REQUIRED", "NPC_ATTACK_COOLDOWN_MILLIS", "NPC_ATTACK_RANGE_BLOCKS",
                    "NPC_ATTACK_DAMAGE", "NPC_BRIBE_MOVE_SPEED", "NPC_PATROL_MOVE_SPEED",
                    "NPC_RECOVERY_HORIZONTAL", "NPC_RECOVERY_VERTICAL", "NPC_MOTION_FACTOR",
                    "NPC_MIN_VERTICAL_SPEED", "NPC_MAX_VERTICAL_SPEED", "REVEALER_PROJECTILE_LIFETIME_TICKS",
                    "REVEALER_PROJECTILE_SPEED", "REVEALER_PROJECTILE_VERTICAL_SPEED",
                    "NPC_PATROL_WAYPOINT_ATTEMPTS", "NPC_SPAWN_SEARCH_ATTEMPTS",
                    "PROSECUTOR_DISTINCT_TARGETS_REQUIRED", "DUELS_OTHER_MASTERS_REQUIRED",
                    "BASE_SMOKE_BOMBS", "ESCAPE_ARTIST_EXTRA_SMOKE_BOMBS", "ASSASSIN_STARTING_ARROWS",
                    "BRIBER_STARTING_EMERALDS", "REVEALER_STARTING_DIAMONDS", "POWERUP_SMOKE_BOMBS",
                    "KILL_BONUS_SMOKE_BOMBS" -> true;
            default -> false;
        };
    }

    private static long value(String name, long fallback) {
        return GameDebugRuntimeOverrides.longValue("SNEAKY_ASSASSINS_" + name, fallback);
    }

    private static double decimal(String name, double fallback) {
        double value = GameDebugRuntimeOverrides.doubleValue("SNEAKY_ASSASSINS_" + name, fallback);
        return Double.isFinite(value) ? value : fallback;
    }

}
