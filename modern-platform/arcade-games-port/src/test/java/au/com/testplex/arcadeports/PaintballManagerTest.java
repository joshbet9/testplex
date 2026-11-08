package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaintballManagerTest {
    @Test
    void controllerClaimsOnlyPaintball() {
        PaintballManager paintball = new PaintballManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(paintball);

        assertSame(paintball, registry.get(ArcadePortGame.PAINTBALL));
        assertNull(registry.get(ArcadePortGame.BOMB_LOBBERS));
    }

    @Test
    void emptyMatchScoreboardUpdateIsSafe() {
        PaintballManager paintball = new PaintballManager();
        SessionState session = new SessionState(ArcadePortGame.PAINTBALL, null, List.of(), false);

        paintball.updateScoreboard(session, 500L);
        paintball.updateScoreboard(null, 500L);

        assertTrue(session.paintballScoreboardEntries.isEmpty());
    }
}
