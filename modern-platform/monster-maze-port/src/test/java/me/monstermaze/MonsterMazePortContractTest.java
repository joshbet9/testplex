package me.monstermaze;

import me.monstermaze.game.MazeMode;
import me.monstermaze.kit.KitType;
import me.monstermaze.maze.MazeLayouts;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
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

    @Test void shipsSafeIsolatedTestDefaults() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(input, "config.yml must be packaged with MonsterMazeModern.jar");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            assertTrue(config.getBoolean("test-mode", false),
                    "The modern test port must keep production backend writes disabled by default");
            assertFalse(config.getBoolean("record-runs", true),
                    "Test runs must not be recorded to the production backend by default");
        }
    }

    @Test void configuredMapsUseTheSevenSourceWorldFoldersProvisionedByDeployment() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("maps.yml")) {
            assertNotNull(input, "maps.yml must be packaged with MonsterMazeModern.jar");
            YamlConfiguration maps = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            Set<String> mapNames = maps.getKeys(false);
            assertEquals(Set.of("eyeofender", "volcano", "colombia", "sandycoast",
                    "siberian", "swampland", "tesorohundido"), mapNames);

            Set<String> worldFolders = mapNames.stream()
                    .map(name -> maps.getString(name + ".world-folder", "mm_void"))
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(Set.of("mm_void", "mm_volcano", "mm_colombia", "mm_sandycoast",
                    "mm_siberian", "mm_swampland", "mm_tesorohundido"), worldFolders);
        }
    }

}