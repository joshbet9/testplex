package au.com.testplex.arcadeports;

/**
 * Mixed Arcade adapts the 1.8 free-for-all SkyWars rules to the 8–16 player queue.
 */
public final class SkyWarsMixedArcadeRules {
    private SkyWarsMixedArcadeRules() {}

    public static final int MAX_PLAYERS = 16;
    public static final long PREPARE_MILLIS = 10_000L;
    public static final long ROUND_MILLIS = 20 * 60_000L;
    public static final long CRUMBLE_DELAY_MILLIS = 100_000L;
    public static final long TNT_GENERATOR_INTERVAL_MILLIS = 30_000L;
    public static final int TNT_PER_DROP = 2;
    public static final long TNT_THROW_FUSE_TICKS = 60L;
    public static final long TNT_THROW_COOLDOWN_MILLIS = 500L;
    public static final double TNT_THROW_STRENGTH = 1.6D;

    public static boolean hasRoom(int queuedPlayers) {
        return queuedPlayers < maxPlayers();
    }

    public static int acceptedPlayers(int requestedPlayers) {
        return Math.max(0, Math.min(maxPlayers(), requestedPlayers));
    }

    public static long roundEndsAt(long startMillis) {
        return startMillis + prepareMillis() + roundMillis();
    }

    public static long liveStartsAt(long startMillis) {
        return startMillis + prepareMillis();
    }

    public static long crumbleStartsAt(long startMillis) {
        return liveStartsAt(startMillis) + crumbleDelayMillis();
    }

    public static long firstTntAt(long startMillis) {
        return liveStartsAt(startMillis) + tntGeneratorIntervalMillis();
    }

    public static int maxPlayers() {
        return Math.max(1, Math.min(16, intRule("SKY_WARS_MAX_PLAYERS", MAX_PLAYERS)));
    }
    public static long prepareMillis() { return longRule("SKY_WARS_PREPARE_MILLIS", PREPARE_MILLIS); }
    public static long roundMillis() { return longRule("SKY_WARS_ROUND_MILLIS", ROUND_MILLIS); }
    public static long crumbleDelayMillis() { return longRule("SKY_WARS_CRUMBLE_DELAY_MILLIS", CRUMBLE_DELAY_MILLIS); }
    public static long tntGeneratorIntervalMillis() {
        return longRule("SKY_WARS_TNT_GENERATOR_INTERVAL_MILLIS", TNT_GENERATOR_INTERVAL_MILLIS);
    }
    public static int tntPerDrop() {
        return Math.max(1, Math.min(64, intRule("SKY_WARS_TNT_PER_DROP", TNT_PER_DROP)));
    }

    private static int intRule(String key, int fallback) {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(key, fallback));
    }

    private static long longRule(String key, long fallback) {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue(key, fallback));
    }

    public static final double ORE_ISLAND_RADIUS_BLOCKS = 8.0D;
    public static final int DIAMOND_VEINS_PER_ISLAND = 3;
    public static final int DIAMOND_VEIN_SIZE = 2;
    public static final int IRON_VEINS_PER_ISLAND = 7;
    public static final int GRAVEL_VEINS_PER_ISLAND = 4;
    public static final int RANDOM_VEIN_BASE_SIZE = 3;
    public static final int RANDOM_VEIN_EXTRA_SIZE = 2;

    /** Source WorldData.GetCustomLocs("56") slots are assigned within eight horizontal blocks of an island spawn. */
    public static boolean isOreSlotInIsland(double slotX, double slotZ, double spawnX, double spawnZ) {
        if (!Double.isFinite(slotX) || !Double.isFinite(slotZ)
                || !Double.isFinite(spawnX) || !Double.isFinite(spawnZ)) return false;
        double dx = slotX - spawnX;
        double dz = slotZ - spawnZ;
        return dx * dx + dz * dz <= ORE_ISLAND_RADIUS_BLOCKS * ORE_ISLAND_RADIUS_BLOCKS;
    }

    /** Reborn's OreHider leaves ore exposed only when a surrounding block is non-occluding. */
    public static boolean shouldHideOre(boolean hasNonOccludingNeighbour) {
        return !hasNonOccludingNeighbour;
    }

    /** Iron and gravel veins are 3 + random(3), where random(3) is 0, 1 or 2. */
    public static int randomVeinSize(int randomRoll) {
        if (randomRoll < 0 || randomRoll > RANDOM_VEIN_EXTRA_SIZE) {
            throw new IllegalArgumentException("SkyWars vein random roll must be 0, 1 or 2.");
        }
        return RANDOM_VEIN_BASE_SIZE + randomRoll;
    }

    public static boolean hasSourceBonusDrop(String materialName) {
        if (materialName == null) return false;
        return switch (materialName.toUpperCase(java.util.Locale.ROOT)) {
            case "COBWEB", "WEB", "GRAVEL", "IRON_ORE" -> true;
            default -> false;
        };
    }

    /** Source block-placement cap is three blocks below WorldData.MaxY. */
    public static boolean shouldBlockSkyWarsPlacement(String materialName, int blockY, int mapMaxY) {
        if (materialName == null) return true;
        String material = materialName.toUpperCase(java.util.Locale.ROOT);
        boolean forbidden = switch (material) {
            case "CHEST", "TRAPPED_CHEST", "PISTON", "STICKY_PISTON", "HOPPER" -> true;
            default -> false;
        };
        return forbidden || blockY >= mapMaxY - 3;
    }
}
