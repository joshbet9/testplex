package nautilus.game.arcade.gui.privateServer.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import mineplex.core.common.util.C;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.game.Game.GameState;
import mineplex.core.shop.item.IButton;

public class StartGameButton implements IButton
{
	private final ArcadeManager _arcadeManager;

	public StartGameButton(ArcadeManager arcadeManager)
	{
		_arcadeManager = arcadeManager;
	}

	@Override
	public void onClick(Player player, ClickType clickType)
	{
		Game game = _arcadeManager.GetGame();
		if (game == null)
		{
			player.sendMessage(C.cRed + "No game is loaded. Select a game in the Game Panel first.");
			return;
		}

		if (game.GetState() != GameState.Recruit)
		{
			player.sendMessage(C.cRed + "The game is not in the recruiting lobby and cannot be started manually.");
			return;
		}

		_arcadeManager.GetGameManager().StateCountdown(game, 20, true);
		game.Announce(C.cAqua + C.Bold + player.getName() + " has started the game.");
	}
}
