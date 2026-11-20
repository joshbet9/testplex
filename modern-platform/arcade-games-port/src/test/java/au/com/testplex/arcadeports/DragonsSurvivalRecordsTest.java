package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonsSurvivalRecordsTest {
    @Test
    void recordsOnlyIncreasingSurvivalTimesAndIsolatesMapAndKit() {
        YamlConfiguration config = new YamlConfiguration();
        UUID player = UUID.randomUUID();
        assertFalse(DragonsSurvivalRecords.update(config, player, "maps/dragons.zip", "coward", 0L));
        assertTrue(DragonsSurvivalRecords.update(config, player, "maps/dragons.zip", "coward", 60_000L));
        assertFalse(DragonsSurvivalRecords.update(config, player, "maps/dragons.zip", "coward", 59_999L));
        assertTrue(DragonsSurvivalRecords.update(config, player, "maps/dragons.zip", "marksman", 45_000L));
        assertTrue(DragonsSurvivalRecords.update(config, player, "source-maps/Dragons/Other.zip", "coward", 30_000L));
        assertEquals(60_000L, DragonsSurvivalRecords.get(config, player, "maps/dragons.zip", "coward"));
        assertEquals(45_000L, DragonsSurvivalRecords.get(config, player, "maps/dragons.zip", "marksman"));
        assertEquals(30_000L, DragonsSurvivalRecords.get(config, player, "source-maps/Dragons/Other.zip", "coward"));
    }

    @Test
    void ranksLongerRunsFirstWithCompetitionTiesAndBestPerPlayer() {
        UUID first = UUID.randomUUID();
        UUID tied = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<DragonsSurvivalRecords.RankedRun> ranked = DragonsSurvivalRecords.rank(List.of(
                new DragonsSurvivalRecords.Run(first, 60_000L),
                new DragonsSurvivalRecords.Run(first, 55_000L),
                new DragonsSurvivalRecords.Run(first, 61_000L),
                new DragonsSurvivalRecords.Run(tied, 61_000L),
                new DragonsSurvivalRecords.Run(third, 40_000L),
                new DragonsSurvivalRecords.Run(UUID.randomUUID(), -1L)));
        assertEquals(3, ranked.size());
        assertEquals(1, ranked.get(0).rank());
        assertEquals(61_000L, ranked.get(0).elapsedMillis());
        assertEquals(1, ranked.get(1).rank());
        assertEquals(3, ranked.get(2).rank());
        assertEquals(40_000L, ranked.get(2).elapsedMillis());
    }
}
