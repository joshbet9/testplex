package nautilus.game.arcade.kit.perks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.common.util.UtilAction;
import mineplex.core.common.util.UtilBlock;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilGear;
import mineplex.core.common.util.UtilInv;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.itemstack.ItemStackFactory;
import mineplex.core.projectile.IThrown;
import mineplex.core.projectile.ProjectileUser;
import mineplex.core.recharge.Recharge;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.game.games.baconbrawl.BaconBrawlCosmeticEffects;
import nautilus.game.arcade.kit.Perk;

public class PerkBaconBlast extends Perk implements IThrown
{
	private final Map<Item, Player> _cosmeticItems = new HashMap<Item, Player>();
	public PerkBaconBlast() 
	{
		super("Bacon Blast", new String[] 
				{
				C.cYellow + "Right-Click" + C.cGray + " with Axe to use " + C.cGreen + "Bacon Blast"
				});
	}


	@EventHandler
	public void Shoot(PlayerInteractEvent event)
	{
		if (event.isCancelled())
			return;

		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		activate(event.getPlayer(), event);
	}

	/**
	 * Bukkit reports right-clicking an entity through a separate event rather than
	 * PlayerInteractEvent. Route both event types through the same activation path
	 * so the ability works on air, blocks and players without double-consuming charge.
	 */
	@EventHandler
	public void ShootEntity(PlayerInteractEntityEvent event)
	{
		if (event.isCancelled())
			return;

		activate(event.getPlayer(), event);
	}

	private void activate(Player player, Cancellable event)
	{
		if (!UtilGear.isAxe(player.getItemInHand()))
			return;

		if (!Kit.HasKit(player))
			return;

		if (!Recharge.Instance.use(player, GetName(), 3000, true, true))
			return;

		event.setCancelled(true);
		UtilInv.Update(player);

		org.bukkit.entity.Item ent = player.getWorld().dropItem(player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5)), ItemStackFactory.Instance.CreateStack(Material.PORK, (byte)0, 16));
		if (Manager.GetGame().GetType() == GameType.BaconBrawl)
		{
			_cosmeticItems.put(ent, player);
			BaconBrawlCosmeticEffects.play(player, ent.getLocation(), "bacon-blast-activation");
		}
		UtilAction.velocity(ent, player.getLocation().getDirection(), 1, false, 0, 0.2, 10, false);

		Manager.GetProjectile().AddThrow(ent, player, this, -1, true, true, true, true,
				null, 1f, 1f,
				null, 1, UpdateType.SLOW,
				0.5f);

		UtilPlayer.message(player, F.main("Game", "You used " + F.skill(GetName()) + "."));
		player.getWorld().playSound(player.getLocation(), Sound.PIG_IDLE, 2f, 2f);
	}

	@Override
	public void Collide(LivingEntity target, Block block, ProjectileUser data) 
	{
		Explode(data);

		if (target == null)
			return;

		//Damage Event
		Manager.GetDamage().NewDamageEvent(target, data.getThrower(), null,
				DamageCause.PROJECTILE, 6, true, true, false,
				UtilEnt.getName(data.getThrower()), GetName());
	}

	@Override
	public void Idle(ProjectileUser data) 
	{
		Explode(data);
	}

	@Override
	public void Expire(ProjectileUser data) 
	{
		Explode(data);
	}
	
	@Override
	public void ChunkUnload(ProjectileUser data)
	{
		if (data.getThrown() instanceof Item) _cosmeticItems.remove((Item) data.getThrown());
		data.getThrown().remove();
	}

	@EventHandler
	public void updateCosmeticTrail(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK || Manager.GetGame().GetType() != GameType.BaconBrawl) return;
		Iterator<Map.Entry<Item, Player>> iterator = _cosmeticItems.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<Item, Player> entry = iterator.next();
			Item item = entry.getKey(); Player owner = entry.getValue();
			if (item == null || !item.isValid() || owner == null || !owner.isOnline()) { iterator.remove(); continue; }
			BaconBrawlCosmeticEffects.play(owner, item.getLocation(), "bacon-blast-trail");
		}
	}

	public void Explode(ProjectileUser data)
	{
		if (data.getThrown() instanceof Item)
		{
			Item item = (Item) data.getThrown(); Player owner = _cosmeticItems.remove(item);
			if (owner != null && Manager.GetGame().GetType() == GameType.BaconBrawl)
				BaconBrawlCosmeticEffects.play(owner, item.getLocation(), "bacon-blast-impact");
		}
		// for whatever reason, you can't put a location in createExplosion if you don't want it to break blocks >.>
		Location loc = data.getThrown().getLocation();
		data.getThrown().getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0.5f, false, false);
		data.getThrown().remove();
	}
}