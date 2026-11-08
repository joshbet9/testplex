package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

    @Test
    void emptySpectatorSessionKeepsOriginalQuarterSecondRefreshCadence() {
        SpleefManager spleef = new SpleefManager();
        SessionState session = new SessionState(ArcadePortGame.SPLEEF, null, List.of(), false);

        spleef.updateSpectatorCompasses(session, 100L);
        assertEquals(350L, session.spleefNextCompassUpdateAt);
        spleef.updateSpectatorCompasses(session, 200L);
        assertEquals(350L, session.spleefNextCompassUpdateAt);
        spleef.updateSpectatorCompasses(session, 350L);
        assertEquals(600L, session.spleefNextCompassUpdateAt);
        assertNull(spleef.nearestSpectatorTarget(session, null));
        assertFalse(spleef.isTrackingCompass(null));
        assertFalse(spleef.isFletchedArrow(null));
    }
}
