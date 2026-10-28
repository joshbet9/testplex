package au.com.testplex.modern.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Modern game catalogue. Cosmetic options are intentionally distinct per game;
 * actual renderers and win-scene modules are wired as each game is ported.
 */
public final class ArcadeCatalog {
    private final Map<String, GameDefinition> games = new LinkedHashMap<>();

    public ArcadeCatalog() {
        register(game("bacon-brawl", "Bacon Brawl", "PORKCHOP", "Torpedo into rivals and launch them from the arena.",
                GameDefinition.PortStatus.PLANNED,
                line("signature-impact", "Signature Impact", "A game-specific presentation for the Body Slam / Torpedo ability.", "FIREWORK_ROCKET",
                        single("pork-comet", "Pork Comet", "A bacon-shaped comet slams into the impact point."),
                        single("crispy-ring", "Crispy Ring", "A sizzling ring expands from the impact point."),
                        single("sauce-splash", "Sauce Splash", "A red-orange splash bursts outward.")),
                win("lava-arena", "Mighty Lava Arena", "A compact lava-arena podium scene."),
                win("bacon-crown", "Bacon Crown", "A floating bacon crown and victory fanfare."),
                win("crispy-finale", "Crispy Finale", "A sizzling floor burst and smoke columns")));

        register(game("bomb-lobbers", "Bomb Lobbers", "TNT", "Land direct TNT hits while surviving the Turtles arena.",
                GameDefinition.PortStatus.PLANNED,
                line("tnt-impact", "TNT Impact Signature", "Customises the direct-hit presentation, not blast damage.", "TNT",
                        single("turtle-shell", "Turtle Shell", "A spinning shell-shaped ring on direct impact."),
                        single("starburst-tnt", "Starburst TNT", "A radial star-shaped explosion of particles."),
                        single("pixel-crater", "Pixel Crater", "A square shockwave with ember sparks.")),
                win("turtles-dock", "Turtles Dock", "The winner is raised above a miniature turtle dock."),
                win("shell-parade", "Shell Parade", "Spinning shells orbit the winner."),
                win("perfect-throw", "Perfect Throw", "A target reticle locks in, followed by a clean TNT burst")));

        register(game("death-tag", "Death Tag", "SKELETON_SKULL", "Outrun the undead through the village-tree course.",
                GameDefinition.PortStatus.PLANNED,
                line("runner-trail", "Runner Trail Identity", "A recognisable trail silhouette rather than a generic particle recolour.", "SOUL_LANTERN",
                        single("bark-footprints", "Bark Footprints", "Short-lived leaf and bark footprints."),
                        single("soul-ribbons", "Soul Ribbons", "Two curved soul ribbons trail behind the runner."),
                        single("lantern-motes", "Lantern Motes", "Small warm lights rise from each step.")),
                win("barbarians-canopy", "Barbarians Canopy", "The winner appears on a tree platform above the village."),
                win("last-survivor", "Last Survivor", "The arena lights dim and a ring of lanterns lights."),
                win("untouched-run", "Untouched Run", "A route map draws itself around the winner")));

        register(game("dragon-escape", "Dragon Escape", "DRAGON_HEAD", "Race the dragon through a collapsing parkour route.",
                GameDefinition.PortStatus.PLANNED,
                line("leader-marker", "Course Leader Marker", "A course-specific marker used only for the current leader.", "BEACON",
                        single("dragon-eye", "Dragon Eye", "An iris-like marker pulses above the leader."),
                        single("sky-compass", "Sky Compass", "A small rotating compass motif follows the leader."),
                        single("wing-flare", "Wing Flare", "Short wing-shaped particles flash at checkpoints.")),
                line("finish-celebration", "Finish Celebration", "A finish-line moment, separate from the leader marker.", "FIREWORK_ROCKET",
                        single("skylands-gate", "Skylands Gate", "A luminous gate opens behind the finisher."),
                        single("dragon-roar", "Dragon Roar", "A dragon silhouette sweeps across the finish."),
                        single("cloud-surf", "Cloud Surf", "Cloud rings cascade down the finish platform")),
                win("skylands-gate", "Skylands Gate", "A large floating gate frames the winner."),
                win("dragon-perch", "Dragon Perch", "The winner stands on a stylised dragon perch"),
                win("cloud-throne", "Cloud Throne", "A cloud-built throne rises under the winner")));

        register(game("dragons", "Dragons", "BLAZE_POWDER", "Use Pyro platforms to survive dragon attacks.",
                GameDefinition.PortStatus.PLANNED,
                line("pyro-signature", "Pyro Signature", "Unique shape language for the Pyro ability.", "BLAZE_POWDER",
                        single("ember-wing", "Ember Wing", "A fan of flame-shaped particles."),
                        single("phoenix-arc", "Phoenix Arc", "An arcing phoenix outline made of particles."),
                        single("molten-orbit", "Molten Orbit", "Molten sparks orbit the launch point")),
                win("dragon-ring", "Dragon Ring", "Dragons circle the winner in a tight ring."),
                win("phoenix-rise", "Phoenix Rise", "The winner rises through a phoenix-shaped flame arc"),
                win("last-platform", "Last Platform", "The remaining platform is spotlighted as the arena fades")));

        register(game("evolution", "Evolution", "DRAGON_EGG", "Fly upward as a chicken and evolve at the summit.",
                GameDefinition.PortStatus.PLANNED,
                line("evolution-metamorphosis", "Metamorphosis", "A layered evolution sequence, not a generic egg recolour.", "DRAGON_EGG",
                        single("shell-fragments", "Shell Fragments", "Egg-shell fragments burst outward."),
                        single("nature-bloom", "Nature Bloom", "Leaves and pollen unfold from the player."),
                        single("void-unfold", "Void Unfold", "A dark geometric shell folds open")),
                win("summit-nest", "Summit Nest", "The winner is framed by a nest at the summit."),
                win("evolution-tree", "Evolution Tree", "A branching evolutionary tree grows behind the winner"),
                win("gene-helix", "Gene Helix", "A double helix spirals into the sky")));

        register(game("gladiators", "Gladiators", "IRON_SWORD", "Fight for the last position in the duel arena.",
                GameDefinition.PortStatus.PLANNED,
                line("duel-intro", "Duel Entrance", "A distinct combat entrance shown before the first hit.", "IRON_SWORD",
                        single("arena-challenger", "Arena Challenger", "An arena bell and crossed-sword flash."),
                        single("red-sand", "Red Sand", "A compact sand burst under the fighter."),
                        single("oath-mark", "Oath Mark", "An emblem is stamped briefly at the player's feet")),
                win("champions-podium", "Champion's Podium", "The champion is raised on a stone podium."),
                win("crossed-steel", "Crossed Steel", "Two swords frame the winner"),
                win("arena-banners", "Arena Banners", "Banners unfurl around the final combatant")));

        register(game("micro-battles", "Micro Battles", "LIGHT_BLUE_STAINED_GLASS", "Fight on tiny platforms as the border closes.",
                GameDefinition.PortStatus.PLANNED,
                line("platform-identity", "Platform Identity", "A starting platform material/pattern per player.", "GLASS",
                        single("cracked-ice", "Cracked Ice", "Frosted tiles with an icy edge."),
                        single("prism-grid", "Prism Grid", "A two-tone geometric grid."),
                        single("molten-seam", "Molten Seam", "Dark glass with glowing seams")),
                line("border-language", "Border Language", "A compact visual cue for a closing map border.", "END_ROD",
                        single("sweeping-tape", "Sweeping Tape", "A moving ribbon tracks the safe edge."),
                        single("warning-pips", "Warning Pips", "Small warning ticks appear on the closing side"),
                        single("shatter-line", "Shatter Line", "The border emits a glass-shatter silhouette")),
                win("last-tile", "Last Tile", "Only the winner's final tile stays lit"),
                win("micro-monument", "Micro Monument", "A tiny monument assembles around the winner"),
                win("falling-stars", "Falling Stars", "A narrow star shower descends onto the platform")));

        register(game("monster-maze", "Monster Maze", "ROTTEN_FLESH", "Reach the active Safe Pad while mobs pursue each stage.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS,
                new CosmeticLineDefinition("mob-sets", "Mob Forms", "Independently enable supported mob appearances for the round.", "CREEPER_HEAD",
                        CosmeticLineDefinition.SelectionMode.MULTI, List.of(
                                single("creeper", "Creeper", "Creeper form"),
                                single("skeleton", "Skeleton", "Skeleton form"),
                                single("spider", "Spider", "Spider form"),
                                single("zombie", "Zombie", "Zombie form"),
                                single("witch", "Witch", "Witch form"),
                                single("slime", "Slime", "Slime form"),
                                single("enderman", "Enderman", "Enderman form"),
                                single("guardian", "Guardian", "Guardian form")), "creeper"),
                line("objective-palette", "Objective Palette per Mob Set", "Beacon glass and Safe Pad surface are separate settings per mob set; this line is an extensible per-set configuration entry, not one global dye.", "STAINED_GLASS",
                        single("per-mob-set", "Configure Each Mob Set", "Choose a mob set, then edit its beacon and pad colours independently.")),
                win("maze-blueprint", "Maze Blueprint", "A miniature maze trace forms above the winner"),
                win("mob-procession", "Mob Procession", "The round's selected mob forms parade behind the winner"),
                win("safe-pad-coronation", "Safe Pad Coronation", "The Safe Pad raises into a compact victory dais")));

        register(game("one-in-the-quiver", "One in the Quiver", "ARROW", "Land the decisive bow shot with one arrow at a time.",
                GameDefinition.PortStatus.PLANNED,
                line("arrow-flight", "Arrow Flight Signature", "A distinct arrow-flight trail and hit confirm.", "SPECTRAL_ARROW",
                        single("bullseye-dash", "Bullseye Dash", "A short target line follows the arrow."),
                        single("fletching-sparks", "Fletching Sparks", "Feather-shaped sparks mark the trajectory."),
                        single("ricochet-rune", "Ricochet Rune", "A rune briefly marks the point of impact")),
                win("bullseye-stage", "Bullseye Stage", "A target board grows behind the winner"),
                win("quiverless", "Quiverless", "A single arrow hangs above the last archer"),
                win("archer-halo", "Archer Halo", "An archer's halo draws around the winner")));

        register(game("paintball", "Paintball", "SNOWBALL", "Control lanes with paintball weapons and team colours.",
                GameDefinition.PortStatus.PLANNED,
                line("weapon-personality", "Weapon Personality", "Per-weapon visual identity; projectile mechanics stay unchanged.", "CROSSBOW",
                        single("chrome-marker", "Chrome Marker", "A reflective chrome finish."),
                        single("splatter-cannon", "Splatter Cannon", "A chunky paint cannon silhouette."),
                        single("neon-suppressor", "Neon Suppressor", "A bright, angular weapon finish")),
                line("paint-impact", "Paint Impact Signature", "A unique splat silhouette for impacts.", "SLIME_BALL",
                        single("colourburst", "Colourburst", "A radial paint bloom."),
                        single("paint-drip", "Paint Drip", "Paint droplets slide down the hit point"),
                        single("stencil-stamp", "Stencil Stamp", "A team-coloured stencil appears for a moment")),
                win("paint-wall", "Paint Wall", "A huge team-coloured mural paints in behind the winner"),
                win("marker-medal", "Marker Medal", "A medal assembled from paint marker parts"),
                win("splatter-crown", "Splatter Crown", "A crown of layered paint splashes")));

        register(game("runner", "Runner", "LEATHER_BOOTS", "Survive by running the course without falling into gaps.",
                GameDefinition.PortStatus.PLANNED,
                line("route-mark", "Route Mark", "A recognisable path drawn along the winner's route.", "GLOW_INK_SAC",
                        single("red-walk", "Red Walk", "A clean red W-shaped route."),
                        single("chalk-dashes", "Chalk Dashes", "A chalk-like dashed route"),
                        single("ribbon-replay", "Ribbon Replay", "The winner's route redraws as a ribbon")),
                win("route-replay", "Route Replay", "The final route replays as a floating ribbon"),
                win("finish-tape", "Finish Tape", "A finish-line tape snaps across the arena"),
                win("runner-statue", "Runner Statue", "A running silhouette assembles into a statue")));

        register(game("sheep-quest", "Sheep Quest", "WHITE_WOOL", "Capture sheep and return them to your team's pen.",
                GameDefinition.PortStatus.PLANNED,
                line("capture-signature", "Capture Signature", "A clear feedback style when a sheep is secured.", "LEAD",
                        single("sheep-bell", "Sheep Bell", "A distinct bell and wool puff."),
                        single("wool-confetti", "Wool Confetti", "Colour-coded wool flecks burst outward"),
                        single("shepherd-flare", "Shepherd Flare", "A short shepherd's crook motif appears")),
                win("pen-parade", "Pen Parade", "The captured flock parades around the winning pen"),
                win("golden-fleece", "Golden Fleece", "A golden fleece is displayed on a wool pedestal"),
                win("shepherd-crown", "Shepherd Crown", "A leafy crown forms above the winner")));

        register(game("snake", "Snake", "SLIME_BALL", "Collect food, avoid collisions, and use the Mario-Kart-style item roll.",
                GameDefinition.PortStatus.MECHANICS_IN_PROGRESS,
                line("snake-identity", "Snake Identity", "A connected body pattern rather than the same particle trail for every skin.", "SLIME_BALL",
                        single("anaconda", "Anaconda", "Segmented heavy scales along the snake body."),
                        single("cobra", "Cobra", "A raised hood silhouette and alternating scales"),
                        single("neon-viper", "Neon Viper", "A two-tone luminous stripe"),
                        single("reef-serpent", "Reef Serpent", "Coral-like fins along the body")),
                line("item-roll-visual", "Item Roll Visual", "Distinct Mario-Kart-style hotbar roll presentation; effect timing remains gameplay-owned.", "CHEST_MINECART",
                        single("slot-reel", "Slot Reel", "A rapid 3D-looking item strip in the hotbar"),
                        single("dice-lock", "Dice Lock", "An animated dice reveal with a lock-in pulse"),
                        single("roulette-orbit", "Roulette Orbit", "Items orbit briefly before the selected effect is shown")),
                win("race-lap", "Final Lap", "A lap counter and racing pennants appear"),
                win("snake-podium", "Snake Podium", "The winner coils around a compact podium"),
                win("fruit-fountain", "Fruit Fountain", "Food items spiral into a colourful fountain")));

        register(game("spleef", "Spleef", "IRON_SHOVEL", "Break the floor beneath opponents and stay above the void.",
                GameDefinition.PortStatus.PLANNED,
                line("break-signature", "Block Break Signature", "The shape of each block break is game-specific.", "IRON_SHOVEL",
                        single("snowburst", "Snowburst", "A short snow burst at the broken tile"),
                        single("ice-shards", "Ice Shards", "Crystalline fragments spread outwards"),
                        single("chalk-grid", "Chalk Grid", "The broken tile briefly outlines a grid")),
                win("ice-throne", "Ice Throne", "A seat of ice rises from the surviving floor"),
                win("last-snowflake", "Last Snowflake", "A single snowflake floats above the winner"),
                win("shovel-salute", "Shovel Salute", "A ceremonial row of shovels rises from the floor")));

        register(game("turf-wars", "Turf Wars", "BOW", "Win ranged combat and control the central turf.",
                GameDefinition.PortStatus.PLANNED,
                line("arrow-mark", "Arrow Mark", "A block impact mark that uses the player's team palette.", "TARGET",
                        single("team-sigil", "Team Sigil", "A team sigil appears at the impact point"),
                        single("turf-crack", "Turf Crack", "A short crack traces out from the hit"),
                        single("painted-chevron", "Painted Chevron", "A bold directional chevron is stamped on the wall")),
                win("turf-monument", "Turf Monument", "A team monument rises out of the captured turf"),
                win("stadium-finale", "Stadium Finale", "Stadium lights sweep towards the winning team"),
                win("banner-wave", "Banner Wave", "A moving wall of team banners celebrates the winner")));
    }

