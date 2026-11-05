package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

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
}
