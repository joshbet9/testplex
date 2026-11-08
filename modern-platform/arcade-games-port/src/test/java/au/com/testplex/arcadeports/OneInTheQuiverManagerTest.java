package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
}
