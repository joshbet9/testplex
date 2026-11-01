package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeTestPlayerManagerTest {
    @Test
    void fillsEachGameToSixteenTotalPlayersRatherThanAddingSixteenExtraBots() {
        assertEquals(16, ArcadeTestPlayerManager.additionalPlayersNeeded(0));
        assertEquals(15, ArcadeTestPlayerManager.additionalPlayersNeeded(1));
        assertEquals(14, ArcadeTestPlayerManager.additionalPlayersNeeded(2));
        assertEquals(8, ArcadeTestPlayerManager.additionalPlayersNeeded(8));
        assertEquals(1, ArcadeTestPlayerManager.additionalPlayersNeeded(15));
        assertEquals(0, ArcadeTestPlayerManager.additionalPlayersNeeded(16));
        assertEquals(0, ArcadeTestPlayerManager.additionalPlayersNeeded(20));
        assertEquals(16, ArcadeTestPlayerManager.additionalPlayersNeeded(-3));
    }

    @Test
    void rejectsHumanQueuesLargerThanTheSixteenPlayerFixtureRoster() {
        assertTrue(ArcadeTestPlayerManager.canFitHumanPlayers(0));
        assertTrue(ArcadeTestPlayerManager.canFitHumanPlayers(16));
        assertFalse(ArcadeTestPlayerManager.canFitHumanPlayers(17));
        assertFalse(ArcadeTestPlayerManager.canFitHumanPlayers(20));
    }

    @Test
    void teardownWaitsForTheFppRespawnHandOffBeforeDespawn() {
        assertEquals(ArcadeTestPlayerManager.CleanupAction.WAIT_FOR_RESPAWN,
                ArcadeTestPlayerManager.cleanupAction(true, true, false, true, false, false));
        assertEquals(ArcadeTestPlayerManager.CleanupAction.WAIT_FOR_ENTITY,
                ArcadeTestPlayerManager.cleanupAction(true, false, false, false, false, false));
    }

    @Test
    void teardownDespawnsOnlyAfterRespawnHasFinishedAndAnEntityIsAvailable() {
        assertEquals(ArcadeTestPlayerManager.CleanupAction.DESPAWN,
                ArcadeTestPlayerManager.cleanupAction(true, false, true, true, false, false));
    }

    @Test
    void teardownWaitsForBukkitDisconnectAfterFppAcceptsDespawn() {
        assertEquals(ArcadeTestPlayerManager.CleanupAction.WAIT_FOR_DISCONNECT,
                ArcadeTestPlayerManager.cleanupAction(false, false, false, true, true, false));
        assertEquals(ArcadeTestPlayerManager.CleanupAction.COMPLETE,
                ArcadeTestPlayerManager.cleanupAction(false, false, false, false, true, false));
    }

    @Test
    void persistentProgressionExcludesTestPlayersAndMissingIds() {
        assertTrue(ArcadeTestPlayerManager.shouldPersistProgression(UUID.randomUUID(), false));
        assertFalse(ArcadeTestPlayerManager.shouldPersistProgression(UUID.randomUUID(), true));
        assertFalse(ArcadeTestPlayerManager.shouldPersistProgression(null, false));
        assertFalse(ArcadeTestPlayerManager.shouldPersistProgression(null, true));
    }

    @Test
    void teardownKicksOrphanedPlayersAndTimesOutStuckRespawns() {
        assertEquals(ArcadeTestPlayerManager.CleanupAction.KICK_ORPHAN,
                ArcadeTestPlayerManager.cleanupAction(false, false, false, true, false, false));
        assertEquals(ArcadeTestPlayerManager.CleanupAction.KICK_ORPHAN,
                ArcadeTestPlayerManager.cleanupAction(true, true, false, true, false, true));
    }
}
