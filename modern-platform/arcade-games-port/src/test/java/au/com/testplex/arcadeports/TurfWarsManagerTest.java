package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class TurfWarsManagerTest {
    @Test
    void controllerClaimsOnlyTurfWars() {
        TurfWarsManager turf = new TurfWarsManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(turf);

        assertSame(turf, registry.get(ArcadePortGame.TURF_WARS));
        assertNull(registry.get(ArcadePortGame.MICRO_BATTLES));
    }

    @Test
    void identifiesMaterialsAndKeepsBarrageSpreadBounded() {
        TurfWarsManager turf = new TurfWarsManager();
        assertTrue(turf.isWool(Material.RED_WOOL));
        assertFalse(turf.isWool(Material.RED_TERRACOTTA));
        assertTrue(turf.isTerracotta(Material.RED_TERRACOTTA));
        assertFalse(turf.isTerracotta(null));
        assertFalse(turf.isFletchedArrow(null));
        assertEquals(0.0D, turf.barrageSpread(0.0D));
        assertEquals(0.0D, turf.barrageSpread(-1.0D));
        for (int i = 0; i < 100; i++) {
            double deviation = turf.barrageSpread(0.2D);
            assertTrue(deviation >= -0.2D && deviation < 0.2D);
        }
    }

    @Test
    void emptyMatchDoesNotStartOrFireBarrages() {
        TurfWarsManager turf = new TurfWarsManager();
        SessionState session = new SessionState(ArcadePortGame.TURF_WARS, null, List.of(), false);
        turf.startBarrage(session, null, false);
        turf.updateBarrages(session, 150L, ignored -> false);
        assertTrue(session.turfBarrageFiring.isEmpty());
        assertTrue(session.turfBarrageCharges.isEmpty());
        assertTrue(session.turfTrackedArrows.isEmpty());
    }
}
