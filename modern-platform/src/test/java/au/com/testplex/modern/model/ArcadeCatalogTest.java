package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    @Test
    void registersAllEighteenArcadeGames() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(18, catalog.size());
        assertEquals(18, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void eachGameHasItsOwnCosmeticLinesAndWinEffects() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            boolean gameplayFirstPort = Set.of("draw-my-thing", "master-builders").contains(game.getId());
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
    void deathTagIsReadyForInGameAcceptanceWhileOtherPortsRemainInProgress() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        GameDefinition deathTag = catalog.getGame("death-tag");
        assertNotNull(deathTag);
        assertEquals(GameDefinition.PortStatus.READY_FOR_TEST, deathTag.getPortStatus());
        assertTrue(deathTag.isReadyForTest());

        for (GameDefinition game : catalog.getGames()) {
            if (game.getId().equals("death-tag")) continue;
            assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus(),
                    game.getId() + " must remain in progress until its gameplay parity is validated");
            assertFalse(game.isReadyForTest());
        }
        long readyForTest = catalog.getGames().stream()
                .filter(GameDefinition::isReadyForTest)
                .count();
        long inProgress = catalog.getGames().stream()
                .filter(game -> game.getPortStatus() == GameDefinition.PortStatus.MECHANICS_IN_PROGRESS)
                .count();
        assertEquals(1, readyForTest);
        assertEquals(17, inProgress);
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
    void microBattlesHasIndependentPersonalGlassAndSharedBorderColourLines() {
        GameDefinition game = new ArcadeCatalog().getGame("micro-battles");
        assertNotNull(game);
        CosmeticLineDefinition middleGlass = game.getCosmeticLines().stream()
                .filter(line -> line.getId().equals("middle-glass-colour"))
                .findFirst().orElseThrow();
        CosmeticLineDefinition borderParticles = game.getCosmeticLines().stream()
                .filter(line -> line.getId().equals("border-particle-colour"))
                .findFirst().orElseThrow();

        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, middleGlass.getSelectionMode());
        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, borderParticles.getSelectionMode());
        assertEquals("light-blue", middleGlass.getDefaultOptionId());
        assertEquals("light-blue", borderParticles.getDefaultOptionId());
        assertEquals(16, middleGlass.getOptions().size());
        assertEquals(16, borderParticles.getOptions().size());
        assertTrue(middleGlass.getDescription().contains("Only you"));
        assertTrue(borderParticles.getDescription().contains("Everyone"));
        assertEquals(
                middleGlass.getOptions().stream().map(CosmeticOption::getId).toList(),
                borderParticles.getOptions().stream().map(CosmeticOption::getId).toList());
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
    void allGameLookupsAreCaseInsensitive() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertNotNull(catalog.getGame("SNAKE"));
        assertNotNull(catalog.getGame("Monster-Maze"));
        assertNull(catalog.getGame("not-a-game"));
    }
}
