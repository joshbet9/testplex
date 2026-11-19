package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcadeLobbyLifecycleRulesTest {
    @Test void recruitCountdownUsesLegacyFullnessThresholds() {
        assertEquals(-1, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(0, 1, 16));
        assertEquals(20, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(1, 1, 16));
        assertEquals(20, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(11, 1, 16));
        assertEquals(30, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(12, 1, 16));
        assertEquals(30, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(15, 1, 16));
        assertEquals(10, ArcadeLobbyLifecycleRules.recruitCountdownSeconds(16, 1, 16));
    }
    @Test void gameplayInputIsBlockedUntilLiveBegins() {
        assertEquals(false, ArcadeLobbyLifecycleRules.gameplayInputAllowed(false));
        assertEquals(true, ArcadeLobbyLifecycleRules.gameplayInputAllowed(true));
    }

    @Test void onlyMonsterMazeAllowsWalkingDuringItsOwnStartCountdown() {
        assertEquals(true, ArcadeLobbyLifecycleRules.allowPrepareMovement(ArcadePortGame.MONSTER_MAZE, "STARTING"));
        assertEquals(true, ArcadeLobbyLifecycleRules.allowPrepareMovement(ArcadePortGame.MONSTER_MAZE_MODERN, "STARTING"));
        assertEquals(false, ArcadeLobbyLifecycleRules.allowPrepareMovement(ArcadePortGame.MONSTER_MAZE, "LIVE"));
        assertEquals(false, ArcadeLobbyLifecycleRules.allowPrepareMovement(ArcadePortGame.MONSTER_MAZE, "ENDING"));
        assertEquals(false, ArcadeLobbyLifecycleRules.allowPrepareMovement(ArcadePortGame.DRAGONS, "STARTING"));
        assertEquals(false, ArcadeLobbyLifecycleRules.allowPrepareMovement(null, "STARTING"));
    }

    @Test void preparedTimeIsExcludedFromGameDeadlinesAndCooldowns() {
        assertEquals(15_000L, ArcadeLobbyLifecycleRules.shiftPausedTimestamp(5_000L, 10_000L));
        assertEquals(0L, ArcadeLobbyLifecycleRules.shiftPausedTimestamp(0L, 10_000L));
        assertEquals(-1L, ArcadeLobbyLifecycleRules.shiftPausedTimestamp(-1L, 10_000L));
        assertEquals(Long.MAX_VALUE,
                ArcadeLobbyLifecycleRules.shiftPausedTimestamp(Long.MAX_VALUE - 1L, 10_000L));
        assertEquals(5_000L, ArcadeLobbyLifecycleRules.shiftPausedTimestamp(5_000L, 0L));
    }

    @Test void phaseDurationsAreBounded() {
        assertEquals(3, ArcadeLobbyLifecycleRules.DEFAULT_PREPARE_SECONDS);
        assertEquals(3, ArcadeLobbyLifecycleRules.prepareSeconds(-1));
        assertEquals(10, ArcadeLobbyLifecycleRules.prepareSeconds(10));
        assertEquals(30, ArcadeLobbyLifecycleRules.prepareSeconds(100));
        assertEquals(0, ArcadeLobbyLifecycleRules.endPresentationSeconds(-1));
        assertEquals(10, ArcadeLobbyLifecycleRules.endPresentationSeconds(10));
        assertEquals(30, ArcadeLobbyLifecycleRules.endPresentationSeconds(100));
        assertEquals("Starting in 10 Seconds", ArcadeLobbyLifecycleRules.countdownStatus(10));
    }

    @Test void lobbyReturnUsesPersistedSelectionAndFallsBackWhenItIsInvalid() {
        assertEquals(ArcadePortGame.DRAGON_ESCAPE,
                ArcadeLobbyLifecycleRules.selectedGameAfterReturn("dragon-escape", ArcadePortGame.BACON_BRAWL));
        assertEquals(ArcadePortGame.DRAGON_ESCAPE,
                ArcadeLobbyLifecycleRules.selectedGameAfterReturn("dragon-escape", ArcadePortGame.DRAGON_ESCAPE));
        assertEquals(ArcadePortGame.BACON_BRAWL,
                ArcadeLobbyLifecycleRules.selectedGameAfterReturn("unknown-game", ArcadePortGame.BACON_BRAWL));
        assertEquals(ArcadePortGame.SHEEP_QUEST,
                ArcadeLobbyLifecycleRules.selectedGameAfterReturn(null, ArcadePortGame.SHEEP_QUEST));
    }

    @Test void sameTickLobbyReturnsScheduleOnlyOneFullSelectionRefresh() {
        assertEquals(true, ArcadeLobbyLifecycleRules.shouldScheduleLobbyReturnRefresh(false));
        assertEquals(false, ArcadeLobbyLifecycleRules.shouldScheduleLobbyReturnRefresh(true));
    }

}
