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
        return winnerIsMasterAssassin && otherMasterAssassins >= 2;
    }

    static boolean shouldAwardProsecutor(int distinctTargets) {
        return distinctTargets >= 5;
    }

    static boolean isPowerUpKillWindow(long nowMillis, long collectedAtMillis) {
        return collectedAtMillis > 0L && nowMillis >= collectedAtMillis
                && nowMillis - collectedAtMillis <= GameDebugRuntimeOverrides.longValue(
                        "SNEAKY_ASSASSINS_POWERUP_KILL_WINDOW_MILLIS", POWERUP_KILL_WINDOW_MILLIS);
    }

    static long firstPowerUpDelayMillis() { return value("FIRST_POWERUP_DELAY_MILLIS", FIRST_POWERUP_DELAY_MILLIS); }
    static long nextPowerUpDelayMillis() { return value("NEXT_POWERUP_DELAY_MILLIS", NEXT_POWERUP_DELAY_MILLIS); }
    static int maxActivePowerUps() { return GameDebugRuntimeOverrides.intValue("SNEAKY_ASSASSINS_MAX_ACTIVE_POWERUPS", MAX_ACTIVE_POWERUPS); }

    static double swordDamage(Material weapon) {
        if (weapon == null) return 0.0D;
        return switch (weapon) {
            case WOODEN_SWORD -> 4.0D;
            case STONE_SWORD -> 4.5D;
            case GOLDEN_SWORD -> 5.0D;
            case IRON_SWORD -> 6.0D;
            case DIAMOND_SWORD -> 7.0D;
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
        return Math.max(0, Math.min(5, current + 1));
    }

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
        return randomRoll >= 0 && randomRoll < GameDebugRuntimeOverrides.intValue(
                "SNEAKY_ASSASSINS_NPC_PATROL_ORDER_CHANCE_DENOMINATOR", NPC_PATROL_ORDER_CHANCE_DENOMINATOR);
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
                    "NPC_PATROL_ORDER_CHANCE_DENOMINATOR", "NPC_STUCK_RECOVERY_MILLIS" -> true;
            default -> false;
        };
    }

    private static long value(String name, long fallback) {
        return GameDebugRuntimeOverrides.longValue("SNEAKY_ASSASSINS_" + name, fallback);
    }

}
