package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SpleefManagerTest {
    @Test
    void floorRulesUseOnlySpleefMaterialLogic() {
        SpleefManager spleef = new SpleefManager();
        assertTrue(spleef.isWoolOrTerracotta(Material.RED_WOOL));
        assertTrue(spleef.isWoolOrTerracotta(Material.TERRACOTTA));
        assertFalse(spleef.isWoolOrTerracotta(Material.STONE));
        assertNull(spleef.projectileImpactBlock(null));
    }

    @Test
    void controllerClaimsOnlySpleef() {
        SpleefManager spleef = new SpleefManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(spleef);
        assertSame(spleef, registry.get(ArcadePortGame.SPLEEF));
        assertNull(registry.get(ArcadePortGame.RUNNER));
    }
}
