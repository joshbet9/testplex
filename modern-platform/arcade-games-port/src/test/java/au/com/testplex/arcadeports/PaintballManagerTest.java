package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void spectatorCompassUpdateKeepsCadenceWithoutLivingPlayers() {
        PaintballManager paintball = new PaintballManager();
        SessionState session = new SessionState(ArcadePortGame.PAINTBALL, null, List.of(), false);

        paintball.updateSpectatorCompasses(session, 100L);
        assertEquals(350L, session.paintballNextCompassUpdateAt);
        paintball.updateSpectatorCompasses(session, 200L);
        assertEquals(350L, session.paintballNextCompassUpdateAt);
        paintball.updateSpectatorCompasses(null, 400L);
        assertFalse(paintball.isTrackingCompass(null));
    }
}
