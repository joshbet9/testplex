package au.com.testplex.arcadeports;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

/** Precise predicates for source and kit-mastery SkyWars achievements. */
public final class SkyWarsAchievementRules {
    private SkyWarsAchievementRules() {}

    public static final int SKY_KING_WINS = 20;
    public static final int TNT_HOARDER_PICKUPS = 100;
    public static final int LEFT_FOR_DEAD_ZOMBIES = 120;
    public static final int COLD_BLOODED_KILLS = 80;
    public static final double SECOND_CHANCE_FALL_BLOCKS = 20.0D;
    public static final int FIRE_SALE_VICTIMS = 2;
    public static final double BRIDGE_THE_GAP_BLOCKS = 16.0D;
    public static final int DIRT_CHEAP_VOID_KILLS = 2;
    public static final int FULL_METAL_ARMOR_PIECES = 4;
    public static final int DOUBLE_DEMOLITION_VICTIMS = 2;
    public static final int HIGH_GROUND_KILLS = 3;
    public static final double HIGH_GROUND_BLOCKS = 20.0D;

    public static boolean reached(int progress, int requirement) {
        return requirement > 0 && progress >= requirement;
    }

    public static boolean secondChance(double startingIslandY, double currentY, boolean falling) {
        return falling && Double.isFinite(startingIslandY) && Double.isFinite(currentY)
                && startingIslandY - currentY >= SECOND_CHANCE_FALL_BLOCKS;
    }

    public static boolean crossedBridge(double horizontalDistance, boolean steppedOnOwnBridge) {
        return steppedOnOwnBridge && Double.isFinite(horizontalDistance)
                && horizontalDistance >= BRIDGE_THE_GAP_BLOCKS;
    }

    public static boolean fireBurstElimination(UUID burstOwner, UUID creditedKiller, String killMode,
            boolean recent) {
        return recent && burstOwner != null && burstOwner.equals(creditedKiller)
                && killMode != null && killMode.toLowerCase(Locale.ROOT).contains("fire burst");
    }

    public static boolean hasFullMetalArmor(int metalArmorPieces) {
        return metalArmorPieces >= FULL_METAL_ARMOR_PIECES;
    }

    public static boolean highGroundElimination(double killerY, double victimY) {
        return Double.isFinite(killerY) && Double.isFinite(victimY)
                && killerY - victimY >= HIGH_GROUND_BLOCKS;
    }

    /** Tracks distinct victims for a single cast/explosion and within a single match. */
    public static final class MatchTracker {
        private final Set<UUID> fireBurstVictims = new HashSet<>();
        private final Set<UUID> dirtVoidVictims = new HashSet<>();
        private final Set<UUID> tntVictims = new HashSet<>();
        private final Set<UUID> highGroundVictims = new HashSet<>();
        private boolean secondChance;
        private boolean bridgeCrossed;
        private boolean magnetVoidElimination;

        public boolean recordFireBurstElimination(UUID victim) {
            return victim != null && fireBurstVictims.add(victim)
                    && reached(fireBurstVictims.size(), FIRE_SALE_VICTIMS);
        }

        public boolean recordDirtVoidElimination(UUID victim) {
            return victim != null && dirtVoidVictims.add(victim)
                    && reached(dirtVoidVictims.size(), DIRT_CHEAP_VOID_KILLS);
        }

        public boolean recordThrowableTntElimination(UUID victim) {
            return victim != null && tntVictims.add(victim)
                    && reached(tntVictims.size(), DOUBLE_DEMOLITION_VICTIMS);
        }

        public boolean recordHighGroundElimination(UUID victim, double killerY, double victimY) {
            return victim != null && highGroundElimination(killerY, victimY)
                    && highGroundVictims.add(victim)
                    && reached(highGroundVictims.size(), HIGH_GROUND_KILLS);
        }

        public void recordSecondChance(boolean qualifies) { secondChance |= qualifies; }
        public void recordBridgeCrossing(boolean qualifies) { bridgeCrossed |= qualifies; }
        public void recordMagnetVoidElimination(boolean qualifies) { magnetVoidElimination |= qualifies; }

        public boolean secondChance() { return secondChance; }
        public boolean bridgeCrossed() { return bridgeCrossed; }
        public boolean magnetVoidElimination() { return magnetVoidElimination; }
        public int fireBurstVictims() { return fireBurstVictims.size(); }
        public int dirtVoidVictims() { return dirtVoidVictims.size(); }
        public int tntVictims() { return tntVictims.size(); }
        public int highGroundVictims() { return highGroundVictims.size(); }
    }
}
