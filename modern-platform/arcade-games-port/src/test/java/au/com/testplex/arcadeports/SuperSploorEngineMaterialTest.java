package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SuperSploorEngineMaterialTest {
    @Test
    void eligiblePaintMaterialsAreLimitedToTheIntendedPalette() {
        assertTrue(SuperSploorEngine.isPaintable(Material.WHITE_WOOL));
        assertTrue(SuperSploorEngine.isPaintable(Material.LIGHT_BLUE_STAINED_GLASS));
        assertTrue(SuperSploorEngine.isPaintable(Material.WHITE_STAINED_GLASS_PANE));
        assertTrue(SuperSploorEngine.isPaintable(Material.WHITE_CONCRETE));
        assertTrue(SuperSploorEngine.isPaintable(Material.WHITE_TERRACOTTA));
        assertTrue(SuperSploorEngine.isPaintable(Material.TERRACOTTA));

        assertFalse(SuperSploorEngine.isPaintable(Material.WHITE_GLAZED_TERRACOTTA));
        assertFalse(SuperSploorEngine.isPaintable(Material.GLASS));
        assertFalse(SuperSploorEngine.isPaintable(Material.STONE));
        assertFalse(SuperSploorEngine.isPaintable(null));
    }

    @Test
    void paintableBlocksAtSpawnRemainPartOfTerritory() {
        assertTrue(SuperSploorEngine.shouldTrackTerritoryBlock(Material.RED_WOOL));
        assertTrue(SuperSploorEngine.shouldTrackTerritoryBlock(Material.LIGHT_BLUE_TERRACOTTA));
        assertFalse(SuperSploorEngine.shouldTrackTerritoryBlock(Material.STONE));
    }
}
