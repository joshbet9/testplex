package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpeedBuildersProgressStoreTest {
    @Test
    void persistsAllThreeSpeedBuildersMilestoneCounters() {
        YamlConfiguration config = new YamlConfiguration();
        UUID player = UUID.fromString("d91366e2-e997-4ae4-a411-4502fd4d2207");

        SpeedBuildersProgressStore.increment(config, player, "wins", 1);
        SpeedBuildersProgressStore.increment(config, player, "perfect-builds", 2);
        SpeedBuildersProgressStore.increment(config, player, "blocks-placed", 37);
        SpeedBuildersProgressStore.increment(config, player, "blocks-placed", 5);

        String root = "local-stats." + player + ".speed-builders-arcade.";
        assertEquals(1L, config.getLong(root + "wins"));
        assertEquals(2L, config.getLong(root + "perfect-builds"));
        assertEquals(42L, config.getLong(root + "blocks-placed"));
    }

    @Test
    void rejectsUnknownStatsAndNonPositiveChanges() {
        YamlConfiguration config = new YamlConfiguration();
        UUID player = UUID.fromString("d91366e2-e997-4ae4-a411-4502fd4d2207");

        SpeedBuildersProgressStore.increment(config, player, "wins", 0);
        SpeedBuildersProgressStore.increment(config, player, "wins", -1);
        SpeedBuildersProgressStore.increment(config, player, "losses", 4);

        String root = "local-stats." + player + ".speed-builders-arcade.";
        assertEquals(0L, config.getLong(root + "wins"));
        assertEquals(0L, config.getLong(root + "losses"));
    }
}
