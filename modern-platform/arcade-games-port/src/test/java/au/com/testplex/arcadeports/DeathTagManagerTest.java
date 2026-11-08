package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeathTagManagerTest {
    @Test
    void deathTagUsesDedicatedControllerWithoutClaimingOtherArcadeGames() {
        DeathTagManager deathTag = new DeathTagManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(deathTag);

        assertSame(deathTag, registry.get(ArcadePortGame.DEATH_TAG));
        assertNull(registry.get(ArcadePortGame.BACON_BRAWL));
    }

    @Test
    void sidebarAndCompassHandleAnEmptyMatchWithoutAllocatingATask() {
        DeathTagManager deathTag = new DeathTagManager();
        SessionState session = new SessionState(ArcadePortGame.DEATH_TAG, null, List.of(), false);

        deathTag.updateDeathTagScoreboard(session);
        deathTag.updateDeathTagCompass(session);

        assertFalse(deathTag.isDeathTagTrackingCompass(null));
    }
}
