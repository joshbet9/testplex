package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class MasterBuildersManagerTest {
    @Test
    void controllerClaimsOnlyMasterBuilders() {
        MasterBuildersManager builders = new MasterBuildersManager(Logger.getAnonymousLogger());
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(builders);

        assertSame(builders, registry.get(ArcadePortGame.MASTER_BUILDERS));
        assertNull(registry.get(ArcadePortGame.DRAW_MY_THING));
    }

    @Test
    void voteRecordingRejectsSelfExpiredInvalidAndNonReviewVotes() {
        MasterBuildersManager builders = new MasterBuildersManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.MASTER_BUILDERS, null, List.of(), false);
        UUID builder = UUID.randomUUID();
        UUID voter = UUID.randomUUID();
        session.masterBuilderCurrentTarget = builder;
        session.masterBuilderVoteEndsAt = 1_000L;

        assertTrue(builders.recordVote(session, voter, 6, true, 999L));
        assertEquals(6, session.masterBuilderVotes.get(builder).get(voter));
        assertFalse(builders.recordVote(session, builder, 5, true, 999L));
        assertFalse(builders.recordVote(session, UUID.randomUUID(), 5, true, 1_000L));
        assertFalse(builders.recordVote(session, UUID.randomUUID(), 7, true, 999L));
        assertFalse(builders.recordVote(session, UUID.randomUUID(), 5, false, 999L));
    }

    @Test
    void sidebarTimeRoundsUpToNextSecond() {
        assertEquals("0:00", MasterBuildersManager.formatTime(0L));
        assertEquals("0:01", MasterBuildersManager.formatTime(1L));
        assertEquals("1:01", MasterBuildersManager.formatTime(60_001L));
    }

    @Test
    void emptySessionIsSafeWithoutBukkitScoreboard() {
        MasterBuildersManager builders = new MasterBuildersManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.MASTER_BUILDERS, null, List.of(), false);

        builders.updateScoreboard(session, 5L, true, false);
        builders.cleanupScoreboard(session);
        builders.updateScoreboard(null, 5L, false, true);

        assertTrue(session.masterBuildersScoreboardEntries.isEmpty());
    }
}
