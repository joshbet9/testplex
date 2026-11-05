package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.List;

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

    private GameDebugEventCatalog() { }

    static List<Event> events(ArcadePortGame game) {
        if (game == ArcadePortGame.WITHER_ASSAULT) return WITHER_ASSAULT;
        if (game == ArcadePortGame.DRAW_MY_THING) return DRAW_MY_THING;
        if (game == ArcadePortGame.DRAGONS) return DRAGONS;
        if (game == ArcadePortGame.ONE_IN_THE_QUIVER) return ONE_IN_THE_QUIVER;
        if (game == ArcadePortGame.PAINTBALL) return PAINTBALL;
        if (game == ArcadePortGame.DEATH_TAG) return DEATH_TAG;
        return List.of();
    }
}
