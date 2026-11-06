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
    public static final double ORE_ISLAND_RADIUS_BLOCKS = 8.0D;
    public static final int DIAMOND_VEINS_PER_ISLAND = 3;
    public static final int DIAMOND_VEIN_SIZE = 2;
    public static final int IRON_VEINS_PER_ISLAND = 7;
    public static final int GRAVEL_VEINS_PER_ISLAND = 4;
    public static final int RANDOM_VEIN_BASE_SIZE = 3;
    public static final int RANDOM_VEIN_EXTRA_SIZE = 2;
    public static final long ICE_BRIDGE_COOLDOWN_MILLIS = 30_000L;
    public static final long ICE_BRIDGE_DURATION_MILLIS = 4_000L;
    public static final double ICE_BRIDGE_LAUNCH_Y = 0.5D;
    public static final double FIRE_BURST_RADIUS_BLOCKS = 6.0D;
    public static final double FIRE_BURST_DAMAGE = 6.0D;
    public static final int FIRE_BURST_FIRE_TICKS = 100;
    public static final long KIT_FIRE_COOLDOWN_MILLIS = 30_000L;
    public static final double METAL_MAGNET_RANGE_BLOCKS = 12.0D;
    public static final long METAL_MAGNET_COOLDOWN_MILLIS = 15_000L;
    public static final double METAL_MAGNET_DAMAGE = 1.0D;
    public static final double METAL_MAGNET_PULL_BASE = 0.5D;
    public static final double METAL_MAGNET_PULL_PER_ARMOR = 0.25D;
    public static final long EARTH_THROW_COOLDOWN_MILLIS = 500L;
    public static final double EARTH_THROW_SPEED = 2.0D;

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

    public static long tntThrowFuseTicks() { return longRule("SKY_WARS_TNT_THROW_FUSE_TICKS", TNT_THROW_FUSE_TICKS); }
    public static long tntThrowCooldownMillis() { return longRule("SKY_WARS_TNT_THROW_COOLDOWN_MILLIS", TNT_THROW_COOLDOWN_MILLIS); }
    public static double tntThrowStrength() { return doubleRule("SKY_WARS_TNT_THROW_STRENGTH", TNT_THROW_STRENGTH, 0.0D, 8.0D); }
    public static double oreIslandRadiusBlocks() { return doubleRule("SKY_WARS_ORE_ISLAND_RADIUS_BLOCKS", ORE_ISLAND_RADIUS_BLOCKS, 1.0D, 32.0D); }
    public static int diamondVeinsPerIsland() { return boundedInt("SKY_WARS_DIAMOND_VEINS_PER_ISLAND", DIAMOND_VEINS_PER_ISLAND, 0, 32); }
    public static int diamondVeinSize() { return boundedInt("SKY_WARS_DIAMOND_VEIN_SIZE", DIAMOND_VEIN_SIZE, 1, 16); }
    public static int ironVeinsPerIsland() { return boundedInt("SKY_WARS_IRON_VEINS_PER_ISLAND", IRON_VEINS_PER_ISLAND, 0, 32); }
    public static int gravelVeinsPerIsland() { return boundedInt("SKY_WARS_GRAVEL_VEINS_PER_ISLAND", GRAVEL_VEINS_PER_ISLAND, 0, 32); }
    public static int randomVeinBaseSize() { return boundedInt("SKY_WARS_RANDOM_VEIN_BASE_SIZE", RANDOM_VEIN_BASE_SIZE, 1, 16); }
    public static int randomVeinExtraSize() { return boundedInt("SKY_WARS_RANDOM_VEIN_EXTRA_SIZE", RANDOM_VEIN_EXTRA_SIZE, 0, 16); }
    public static long iceBridgeCooldownMillis() { return longRule("SKY_WARS_ICE_BRIDGE_COOLDOWN_MILLIS", ICE_BRIDGE_COOLDOWN_MILLIS); }
    public static long iceBridgeDurationMillis() { return longRule("SKY_WARS_ICE_BRIDGE_DURATION_MILLIS", ICE_BRIDGE_DURATION_MILLIS); }
    public static double iceBridgeLaunchY() { return doubleRule("SKY_WARS_ICE_BRIDGE_LAUNCH_Y", ICE_BRIDGE_LAUNCH_Y, 0.0D, 3.0D); }
    public static double fireBurstRadiusBlocks() { return doubleRule("SKY_WARS_FIRE_BURST_RADIUS_BLOCKS", FIRE_BURST_RADIUS_BLOCKS, 0.0D, 32.0D); }
    public static double fireBurstDamage() { return doubleRule("SKY_WARS_FIRE_BURST_DAMAGE", FIRE_BURST_DAMAGE, 0.0D, 40.0D); }
    public static int fireBurstFireTicks() { return boundedInt("SKY_WARS_FIRE_BURST_FIRE_TICKS", FIRE_BURST_FIRE_TICKS, 0, 2_400); }
    public static long fireKitCooldownMillis() { return longRule("SKY_WARS_KIT_FIRE_COOLDOWN_MILLIS", KIT_FIRE_COOLDOWN_MILLIS); }
    public static double metalMagnetRangeBlocks() { return doubleRule("SKY_WARS_METAL_MAGNET_RANGE_BLOCKS", METAL_MAGNET_RANGE_BLOCKS, 0.0D, 64.0D); }
    public static long metalMagnetCooldownMillis() { return longRule("SKY_WARS_METAL_MAGNET_COOLDOWN_MILLIS", METAL_MAGNET_COOLDOWN_MILLIS); }
    public static double metalMagnetDamage() { return doubleRule("SKY_WARS_METAL_MAGNET_DAMAGE", METAL_MAGNET_DAMAGE, 0.0D, 20.0D); }
    public static double metalMagnetPullBase() { return doubleRule("SKY_WARS_METAL_MAGNET_PULL_BASE", METAL_MAGNET_PULL_BASE, 0.0D, 4.0D); }
    public static double metalMagnetPullPerArmor() { return doubleRule("SKY_WARS_METAL_MAGNET_PULL_PER_ARMOR", METAL_MAGNET_PULL_PER_ARMOR, 0.0D, 2.0D); }
    public static long earthThrowCooldownMillis() { return longRule("SKY_WARS_EARTH_THROW_COOLDOWN_MILLIS", EARTH_THROW_COOLDOWN_MILLIS); }
    public static double earthThrowSpeed() { return doubleRule("SKY_WARS_EARTH_THROW_SPEED", EARTH_THROW_SPEED, 0.0D, 8.0D); }

    private static int boundedInt(String key, int fallback, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, GameDebugRuntimeOverrides.intValue(key, fallback)));
    }

    private static double doubleRule(String key, double fallback, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, GameDebugRuntimeOverrides.doubleValue(key, fallback)));
    }

    private static int intRule(String key, int fallback) {
        return Math.max(1, GameDebugRuntimeOverrides.intValue(key, fallback));
    }

    private static long longRule(String key, long fallback) {
        return Math.max(1L, GameDebugRuntimeOverrides.longValue(key, fallback));
    }

    /** Source WorldData.GetCustomLocs("56") slots are assigned within eight horizontal blocks of an island spawn. */
    public static boolean isOreSlotInIsland(double slotX, double slotZ, double spawnX, double spawnZ) {
        if (!Double.isFinite(slotX) || !Double.isFinite(slotZ)
                || !Double.isFinite(spawnX) || !Double.isFinite(spawnZ)) return false;
        double dx = slotX - spawnX;
        double dz = slotZ - spawnZ;
        double radius = oreIslandRadiusBlocks();
        return dx * dx + dz * dz <= radius * radius;
    }

    /** Reborn's OreHider leaves ore exposed only when a surrounding block is non-occluding. */
    public static boolean shouldHideOre(boolean hasNonOccludingNeighbour) {
        return !hasNonOccludingNeighbour;
    }

    /** Iron and gravel veins are 3 + random(3), where random(3) is 0, 1 or 2. */
    public static int randomVeinSize(int randomRoll) {
        if (randomRoll < 0 || randomRoll > randomVeinExtraSize()) {
            throw new IllegalArgumentException("SkyWars vein random roll is outside the configured extra-size range.");
        }
        return randomVeinBaseSize() + Math.min(randomVeinExtraSize(), randomRoll);
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
