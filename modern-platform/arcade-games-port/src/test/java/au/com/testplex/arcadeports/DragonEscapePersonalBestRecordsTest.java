package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DragonEscapePersonalBestRecordsTest {
    @Test
    void savesIndependentRecordsForAllFiveKitsWithoutReplacingTheOverallBest() {
        YamlConfiguration config = new YamlConfiguration();
        String base = "dragon-escape-pb.player-id.map-key.";
        config.set(base + "time-ms", 12_000L);
        for (String kit : DragonEscapePersonalBestViews.all().subList(1, 6)) {
            assertTrue(DragonEscapePersonalBestRecords.update(config, base, kit, 18_000L));
            assertTrue(DragonEscapePersonalBestRecords.update(config, base, kit, 17_000L));
            assertFalse(DragonEscapePersonalBestRecords.update(config, base, kit, 19_000L));
            assertEquals(17_000L, DragonEscapePersonalBestRecords.get(config, base, kit));
        }
        assertEquals(12_000L, config.getLong(base + "time-ms"));
        assertFalse(DragonEscapePersonalBestRecords.update(config, base, "unknown", 10_000L));
        assertFalse(DragonEscapePersonalBestRecords.update(config, base, "best-time", 10_000L));
        assertEquals(-1L, DragonEscapePersonalBestRecords.get(config, base, "unknown"));
    }

    @Test
    void recordsAreSpecificToEachMapAndNeverOverwriteExistingGhostSplits() {
        YamlConfiguration config = new YamlConfiguration();
        String first = "dragon-escape-pb.player-id.skylands.";
        String second = "dragon-escape-pb.player-id.pirate-bay.";
        config.set(first + "splits.1", 4_000L);
        assertTrue(DragonEscapePersonalBestRecords.update(config, first, "builder", 19_000L));
        assertTrue(DragonEscapePersonalBestRecords.update(config, second, "builder", 22_000L));
        assertEquals(19_000L, DragonEscapePersonalBestRecords.get(config, first, "builder"));
        assertEquals(22_000L, DragonEscapePersonalBestRecords.get(config, second, "builder"));
        assertEquals(4_000L, config.getLong(first + "splits.1"));
    }
}
