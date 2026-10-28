package au.com.testplex.modern.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modern catalogue for all 16 Arcade games. Each game owns its own cosmetic
 * lines and win-scene choices; actual render/gameplay handlers are added with
 * each port rather than pretending global cosmetics are game-specific.
 */
public final class ArcadeCatalog {
    private final Map<String, GameDefinition> games = new LinkedHashMap<>();

    public ArcadeCatalog() {
        register(game("bacon-brawl", "Bacon Brawl", "PORKCHOP", "Torpedo into rivals and launch them from the arena.",
                GameDefinition.PortStatus.PLANNED,
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

        register(game("bomb-lobbers", "Bomb Lobbers", "TNT", "Land direct TNT hits while surviving the Turtles arena.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("tnt-impact", "TNT Impact Signature", "Direct-hit presentation, not blast damage.", "TNT",
                        single("turtle-shell", "Turtle Shell", "A spinning shell-shaped ring."),
                        single("starburst-tnt", "Starburst TNT", "A radial star-shaped detonation."),
                        single("pixel-crater", "Pixel Crater", "A square shockwave with ember sparks."))),
                win("turtles-dock", "Turtles Dock", "The winner is raised above a miniature turtle dock."),
                win("shell-parade", "Shell Parade", "Spinning shells orbit the winner."),
                win("perfect-throw", "Perfect Throw", "A target reticle locks in before a clean TNT burst")));

        register(game("death-tag", "Death Tag", "SKELETON_SKULL", "Outrun the undead through the village-tree course.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("runner-trail", "Runner Trail Identity", "A recognisable trail silhouette, not just a particle recolour.", "SOUL_LANTERN",
                        single("bark-footprints", "Bark Footprints", "Leaf and bark footprints fade behind the runner."),
                        single("soul-ribbons", "Soul Ribbons", "Two curved soul ribbons trail behind."),
                        single("lantern-motes", "Lantern Motes", "Warm lights rise from each step."))),
                win("barbarians-canopy", "Barbarians Canopy", "The winner appears on a tree platform above the village."),
                win("last-survivor", "Last Survivor", "The arena dims as lanterns light one by one."),
                win("untouched-run", "Untouched Run", "A route map draws itself around the winner")));

        register(game("dragon-escape", "Dragon Escape", "DRAGON_HEAD", "Race the dragon through a collapsing parkour route.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("leader-marker", "Course Leader Marker", "A course-specific marker for the current leader.", "BEACON",
                        single("dragon-eye", "Dragon Eye", "An iris-like marker pulses above the leader."),
                        single("sky-compass", "Sky Compass", "A rotating compass motif follows the leader."),
                        single("wing-flare", "Wing Flare", "Wing-shaped particles flash at checkpoints.")),
                     line("finish-celebration", "Finish Celebration", "A separate finish-line moment.", "FIREWORK_ROCKET",
                        single("skylands-gate", "Skylands Gate", "A luminous gate opens behind the finisher."),
                        single("dragon-roar", "Dragon Roar", "A dragon silhouette sweeps over the finish."),
                        single("cloud-surf", "Cloud Surf", "Cloud rings cascade down the finish platform."))),
                win("skylands-gate", "Skylands Gate", "A large floating gate frames the winner."),
                win("dragon-perch", "Dragon Perch", "The winner stands on a stylised dragon perch."),
                win("cloud-throne", "Cloud Throne", "A cloud-built throne rises under the winner")));

        register(game("dragons", "Dragons", "BLAZE_POWDER", "Use Pyro platforms to survive dragon attacks.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("pyro-signature", "Pyro Signature", "Unique shape language for the Pyro ability.", "BLAZE_POWDER",
                        single("ember-wing", "Ember Wing", "A fan of flame-shaped particles."),
                        single("phoenix-arc", "Phoenix Arc", "A phoenix outline arcs through the air."),
                        single("molten-orbit", "Molten Orbit", "Molten sparks orbit the launch point."))),
                win("dragon-ring", "Dragon Ring", "Dragons circle the winner."),
                win("phoenix-rise", "Phoenix Rise", "The winner rises through a phoenix-shaped flame arc."),
                win("last-platform", "Last Platform", "The remaining platform is spotlighted as the arena fades")));

        register(game("evolution", "Evolution", "DRAGON_EGG", "Fly upward as a chicken and evolve at the summit.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("metamorphosis", "Metamorphosis", "A layered evolution sequence, not an egg recolour.", "DRAGON_EGG",
                        single("shell-fragments", "Shell Fragments", "Egg-shell fragments burst outward."),
                        single("nature-bloom", "Nature Bloom", "Leaves and pollen unfold from the player."),
                        single("void-unfold", "Void Unfold", "A dark geometric shell folds open."))),
                win("summit-nest", "Summit Nest", "The winner is framed by a nest at the summit."),
                win("evolution-tree", "Evolution Tree", "A branching evolutionary tree grows behind the winner."),
                win("gene-helix", "Gene Helix", "A double helix spirals into the sky")));

        register(game("gladiators", "Gladiators", "IRON_SWORD", "Fight for the last position in the duel arena.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("duel-intro", "Duel Entrance", "A distinct combat entrance before the first hit.", "IRON_SWORD",
                        single("arena-challenger", "Arena Challenger", "An arena bell and crossed-sword flash."),
                        single("red-sand", "Red Sand", "A compact sand burst under the fighter."),
                        single("oath-mark", "Oath Mark", "An emblem stamps briefly at the player's feet."))),
                win("champions-podium", "Champion's Podium", "The champion is raised on a stone podium."),
                win("crossed-steel", "Crossed Steel", "Two swords frame the winner."),
                win("arena-banners", "Arena Banners", "Banners unfurl around the final combatant")));

        register(game("micro-battles", "Micro Battles", "LIGHT_BLUE_STAINED_GLASS", "Fight on tiny platforms as the border closes.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("platform-identity", "Platform Identity", "A starting platform material/pattern per player.", "GLASS",
                        single("cracked-ice", "Cracked Ice", "Frosted tiles with an icy edge."),
                        single("prism-grid", "Prism Grid", "A two-tone geometric grid."),
                        single("molten-seam", "Molten Seam", "Dark glass with glowing seams.")),
                     line("border-language", "Border Language", "A visual cue for a closing map border.", "END_ROD",
                        single("sweeping-tape", "Sweeping Tape", "A moving ribbon tracks the safe edge."),
                        single("warning-pips", "Warning Pips", "Warning ticks appear on the closing side."),
                        single("shatter-line", "Shatter Line", "The border emits a glass-shatter silhouette."))),
                win("last-tile", "Last Tile", "Only the winner's final tile stays lit."),
                win("micro-monument", "Micro Monument", "A tiny monument assembles around the winner."),
                win("falling-stars", "Falling Stars", "A narrow star shower descends onto the platform")));

        register(game("monster-maze", "Monster Maze", "ROTTEN_FLESH", "Reach the active Safe Pad while mobs pursue each stage.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS,
                List.of(lineMulti("mob-sets", "Mob Forms", "Enable multiple mob forms independently for the round.", "CREEPER_HEAD",
                        single("creeper", "Creeper", "Creeper form"),
                        single("skeleton", "Skeleton", "Skeleton form"),
                        single("spider", "Spider", "Spider form"),
                        single("zombie", "Zombie", "Zombie form"),
                        single("witch", "Witch", "Witch form"),
                        single("slime", "Slime", "Slime form"),
                        single("enderman", "Enderman", "Enderman form"),
                        single("guardian", "Guardian", "Guardian form")),
                     line("objective-palette", "Objective Palette per Mob Set", "Keep beacon-glass colour and Safe Pad surface colour independent for every mob set.", "STAINED_GLASS",
                        single("per-mob-set", "Configure Each Mob Set", "Select a mob set, then edit beacon and pad colours independently."))),
                win("maze-blueprint", "Maze Blueprint", "A miniature maze trace forms above the winner."),
                win("mob-procession", "Mob Procession", "The round's selected mob forms parade behind the winner."),
                win("safe-pad-coronation", "Safe Pad Coronation", "The Safe Pad raises into a compact victory dais")));

        register(game("one-in-the-quiver", "One in the Quiver", "ARROW", "Land the decisive bow shot with one arrow at a time.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("arrow-flight", "Arrow Flight Signature", "A distinct arrow-flight trail and hit confirm.", "SPECTRAL_ARROW",
                        single("bullseye-dash", "Bullseye Dash", "A short target line follows the arrow."),
                        single("fletching-sparks", "Fletching Sparks", "Feather-shaped sparks mark the trajectory."),
                        single("ricochet-rune", "Ricochet Rune", "A rune marks the impact point."))),
                win("bullseye-stage", "Bullseye Stage", "A target board grows behind the winner."),
                win("quiverless", "Quiverless", "A single arrow hangs above the last archer."),
                win("archer-halo", "Archer Halo", "An archer's halo draws around the winner")));

        register(game("paintball", "Paintball", "SNOWBALL", "Control lanes with paintball weapons and team colours.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("weapon-personality", "Weapon Personality", "Per-weapon identity; projectile mechanics remain unchanged.", "CROSSBOW",
                        single("chrome-marker", "Chrome Marker", "A reflective chrome finish."),
                        single("splatter-cannon", "Splatter Cannon", "A chunky paint cannon silhouette."),
                        single("neon-suppressor", "Neon Suppressor", "A bright, angular weapon finish.")),
                     line("paint-impact", "Paint Impact Signature", "A unique splat silhouette for impacts.", "SLIME_BALL",
                        single("colourburst", "Colourburst", "A radial paint bloom."),
                        single("paint-drip", "Paint Drip", "Paint droplets slide down the hit point."),
                        single("stencil-stamp", "Stencil Stamp", "A team-coloured stencil flashes briefly."))),
                win("paint-wall", "Paint Wall", "A team-coloured mural paints in behind the winner."),
                win("marker-medal", "Marker Medal", "A medal assembles from paint-marker parts."),
                win("splatter-crown", "Splatter Crown", "A crown of layered paint splashes")));

        register(game("runner", "Runner", "LEATHER_BOOTS", "Survive by running the course without falling into gaps.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("route-mark", "Route Mark", "A recognisable path drawn along the winner's route.", "GLOW_INK_SAC",
                        single("red-walk", "Red Walk", "A clean red W-shaped route."),
                        single("chalk-dashes", "Chalk Dashes", "A chalk-like dashed route."),
                        single("ribbon-replay", "Ribbon Replay", "The winner's route redraws as a ribbon."))),
                win("route-replay", "Route Replay", "The final route replays as a floating ribbon."),
                win("finish-tape", "Finish Tape", "A finish-line tape snaps across the arena."),
                win("runner-statue", "Runner Statue", "A running silhouette assembles into a statue")));

        register(game("sheep-quest", "Sheep Quest", "WHITE_WOOL", "Capture sheep and return them to your team's pen.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("capture-signature", "Capture Signature", "Distinct feedback when a sheep is secured.", "LEAD",
                        single("sheep-bell", "Sheep Bell", "A distinct bell and wool puff."),
                        single("wool-confetti", "Wool Confetti", "Colour-coded wool flecks burst outward."),
                        single("shepherd-flare", "Shepherd Flare", "A shepherd's-crook motif appears."))),
                win("pen-parade", "Pen Parade", "The captured flock parades around the winning pen."),
                win("golden-fleece", "Golden Fleece", "A golden fleece appears on a wool pedestal."),
                win("shepherd-crown", "Shepherd Crown", "A leafy crown forms above the winner")));

        register(game("snake", "Snake", "SLIME_BALL", "Collect food, avoid collisions, and use the Mario-Kart-style item roll.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS,
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
                GameDefinition.PortStatus.PLANNED,
                List.of(line("break-signature", "Block Break Signature", "A game-specific shape for each block break.", "IRON_SHOVEL",
                        single("snowburst", "Snowburst", "A short snow burst at the broken tile."),
                        single("ice-shards", "Ice Shards", "Crystalline fragments spread outward."),
                        single("chalk-grid", "Chalk Grid", "The broken tile briefly outlines a grid."))),
                win("ice-throne", "Ice Throne", "A seat of ice rises from the surviving floor."),
                win("last-snowflake", "Last Snowflake", "A single snowflake floats above the winner."),
                win("shovel-salute", "Shovel Salute", "A ceremonial row of shovels rises from the floor")));

        register(game("turf-wars", "Turf Wars", "BOW", "Win ranged combat and control the central turf.",
                GameDefinition.PortStatus.PLANNED,
                List.of(line("arrow-mark", "Arrow Mark", "A block-impact mark that follows the player's team palette.", "TARGET",
                        single("team-sigil", "Team Sigil", "A team sigil appears at the impact point."),
                        single("turf-crack", "Turf Crack", "A short crack traces from the hit."),
                        single("painted-chevron", "Painted Chevron", "A bold directional chevron is stamped on the wall."))),
                win("turf-monument", "Turf Monument", "A team monument rises out of captured turf."),
                win("stadium-finale", "Stadium Finale", "Stadium lights sweep towards the winning team."),
                win("banner-wave", "Banner Wave", "A moving wall of team banners celebrates the winner")));
    }

    private void register(GameDefinition definition) {
        if (games.putIfAbsent(definition.getId(), definition) != null) {
            throw new IllegalArgumentException("Duplicate Arcade game id: " + definition.getId());
        }
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

    private static CosmeticOption win(String id, String name, String description) {
        return new CosmeticOption(id, name, description);
    }

    public List<GameDefinition> getGames() { return List.copyOf(games.values()); }
    public GameDefinition getGame(String id) { return id == null ? null : games.get(id.toLowerCase(Locale.ROOT)); }
    public int size() { return games.size(); }
}
