package me.monstermaze.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MobThemeManagerThemeRulesTest {
    @Test
    void normalizesLegacyAndMapSpecificMobNames() {
        assertEquals("snowman", MobThemeManager.normalize("snow_golem"));
        assertEquals("zombie_pigman", MobThemeManager.normalize("zombified_piglin"));
        assertEquals("mooshroom", MobThemeManager.normalize("mushroom_cow"));
        assertEquals("enderman", MobThemeManager.normalize("Eye of Ender"));
        assertEquals("map_default", MobThemeManager.normalize("map_default"));
        assertEquals("creeper", MobThemeManager.normalize(null));
        assertEquals("creeper", MobThemeManager.normalize("not-a-real-mob"));
    }

    @Test
    void twoBlockRuleMeasuresMazeFootprintNotVerticalHeight() {
        // Wither Skeleton is taller than two blocks but only ~0.7 block wide.
        for (String tallAndNarrow : List.of("wither_skeleton", "enderman", "iron_golem",
                "warden", "creaking", "villager")) {
            assertEquals(tallAndNarrow, MobThemeManager.normalize(tallAndNarrow));
        }
        assertEquals(Material.WITHER_SKELETON_SKULL,
                MobThemeManager.selectorIcon("wither_skeleton"));
    }

    @Test
    void modernTwentySixThreeMobFormsAreAvailableAsPersonalViewCosmetics() {
        var available = MobThemeManager.allowedThemes(Set.of("wither_skeleton",
                "bogged", "breeze", "parched", "copper_golem", "sulfur_cube",
                "nautilus", "zombie_nautilus", "camel_husk", "elder_guardian"));
        assertEquals(10, available.size());
        assertTrue(available.contains("wither_skeleton"));
        assertTrue(available.contains("sulfur_cube"));
        assertTrue(available.contains("camel_husk"));
    }

    @Test
    void oversizeHorizontalBossModelsCannotBeSelectedOrRandomized() {
        for (String oversized : List.of("ghast", "happy_ghast", "ender_dragon",
                "giant", "wither")) {
            assertEquals("creeper", MobThemeManager.normalize(oversized));
        }
        assertEquals(List.of("map_default"),
                MobThemeManager.allowedThemes(Set.of("ghast", "happy_ghast")));
    }

    @Test
    void paletteColorsMatchTheMobThemeMappings() {
        assertEquals("LIME", MobThemeManager.color("creeper"));
        assertEquals("PURPLE", MobThemeManager.color("witch"));
        assertEquals("LIGHT_GRAY", MobThemeManager.color("skeleton"));
        assertEquals("WHITE", MobThemeManager.color("snowman"));
        assertEquals("RED", MobThemeManager.color("zombie_pigman"));
        assertEquals("PINK", MobThemeManager.color("pig"));
        assertEquals("CYAN", MobThemeManager.color("guardian"));
        assertEquals("BLUE", MobThemeManager.color("squid"));
    }

    @Test
    void selectorUsesThemeSpecificVanillaIcons() {
        assertEquals(Material.PUMPKIN, MobThemeManager.selectorIcon("snowman"));
        assertEquals(Material.NETHER_STAR, MobThemeManager.selectorIcon("random"));
        assertEquals(Material.CREEPER_HEAD, MobThemeManager.selectorIcon("creeper"));
        assertEquals(Material.SKELETON_SKULL, MobThemeManager.selectorIcon("skeleton"));
        assertEquals(Material.ZOMBIE_HEAD, MobThemeManager.selectorIcon("zombie"));
        assertEquals(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG,
                MobThemeManager.spawnEggMaterial("zombie_pigman"));
    }

    @Test
    void randomThemeIsStableForViewerAndEntityAndNeverRecurses() {
        UUID viewer = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID entity = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        String first = MobThemeManager.randomConcrete(viewer, entity);
        assertEquals(first, MobThemeManager.randomConcrete(viewer, entity));
        assertNotEquals("map_default", first);
        assertNotEquals("random", first);
        assertTrue(MobThemeManager.normalize(first).equals(first));
    }

    @Test
    void themeLabelsRemainReadable() {
        assertEquals("Map Default", MobThemeManager.displayName("map_default"));
        assertEquals("Random Mob", MobThemeManager.displayName("random"));
        assertEquals("Zombie Pigman", MobThemeManager.displayName("zombie_pigman"));
    }

    @Test
    void selectorCyclesWhenLookingAtMobButOnlyForMainHand() {
        assertTrue(MobThemeManager.shouldCycleSelectorEntityInteract(true, true));
        assertFalse(MobThemeManager.shouldCycleSelectorEntityInteract(false, true));
        assertFalse(MobThemeManager.shouldCycleSelectorEntityInteract(true, false));
    }

    @Test
    void selectorCanBeRightClickedInThePlayerInventoryWithoutBeingMoved() {
        assertTrue(MobThemeManager.shouldCycleSelectorInventoryClick(true, true, true));
        assertFalse(MobThemeManager.shouldCycleSelectorInventoryClick(false, true, true));
        assertFalse(MobThemeManager.shouldCycleSelectorInventoryClick(true, false, true));
        assertFalse(MobThemeManager.shouldCycleSelectorInventoryClick(true, true, false));
    }

    @Test
    void selectorRotatesAmongMultipleSelectedMobSetsEvenWithGroupedPaletteOptionsPresent() {
        List<String> enabled = MobThemeManager.allowedThemes(Set.of(
                "creeper", "skeleton", "beacon-creeper-red", "pad-skeleton-blue"));
        assertEquals(List.of("creeper", "skeleton"), enabled);
        assertEquals("skeleton", MobThemeManager.nextTheme("creeper", enabled));
        assertEquals("creeper", MobThemeManager.nextTheme("skeleton", enabled));
    }

    @Test
    void selectorCyclesOnlyCosmeticMenuEnabledThemes() {
        List<String> enabled = MobThemeManager.allowedThemes(Set.of("map_default", "axolotl", "frog"));

        assertEquals(List.of("map_default", "axolotl", "frog"), enabled);
        assertEquals("axolotl", MobThemeManager.nextTheme("map_default", enabled));
        assertEquals("map_default", MobThemeManager.nextTheme("frog", enabled));
        assertEquals(List.of("map_default"), MobThemeManager.allowedThemes(Set.of()));
    }
}
