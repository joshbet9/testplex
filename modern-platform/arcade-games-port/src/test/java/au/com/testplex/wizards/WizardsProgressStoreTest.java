package au.com.testplex.wizards;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WizardsProgressStoreTest {
    @Test
    void writesTheWinsKillsAndSpellsBoundKeysUsedByArcadeProgress() {
        YamlConfiguration config = new YamlConfiguration();
        UUID player = UUID.fromString("4c9fb0d2-b947-4fb7-a2fd-41d8cbb6bc5d");

        WizardsProgressStore.increment(config, player, "wins", 1L);
        WizardsProgressStore.increment(config, player, "kills", 3L);
        WizardsProgressStore.increment(config, player, "spells-bound", 7L);
        WizardsProgressStore.increment(config, player, "kills", 2L);

        String root = "local-stats." + player + ".wizards.";
        assertEquals(1L, config.getLong(root + "wins"));
        assertEquals(5L, config.getLong(root + "kills"));
        assertEquals(7L, config.getLong(root + "spells-bound"));
    }

    @Test
    void ignoresUnknownStatsAndNonPositiveIncrements() {
        YamlConfiguration config = new YamlConfiguration();
        UUID player = UUID.fromString("4c9fb0d2-b947-4fb7-a2fd-41d8cbb6bc5d");

        WizardsProgressStore.increment(config, player, "wins", 0L);
        WizardsProgressStore.increment(config, player, "wins", -1L);
        WizardsProgressStore.increment(config, player, "games-played", 4L);

        String root = "local-stats." + player + ".wizards.";
        assertEquals(0L, config.getLong(root + "wins"));
        assertEquals(0L, config.getLong(root + "games-played"));
    }
}
