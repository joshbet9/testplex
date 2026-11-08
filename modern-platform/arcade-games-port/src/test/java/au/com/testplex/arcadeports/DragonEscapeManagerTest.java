package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class DragonEscapeManagerTest {
    @Test
    void controllerClaimsOnlyDragonEscape() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(escape);

        assertSame(escape, registry.get(ArcadePortGame.DRAGON_ESCAPE));
        assertNull(registry.get(ArcadePortGame.DRAGONS));
    }

    @Test
    void emptyRaceScoreboardUpdateAndCleanupDoNotRequireServer() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.DRAGON_ESCAPE, null, List.of(), false);

        escape.updateScoreboard(session, 10L, List.of(), (leader, racer) -> 0L);
        escape.cleanupScoreboard(session);
        escape.updateScoreboard(null, 10L, List.of(), (leader, racer) -> 0L);

        assertTrue(session.dragonEscapeScoreboardTeams.isEmpty());
    }
}
