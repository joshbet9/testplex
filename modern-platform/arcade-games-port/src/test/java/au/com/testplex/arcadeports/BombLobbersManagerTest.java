package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BombLobbersManagerTest {
    @Test
    void controllerOwnsOnlyBombLobbersScoreboard() {
        BombLobbersManager bombLobbers = new BombLobbersManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(bombLobbers);

        assertSame(bombLobbers, registry.get(ArcadePortGame.BOMB_LOBBERS));
        assertNull(registry.get(ArcadePortGame.DEATH_TAG));
    }

    @Test
    void emptyMatchScoreboardUpdateDoesNotAllocateTasksOrPlayers() {
        BombLobbersManager bombLobbers = new BombLobbersManager();
        SessionState session = new SessionState(ArcadePortGame.BOMB_LOBBERS, null, List.of(), false);

        bombLobbers.updateScoreboard(session);
        bombLobbers.updateScoreboard(null);

        assertTrue(session.bombPowerupStatusByTeam.isEmpty());
        assertTrue(session.alive.isEmpty());
    }
}
