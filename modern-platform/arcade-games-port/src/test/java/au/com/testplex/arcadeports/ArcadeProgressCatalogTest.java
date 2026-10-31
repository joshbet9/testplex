package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ArcadeProgressCatalogTest {
    private static final Map<String, List<ArcadeProgressCatalog.Achievement>> ACHIEVEMENTS = Map.ofEntries(
            Map.entry("bacon-brawl", List.of(
                    a("king-of-bacon", "King of Bacon", "Win 15 Bacon Brawl games."),
                    a("pigs-in-blankets", "Pigs In Blankets", "Get 6 kills in one game."),
                    a("flash-fried", "Flash Fried", "Win Bacon Brawl in 60 seconds or less."),
                    a("raw-pork", "Raw Pork", "Win Bacon Brawl without using an ability."),
                    a("three-for-one", "Three for One!", "Get 3 Body Slam kills in one game."),
                    a("goat-sty", "GOAT Sty", "Win as Pig, landing only cloaked backstabs."),
                    a("pork-on-fork", "Get Some Pork on Your Fork", "Win a game without a kill."),
                    a("mama-camera", "Mama Get the Camera", "As Mama Piggles, get 4 kills from Bacon Blast or its kill combo."),
                    a("got-em", "Got em’", "Kill a player during their Body Slam recovery before they land."),
                    a("hamished", "Hamished", "Win while taking starvation damage."))),
            Map.entry("bomb-lobbers", List.of(
                    a("ultimate-killer", "Ultimate Killer", "Get 5 kills in one game."),
                    a("two-birds-one-stone", "Two birds, one stone", "Kill 2 players with one TNT."),
                    a("team-player", "Team player!", "Get 5 assists in one game."),
                    a("whats-a-double-jump", "What's a double jump?", "Win without using double jump while alive."),
                    a("the-mvp", "The MVP", "Win a 1v3 clutch as the last living member of your team."),
                    a("blast-proof", "Blast Proof", "Win a game as the Armorer."),
                    a("jelly-skin", "Jelly Skin", "Win a game without taking damage."),
                    a("master-bomber", "Master Bomber", "Win 30 Bomb Lobbers games."),
                    a("wonderkid", "Wonderkid", "Get 3 kills without throwing TNT that explodes immediately on impact."),
                    a("technical", "Technical", "Kill a player with a TNT thrown from less than 20 blocks away."),
                    a("calma", "Calma", "Jump 100 times in one game."),
                    a("over-9000", "Over 9000", "Win a 1v3 clutch using only powerups."))),
            Map.entry("death-tag", List.of(
                    a("statue", "Statue", "Win while staying in the same 5×5 area for 3 minutes."),
                    a("gallows", "Gallows", "As Runner Traitor, assist 3 comrade deaths in one game."))),
            Map.entry("dragon-escape", List.of(
                    a("strats", "Strats", "As Warper, warp to a leader at least 40 blocks ahead and win."),
                    a("ragebaiter", "Ragebaiter", "Kill 3 players with Disruptor TNT in one game."),
                    a("twinning-this-halloween", "Twinning This Halloween?", "Tie a race with your ghost."),
                    a("learn-to-code", "Learn to Code", "As Digger, use all breaks in a winning race without running through a broken block."))),
            Map.entry("dragons", List.of(
                    a("ultra", "Ultra", "Win after combining Dragon Flare and a Pyro emerald."),
                    a("survivors-guilt", "Survivor’s Guilt", "Win after reaching half a heart during the game."),
                    a("freefallin", "Freefallin’", "Win while falling into the void."),
                    a("dodgems", "Dodgems", "Survive one minute without being hit by a dragon."),
                    a("sticks-and-stones", "Sticks and stones", "Take at least 7.5 hearts of fall damage and survive."),
                    a("mlg-pro", "MLG Pro", "Win using Leap as your only fall-damage cancel."))),
            Map.entry("evolution", List.of(
                    a("overkill", "Overkill", "Get 2 kills before each evolution."),
                    a("mercy-rule", "Mercy Rule", "Win without manually evolving."),
                    a("objection", "Objection!", "Win by killing only players who share your current mob."),
                    a("turns-tabled", "How the Turns have Tabled", "As a Chicken, kill an Iron Golem and win."),
                    a("try-again", "Try Try Again", "Evolve 3 times only from assists in a winning game."),
                    a("wrong-kit-champ", "Wrong Kit Champ", "Without Quick Evolver, lose a game where that kit would have let you evolve in time to win."))),
            Map.entry("gladiators", List.of(
                    a("are-you-not-entertained", "Are you not entertained?", "Win without using a bow or rod when available."),
                    a("not-even-close", "Not even close.", "Win a duel with half a heart remaining."),
                    a("silky-seconds", "Silky Seconds", "Win from loser bracket after a first-round loss by less than 0.5 hearts."),
                    a("comeback-kid", "Comeback Kid", "Win from loser bracket after being 10-d in round one."),
                    a("who-do-you-think-you-are", "Who do you think you are?", "Beat an opponent who used all potions/arrows without using any."),
                    a("man-of-integrity", "Man of Integrity", "Win without using a fishing rod when it was available."))),
            Map.entry("micro-battles", List.of(
                    a("annihilation", "Annihilation", "Get 8 kills in one Micro Battles game."),
                    a("last-two", "Last Two", "Be alive when exactly two teams remain."),
                    a("phew", "PHEW!", "Build up to the border without dying to it."),
                    a("micro-battles-king", "Micro Battles King", "Use a placed block to knock a jumper into lava/water and get 3 kills."),
                    a("mosh-pit-playroom", "Mosh-Pit Playroom", "Get 8 assists in one game."),
                    a("built-for-this", "Built for This", "As Fighter, win the final 1v1 where another kit would have lost."),
                    a("sniper-dawg", "Sniper Dawg", "As Archer, knock a player from every team off a skybase in one game."),
                    a("gravedigger", "Gravedigger", "Get a melee kill with a shovel."))),
            Map.entry("monster-maze", List.of(
                    a("jumpman", "Jumpman", "Spend all jumps on stage 1, reach the first safe pad, and win."),
                    a("just-to-suffer", "Just to Suffer", "Use a jump on a one-block gap."),
                    a("blizzard", "Blizzard", "Freeze 10 mobs in one Cryo Blitz."),
                    a("out-of-the-cup", "Out of the cup", "Reach a safe pad first, then slowball an opponent who misses that pad."),
                    a("blackout", "Blackout", "As Body Builder, win without reaching a first safe pad."),
                    a("stampede", "Stampede", "Send 10 mobs flying with Body Rush in one game."),
                    a("perfect-run", "Perfect Run", "Win after reaching every first safe pad."),
                    a("breakaway", "Breakaway", "Reach a safe pad first while the closest player is more than 30 blocks away."),
                    a("photo-finish", "Photo Finish", "Win on the final second with an opponent within 3 blocks."))));

    private static final Map<String, List<ArcadeProgressCatalog.Milestone>> MILESTONES = Map.ofEntries(
            Map.entry("bacon-brawl", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 1000, "kills"))),
            Map.entry("bomb-lobbers", List.of(
                    m("wins", "Wins", "wins", 500, "wins"),
                    m("kills", "Kills", "kills", 1000, "kills"))),
            Map.entry("death-tag", List.of(
                    m("kills", "Kills", "kills", 500, "kills"),
                    m("chaser-wins", "Chaser Wins", "chaser-wins", 125, "wins"),
                    m("runner-wins", "Runner Wins", "runner-wins", 125, "wins"))),
            Map.entry("dragon-escape", List.of(
                    m("wins", "Wins", "wins", 2000, "wins"),
                    m("courses", "Course Completes", "course-completes", 3000, "courses"),
                    m("distance", "Blocks Travelled", "blocks-travelled", 750000, "blocks"))),
            Map.entry("dragons", List.of(
                    m("survival", "Survive", "survival-millis", 36_000_000, "milliseconds"),
                    m("wins", "Wins", "wins", 250, "wins"))),
            Map.entry("evolution", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 2000, "kills"))),
            Map.entry("gladiators", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 1000, "kills"),
                    m("loser-bracket-wins", "Loser Bracket Wins", "loser-bracket-wins", 100, "wins"))),
            Map.entry("micro-battles", List.of(
                    m("wins", "Wins", "wins", 1500, "wins"),
                    m("kills", "Kills", "kills", 5000, "kills"))),
            Map.entry("monster-maze", List.of(
                    ms("wins", "Wins", "wins", 250, "wins", 25, 50, 100, 250),
                    ms("first-safe-pads", "First Safe Pads", "first-safe-pads", 1000, "pads", 100, 250, 500, 1000),
                    ms("highest-stage", "Highest Stage", "highest-stage", 21, "stage", 12, 15, 18, 21))));

    private static final String[] GAMES = {"bacon-brawl", "bomb-lobbers", "death-tag", "dragon-escape",
            "dragons", "evolution", "gladiators", "micro-battles", "monster-maze"};

    @Test
    void achievementCatalogMatchesTheSourceDefinitionsExactly() {
        int total = 0;
        for (String game : GAMES) {
            assertEquals(ACHIEVEMENTS.get(game), ArcadeProgressCatalog.achievements(game), game);
            total += ACHIEVEMENTS.get(game).size();
        }
        assertEquals(63, total);
        assertEquals(List.of(), ArcadeProgressCatalog.achievements("not-a-game"));
    }

    @Test
    void milestoneCatalogMatchesTheSourceDefinitionsExactly() {
        int total = 0;
        for (String game : GAMES) {
            assertEquals(MILESTONES.get(game), ArcadeProgressCatalog.milestones(game), game);
            total += MILESTONES.get(game).size();
        }
        assertEquals(22, total);
        assertEquals(List.of(), ArcadeProgressCatalog.milestones("not-a-game"));
    }

    @Test
    void monsterMazeUsesTheRequestedMilestoneThresholds() {
        assertArrayEquals(new long[] {25, 50, 100, 250}, stages("monster-maze", "wins"));
        assertArrayEquals(new long[] {100, 250, 500, 1000}, stages("monster-maze", "first-safe-pads"));
        assertArrayEquals(new long[] {12, 15, 18, 21}, stages("monster-maze", "highest-stage"));
    }

    @Test
    void otherMilestonesHaveFourComputedProgressStagesAndReadPerPlayerValues() {
        var milestone = ArcadeProgressCatalog.milestones("bacon-brawl").get(0);
        assertArrayEquals(new long[] {25, 63, 125, 250}, milestone.stages());
        UUID player = UUID.randomUUID();
        YamlConfiguration config = new YamlConfiguration();
        config.set("local-stats." + player + ".bacon-brawl.wins", 77);
        assertEquals(77, milestone.value(config, player, "bacon-brawl"));
        assertEquals(0, milestone.value(config, UUID.randomUUID(), "bacon-brawl"));
        assertEquals(0, milestone.value(new YamlConfiguration(), player, "bacon-brawl"));
    }

    @Test
    void everyMilestoneStageIsAscendingAndCappedByItsMaximum() {
        for (List<ArcadeProgressCatalog.Milestone> milestones : MILESTONES.values()) {
            for (ArcadeProgressCatalog.Milestone milestone : milestones) {
                long[] stages = milestone.stages();
                assertEquals(4, stages.length, milestone.id());
                for (int i = 0; i < stages.length; i++) {
                    if (i > 0) assertArrayEquals(new long[] {Math.max(stages[i - 1], stages[i])},
                            new long[] {stages[i]}, milestone.id());
                    assertEquals(stages[i], Math.min(stages[i], milestone.maximum()), milestone.id());
                }
                assertEquals(milestone.maximum(), stages[stages.length - 1], milestone.id());
            }
        }
    }

    private long[] stages(String gameId, String id) {
        return ArcadeProgressCatalog.milestones(gameId).stream()
                .filter(milestone -> milestone.id().equals(id))
                .findFirst().orElseThrow().stages();
    }

    private static ArcadeProgressCatalog.Achievement a(String id, String name, String requirement) {
        return new ArcadeProgressCatalog.Achievement(id, name, requirement);
    }

    private static ArcadeProgressCatalog.Milestone m(String id, String name, String statKey, long maximum, String unit) {
        return new ArcadeProgressCatalog.Milestone(id, name, statKey, maximum, unit);
    }

    private static ArcadeProgressCatalog.Milestone ms(String id, String name, String statKey, long maximum,
            String unit, long... stages) {
        return new ArcadeProgressCatalog.Milestone(id, name, statKey, maximum, unit,
                java.util.Arrays.stream(stages).boxed().toList());
    }
}
