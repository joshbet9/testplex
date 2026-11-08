package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class GladiatorsManagerTest {
    @Test
    void controllerClaimsOnlyGladiators() {
        GladiatorsManager gladiators = new GladiatorsManager(Logger.getAnonymousLogger());
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(gladiators);

        assertSame(gladiators, registry.get(ArcadePortGame.GLADIATORS));
        assertNull(registry.get(ArcadePortGame.DRAGON_ESCAPE));
    }

    @Test
    void prePrepareUpdatesAndCleanupAreSafe() {
        GladiatorsManager gladiators = new GladiatorsManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.GLADIATORS, null, List.of(), false);

        gladiators.updateScoreboard(session, 500L);
        gladiators.cleanupScoreboard(session);
        gladiators.updateScoreboard(null, 500L);

        assertTrue(session.gladiatorScoreboardEntries.isEmpty());
    }
}
