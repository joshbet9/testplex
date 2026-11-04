package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    @Test
    void registersAllThirtyTwoSelectableArcadeIds() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(32, catalog.size());
        assertEquals(32, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void everyGameHasExactlyOneSingleSelectAbilityEffectsLineAndSeparateWinEffects() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            assertEquals(1, game.getCosmeticLines().size(), game.getId());
            CosmeticLineDefinition line = game.getCosmeticLines().get(0);
            assertEquals("ability-effects", line.getId(), game.getId());
            assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, line.getSelectionMode(), game.getId());
            assertTrue(line.getOptions().size() >= 2, game.getId() + " needs preview choices");
            assertNotNull(line.getDefaultOptionId(), game.getId());
            assertTrue(line.getOptions().stream().anyMatch(option -> option.getId().equals(line.getDefaultOptionId())),
                    game.getId() + " default ability style must be selectable");
            Set<String> optionIds = new HashSet<>();
            for (CosmeticOption option : line.getOptions()) {
                assertTrue(optionIds.add(option.getId()), game.getId() + " has a duplicate ability style");
                assertNull(option.getSelectionGroup(), game.getId() + " should not split a style across categories");
            }
            assertTrue(line.getDescription().contains("does not change global cosmetics"), game.getId());
            assertFalse(game.getWinEffects().isEmpty(), game.getId() + " win celebrations remain independent");
            assertTrue(game.getWinEffects().stream().anyMatch(effect -> effect.getId().equals(game.getDefaultWinEffectId())),
                    game.getId() + " keeps a valid separate default win effect");
        }
    }

    @Test
    void monsterMazeModesShareOneAbilityLineWithoutASeparateCosmeticArchitecture() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        List<String> expected = List.of("maze-pulse", "soul-grid", "glow-path");
        for (String id : List.of("monster-maze", "monster-maze-speed", "monster-maze-modern")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game);
            assertTrue(game.isReadyForTest(), id);
            assertEquals(1, game.getCosmeticLines().size(), id);
            assertEquals("ability-effects", game.getCosmeticLines().get(0).getId(), id);
            assertEquals(expected, game.getCosmeticLines().get(0).getOptions().stream()
                    .map(CosmeticOption::getId).toList(), id);
        }
    }

    @Test
    void stylesMatchTheGamesAbilityIdentityAndPreserveRunnerRedFallback() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals("phoenix-arc", line(catalog, "dragons").getDefaultOptionId());
        assertEquals(Set.of("chrome-marker", "colourburst", "stencil-stamp"), optionIds(catalog, "paintball"));
        assertEquals("trail-red", line(catalog, "runner").getDefaultOptionId());
        assertEquals(Set.of("snowburst", "ice-shards", "chalk-grid"), optionIds(catalog, "spleef"));
        assertEquals(Set.of("ink-current", "prism-splat", "neon-squid"), optionIds(catalog, "super-sploor"));
    }

    @Test
    void everyGameRemainsAvailableForControlledTesting() {
        for (GameDefinition game : new ArcadeCatalog().getGames()) {
            assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus(), game.getId());
            assertTrue(game.isReadyForTest(), game.getId());
        }
    }

    private static CosmeticLineDefinition line(ArcadeCatalog catalog, String gameId) {
        return catalog.getGame(gameId).getCosmeticLines().get(0);
    }

    private static Set<String> optionIds(ArcadeCatalog catalog, String gameId) {
        return new HashSet<>(line(catalog, gameId).getOptions().stream().map(CosmeticOption::getId).toList());
    }
}
