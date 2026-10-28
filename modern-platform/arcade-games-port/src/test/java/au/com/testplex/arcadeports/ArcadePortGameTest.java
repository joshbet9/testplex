package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadePortGameTest {
    @Test void exposesTheFourteenGamesNotCoveredByDedicatedMechanicsModules() {
        assertEquals(14, ArcadePortGame.values().length);
        Set<String> ids = new HashSet<>();
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertTrue(ids.add(game.id()), "duplicate game id: " + game.id());
            assertNotNull(ArcadePortGame.fromId(game.id()));
            assertNotNull(game.floor());
        }
        assertNull(ArcadePortGame.fromId("monster-maze"));
        assertNull(ArcadePortGame.fromId("snake"));
    }

    @Test void normalizesSpacesAndUnderscoresInTestCommands() {
        assertEquals(ArcadePortGame.ONE_IN_THE_QUIVER, ArcadePortGame.fromId("one_in_the_quiver"));
        assertEquals(ArcadePortGame.BOMB_LOBBERS, ArcadePortGame.fromId("Bomb Lobbers"));
        assertEquals(ArcadePortGame.MICRO_BATTLES, ArcadePortGame.fromId("micro-battles"));
    }
}
