package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CastleAssaultHeroesOfGwenMixedVariantTest {
    @Test
    void castleAssaultMixedArcadeHalvesObjectiveTntRespawnDelay() {
        assertEquals(60_000L, ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(false));
        assertEquals(30_000L, ArcadePortRules.castleAssaultObjectiveTntRespawnMillis(true));
    }

    @Test
    void heroesOfGwenMixedArcadeHalvesStoreCostWithoutChangingStandardPrices() {
        assertEquals(800, ArcadePortRules.heroesOfGwenStoreItemCost(800, false));
        assertEquals(400, ArcadePortRules.heroesOfGwenStoreItemCost(800, true));
        assertEquals(375, ArcadePortRules.heroesOfGwenStoreItemCost(750, true));
        assertEquals(0, ArcadePortRules.heroesOfGwenStoreItemCost(0, true));
        assertThrows(IllegalArgumentException.class,
                () -> ArcadePortRules.heroesOfGwenStoreItemCost(-1, true));
    }

    @Test
    void heroesOfGwenMixedArcadeSkipsOnlyTheMiddleTowerMarkers() {
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(false, true));
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(false, false));
        assertFalse(ArcadePortRules.heroesOfGwenShouldSpawnTower(true, true));
        assertTrue(ArcadePortRules.heroesOfGwenShouldSpawnTower(true, false));
    }

    @Test
    void heroesOfGwenMiddleTowerFilterRecognisesOnlyExplicitMiddleMarkers() {
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED MIDDLE"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER_BLUE_MID"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER CENTRE LANE"));
        // In the archived Heros Valley map, marker 1 is the central tower beside the beacons;
        // marker 2 is the base-side tower and remains in the Mixed Arcade variant.
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED 1"));
        assertTrue(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER BLUE 1"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER BLUE 2"));
        assertFalse(CastleAssaultHeroesOfGwenRuntime.isMiddleTowerMarker("TOWER RED 2"));
    }

    @Test
    void sourceMapDataExposesAuthoredTowerMarkerGroupsWithoutSharingMutableLists() {
        SourceMapCatalog.MapData data = SourceMapCatalog.parseMapConfig(null, List.of(
                "MAP_NAME: Heroes of GWEN",
                "CUSTOM_NAME: TOWER RED 1",
                "CUSTOM_LOCS: 10,64,20",
                "CUSTOM_NAME: TOWER RED MIDDLE",
                "CUSTOM_LOCS: 12,64,24"
        ));
        assertNotNull(data);
        assertEquals(1, data.customLocationGroups().get("TOWER RED 1").size());
        assertEquals(1, data.customLocationGroups().get("TOWER RED MIDDLE").size());
        data.customLocationGroups().get("TOWER RED 1").clear();
        assertEquals(1, data.customLocations("TOWER RED 1").size());
    }

    @Test
    void bothGamesResolveTheirOriginalSourceMapFoldersAndIds() {
        assertEquals(ArcadePortGame.CASTLE_ASSAULT, ArcadePortGame.fromId("castle-assault"));
        assertEquals(ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.fromId("Heroes of Gwen"));
        assertEquals(List.of("Castle Assault"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.CASTLE_ASSAULT));
        assertEquals(List.of("Heroes of GWEN"),
                SourceMapCatalog.mapFoldersFor(ArcadePortGame.HEROES_OF_GWEN));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(ArcadePortGame.CASTLE_ASSAULT,
                "source-maps/Castle Assault/CastleAssault_Castle Siege.zip"));
        assertTrue(SourceMapCatalog.resourcePathBelongsToGame(ArcadePortGame.HEROES_OF_GWEN,
                "source-maps/Heroes of GWEN/HOG_Heros_Valley.zip"));
    }
}
