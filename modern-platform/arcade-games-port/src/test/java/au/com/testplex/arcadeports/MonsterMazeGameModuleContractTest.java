package au.com.testplex.arcadeports;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.game.MazeMode;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonsterMazeGameModuleContractTest {
    @Test
    void monsterMazeUsesTheArcadeOwnedLifecycleInsteadOfAnIndependentBukkitPlugin() throws Exception {
        assertTrue(ArcadeGameModule.class.isAssignableFrom(MonsterMazeGameModuleAdapter.class));
        assertFalse(JavaPlugin.class.isAssignableFrom(MonsterMazePlugin.class));
        assertEquals(ArcadePortGame.class,
                MonsterMazeGameModuleAdapter.class.getDeclaredMethod("game").getReturnType());
        assertDoesNotThrow(() -> MonsterMazePlugin.class.getDeclaredMethod("initialize"));
        assertDoesNotThrow(() -> MonsterMazeGameModuleAdapter.class.getDeclaredMethod("games"));
        assertTrue(ArcadePortGame.MONSTER_MAZE.isMonsterMaze());
        assertTrue(ArcadePortGame.MONSTER_MAZE_SPEED.isMonsterMaze());
        assertTrue(ArcadePortGame.MONSTER_MAZE_MODERN.isMonsterMaze());
        assertFalse(ArcadePortGame.BACON_BRAWL.isMonsterMaze());
        assertDoesNotThrow(() -> MonsterMazePlugin.class.getDeclaredMethod(
                "startTestplexGame", java.util.List.class, MazeMode.class, ArcadePortGame.class));
        assertDoesNotThrow(() -> MonsterMazePlugin.class.getDeclaredMethod("shutdown"));
        assertThrows(NoSuchMethodException.class,
                () -> MonsterMazePlugin.class.getDeclaredMethod("enable"));
    }
}
