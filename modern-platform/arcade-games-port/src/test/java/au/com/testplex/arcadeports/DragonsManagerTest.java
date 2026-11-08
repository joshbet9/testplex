package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
}
