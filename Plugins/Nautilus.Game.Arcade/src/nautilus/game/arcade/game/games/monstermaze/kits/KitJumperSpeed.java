package nautilus.game.arcade.game.games.monstermaze.kits;

import mineplex.core.game.kit.GameKit;
import nautilus.game.arcade.ArcadeManager;

/** Speed-mode Jumper, preserving the native Speed game kit identity. */
public class KitJumperSpeed extends KitJumperModern
{
    public KitJumperSpeed(ArcadeManager manager)
    {
        super(manager, GameKit.MONSTER_MAZE_SPEED_JUMPER);
    }
}
