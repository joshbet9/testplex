package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
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
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.SKY_WARS, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
        assertTrue(GameDebugVariableCatalog.clamp(variable, maximum) >= minimum, key);
    }
}
