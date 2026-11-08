package au.com.testplex.arcadeports;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BombLobbersSpatialRulesTest {
    @Test void missingMapOrTeamSpawnsCannotInventOwnership() {
        SessionState match = new SessionState(ArcadePortGame.BOMB_LOBBERS, null, List.of(), false);
        BombLobbersSpatialRules.cacheBombLobbersTeamSpawnCentres(match, null);
        assertTrue(match.bombTeamSpawnCentres.isEmpty());
        assertEquals(-1, BombLobbersSpatialRules.nearestBombLobbersSpawnTeam(match, null));
        assertEquals(-1, BombLobbersSpatialRules.nearestBombLobbersSpawnTeam(match,
                new Location(null, 0, 64, 0)));
        assertEquals(0, BombLobbersSpatialRules.bombLobbersOnlineMatchPlayers(match));
        assertEquals(0, BombLobbersSpatialRules.bombLobbersOnlineMatchPlayers(null));
    }

    @Test void nullTargetsNeverReceiveBlastExposure() {
        assertEquals(0.0D, BombLobbersSpatialRules.bombLobbersBlastExposure(null, null));
    }
}
