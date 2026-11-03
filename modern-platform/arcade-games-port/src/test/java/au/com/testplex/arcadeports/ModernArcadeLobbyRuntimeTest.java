package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ModernArcadeLobbyRuntimeTest {
    @Test
    void kitNpcArmorIsAlwaysPaddedOrTruncatedToFourEquipmentSlots() {
        assertArrayEquals(new Material[] {Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
                        Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET},
                ModernArcadeLobbyRuntime.npcArmorSlots(new Material[] {Material.LEATHER_BOOTS,
                        Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET,
                        Material.DIAMOND_HELMET}));
        assertArrayEquals(new Material[] {Material.IRON_BOOTS, null, null, null},
                ModernArcadeLobbyRuntime.npcArmorSlots(new Material[] {Material.IRON_BOOTS}));
        assertArrayEquals(new Material[] {null, null, null, null}, ModernArcadeLobbyRuntime.npcArmorSlots(null));
    }
}
