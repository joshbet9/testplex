package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class OneInTheQuiverManagerTest {
    @Test
    void controllerClaimsOnlyQuiver() {
        OneInTheQuiverManager quiver = new OneInTheQuiverManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(quiver);

        assertSame(quiver, registry.get(ArcadePortGame.ONE_IN_THE_QUIVER));
        assertNull(registry.get(ArcadePortGame.DRAGONS));
    }

    @Test
    void emptySessionRankingUpdateDoesNotRequireServer() {
        OneInTheQuiverManager quiver = new OneInTheQuiverManager();
        SessionState session = new SessionState(ArcadePortGame.ONE_IN_THE_QUIVER, null, List.of(), false);

        quiver.updateScoreboard(session);
        quiver.updateScoreboard(null);

        assertTrue(session.quiverScoreboardEntries.isEmpty());
    }

    @Test
    void unsafeAuthoredSpawnAndHazardsAreRejected() {
        assertFalse(OneInTheQuiverManager.isSafeSpawn(null));
        assertFalse(OneInTheQuiverManager.isSafeSpawn(new Location(null, 0, 64, 0)));
        assertTrue(OneInTheQuiverManager.isSpawnHazard(Material.LAVA));
        assertTrue(OneInTheQuiverManager.isSpawnHazard(Material.POWDER_SNOW));
        assertFalse(OneInTheQuiverManager.isSpawnHazard(Material.STONE));
        assertEquals(List.of(), new OneInTheQuiverManager().spawnLocations(
                null, null, 16, Logger.getLogger("quiver-spawn-test")));
    }
}
