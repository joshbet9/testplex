package au.com.testplex.arcadeports;

import org.bukkit.Material;

/** Pure, regression-tested rules extracted from the Mineplex Reborn Sneaky Assassins source. */
final class SneakyAssassinsRules {
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
                && nowMillis - collectedAtMillis <= 1_000L;
    }

    static long firstPowerUpDelayMillis() { return 12_000L; }
    static long nextPowerUpDelayMillis() { return 7_000L; }
    static int maxActivePowerUps() { return 2; }

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
        return currentPowerUps == 4;
    }

    static boolean isSmokeBombKit(String kit) {
        return "escape-artist".equals(kit) || "assassin".equals(kit)
                || "briber".equals(kit) || "revealer".equals(kit);
    }

    static double revealRadiusSquared() {
        return 12.0D * 12.0D;
    }

    static double smokeRadiusSquared() {
        return 5.0D * 5.0D;
    }

    static long globalRevealCycleMillis() {
        return 60_000L;
    }

    static long revealerDurationMillis() {
        return 5_000L;
    }

    static long smokeCloakDurationMillis() {
        return 1_200L;
    }

    static long bribeCooldownMillis() {
        return 8_000L;
    }

    static int sourceNpcCount() {
        return 200;
    }

    /** Source NpcManager removes one random town NPC every four seconds while Live. */
    static long npcCullIntervalMillis() {
        return 4_000L;
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
                && Math.abs(npcX - anchorX) <= 16.0D
                && Math.abs(npcZ - anchorZ) <= 16.0D;
    }

    /** Reborn picks one independent patrol waypoint attempt with probability 1/40 each tick. */
    static boolean shouldIssueNpcPatrolOrder(int randomRoll) {
        return randomRoll >= 0 && randomRoll < 40;
    }

    /** A generated-town patrol destination needs support, two clear blocks and no liquid floor. */
    static boolean isSafeNpcPatrolDestination(boolean solidFloor, boolean liquidFloor,
            boolean feetPassable, boolean headPassable) {
        return solidFloor && !liquidFloor && feetPassable && headPassable;
    }

    /** Reborn applies a recovery jump when a bribed villager cannot move for 800 ms. */
    static boolean shouldRecoverBribedNpc(long nowMillis, long lastMovementAtMillis) {
        return lastMovementAtMillis > 0L && nowMillis >= lastMovementAtMillis
                && nowMillis - lastMovementAtMillis >= 800L;
    }

}
