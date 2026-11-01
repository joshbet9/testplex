package au.com.testplex.arcadeports;

import me.monstermaze.MonsterMazePlugin;

import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts Monster Maze's preserved gameplay engine to the shared Arcade module lifecycle.
 * This is an in-process controller adapter, not a Bukkit plugin.
 */
final class MonsterMazeGameModuleAdapter implements ArcadeGameModule {
    private final MonsterMazePlugin monsterMaze;

    MonsterMazeGameModuleAdapter(MonsterMazePlugin monsterMaze) {
        this.monsterMaze = Objects.requireNonNull(monsterMaze, "monsterMaze");
    }

    @Override
    public ArcadePortGame game() {
        return ArcadePortGame.MONSTER_MAZE;
    }

    @Override
    public Set<ArcadePortGame> games() {
        return new LinkedHashSet<>(java.util.List.of(
                ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED,
                ArcadePortGame.MONSTER_MAZE_MODERN));
    }

    @Override
    public void initialize() {
        monsterMaze.initialize();
        if (monsterMaze.getGameManager() == null || monsterMaze.getMapManager() == null) {
            throw new IllegalStateException("Monster Maze did not create its gameplay and map managers.");
        }
    }

    @Override
    public void shutdown() {
        monsterMaze.shutdown();
    }

    @Override
    public boolean isGameRunning() {
        return monsterMaze.isGameRunning();
    }
}
