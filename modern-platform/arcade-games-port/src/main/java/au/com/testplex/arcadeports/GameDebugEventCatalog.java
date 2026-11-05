package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/** Explicit game-scoped events that authorised testers can invoke through normal runtime paths. */
final class GameDebugEventCatalog {
    record Event(String id, String title, Material icon, String description) { }

    private static final List<Event> WITHER_ASSAULT = List.of(
            new Event("spawn-minions", "Spawn Minion Wave", Material.WITHER_SKELETON_SKULL,
                    "Run the active Wither's normal minion ability and cooldown."));
    private static final List<Event> DRAW_MY_THING = List.of(
            new Event("reveal-letter", "Reveal Next Hint Letter", Material.WRITABLE_BOOK,
                    "Reveal one additional answer letter to the guessers through the normal hint display."));
    private static final List<Event> DRAGONS = List.of(
            new Event("spawn-dragon", "Spawn Next Dragon Now", Material.DRAGON_EGG,
                    "Run the normal map spawn and target initialization without waiting for the interval."));
    private static final List<Event> DRAGON_ESCAPE = List.of(
            new Event("advance-dragon", "Advance Douglas Once", Material.DRAGON_HEAD,
                    "Run Douglas's normal chase, block clearing and catch checks once now."),
            new Event("use-kit-ability", "Test Selected Kit Ability", Material.NETHER_STAR,
                    "Run your selected kit's normal ability and cooldown checks immediately."),
            new Event("finish-run", "Complete the Course", Material.DRAGON_EGG,
                    "Complete your run through the normal finish and round lifecycle without recording stats."));
    private static final List<Event> SNAKE = List.of(
            new Event("roll-item", "Test Item Roulette", Material.MAGMA_CREAM,
                    "Start your normal random control-debuff roll now; only works for a living player."));
    private static final List<Event> SNEAKY_ASSASSINS = List.of(
            new Event("use-kit-ability", "Test Selected Kit Ability", Material.SMOKE_BOMB,
                    "Run Escape Artist's smoke cloak or Revealer's normal diamond projectile effect."));
    private static final List<Event> CAMERA_SHY = List.of(
            new Event("take-photo", "Test Camera Flash", Material.SPYGLASS,
                    "Run the normal F-key flash and photo checks now, including a visible flash on a missed photo."),
            new Event("use-kit-ability", "Test Selected Kit Ability", Material.NETHER_STAR,
                    "Run your selected kit's normal ability and cooldown checks immediately."));
    private static final List<Event> EVOLUTION = List.of(
            new Event("begin-evolution", "Start Evolution", Material.DRAGON_EGG,
                    "Begin your normal evolution animation immediately to test form and cosmetic effects."));
    private static final List<Event> HOOPS_REMASTERED = List.of(
            new Event("shoot-ball", "Take a Shot", Material.MAGMA_CREAM,
                    "If you currently have possession, release a normal shot using your current aim and position."));
    private static final List<Event> MASTER_BUILDERS = List.of(
            new Event("reset-own-plot", "Clear Own Plot", Material.TNT,
                    "Clear your active plot interior so you can test another build without ending the round."));
    private static final List<Event> ONE_IN_THE_QUIVER = List.of(
            new Event("refill-arrow", "Restore Super Arrow", Material.ARROW,
                    "Restore the normal one-shot arrow to its configured hotbar slot for testing."));
    private static final List<Event> WIZARDS = List.of(
            new Event("grant-spells", "Grant Spell Test Loadout", Material.BLAZE_ROD,
                    "Unlock implemented spells, refill mana and clear spell cooldowns for your next casts."));
    private static final List<Event> PAINTBALL = List.of(
            new Event("restore-armour", "Restore Paintball Armour", Material.LEATHER_CHESTPLATE,
                    "Restore your normal four armour pieces using the selected team's colours."));
    private static final List<Event> DEATH_TAG = List.of(
            new Event("promote-self", "Promote Self to Chaser", Material.SKELETON_SKULL,
                    "Run the normal Runner-to-Chaser conversion on yourself."));
    private static final List<Event> GLADIATORS = List.of(
            new Event("forfeit-duel", "Forfeit Current Duel", Material.WHITE_BANNER,
                    "Resolve your current duel as a normal loss so you can test bracket advancement."),
            REFRESH_KIT.getFirst());
    private static final List<Event> REFRESH_KIT = List.of(
            new Event("refresh-kit", "Refresh Selected Kit", Material.NETHER_STAR,
                    "Reapply your selected kit through its normal in-game kit setup."));
    private static final List<Event> MICRO_BATTLES = List.of(
            new Event("drop-barriers", "Drop Start Barriers", Material.GLASS,
                    "Run the normal start-glass removal now so you can test combat through the opening."),
            REFRESH_KIT.getFirst());
    private static final List<Event> SPLEEF = List.of(
            new Event("restore-hunger", "Restore Hunger", Material.COOKED_BEEF,
                    "Restore your hunger and saturation to full during an active round."),
            REFRESH_KIT.getFirst());
    private static final List<Event> SUPER_SPLOOR = List.of(
            new Event("use-special", "Test Selected Special", Material.FIREWORK_STAR,
                    "Fill and activate your selected kit's normal Special so you can test its full effect."));
    private static final List<Event> MILK_THE_COW = List.of(
            new Event("spawn-herd", "Spawn Cow Herd Now", Material.COW_SPAWN_EGG,
                    "Run the normal herd follow and spawn update beside the active Cow-role tester."));
    private static final List<Event> SHEEP_QUEST = List.of(
            new Event("spawn-sheep", "Spawn Sheep Now", Material.WHITE_WOOL,
                    "Spawn and configure one neutral sheep using the normal round map spawn rules."));
    private static final List<Event> SKY_WARS = List.of(
            new Event("spawn-tnt", "Spawn Generator TNT", Material.TNT,
                    "Run the selected map's normal TNT generator drop immediately."));
    private static final List<Event> BACON_BRAWL = List.of(
            new Event("fill-meter", "Fill Ability Meter", Material.IRON_AXE,
                    "Fill your selected kit's live ability meter so you can test its ability now."));
    private static final List<Event> BOMB_LOBBERS = List.of(
            new Event("spawn-powerups", "Spawn Power-up Wave", Material.FIREWORK_ROCKET,
                    "Run the normal paired team power-up drop wave immediately."));
    private static final List<Event> CASTLE_ASSAULT = List.of(
            new Event("spawn-objective", "Spawn Objective TNT", Material.TNT,
                    "Run Castle Assault's normal objective spawn when no objective is active or carried."));
    private static final List<Event> RUNNER = List.of(
            new Event("use-position-swap", "Test Position Swap", Material.ENDER_EYE,
                    "Use the selected Switcher kit's normal random eligible-player swap and landing protection."));
    private static final List<Event> HEROES_OF_GWEN = List.of(
            new Event("spawn-minion-wave", "Start Minion Wave", Material.ZOMBIE_SPAWN_EGG,
                    "Start both teams' normal minion wave and source-map route processing."));
    private static final Set<ArcadePortGame> KIT_REFRESH_GAMES = Set.of(
            ArcadePortGame.BACON_BRAWL, ArcadePortGame.BOMB_LOBBERS, ArcadePortGame.CASTLE_ASSAULT,
            ArcadePortGame.DRAGON_ESCAPE, ArcadePortGame.DRAGONS, ArcadePortGame.EVOLUTION,
            ArcadePortGame.GLADIATORS, ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.MICRO_BATTLES,
            ArcadePortGame.ONE_IN_THE_QUIVER, ArcadePortGame.PAINTBALL, ArcadePortGame.RUNNER,
            ArcadePortGame.SHEEP_QUEST, ArcadePortGame.SPLEEF, ArcadePortGame.SKY_WARS,
            ArcadePortGame.TURF_WARS, ArcadePortGame.WITHER_ASSAULT);

