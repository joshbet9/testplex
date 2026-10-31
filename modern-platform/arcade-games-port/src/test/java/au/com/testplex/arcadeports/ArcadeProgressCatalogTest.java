package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                "evolution", "gladiators", "micro-battles", "monster-maze"};
        int[] achievementCounts = {5, 4, 2, 4, 6, 5, 4, 6, 9};
        for (int i = 0; i < games.length; i++) {
            assertTrue(ArcadeProgressCatalog.milestones(games[i]).size() >= 2, games[i]);
            assertTrue(ArcadeProgressCatalog.achievements(games[i]).size() >= achievementCounts[i], games[i]);
        }
    }

    @Test
    void grenadeFromTheGraveRequiresTwoKillsAfterElimination() {
        var achievement = ArcadeProgressCatalog.achievements("bomb-lobbers").stream()
                .filter(entry -> entry.id().equals("grenade-from-the-grave"))
                .findFirst().orElseThrow();
        assertEquals("Grenade From the Grave", achievement.name());
        assertFalse(BombLobbersAchievement.shouldAwardGrenadeFromTheGrave(0));
        assertFalse(BombLobbersAchievement.shouldAwardGrenadeFromTheGrave(1));
        assertTrue(BombLobbersAchievement.shouldAwardGrenadeFromTheGrave(2));
        assertTrue(BombLobbersAchievement.shouldAwardGrenadeFromTheGrave(3));
    }

    private long[] stages(String gameId, String id) {
        return ArcadeProgressCatalog.milestones(gameId).stream()
                .filter(milestone -> milestone.id().equals(id))
                .findFirst().orElseThrow().stages();
    }
}
