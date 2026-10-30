package nautilus.game.arcade.game.games.dragons.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import mineplex.core.common.util.F;
import mineplex.core.common.util.UtilInv;
import mineplex.core.itemstack.ItemStackFactory;

import nautilus.game.arcade.game.games.dragons.Dragons;
import nautilus.game.arcade.kit.Perk;

/**
 * Defensive counterpart to the Pyrotechnic Sparkler.
 *
 * A flare is a finite kit item. On use it creates a short-lived exclusion area;
 * Dragons recalculates any current target in that area and will not choose players
 * or Pyrotechnic sparkler lures inside it until the exclusion expires.
 */
public class PerkFlare extends Perk
{
	private static final Material FLARE_MATERIAL = Material.FIREWORK;

	private final int _flareCount;

	public PerkFlare(int flareCount)
	{
		super("Dragon Flare", new String[]
				{
						"Receive " + Math.max(0, flareCount) + " Dragon Flares for this round.",
						"Right-click to divert dragons away from you and your surrounding area."
				});

		_flareCount = Math.max(0, flareCount);
	}

	@Override
	public void Apply(Player player)
	{
		if (_flareCount <= 0)
		{
			return;
		}

		player.getInventory().addItem(ItemStackFactory.Instance.CreateStack(
				FLARE_MATERIAL, (byte) 0, _flareCount, F.item("Dragon Flare")));
	}

	@EventHandler
	public void useFlare(PlayerInteractEvent event)
	{
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}

		Player player = event.getPlayer();
		if (!UtilInv.IsItem(player.getItemInHand(), FLARE_MATERIAL, (byte) 0))
		{
			return;
		}

		// Do not allow the custom firework item to be launched by vanilla behaviour,
		// even if a player right-clicks it outside the live round.
		event.setCancelled(true);

		if (!Kit.HasKit(player) || Manager.GetGame() == null
				|| !Manager.GetGame().IsLive() || !Manager.GetGame().IsAlive(player)
				|| !(Manager.GetGame() instanceof Dragons))
		{
			return;
		}

		UtilInv.remove(player, FLARE_MATERIAL, (byte) 0, 1);
		UtilInv.Update(player);
		((Dragons) Manager.GetGame()).activateFlare(player);
	}

	@EventHandler
	public void preventDrop(PlayerDropItemEvent event)
	{
		if (UtilInv.IsItem(event.getItemDrop().getItemStack(), FLARE_MATERIAL, (byte) 0))
		{
			event.setCancelled(true);
			event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You cannot drop Dragon Flares.");
		}
	}

	@EventHandler
	public void preventInventoryMove(InventoryClickEvent event)
	{
		UtilInv.DisallowMovementOf(event, "Dragon Flare", FLARE_MATERIAL, (byte) 0, true);
	}

	@EventHandler
	public void removeOnDeath(PlayerDeathEvent event)
	{
		event.getDrops().removeIf(item -> UtilInv.IsItem(item, FLARE_MATERIAL, (byte) 0));
	}
}
