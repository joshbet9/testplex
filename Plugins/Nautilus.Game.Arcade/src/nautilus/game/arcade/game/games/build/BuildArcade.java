package nautilus.game.arcade.game.games.build;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.game.modules.compass.CompassModule;

/**
 * Shorter Mixed Arcade format of Master Builders.
 */
public class BuildArcade extends Build
{
	public BuildArcade(ArcadeManager manager)
	{
		super(manager, GameType.BuildArcade);
		_buildTime = 180000L;

		new CompassModule()
				.setGiveCompass(true)
				.setGiveCompassToSpecs(true)
				.setGiveCompassToAlive(false)
				.register(this);
	}
}
