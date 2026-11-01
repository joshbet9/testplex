package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;

/** Local Testplex Arcade achievements and long-term milestone tracks. */
public final class ArcadeProgressCatalog {
    public record Achievement(String id, String name, String requirement) { }

    public record Milestone(String id, String name, String statKey, long maximum, String unit, List<Long> explicitStages) {
        public Milestone(String id, String name, String statKey, long maximum, String unit) {
            this(id, name, statKey, maximum, unit, List.of());
        }

        public long[] stages() {
            if (!explicitStages.isEmpty()) return explicitStages.stream().mapToLong(Long::longValue).toArray();
            return new long[] {
                    Math.max(1L, (maximum + 9L) / 10L),
                    Math.max(1L, (maximum + 3L) / 4L),
                    Math.max(1L, (maximum + 1L) / 2L),
                    maximum
            };
        }

        public long value(FileConfiguration config, UUID playerId, String gameId) {
            String path = "local-stats." + playerId + "." + gameId + ".";
            return Math.max(0L, config.getLong(path + statKey, 0L));
        }
    }

    private static final Map<String, List<Achievement>> ACHIEVEMENTS = Map.ofEntries(
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
                    a("over-9000", "Over 9000", "Win a 1v3 clutch using only powerups."),
                    a("grenade-from-the-grave", "Grenade From the Grave", "Get 2 kills after you die in one Bomb Lobbers game."))),
            Map.entry("death-tag", List.of(
                    a("statue", "Statue", "Win while staying in the same 5×5 area for 3 minutes."),
                    a("gallows", "Gallows", "As Runner Traitor, assist 3 comrade deaths in one game."))),
            Map.entry("dragon-escape", List.of(
                    a("strats", "Strats", "As Warper, warp to a leader at least 40 blocks ahead and win."),
                    a("ragebaiter", "Ragebaiter", "Kill 3 players with Disruptor TNT in one game."),
                    a("twinning-this-halloween", "Twinning This Halloween?", "Tie a race with your ghost."),
                    a("learn-to-code", "Learn to Code", "As Digger, use all breaks in a winning race without running through a broken block."))),
            Map.entry("dragons", List.of(
                    a("win-three-in-a-row", "Win 3 in a row", "Win three completed Dragons games consecutively."),
                    a("survive-five-minutes", "Survive 5 minutes", "Stay alive in a Dragons match for at least five minutes."),
                    a("no-damage-win", "Untouched", "Win a Dragons match without taking damage."))),
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

            Map.entry("one-in-the-quiver", List.of(
                    a("heads", "HEADS!", "Kill a player with a high-arced bow shot."),
                    a("rhythm-heaven", "Rhythm Heaven", "Win a game with 10 separate two-kill streaks."))),
            Map.entry("paintball", List.of(
                    a("rambo", "Rambo", "Hit 8 shots in a row with the Machine Gun."),
                    a("spray-can", "Spray Can", "Land 6 hits from a single Shotgun blast."),
                    a("share-your-screen", "Share your screen", "Win as Rifle with at least 3 shots and no misses."),
                    a("how-does-he-know", "How does he know?", "As the last player alive, stare at the ground continuously for 20 seconds."),
                    a("aura", "AURA", "Hit a player with a shot that rises high and comes back down."))),
            Map.entry("runner", List.of(
                    a("nsfm", "NSFM", "Win after assisting in 5 opponent deaths with Frost Balls."),
                    a("ice-in-my-veins", "Ice in my veins", "Win while falling into the void, lava, or water."),
                    a("failed-upwards", "Failed Upwards", "Win below 3 hearts after your final opponent runs over a block you ran in the final 5 seconds."),
                    a("took-a-dive", "Took a Dive", "Win after falling below the top layer before the game begins."),
                    a("special-play", "Special Play", "Win after leap-cancelling a fall that would otherwise have killed you."),
                    a("made-it-work", "Made it work", "Win after assisting in 5 opponent deaths with Archer."),
                    a("sore-feet", "Sore Feet", "Take 20 hearts worth of fall damage in one game."))),
            Map.entry("sheep-quest", List.of(
                    a("whats-a-sheep", "What's a Sheep?", "Win with at least 10 kills and 0 sheep captures."),
                    a("cheeky", "Cheeky", "Win after capturing 5 sheep in the final 10 seconds."),
                    a("gladiator", "Gladiator", "As Brute, throw a teammate into a fight where they get a double kill."),
                    a("yeet", "Yeet", "As Berserker, win while capturing sheep only while airborne after a leap."),
                    a("unconventional-but-effective", "Unconventional but effective", "As Archer, win without entering your team's pen."),
                    a("solo-carry", "Solo carry", "Kill 3 opponents and then capture 2 of their sheep without teammate assists."),
                    a("the-biggest-loser", "The Biggest Loser", "Lose after capturing 8 sheep."),
                    a("baaaad-to-the-bone", "Baaaaad to the bone", "As Archer, kill the same eligible player 5 times with bowshots."))),

            Map.entry("spleef", List.of(
                    a("praying-to-the-gods", "Praying to the Gods", "Win while falling into the void."),
                    a("clutchest-of-them-all", "Clutchest of them all", "As Brawler, spleef yourself and your final opponent, then leap back to the ground and win."),
                    a("ranger", "Ranger", "Win after assisting 5 player deaths with arrows."),
                    a("wrong-kit-lil-bro", "Wrong Kit Lil’ Bro", "Win as Snowballer without throwing a snowball."),
                    a("spleeftastic", "Spleeftastic!", "Win as Brawler without hitting another player."),
                    a("block-drought", "Block Drought", "Win while taking starvation damage."),
                    a("no-mods-in-here", "No Mods in Here", "Tie a game of Spleef."))),
            Map.entry("turf-wars", List.of(
                    a("romped", "Romped", "Win without the opposing team getting a kill."),
                    a("genius-mathematician", "Genius Mathematician", "Win without leaving your base or being killed."),
                    a("why", "WHY???", "Lose a game with a 5.0 kill/death ratio."),
                    a("triple-combo", "Triple Combo", "Land 3 bounce shots in a row."),
                    a("360-no-scope", "360 No Scope", "Land a bounce shot after turning 360 degrees mid-air."),
                    a("watch-your-step", "Watch Your Step", "Hit a player whose feet are visible while their upper body is behind blocks."),
                    a("barragist", "Barragist", "As Shredder, hit 3 players in one barrage."),
                    a("invincible", "Invincible", "As Infiltrator, get 5 kills without returning to your team's side."),
                    a("fair-game", "Fair Game", "As Marksman, get killed by a Shredder bowshot."))),
            Map.entry("sneaky-assassins", List.of(
                    a("you-need-directions", "You Need Directions?", "As a Master Assassin, kill a player with your compass."),
                    a("not-so-sneaky", "Not So Sneaky", "Win as the Ranged Assassin without dealing any melee damage."),
                    a("prosecutor", "Prosecutor", "Reveal 5 different players in one game."),
                    a("idiot", "Idiot", "Win as Escape Artist without using a Smoke Bomb."),
                    a("the-duels-duels-duel", "The Duel’s Duel’s Duel", "Win as a Master Assassin against at least two other Master Assassins."),
                    a("disappointment-is-immeasurable", "Disappointment is Immeasurable", "Kill a player within one second of them collecting a power-up."))),
            Map.entry("speed-builders-arcade", List.of(
                    a("speedrunner", "Speedrunner", "Win a Speed Builders game that is scored by time."),
                    a("unlucky", "Unlucky", "Lose a Speed Builders game that is scored by time."),
                    a("bad-builds", "Bad Builds", "Win a game with 3 or fewer perfect builds."),
                    a("right-on-time", "Right on time", "Win with a perfect build in the final round with less than one second remaining."))),
            Map.entry("wizards", List.of(
                    a("youre-a-wizard-harry", "You’re a wizard Harry", "Win a game without casting a spell."),
                    a("strong-willed-small-minded", "Strong Willed, Small Minded", "Win using only max-level spells."),
                    a("ding-dong", "Ding Dong", "Lose a game after getting 6 kills."),
                    a("trifecta", "Trifecta", "Kill three players with one spell."),
                    a("one-trick-wizard", "One-Trick Wizard", "Win using spells from only one spell family."),
                    a("spell-it-out", "Spell it out", "Get a kill finalised by your opponent killing themselves with their own spell."))),
            Map.entry("wither-assault", List.of(
                    a("lucid-dreams", "Lucid Dreams", "Win as a human without taking any damage."),
                    a("eye-candy", "Eye Candy", "As a human, reach the top of the Labyrinth map."),
                    a("split-up", "SPLIT UP!", "As a miner, hide in an underground or wall tunnel and have at least 5 teammates follow you."),
                    a("savior", "Savior", "Revive 5 humans in one game."),
                    a("tormentor", "Tormentor", "Kill 3 humans with one Wither Skull hit."),
                    a("worst-wither-ever", "Worst Wither Ever", "Kill the same non-AFK human 5 times in one game."),
                    a("wither-world", "Wither World", "Win as a Wither on every Wither Assault map."),
                    a("its-a-mans-world", "It’s a man’s world", "Win as a human on every Wither Assault map."))),
            Map.entry("hoops-remastered", List.of(
                    a("double-double", "Double Double", "Reach at least 3 in two of points, assists, rebounds, blocks and steals."),
                    a("triple-double", "Triple Double", "Reach at least 3 in three of points, assists, rebounds, blocks and steals."),
                    a("quadruple-double", "Quadruple Double", "Reach at least 3 in four of points, assists, rebounds, blocks and steals."),
                    a("quintuple-double", "Quintuple Double", "Reach at least 3 in all five of points, assists, rebounds, blocks and steals."),
                    a("playoff-sweep", "Playoff Sweep", "Win 4 or more games in a row."),
                    a("buzzer-beater", "Buzzer Beater", "Score or assist the game-winning shot with 3 seconds or less remaining."),
                    a("in-the-clutch", "In The Clutch", "Contribute 6 points through points or assists in the final minute."),
                    a("comeback-kings", "Comeback Kings", "Win after your team was at least 10 points behind."),
                    a("brick-by-brick", "Brick by Brick", "Take at least 3 shots, score 0 points, and win the game."))),
            Map.entry("milk-the-cow", List.of(
                    a("pure-skill", "Pure Skill", "Win a game after being 5 points behind."),
                    a("pinger", "Pinger", "Win while a player you are tied with is mid-drink."),
                    a("milk-high-club", "Milk High Club", "Win while only drinking milk when airborne."),
                    a("perfect-game", "Perfect Game", "Win without dying."),
                    a("slaughterhouse", "Slaughterhouse", "Assist 15 times in one game."),
                    a("stop-stop-hes-already-dead", "Stop Stop he’s already dead", "Prevent a player who has led 3 or more times from winning."),
                    a("got-the-chops", "Got the Chops", "Kill 10 airborne players in one game."))),
            Map.entry("draw-my-thing", List.of(
                    a("pixel-linguist", "Pixel Linguist", "Win by guessing every word and making only one guess per round."),
                    a("why-cant-we-have-nice-things", "Why can’t we have nice things", "Win in a lobby where another player guesses the word within 5 seconds on at least two occasions."),
                    a("doubted-but-not-in-doubt", "Doubted but not in doubt", "Win after being at least 10 points behind."),
                    a("triple-threat", "Triple Threat", "Get 3 first guesses in a row, each as your only guess that round."),
                    a("for-the-people", "For the people", "Have every player in the lobby guess your drawing."))),
            Map.entry("master-builders", List.of(
                    a("purist", "Purist", "Win with a build using only one block type."),
                    a("sharing-is-caring", "Sharing is Caring", "Tie for first place."),
                    a("for-the-greater-good", "For the greater good", "Lose a game you would have won if you had voted the winner 1/5."),
                    a("architect", "Architect", "Win a game by 20 or more points."))),
            Map.entry("heroes-of-gwen", List.of(
                    a("mvp", "MVP", "Win after landing the final hit on both towers and the final boss."),
                    a("moba-master", "MOBA Master", "Win a game with every hero."),
                    a("tactical-genius", "Tactical Genius", "Come back from at least one crystal down after changing hero mid-game."),
                    a("gold-mine", "Gold Mine", "Win while holding both beacons for the entire game."),
                    a("objective-null", "Objective Null", "Get 15 kills in one game."),
                    a("know-your-enemy", "Know Your Enemy", "Only damage opposing players in your own role, out-kill their counterpart, and win."))),
            Map.entry("castle-assault", List.of(
                    a("one-man-army", "One Man Army", "Destroy both enemy crystals and land the final hit on the King."),
                    a("remember-the-days", "Remember The Days", "Win 3 Castle Assault games in a row."),
                    a("clutchness", "Clutchness", "Win while your King has less than 50 health."),
                    a("multiplicative", "Multiplicative", "Get twice as many kills as the next-highest killer on your team."),
                    a("blasphemous", "Blasphemous", "Get 8 kills in one game without using a fishing rod."),
                    a("moral-exercise", "Moral Exercise", "Get 5 kills by blowing up opponents as the TNT Holder."))),
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

    private static final Map<String, List<Milestone>> MILESTONES = Map.ofEntries(
            Map.entry("bacon-brawl", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 1000, "kills"))),
            Map.entry("bomb-lobbers", List.of(m("wins", "Wins", "wins", 500, "wins"), m("kills", "Kills", "kills", 1000, "kills"))),
            Map.entry("death-tag", List.of(m("kills", "Kills", "kills", 500, "kills"), m("chaser-wins", "Chaser Wins", "chaser-wins", 125, "wins"), m("runner-wins", "Runner Wins", "runner-wins", 125, "wins"))),
            Map.entry("dragon-escape", List.of(m("wins", "Wins", "wins", 2000, "wins"), m("courses", "Course Completes", "course-completes", 3000, "courses"), m("distance", "Blocks Travelled", "blocks-travelled", 750000, "blocks"))),
            Map.entry("dragons", List.of(m("survival", "Survive", "survival-millis", 36_000_000, "milliseconds"), m("wins", "Wins", "wins", 250, "wins"))),
            Map.entry("evolution", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 2000, "kills"))),
            Map.entry("gladiators", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 1000, "kills"), m("loser-bracket-wins", "Loser Bracket Wins", "loser-bracket-wins", 100, "wins"))),
            Map.entry("micro-battles", List.of(m("wins", "Wins", "wins", 1500, "wins"), m("kills", "Kills", "kills", 5000, "kills"))),

            Map.entry("one-in-the-quiver", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 5000, "kills"))),
            Map.entry("paintball", List.of(m("wins", "Wins", "wins", 5000, "wins"), m("kills", "Kills", "kills", 5000, "kills"), m("revives", "Revives", "medic", 2500, "revives"))),
            Map.entry("runner", List.of(m("wins", "Wins", "wins", 250, "wins"), m("blocks-run", "Blocks Run", "marathon-runner", 100000, "blocks"))),
            Map.entry("sheep-quest", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 2000, "kills"), m("sheep-captures", "Sheep Captures", "sheep-captures", 1000, "captures"))),
            Map.entry("snake", List.of(
                    a("Cannibal", "Cannibal", "Get six kills in one Snake game."),
                    a("ChooChoo", "Choo Choo", "Grow your tail to at least 60 sheep segments."),
                    a("SlimySheep", "Slimy Sheep", "Eat 20 green slime pickups in one Snake game."),
                    a("dont-want-none", "Don't want none", "Win a game without getting a kill."),
                    a("resilience-snakeified", "Resilience Snakeified", "Win after being hit by a 10-second Magma Cube nerf."),
                    a("busted", "Busted", "Roll the same Magma Cube nerf 3 times in a row."),
                    a("one-of-the-ogs", "One of the OGs", "Win a game without using an ability."),
                    a("flawless", "Flawless", "Win with every kill earned while an ability is active."),
                    a("bots-everywhere", "Bots Everywhere!", "Win while staying within 10 blocks of the map edge throughout the game."))),
            Map.entry("spleef", List.of(m("wins", "Wins", "wins", 250, "wins"), m("blocks-broken", "Blocks Broken", "blocks-broken", 250000, "blocks"))),
            Map.entry("turf-wars", List.of(m("wins", "Wins", "wins", 1000, "wins"), m("kills", "Kills", "kills", 10000, "kills"), m("blocks-broken", "Blocks Broken", "blocks-broken", 10000, "blocks"))),
            Map.entry("sneaky-assassins", List.of(
                    m("kills", "Kills", "kills", 1000, "kills"),
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("powerups", "Power-ups Obtained", "powerups", 750, "power-ups"))),
            Map.entry("speed-builders-arcade", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("perfect-builds", "Perfect Builds", "perfect-builds", 1000, "perfect builds"),
                    m("blocks-placed", "Blocks Placed", "blocks-placed", 50000, "blocks"))),
            Map.entry("wizards", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 1000, "kills"),
                    m("spells-bound", "Spells Bound", "spells-bound", 5000, "spells"))),
            Map.entry("wither-assault", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 500, "kills"),
                    m("revives", "Revives", "revives", 500, "revives"))),
            Map.entry("hoops-remastered", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("points", "Points", "points", 500, "points"),
                    m("assists", "Assists", "assists", 250, "assists"),
                    m("rebounds", "Rebounds", "rebounds", 250, "rebounds"),
                    m("steals", "Steals", "steals", 250, "steals"),
                    m("blocks", "Blocks", "blocks", 250, "blocks"))),
            Map.entry("milk-the-cow", List.of(
                    m("cow-wins", "Cow Wins", "cow-wins", 125, "wins"),
                    m("human-wins", "Human Wins", "human-wins", 125, "wins"),
                    m("kills", "Kills", "kills", 1500, "kills"),
                    m("buckets-digested", "Buckets Digested", "buckets-digested", 1500, "buckets"))),
            Map.entry("skywars", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("kills", "Kills", "kills", 2000, "kills"),
                    m("tnt-kills", "TNT Kills", "tnt-kills", 100, "TNT kills"))),
            Map.entry("draw-my-thing", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("correct-guesses", "Correct Guesses", "correct-guesses", 2000, "guesses"),
                    m("first-guesses", "First Guesses", "first-guesses", 500, "first guesses"))),
            Map.entry("master-builders", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("fair-votes", "Vote Fairly", "fair-votes", 250, "fair votes"))),
            Map.entry("heroes-of-gwen", List.of(
                    m("hunter-wins", "Hunter Wins", "hunter-wins", 100, "wins"),
                    m("warrior-wins", "Warrior Wins", "warrior-wins", 100, "wins"),
                    m("assassin-wins", "Assassin Wins", "assassin-wins", 100, "wins"),
                    m("mage-wins", "Mage Wins", "mage-wins", 100, "wins"))),
            Map.entry("castle-assault", List.of(
                    m("wins", "Wins", "wins", 250, "wins"),
                    m("crystals-destroyed", "Crystals Destroyed", "crystals-destroyed", 250, "crystals"),
                    m("kills", "Kills", "kills", 1000, "kills"))),
            Map.entry("monster-maze", List.of(
                    ms("wins", "Wins", "wins", 250, "wins", 25, 50, 100, 250),
                    ms("first-safe-pads", "First Safe Pads", "first-safe-pads", 1000, "pads", 100, 250, 500, 1000),
                    ms("highest-stage", "Highest Stage", "highest-stage", 21, "stage", 12, 15, 18, 21))));

    private ArcadeProgressCatalog() { }

    public static List<Achievement> achievements(String gameId) {
        return ACHIEVEMENTS.getOrDefault(gameId, List.of());
    }

    public static List<Milestone> milestones(String gameId) {
        return MILESTONES.getOrDefault(gameId, List.of());
    }

    private static Achievement a(String id, String name, String requirement) {
        return new Achievement(id, name, requirement);
    }

    private static Milestone m(String id, String name, String statKey, long maximum, String unit) {
        return new Milestone(id, name, statKey, maximum, unit);
    }

    private static Milestone ms(String id, String name, String statKey, long maximum, String unit, long... stages) {
        return new Milestone(id, name, statKey, maximum, unit, java.util.Arrays.stream(stages).boxed().toList());
    }
}
