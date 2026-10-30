package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ArcadeCatalogTest {
    @Test
    void registersAllSixteenArcadeGames() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertEquals(16, catalog.size());
        assertEquals(16, new HashSet<>(catalog.getGames().stream().map(GameDefinition::getId).toList()).size());
    }

    @Test
    void eachGameHasItsOwnCosmeticLinesAndWinEffects() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            assertFalse(game.getCosmeticLines().isEmpty(), game.getId() + " needs a cosmetic line");
            assertTrue(game.getWinEffects().size() >= 3, game.getId() + " needs multiple distinct win effects");
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
    void everyGameHasAnInitialMechanicsPortButNoneIsDeclaredReadyBeforeLiveValidation() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        for (GameDefinition game : catalog.getGames()) {
            assertEquals(GameDefinition.PortStatus.MECHANICS_IN_PROGRESS, game.getPortStatus(),
                    game.getId() + " must remain in progress until its modern gameplay is validated");
            assertFalse(game.isReadyForTest());
        }
        long inProgress = catalog.getGames().stream()
                .filter(game -> game.getPortStatus() == GameDefinition.PortStatus.MECHANICS_IN_PROGRESS)
                .count();
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
    void allGameLookupsAreCaseInsensitive() {
        ArcadeCatalog catalog = new ArcadeCatalog();
        assertNotNull(catalog.getGame("SNAKE"));
        assertNotNull(catalog.getGame("Monster-Maze"));
        assertNull(catalog.getGame("not-a-game"));
    }
}
