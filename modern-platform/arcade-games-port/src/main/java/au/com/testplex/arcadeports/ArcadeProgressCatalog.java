package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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

        /** Dynamic tracks are derived from live content, not a stale stored target. */
        public Progress progress(JavaPlugin plugin, UUID playerId, String gameId) {
            if ("dragon-escape".equals(gameId) && "world-tour".equals(id)) {
                List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE);
                Set<String> completed = maps.stream().map(SourceMapCatalog.MapArchive::resource).filter(resource -> {
                    String key = ArcadePortRules.dragonEscapeMapStorageKey(resource);
                    return !key.isEmpty() && plugin.getConfig().getLong(
                            "dragon-escape-pb." + playerId + "." + key + ".time-ms", -1L) > 0L;
                }).collect(java.util.stream.Collectors.toSet());
                return worldTourProgress(maps.stream().map(SourceMapCatalog.MapArchive::resource).toList(), completed);
            }
            return new Progress(value(plugin.getConfig(), playerId, gameId), maximum, stages());
        }
    }

    public record Progress(long value, long maximum, long[] stages) { }

    /** A change to the selectable map pool immediately changes World Tour's target and progress. */
    static Progress worldTourProgress(List<String> rotation, Set<String> completed) {
        Set<String> playable = new java.util.HashSet<>(rotation);
        playable.removeIf(resource -> resource == null || resource.isBlank());
        if (playable.isEmpty()) return new Progress(0L, 1L, new long[] {1L});
        long total = playable.size();
        long value = playable.stream().filter(completed::contains).count();
        java.util.TreeSet<Long> thresholds = new java.util.TreeSet<>();
        for (int fraction = 1; fraction <= 4; fraction++) {
            thresholds.add((total * fraction + 3L) / 4L);
        }
        return new Progress(value, total, thresholds.stream().mapToLong(Long::longValue).toArray());
    }

    private static final Map<String, List<Achievement>> ACHIEVEMENTS = Map.ofEntries(
            Map.entry("bacon-brawl", List.of(
                    a("king-of-bacon", "King of Bacon", "Win 15 Bacon Brawl games."),
                    a("pigs-in-blankets", "Pigs In Blankets", "Get 6 kills in one game."),
                    a("flash-fried", "Flash Fried", "Win Bacon Brawl in 60 seconds or less."),
                    a("raw-pork", "Raw Pork", "Win Bacon Brawl without using an ability."),
                    a("three-for-one", "Three for One!", "Get 3 Body Slam kills in one game."),
                    a("goat-sty", "GOAT Sty", "Win as Pig with at least one kill. Every kill must come from a melee backstab while Cloak is active."),
                    a("pork-on-fork", "Get Some Pork on Your Fork", "Win a game without a kill."),
                    a("mama-camera", "Mama Get the Camera", "As Mama Piggles, get 4 kills from Bacon Blast or its kill combo."),
                    a("got-em", "Got em’", "Hit a player after they activate Body Slam while airborne, and have them be knocked off the arena before they land."),
                    a("hamished", "Hamished", "Win while taking starvation damage."),
                    a("iron-hog", "Iron Hog", "As Brutus Bacon, block 8 enemy attacks with Brutus Guard in one game."),
                    a("extra-crispy", "Extra Crispy", "As Chris P. Bacon, ignite 5 different opponents with Crispy Bacon projectiles or dash in one game."),
                    a("variety-pack", "Variety Pack", "Win at least one Bacon Brawl game using each of the 5 kits."),
                    a("quadruple-serving", "Quadruple Serving", "Get 4 consecutive kills, each within 10 seconds of the previous kill."))),
            Map.entry("skywars", List.of(
                    a("death-bomber", "Death Bomber", "Kill 3 players with Throwable TNT in one game."),
                    a("no-chest", "No Chest", "Win without opening a chest."),
                    a("no-armor", "No Armor", "Win without wearing armor."))),
            Map.entry("bomb-lobbers", List.of(
                    a("ultimate-killer", "Ultimate Killer", "Get 5 kills in one game."),
                    a("two-birds-one-stone", "Two birds, one stone", "Kill 2 players with one TNT."),
                    a("team-player", "Team player!", "Get 5 assists in one game."),
                    a("whats-a-double-jump", "What's a double jump?", "Win as Acrobat without using your double jump."),
                    a("the-mvp", "The MVP", "Win a 1v3 clutch as the last living member of your team."),
                    a("blast-proof", "Blast Proof", "Win 10 games as Armorer."),
                    a("jelly-skin", "Jelly Skin", "Win a game without taking damage."),
                    a("master-bomber", "Master Bomber", "Win 30 Bomb Lobbers games."),
                    a("wonderkid", "Wonderkid", "Get 3 kills in one game from high-lob TNT that explodes immediately on impact."),
                    a("technical", "Technical", "Kill a player with TNT thrown from less than 10 blocks away."),
                    a("calma", "Calma", "Double-jump 100 times in one game."),
                    a("over-9000", "Over 9000", "Get 3 kills with one power-up TNT."),
                    a("grenade-from-the-grave", "Grenade From the Grave", "Get 2 TNT kills after you die in one game."),
                    a("not-on-my-watch", "Not On My Watch", "Block 3 enemy TNT throws with Constructor defenses in one game."))),
            Map.entry("death-tag", List.of(
                    a("death-proof", "Death Proof", "Win 20 games of Death Tag."),
                    a("come-at-me-bro", "Come At Me Bro!", "Kill 2 Undead Chasers in a single game."),
                    a("statue", "Statue", "Win while staying in the same 5×5 area for 3 minutes."),
                    a("gallows", "Gallows", "As Runner Traitor, knock fellow Runners toward danger and assist 3 of their deaths to Chasers within 8 seconds of hitting them, in one game."),
                    a("ultimate-chaser", "The Ultimate Chaser", "Kill 8 Runners in a single game."),
                    a("hasty-hunter", "Hasty Hunter", "Kill 3 Runners within 30 seconds."),
                    a("resilient-runner", "Resilient Runner", "Regenerate 250 hearts total while using Runner Basher."),
                    a("catch-me-if-you-can", "Catch Me If You Can", "Run 10,000 blocks in Death Tag."),
                    a("lots-of-subject-matter", "Lots of Subject Matter", "As a Runner, land melee hits on 5 different Chasers in one game. Unlocks Runner Turtle."),
                    a("smoke-and-mirrors", "Smoke and Mirrors", "Win as a Runner after surviving nonlethal hits from 3 different Chasers in one game. Unlocks Runner Trickster."),
                    a("fletching-frenzy", "Fletching Frenzy", "As Runner Archer, land 10 successful arrow hits on Chasers in one game."),
                    a("shell-shock", "Shell Shock", "As Runner Turtle, apply a full three-stack Slowness and Weakness effect to 2 different Chasers in one game."),
                    a("sleight-of-hand", "Sleight of Hand", "As Runner Trickster, use Chaser Swap within 5 blocks of a Chaser, then survive 30 seconds without being tagged."),
                    a("patient-zero", "Patient Zero", "As the original Alpha Chaser, have your first converted Runner start a four-generation infection chain: Alpha → A → B → C → D."))),
            Map.entry("dragon-escape", List.of(
                    a("douglas-defeater", "Douglas Defeater", "Win 50 games of Dragon Escape."),
                    a("paralympics", "Paralympics", "Win a game without using Leap."),
                    a("skylands-master", "Skylands Master", "Finish Skylands 5 times."),
                    a("to-hell-and-back", "To Hell and Back", "Finish Through Hell 5 times."),
                    a("plundered", "Plundered", "Finish Pirate Bay 5 times."),
                    a("strats", "Strats", "As Warper, warp to a leader at least 40 blocks ahead and win."),
                    a("ragebaiter", "Ragebaiter", "Cause 3 opponents to fall within 7 seconds of launching them with Disruptor TNT, in one game."),
                    a("twinning-this-halloween", "Twinning This Halloween?", "Tie a race with your ghost."),
                    a("learn-to-code", "Learn to Code", "As Digger, pillar straight up six consecutive blocks and win the race."),
                    a("last-leap", "Last Leap", "As Jumper, use your final Leap after falling at least 8 blocks, recover onto the course, and win."),
                    a("brick-by-brick", "Brick by Brick", "As Builder, use 3 different temporary Builder bricks as footholds in one winning race."),
                    a("beat-the-clock", "Beat the Clock", "With your PB ghost enabled, beat your previous best time by at least 10 seconds."),
                    a("hot-on-your-heels", "Hot on Your Heels", "Finish a race with Douglas within 5 blocks behind you."),
                    a("untouchable", "Untouchable", "Win a Dragon Escape race without taking any damage."))),
            Map.entry("dragons", List.of(
                    a("win-three-in-a-row", "Win 3 in a row", "Win three completed Dragons games consecutively."),
                    a("survive-five-minutes", "Survive 5 minutes", "Stay alive in a Dragons match for at least five minutes."),
                    a("no-damage-win", "Untouched", "Win a Dragons match without taking damage."),
                    a("adept", "Adept", "Win a Dragons game without using your kit ability."),
                    a("sharpshooter", "Sharpshooter", "Shoot a dragon 20 times in one game."),
                    a("ultra", "Ultra", "Eliminate the final opponent with a sparkler-attracted dragon."),
                    a("survivors-guilt", "Survivor’s Guilt", "Win after being on half a heart during the game."),
                    a("freefallin", "Freefallin’", "Win a Dragons game while falling in the void."),
                    a("dodgems", "Dodgems", "Stay alive for a full minute without a dragon hit."),
                    a("sticks-and-stones", "Sticks and stones", "Survive at least 7.5 hearts of fall damage in one landing."),
                    a("mlg-pro", "MLG Pro", "Win with no fall damage after saving a dangerous fall using Leap."))),
            Map.entry("evolution", List.of(
                    a("perfect-game", "Perfect Game", "Win a game without dying."),
                    a("stealth-mastah", "Stealth Mastah", "Win without taking damage while charging an evolution."),
                    a("rampage", "Rampage", "Get 3 kills with no more than 5 seconds between consecutive kills."),
                    a("melee-monster", "Melee Monster", "Win without using any abilities."),
                    a("overkill", "Overkill", "Win after getting at least 2 credited kills before each evolution."),
                    a("wrong-kit-champ", "Wrong Kit Champ", "As Darwinist, lose while your fifth evolution is still transforming after Quick Evolver would have finished it."),
                    a("second-wind", "Second Wind", "Win after restoring at least 30 hearts through completed evolutions."),
                    a("pity-party", "Pity Party", "Win after completing at least 2 pity-triggered evolutions."),
                    a("quick-change-artist", "Quick Change Artist", "As Quick Evolver, win after completing all 4 transformations without interruption."),
                    a("groundbreaker", "Groundbreaker", "Hit 3 different opponents with one Golem Seismic Slam."),
                    a("bounce-house", "Bounce House", "Hit the same opponent with 3 separate Slime Slams in one life."),
                    a("eggspert", "Eggspert", "As Chicken, double-jump and eliminate an airborne opponent with an egg."),
                    a("blood-harvest", "Blood Harvest", "As Health Harvester, get 3 kills in one life from 25% health or less, healing to full each time."),
                    a("pity-the-ancient", "Pity the Ancient", "As Neanderthal, win after earning 2 kill-based pity reductions in each of your 5 pity cycles."))),
            Map.entry("gladiators", List.of(
                    a("brawler", "Brawler", "Get 3 Gladiators kills with your bare hands."),
                    a("precision", "Precision", "Win after hitting every arrow, with at least 3 shots."),
                    a("swift-kill", "Swift Kill", "Earn first blood in 15 Gladiators games."),
                    a("are-you-not-entertained", "Are you not entertained?", "Win without using a bow or rod when available."),
                    a("not-even-close", "Not even close.", "Win a duel with half a heart remaining."),
                    a("silky-seconds", "Silky Seconds", "Win from loser bracket after a first-round loss by less than 0.5 hearts."),
                    a("comeback-kid", "Comeback Kid", "Win from loser bracket after being 10-d in round one."),
                    a("who-do-you-think-you-are", "Who do you think you are?", "Beat an opponent who used all potions/arrows without using any."),
                    a("man-of-integrity", "Man of Integrity", "Win without using a fishing rod when it was available."),
                    a("second-wind", "Second Wind", "Lose the first Grand Final, then win the deciding rematch."),
                    a("untouchable", "Untouchable", "Win 10 Gladiators duels without taking damage."),
                    a("all-modes", "All-Round Gladiator", "Win a Gladiators tournament in every PvP mode."),
                    a("full-arsenal", "Full Arsenal", "Win a Normal-mode tournament after landing a sword, bow, and fishing-rod hit on every opponent."),
                    a("blade-protocol", "Blade Protocol", "Win a Modern-mode duel after landing 3 timed critical sword hits in a row without taking an opponent hit between them."))),
            Map.entry("micro-battles", List.of(
                    a("annihilation", "Annihilation", "Get 8 kills in one Micro Battles game."),
                    a("last-two", "Last Two", "Be alive when two teams remain, eliminate every player on the other team, and win."),
                    a("phew", "PHEW!", "Place 3 blocks within 2 blocks of the border in one game without dying to it."),
                    a("micro-battles-king", "Micro Battles King", "Use a placed block to knock a jumper into lava/water and get 3 kills."),
                    a("mosh-pit-playroom", "Mosh-Pit Playroom", "Get 8 assists in one game."),
                    a("built-for-this", "Built for This", "As Fighter, win the final 1v1 after Iron Skin prevents a lethal hit."),
                    a("speedster", "Speedster", "Win a Micro Battles game in under 1:30. Unlocks the Dasher kit."),
                    a("sniper-dawg", "Sniper Dawg", "As Archer, knock a player from every team off a skybase in one game."),
                    a("gravedigger", "Gravedigger", "Get a melee kill with a shovel."),
                    a("air-mail", "Air Mail", "As Dasher, double-jump across a gap, land on an enemy island, and eliminate an enemy there."),
                    a("needle-threader", "Needle Threader", "As Archer, eliminate an opponent with an arrow from at least 25 blocks away."),
                    a("shift-supervisor", "Shift Supervisor", "As Worker, break 30 blocks, place 30 blocks, and win the game."),
                    a("no-room-to-build", "No Room to Build", "Win a game without placing any blocks."),
                    a("last-breath", "Last Breath", "Win the final 1v1 with 1 heart or less."))),

            Map.entry("one-in-the-quiver", List.of(
                    a("the-perfect-game", "The Perfect Game", "Achieve a Perfect Run killstreak in a game."),
                    a("perfectionist", "Perfectionist", "Win a game without dying."),
                    a("sharpshooter", "Sharpshooter", "Land 8 arrow hits without missing."),
                    a("whats-a-bow", "What's A bow?", "Win with 20 kills without using archery."),
                    a("speedrunner", "Speedrunner", "Win a game in under 1:30."),
                    a("thats-not-a-sword", "That's not a sword…", "Get 3 kills with fist/bow punch."),
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
                    a("hungry-hungry-hippo", "Hungry Hungry Hippo", "Win 50 games of Sheep Quest."),
                    a("thief", "Thief", "Steal 300 sheep from enemy pens."),
                    a("animal-rescue", "Animal Rescue", "Make 300 enemies drop their sheep."),
                    a("selfish", "Selfish", "Win with more than 12 sheep in your pen."),
                    a("whats-a-sheep", "What's a Sheep?", "Win with at least 10 kills and 0 sheep captures."),
                    a("cheeky", "Cheeky", "Win after capturing 5 sheep in the final 10 seconds."),
                    a("gladiator", "Gladiator", "As Brute, throw a teammate into a fight where they get a double kill."),
                    a("yeet", "Yeet", "As Berserker, win while capturing sheep only while airborne after a leap."),
                    a("unconventional-but-effective", "Unconventional but effective", "As Archer, win without entering your team's pen."),
                    a("solo-carry", "Solo carry", "Kill 3 opponents and then capture 2 of their sheep without teammate assists."),
                    a("the-biggest-loser", "The Biggest Loser", "Lose after capturing 8 sheep."),
                    a("baaaad-to-the-bone", "Baaaaad to the bone", "As Archer, kill the same eligible player 5 times with bowshots."))),
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
                    a("speedrunner", "Speedrunner", "Win a tied top-score Speed Builders game on the cumulative build-time tiebreak."),
                    a("unlucky", "Unlucky", "Lose a tied top-score Speed Builders game on the cumulative build-time tiebreak."),
                    a("bad-builds", "Bad Builds", "Win a game with 3 or fewer perfect builds."),
                    a("right-on-time", "Right on time", "Win with a perfect build in the final round with less than one second remaining."))),
            Map.entry("wizards", List.of(
                    a("youre-a-wizard-harry", "You’re a wizard Harry", "Win a game without casting a spell."),
                    a("strong-willed-small-minded", "Strong Willed, Small Minded", "Win after casting at least one spell, with every spell cast at its maximum level."),
                    a("ding-dong", "Ding Dong", "Lose a game after getting 6 kills."),
                    a("trifecta", "Trifecta", "Kill three players with one spell."),
                    a("one-trick-wizard", "One-Trick Wizard", "Win after casting spells from exactly one category: Attack, Support or Misc."),
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
                    a("brick-by-brick", "Brick by Brick", "Take at least 3 shots, score 0 points, and win the game."),
                    a("skywalker", "Skywalker", "Score a dunk without touching the ground after using Hops."),
                    a("alley-oop", "Alley-Oop", "After using Hops, receive a teammate’s pass and dunk without touching the ground."),
                    a("pickpockets-payback", "Pickpocket’s Payback", "Steal the ball and score before the other team gets possession."),
                    a("swat-team", "Swat Team", "Block a shot and have your team recover the ball before your opponents do."),
                    a("logo-launcher", "Logo Launcher", "Win Hoops with a game-winning three-pointer."))),
            Map.entry("milk-the-cow", List.of(
                    a("pure-skill", "Pure Skill", "Win a game after being 5 points behind."),
                    a("pinger", "Pinger", "Win while a player you are tied with is mid-drink."),
                    a("milk-high-club", "Milk High Club", "Win while only drinking milk when airborne."),
                    a("perfect-game", "Perfect Game", "Win without dying."),
                    a("slaughterhouse", "Slaughterhouse", "Assist 15 times in one game."),
                    a("stop-stop-hes-already-dead", "Stop Stop he’s already dead", "Prevent a player who has led 3 or more times from winning."),
                    a("got-the-chops", "Got the Chops", "Kill 10 airborne players in one game."),
                    a("last-drop", "The Last Drop", "Score the winning point by drinking milk while at one heart or less."),
                    a("whole-herd", "Whole Herd", "As a Farmer, obtain milk from three different Cows and win."),
                    a("udderly-explosive", "Udderly Explosive", "As a Cow, hit three Farmers with one Cow Bomb and eliminate at least one of them."),
                    a("seismic-stampede", "Seismic Stampede", "As a Cow, hit three Farmers with one Body Slam and eliminate at least one of them."),
                    a("the-full-moo", "The Full Moo", "As a Cow, hit the same Farmer with Cow Bomb, Body Slam, and Cow Charge, then eliminate them."),
                    a("stampede", "Stampede!", "As a Cow, hit five different Farmers in one uninterrupted Cow Charge and earn three credited eliminations."),
                    a("spilt-milk", "Spilt Milk", "As a Farmer, lose after Cow Charge empties your milk bucket three times while you are one point from winning."))),
            Map.entry("draw-my-thing", List.of(
                    a("pixel-linguist", "Pixel Linguist", "Win by guessing every word and making only one guess per round."),
                    a("why-cant-we-have-nice-things", "Why can’t we have nice things", "Win in a lobby where another player guesses the word within 5 seconds on at least two occasions."),
                    a("doubted-but-not-in-doubt", "Doubted but not in doubt", "Win after being at least 10 points behind."),
                    a("triple-threat", "Triple Threat", "Get 3 first guesses in a row, each as your only guess that round."),
                    a("for-the-people", "For the People", "Have every other human player guess your drawing."),
                    a("minimalist-masterpiece", "Minimalist Masterpiece", "Get your drawing guessed with no more than 12 painted blocks, then have at least two human players guess it."),
                    a("spray-and-pray", "Spray and Pray", "Change at least 20 distinct canvas blocks in one Spray Can use, then have a human guess your drawing."),
                    a("line-in-the-sand", "Line in the Sand", "Change at least 12 distinct canvas blocks with one Line Tool use, then have a human guess your drawing."),
                    a("perfectly-round", "Perfectly Round", "Change at least 16 distinct canvas blocks with one Circle Tool use, then have a human guess your drawing."),
                    a("clean-slate", "Clean Slate", "Clear a drawing of at least 8 painted blocks, repaint at least 8 blocks, then have a human guess it."),
                    a("last-second-sketch", "Last-Second Sketch", "Guess the drawing in the final 3 seconds of the round."),
                    a("two-hats", "Two Hats", "Win after earning at least 2 points as a drawer and 6 points as a guesser."),
                    a("read-between-the-lines", "Read Between the Lines", "Be the first player to guess correctly after exactly one hint letter is revealed."),
                    a("third-times-the-charm", "Third Time’s the Charm", "Guess correctly on your third accepted guess while no more than 25% of the word’s letters are revealed."))),
            Map.entry("master-builders", List.of(
                    a("purist", "Purist", "Win with a build using only one block type."),
                    a("sharing-is-caring", "Sharing is Caring", "Tie for first place."),
                    a("for-the-greater-good", "For the greater good", "Finish ahead of the winner after changing your vote for them to 1/6."),
                    a("architect", "Architect", "Win a game by 20 or more points."),
                    a("block-party", "Block Party", "Use at least 12 different block types in one build."),
                    a("minimalist-masterpiece", "Minimalist Masterpiece", "Win with 32 or fewer placed blocks remaining in your plot."),
                    a("second-draft", "Second Draft", "Break at least 30 blocks you placed yourself, then win."),
                    a("touching-the-sky", "Touching the Sky", "Win with a placed block at the highest buildable level of your plot."),
                    a("critics-darling", "Critic’s Darling", "Win with an average rating of at least 5/6."),
                    a("unanimous-ovation", "Unanimous Ovation", "Win with a 6/6 rating from every other human builder."),
                    a("mixed-reviews", "Mixed Reviews", "Win with at least one 1/6 rating and one 6/6 rating."),
                    a("kingmaker", "Kingmaker", "Give the winner 6/6 when that vote makes the difference between winning and not winning."),
                    a("fair-critic", "Fair Critic", "Rate every other human build within one point of its final average."),
                    a("golden-eye", "Golden Eye", "Rate every other human build in the same order as the final standings, using rounded results."))),
            Map.entry("heroes-of-gwen", List.of(
                    a("mvp", "MVP", "Win after landing the final hit on both towers and the final boss."),
                    a("moba-master", "MOBA Master", "Win a game with every available hero."),
                    a("tactical-genius", "Tactical Genius", "After one of your towers falls, switch heroes and win the game."),
                    a("gold-mine", "Gold Mine", "Win after your team holds both beacons simultaneously for 120 uninterrupted seconds."),
                    a("objective-null", "Objective Null", "Get 15 kills in one game."),
                    a("know-your-enemy", "Know Your Enemy", "Only damage opposing heroes in your role, out-kill the opposing role, and win."),
                    a("hattori-smoke-and-mirrors", "Smoke and Mirrors", "As Hattori, hit the same enemy with Shuriken, Ninja Dash, and Ender Blade within 15 seconds."),
                    a("bardolf-full-moon-rising", "Full Moon Rising", "As Bardolf, summon five living wolves, activate Full Moon, and have your pack defeat an enemy hero before it ends."),
                    a("devon-collateral-damage", "Collateral Damage", "As Devon, damage an enemy hero and deal the final damage to an enemy tower with the same TNT Arrow explosion."),
                    a("rowena-bombardment", "Bombardment!", "As Rowena, destroy both enemy towers with Light Arrows. Any damage dealt by another player voids this; minion damage is allowed."),
                    a("anath-from-ember-to-inferno", "From Ember to Inferno", "As Anath, ignite an enemy with your Flame Wand, then hit them with Burn Beam and Meteor before the fire expires."),
                    a("larissa-turn-the-tide", "Turn the Tide", "As Larissa, hit an enemy with Aqua Cannon and restore a teammate's health with Storm Blessing within 10 seconds."),
                    a("dana-rally-the-vanguard", "Rally the Vanguard", "As Dana, heal three allies and damage two enemies with one Rally."),
                    a("biff-the-long-ride", "The Long Ride", "As Biff, Tether an enemy and hit them with Cavalry Charge before the Tether ends."),
                    a("ivy-garden-variety-trouble", "Garden Variety Trouble", "As Ivy, pull an enemy into Thorn Wall, then hit them with Floral Leap before Thorn Wall expires."))),
            Map.entry("castle-assault", List.of(
                    a("one-man-army", "One Man Army", "Destroy both enemy crystals and land the final hit on the King."),
                    a("remember-the-days", "Remember The Days", "Win 3 Castle Assault games in a row."),
                    a("clutchness", "Clutchness", "Win while your King has less than 10 health."),
                    a("multiplicative", "Multiplicative", "Get at least 5 kills and twice as many as the next-highest killer on your team."),
                    a("blasphemous", "Blasphemous", "Get 8 kills in one game without using a fishing rod."),
                    a("moral-exercise", "Moral Exercise", "Get 5 kills by blowing up opponents as the TNT Holder."),
                    a("bad-chemistry", "Bad Chemistry", "As Alchemist, kill 2 enemies affected by your Slowness or Weakness potions in one game."),
                    a("pin-cushion", "Pin Cushion", "As Archer, get 5 bow kills in one game."),
                    a("tnt-rrific", "TNT-rrific", "As Demolitionist, get 3 kills with your thrown TNT in one game."),
                    a("golden-boy", "Golden Boy", "As Fighter, eat a Golden Applegate and get 5 kills without dying in the same life."),
                    a("immovable-object", "Immovable Object", "As Tank, survive 10 enemy hits in one life, then kill an enemy before dying."),
                    a("express-delivery", "Express Delivery", "Destroy an enemy crystal with objective TNT within 15 seconds of picking it up."),
                    a("royal-guard", "Royal Guard", "Kill 3 enemies within 15 blocks of your King in one game."),
                    a("against-all-odds", "Against All Odds", "Win after both of your team’s crystals are destroyed."))),
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
            Map.entry("dragon-escape", List.of(m("wins", "Wins", "wins", 2000, "wins"), m("courses", "Course Completes", "course-completes", 3000, "courses"), m("distance", "Blocks Travelled", "blocks-travelled", 750000, "blocks"), m("world-tour", "World Tour", "world-tour", 1, "maps"))),
            Map.entry("dragons", List.of(m("survival", "Survive", "survival-millis", 36_000_000, "milliseconds"), m("wins", "Wins", "wins", 250, "wins"))),
            Map.entry("evolution", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 2000, "kills"))),
            Map.entry("gladiators", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 1000, "kills"), m("swift-kill", "Swift Kill", "swift-kill", 15, "first bloods"), m("loser-bracket-wins", "Loser Bracket Wins", "loser-bracket-wins", 100, "wins"))),
            Map.entry("micro-battles", List.of(m("wins", "Wins", "wins", 1500, "wins"), m("kills", "Kills", "kills", 5000, "kills"))),

            Map.entry("one-in-the-quiver", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 5000, "kills"))),
            Map.entry("paintball", List.of(m("wins", "Wins", "wins", 5000, "wins"), m("kills", "Kills", "kills", 5000, "kills"), m("revives", "Revives", "medic", 2500, "revives"))),
            Map.entry("runner", List.of(m("wins", "Wins", "wins", 250, "wins"), m("blocks-run", "Blocks Run", "marathon-runner", 100000, "blocks"))),
            Map.entry("sheep-quest", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "kills", 2000, "kills"), m("sheep-captures", "Sheep Captures", "sheep-captures", 1000, "captures"))),
            Map.entry("snake", List.of(m("wins", "Wins", "wins", 250, "wins"), m("kills", "Kills", "lifetime-kills", 750, "kills"))),
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
                    m("tnt-kills", "TNT Kills", "tnt-kills", 100, "TNT kills"),
                    m("tnt-pickups", "Throwable TNT Pickups", "tnt-pickups", 250, "pickups"),
                    m("zombie-kills", "Zombie Guardian Kills", "zombie-kills", 250, "kills"))),
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
