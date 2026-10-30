package nautilus.game.arcade.game.games.draw;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;

/**
 * Mixed Arcade format of Draw My Thing.
 *
 * Uses the original Draw implementation and maps, but has shorter rounds,
 * one turn per player, guess limits, and no resource-pack requirement.
 */
public class DrawArcade extends Draw
{
	public DrawArcade(ArcadeManager manager)
	{
		super(manager, GameType.DrawArcade);
	}
}
