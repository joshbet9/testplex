package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyWarsMixedArcadeRulesTest {
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

    @Test void mixedSkyWarsUsesShorterRoundCadence() {
        long start = 1_000_000L;
        assertEquals(start + 5_000L, SkyWarsMixedArcadeRules.liveStartsAt(start));
        assertEquals(start + 65_000L, SkyWarsMixedArcadeRules.crumbleStartsAt(start));
        assertEquals(start + 20_000L, SkyWarsMixedArcadeRules.firstTntAt(start));
        assertEquals(start + 485_000L, SkyWarsMixedArcadeRules.roundEndsAt(start));
        assertEquals(45_000L, SkyWarsMixedArcadeRules.BORDER_SHRINK_MILLIS);
        assertEquals(32.0D, SkyWarsMixedArcadeRules.FINAL_BORDER_SIZE);
        assertEquals(60L, SkyWarsMixedArcadeRules.TNT_THROW_FUSE_TICKS);
        assertEquals(500L, SkyWarsMixedArcadeRules.TNT_THROW_COOLDOWN_MILLIS);
        assertEquals(1.6D, SkyWarsMixedArcadeRules.TNT_THROW_STRENGTH);
        assertEquals(2, SkyWarsMixedArcadeRules.TNT_PER_DROP);
    }
}
