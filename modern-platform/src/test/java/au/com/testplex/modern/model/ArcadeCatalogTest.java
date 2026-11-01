package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    @Test
    void registersAllThirtyOneSelectableArcadeIds() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(31, catalog.size());
        assertEquals(31, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void monsterMazeOriginalSpeedAndModernAreSeparateReadyToTestGames() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals("Monster Maze (Original)", catalog.getGame("monster-maze").getName());
        assertEquals("Monster Maze (Speed)", catalog.getGame("monster-maze-speed").getName());
        assertEquals("Monster Maze (Modern)", catalog.getGame("monster-maze-modern").getName());
        for (String id : List.of("monster-maze", "monster-maze-speed", "monster-maze-modern")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game);
            assertTrue(game.isReadyForTest(), id + " must use the standard ready-game selection flow");
            if ("monster-maze".equals(id)) assertEquals(2, game.getCosmeticLines().size());
            else assertTrue(game.getCosmeticLines().isEmpty(),
                    "Speed and Modern must reuse the single Monster Maze cosmetics page");
        }
    }

    @Test
    void eachGameHasItsOwnCosmeticLinesAndWinEffects() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            boolean gameplayFirstPort = Set.of("draw-my-thing", "master-builders", "sneaky-assassins", "castle-assault", "heroes-of-gwen", "hoops-remastered", "milk-the-cow", "wither-assault", "plexball", "super-sploor", "camera-shy", "speed-builders", "monster-maze-speed", "monster-maze-modern").contains(game.getId());
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
    void castleAssaultMilkTheCowAndHeroesOfGwenAreReadyForTest() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        GameDefinition castleAssault = catalog.getGame("castle-assault");
        GameDefinition milkTheCow = catalog.getGame("milk-the-cow");
        GameDefinition heroesOfGwen = catalog.getGame("heroes-of-gwen");
        assertNotNull(castleAssault);
        assertNotNull(milkTheCow);
        assertNotNull(heroesOfGwen);
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, castleAssault.getPortStatus());
        assertTrue(castleAssault.isReadyForTest());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, milkTheCow.getPortStatus());
        assertTrue(milkTheCow.isReadyForTest());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, heroesOfGwen.getPortStatus());
        assertTrue(heroesOfGwen.isReadyForTest());
    }

    @Test
    void dragonsUsePyroAndSparklerCosmeticsWithoutDragonFlares() {
        GameDefinition dragons = new ArcadeCatalog().getGame("dragons");
        assertNotNull(dragons);
        assertTrue(dragons.getCosmeticLines().stream()
                .anyMatch(line -> line.getId().equals("pyro-signature")));
        CosmeticLineDefinition sparklers = dragons.getCosmeticLines().stream()
                .filter(line -> line.getId().equals("sparkler-effects"))
                .findFirst().orElseThrow();
        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, sparklers.getSelectionMode());
        assertEquals("golden-thread", sparklers.getDefaultOptionId());
        assertEquals(Set.of("golden-thread", "cinder-drift", "glacier-dust", "void-motes", "starlight", "prismatic"),
                new HashSet<>(sparklers.getOptions().stream().map(CosmeticOption::getId).toList()));
        assertFalse(dragons.getCosmeticLines().stream()
                .anyMatch(line -> line.getId().equals("flare-effects")));
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
        Set<String> readyIds = Set.of("death-tag", "one-in-the-quiver", "bomb-lobbers", "bacon-brawl", "castle-assault", "gladiators", "micro-battles", "milk-the-cow", "paintball", "plexball", "spleef", "super-sploor", "dragon-escape", "dragons", "runner", "sheep-quest", "turf-wars", "camera-shy", "monster-maze", "monster-maze-speed", "monster-maze-modern", "snake", "draw-my-thing", "master-builders", "skywars", "wither-assault", "sneaky-assassins", "speed-builders", "evolution", "heroes-of-gwen", "hoops-remastered");

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
        assertEquals(31, readyForTest);
        assertEquals(0, inProgress);
    }

    @Test
    void turfWarsIsPromotedForControlledSourceMapAcceptance() {
        GameDefinition game = new ArcadeCatalog().getGame("turf-wars");
        assertNotNull(game);
        assertEquals("Turf Wars", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
    }

    @Test
    void sheepQuestIsPromotedForControlledSourceMapAcceptance() {
        GameDefinition game = new ArcadeCatalog().getGame("sheep-quest");
        assertNotNull(game);
        assertEquals("Sheep Quest", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
    }

    @Test
    void dragonEscapeIsPromotedForControlledSourceMapAcceptance() {
        GameDefinition game = new ArcadeCatalog().getGame("dragon-escape");
        assertNotNull(game);
        assertEquals("Dragon Escape", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
    }

    @Test
    void dragonsAndRunnerArePromotedForControlledSourceMapAcceptance() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertTrue(catalog.getGame("dragons").isReadyForTest());
        assertTrue(catalog.getGame("runner").isReadyForTest());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, catalog.getGame("dragons").getPortStatus());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, catalog.getGame("runner").getPortStatus());
    }

    @Test
    void cameraShyIsCataloguedForControlledSpyglassEliminationTesting() {
        GameDefinition game = new ArcadeCatalog().getGame("camera-shy");
        assertNotNull(game);
        assertEquals("Snap", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
        assertTrue(game.getDescription().contains("five exposures"));
        assertEquals(List.of("last-photographer-standing"),
                game.getWinEffects().stream().map(CosmeticOption::getId).toList());
        assertTrue(game.getCosmeticLines().isEmpty());
    }

    @Test
    void superSploorIsCataloguedForControlledTeamTerritoryTesting() {
        GameDefinition game = new ArcadeCatalog().getGame("super-sploor");
        assertNotNull(game);
        assertEquals("Super Sploor", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
        assertTrue(game.getCosmeticLines().isEmpty());
    }

    @Test
    void microBattlesUsesOneCombinedMultiSelectGlassAndBorderLine() {
        GameDefinition game = new ArcadeCatalog().getGame("micro-battles");
        assertNotNull(game);
        assertEquals(1, game.getCosmeticLines().size());
        CosmeticLineDefinition line = game.getCosmeticLines().get(0);
        assertEquals("island-visuals", line.getId());
        assertEquals(CosmeticLineDefinition.SelectionMode.MULTI, line.getSelectionMode());
        assertEquals(32, line.getOptions().size());
        assertEquals(16, line.getOptions().stream().filter(option -> option.getId().startsWith("glass-")).count());
        assertEquals(16, line.getOptions().stream().filter(option -> option.getId().startsWith("border-")).count());
        assertTrue(line.getDescription().contains("private starting-glass"));
        assertTrue(line.getDescription().contains("shared border-particle"));
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
    void newlyPromotedGamesAndHeroesOfGwenAreReadyForTest() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (String id : List.of("skywars", "wither-assault", "sneaky-assassins")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game, id);
            assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus(), id);
            assertTrue(game.isReadyForTest(), id);
        }
        for (String id : List.of("heroes-of-gwen", "hoops-remastered")) {
            GameDefinition game = catalog.getGame(id);
            assertNotNull(game, id);
            assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus(), id);
            assertTrue(game.isReadyForTest(), id);
        }
    }

    @Test
    void hoopsRemasteredIsAvailableForControlledTwoToSixteenPlayerTesting() {
        GameDefinition game = new ArcadeCatalog().getGame("hoops-remastered");
        assertNotNull(game);
        assertEquals("Hoops Remastered", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
        assertTrue(game.getCosmeticLines().isEmpty());
    }

    @Test
    void speedBuildersIsPromotedForControlledSixteenPlayerTesting() {
        GameDefinition game = new ArcadeCatalog().getGame("speed-builders");
        assertNotNull(game);
        assertEquals("Speed Builders", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.isReadyForTest());
        assertTrue(game.getCosmeticLines().isEmpty());
        assertEquals(List.of("highest-accuracy"),
                game.getWinEffects().stream().map(CosmeticOption::getId).toList());
    }

    @Test
    void allGameLookupsAreCaseInsensitive() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertNotNull(catalog.getGame("SNAKE"));
        assertNotNull(catalog.getGame("Monster-Maze"));
        assertNull(catalog.getGame("not-a-game"));
    }
    @Test
    void sneakyAssassinsIsReadyForTestWithoutInventingCosmeticLines() {
        GameDefinition game = new ArcadeCatalog().getGame("sneaky-assassins");
        assertNotNull(game);
        assertEquals("Sneaky Assassins", game.getName());
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, game.getPortStatus());
        assertTrue(game.getCosmeticLines().isEmpty());
        assertEquals(List.of("last-assassin-standing"),
                game.getWinEffects().stream().map(CosmeticOption::getId).toList());
    }

}
