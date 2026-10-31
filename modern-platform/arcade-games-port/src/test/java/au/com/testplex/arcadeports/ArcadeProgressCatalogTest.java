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
                "evolution", "gladiators", "micro-battles", "monster-maze", "one-in-the-quiver",
                "paintball", "runner", "sheep-quest", "snake", "spleef", "turf-wars"};
        int[] achievementCounts = {5, 4, 2, 4, 6, 5, 4, 6, 9, 2, 5, 7, 8, 6, 7, 9};
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


    @Test
    void nextSevenGameMilestonesUseTheRequestedLifetimeThresholds() {
        assertArrayEquals(new long[] {25, 63, 125, 250}, stages("one-in-the-quiver", "wins"));
        assertArrayEquals(new long[] {500, 1250, 2500, 5000}, stages("paintball", "kills"));
        assertArrayEquals(new long[] {10000, 25000, 50000, 100000}, stages("runner", "blocks-run"));
        assertArrayEquals(new long[] {200, 500, 1000, 2000}, stages("sheep-quest", "kills"));
        assertArrayEquals(new long[] {75, 188, 375, 750}, stages("snake", "kills"));
        assertArrayEquals(new long[] {25000, 62500, 125000, 250000}, stages("spleef", "blocks-broken"));
        assertArrayEquals(new long[] {1000, 2500, 5000, 10000}, stages("turf-wars", "blocks-broken"));
    }

    @Test
    void newAchievementCatalogContainsAllRequestedNames() {
        assertTrue(hasAchievement("one-in-the-quiver", "heads", "HEADS!"));
        assertTrue(hasAchievement("paintball", "aura", "AURA"));
        assertTrue(hasAchievement("runner", "special-play", "Special Play"));
        assertTrue(hasAchievement("sheep-quest", "baaaad-to-the-bone", "Baaaaad to the bone"));
        assertTrue(hasAchievement("snake", "busted", "Busted"));
        assertTrue(hasAchievement("spleef", "no-mods-in-here", "No Mods in Here"));
        assertTrue(hasAchievement("turf-wars", "fair-game", "Fair Game"));
    }

    private boolean hasAchievement(String game, String id, String name) {
        return ArcadeProgressCatalog.achievements(game).stream()
                .anyMatch(entry -> entry.id().equals(id) && entry.name().equals(name));
    }

    private long[] stages(String gameId, String id) {
        return ArcadeProgressCatalog.milestones(gameId).stream()
                .filter(milestone -> milestone.id().equals(id))
                .findFirst().orElseThrow().stages();
    }
}
