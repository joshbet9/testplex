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
        int[] achievementCounts = {5, 4, 3, 4, 3, 5, 4, 6, 9, 2, 5, 7, 8, 6, 7, 9};
        for (int i = 0; i < games.length; i++) {
            assertTrue(ArcadeProgressCatalog.milestones(games[i]).size() >= 2, games[i]);
            assertTrue(ArcadeProgressCatalog.achievements(games[i]).size() >= achievementCounts[i], games[i]);
        }
    }

    @Test
    void dragonsCatalogUsesTheCommunityAchievementSetWithoutFlareAchievements() {
        assertTrue(hasAchievement("dragons", "win-three-in-a-row", "Win 3 in a row"));
        assertTrue(hasAchievement("dragons", "survive-five-minutes", "Survive 5 minutes"));
        assertTrue(hasAchievement("dragons", "no-damage-win", "Untouched"));
        assertEquals(3, ArcadeProgressCatalog.achievements("dragons").size());
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
        assertTrue(hasAchievement("death-tag", "come-at-me-bro", "Come At Me Bro"));
        assertTrue(hasAchievement("spleef", "no-mods-in-here", "No Mods in Here"));
        assertTrue(hasAchievement("turf-wars", "fair-game", "Fair Game"));
    }


    @Test
    void allSevenGamesExposeEveryRequestedMilestoneKeyAndThreshold() {
        assertMilestone("one-in-the-quiver", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("one-in-the-quiver", "kills", "kills", 5000, new long[] {500, 1250, 2500, 5000});

        assertMilestone("paintball", "wins", "wins", 5000, new long[] {500, 1250, 2500, 5000});
        assertMilestone("paintball", "kills", "kills", 5000, new long[] {500, 1250, 2500, 5000});
        assertMilestone("paintball", "revives", "medic", 2500, new long[] {250, 625, 1250, 2500});

        assertMilestone("runner", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("runner", "blocks-run", "marathon-runner", 100000, new long[] {10000, 25000, 50000, 100000});

        assertMilestone("sheep-quest", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("sheep-quest", "kills", "kills", 2000, new long[] {200, 500, 1000, 2000});
        assertMilestone("sheep-quest", "sheep-captures", "sheep-captures", 1000, new long[] {100, 250, 500, 1000});

        assertMilestone("snake", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("snake", "kills", "lifetime-kills", 750, new long[] {75, 188, 375, 750});

        assertMilestone("spleef", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("spleef", "blocks-broken", "blocks-broken", 250000, new long[] {25000, 62500, 125000, 250000});

        assertMilestone("turf-wars", "wins", "wins", 1000, new long[] {100, 250, 500, 1000});
        assertMilestone("turf-wars", "kills", "kills", 10000, new long[] {1000, 2500, 5000, 10000});
        assertMilestone("turf-wars", "blocks-broken", "blocks-broken", 10000, new long[] {1000, 2500, 5000, 10000});
    }

    @Test
    void secondAchievementBatchContainsTheRequestedTitles() {
        assertTrue(hasAchievement("sneaky-assassins", "you-need-directions", "You Need Directions?"));
        assertTrue(hasAchievement("sneaky-assassins", "not-so-sneaky", "Not So Sneaky"));
        assertTrue(hasAchievement("sneaky-assassins", "prosecutor", "Prosecutor"));
        assertTrue(hasAchievement("sneaky-assassins", "idiot", "Idiot"));
        assertTrue(hasAchievement("sneaky-assassins", "the-duels-duels-duel", "The Duel’s Duel’s Duel"));
        assertTrue(hasAchievement("sneaky-assassins", "disappointment-is-immeasurable", "Disappointment is Immeasurable"));
        assertTrue(hasAchievement("speed-builders-arcade", "speedrunner", "Speedrunner"));
        assertTrue(hasAchievement("speed-builders-arcade", "unlucky", "Unlucky"));
        assertTrue(hasAchievement("speed-builders-arcade", "bad-builds", "Bad Builds"));
        assertTrue(hasAchievement("speed-builders-arcade", "right-on-time", "Right on time"));
        assertTrue(hasAchievement("wizards", "trifecta", "Trifecta"));
        assertTrue(hasAchievement("wither-assault", "savior", "Savior"));
        assertTrue(hasAchievement("hoops-remastered", "quintuple-double", "Quintuple Double"));
        assertTrue(hasAchievement("milk-the-cow", "got-the-chops", "Got the Chops"));
        assertTrue(hasAchievement("draw-my-thing", "for-the-people", "For the people"));
        assertTrue(hasAchievement("master-builders", "architect", "Architect"));
        assertTrue(hasAchievement("heroes-of-gwen", "know-your-enemy", "Know Your Enemy"));
        assertTrue(hasAchievement("castle-assault", "moral-exercise", "Moral Exercise"));
        assertTrue(ArcadeProgressCatalog.achievements("skywars").isEmpty());
    }

    @Test
    void secondMilestoneBatchUsesAllRequestedLifetimeTargets() {
        assertMilestone("sneaky-assassins", "kills", "kills", 1000, new long[] {100, 250, 500, 1000});
        assertMilestone("sneaky-assassins", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("sneaky-assassins", "powerups", "powerups", 750, new long[] {75, 188, 375, 750});
        assertMilestone("speed-builders-arcade", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("speed-builders-arcade", "perfect-builds", "perfect-builds", 1000, new long[] {100, 250, 500, 1000});
        assertMilestone("speed-builders-arcade", "blocks-placed", "blocks-placed", 50000, new long[] {5000, 12500, 25000, 50000});
        assertMilestone("wizards", "wins", "wins", 250, new long[] {25, 63, 125, 250});
        assertMilestone("wizards", "kills", "kills", 1000, new long[] {100, 250, 500, 1000});
        assertMilestone("wizards", "spells-bound", "spells-bound", 5000, new long[] {500, 1250, 2500, 5000});
        assertMilestone("wither-assault", "revives", "revives", 500, new long[] {50, 125, 250, 500});
        assertMilestone("hoops-remastered", "points", "points", 500, new long[] {50, 125, 250, 500});
        assertMilestone("hoops-remastered", "blocks", "blocks", 250, new long[] {25, 63, 125, 250});
        assertMilestone("milk-the-cow", "buckets-digested", "buckets-digested", 1500, new long[] {150, 375, 750, 1500});
        assertMilestone("skywars", "tnt-kills", "tnt-kills", 100, new long[] {10, 25, 50, 100});
        assertMilestone("draw-my-thing", "first-guesses", "first-guesses", 500, new long[] {50, 125, 250, 500});
        assertMilestone("master-builders", "fair-votes", "fair-votes", 250, new long[] {25, 63, 125, 250});
        assertMilestone("heroes-of-gwen", "mage-wins", "mage-wins", 100, new long[] {10, 25, 50, 100});
        assertMilestone("castle-assault", "crystals-destroyed", "crystals-destroyed", 250, new long[] {25, 63, 125, 250});
    }

    private void assertMilestone(String game, String id, String statKey, long maximum, long[] stages) {
        var milestone = ArcadeProgressCatalog.milestones(game).stream()
                .filter(entry -> entry.id().equals(id))
                .findFirst().orElseThrow(() -> new AssertionError("Missing milestone " + game + "/" + id));
        assertEquals(statKey, milestone.statKey(), game + "/" + id + " stat key");
        assertEquals(maximum, milestone.maximum(), game + "/" + id + " maximum");
        assertArrayEquals(stages, milestone.stages(), game + "/" + id + " stages");
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
