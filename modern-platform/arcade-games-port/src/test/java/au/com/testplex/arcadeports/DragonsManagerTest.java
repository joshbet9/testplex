package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class DragonsManagerTest {
    @Test
    void registersOnlyForDragons() {
        DragonsManager dragons = new DragonsManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(dragons);

        assertSame(dragons, registry.get(ArcadePortGame.DRAGONS));
        assertNull(registry.get(ArcadePortGame.DEATH_TAG));
    }

    @Test
    void scoreboardUpdateIsSafeBeforePreparation() {
        DragonsManager dragons = new DragonsManager();
        SessionState session = new SessionState(ArcadePortGame.DRAGONS, null, List.of(), false);

        dragons.updateDragonsScoreboard(session, 1_000L);
        dragons.updateDragonsScoreboard(null, 1_000L);
    }
}
