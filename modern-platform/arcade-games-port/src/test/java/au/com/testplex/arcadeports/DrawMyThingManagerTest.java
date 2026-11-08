package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DrawMyThingManagerTest {
    @Test
    void controllerClaimsOnlyDrawMyThing() {
        DrawMyThingManager draw = new DrawMyThingManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(draw);

        assertSame(draw, registry.get(ArcadePortGame.DRAW_MY_THING));
        assertNull(registry.get(ArcadePortGame.MASTER_BUILDERS));
    }

    @Test
    void wordSelectionRecordsUsedWords() {
        DrawMyThingManager draw = new DrawMyThingManager();
        SessionState session = new SessionState(ArcadePortGame.DRAW_MY_THING, null, List.of(), false);

        String selected = draw.chooseWord(session);

        assertTrue(DrawMyThingContent.WORDS.contains(selected));
        assertTrue(session.drawUsedWords.contains(selected));
    }

    @Test
    void hintMaskKeepsSpacesAndOnlyRevealsSelectedLetters() {
        assertEquals("C _ _   D _ _ ", DrawMyThingManager.hintMask("CAT DOG", Set.of(0, 4)));
        assertEquals("_ _ _ ", DrawMyThingManager.hintMask("CAT", Set.of()));
    }
}
