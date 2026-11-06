package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SkyWarsMixedArcadeRulesTest {
    @AfterEach void clearDebugOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void debugProfileValuesChangeSkyWarsQueueAndRoundRulesAtRuntime() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "SKY_WARS_MAX_PLAYERS", 12,
                "SKY_WARS_PREPARE_MILLIS", 5_000L,
                "SKY_WARS_ROUND_MILLIS", 60_000L,
                "SKY_WARS_CRUMBLE_DELAY_MILLIS", 30_000L,
                "SKY_WARS_TNT_GENERATOR_INTERVAL_MILLIS", 10_000L,
                "SKY_WARS_TNT_PER_DROP", 4));

        assertEquals(12, SkyWarsMixedArcadeRules.acceptedPlayers(16));
        assertFalse(SkyWarsMixedArcadeRules.hasRoom(12));
        assertEquals(5_000L, SkyWarsMixedArcadeRules.prepareMillis());
        assertEquals(60_000L, SkyWarsMixedArcadeRules.roundMillis());
        assertEquals(30_000L, SkyWarsMixedArcadeRules.crumbleDelayMillis());
        assertEquals(10_000L, SkyWarsMixedArcadeRules.tntGeneratorIntervalMillis());
        assertEquals(4, SkyWarsMixedArcadeRules.tntPerDrop());

        GameDebugRuntimeOverrides.activate(Map.of("SKY_WARS_MAX_PLAYERS", 90,
                "SKY_WARS_TNT_PER_DROP", 200));
        assertEquals(16, SkyWarsMixedArcadeRules.maxPlayers());
        assertEquals(64, SkyWarsMixedArcadeRules.tntPerDrop());

        GameDebugRuntimeOverrides.clear();
        assertEquals(16, SkyWarsMixedArcadeRules.maxPlayers());
    }

    @Test void mixedSkyWarsCapsLobbiesAtSixteen() {
        assertEquals(16, SkyWarsMixedArcadeRules.acceptedPlayers(16));
        assertEquals(16, SkyWarsMixedArcadeRules.acceptedPlayers(20));
        assertEquals(1, SkyWarsMixedArcadeRules.acceptedPlayers(1));
        assertEquals(0, SkyWarsMixedArcadeRules.acceptedPlayers(-1));
        assertTrue(SkyWarsMixedArcadeRules.hasRoom(15));
        assertFalse(SkyWarsMixedArcadeRules.hasRoom(16));
    }

    @Test void sourceIslandOreSlotsUseTheEightBlockHorizontalRadius() {
        assertTrue(SkyWarsMixedArcadeRules.isOreSlotInIsland(8.0D, 0.0D, 0.0D, 0.0D));
        assertTrue(SkyWarsMixedArcadeRules.isOreSlotInIsland(4.0D, 4.0D, 0.0D, 0.0D));
        assertFalse(SkyWarsMixedArcadeRules.isOreSlotInIsland(8.01D, 0.0D, 0.0D, 0.0D));
        assertFalse(SkyWarsMixedArcadeRules.isOreSlotInIsland(0.0D, -8.01D, 0.0D, 0.0D));
    }

    @Test void sourceOreHiderExposesOreOnlyBesideNonOccludingBlocks() {
        assertTrue(SkyWarsMixedArcadeRules.shouldHideOre(false));
        assertFalse(SkyWarsMixedArcadeRules.shouldHideOre(true));
    }

    @Test void sourcePerIslandVeinCountsAndSizesArePreserved() {
        assertEquals(3, SkyWarsMixedArcadeRules.DIAMOND_VEINS_PER_ISLAND);
        assertEquals(2, SkyWarsMixedArcadeRules.DIAMOND_VEIN_SIZE);
        assertEquals(7, SkyWarsMixedArcadeRules.IRON_VEINS_PER_ISLAND);
        assertEquals(4, SkyWarsMixedArcadeRules.GRAVEL_VEINS_PER_ISLAND);
        assertEquals(3, SkyWarsMixedArcadeRules.randomVeinSize(0));
        assertEquals(4, SkyWarsMixedArcadeRules.randomVeinSize(1));
        assertEquals(5, SkyWarsMixedArcadeRules.randomVeinSize(2));
        assertThrows(IllegalArgumentException.class, () -> SkyWarsMixedArcadeRules.randomVeinSize(3));
    }

    @Test void sourceBonusDropsCoverWebGravelAndIronOreOnly() {
        assertTrue(SkyWarsMixedArcadeRules.hasSourceBonusDrop("COBWEB"));
        assertTrue(SkyWarsMixedArcadeRules.hasSourceBonusDrop("GRAVEL"));
        assertTrue(SkyWarsMixedArcadeRules.hasSourceBonusDrop("IRON_ORE"));
        assertFalse(SkyWarsMixedArcadeRules.hasSourceBonusDrop("STONE"));
        assertFalse(SkyWarsMixedArcadeRules.hasSourceBonusDrop("DIAMOND_ORE"));
    }

    @Test void sourcePlacementCapAndForbiddenInventoryBlocksAreEnforced() {
        assertFalse(SkyWarsMixedArcadeRules.shouldBlockSkyWarsPlacement("OAK_PLANKS", 100, 128));
        assertTrue(SkyWarsMixedArcadeRules.shouldBlockSkyWarsPlacement("OAK_PLANKS", 125, 128));
        assertTrue(SkyWarsMixedArcadeRules.shouldBlockSkyWarsPlacement("CHEST", 64, 128));
        assertTrue(SkyWarsMixedArcadeRules.shouldBlockSkyWarsPlacement("HOPPER", 64, 128));
        assertTrue(SkyWarsMixedArcadeRules.shouldBlockSkyWarsPlacement("STICKY_PISTON", 64, 128));
    }

    @Test void mixedSkyWarsKeepsSourceRoundCadence() {
        long start = 1_000_000L;
        assertEquals(start + 10_000L, SkyWarsMixedArcadeRules.liveStartsAt(start));
        assertEquals(start + 110_000L, SkyWarsMixedArcadeRules.crumbleStartsAt(start));
        assertEquals(start + 40_000L, SkyWarsMixedArcadeRules.firstTntAt(start));
        assertEquals(start + 1_210_000L, SkyWarsMixedArcadeRules.roundEndsAt(start));
        assertEquals(60L, SkyWarsMixedArcadeRules.TNT_THROW_FUSE_TICKS);
        assertEquals(500L, SkyWarsMixedArcadeRules.TNT_THROW_COOLDOWN_MILLIS);
        assertEquals(1.6D, SkyWarsMixedArcadeRules.TNT_THROW_STRENGTH);
        assertEquals(2, SkyWarsMixedArcadeRules.TNT_PER_DROP);
    }

    @Test void abilityAndResourceProfileValuesChangeProductionRulesAndRestoreDefaults() {
        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SKY_WARS_ORE_ISLAND_RADIUS_BLOCKS", 12.0D),
                Map.entry("SKY_WARS_DIAMOND_VEINS_PER_ISLAND", 5),
                Map.entry("SKY_WARS_TNT_THROW_STRENGTH", 2.5D),
                Map.entry("SKY_WARS_ICE_BRIDGE_DURATION_MILLIS", 6_000L),
                Map.entry("SKY_WARS_FIRE_BURST_DAMAGE", 9.0D),
                Map.entry("SKY_WARS_METAL_MAGNET_PULL_PER_ARMOR", 0.5D),
                Map.entry("SKY_WARS_EARTH_THROW_SPEED", 3.0D)));
        assertTrue(SkyWarsMixedArcadeRules.isOreSlotInIsland(10.0D, 0.0D, 0.0D, 0.0D));
        assertEquals(5, SkyWarsMixedArcadeRules.diamondVeinsPerIsland());
        assertEquals(2.5D, SkyWarsMixedArcadeRules.tntThrowStrength());
        assertEquals(6_000L, SkyWarsMixedArcadeRules.iceBridgeDurationMillis());
        assertEquals(9.0D, SkyWarsMixedArcadeRules.fireBurstDamage());
        assertEquals(0.5D, SkyWarsMixedArcadeRules.metalMagnetPullPerArmor());
        assertEquals(3.0D, SkyWarsMixedArcadeRules.earthThrowSpeed());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertFalse(SkyWarsMixedArcadeRules.isOreSlotInIsland(10.0D, 0.0D, 0.0D, 0.0D));
        assertEquals(3, SkyWarsMixedArcadeRules.diamondVeinsPerIsland());
        assertEquals(1.6D, SkyWarsMixedArcadeRules.tntThrowStrength());
        assertEquals(4_000L, SkyWarsMixedArcadeRules.iceBridgeDurationMillis());
        assertEquals(6.0D, SkyWarsMixedArcadeRules.fireBurstDamage());
        assertEquals(0.25D, SkyWarsMixedArcadeRules.metalMagnetPullPerArmor());
        assertEquals(2.0D, SkyWarsMixedArcadeRules.earthThrowSpeed());
    }

    @Test void newSkyWarsControlsAreListedWithBoundedGameplayRanges() {
        assertVariable("SKY_WARS_FIRE_BURST_DAMAGE", 0.0D, 40.0D);
        assertVariable("SKY_WARS_ORE_ISLAND_RADIUS_BLOCKS", 1.0D, 32.0D);
        assertVariable("SKY_WARS_TNT_THROW_STRENGTH", 0.0D, 8.0D);
        assertVariable("SKY_WARS_ICE_BRIDGE_DURATION_MILLIS", 1.0D, 120_000.0D);
        assertVariable("SKY_WARS_EARTH_THROW_SPEED", 0.0D, 8.0D);
        assertVariable("SKY_WARS_AIR_DOUBLE_JUMP_COOLDOWN_MILLIS", 1.0D, 120_000.0D);
        assertVariable("SKY_WARS_EARTH_MAX_DIRT", 1.0D, 64.0D);
        assertVariable("SKY_WARS_CRUMBLE_BLOCKS_PER_TICK", 1.0D, 256.0D);
        assertVariable("SKY_WARS_CRUMBLE_FALLING_BLOCK_CHANCE", 0.0D, 1.0D);
        assertVariable("SKY_WARS_GUARDIAN_HEALTH", 1.0D, 100.0D);
        assertVariable("SKY_WARS_DIRT_PROJECTILE_DAMAGE", 0.0D, 40.0D);
        assertVariable("SKY_WARS_DIRT_PROJECTILE_KNOCKBACK_HORIZONTAL_MULTIPLIER", 0.0D, 8.0D);
        assertVariable("SKY_WARS_AIR_SAFE_RETURN_VERTICAL_OFFSET", 0.0D, 8.0D);
        assertVariable("SKY_WARS_ICE_BRIDGE_LENGTH_BLOCKS", 1.0D, 64.0D);
        assertVariable("SKY_WARS_ICE_BRIDGE_HALF_WIDTH_BLOCKS", 0.0D, 8.0D);
        assertVariable("SKY_WARS_ICE_BRIDGE_BLOCKS_PER_TICK", 1.0D, 64.0D);
    }

    @Test void unprofiledKitAndMapConstantsAreAdjustableAndResetToTheSourceDefaults() {
        assertEquals(25_000L, SkyWarsMixedArcadeRules.airDoubleJumpCooldownMillis());
        assertEquals(1.0D, SkyWarsMixedArcadeRules.airDoubleJumpMinVertical());
        assertEquals(0.8D, SkyWarsMixedArcadeRules.airDoubleJumpVerticalAdd());
        assertEquals(20_000L, SkyWarsMixedArcadeRules.earthDirtRefillIntervalMillis());
        assertEquals(4, SkyWarsMixedArcadeRules.earthMaxDirt());
        assertEquals(1.0D, SkyWarsMixedArcadeRules.metalArmorHealthPerPiece());
        assertEquals(0.6D, SkyWarsMixedArcadeRules.metalPullMinVertical());
        assertEquals(1.2D, SkyWarsMixedArcadeRules.dirtProjectileHitRadiusSquared());
        assertEquals(1.0D, SkyWarsMixedArcadeRules.dirtProjectileKnockbackHorizontalMultiplier());
        assertEquals(2.0D, SkyWarsMixedArcadeRules.airSafeReturnVerticalOffset());
        assertEquals(16, SkyWarsMixedArcadeRules.iceBridgeLengthBlocks());
        assertEquals(1, SkyWarsMixedArcadeRules.iceBridgeHalfWidthBlocks());
        assertEquals(1, SkyWarsMixedArcadeRules.iceBridgeBlocksPerTick());
        Vector defaultDirtKnockback = SkyWarsMixedArcadeRules.dirtProjectileKnockback(new Vector(1.0D, 0.0D, 0.0D));
        assertEquals(1.0D, defaultDirtKnockback.getX());
        assertEquals(0.5D, defaultDirtKnockback.getY());
        assertEquals(4, SkyWarsMixedArcadeRules.crumbleBlocksPerTick());
        assertEquals(0.05D, SkyWarsMixedArcadeRules.crumbleFallingBlockChance());
        assertEquals(15.0D, SkyWarsMixedArcadeRules.guardianHealth());
        assertEquals(8.0D, SkyWarsMixedArcadeRules.guardianExplosionDamage());
        assertEquals(3, SkyWarsMixedArcadeRules.deathBomberKills());
        assertTrue(SkyWarsMixedArcadeRules.shouldHideOre(false, true));
        assertFalse(SkyWarsMixedArcadeRules.shouldHideOre(false, false));

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SKY_WARS_AIR_DOUBLE_JUMP_COOLDOWN_MILLIS", 5_000L),
                Map.entry("SKY_WARS_AIR_DOUBLE_JUMP_MIN_VERTICAL", 2.0D),
                Map.entry("SKY_WARS_AIR_DOUBLE_JUMP_VERTICAL_ADD", 1.2D),
                Map.entry("SKY_WARS_EARTH_DIRT_REFILL_INTERVAL_MILLIS", 7_500L),
                Map.entry("SKY_WARS_EARTH_MAX_DIRT", 8),
                Map.entry("SKY_WARS_METAL_ARMOR_HEALTH_PER_PIECE", 2.0D),
                Map.entry("SKY_WARS_METAL_PULL_MIN_VERTICAL", 1.1D),
                Map.entry("SKY_WARS_DIRT_PROJECTILE_HIT_RADIUS_SQUARED", 4.0D),
                Map.entry("SKY_WARS_DIRT_PROJECTILE_DAMAGE", 6.0D),
                Map.entry("SKY_WARS_DIRT_PROJECTILE_KNOCKBACK_HORIZONTAL_MULTIPLIER", 2.5D),
                Map.entry("SKY_WARS_DIRT_PROJECTILE_KNOCKBACK_VERTICAL", 1.0D),
                Map.entry("SKY_WARS_AIR_SAFE_RETURN_VERTICAL_OFFSET", 3.0D),
                Map.entry("SKY_WARS_ICE_BRIDGE_LENGTH_BLOCKS", 24),
                Map.entry("SKY_WARS_ICE_BRIDGE_HALF_WIDTH_BLOCKS", 2),
                Map.entry("SKY_WARS_ICE_BRIDGE_BLOCKS_PER_TICK", 4),
                Map.entry("SKY_WARS_CRUMBLE_LAYERS_PER_TICK", 3),
                Map.entry("SKY_WARS_CRUMBLE_BLOCKS_PER_TICK", 12),
                Map.entry("SKY_WARS_CRUMBLE_FALLING_BLOCK_CHANCE", 0.25D),
                Map.entry("SKY_WARS_GUARDIAN_HEALTH", 25.0D),
                Map.entry("SKY_WARS_GUARDIAN_EXPLOSION_DAMAGE", 12.0D),
                Map.entry("SKY_WARS_GUARDIAN_HOME_LEASH_RADIUS_BLOCKS", 16.0D),
                Map.entry("SKY_WARS_GUARDIAN_HOME_SPEED", 1.5D),
                Map.entry("SKY_WARS_DEATH_BOMBER_KILLS", 1)));
        assertEquals(5_000L, SkyWarsMixedArcadeRules.airDoubleJumpCooldownMillis());
        assertEquals(2.0D, SkyWarsMixedArcadeRules.airDoubleJumpMinVertical());
        assertEquals(1.2D, SkyWarsMixedArcadeRules.airDoubleJumpVerticalAdd());
        assertEquals(7_500L, SkyWarsMixedArcadeRules.earthDirtRefillIntervalMillis());
        assertEquals(8, SkyWarsMixedArcadeRules.earthMaxDirt());
        assertEquals(2.0D, SkyWarsMixedArcadeRules.metalArmorHealthPerPiece());
        assertEquals(1.1D, SkyWarsMixedArcadeRules.metalPullMinVertical());
        assertEquals(4.0D, SkyWarsMixedArcadeRules.dirtProjectileHitRadiusSquared());
        assertEquals(6.0D, SkyWarsMixedArcadeRules.dirtProjectileDamage());
        assertEquals(2.5D, SkyWarsMixedArcadeRules.dirtProjectileKnockbackHorizontalMultiplier());
        assertEquals(1.0D, SkyWarsMixedArcadeRules.dirtProjectileKnockbackVertical());
        assertEquals(3.0D, SkyWarsMixedArcadeRules.airSafeReturnVerticalOffset());
        assertEquals(24, SkyWarsMixedArcadeRules.iceBridgeLengthBlocks());
        assertEquals(2, SkyWarsMixedArcadeRules.iceBridgeHalfWidthBlocks());
        assertEquals(4, SkyWarsMixedArcadeRules.iceBridgeBlocksPerTick());
        Vector adjustedDirtKnockback = SkyWarsMixedArcadeRules.dirtProjectileKnockback(new Vector(1.0D, 0.0D, 0.0D));
        assertEquals(2.5D, adjustedDirtKnockback.getX());
        assertEquals(1.0D, adjustedDirtKnockback.getY());
        assertEquals(3, SkyWarsMixedArcadeRules.crumbleLayersPerTick());
        assertEquals(12, SkyWarsMixedArcadeRules.crumbleBlocksPerTick());
        assertEquals(0.25D, SkyWarsMixedArcadeRules.crumbleFallingBlockChance());
        assertEquals(25.0D, SkyWarsMixedArcadeRules.guardianHealth());
        assertEquals(12.0D, SkyWarsMixedArcadeRules.guardianExplosionDamage());
        assertEquals(16.0D, SkyWarsMixedArcadeRules.guardianHomeLeashRadiusBlocks());
        assertEquals(1.5D, SkyWarsMixedArcadeRules.guardianHomeSpeed());
        assertEquals(1, SkyWarsMixedArcadeRules.deathBomberKills());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(25_000L, SkyWarsMixedArcadeRules.airDoubleJumpCooldownMillis());
        assertEquals(1.0D, SkyWarsMixedArcadeRules.dirtProjectileKnockbackHorizontalMultiplier());
        assertEquals(2.0D, SkyWarsMixedArcadeRules.airSafeReturnVerticalOffset());
        assertEquals(16, SkyWarsMixedArcadeRules.iceBridgeLengthBlocks());
        assertEquals(1, SkyWarsMixedArcadeRules.iceBridgeHalfWidthBlocks());
        assertEquals(1, SkyWarsMixedArcadeRules.iceBridgeBlocksPerTick());
        assertEquals(4, SkyWarsMixedArcadeRules.earthMaxDirt());
        assertEquals(4, SkyWarsMixedArcadeRules.crumbleBlocksPerTick());
        assertEquals(15.0D, SkyWarsMixedArcadeRules.guardianHealth());
        assertEquals(3, SkyWarsMixedArcadeRules.deathBomberKills());
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.SKY_WARS, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
        assertTrue(GameDebugVariableCatalog.clamp(variable, maximum) >= minimum, key);
    }
}
