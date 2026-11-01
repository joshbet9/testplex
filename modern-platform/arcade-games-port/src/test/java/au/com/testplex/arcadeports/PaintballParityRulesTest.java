package au.com.testplex.arcadeports;

import org.bukkit.Color;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaintballParityRulesTest {
    @Test void sourceTeamOrderIsFrostAquaThenNetherRed() {
        assertEquals(Color.AQUA, ArcadePortRules.paintballTeamColor(0));
        assertEquals(Color.RED, ArcadePortRules.paintballTeamColor(1));
        assertFalse(ArcadePortRules.isPaintballRedTeam(0));
        assertTrue(ArcadePortRules.isPaintballRedTeam(1));
    }
    @Test void projectileImpactUsesSourcePaintOnWool() {
        assertEquals(Material.LIGHT_BLUE_WOOL, ArcadePortRules.paintballPaintedBlock(Material.WHITE_WOOL, 0));
        assertEquals(Material.RED_WOOL, ArcadePortRules.paintballPaintedBlock(Material.WHITE_WOOL, 1));
    }
    @Test void projectileImpactConvertsHardClayToTeamTerracotta() {
        assertEquals(Material.LIGHT_BLUE_TERRACOTTA, ArcadePortRules.paintballPaintedBlock(Material.TERRACOTTA, 0));
        assertEquals(Material.RED_TERRACOTTA, ArcadePortRules.paintballPaintedBlock(Material.TERRACOTTA, 1));
    }
    @Test void unrelatedBlocksAreNotPainted() {
        assertNull(ArcadePortRules.paintballPaintedBlock(Material.STONE, 0));
        assertNull(ArcadePortRules.paintballPaintedBlock(Material.GLASS, 1));
    }
}
