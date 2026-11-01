package au.com.testplex.snake;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnakeMapCatalogTest {
    @Test
    void parsesOriginalWorldConfigTeamSpawnsBoundsAndSpectatorMarker() {
        String config = String.join("\n",
                "MAP_NAME: Snake Pit",
                "MIN_X: -48",
                "MAX_X: 48",
                "MIN_Z: -46",
                "MAX_Z: 46",
                "MIN_Y: 40",
                "MAX_Y: 90",
                "TEAM_NAME: Unassigned",
                "TEAM_SPAWNS: 1,64,2:10,64,12",
                "CUSTOM_NAME: Spectator",
                "CUSTOM_LOCS: 0,72,0");

        SnakeMapCatalog.MapData map = SnakeMapCatalog.parseMapConfig(null, config);
        assertEquals("Snake Pit", map.mapName());
        assertTrue(map.hasSpawnLocations());
        assertEquals(2, map.spawnLocations().size());
        assertEquals(1.5D, map.spawnLocations().get(0).getX(), 0.000001D);
        assertEquals(2.5D, map.spawnLocations().get(0).getZ(), 0.000001D);
        assertEquals(-42.0D, map.clampX(-200.0D, 6.0D), 0.000001D);
        assertEquals(40, map.minY());
        assertEquals(90, map.maxY());
        Location foodOrigin = map.foodSpawnOrigin();
        assertEquals(1.5D, foodOrigin.getX(), 0.000001D);
        assertEquals(0.0D, map.spectatorLocation().getX(), 0.000001D);
        assertEquals(0.0D, map.spectatorLocation().getZ(), 0.000001D);
    }

    @Test
    void rejectsWorldConfigsWithoutTeamSpawnMarkers() {
        SnakeMapCatalog.MapData map = SnakeMapCatalog.parseMapConfig(null, "MAP_NAME: Missing Starts\nMIN_X: -20\nMAX_X: 20");
        assertFalse(map.hasSpawnLocations());
        assertEquals("Missing Starts", map.mapName());
    }

    @Test
    void exposesAllOriginalSnakeMapNames() {
        assertEquals("Snake Pit", SnakeMapCatalog.displayName("source-maps/Snake/Snake_Pit.zip"));
        assertEquals("Snake Volcanic", SnakeMapCatalog.displayName("source-maps/Snake/Snake_Volcanic.zip"));
        assertEquals("Snake WinterBabylon", SnakeMapCatalog.displayName("source-maps/Snake/Snake_WinterBabylon.zip"));
    }
}
