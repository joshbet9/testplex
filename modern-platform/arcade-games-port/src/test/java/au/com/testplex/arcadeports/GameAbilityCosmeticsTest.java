package au.com.testplex.arcadeports;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.CosmeticOption;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameAbilityCosmeticsTest {
    @Test
    void abilityNamesUseStableDistinctMotifsWithinAStyle() {
        GameAbilityCosmetics.Palette style = GameAbilityCosmetics.palette("arcane-script");
        Set<GameAbilityCosmetics.Motif> motifs = new HashSet<>();
        for (String spell : Set.of("Anvil Drop", "Fireball", "Blink", "Ice Shards",
                "Lightning Strike", "Rumble", "Spectral Arrow", "Frost Barrier")) {
            GameAbilityCosmetics.Palette result = GameAbilityCosmetics.abilityPalette(style, spell);
            assertEquals(style.colour(), result.colour(), "Spell identity must keep the equipped palette");
            assertEquals(result.motif(), GameAbilityCosmetics.abilityPalette(style, spell).motif(),
                    "The same spell must keep its visual signature");
            motifs.add(result.motif());
        }
        assertTrue(motifs.size() >= 8, "Wizard spells should have recognizable silhouettes under one style");
    }

    @Test
    void onlyAbilityEffectLinesUseTheSharedParticleRenderer() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        int effectLines = 0;
        for (var game : catalog.getGames()) {
            for (CosmeticLineDefinition line : game.getCosmeticLines()) {
                if (!"ability-effects".equals(line.getId())) continue;
                effectLines++;
                Set<GameAbilityCosmetics.Motif> motifs = new HashSet<>();
                Set<Object> colours = new HashSet<>();
                for (CosmeticOption option : line.getOptions()) {
                    GameAbilityCosmetics.Palette palette = GameAbilityCosmetics.palette(option.getId());
                    assertNotNull(palette, "Missing palette for " + game.getId() + "/" + option.getId());
                    assertNotNull(palette.motif(), "Missing geometry for " + game.getId() + "/" + option.getId());
                    motifs.add(palette.motif());
                    colours.add(palette.colour());
                }
                assertTrue(line.getOptions().size() >= 2, game.getId() + " needs test styles");
                assertTrue(motifs.size() >= 2, game.getId() + " styles need distinct effect shapes");
                assertTrue(colours.size() >= 2, game.getId() + " styles need distinct colours");
            }
        }
        assertEquals(3, effectLines, "Only Bacon Brawl, Snake and Super Sploor use the shared ability renderer.");
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
