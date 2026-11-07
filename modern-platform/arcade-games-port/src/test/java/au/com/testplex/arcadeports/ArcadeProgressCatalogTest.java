package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ArcadeProgressCatalogTest {
    @Test
    void dragonEscapeHasFourteenAchievementsAndWorldTourIsADynamicMilestone() {
        var achievements = ArcadeProgressCatalog.achievements("dragon-escape");
        assertEquals(14, achievements.size());
        assertEquals(14, achievements.stream().map(ArcadeProgressCatalog.Achievement::id).distinct().count());
        assertFalse(achievements.stream().anyMatch(entry -> entry.id().equals("world-tour")));
        for (String id : List.of("ragebaiter", "learn-to-code", "last-leap", "brick-by-brick",
                "beat-the-clock", "hot-on-your-heels", "untouchable")) {
            assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals(id)), id);
        }
        assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals("ragebaiter")
                && entry.name().equals("Ragebaiter") && entry.requirement().contains("7 seconds")));
        assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals("last-leap")
                && entry.requirement().contains("8 blocks")));
        assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals("learn-to-code")
                && entry.requirement().contains("six consecutive blocks")));
        assertTrue(ArcadeProgressCatalog.milestones("dragon-escape").stream()
                .anyMatch(milestone -> milestone.id().equals("world-tour") && milestone.name().equals("World Tour")));
    }

    @Test
    void worldTourTracksTheCurrentRotationWithoutForgettingPreviouslyCompletedMaps() {
        var initial = ArcadeProgressCatalog.worldTourProgress(
                List.of("map-a", "map-b", "map-c", "map-d"), Set.of("map-a", "map-c"));
        assertEquals(2, initial.value());
        assertEquals(4, initial.maximum());
        assertArrayEquals(new long[] {1, 2, 3, 4}, initial.stages());

        var added = ArcadeProgressCatalog.worldTourProgress(
                List.of("map-a", "map-b", "map-c", "map-d", "map-e"), Set.of("map-a", "map-c"));
        assertEquals(2, added.value());
        assertEquals(5, added.maximum());
        assertArrayEquals(new long[] {2, 3, 4, 5}, added.stages());

        var removed = ArcadeProgressCatalog.worldTourProgress(
                List.of("map-a", "map-c", "map-c"), Set.of("map-a", "map-c", "map-z"));
        assertEquals(2, removed.value());
        assertEquals(2, removed.maximum());
        assertArrayEquals(new long[] {1, 2}, removed.stages());

        var none = ArcadeProgressCatalog.worldTourProgress(List.of(), Set.of());
        assertEquals(0, none.value());
        assertEquals(1, none.maximum()); // No maps must never appear mastered.
    }

    @Test
    void ragebaiterRequiresAnActualFallWithinSevenSecondsOfMineLaunch() {
        assertTrue(ArcadePortRules.shouldCreditDragonEscapeRagebaiter(15_000L, 8_000L, "fell into the void"));
        assertFalse(ArcadePortRules.shouldCreditDragonEscapeRagebaiter(15_001L, 8_000L, "fell into the void"));
        assertFalse(ArcadePortRules.shouldCreditDragonEscapeRagebaiter(14_000L, 8_000L, "was caught by Douglas"));
        assertFalse(ArcadePortRules.shouldCreditDragonEscapeRagebaiter(14_000L, 0L, "fell"));
        assertFalse(ArcadePortRules.shouldCreditDragonEscapeRagebaiter(7_999L, 8_000L, "fell"));
    }

    @Test
    void deathTagHasFourteenReviewedAchievementsAndNoCircularKitUnlocks() {
        var achievements = ArcadeProgressCatalog.achievements("death-tag");
        assertEquals(14, achievements.size());
        assertEquals(14, achievements.stream().map(ArcadeProgressCatalog.Achievement::id).distinct().count());
        assertEquals("Win 20 games of Death Tag.", achievements.stream()
                .filter(a -> a.id().equals("death-proof")).findFirst().orElseThrow().requirement());
        assertEquals("Kill 3 Runners within 30 seconds.", achievements.stream()
                .filter(a -> a.id().equals("hasty-hunter")).findFirst().orElseThrow().requirement());
        for (String id : java.util.List.of("lots-of-subject-matter", "smoke-and-mirrors",
                "fletching-frenzy", "shell-shock", "sleight-of-hand", "patient-zero")) {
            assertTrue(achievements.stream().anyMatch(a -> a.id().equals(id)), id);
        }
        assertFalse(achievements.stream().anyMatch(a -> a.id().equals("turtle-shell")));
    }

    @Test
    void baconBrawlHasFourteenAchievementsAndOneSpecificToEveryKit() {
        assertEquals(14, ArcadeProgressCatalog.achievements("bacon-brawl").size());
        assertTrue(hasAchievement("bacon-brawl", "three-for-one", "Three for One!"));
        assertTrue(hasAchievement("bacon-brawl", "mama-camera", "Mama Get the Camera"));
        assertTrue(hasAchievement("bacon-brawl", "goat-sty", "GOAT Sty"));
        assertTrue(hasAchievement("bacon-brawl", "iron-hog", "Iron Hog"));
        assertTrue(hasAchievement("bacon-brawl", "extra-crispy", "Extra Crispy"));
        assertTrue(hasAchievement("bacon-brawl", "variety-pack", "Variety Pack"));
        assertTrue(hasAchievement("bacon-brawl", "quadruple-serving", "Quadruple Serving"));
        assertEquals(14, ArcadeProgressCatalog.achievements("bacon-brawl").stream()
                .map(ArcadeProgressCatalog.Achievement::id).distinct().count());
    }

    @Test
    void baconBrawlApprovedWordingMatchesTheActualDetectors() {
        var achievements = ArcadeProgressCatalog.achievements("bacon-brawl");
        assertEquals("Win as Pig with at least one kill. Every kill must come from a melee backstab while Cloak is active.",
                achievements.stream().filter(entry -> entry.id().equals("goat-sty"))
                        .findFirst().orElseThrow().requirement());
        assertEquals("Hit a player after they activate Body Slam while airborne, and have them be knocked off the arena before they land.",
                achievements.stream().filter(entry -> entry.id().equals("got-em"))
                        .findFirst().orElseThrow().requirement());
        assertEquals("Get 4 consecutive kills, each within 10 seconds of the previous kill.",
                achievements.stream().filter(entry -> entry.id().equals("quadruple-serving"))
                        .findFirst().orElseThrow().requirement());
    }

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
        int[] achievementCounts = {5, 4, 4, 4, 3, 5, 4, 6, 14, 8, 5, 7, 8, 6, 7, 9};
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
        assertTrue(hasAchievement("dragons", "adept", "Adept"));
        assertTrue(hasAchievement("dragons", "sharpshooter", "Sharpshooter"));
        assertTrue(hasAchievement("dragons", "ultra", "Ultra"));
        assertTrue(hasAchievement("dragons", "survivors-guilt", "Survivor’s Guilt"));
        assertTrue(hasAchievement("dragons", "freefallin", "Freefallin’"));
        assertTrue(hasAchievement("dragons", "dodgems", "Dodgems"));
        assertTrue(hasAchievement("dragons", "sticks-and-stones", "Sticks and stones"));
        assertTrue(hasAchievement("dragons", "mlg-pro", "MLG Pro"));
        assertEquals(11, ArcadeProgressCatalog.achievements("dragons").size());
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

    @Test void bombLobbersHasFourteenAchievementsWithOneForEveryKit() {
        assertEquals(14, ArcadeProgressCatalog.achievements("bomb-lobbers").size());
        assertTrue(hasAchievement("bomb-lobbers", "whats-a-double-jump", "What's a double jump?"));
        assertTrue(hasAchievement("bomb-lobbers", "blast-proof", "Blast Proof"));
        assertTrue(hasAchievement("bomb-lobbers", "not-on-my-watch", "Not On My Watch"));
        assertEquals(14, ArcadeProgressCatalog.achievements("bomb-lobbers").stream()
                .map(ArcadeProgressCatalog.Achievement::id).distinct().count());
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
        assertTrue(hasAchievement("one-in-the-quiver", "the-perfect-game", "The Perfect Game"));
        assertFalse(ArcadeProgressCatalog.achievements("one-in-the-quiver").stream()
                .anyMatch(achievement -> achievement.id().equals("perfectionist")));
        assertTrue(hasAchievement("one-in-the-quiver", "phoenix-rising", "Phoenix Rising"));
        assertEquals(14, ArcadeProgressCatalog.achievements("one-in-the-quiver").size());
        assertEquals(14, ArcadeProgressCatalog.achievements("paintball").size());
        assertTrue(hasAchievement("one-in-the-quiver", "sharpshooter", "Sharpshooter"));
        assertTrue(hasAchievement("one-in-the-quiver", "heads", "HEADS!"));
        assertTrue(hasAchievement("paintball", "aura", "AURA"));
        assertTrue(hasAchievement("runner", "special-play", "Special Play"));
        assertEquals(14, ArcadeProgressCatalog.achievements("runner").size());
        assertEquals(14, ArcadeProgressCatalog.achievements("sheep-quest").size());
        assertTrue(hasAchievement("runner", "switcheroo", "Switcheroo"));
        assertTrue(hasAchievement("runner", "long-leaper", "Long Leaper"));
        assertTrue(hasAchievement("runner", "cold-front", "Cold Front"));
        assertTrue(hasAchievement("runner", "deadeye", "Deadeye"));
        assertTrue(hasAchievement("sheep-quest", "the-shepherds-flock", "The Shepherd's Flock"));
        assertTrue(hasAchievement("sheep-quest", "clean-getaway", "Clean Getaway"));
        assertTrue(hasAchievement("sheep-quest", "baaaad-to-the-bone", "Baaaaad to the bone"));
        assertTrue(hasAchievement("death-tag", "death-proof", "Death Proof"));
        assertTrue(hasAchievement("death-tag", "come-at-me-bro", "Come At Me Bro!"));
        assertTrue(hasAchievement("snake", "busted", "Busted"));
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
        assertTrue(hasAchievement("skywars", "death-bomber", "Death Bomber"));
        assertTrue(hasAchievement("skywars", "no-chest", "No Chest"));
        assertTrue(hasAchievement("skywars", "no-armor", "No Armor"));
    }

    @Test
    void castleAssaultHasFourteenAchievementsCoveringAllFiveKits() {
        assertEquals(14, ArcadeProgressCatalog.achievements("castle-assault").size());
        assertTrue(hasAchievement("castle-assault", "bad-chemistry", "Bad Chemistry"));
        assertTrue(hasAchievement("castle-assault", "pin-cushion", "Pin Cushion"));
        assertTrue(hasAchievement("castle-assault", "tnt-rrific", "TNT-rrific"));
        assertTrue(hasAchievement("castle-assault", "golden-boy", "Golden Boy"));
        assertTrue(hasAchievement("castle-assault", "immovable-object", "Immovable Object"));
        assertTrue(hasAchievement("castle-assault", "express-delivery", "Express Delivery"));
        assertTrue(hasAchievement("castle-assault", "royal-guard", "Royal Guard"));
        assertTrue(hasAchievement("castle-assault", "against-all-odds", "Against All Odds"));
        assertTrue(ArcadeProgressCatalog.achievements("castle-assault").stream()
                .anyMatch(a -> a.id().equals("clutchness") && a.requirement().contains("less than 10 health")));
    }

    @Test
    void milkTheCowHasFourteenAchievementsWithTheApprovedNewChallenges() {
        var achievements = ArcadeProgressCatalog.achievements("milk-the-cow");
        assertEquals(14, achievements.size());
        assertEquals(14, achievements.stream().map(ArcadeProgressCatalog.Achievement::id).distinct().count());
        for (String id : List.of("pure-skill", "pinger", "milk-high-club", "perfect-game",
                "slaughterhouse", "stop-stop-hes-already-dead", "got-the-chops", "last-drop",
                "whole-herd", "udderly-explosive", "seismic-stampede", "the-full-moo",
                "stampede", "spilt-milk")) {
            assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals(id)), id);
        }
        assertTrue(achievements.stream().anyMatch(entry -> entry.id().equals("stampede")
                && entry.requirement().contains("five different Farmers")
                && entry.requirement().contains("three credited eliminations")));
        assertFalse(ArcadeProgressCatalog.milestones("milk-the-cow").isEmpty());
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
