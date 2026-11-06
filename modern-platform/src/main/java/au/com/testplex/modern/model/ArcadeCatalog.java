package au.com.testplex.modern.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Modern catalogue for Arcade games, game-specific cosmetics and win scenes. */
public final class ArcadeCatalog {
    private final Map<String, GameDefinition> games = new LinkedHashMap<>();

    public ArcadeCatalog() {
        register(game("bacon-brawl", "Bacon Brawl", "PORKCHOP", "Torpedo into rivals and launch them from the arena.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("signature-impact", "Signature Impact", "Body-Slam/Torpedo presentation.", "FIREWORK_ROCKET",
                        single("pork-comet", "Pork Comet", "A bacon comet slams into the impact point."),
                        single("crispy-ring", "Crispy Ring", "A sizzling ring expands from the impact point."),
                        single("sauce-splash", "Sauce Splash", "A red-orange splash bursts outward.")),
                     line("bacon-trail", "Bacon Blast Trail", "A short-lived ability-specific trail.", "PORKCHOP",
                        single("grease-skid", "Grease Skid", "A skid mark appears along the dash."),
                        single("smokehouse", "Smokehouse", "A curl of smoke follows the player."))),
                win("lava-arena", "Mighty Lava Arena", "A compact lava-arena podium scene."),
                win("bacon-crown", "Bacon Crown", "A floating bacon crown and victory fanfare."),
                win("crispy-finale", "Crispy Finale", "A sizzling floor burst and smoke columns")));

        register(game("bomb-lobbers", "Bomb Lobbers", "FIRE_CHARGE", "Land direct TNT hits while surviving the Turtles arena.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("tnt-impact", "TNT Impact Signature", "A signature effect on every TNT detonation.", "TNT",
                        single("turtle-shell", "Turtle Shell", "A spinning shell-shaped ring."),
                        single("starburst-tnt", "Starburst TNT", "A radial star-shaped detonation."),
                        single("pixel-crater", "Pixel Crater", "A square shockwave with ember sparks."))),
                win("turtles-dock", "Turtles Dock", "The winner is raised above a miniature turtle dock."),
                win("shell-parade", "Shell Parade", "Spinning shells orbit the winner."),
                win("perfect-throw", "Perfect Throw", "A target reticle locks in before a clean TNT burst")));

        register(game("death-tag", "Death Tag", "SKELETON_SKULL", "Outrun the undead through the village-tree course.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("runner-trail", "Runner Trail Identity", "A recognisable trail silhouette, not just a particle recolour.", "SOUL_LANTERN",
                        single("bark-footprints", "Bark Footprints", "Leaf and bark footprints fade behind the runner."),
                        single("soul-ribbons", "Soul Ribbons", "Two curved soul ribbons trail behind."),
                        single("lantern-motes", "Lantern Motes", "Warm lights rise from each step."))),
                win("barbarians-canopy", "Barbarians Canopy", "The winner appears on a tree platform above the village."),
                win("last-survivor", "Last Survivor", "The arena dims as lanterns light one by one."),
                win("untouched-run", "Untouched Run", "A route map draws itself around the winner")));

        register(game("dragon-escape", "Dragon Escape", "DRAGON_EGG", "Race the dragon through a collapsing parkour route.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("leader-marker", "Course Leader Marker", "A course-specific marker for the current leader.", "BEACON",
                        single("dragon-eye", "Dragon Eye", "An iris-like marker pulses above the leader."),
                        single("sky-compass", "Sky Compass", "A rotating compass motif follows the leader."),
                        single("wing-flare", "Wing Flare", "Wing-shaped particles flash at checkpoints.")),
                     line("leader-beacon", "Leader Beacon Colour", "Choose the visible particle colour around the current course leader.", "BEACON",
                        single("white", "White", "White beacon particles."),
                        single("orange", "Orange", "Orange beacon particles."),
                        single("magenta", "Magenta", "Magenta beacon particles."),
                        single("light-blue", "Light Blue", "Light blue beacon particles."),
                        single("yellow", "Yellow", "Yellow beacon particles."),
                        single("lime", "Lime", "Lime beacon particles."),
                        single("pink", "Pink", "Pink beacon particles."),
                        single("gray", "Gray", "Gray beacon particles."),
                        single("silver", "Silver", "Silver beacon particles."),
                        single("cyan", "Cyan", "Cyan beacon particles."),
                        single("purple", "Purple", "Purple beacon particles."),
                        single("blue", "Blue", "Blue beacon particles."),
                        single("brown", "Brown", "Brown beacon particles."),
                        single("green", "Green", "Green beacon particles."),
                        single("red", "Red", "Red beacon particles."),
                        single("black", "Black", "Black beacon particles.")),
                     line("finish-celebration", "Finish Celebration", "A separate finish-line moment.", "FIREWORK_ROCKET",
                        single("skylands-gate", "Skylands Gate", "A luminous gate opens behind the finisher."),
                        single("dragon-roar", "Dragon Roar", "A dragon silhouette sweeps over the finish."),
                        single("cloud-surf", "Cloud Surf", "Cloud rings cascade down the finish platform."))),
                win("skylands-gate", "Skylands Gate", "A large floating gate frames the winner."),
                win("dragon-perch", "Dragon Perch", "The winner stands on a stylised dragon perch."),
                win("cloud-throne", "Cloud Throne", "A cloud-built throne rises under the winner")));

        register(game("dragons", "Dragons", "END_STONE", "Use Pyro platforms to survive dragon attacks.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("pyro-signature", "Pyro Signature", "Unique shape language for the Pyro ability.", "BLAZE_POWDER",
                        single("ember-wing", "Ember Wing", "A fan of flame-shaped particles."),
                        single("phoenix-arc", "Phoenix Arc", "A phoenix outline arcs through the air."),
                        single("molten-orbit", "Molten Orbit", "Molten sparks orbit the launch point.")),
                     line("sparkler-effects", "Sparkler Effects", "Choose the visual sparkle style emitted by your Pyrotechnic Sparklers. Cosmetic only; it never changes dragon targeting or attraction.", "FIREWORK_ROCKET",
                        single("golden-thread", "Golden Thread", "A warm golden streamer with a soft end-rod shimmer."),
                        single("cinder-drift", "Cinder Drift", "Tiny orange cinders trail behind the sparkler."),
                        single("glacier-dust", "Glacier Dust", "Cool white and pale-blue frost motes."),
                        single("void-motes", "Void Motes", "Violet motes swirl around the sparkler."),
                        single("starlight", "Starlight", "Bright yellow-white star sparks follow the throw."),
                        single("prismatic", "Prismatic", "A shifting multicolour shimmer and enchanting motes."))),
                win("dragon-ring", "Dragon Ring", "Dragons circle the winner."),
                win("phoenix-rise", "Phoenix Rise", "The winner rises through a phoenix-shaped flame arc."),
                win("last-platform", "Last Platform", "The remaining platform is spotlighted as the arena fades")));

        register(game("evolution", "Evolution", "EMERALD",
                "Earn evolutions through kills or the 90-second pity clock; progress through five forms.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("metamorphosis", "Dragon Egg Variants",
                        "Choose the dragon egg and its evolving particle sequence; effects are visible to everyone in the game.",
                        "DRAGON_EGG",
                        single("shell-fragments", "Charged Dragon Egg", "Electric sparks and luminous energy spiral around the egg."),
                        single("void-unfold", "Void Dragon Egg", "Portal and dragon-breath particles spiral around the egg."),
                        single("nature-bloom", "Nature Dragon Egg", "Green motes and pollen bloom around the egg."))),
                win("summit-nest", "Summit Nest", "The winner is framed by a nest at the summit."),
                win("evolution-tree", "Evolution Tree", "A branching evolutionary tree grows behind the winner."),
                win("gene-helix", "Gene Helix", "A double helix spirals into the sky")));

        register(game("gladiators", "Gladiators", "IRON_SWORD", "Fight for the last position in the duel arena.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("duel-intro", "Duel Entrance", "A distinct combat entrance before the first hit.", "IRON_SWORD",
                        single("arena-challenger", "Arena Challenger", "An arena bell and crossed-sword flash."),
                        single("red-sand", "Red Sand", "A compact sand burst under the fighter."),
                        single("oath-mark", "Oath Mark", "An emblem stamps briefly at the player's feet.")),
                     line("kill-message", "Kill Message", "Choose the message everyone sees when you eliminate an opponent.", "WRITABLE_BOOK",
                        single("classic", "Classic", "A straightforward Gladiators elimination message."),
                        single("sent-to-sand", "Sent to the Sand", "A dramatic arena-themed elimination message."),
                        single("broken-oath", "Broken Oath", "A ceremonial challenge-ending message."),
                        single("outclassed", "Outclassed", "A short, competitive kill message."))),
                win("champions-podium", "Champion's Podium", "The champion is raised on a stone podium."),
                win("crossed-steel", "Crossed Steel", "Two swords frame the winner."),
                win("arena-banners", "Arena Banners", "Banners unfurl around the final combatant")));

        register(game("micro-battles", "Micro Battles", "LAVA_BUCKET", "Fight on tiny platforms as the border closes.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(lineMulti("island-visuals", "Island Glass & Border Colours",
                                "Equip one private starting-glass colour and one shared border-particle colour.", "LIGHT_BLUE_STAINED_GLASS",
                        grouped("glass-light-blue", "Light Blue Island Glass", "Only you see this light blue starting-glass colour.", "glass"),
                        grouped("glass-white", "White Island Glass", "Only you see this white starting-glass colour.", "glass"),
                        grouped("glass-orange", "Orange Island Glass", "Only you see this orange starting-glass colour.", "glass"),
                        grouped("glass-magenta", "Magenta Island Glass", "Only you see this magenta starting-glass colour.", "glass"),
                        grouped("glass-yellow", "Yellow Island Glass", "Only you see this yellow starting-glass colour.", "glass"),
                        grouped("glass-lime", "Lime Island Glass", "Only you see this lime starting-glass colour.", "glass"),
                        grouped("glass-pink", "Pink Island Glass", "Only you see this pink starting-glass colour.", "glass"),
                        grouped("glass-gray", "Gray Island Glass", "Only you see this gray starting-glass colour.", "glass"),
                        grouped("glass-light-gray", "Light Gray Island Glass", "Only you see this light gray starting-glass colour.", "glass"),
                        grouped("glass-cyan", "Cyan Island Glass", "Only you see this cyan starting-glass colour.", "glass"),
                        grouped("glass-purple", "Purple Island Glass", "Only you see this purple starting-glass colour.", "glass"),
                        grouped("glass-blue", "Blue Island Glass", "Only you see this blue starting-glass colour.", "glass"),
                        grouped("glass-brown", "Brown Island Glass", "Only you see this brown starting-glass colour.", "glass"),
                        grouped("glass-green", "Green Island Glass", "Only you see this green starting-glass colour.", "glass"),
                        grouped("glass-red", "Red Island Glass", "Only you see this red starting-glass colour.", "glass"),
                        grouped("glass-black", "Black Island Glass", "Only you see this black starting-glass colour.", "glass"),
                        grouped("border-light-blue", "Light Blue Border Particles", "Everyone sees your light blue border-warning particles.", "border"),
                        grouped("border-white", "White Border Particles", "Everyone sees your white border-warning particles.", "border"),
                        grouped("border-orange", "Orange Border Particles", "Everyone sees your orange border-warning particles.", "border"),
                        grouped("border-magenta", "Magenta Border Particles", "Everyone sees your magenta border-warning particles.", "border"),
                        grouped("border-yellow", "Yellow Border Particles", "Everyone sees your yellow border-warning particles.", "border"),
                        grouped("border-lime", "Lime Border Particles", "Everyone sees your lime border-warning particles.", "border"),
                        grouped("border-pink", "Pink Border Particles", "Everyone sees your pink border-warning particles.", "border"),
                        grouped("border-gray", "Gray Border Particles", "Everyone sees your gray border-warning particles.", "border"),
                        grouped("border-light-gray", "Light Gray Border Particles", "Everyone sees your light gray border-warning particles.", "border"),
                        grouped("border-cyan", "Cyan Border Particles", "Everyone sees your cyan border-warning particles.", "border"),
                        grouped("border-purple", "Purple Border Particles", "Everyone sees your purple border-warning particles.", "border"),
                        grouped("border-blue", "Blue Border Particles", "Everyone sees your blue border-warning particles.", "border"),
                        grouped("border-brown", "Brown Border Particles", "Everyone sees your brown border-warning particles.", "border"),
                        grouped("border-green", "Green Border Particles", "Everyone sees your green border-warning particles.", "border"),
                        grouped("border-red", "Red Border Particles", "Everyone sees your red border-warning particles.", "border"),
                        grouped("border-black", "Black Border Particles", "Everyone sees your black border-warning particles.", "border"))),
                win("last-tile", "Last Tile", "Only the winner's final tile stays lit."),
                win("micro-monument", "Micro Monument", "A tiny monument assembles around the winner."),
                win("falling-stars", "Falling Stars", "A narrow star shower descends onto the platform")));

        register(monsterMazeGame("monster-maze", "Monster Maze (Original)",
                "The original Monster Maze experience, recreated as faithfully as possible."));
        register(monsterMazeModeGame("monster-maze-speed", "Monster Maze (Speed)",
                "The Speed mode keeps the original timer and monster spawning with the Speed gameplay rules."));
        register(monsterMazeModeGame("monster-maze-modern", "Monster Maze (Modern)",
                "The Modern mode accelerates stages, increases monster counts and expands kit abilities."));

        register(game("one-in-the-quiver", "One in the Quiver", "ARROW", "Land the decisive bow shot with one arrow at a time.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("arrow-flight", "Arrow Flight Signature", "A distinct arrow-flight trail and hit confirm.", "SPECTRAL_ARROW",
                        single("bullseye-dash", "Bullseye Dash", "A short target line follows the arrow."),
                        single("fletching-sparks", "Fletching Sparks", "Feather-shaped sparks mark the trajectory."),
                        single("ricochet-rune", "Ricochet Rune", "A rune marks the impact point.")),
                     line("death-streak", "Streak Taunts & Effects",
                        "A public killstreak announcement and visible effect at each defeated opponent; everyone in the arena sees it.", "FIREWORK_ROCKET",
                        single("subtle", "Subtle", "A compact critical-hit sparkle at the elimination point."),
                        single("burst", "Burst", "A larger firework-spark burst at the elimination point."),
                        single("bullseye", "Bullseye", "A red target ring appears around the elimination point."),
                        single("grand-finale", "Grand Finale", "An explosion flash, note burst and level-up sound."))),
                win("bullseye-stage", "Bullseye Stage", "A target board grows behind the winner."),
                win("quiverless", "Quiverless", "A single arrow hangs above the last archer."),
                win("archer-halo", "Archer Halo", "An archer's halo draws around the winner")));

        register(game("paintball", "Paintball", "ENDER_PEARL", "Control lanes with paintball weapons and team colours.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(lineMulti("weapon-and-pellet-identity", "Weapon & Pellet Identity",
                        "Equip one visible weapon finish and one visible in-flight pellet style. Both are shown to all players.",
                        "CROSSBOW",
                        grouped("chrome-marker", "Chrome Marker", "A reflective metal finish for your held paintball weapon.", "weapon"),
                        grouped("splatter-cannon", "Splatter Cannon", "A chunky crossbow-like paint cannon finish.", "weapon"),
                        grouped("neon-suppressor", "Neon Suppressor", "A bright amethyst-and-neon weapon finish.", "weapon"),
                        grouped("colourburst", "Colourburst Pellets", "A radial team-coloured paint bloom follows every pellet.", "pellet"),
                        grouped("paint-drip", "Paint Drip Pellets", "A droplet-shaped team-coloured trail follows every pellet.", "pellet"),
                        grouped("stencil-stamp", "Stencil Stamp Pellets", "A compact square stencil pattern follows every pellet.", "pellet"))),
                win("paint-wall", "Paint Wall", "A team-coloured mural paints in behind the winner."),
                win("marker-medal", "Marker Medal", "A medal assembles from paint-marker parts."),
                win("splatter-crown", "Splatter Crown", "A crown of layered paint splashes")));

        register(game("runner", "Runner", "LEATHER_BOOTS", "Survive by running the course without falling into gaps.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("running-trail-colour", "Running Trail Colour",
                        "Choose a trail colour. Red is the source default and fallback for map clashes.",
                        "GLOW_INK_SAC",
                        grouped("trail-red", "Red", "The original red running trail.", "trail"),
                        grouped("trail-cyan", "Cyan", "A bright cyan running trail.", "trail"),
                        grouped("trail-white", "White", "A clean white running trail.", "trail"),
                        grouped("trail-orange", "Orange", "A vivid orange running trail.", "trail"),
                        grouped("trail-magenta", "Magenta", "A bright magenta running trail.", "trail"),
                        grouped("trail-light-blue", "Light Blue", "A light-blue running trail.", "trail"),
                        grouped("trail-yellow", "Yellow", "A bright yellow running trail.", "trail"),
                        grouped("trail-lime", "Lime", "A high-visibility lime running trail.", "trail"),
                        grouped("trail-pink", "Pink", "A pink running trail.", "trail"),
                        grouped("trail-gray", "Gray", "A neutral gray running trail.", "trail"),
                        grouped("trail-light-gray", "Light Gray", "A light-gray running trail.", "trail"),
                        grouped("trail-purple", "Purple", "A purple running trail.", "trail"),
                        grouped("trail-blue", "Blue", "A blue running trail.", "trail"),
                        grouped("trail-brown", "Brown", "A brown running trail.", "trail"),
                        grouped("trail-green", "Green", "A green running trail.", "trail"),
                        grouped("trail-black", "Black", "A dark running trail.", "trail"))),
                win("route-replay", "Route Replay", "The final route replays as a floating ribbon."),
                win("finish-tape", "Finish Tape", "A finish-line tape snaps across the arena."),
                win("runner-statue", "Runner Statue", "A running silhouette assembles into a statue")));

        register(game("sheep-quest", "Sheep Quest", "YELLOW_WOOL", "Capture sheep and return them to your team's pen.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("capture-signature", "Capture Signature", "Distinct feedback when a sheep is secured.", "LEAD",
                        single("sheep-bell", "Sheep Bell", "A distinct bell and wool puff."),
                        single("wool-confetti", "Wool Confetti", "Colour-coded wool flecks burst outward."),
                        single("shepherd-flare", "Shepherd Flare", "A shepherd's-crook motif appears."))),
                win("pen-parade", "Pen Parade", "The captured flock parades around the winning pen."),
                win("golden-fleece", "Golden Fleece", "A golden fleece appears on a wool pedestal."),
                win("shepherd-crown", "Shepherd Crown", "A leafy crown forms above the winner")));

        register(game("snake", "Snake", "WHITE_WOOL", "Collect food, avoid collisions, and use the Mario-Kart-style item roll.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("snake-identity", "Snake Identity", "A connected body pattern, not one generic particle trail.", "SLIME_BALL",
                        single("anaconda", "Anaconda", "Segmented heavy scales along the body."),
                        single("cobra", "Cobra", "A raised hood silhouette and alternating scales."),
                        single("neon-viper", "Neon Viper", "A two-tone luminous stripe."),
                        single("reef-serpent", "Reef Serpent", "Coral-like fins along the body.")),
                     line("item-roll-visual", "Item Roll Visual", "The hotbar roll presentation; effect timing remains gameplay-owned.", "CHEST_MINECART",
                        single("slot-reel", "Slot Reel", "A rapid item-strip animation."),
                        single("dice-lock", "Dice Lock", "An animated dice reveal with a lock-in pulse."),
                        single("roulette-orbit", "Roulette Orbit", "Items orbit briefly before the selected effect appears."))),
                win("race-lap", "Final Lap", "A lap counter and racing pennants appear."),
                win("snake-podium", "Snake Podium", "The winner coils around a compact podium."),
                win("fruit-fountain", "Fruit Fountain", "Food items spiral into a colourful fountain")));

        register(game("spleef", "Spleef", "IRON_SHOVEL", "Break the floor beneath opponents and stay above the void.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("break-signature", "Block Break Signature", "A game-specific shape for each block break.", "IRON_SHOVEL",
                        grouped("snowburst", "Snowburst", "A short snow burst at the broken tile.", "signature"),
                        grouped("ice-shards", "Ice Shards", "Crystalline fragments spread outward.", "signature"),
                        grouped("chalk-grid", "Chalk Grid", "The broken tile briefly outlines a grid.", "signature"))),
                win("ice-throne", "Ice Throne", "A seat of ice rises from the surviving floor."),
                win("last-snowflake", "Last Snowflake", "A single snowflake floats above the winner."),
                win("shovel-salute", "Shovel Salute", "A ceremonial row of shovels rises from the floor")));

        register(game("draw-my-thing", "Draw My Thing", "WRITABLE_BOOK",
                "Take one 30-second drawing turn, reveal timed hints, and guess using a limited number of attempts.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("whiteboard-reveal", "Whiteboard Reveal", "The final drawing is framed above the winning guesser.")));

        register(game("master-builders", "Master Builders", "OAK_PLANKS",
                "Build your theme in three minutes, then review and rate the other plots.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("builders-podium", "Builder's Podium", "The top-rated build rises above the winning builder.")));

        register(game("castle-assault", "Castle Assault", "DIAMOND_CHESTPLATE",
                "Destroy the enemy sentry crystals and defeat their King. Mixed Arcade doubles objective TNT spawns.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("castle-breach", "Castle Breach", "The enemy castle gates collapse around the winning team."),
                win("kingfall", "Kingfall", "The defeated King falls as the winning banner rises."),
                win("sentry-shatter", "Sentry Shatter", "The last sentry crystal breaks in a burst of red sparks.")));

        register(game("heroes-of-gwen", "Heroes of Gwen", "PRISMARINE_BRICKS",
                "Choose a hero, capture beacons, destroy enemy towers and defeat the opposing Wither in the original Heroes of Gwen Arcade battle.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("gwen-throne", "Gwen Throne", "A crystal throne rises above the winning hero."),
                win("beacon-crown", "Beacon Crown", "Captured beacons form a crown around the victor."),
                win("wither-fall", "Wither Fall", "The defeated Wither fades behind the winning team.")));

        register(game("hoops-remastered", "Hoops Remastered", "MAGMA_CREAM",
                "Play a fast 2–16 player RED versus BLUE basketball match with passing, shooting, layups, dunks, steals and blocks.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("hoops-trophy", "Hoops Trophy", "The winning team raises the championship cup above the court."),
                win("buzzer-beater", "Buzzer Beater", "The decisive final basket lights up the arena clock."),
                win("court-dominance", "Court Dominance", "The winning lineup takes centre court.")));

        register(game("wizards", "Wizards", "BLAZE_ROD",
                "Choose a wizard kit, explore the original Wizards and Nano Wizards maps, collect spellbooks and be the last wizard standing.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("last-wizard-standing", "Last Wizard Standing", "The last wizard alive claims the arena."),
                win("arcane-survivor", "Arcane Survivor", "Survive the final endgame strikes."),
                win("spellmaster", "Spellmaster", "A chain of spells marks the winning mage.")));

        register(game("milk-the-cow", "Milk the Cow", "MILK_BUCKET",
                "Collect milk for your team, protect the herd and use Cow charge and body-slam abilities.",
                GameDefinition.PortStatus.READY_FOR_TEST, List.of(),
                win("dairy-champion", "Dairy Champion", "The winning farmer is celebrated above the dairy arena")));

        register(game("wither-assault", "Wither Assault", "WITHER_SKELETON_SKULL",
                "Defend your team and control the Wither Assault objective using source-derived movement rules. Controlled testing uses a generated arena; original maps and Medic passives remain outstanding.",
                GameDefinition.PortStatus.READY_FOR_TEST, List.of(),
                win("wither-victory", "Wither Victory", "The surviving team stands above the defeated Wither")));

        register(game("skywars", "SkyWars Mixed", "FEATHER",
                "Fast 16-player Mixed Arcade free-for-all: loot your island, throw TNT and survive the collapse.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("sky-combat-signature", "Sky Combat Signature",
                        "A visible identity for throwable TNT and fast island collapse.", "TNT",
                        single("tnt-burst", "TNT Burst", "A bright TNT burst with smoke and ember sparks."),
                        single("skyfall", "Skyfall", "A short wind ring marks the explosion path."),
                        single("island-collapse", "Island Collapse", "A block-shatter cue marks the island crumble."))),
                win("sky-island", "Sky Island", "The winner is raised above a floating island."),
                win("last-survivor", "Last Survivor", "A beacon shines over the last player standing."),
                win("skyfall-crown", "Skyfall Crown", "A floating crown of cloud and TNT frames the winner.")));

        register(game("turf-wars", "Turf Wars", "RED_TERRACOTTA", "Win ranged combat and control the central turf.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(line("arrow-mark", "Arrow Mark", "A block-impact mark that follows the player's team palette.", "TARGET",
                        single("team-sigil", "Team Sigil", "A team sigil appears at the impact point."),
                        single("turf-crack", "Turf Crack", "A short crack traces from the hit."),
                        single("painted-chevron", "Painted Chevron", "A bold directional chevron is stamped on the wall."))),
                win("turf-monument", "Turf Monument", "A team monument rises out of captured turf."),
                win("stadium-finale", "Stadium Finale", "Stadium lights sweep towards the winning team."),
                win("banner-wave", "Banner Wave", "A moving wall of team banners celebrates the winner")));

        register(game("sneaky-assassins", "Sneaky Assassins", "INK_SAC",
                "Stay disguised as a villager, collect power-ups and be the last assassin alive. Controlled testing uses a generated town while authentic map and NMS patrol parity remain outstanding.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("last-assassin-standing", "Last Assassin Standing",
                        "A simple survivor podium; no additional game-specific cosmetics are claimed yet.")));

        register(game("camera-shy", "Snap", "SPYGLASS",
                "Photograph opponents with a spyglass while avoiding five exposures. Last photographer standing wins.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("last-photographer-standing", "Last Photographer Standing",
                        "The winner is celebrated as the last photographer remaining in the maze.")));

        register(game("plexball", "Plexball", "CHAINMAIL_HELMET",
                "Take turns hitting a ball off a tee while every non-batter tries to catch it. Longest uncaught hit wins.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("home-run-stage", "Home Run Stage", "A 100-metre marker rises behind the winner."),
                win("golden-glove", "Golden Glove", "A golden glove and ball form a compact victory display.")));

        register(game("super-sploor", "Super Sploor", "DIAMOND_HORSE_ARMOR",
                "Paint and reclaim territory as a five-kit team in the NANO-inspired Squid Turf War.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("colour-surge", "Colour Surge", "A vivid wash of team-coloured ink rolls across the winning team's podium.")));

        register(game("speed-builders", "Speed Builders", "QUARTZ_BLOCK",
                "Recreate Mineplex build models in your own plot. Up to sixteen builders compete across synchronized mirrored arenas, scored automatically by build accuracy.",
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("highest-accuracy", "Highest Accuracy",
                        "The builder with the highest cumulative model accuracy is announced as the winner.")));
    }

    /** Speed and Modern share Original's one player-facing Monster Maze cosmetics page. */
    private static GameDefinition monsterMazeModeGame(String id, String name, String description) {
        return game(id, name, "ROTTEN_FLESH", description,
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(),
                win("maze-blueprint", "Maze Blueprint", "A miniature maze trace forms above the winner."));
    }

    private static GameDefinition monsterMazeGame(String id, String name, String description) {
        return game(id, name, "ROTTEN_FLESH", description,
                GameDefinition.PortStatus.READY_FOR_TEST,
                List.of(lineMulti("mob-sets", "Mob Forms", "Enable multiple mob forms independently for the round.", "CREEPER_HEAD",
                                single("creeper", "Creeper", "Creeper form"),
                                single("skeleton", "Skeleton", "Skeleton form"),
                                single("spider", "Spider", "Spider form"),
                                single("zombie", "Zombie", "Zombie form"),
                                single("witch", "Witch", "Witch form"),
                                single("slime", "Slime", "Slime form"),
                                single("enderman", "Enderman", "Enderman form"),
                                single("guardian", "Guardian", "Guardian form")),
                        line("objective-palette", "Objective Palette per Mob Set",
                                "Keep beacon-glass colour and Safe Pad surface colour independent for every mob set.",
                                "STAINED_GLASS",
                                single("per-mob-set", "Configure Each Mob Set",
                                        "Select a mob set, then edit beacon and pad colours independently."))),
                win("maze-blueprint", "Maze Blueprint", "A miniature maze trace forms above the winner."),
                win("mob-procession", "Mob Procession", "The round's selected mob forms parade behind the winner."),
                win("safe-pad-coronation", "Safe Pad Coronation", "The Safe Pad raises into a compact victory dais"));
    }

    private void register(GameDefinition definition) {
        List<CosmeticLineDefinition> lines = approvedCosmeticLines(definition);
        GameDefinition normalized = new GameDefinition(definition.getId(), definition.getName(),
                definition.getDescription(), definition.getIcon(), definition.getPortStatus(), lines,
                definition.getWinEffects(), definition.getDefaultWinEffectId());
        if (games.putIfAbsent(normalized.getId(), normalized) != null) {
            throw new IllegalArgumentException("Duplicate Arcade game id: " + definition.getId());
        }
    }

    /** Keep only the game-specific cosmetic lines reviewed and approved for testing. */
    private static List<CosmeticLineDefinition> approvedCosmeticLines(GameDefinition game) {
        String id = game.getId();
        return switch (id) {
            case "bacon-brawl" -> List.of(line("ability-effects", "Bacon Ability Styles",
                    "One visual style ties together this player's kit abilities.", "PORKCHOP",
                    style("blue-flame", "Blue Flame", "Blue fire and matching accents across Bacon Brawl abilities."),
                    style("crispy-comet", "Crispy Comet", "A warm comet finish across this player's abilities."),
                    style("smokehouse", "Smokehouse", "Smoky curls and ember accents across abilities."),
                    style("molten-pepper", "Molten Pepper", "A molten orange theme across the kit."),
                    style("frosted-bacon", "Frosted Bacon", "Cool frost accents across the kit.")));
            case "bomb-lobbers" -> List.of(line("tnt-explosions", "TNT Explosion Effects",
                    "The regular TNT explosion always plays; this adds the thrower's chosen visual effect.", "TNT",
                    style("ember-ring", "Ember Ring", "An extra ring of orange sparks around the blast."),
                    style("arcane-burst", "Arcane Burst", "A brief burst of coloured magical runes."),
                    style("frost-shards", "Frost Shards", "A spray of icy shards around the blast."),
                    style("starburst", "Starburst", "A bright radial burst of coloured sparks.")));
            case "castle-assault" -> List.of(line("siege-effects", "Siege Effects",
                    "Extend the source TNT-holder particles and accent crystals broken by this player.", "TNT",
                    style("royal-gold", "Royal Gold", "Golden sparks around carried TNT and crystal breaks."),
                    style("void-siege", "Void Siege", "A purple runic accent on carried TNT and crystal breaks."),
                    style("verdant-siege", "Verdant Siege", "A green shard accent on carried TNT and crystal breaks.")));
            case "dragon-escape" -> List.of(line("winner-beacon-colour", "Winner Beacon Colour",
                    "When this player wins, their selected colour changes the finish beacon for everyone.", "BEACON",
                    color("white", "White"), color("orange", "Orange"), color("magenta", "Magenta"),
                    color("light-blue", "Light Blue"), color("yellow", "Yellow"), color("lime", "Lime"),
                    color("pink", "Pink"), color("gray", "Gray"), color("silver", "Silver"),
                    color("cyan", "Cyan"), color("purple", "Purple"), color("blue", "Blue"),
                    color("brown", "Brown"), color("green", "Green"), color("red", "Red"),
                    color("black", "Black")));
            case "dragons" -> List.of(line("sparkler-effects", "Pyro Sparkler Effects",
                    "The sparkler owner's style controls its particles when the sparkler lands; everyone sees them.",
                    "FIREWORK_ROCKET",
                    style("golden-thread", "Golden Thread", "A warm golden streamer and soft end-rod shimmer."),
                    style("cinder-drift", "Cinder Drift", "Tiny orange cinders drift from the landing point."),
                    style("glacier-dust", "Glacier Dust", "Cool white and pale-blue frost motes."),
                    style("void-motes", "Void Motes", "Violet motes swirl around the landing point."),
                    style("starlight", "Starlight", "Bright star sparks flare where the sparkler lands."),
                    style("prismatic", "Prismatic", "A multicolour shimmer and enchanting motes.")));
            case "evolution" -> List.of(line("metamorphosis", "Evolution Styles",
                    "Apply the chosen style to the evolving player, their egg and the transformation particles.",
                    "DRAGON_EGG",
                    style("shell-fragments", "Charged Dragon Egg", "Electric sparks spiral around the player and egg."),
                    style("void-unfold", "Void Dragon Egg", "Portal particles spiral around the player and egg."),
                    style("nature-bloom", "Nature Dragon Egg", "Green motes bloom around the player and egg.")));
            case "gladiators" -> List.of(line("score-flares", "Gladiators Score Flares",
                    "The scorer's selected flare triggers at their kill and is visible to everyone.", "FIREWORK_ROCKET",
                    style("golden-burst", "Golden Burst", "A compact golden flare at the duel's scoring moment."),
                    style("court-confetti", "Arena Confetti", "A short coloured confetti flourish after a point."),
                    style("starflare", "Starflare", "A sharp star-shaped light burst when you score.")));
            case "hoops-remastered" -> List.of(line("score-flares", "Score Celebration Flares",
                    "The scorer's selected flare triggers at the hoop and is visible to everyone.", "FIREWORK_ROCKET",
                    style("golden-burst", "Golden Burst", "A compact golden flare at the scoring hoop."),
                    style("court-confetti", "Court Confetti", "A short team-coloured confetti flourish."),
                    style("starflare", "Starflare", "A sharp star-shaped light burst.")));
            case "micro-battles" -> List.of(line("start-glass-colour", "Start Glass Colour",
                    "Colour your starting glass for everyone; the same colour marks you near the upper border.",
                    "LIGHT_BLUE_STAINED_GLASS",
                    color("white", "White"), color("orange", "Orange"), color("magenta", "Magenta"),
                    color("light-blue", "Light Blue"), color("yellow", "Yellow"), color("lime", "Lime"),
                    color("pink", "Pink"), color("gray", "Gray"), color("silver", "Silver"),
                    color("cyan", "Cyan"), color("purple", "Purple"), color("blue", "Blue"),
                    color("brown", "Brown"), color("green", "Green"), color("red", "Red"),
                    color("black", "Black")));
            case "monster-maze", "monster-maze-speed", "monster-maze-modern" -> List.of(monsterMazeMobSetLine());
            case "paintball" -> List.of(line("pellet-styles", "Paintball Pellet Styles",
                    "Choose the appearance of your paint pellets; team identity and hits remain unchanged.", "SNOWBALL",
                    style("classic-pellet", "Classic Pellet", "A clean, compact paint pellet."),
                    style("prism-pellet", "Prism Pellet", "A bright faceted pellet with a team-coloured core."),
                    style("comet-pellet", "Comet Pellet", "A sleek pellet with a short team-coloured streak."),
                    style("crystal-pellet", "Crystal Pellet", "A crisp crystal-like pellet, tinted for its team.")));
            case "plexball" -> List.of(line("ball-styles", "Plexball Styles",
                    "The ball keeps its owner's appearance while held and in flight; gameplay is unchanged.",
                    "MAGMA_CREAM",
                    style("carbon", "Carbon", "A dark carbon-fibre finish."),
                    style("prism", "Prism", "A bright prismatic finish."),
                    style("solar", "Solar", "A warm solar-orange finish.")));
            case "runner" -> List.of(line("trail-colour", "Runner Trail Colour",
                    "Colour the short-lived trail on blocks you run over; it adapts to the map palette. Red is the fallback.",
                    "RED_CONCRETE",
                    color("red", "Classic Red"), color("cyan", "Cyan"), color("orange", "Orange"),
                    color("magenta", "Magenta"), color("yellow", "Yellow"), color("lime", "Lime"),
                    color("pink", "Pink"), color("purple", "Purple"), color("white", "White")));
            case "sheep-quest" -> List.of(line("capture-effects", "Sheep Capture Effects",
                    "The capturer's selected effect plays at the capture point, visible to everyone.", "WHITE_WOOL",
                    style("wool-burst", "Wool Burst", "A compact wool-coloured burst at capture."),
                    style("golden-bell", "Golden Bell", "A brief golden bell flourish at capture."),
                    style("pasture-bloom", "Pasture Bloom", "A short green bloom around the captured sheep.")));
            case "snake" -> List.of(line("ability-effects", "Snake Ability Effects",
                    "Each ability has a distinct signature within the selected visual theme.", "SLIME_BALL",
                    style("anaconda", "Anaconda", "Scale pulses distinguish each ability in a shared green theme."),
                    style("cobra", "Cobra", "Hood flashes and distinct scale marks identify each ability."),
                    style("neon-viper", "Neon Viper", "Two-tone luminous effects distinguish each ability.")));
            case "super-sploor" -> List.of(line("ability-effects", "Super Sploor Ability Effects",
                    "Each ability has a distinct, recognizable effect within one cohesive theme.", "INK_SAC",
                    style("ink-pearl", "Ink & Pearl", "Dark ink swirls and pale pearl flashes distinguish each ability."),
                    style("deep-current", "Deep Current", "Teal currents and bubbles distinguish each ability."),
                    style("reef-burst", "Reef Burst", "Coral and cyan flares distinguish each ability.")));
            case "wizards" -> List.of(line("spell-effects", "Wizard Spell Effects",
                    "Each spell keeps its own recognizable visual within the chosen theme.", "BLAZE_ROD",
                    style("arcane-script", "Arcane Script", "Distinct rune patterns mark each spell."),
                    style("frost-runes", "Frost Runes", "Ice sigils and sharp frost accents mark each spell."),
                    style("ember-spiral", "Ember Spiral", "Warm ember spirals give each spell a clear signature.")));
            case "camera-shy" -> List.of(line("camera-flash", "Camera Flash Styles",
                    "The photographer's choice styles each F-press flash and successful photo confirmation.",
                    "SPYGLASS",
                    style("vintage-flash", "Vintage Flash", "A warm camera flash with a brief frame accent."),
                    style("neon-shutter", "Neon Shutter", "A crisp neon shutter flash."),
                    style("polaroid-frame", "Polaroid Frame", "A compact white frame flash and clean confirmation.")));
            case "death-tag", "heroes-of-gwen", "one-in-the-quiver", "master-builders",
                    "milk-the-cow", "sneaky-assassins", "skywars", "speed-builders", "spleef",
                    "turf-wars", "wither-assault" -> List.of();
            default -> List.of();
        };
    }

    private static CosmeticOption color(String id, String name) {
        return single(id, name, "Use the " + name.toLowerCase(Locale.ROOT) + " colour.");
    }

    /** One cohesive, game-scoped style choice changes ability/action feedback as a set. */
    private static CosmeticLineDefinition abilityEffectsLine(GameDefinition game) {
        return line("ability-effects", game.getName() + " Ability Effects",
                "Choose one visual style for this game's abilities and action feedback. "
                        + "It does not change global cosmetics or victory celebrations.",
                game.getIcon(), abilityEffectStyles(game.getId()));
    }

    private static CosmeticOption[] abilityEffectStyles(String gameId) {
        return switch (gameId) {
            case "bacon-brawl" -> new CosmeticOption[] {
                    style("crispy-comet", "Crispy Comet", "Warm bacon sparks, smoky trails and a bright impact ring."),
                    style("blue-flame", "Blue Flame", "Cool blue fire traces every kit ability and hit."),
                    style("smokehouse", "Smokehouse", "Smoked ember curls and dark, quick impact bursts.")};
            case "bomb-lobbers" -> new CosmeticOption[] {
                    style("turtle-shell", "Turtle Shell", "Shell-shaped rings mark throws, impacts and blocks."),
                    style("starburst-tnt", "Starburst TNT", "A bright radial flash carries through every blast."),
                    style("pixel-crater", "Pixel Crater", "Square shockwaves and ember sparks mark the action.")};
            case "death-tag" -> new CosmeticOption[] {
                    style("soul-ribbons", "Soul Ribbons", "Soul wisps and curved ribbons mark chases and hits."),
                    style("lantern-motes", "Lantern Motes", "Warm motes trace movement, tags and escapes."),
                    style("bone-dust", "Bone Dust", "Pale fragments and sharp white impact flashes.")};
            case "dragon-escape" -> new CosmeticOption[] {
                    style("dragon-eye", "Dragon Eye", "An iris pulse marks boosts, checkpoints and the finish."),
                    style("wing-flare", "Wing Flare", "Wing-shaped bursts follow movement abilities."),
                    style("cloud-surf", "Cloud Surf", "Cloud rings and sky-blue trails mark course actions.")};
            case "dragons" -> new CosmeticOption[] {
                    style("phoenix-arc", "Phoenix Arc", "A phoenix outline traces Pyro launches and impacts."),
                    style("molten-orbit", "Molten Orbit", "Molten sparks spiral around ability effects."),
                    style("glacier-dust", "Glacier Dust", "Cool frost motes and crystal flashes follow abilities.")};
            case "evolution" -> new CosmeticOption[] {
                    style("charged-shell", "Charged Shell", "Electric sparks spiral through each evolution effect."),
                    style("void-unfold", "Void Unfold", "Portal fragments open and fold around ability effects."),
                    style("nature-bloom", "Nature Bloom", "Green motes and petals bloom at each action.")};
            case "gladiators" -> new CosmeticOption[] {
                    style("oath-mark", "Oath Mark", "A restrained arena sigil marks combat abilities."),
                    style("red-sand", "Red Sand", "Red sand bursts and crossed-sword impact flashes."),
                    style("arena-challenger", "Arena Challenger", "A bright duel bell and steel glint mark each move.")};
            case "micro-battles" -> new CosmeticOption[] {
                    style("cracked-ice", "Cracked Ice", "Ice fractures mark hits, kit abilities and border pressure."),
                    style("prism-grid", "Prism Grid", "Prismatic lines frame combat and island effects."),
                    style("molten-seam", "Molten Seam", "Hot cracks and ember pips mark each action.")};
            case "one-in-the-quiver" -> new CosmeticOption[] {
                    style("bullseye-dash", "Bullseye Dash", "Target lines trace shots and impact confirms."),
                    style("fletching-sparks", "Fletching Sparks", "Feather-like sparks follow shots and kit actions."),
                    style("ricochet-rune", "Ricochet Rune", "A compact rune marks every ability and hit.")};
            case "paintball" -> new CosmeticOption[] {
                    style("chrome-marker", "Chrome Marker", "A polished marker with bright, clean paint impacts."),
                    style("colourburst", "Colourburst", "A shared radial paint bloom styles weapons and pellets."),
                    style("stencil-stamp", "Stencil Stamp", "A bold stencil pattern carries across every kit action.")};
            case "runner" -> new CosmeticOption[] {
                    style("trail-red", "Classic Red", "The source red trail styles every Runner ability."),
                    style("trail-cyan", "Cyan Dash", "Cyan ribbons trace jumps, swaps and movement abilities."),
                    style("trail-chalk", "Chalk Dashes", "Chalk strokes and route marks style every ability.")};
            case "sheep-quest" -> new CosmeticOption[] {
                    style("sheep-bell", "Sheep Bell", "A clear bell and wool puff mark abilities and captures."),
                    style("wool-confetti", "Wool Confetti", "Team-coloured wool flecks follow every action."),
                    style("shepherd-flare", "Shepherd Flare", "A crook-shaped flare ties movement and capture effects together.")};
            case "snake" -> new CosmeticOption[] {
                    style("anaconda", "Anaconda", "Segmented scale pulses mark movement and item abilities."),
                    style("cobra", "Cobra", "A raised hood flash and alternating scales mark actions."),
                    style("neon-viper", "Neon Viper", "A two-tone luminous stripe follows each ability.")};
            case "spleef" -> new CosmeticOption[] {
                    style("snowburst", "Snowburst", "A compact snow burst marks shovel hits and abilities."),
                    style("ice-shards", "Ice Shards", "Crystalline fragments carry through the game's actions."),
                    style("chalk-grid", "Chalk Grid", "A sharp grid outline marks breaks and kit abilities.")};
            case "draw-my-thing" -> new CosmeticOption[] {
                    style("ink-splash", "Ink Splash", "Colourful ink marks drawing tools and correct guesses."),
                    style("chalk-stroke", "Chalk Stroke", "Soft chalk strokes style drawing and hint feedback."),
                    style("prism-ink", "Prism Ink", "Prismatic strokes connect drawing and round feedback.")};
            case "master-builders" -> new CosmeticOption[] {
                    style("blueprint-pulse", "Blueprint Pulse", "Blueprint lines mark block placement and scoring."),
                    style("builder-spark", "Builder Spark", "A brief spark follows successful build actions."),
                    style("palette-flare", "Palette Flare", "Colour swatches pulse on block placement and review.")};
            case "castle-assault" -> new CosmeticOption[] {
                    style("crystal-fracture", "Crystal Fracture", "Crystal shards mark objective damage and abilities."),
                    style("siege-spark", "Siege Spark", "A compact blast signature follows siege actions."),
                    style("gate-rune", "Gate Rune", "Runic pulses tie combat abilities to castle objectives.")};
            case "heroes-of-gwen" -> new CosmeticOption[] {
                    style("beacon-pulse", "Beacon Pulse", "A beacon ring follows hero abilities and captures."),
                    style("tower-impact", "Tower Impact", "Stone and crystal bursts mark hero attacks."),
                    style("arcane-banner", "Arcane Banner", "A coloured hero sigil trails abilities and movement.")};
            case "hoops-remastered" -> new CosmeticOption[] {
                    style("court-trail", "Court Trail", "A court-line trail styles passes, steals and shots."),
                    style("rim-spark", "Rim Spark", "A bright rim burst marks blocks, dunks and makes."),
                    style("orange-comet", "Orange Comet", "A basketball-coloured comet follows every court action.")};
            case "wizards" -> new CosmeticOption[] {
                    style("arcane-glyph", "Arcane Glyph", "A rune signature follows spells and kit abilities."),
                    style("elemental-burst", "Elemental Burst", "An elemental flare styles every spell impact."),
                    style("void-script", "Void Script", "Violet glyphs trail casts and spellbook actions.")};
            case "milk-the-cow" -> new CosmeticOption[] {
                    style("dairy-splash", "Dairy Splash", "A clean splash marks Cow charges and body slams."),
                    style("hoof-quake", "Hoof Quake", "Hoof-shaped shockwaves mark movement and hits."),
                    style("hay-bloom", "Hay Bloom", "Warm hay motes follow abilities and team actions.")};
            case "wither-assault" -> new CosmeticOption[] {
                    style("soul-rift", "Soul Rift", "A dark rift marks objective and combat abilities."),
                    style("wither-pulse", "Wither Pulse", "A smoky pulse follows every team ability."),
                    style("siege-ember", "Siege Ember", "Soul embers trace attacks and objective damage.")};
            case "skywars" -> new CosmeticOption[] {
                    style("tnt-burst", "TNT Burst", "A bright blast signature follows TNT and combat actions."),
                    style("skyfall", "Skyfall", "A wind ring traces throws and movement abilities."),
                    style("island-collapse", "Island Collapse", "Block-shatter lines style hits and island events.")};
            case "turf-wars" -> new CosmeticOption[] {
                    style("team-sigil", "Team Sigil", "A team-coloured sigil marks shots and abilities."),
                    style("turf-crack", "Turf Crack", "A short crack traces hits, shots and movement."),
                    style("painted-chevron", "Painted Chevron", "A bold chevron carries the team palette through actions.")};
            case "sneaky-assassins" -> new CosmeticOption[] {
                    style("smoke-artist", "Smoke Artist", "A compact smoke flourish styles disguise abilities."),
                    style("decoy", "Decoy", "A brief silhouette echo marks movement and tricks."),
                    style("understudy", "Understudy", "A villager-like shimmer follows disguise actions.")};
            case "camera-shy" -> new CosmeticOption[] {
                    style("flash-frame", "Flash Frame", "A camera-frame flash styles photos and exposure effects."),
                    style("smoke-artist", "Smoke Artist", "Soft smoke traces escapes and camera tricks."),
                    style("spotter", "Spotter", "A focused lens ring marks photos and detection abilities.")};
            case "plexball" -> new CosmeticOption[] {
                    style("pitch-flare", "Pitch Flare", "A short trail styles throws, hits, catches and blocks."),
                    style("curve-trail", "Curve Trail", "A curved stadium ribbon follows ball actions."),
                    style("home-run-impact", "Home Run Impact", "A clean impact burst marks every batting ability.")};
            case "super-sploor" -> new CosmeticOption[] {
                    style("ink-current", "Ink Current", "A flowing ink signature styles Squid and kit abilities."),
                    style("prism-splat", "Prism Splat", "A bright team-coloured splat follows every action."),
                    style("neon-squid", "Neon Squid", "A luminous squid-shaped current traces abilities.")};
            case "speed-builders" -> new CosmeticOption[] {
                    style("blueprint-pulse", "Blueprint Pulse", "Blueprint lines mark block placement and accuracy."),
                    style("perfect-match", "Perfect Match", "A bright match spark confirms accurate build actions."),
                    style("speed-trace", "Speed Trace", "Fast construction leaves a short quartz-like trail.")};
            case "monster-maze", "monster-maze-speed", "monster-maze-modern" -> new CosmeticOption[] {
                    style("maze-pulse", "Maze Pulse", "A shared maze glyph styles boosts, hazards and stage events."),
                    style("soul-grid", "Soul Grid", "A soul-lit grid marks the same abilities in every mode."),
                    style("glow-path", "Glow Path", "A luminous path traces movement and objective actions.")};
            default -> new CosmeticOption[] {
                    style("classic-action", "Classic Action", "A compact visual signature for this game's actions."),
                    style("arcade-pulse", "Arcade Pulse", "A bright pulse styles abilities and hit feedback."),
                    style("prism-action", "Prism Action", "A prismatic flourish carries across game actions.")};
        };
    }

    private static CosmeticOption style(String id, String name, String description) {
        return single(id, name, description);
    }

    private static GameDefinition game(String id, String name, String icon, String description,
                                       GameDefinition.PortStatus status, List<CosmeticLineDefinition> lines,
                                       CosmeticOption... wins) {
        if (wins.length == 0) throw new IllegalArgumentException("Every game needs a win-effect choice: " + id);
        return new GameDefinition(id, name, description, icon, status, lines, List.of(wins), wins[0].getId());
    }

    private static CosmeticLineDefinition line(String id, String title, String description, String icon,
                                               CosmeticOption... options) {
        return new CosmeticLineDefinition(id, title, description, icon,
                CosmeticLineDefinition.SelectionMode.SINGLE, List.of(options), options[0].getId());
    }

    private static CosmeticLineDefinition lineMulti(String id, String title, String description, String icon,
                                                    CosmeticOption... options) {
        return new CosmeticLineDefinition(id, title, description, icon,
                CosmeticLineDefinition.SelectionMode.MULTI, List.of(options), options[0].getId());
    }

    private static CosmeticOption single(String id, String name, String description) {
        return new CosmeticOption(id, name, description);
    }

    private static CosmeticOption grouped(String id, String name, String description, String selectionGroup) {
        return new CosmeticOption(id, name, description, selectionGroup);
    }

    private static CosmeticLineDefinition monsterMazeMobSetLine() {
        String[] mobSets = {"map_default", "creeper", "skeleton", "spider", "zombie", "slime", "witch",
                "enderman", "guardian", "cave_spider", "silverfish", "blaze", "magma_cube", "bat",
                "endermite", "pig", "sheep", "cow", "chicken", "squid", "wolf", "mooshroom", "snowman",
                "ocelot", "horse", "rabbit", "villager", "axolotl", "allay", "frog", "goat", "fox", "panda",
                "parrot", "turtle", "bee", "sniffer", "armadillo", "random"};
        String[] dyeColours = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        List<CosmeticOption> options = new ArrayList<>();
        for (String mob : mobSets) {
            String display = mobDisplayName(mob);
            String description = switch (mob) {
                case "map_default" -> "Use the selected map's default mob and objective colours.";
                case "random" -> "Use a stable random mob form for each player and maze entity.";
                case "axolotl", "allay", "frog", "goat", "fox", "panda", "parrot", "turtle", "bee",
                        "sniffer", "armadillo" -> "Modern mob form under two blocks tall and long.";
                default -> display + " form.";
            };
            options.add(single(mob, display, description));
            for (String part : List.of("beacon", "pad")) {
                String group = part + "-" + mob;
                String label = "map_default".equals(mob) ? "Map Default" : display;
                options.add(grouped(part + "-" + mob + "-inherit", label + " " + part + ": Mob colour",
                        "Use the colour associated with this mob set.", group));
                for (String colour : dyeColours) {
                    options.add(grouped(part + "-" + mob + "-" + colour,
                            label + " " + part + ": " + mobDisplayName(colour),
                            "Use " + mobDisplayName(colour) + " for this mob set's " + part + " colour.", group));
                }
            }
        }
        return new CosmeticLineDefinition("mob-sets", "Monster Maze Mob Sets",
                "Choose mob forms for your in-game cycle slot and set separate beacon-glass and Safe Pad colours for each form.",
                "CREEPER_HEAD", CosmeticLineDefinition.SelectionMode.MULTI, options, "map_default");
    }

    private static String mobDisplayName(String id) {
        StringBuilder result = new StringBuilder();
        for (String part : id.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            if (!part.isEmpty()) result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static CosmeticOption win(String id, String name, String description) {
        return new CosmeticOption(id, name, description);
    }

    public List<GameDefinition> getGames() { return List.copyOf(games.values()); }
    public GameDefinition getGame(String id) { return id == null ? null : games.get(id.toLowerCase(Locale.ROOT)); }
    public int size() { return games.size(); }
}
