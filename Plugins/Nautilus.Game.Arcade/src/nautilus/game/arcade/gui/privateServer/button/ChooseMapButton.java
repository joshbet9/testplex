package nautilus.game.arcade.gui.privateServer.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import mineplex.core.common.util.C;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.common.util.UtilServer;
import mineplex.core.shop.item.IButton;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.gui.privateServer.PrivateServerShop;
import nautilus.game.arcade.managers.GameCreationManager;

public class ChooseMapButton implements IButton
{
	private final ArcadeManager _arcadeManager;
	private final GameType _gameType;
	private final GameType _mapSource;
	private final String _map;

	public ChooseMapButton(ArcadeManager arcadeManager, PrivateServerShop privateServerShop, GameType gameType, GameType mapSource, String map)
	{
		_arcadeManager = arcadeManager;
		_gameType = gameType;
		_mapSource = mapSource;
		_map = map;
	}

	@Override
	public void onClick(Player player, ClickType clickType)
	{
		GameCreationManager creation = _arcadeManager.GetGameCreationManager();
		creation.MapSource = _mapSource;
		creation.MapPref = _map.replaceAll(" ", "");
		creation.ModePref = null;

		if (_arcadeManager.GetGame() == null)
		{
			creation.setNextGameType(_gameType);
			if (UtilServer.isTestServer())
			{
				_arcadeManager.GetGameList().clear();
				_arcadeManager.GetGameList().add(_gameType);
			}
		}
		else
		{
			_arcadeManager.GetGame().setGame(_gameType, player, true);
		}

		UtilPlayer.message(player, C.cGreen + "Selected " + _gameType.getName() + " on map " + _map + ".");
		player.closeInventory();
	}
}
