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
    void mageStarterSpellsUseOriginalTenPointLootWeightRoll() {
        assertEquals(true, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 0, 1));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 4, 4));
        assertEquals(true, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 9, 10));
        assertEquals(true, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 9, 15));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.SORCERER, 0, 15));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MYSTIC, 0, 15));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.WITCH_DOCTOR, 0, 15));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 10, 15));
        assertEquals(false, WizardsRules.mageStarterSpellRollSucceeds(WizardsKit.MAGE, 0, 0));
        assertEquals(2, WizardsRules.MAGE_RANDOM_STARTER_SPELL_COUNT);
        assertEquals(100, WizardsRules.MAGE_STARTER_SPELL_SELECTION_ATTEMPTS);
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
