package nautilus.game.arcade.gui.privateServer.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import mineplex.core.shop.item.ShopItem;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.gui.privateServer.PrivateServerShop;
import nautilus.game.arcade.gui.privateServer.button.ChooseMapButton;

public class ChooseMapPage extends BasePage
{
	private static final int MAPS_PER_PAGE = 36;

	private final GameType _gameType;
	private final List<MapChoice> _maps = new ArrayList<>();
	private int _page;

	public ChooseMapPage(ArcadeManager plugin, PrivateServerShop shop, Player player, GameType gameType)
	{
		super(plugin, shop, "Choose Map", player);
		_gameType = gameType;

		for (GameType source : Game.getWorldHostNames(_gameType))
		{
			for (String map : getPlugin().LoadFiles(source.getName()))
			{
				_maps.add(new MapChoice(source, map));
			}
		}

		_maps.sort(Comparator.comparing((MapChoice choice) -> choice.map.toLowerCase())
				.thenComparing(choice -> choice.source.getName().toLowerCase()));
		buildPage();
	}

	@Override
	protected void buildPage()
	{
		addBackToSetGamePage();

		int start = _page * MAPS_PER_PAGE;
		int end = Math.min(_maps.size(), start + MAPS_PER_PAGE);
		int slot = 9;

		for (int i = start; i < end; i++)
		{
			MapChoice choice = _maps.get(i);
			String name = choice.map.contains("_") ? choice.map.substring(choice.map.indexOf('_') + 1) : choice.map;
			String[] lore = {
					"§7Source: " + choice.source.getName(),
					"§7Left-click to select this map."
			};
			ChooseMapButton button = new ChooseMapButton(getPlugin(), getShop(), _gameType, choice.source, choice.map);
			addButton(slot++, new ShopItem(Material.PAPER, name, lore, 1, false), button);
		}

		if (_maps.isEmpty())
		{
			String reason = _gameType == GameType.UHC
					? "This game generates its map automatically."
					: "No map ZIPs were found in the installed Mineplex map folders.";
			addItem(22, new ShopItem(Material.BARRIER, "No Maps Installed", new String[] { "§7" + reason }, 1, false));
		}

		if (_page > 0)
		{
			addButton(45, PREVIOUS_PAGE, (player, clickType) ->
			{
				_page--;
				refresh();
			});
		}

		if (end < _maps.size())
		{
			addButton(53, NEXT_PAGE, (player, clickType) ->
			{
				_page++;
				refresh();
			});
		}
	}

	private static final class MapChoice
	{
		private final GameType source;
		private final String map;

		private MapChoice(GameType source, String map)
		{
			this.source = source;
			this.map = map;
		}
	}
}
