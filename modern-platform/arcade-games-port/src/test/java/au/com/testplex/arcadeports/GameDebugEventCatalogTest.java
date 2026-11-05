package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameDebugEventCatalogTest {
    @Test
    void eventToolsAreExplicitlyScopedToTheirGame() {
        var witherEvents = GameDebugEventCatalog.events(ArcadePortGame.WITHER_ASSAULT);
        assertEquals(1, witherEvents.size());
        assertEquals("spawn-minions", witherEvents.getFirst().id());
        assertEquals("reveal-letter", GameDebugEventCatalog.events(ArcadePortGame.DRAW_MY_THING).getFirst().id());
        assertEquals("spawn-dragon", GameDebugEventCatalog.events(ArcadePortGame.DRAGONS).getFirst().id());
        assertEquals("refill-arrow", GameDebugEventCatalog.events(ArcadePortGame.ONE_IN_THE_QUIVER)
                .getFirst().id());
        assertEquals("restore-armour", GameDebugEventCatalog.events(ArcadePortGame.PAINTBALL)
                .getFirst().id());
        assertEquals("promote-self", GameDebugEventCatalog.events(ArcadePortGame.DEATH_TAG)
                .getFirst().id());
        assertEquals("refresh-kit", GameDebugEventCatalog.events(ArcadePortGame.SPLEEF)
                .getFirst().id());
        assertFalse(GameDebugEventCatalog.events(ArcadePortGame.BACON_BRAWL).stream()
                .anyMatch(event -> event.id().equals("spawn-minions")));
    }

    @Test
    void everyListedEventHasAProductionDispatcher() throws IOException {
        Path managerPath = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports",
                "ArcadePortManager.java");
        if (!Files.isRegularFile(managerPath)) {
            managerPath = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports", "ArcadePortManager.java");
        }
        String source = Files.readString(managerPath);
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (GameDebugEventCatalog.Event event : GameDebugEventCatalog.events(game)) {
                boolean directDispatcher = source.contains("type == ArcadePortGame." + game.name()
                        + " && \"" + event.id() + "\".equals(eventId)");
                boolean sharedKitDispatcher = "refresh-kit".equals(event.id())
                        && GameDebugEventCatalog.supportsKitRefresh(game)
                        && source.contains("GameDebugEventCatalog.supportsKitRefresh(type)"
                                + " && \"refresh-kit\".equals(eventId)");
                assertTrue(directDispatcher || sharedKitDispatcher,
                        game.id() + " event " + event.id() + " has no gameplay dispatcher");
            }
        }
    }

    @Test
    void kitRefreshIsOnlyOfferedForGamesWithSharedKitSetup() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            boolean offered = GameDebugEventCatalog.events(game).stream()
                    .anyMatch(event -> event.id().equals("refresh-kit"));
            assertEquals(GameDebugEventCatalog.supportsKitRefresh(game), offered, game.id());
        }
    }
}
