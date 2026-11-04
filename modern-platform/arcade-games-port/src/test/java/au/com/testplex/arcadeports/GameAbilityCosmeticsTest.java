package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GameAbilityCosmeticsTest {
    @Test
    void everyArcadeGameResolvesToItsAbilityStyleAndRendererPalette() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            String catalogId = game.isMonsterMaze() ? "monster-maze" : game.id();
            String option = GameAbilityCosmetics.defaultOption(catalogId);
            assertNotEquals("arcade-pulse", option,
                    "Missing ability-effect default for " + game.id());
            assertNotNull(GameAbilityCosmetics.palette(option),
                    "Missing render palette for " + game.id());
        }
    }
}
