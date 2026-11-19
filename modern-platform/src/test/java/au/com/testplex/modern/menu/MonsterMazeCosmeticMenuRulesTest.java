package au.com.testplex.modern.menu;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.CosmeticOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MonsterMazeCosmeticMenuRulesTest {
    @Test
    void oneMobIconPerSetAndNoColourEntriesInTheMainGrid() {
        var game = new ArcadeCatalog().getGame("monster-maze");
        CosmeticLineDefinition line = game.getCosmeticLines().stream()
                .filter(item -> item.getId().equals("mob-sets")).findFirst().orElseThrow();
        List<String> mobs = line.getOptions().stream()
                .filter(MenuService::isMonsterMazeMobChoice).map(CosmeticOption::getId).toList();

        assertTrue(mobs.size() > 30);
        assertTrue(mobs.size() <= 45, "All forms should fit in one inventory page");
        assertEquals(mobs.size(), Set.copyOf(mobs).size(), "Each mob appears exactly once");
        assertTrue(mobs.contains("map_default"));
        assertTrue(mobs.contains("random"));
        assertTrue(mobs.contains("creeper"));
        assertTrue(mobs.contains("axolotl"));
        assertTrue(mobs.stream().noneMatch(id -> id.startsWith("pad-") || id.startsWith("beacon-")));
    }

    @Test
    void everyMobHasIndependentPadAndBeaconPalettes() {
        var game = new ArcadeCatalog().getGame("monster-maze");
        CosmeticLineDefinition line = game.getCosmeticLines().stream()
                .filter(item -> item.getId().equals("mob-sets")).findFirst().orElseThrow();

        for (String mob : line.getOptions().stream()
                .filter(MenuService::isMonsterMazeMobChoice).map(CosmeticOption::getId).toList()) {
            for (String part : List.of("pad", "beacon")) {
                String group = part + "-" + mob;
                assertEquals(17L, line.getOptions().stream()
                        .filter(option -> group.equals(option.getSelectionGroup())).count(),
                        "Expected inherit plus 16 colours for " + group);
                assertTrue(line.getOptions().stream().anyMatch(
                        option -> option.getId().equals(group + "-inherit")));
            }
        }
    }

    @Test
    void paletteChoiceRetainsTheMobSetIdentity() {
        assertEquals("creeper", MenuService.monsterMazePaletteMob("pad-creeper-lime"));
        assertEquals("zombie_pigman", MenuService.monsterMazePaletteMob("beacon-zombie_pigman-light_blue"));
        assertEquals("map_default", MenuService.monsterMazePaletteMob("pad-map_default-inherit"));
        assertNull(MenuService.monsterMazePaletteMob("creeper"));
        assertNull(MenuService.monsterMazePaletteMob("other-creeper-green"));
        assertNull(MenuService.monsterMazePaletteMob(null));
    }
}
