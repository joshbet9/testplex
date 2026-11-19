package me.monstermaze.game;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerInitialSpawnTest {
    @Test
    void startLocationUsesGeneratedMazeStandingHeightAndBlockCenter() {
        // MazeGenerator lays the center floor at center Y - 1 and centres X/Z at block + 0.5.
        Location center = new Location(null, 100.5D, 64.0D, -20.5D);
        Location spawn = GameManager.initialSpawnLocation(center);

        assertNotSame(center, spawn, "Keep the configured arena center unmodified");
        assertEquals(100.5D, spawn.getX(), 0.0D);
        assertEquals(64.0D, spawn.getY(), 0.0D);
        assertEquals(-20.5D, spawn.getZ(), 0.0D);
        spawn.add(4, 3, 5);
        assertEquals(64.0D, center.getY(), 0.0D);
    }

    @Test
    void missingCenterHasNoFallbackTeleportLocation() {
        assertNull(GameManager.initialSpawnLocation(null));
    }
}
