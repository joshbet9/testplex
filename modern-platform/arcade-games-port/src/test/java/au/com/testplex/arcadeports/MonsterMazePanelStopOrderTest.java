package au.com.testplex.arcadeports;

import me.monstermaze.game.GameState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonsterMazePanelStopOrderTest {
    @Test
    void stopsEachMonsterMazeModeBeforeRestoringParticipants() {
        for (ArcadePortGame mode : new ArcadePortGame[] {
                ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED,
                ArcadePortGame.MONSTER_MAZE_MODERN
        }) {
            assertTrue(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(mode, GameState.STARTING));
            assertTrue(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(mode, GameState.LIVE));
            assertTrue(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(mode, GameState.ENDING));
            assertFalse(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(mode, GameState.IDLE));
        }
    }

    @Test
    void doesNotStopAnIdleOrUnrelatedEndingModule() {
        assertFalse(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(null, GameState.ENDING));
        assertFalse(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(ArcadePortGame.SPLEEF, GameState.ENDING));
        assertFalse(ArcadePortManager.shouldStopMonsterMazeBeforeRestore(null, null));
    }
}
