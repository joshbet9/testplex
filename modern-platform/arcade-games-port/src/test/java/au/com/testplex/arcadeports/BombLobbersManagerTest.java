package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.ChatColor;
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

    @Test
    void blastExposureReturnsZeroForAbsentWorldAndTarget() {
        BombLobbersManager bombLobbers = new BombLobbersManager();
        assertEquals(0.0D, bombLobbers.blastExposure(null, null));
    }

    @Test
    void emptySpectatorSessionDoesNotAllocateCompassesOrMenuTargets() {
        BombLobbersManager bombLobbers = new BombLobbersManager();
        SessionState session = new SessionState(ArcadePortGame.BOMB_LOBBERS, null, List.of(), false);

        bombLobbers.updateSpectatorCompasses(session);
        bombLobbers.updateSpectatorCompasses(null);
        bombLobbers.handleSpectatorMenuClick(session, null, 19, false);

        assertNull(bombLobbers.nearestSpectateTarget(session, null));
        assertNull(bombLobbers.nearestSpectateTarget(null, null));
        assertFalse(bombLobbers.isTrackingCompass(null));
        assertTrue(session.bombCompassMenuTargets.isEmpty());
        assertTrue(session.bombSpectateNextTeleportAt.isEmpty());
    }

    @Test
    void spectatorMenuTitlePreservesSourceFormatting() {
        BombLobbersManager bombLobbers = new BombLobbersManager();
        assertTrue(bombLobbers.isSpectatorMenuTitle(ChatColor.DARK_GREEN + "Spectator Menu"));
        assertFalse(bombLobbers.isSpectatorMenuTitle("Spectator Menu"));
        assertFalse(bombLobbers.isSpectatorMenuTitle(null));
    }
}
