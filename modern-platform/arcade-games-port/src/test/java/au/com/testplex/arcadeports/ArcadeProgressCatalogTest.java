package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ArcadeProgressCatalogTest {
    @Test
    void monsterMazeUsesTheRequestedMilestoneThresholds() {
        assertArrayEquals(new long[] {25, 50, 100, 250}, stages("monster-maze", "wins"));
        assertArrayEquals(new long[] {100, 250, 500, 1000}, stages("monster-maze", "first-safe-pads"));
        assertArrayEquals(new long[] {12, 15, 18, 21}, stages("monster-maze", "highest-stage"));
    }

    @Test
    void otherMilestonesHaveFourProgressStagesAndReadPerPlayerValues() {
        var milestone = ArcadeProgressCatalog.milestones("bacon-brawl").get(0);
        assertArrayEquals(new long[] {25, 63, 125, 250}, milestone.stages());
        UUID player = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        config.set("local-stats." + player + ".bacon-brawl.wins", 77);
        assertEquals(77, milestone.value(config, player, "bacon-brawl"));
        assertEquals(0, milestone.value(config, UUID.randomUUID(), "bacon-brawl"));
    }

    @Test
    void firstBatchCatalogContainsAllProvidedGameCriteria() {
        String[] games = {"bacon-brawl", "bomb-lobbers", "death-tag", "dragon-escape", "dragons",
                "evolution", "gladiators", "micro-battles", "monster-maze", "super-sploor"};
        int[] achievementCounts = {5, 4, 2, 4, 6, 5, 4, 6, 9, 15};
        for (int i = 0; i < games.length; i++) {
            assertTrue(ArcadeProgressCatalog.milestones(games[i]).size() >= 2, games[i]);
            assertTrue(ArcadeProgressCatalog.achievements(games[i]).size() >= achievementCounts[i], games[i]);
        }
    }

    @Test
    void superSploorMilestonesUseFourExplicitStages() {
        assertArrayEquals(new long[] {10, 25, 100, 250}, stages("super-sploor", "wins"));
        assertArrayEquals(new long[] {1_000, 10_000, 50_000, 250_000},
                stages("super-sploor", "territory-painted"));
        assertArrayEquals(new long[] {250, 2_000, 10_000, 50_000},
                stages("super-sploor", "enemy-territory"));
        assertArrayEquals(new long[] {250, 2_500, 25_000, 100_000},
                stages("super-sploor", "squid-travel"));
        assertEquals(15, ArcadeProgressCatalog.achievements("super-sploor").size());
        assertEquals(7, ArcadeProgressCatalog.milestones("super-sploor").size());
    }

    private long[] stages(String gameId, String id) {
        return ArcadeProgressCatalog.milestones(gameId).stream()
                .filter(milestone -> milestone.id().equals(id))
                .findFirst().orElseThrow().stages();
    }
}
