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
    @Test void microBattlesBorderWaitsThirtySecondsThenShrinksEveryTwentySeconds() {
        assertFalse(ArcadePortRules.shouldShrinkMicroBattlesBorder(599));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(600));
        assertFalse(ArcadePortRules.shouldShrinkMicroBattlesBorder(999));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(1000));
        assertTrue(ArcadePortRules.shouldShrinkMicroBattlesBorder(1400));
    }

    @Test void microBattlesBorderStopsAtItsMinimumSize() {
        assertEquals(36.0D, ArcadePortRules.nextMicroBattlesBorderSize(42.0D));
        assertEquals(10.0D, ArcadePortRules.nextMicroBattlesBorderSize(10.0D));
        assertEquals(10.0D, ArcadePortRules.nextMicroBattlesBorderSize(8.0D));
    }

    @Test void worldDragonHazardIsNotConfusedWithPlayerFireball() {
        assertTrue(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, true, true));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.DRAGONS, false, false));
        assertFalse(ArcadePortRules.isWorldDragonHazard(ArcadePortGame.BOMB_LOBBERS, true, false));
    }

}