    private GameDebugEventCatalog() { }

    static List<Event> events(ArcadePortGame game) {
        if (game == ArcadePortGame.BACON_BRAWL) return BACON_BRAWL;
        if (game == ArcadePortGame.BOMB_LOBBERS) return BOMB_LOBBERS;
        if (game == ArcadePortGame.CASTLE_ASSAULT) return CASTLE_ASSAULT;
        if (game == ArcadePortGame.RUNNER) return RUNNER;
        if (game == ArcadePortGame.HEROES_OF_GWEN) return HEROES_OF_GWEN;
        if (game == ArcadePortGame.WITHER_ASSAULT) return WITHER_ASSAULT;
        if (game == ArcadePortGame.DRAW_MY_THING) return DRAW_MY_THING;
        if (game == ArcadePortGame.DRAGONS) return DRAGONS;
        if (game == ArcadePortGame.DRAGON_ESCAPE) return DRAGON_ESCAPE;
        if (game == ArcadePortGame.CAMERA_SHY) return CAMERA_SHY;
        if (game == ArcadePortGame.EVOLUTION) return EVOLUTION;
        if (game == ArcadePortGame.HOOPS_REMASTERED) return HOOPS_REMASTERED;
        if (game == ArcadePortGame.MASTER_BUILDERS) return MASTER_BUILDERS;
        if (game == ArcadePortGame.SNAKE) return SNAKE;
        if (game == ArcadePortGame.SNEAKY_ASSASSINS) return SNEAKY_ASSASSINS;
        if (game == ArcadePortGame.ONE_IN_THE_QUIVER) return ONE_IN_THE_QUIVER;
        if (game == ArcadePortGame.SPLEEF) return SPLEEF;
        if (game == ArcadePortGame.SUPER_SPLOOR) return SUPER_SPLOOR;
        if (game == ArcadePortGame.MILK_THE_COW) return MILK_THE_COW;
        if (game == ArcadePortGame.SHEEP_QUEST) return SHEEP_QUEST;
        if (game == ArcadePortGame.SKY_WARS) return SKY_WARS;
        if (game == ArcadePortGame.PAINTBALL) return PAINTBALL;
        if (game == ArcadePortGame.MICRO_BATTLES) return MICRO_BATTLES;
        if (game == ArcadePortGame.DEATH_TAG) return DEATH_TAG;
        if (game == ArcadePortGame.GLADIATORS) return GLADIATORS;
        if (game == ArcadePortGame.WIZARDS) return WIZARDS;
        if (supportsKitRefresh(game)) return REFRESH_KIT;
        return List.of();
    }

    static boolean supportsKitRefresh(ArcadePortGame game) {
        return KIT_REFRESH_GAMES.contains(game);
    }
}
