package nautilus.game.arcade.gui.privateServer.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import mineplex.core.common.util.C;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.game.Game.GameState;
import mineplex.core.shop.item.IButton;

public class StopGameButton implements IButton
{
	private final ArcadeManager _arcadeManager;

	public StopGameButton(ArcadeManager arcadeManager)
	{
		_arcadeManager = arcadeManager;
	}

	@Override
	public void onClick(Player player, ClickType clickType)
	{
		Game game = _arcadeManager.GetGame();
		if (game == null)
		{
			player.sendMessage(C.cRed + "No game is loaded to stop.");
			return;
		}

		if (game.GetState() == GameState.End || game.GetState() == GameState.Dead)
		{
			player.sendMessage(C.cRed + "The game is already ending.");
			return;
		}

		if (game.inLobby())
		{
			game.SetState(GameState.Dead);
		}
		else
		{
			game.SetState(GameState.End);
		}

		game.Announce(C.cAqua + C.Bold + player.getName() + " has stopped the game.");
	}
}
