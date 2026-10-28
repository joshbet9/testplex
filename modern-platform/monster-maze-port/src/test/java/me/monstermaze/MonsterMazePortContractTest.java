package me.monstermaze;

import me.monstermaze.game.MazeMode;
import me.monstermaze.kit.KitType;
import me.monstermaze.maze.MazeLayouts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonsterMazePortContractTest {
    @Test void preservesTheThreeSourceGameModes() {
        assertEquals(MazeMode.ORIGINAL, MazeMode.byName("Original"));
        assertEquals(MazeMode.MODERN, MazeMode.byName("Modern"));
        assertEquals(MazeMode.CLASSIC, MazeMode.byName("Classic"));
    }
    @Test void preservesKitAvailabilityRules() {
        assertEquals(4, KitType.available(MazeMode.ORIGINAL).size());
        assertFalse(KitType.available(MazeMode.ORIGINAL).contains(KitType.MAVERICK));
        assertTrue(KitType.available(MazeMode.CLASSIC).contains(KitType.MAVERICK));
        assertEquals(KitType.BODY_BUILDER, KitType.byName("body builder"));
    }
    @Test void shipsTheThreeAuthoredMazePatterns() {
        assertEquals(3, MazeLayouts.ALL_MAZES.length);
        for (int[][] pattern : MazeLayouts.ALL_MAZES) {
            assertNotNull(pattern);
            assertTrue(pattern.length > 0);
        }
    }
}