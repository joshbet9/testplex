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

    private GameDebugEventCatalog() { }

    static List<Event> events(ArcadePortGame game) {
        if (game == ArcadePortGame.WITHER_ASSAULT) return WITHER_ASSAULT;
        if (game == ArcadePortGame.DRAW_MY_THING) return DRAW_MY_THING;
        return List.of();
    }
}
