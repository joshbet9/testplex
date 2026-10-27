package nautilus.game.arcade.gui.privateServer.page;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import mineplex.core.common.util.C;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.common.util.UtilServer;
import mineplex.core.shop.item.ShopItem;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.game.Game;
import nautilus.game.arcade.gui.privateServer.PrivateServerShop;
import nautilus.game.arcade.managers.GameCreationManager;

public class SetGamePage extends BasePage
{
	private int _page;

	public SetGamePage(ArcadeManager plugin, PrivateServerShop shop, Player player)
	{
		super(plugin, shop, "Set Game", player);
		buildPage();
	}

	@Override
	protected void buildPage()
	{
		addBackButton(4);

		int gameSlot = 9;
		int gameIndex = _page * GAMES_PER_PAGE;
		List<GameType> types = new ArrayList<>(_manager.getGames());
		types = types.subList(gameIndex, Math.min(types.size(), (_page + 1) * GAMES_PER_PAGE));

		for (GameType type : types)
		{
			addGameButton(gameSlot++, type);
		}

		if (_page > 0)
		{
			addButton(45, PREVIOUS_PAGE, (player, clickType) ->
			{
				_page--;
				refresh();
			});
		}

		if (gameIndex + types.size() < _manager.getGames().size())
		{
			addButton(53, NEXT_PAGE, (player, clickType) ->
			{
				_page++;
				refresh();
			});
		}
	}

	private void addGameButton(int slot, final GameType type)
	{
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.RESET + C.cGray + "Select this as the next game.");
		lore.add("§1");
		if (type == GameType.UHC || type == GameType.UHCSolo || type == GameType.UHCSoloSpeed || type == GameType.UHCTeamsSpeed)
		{
			lore.add(ChatColor.YELLOW + "Left-Click " + C.cGray + "to select (map generated automatically).");
		}
		else
		{
			lore.add(ChatColor.YELLOW + "Left-Click " + C.cGray + "for a random installed map.");
			lore.add(ChatColor.YELLOW + "Right-Click " + C.cGray + "to choose an installed map.");
		}
		if (_manager.hasWarning().contains(type))
		{
			lore.add("§2");
			lore.add("§c§lWARNING: §fThis game was rejected!");
		}

		ShopItem shopItem = new ShopItem(type.GetMaterial(), type.GetMaterialData(), type.getName(), lore.toArray(new String[lore.size()]), 1, false, false);
		addButton(slot, shopItem, (player, clickType) ->
		{
			if (clickType == ClickType.LEFT)
			{
				boolean mapsAvailable = hasMapArchive(type);

				GameCreationManager creation = getPlugin().GetGameCreationManager();
				creation.MapPref = null;
				creation.MapSource = null;
				creation.ModePref = null;

				if (getPlugin().GetGame() == null)
				{
					creation.setNextGameType(type);
					if (UtilServer.isTestServer() || getPlugin().isTestplexSandbox())
					{
						getPlugin().GetGameList().clear();
						getPlugin().GetGameList().add(type);
					}
				}
				else
				{
					getPlugin().GetGame().setGame(type, player, true);
				}

				if (mapsAvailable)
				{
					UtilPlayer.message(player, C.cGreen + "Selected " + type.getName() + " with a random installed map.");
				}
				else
				{
					UtilPlayer.message(player, C.cYellow + "Selected " + type.getName()
							+ ", but no compatible map ZIPs were detected. It will not start until a map is available.");
				}
				player.closeInventory();
			}
			else if (clickType == ClickType.RIGHT)
			{
				if (type == GameType.UHC || type == GameType.UHCSolo || type == GameType.UHCSoloSpeed || type == GameType.UHCTeamsSpeed)
				{
					UtilPlayer.message(player, C.cGray + type.getName() + " creates its map automatically.");
					return;
				}
				getShop().openPageForPlayer(player, new ChooseMapPage(getPlugin(), getShop(), player, type));
			}
		});
	}

	private boolean hasMapArchive(GameType type)
	{
		if (type == GameType.UHC || type == GameType.UHCSolo || type == GameType.UHCSoloSpeed || type == GameType.UHCTeamsSpeed)
		{
			return true;
		}

		for (GameType source : Game.getWorldHostNames(type))
		{
			if (!getPlugin().LoadFiles(source.getName()).isEmpty())
			{
				return true;
			}
		}
		return false;
	}
}
