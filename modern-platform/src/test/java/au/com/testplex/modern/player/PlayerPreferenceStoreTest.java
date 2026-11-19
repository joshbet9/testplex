package au.com.testplex.modern.player;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.GameDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void disablingOneGroupedCosmeticPreservesOtherGroups() {
        assertEquals(Set.of("blue-burst"), PlayerPreferenceStore.selectionAfterDisable(
                Set.of("blue-burst", "prism-pellet"), "prism-pellet"));
    }

    @Test
    void disablingLastSelectionLeavesTheLineUnequipped() {
        assertEquals(Set.of(), PlayerPreferenceStore.selectionAfterDisable(Set.of("classic-pellet"), "classic-pellet"));
        assertEquals(Set.of("classic-pellet"), PlayerPreferenceStore.selectionAfterDisable(
                Set.of("classic-pellet"), "not-selected"));
    }

    @Test
    void enablingAllMonsterMazeMobSetsPreservesPerMobColoursAndIsIdempotent() {
        var game = new ArcadeCatalog().getGame("monster-maze");
        CosmeticLineDefinition mobSets = game.getCosmeticLines().stream()
                .filter(item -> item.getId().equals("mob-sets")).findFirst().orElseThrow();
        List<String> allMobs = mobSets.getOptions().stream()
                .filter(option -> option.getSelectionGroup() == null)
                .map(option -> option.getId()).toList();
        Set<String> original = Set.of("map_default", "pad-creeper-lime", "beacon-zombie-purple");

        Set<String> enabled = PlayerPreferenceStore.selectionAfterAddingOptions(mobSets, original, allMobs);

        assertTrue(enabled.containsAll(allMobs));
        assertTrue(enabled.contains("pad-creeper-lime"));
        assertTrue(enabled.contains("beacon-zombie-purple"));
        assertEquals(enabled,
                PlayerPreferenceStore.selectionAfterAddingOptions(mobSets, enabled, allMobs));
        assertEquals(82, allMobs.size());
    }

    @Test
    void disabledWinEffectDoesNotFallBackToDefault() {
        assertEquals("default", PlayerPreferenceStore.winEffectOrDefault(null, "default"));
        assertEquals("", PlayerPreferenceStore.winEffectOrDefault("", "default"));
        assertEquals("selected", PlayerPreferenceStore.winEffectOrDefault("selected", "default"));
    }
}
