package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

class DragonsManagerTest {
    @Test
    void controllerClaimsOnlyDragons() {
        DragonsManager dragons = new DragonsManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(dragons);

        assertSame(dragons, registry.get(ArcadePortGame.DRAGONS));
        assertNull(registry.get(ArcadePortGame.PAINTBALL));
    }

    @Test
    void emptySessionUpdateDoesNotNeedASchedulerOrScoreboard() {
        DragonsManager dragons = new DragonsManager();
        SessionState session = new SessionState(ArcadePortGame.DRAGONS, null, List.of(), false);

        dragons.updateScoreboard(session, 500L);
        dragons.updateScoreboard(null, 500L);

        assertTrue(session.alive.isEmpty());
    }
    @Test
    void spectatorTargetMenuLayoutKeepsTheSourceRowsAndInventoryBounds() {
        assertEquals(27, DragonsManager.spectatorMenuSize(0));
        assertEquals(27, DragonsManager.spectatorMenuSize(1));
        assertEquals(List.of(19, 20, 21, 22, 23, 24, 25, 28),
                DragonsManager.spectatorMenuSlots(8));
        assertEquals(36, DragonsManager.spectatorMenuSize(8));
        assertTrue(DragonsManager.spectatorMenuSlots(100).stream().allMatch(slot -> slot < 54));
    }

    @Test
    void spectatorToolsHandleEmptySessionAndNullItemsWithoutServerState() {
        DragonsManager dragons = new DragonsManager();
        SessionState session = new SessionState(ArcadePortGame.DRAGONS, null, List.of(), false);

        dragons.updateSpectatorCompasses(session, 1_000L, null);

        assertFalse(dragons.isTrackingCompass(null));
        assertFalse(dragons.isSpectatorMenu((InventoryHolder) null));
        assertNull(dragons.nearestSpectatorTarget(session, null));
    }

}
