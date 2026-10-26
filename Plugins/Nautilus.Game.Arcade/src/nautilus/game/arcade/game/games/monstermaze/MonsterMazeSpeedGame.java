package nautilus.game.arcade.game.games.monstermaze;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;

/** Speed rules: enhanced mechanics and kits, with original timing and monster totals. */
public class MonsterMazeSpeedGame extends MonsterMaze
{
    public MonsterMazeSpeedGame(ArcadeManager manager)
    {
        super(manager, GameType.MonsterMazeSpeed, MonsterMazeMode.SPEED);
    }
}
