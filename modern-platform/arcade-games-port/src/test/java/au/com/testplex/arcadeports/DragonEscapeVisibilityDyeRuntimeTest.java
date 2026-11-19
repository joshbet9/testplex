package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class DragonEscapeVisibilityDyeRuntimeTest {
    @Test
    void usesMonsterMazeStyleStateDyes() {
        assertEquals(Material.GRAY_DYE, DragonEscapeVisibilityDyeRuntime.dyeForVisibility(false));
        assertEquals(Material.LIME_DYE, DragonEscapeVisibilityDyeRuntime.dyeForVisibility(true));
    }
}
