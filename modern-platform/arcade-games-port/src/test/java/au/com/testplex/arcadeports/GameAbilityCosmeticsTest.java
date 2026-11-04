package au.com.testplex.arcadeports;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.CosmeticOption;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameAbilityCosmeticsTest {
    @Test
    void everyArcadeGameHasOneCohesiveAbilityLineWithDistinctRenderableStyles() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (ArcadePortGame game : ArcadePortGame.values()) {
            String catalogId = game.isMonsterMaze() ? "monster-maze" : game.id();
            var definition = catalog.getGame(catalogId);
            assertNotNull(definition, "Missing cosmetic game registration for " + game.id());
            assertEquals(1, definition.getCosmeticLines().size(),
                    "Each game must expose one ability style line; win effects stay separate: " + game.id());
            CosmeticLineDefinition line = definition.getCosmeticLines().get(0);
            assertEquals("ability-effects", line.getId(), "Unexpected ability style line for " + game.id());
            assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, line.getSelectionMode());

            Set<GameAbilityCosmetics.Motif> motifs = new HashSet<>();
            Set<Object> colours = new HashSet<>();
            for (CosmeticOption option : line.getOptions()) {
                GameAbilityCosmetics.Palette palette = GameAbilityCosmetics.palette(option.getId());
                assertNotNull(palette, "Missing render palette for " + game.id() + "/" + option.getId());
                assertNotNull(palette.motif(), "Missing effect geometry for " + game.id() + "/" + option.getId());
                motifs.add(palette.motif());
                colours.add(palette.colour());
            }
            assertTrue(line.getOptions().size() >= 2, "Game has no style choices: " + game.id());
            assertTrue(motifs.size() >= 2, "Styles have identical particle geometry for " + game.id()
                    + ": " + motifs);
            assertTrue(colours.size() >= 2, "Styles have identical palette colours for " + game.id()
                    + ": " + colours);

            String fallback = GameAbilityCosmetics.defaultOption(catalogId);
            assertNotEquals("arcade-pulse", fallback,
                    "Missing ability-effect default for " + game.id());
        }
    }

    @Test
    void namedFallbackPalettesMatchTheirLabels() {
        assertEquals(org.bukkit.Color.fromRGB(91, 208, 255),
                GameAbilityCosmetics.palette("blue-flame").colour());
        assertEquals(org.bukkit.Color.fromRGB(246, 63, 75),
                GameAbilityCosmetics.palette("trail-red").colour());
        assertEquals(org.bukkit.Color.fromRGB(255, 190, 79),
                GameAbilityCosmetics.palette("lantern-motes").colour());
        assertEquals(org.bukkit.Color.fromRGB(235, 241, 247),
                GameAbilityCosmetics.palette("bone-dust").colour());
    }
}
