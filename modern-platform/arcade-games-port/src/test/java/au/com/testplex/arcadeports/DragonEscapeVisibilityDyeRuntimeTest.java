package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

class DragonEscapeVisibilityDyeRuntimeTest {
    @Test
    void usesMonsterMazeStyleStateDyesAndKeepsTheToggleMetadata() {
        ItemStack toggle = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = toggle.getItemMeta();
        meta.setDisplayName("Player Visibility: Invisible");
        toggle.setItemMeta(meta);

        ItemStack invisible = DragonEscapeVisibilityDyeRuntime.withVisibilityDye(toggle, false);
        ItemStack visible = DragonEscapeVisibilityDyeRuntime.withVisibilityDye(toggle, true);

        assertEquals(Material.GRAY_DYE, invisible.getType());
        assertEquals(Material.LIME_DYE, visible.getType());
        assertEquals("Player Visibility: Invisible", invisible.getItemMeta().getDisplayName());
        assertEquals(toggle.getType(), Material.NAME_TAG, "The original manager item remains unmodified");
        assertNotSame(toggle, invisible);
        assertNotSame(toggle, visible);
    }
}
