package nautilus.game.arcade.game.games.baconbrawl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import mineplex.core.common.util.C;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.common.util.UtilServer;
import mineplex.core.common.util.UtilTime;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.damage.CustomDamageEvent;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.SoloGame;
import nautilus.game.arcade.game.games.baconbrawl.kits.KitChrisPBacon;
import nautilus.game.arcade.game.games.baconbrawl.kits.KitMamaPig;
import nautilus.game.arcade.game.games.baconbrawl.kits.KitPig;
import nautilus.game.arcade.game.games.baconbrawl.kits.KitSheepPig;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.stats.KillsWithinGameStatTracker;

public class BaconBrawl extends SoloGame
{

	private static final String[] DESCRIPTION =
			{
					"Knock other pigs out of the arena!",
					"Last pig in the arena wins!",
					"Knockback increases over time!"
			};

	private int _cosmeticAccessoryTick;
	private final Map<UUID, ArmorStand> _cosmeticAccessories = new HashMap<>();
	private final Map<UUID, String> _cosmeticAccessoryChoices = new HashMap<>();
	private double _knockbackMagnitude = 1.5;
	private long _increaseLast, _increaseTime = TimeUnit.SECONDS.toMillis(50);

	public BaconBrawl(ArcadeManager manager)
	{
		super(manager, GameType.BaconBrawl, new Kit[]
				{
						new KitPig(manager),
						new KitMamaPig(manager),
						new KitSheepPig(manager),
						new KitChrisPBacon(manager)
				}, DESCRIPTION);

		DamageTeamSelf = true;
		PrepareFreeze = false;
		StrictAntiHack = true;
		PlayerGameMode = GameMode.ADVENTURE;

		registerChatStats(
				Kills,
				Deaths,
				KDRatio,
				BlankLine,
				Assists,
				DamageDealt,
				DamageTaken
		);

		registerStatTrackers(
				new KillsWithinGameStatTracker(this, 6, "KillsInGame")
		);

		new CompassModule()
				.register(this);
	}

	@EventHandler
	public void live(GameStateChangeEvent event)
	{
		if (event.GetState() != GameState.Live)
		{
			return;
		}

		_increaseLast = System.currentTimeMillis();
	}

	@EventHandler
	public void update(UpdateEvent event)
	{
		if (event.getType() != UpdateType.SLOW || !IsLive())
		{
			return;
		}

		for (Player player : GetPlayers(true))
		{
			if (player.getFoodLevel() <= 0)
			{
				Manager.GetDamage().NewDamageEvent(player, null, null, DamageCause.STARVATION, 4, false, true, false, GetName(), "Starvation");
			}

			UtilPlayer.hunger(player, -1);
		}

		if (_increaseTime > 0 && UtilTime.elapsed(_increaseLast, _increaseTime))
		{
			_increaseLast = System.currentTimeMillis();
			_increaseTime -= TimeUnit.SECONDS.toMillis(5);
			_knockbackMagnitude *= 1.2;

			String message = C.cGreenB + "The ground begins to tremble... All pigs now take more knockback!";

			for (Player player : UtilServer.getPlayersCollection())
			{
				player.sendMessage(message);
				player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1, 0.5F);
			}
		}
	}

	/**
	 * Render each equipped Bacon Brawl accessory above its owner's pig or sheep
	 * disguise. The invisible, marker armor stand has no gameplay collision; only
	 * its helmet slot is visible to every client. No choice means the stock look.
	 */
	@EventHandler
	public void cosmeticAccessories(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;

		if (!IsLive())
		{
			removeAllCosmeticAccessories();
			return;
		}

		// 10 updates/second is smooth enough for a head accessory and keeps entity
		// teleport traffic low on the 1.8 server.
		if (++_cosmeticAccessoryTick % 2 != 0)
			return;

		Set<UUID> active = new HashSet<>();
		for (Player player : GetPlayers(true))
		{
			UUID id = player.getUniqueId();
			String choice = ArcadeCosmeticsMenu.getEquippedChoice(id, "baconbrawl");
			ItemStack accessory = getCosmeticAccessory(choice);
			if (accessory == null)
				continue;

			active.add(id);
			ArmorStand stand = _cosmeticAccessories.get(id);
			if (stand == null || stand.isDead() || !stand.isValid())
			{
				removeCosmeticAccessory(id);
				Location spawn = player.getLocation().clone().subtract(0, 0.18, 0);
				stand = spawn.getWorld().spawn(spawn, ArmorStand.class);
				stand.setVisible(false);
				stand.setSmall(true);
				stand.setGravity(false);
				stand.setBasePlate(false);
				stand.setArms(false);
				stand.setMarker(true);
				stand.setCanPickupItems(false);
				_cosmeticAccessories.put(id, stand);
				_cosmeticAccessoryChoices.remove(id);
			}

			if (!choice.equals(_cosmeticAccessoryChoices.get(id)))
			{
				stand.setHelmet(accessory);
				_cosmeticAccessoryChoices.put(id, choice);
			}

			Location target = player.getLocation().clone().subtract(0, 0.18, 0);
			if (!stand.getWorld().equals(target.getWorld()) || stand.getLocation().distanceSquared(target) > 0.0001
					|| Math.abs(stand.getLocation().getYaw() - target.getYaw()) > 1.0F)
			{
				stand.teleport(target);
			}
		}

		for (UUID id : new HashSet<>(_cosmeticAccessories.keySet()))
		{
			if (!active.contains(id))
				removeCosmeticAccessory(id);
		}
	}

	@EventHandler
	public void cleanupCosmeticAccessories(GameStateChangeEvent event)
	{
		if (event.GetGame() != this)
			return;

		if (event.GetState() == GameState.End || event.GetState() == GameState.Dead)
			removeAllCosmeticAccessories();
	}

	private ItemStack getCosmeticAccessory(String choice)
	{
		if (choice == null)
			return null;

		switch (choice.toLowerCase())
		{
			case "crown":
				return new ItemStack(Material.GOLD_HELMET);
			case "top_hat":
				// A black wool block is a visibly distinct top-hat silhouette in the
				// vanilla 1.8 model set and needs no client resource pack.
				return new ItemStack(Material.WOOL, 1, (short) 15);
			case "chef":
				return new ItemStack(Material.WOOL, 1, (short) 0);
			case "diamond":
				return new ItemStack(Material.DIAMOND_HELMET);
			default:
				return null;
		}
	}

	private void removeCosmeticAccessory(UUID id)
	{
		ArmorStand stand = _cosmeticAccessories.remove(id);
		_cosmeticAccessoryChoices.remove(id);
		if (stand != null && !stand.isDead())
			stand.remove();
	}

	private void removeAllCosmeticAccessories()
	{
		for (UUID id : new HashSet<>(_cosmeticAccessories.keySet()))
			removeCosmeticAccessory(id);
	}

	@EventHandler
	public void hungerRestore(CustomDamageEvent event)
	{
		Player damager = event.GetDamagerPlayer(true);

		if (damager != null)
		{
			UtilPlayer.hunger(damager, 2);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void knockback(CustomDamageEvent event)
	{
		if (!IsLive() || event.GetDamageePlayer() == null)
		{
			return;
		}

		DamageCause cause = event.GetCause();

		if (cause == DamageCause.FIRE_TICK)
		{
			event.SetCancelled("Fire Damage");
		}
		else if (cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.CUSTOM || cause == DamageCause.PROJECTILE)
		{
			UtilPlayer.health(event.GetDamageePlayer(), event.GetDamage());
			event.AddKnockback("Pig Wrestle", _knockbackMagnitude);
		}
	}
}
