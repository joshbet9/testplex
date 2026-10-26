package nautilus.game.arcade.game.games.monstermaze;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;

/**
 * Enhanced Monster Maze variant. It intentionally reuses Reborn's native
 * SoloGame lifecycle, map data, Maze implementation, score tracking, and kit
 * infrastructure; the original GameType.MonsterMaze remains unchanged.
 */
public class MonsterMazeModern extends MonsterMaze
{
    public MonsterMazeModern(ArcadeManager manager)
    {
        super(manager, GameType.MonsterMazeModern, true);
    }
}
