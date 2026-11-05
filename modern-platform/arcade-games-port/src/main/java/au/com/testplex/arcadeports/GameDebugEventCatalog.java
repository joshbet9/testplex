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
    private static final List<Event> ONE_IN_THE_QUIVER = List.of(
            new Event("refill-arrow", "Restore Super Arrow", Material.ARROW,
                    "Restore the normal one-shot arrow to its configured hotbar slot for testing."));
    private static final List<Event> PAINTBALL = List.of(
            new Event("restore-armour", "Restore Paintball Armour", Material.LEATHER_CHESTPLATE,
                    "Restore your normal four armour pieces using the selected team's colours."));
    private static final List<Event> DEATH_TAG = List.of(
            new Event("promote-self", "Promote Self to Chaser", Material.SKELETON_SKULL,
                    "Run the normal Runner-to-Chaser conversion on yourself."));
    private static final List<Event> REFRESH_KIT = List.of(
            new Event("refresh-kit", "Refresh Selected Kit", Material.NETHER_STAR,
                    "Reapply your selected kit through its normal in-game kit setup."));
    private static final Set<ArcadePortGame> KIT_REFRESH_GAMES = Set.of(
            ArcadePortGame.BACON_BRAWL, ArcadePortGame.BOMB_LOBBERS, ArcadePortGame.CASTLE_ASSAULT,
            ArcadePortGame.DRAGON_ESCAPE, ArcadePortGame.DRAGONS, ArcadePortGame.EVOLUTION,
            ArcadePortGame.GLADIATORS, ArcadePortGame.HEROES_OF_GWEN, ArcadePortGame.MICRO_BATTLES,
            ArcadePortGame.ONE_IN_THE_QUIVER, ArcadePortGame.PAINTBALL, ArcadePortGame.RUNNER,
            ArcadePortGame.SHEEP_QUEST, ArcadePortGame.SPLEEF, ArcadePortGame.SKY_WARS,
            ArcadePortGame.TURF_WARS, ArcadePortGame.WITHER_ASSAULT);

    private GameDebugEventCatalog() { }

    static List<Event> events(ArcadePortGame game) {
        if (game == ArcadePortGame.WITHER_ASSAULT) return WITHER_ASSAULT;
        if (game == ArcadePortGame.DRAW_MY_THING) return DRAW_MY_THING;
        if (game == ArcadePortGame.DRAGONS) return DRAGONS;
        if (game == ArcadePortGame.ONE_IN_THE_QUIVER) return ONE_IN_THE_QUIVER;
        if (game == ArcadePortGame.PAINTBALL) return PAINTBALL;
        if (game == ArcadePortGame.DEATH_TAG) return DEATH_TAG;
        if (supportsKitRefresh(game)) return REFRESH_KIT;
        return List.of();
    }

    static boolean supportsKitRefresh(ArcadePortGame game) {
        return KIT_REFRESH_GAMES.contains(game);
    }
}