    private void register(GameDefinition definition) {
        if (games.putIfAbsent(definition.getId(), definition) != null) {
            throw new IllegalArgumentException("Duplicate Arcade game id: " + definition.getId());
        }
    }

    private static GameDefinition game(String id, String name, String icon, String description,
                                       GameDefinition.PortStatus status, CosmeticLineDefinition... linesAndLastWin) {
        List<CosmeticLineDefinition> lines = new ArrayList<>();
        List<CosmeticOption> wins = new ArrayList<>();
        for (CosmeticLineDefinition line : linesAndLastWin) lines.add(line);
        // The win effect collection is intentionally populated by the dedicated overload below.
        throw new IllegalStateException("Use gameWithWins for " + id);
    }

    private static GameDefinition gameWithWins(String id, String name, String icon, String description,
                                               GameDefinition.PortStatus status, List<CosmeticLineDefinition> lines,
                                               CosmeticOption... wins) {
        return new GameDefinition(id, name, description, icon, status, lines, List.of(wins), wins[0].getId());
    }

    private static CosmeticLineDefinition line(String id, String title, String description, String icon,
                                               CosmeticOption... options) {
        return new CosmeticLineDefinition(id, title, description, icon,
                CosmeticLineDefinition.SelectionMode.SINGLE, List.of(options), options[0].getId());
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
