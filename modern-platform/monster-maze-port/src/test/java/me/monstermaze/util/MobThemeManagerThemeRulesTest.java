package me.monstermaze.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}
