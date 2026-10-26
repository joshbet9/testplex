package nautilus.game.arcade.game.games.monstermaze.kits;

import mineplex.core.game.kit.GameKit;
import nautilus.game.arcade.ArcadeManager;

/** Speed-mode Repulsor, bound to the Speed-specific kit registry entry. */
public class KitRepulsorSpeed extends KitRepulsorModern
{
    public KitRepulsorSpeed(ArcadeManager manager)
    {
        super(manager, GameKit.MONSTER_MAZE_SPEED_REPULSOR);
    }
}
