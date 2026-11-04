package au.com.testplex.modern.player;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.GameDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerPreferenceStoreTest {
    private CosmeticLineDefinition line(String gameId) {
        GameDefinition game = new ArcadeCatalog().getGame(gameId);
        return game.getCosmeticLines().get(0);
    }

    @Test
    void selectingPaintballPelletStyleReplacesThePreviousStyle() {
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line("paintball"), Set.of("classic-pellet"), "prism-pellet");
        assertEquals(Set.of("prism-pellet"), selected);
    }

    @Test
    void selectingPlexballFinishReplacesThePreviousStyle() {
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line("plexball"), Set.of("carbon"), "solar");
        assertEquals(Set.of("solar"), selected);
    }

    @Test
    void selectingMicroBattlesColourReplacesThePreviousColour() {
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line("micro-battles"), Set.of("light-blue"), "purple");
        assertEquals(Set.of("purple"), selected);
    }

    @Test
    void selectingUnknownCosmeticIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PlayerPreferenceStore.selectionAfterChoice(
                line("paintball"), Set.of("classic-pellet"), "not-a-pellet"));
    }
}
