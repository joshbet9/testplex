package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    @Test
    void registersAllTwentyFiveArcadeGames() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(25, catalog.size());
        assertEquals(25, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void eachGameHasItsOwnCosmeticLinesAndWinEffects() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            boolean gameplayFirstPort = Set.of("draw-my-thing", "master-builders", "sneaky-assassins", "castle-assault", "heroes-of-gwen", "milk-the-cow", "wither-assault", "plexball").contains(game.getId());
            assertEquals(gameplayFirstPort, game.getCosmeticLines().isEmpty(),
                    game.getId() + " cosmetic lines should remain optional during this gameplay-first port");
            assertTrue(game.getWinEffects().size() >= (gameplayFirstPort ? 1 : 3),
                    game.getId() + " needs its currently defined win-effect choices");
            Set<String> lineIds = new HashSet<>();
            for (CosmeticLineDefinition line : game.getCosmeticLines()) {
                assertTrue(lineIds.add(line.getId()), game.getId() + " has a duplicate line id");
                Set<String> optionIds = new HashSet<>();
                for (CosmeticOption option : line.getOptions()) {
                    assertTrue(optionIds.add(option.getId()), game.getId() + "/" + line.getId() + " has duplicate options");
                }
            }
            Set<String> winIds = new HashSet<>();
            for (CosmeticOption win : game.getWinEffects()) {
                assertTrue(winIds.add(win.getId()), game.getId() + " has duplicate win-effect ids");
            }
        }
    }

    @Test
    void castleAssaultAndHeroesOfGwenAppearInTheLobbyAsInProgress() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        GameDefinition castleAssault = catalog.getGame("castle-assault");
        GameDefinition heroesOfGwen = catalog.getGame("heroes-of-gwen");
        assertNotNull(castleAssault);
        assertNotNull(heroesOfGwen);
        assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, castleAssault.getPortStatus());
        assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, heroesOfGwen.getPortStatus());
        assertFalse(castleAssault.isReadyForTest());
        assertFalse(heroesOfGwen.isReadyForTest());
    }

    @Test
    void dragonsExposeServerVisibleFlareEffectChoices() {
        GameDefinition dragons = new ArcadeCatalog().getGame("dragons");
        CosmeticLineDefinition flares = dragons.getCosmeticLines().stream()
                .filter(line -> line.getId().equals("flare-effects"))
                .findFirst().orElseThrow();
        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, flares.getSelectionMode());
        assertEquals("classic", flares.getDefaultOptionId());
        assertEquals(Set.of("classic", "ember", "frost", "ender", "solar", "aurora"),
                new HashSet<>(flares.getOptions().stream().map(CosmeticOption::getId).toList()));
    }

    @Test
    void gladiatorsIncludesPlayerSelectableKillMessages() {
        GameDefinition game = new ArcadeCatalog().getGame("gladiators");
        CosmeticLineDefinition line = game.getCosmeticLines().stream()
                .filter(candidate -> candidate.getId().equals("kill-message"))
                .findFirst().orElseThrow();
        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, line.getSelectionMode());
        assertEquals(Set.of("classic", "sent-to-sand", "broken-oath", "outclassed"),
                new HashSet<>(line.getOptions().stream().map(CosmeticOption::getId).toList()));
    }

    @Test
    void microBattlesUsesOneCombinedLineWithIndependentPrivateAndSharedColourGroups() {
        GameDefinition game = new ArcadeCatalog().getGame("micro-battles");
        assertNotNull(game);
        assertEquals(1, game.getCosmeticLines().size());
        CosmeticLineDefinition visuals = game.getCosmeticLines().get(0);
        assertEquals("island-visuals", visuals.getId());
        assertEquals(CosmeticLineDefinition.SelectionMode.MULTI, visuals.getSelectionMode());
        assertEquals("glass-light-blue", visuals.getDefaultOptionId());
        assertEquals(32, visuals.getOptions().size());
        assertEquals(16, visuals.getOptions().stream()
                .filter(option -> "glass".equals(option.getSelectionGroup())).count());
        assertEquals(16, visuals.getOptions().stream()
                .filter(option -> "border".equals(option.getSelectionGroup())).count());
        assertTrue(visuals.getOptions().stream()
                .filter(option -> "glass".equals(option.getSelectionGroup()))
                .allMatch(option -> option.getDescription().contains("Only you")));
        assertTrue(visuals.getOptions().stream()
                .filter(option -> "border".equals(option.getSelectionGroup()))
                .allMatch(option -> option.getDescription().contains("Everyone")));
    }

    @Test
    void explicitlyClearedGamesAreAvailableTogetherForInGameAcceptance() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        Set<String> readyIds = Set.of("death-tag", "one-in-the-quiver", "bomb-lobbers", "bacon-brawl", "gladiators", "micro-battles", "paintball", "plexball", "spleef");

        for (GameDefinition game : catalog.getGames()) {
            if (readyIds.contains(game.getId())) {
                assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus(),
                        game.getId() + " is explicitly cleared for the shared in-game test queue");
                assertTrue(game.isReadyForTest());
            } else {
                assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus(),
                        game.getId() + " must remain gated until it is promoted to the shared test queue");
                assertFalse(game.isReadyForTest());
            }
        }
        long readyForTest = catalog.getGames().stream().filter(GameDefinition::isReadyForTest).count();
        long inProgress = catalog.getGames().stream()
                .filter(game -> game.getPortStatus() == GameDefinition.PortStatus.MECHANICS_IN_PROGRESS).count();
        assertEquals(9, readyForTest);
        assertEquals(16, inProgress);
    }

    @Test
    void monsterMazeMobFormsCanBeEquippedIndependently() {
        GameDefinition game = new ArcadeCatalog().getGame("monster-maze");
        CosmeticLineDefinition mobForms = game.getCosmeticLines().stream()
                .filter(line -> line.getId().equals("mob-sets"))
                .findFirst().orElseThrow();
        assertEquals(CosmeticLineDefinition.SelectionMode.MULTI, mobForms.getSelectionMode());
        assertTrue(mobForms.getOptions().size() >= 8);
    }

    @Test
    void paintballUsesOneCombinedLineWithIndependentWeaponAndPelletChoices() {
        GameDefinition paintball = new ArcadeCatalog().getGame("paintball");
        assertEquals(1, paintball.getCosmeticLines().size());
        CosmeticLineDefinition line = paintball.getCosmeticLines().get(0);
        assertEquals("weapon-and-pellet-identity", line.getId());
        assertEquals(CosmeticLineDefinition.SelectionMode.MULTI, line.getSelectionMode());
        assertEquals(3, line.getOptions().stream()
                .filter(option -> "weapon".equals(option.getSelectionGroup())).count());
        assertEquals(3, line.getOptions().stream()
                .filter(option -> "pellet".equals(option.getSelectionGroup())).count());
        assertTrue(line.getOptions().stream().allMatch(option -> option.getSelectionGroup() != null));
    }

    @Test
    void spleefHasOneSelectableBlockBreakSignatureLine() {
        GameDefinition spleef = new ArcadeCatalog().getGame("spleef");
        assertEquals(1, spleef.getCosmeticLines().size());
        CosmeticLineDefinition line = spleef.getCosmeticLines().get(0);
        assertEquals("break-signature", line.getId());
        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, line.getSelectionMode());
        assertEquals(Set.of("snowburst", "ice-shards", "chalk-grid"),
                line.getOptions().stream().map(CosmeticOption::getId)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(line.getOptions().stream()
                .allMatch(option -> "signature".equals(option.getSelectionGroup())));
    }

    @Test
    void newlyIntegratedGamesRemainInProgressUntilAcceptance() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (String id : List.of("skywars", "milk-the-cow", "wither-assault")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game, id);
            assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus(), id);
            assertFalse(game.isReadyForTest(), id);
        }
    }

    @Test
    void allGameLookupsAreCaseInsensitive() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertNotNull(catalog.getGame("SNAKE"));
        assertNotNull(catalog.getGame("Monster-Maze"));
        assertNull(catalog.getGame("not-a-game"));
    }
    @Test
    void sneakyAssassinsIsCataloguedWithoutInventingCosmeticLines() {
        GameDefinition game = new ArcadeCatalog().getGame("sneaky-assassins");
        assertNotNull(game);
        assertEquals("Sneaky Assassins", game.getName());
        assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus());
        assertTrue(game.getCosmeticLines().isEmpty());
        assertEquals(List.of("last-assassin-standing"),
                game.getWinEffects().stream().map(CosmeticOption::getId).toList());
    }

}
