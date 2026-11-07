package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MonsterMazeStartupTeleportRulesTest {
    @Test
    void allowsRegisteredMazeModesToEnterTheirArenaDuringStartup() {
        for (ArcadePortGame type : new ArcadePortGame[] {
                ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED,
                ArcadePortGame.MONSTER_MAZE_MODERN
        }) {
            assertTrue(ArcadePortManager.allowsMonsterMazeStartupTeleport(type, "STARTING", true), type.id());
        }
    }

    @Test
    void blocksStartupTeleportsOutsideTheMazeArenaAndAfterStartup() {
        assertFalse(ArcadePortManager.allowsMonsterMazeStartupTeleport(
                ArcadePortGame.MONSTER_MAZE, "STARTING", false));
        assertFalse(ArcadePortManager.allowsMonsterMazeStartupTeleport(
                ArcadePortGame.MONSTER_MAZE, "LIVE", true));
        assertFalse(ArcadePortManager.allowsMonsterMazeStartupTeleport(
                ArcadePortGame.DEATH_TAG, "STARTING", true));
        assertFalse(ArcadePortManager.allowsMonsterMazeStartupTeleport(null, "STARTING", true));
    }
}
