package au.com.testplex.modern.player;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.GameDefinition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerPreferenceStoreTest {
    private CosmeticLineDefinition paintballLine() {
        GameDefinition game = new ArcadeCatalog().getGame("paintball");
        return game.getCosmeticLines().get(0);
    }

    @Test
    void selectingWeaponFinishKeepsPelletChoiceEquipped() {
        CosmeticLineDefinition line = paintballLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("chrome-marker", "paint-drip"), "neon-suppressor");

        assertEquals(Set.of("neon-suppressor", "paint-drip"), selected);
    }

    @Test
    void selectingPelletStyleKeepsWeaponFinishEquipped() {
        CosmeticLineDefinition line = paintballLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("splatter-cannon", "colourburst"), "stencil-stamp");

        assertEquals(Set.of("splatter-cannon", "stencil-stamp"), selected);
    }

    @Test
    void selectingWithinTheSameGroupReplacesOnlyThatGroupsPreviousChoice() {
        CosmeticLineDefinition line = paintballLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("splatter-cannon", "paint-drip"), "chrome-marker");

        assertEquals(Set.of("chrome-marker", "paint-drip"), selected);
    }
    private CosmeticLineDefinition microBattlesLine() {
        GameDefinition game = new ArcadeCatalog().getGame("micro-battles");
        return game.getCosmeticLines().get(0);
    }

    @Test
    void selectingPrivateMicroGlassKeepsSharedBorderColourEquipped() {
        CosmeticLineDefinition line = microBattlesLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("glass-light-blue", "border-light-blue"), "glass-purple");

        assertEquals(Set.of("glass-purple", "border-light-blue"), selected);
    }

    @Test
    void selectingSharedMicroBorderColourKeepsPrivateGlassEquipped() {
        CosmeticLineDefinition line = microBattlesLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("glass-orange", "border-light-blue"), "border-cyan");

        assertEquals(Set.of("glass-orange", "border-cyan"), selected);
    }

    @Test
    void selectingMicroColourWithinOneGroupReplacesOnlyThatGroupsPreviousChoice() {
        CosmeticLineDefinition line = microBattlesLine();
        Set<String> selected = PlayerPreferenceStore.selectionAfterChoice(
                line, Set.of("glass-orange", "border-cyan"), "border-red");

        assertEquals(Set.of("glass-orange", "border-red"), selected);
    }

}
