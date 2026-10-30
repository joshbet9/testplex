package nautilus.game.arcade.game.games.dragons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

import mineplex.core.Managers;
import mineplex.core.common.util.UtilAction;
import mineplex.core.common.util.UtilAlg;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilFirework;
import mineplex.core.common.util.UtilTime;
import mineplex.core.recharge.Recharge;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.damage.CustomDamageEvent;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.events.PlayerDeathOutEvent;
import nautilus.game.arcade.game.SoloGame;
import nautilus.game.arcade.game.games.dragons.kits.KitCoward;
import nautilus.game.arcade.game.games.dragons.kits.KitMarksman;
import nautilus.game.arcade.game.games.dragons.kits.KitPyrotechnic;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.kit.Perk;
import nautilus.game.arcade.kit.perks.PerkSparkler;
import nautilus.game.arcade.managers.chat.ChatStatData;
import nautilus.game.arcade.stats.SparklezStatTracker;

public class Dragons extends SoloGame
{

	private static final String[] DESCRIPTION =
			{
					"You have angered the Dragons!",
					"Survive as best you can!!!",
					"Last player alive wins!"
			};
	private static final int MAX_DRAGONS = 5;
	private static final double FLARE_EXCLUSION_RADIUS = 24.0D;
	private static final double FLARE_EXCLUSION_RADIUS_SQUARED = FLARE_EXCLUSION_RADIUS * FLARE_EXCLUSION_RADIUS;
	private static final double FLARE_EXCLUSION_VERTICAL_RANGE = 24.0D;
	private static final long FLARE_EXCLUSION_DURATION_MS = 6000L;

	private final Set<MineplexDragon> _dragons;
	private final List<FlareZone> _flareZones = new ArrayList<>();

	private List<Location> _dragonSpawns;
	private PerkSparkler _sparkler;
	private int _pyroCosmeticTick;

	private static final class FlareZone
	{
		private final Location _center;
		private final long _expiresAt;

		private FlareZone(Location center, long expiresAt)
		{
			_center = center.clone();
			_expiresAt = expiresAt;
		}
	}

	public Dragons(ArcadeManager manager)
	{
		this(manager, GameType.Dragons, DESCRIPTION);
	}

	public Dragons(ArcadeManager manager, GameType gameType, String[] description)
	{
		super(manager, gameType, new Kit[]
						{
								new KitCoward(manager),
								new KitMarksman(manager),
								new KitPyrotechnic(manager)
						}, description);

		_dragons = new HashSet<>(MAX_DRAGONS);

		DamagePvP = false;
		HungerSet = 20;
		WorldWaterDamage = 4;
		PlayerGameMode = GameMode.ADVENTURE;

		registerStatTrackers(
				new SparklezStatTracker(this)
		);

		registerChatStats(
				DamageDealt,
				DamageTaken,
				BlankLine,
				new ChatStatData("kit", "Kit", true)
		);

		new CompassModule()
				.register(this);
	}

	@Override
	public void ParseData()
	{
		_dragonSpawns = WorldData.GetDataLocs("RED");

		if (_sparkler == null)
		{
			for (Kit kit : GetKits())
			{
				for (Perk perk : kit.GetPerks())
				{
					if (perk instanceof PerkSparkler)
					{
						_sparkler = (PerkSparkler) perk;
					}
				}
			}
		}
	}

	/** Visual-only particles from the player's equipped Arcade Pyro cosmetic. */
	@EventHandler
	public void updatePyroCosmetic(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK || !IsLive() || ++_pyroCosmeticTick % 4 != 0) return;
		for (Player player : GetPlayers(true))
		{
			String choice = ArcadeCosmeticsMenu.getEquippedChoice(player.getUniqueId(), "dragons", "pyro-particles");
			if (choice == null || choice.isEmpty() || "classic".equalsIgnoreCase(choice)) continue;
			ParticleType particle;
			switch (choice.toLowerCase(java.util.Locale.ROOT))
			{
				case "ember": particle = ParticleType.FLAME; break;
				case "frost": particle = ParticleType.SNOW_SHOVEL; break;
				case "ender": particle = ParticleType.PORTAL; break;
				case "spark": particle = ParticleType.FIREWORKS_SPARK; break;
				case "aurora": particle = ParticleType.WITCH_MAGIC; break;
				default: continue;
			}
			UtilParticle.PlayParticle(particle, player.getLocation().clone().add(0, 0.25, 0), 0.12F, 0.18F, 0.12F, 0.01F, 3, ViewDist.NORMAL);
		}
	}

	/**
	 * A flare temporarily makes its launch point an unattractive destination to every dragon.
	 * This is deliberately the inverse of the Pyrotechnic sparkler: it diverts target selection
	 * away from the player and the surrounding area instead of attracting dragons to a point.
	 */
	public void activateFlare(Player player)
	{
		if (player == null || !IsLive() || !IsAlive(player))
		{
			return;
		}

		Location center = player.getLocation().clone();
		_flareZones.add(new FlareZone(center, System.currentTimeMillis() + FLARE_EXCLUSION_DURATION_MS));

		// Recalculate existing dragon targets immediately, not just on their next normal pass.
		for (MineplexDragon dragon : _dragons)
		{
			dragon.retargetIfInsideFlare();
		}

		String choice = ArcadeCosmeticsMenu.getEquippedChoice(player.getUniqueId(), "dragons", "flare-effects");
		playFlareEffect(center, choice);
		player.sendMessage(ChatColor.GREEN + "You launched a flare! Dragons are diverted from this area.");
	}

	/**
	 * Returns whether a player, target point, or Pyrotechnic sparkler is inside an active
	 * flare exclusion area. The exclusion is horizontal so a dragon's flight height does
	 * not bypass the flare.
	 */
	boolean isInFlareArea(Location location)
	{
		if (location == null || location.getWorld() == null)
		{
			return false;
		}

		long now = System.currentTimeMillis();
		_flareZones.removeIf(zone -> zone._expiresAt <= now);

		for (FlareZone zone : _flareZones)
		{
			if (zone._center.getWorld() != location.getWorld())
			{
				continue;
			}

			double dx = location.getX() - zone._center.getX();
			double dz = location.getZ() - zone._center.getZ();
			double dy = Math.abs(location.getY() - zone._center.getY());

			if (dx * dx + dz * dz <= FLARE_EXCLUSION_RADIUS_SQUARED
					&& dy <= FLARE_EXCLUSION_VERTICAL_RANGE)
			{
				return true;
			}
		}

		return false;
	}

	List<Player> getDragonTargetCandidates()
	{
		List<Player> candidates = new ArrayList<>();

		for (Player candidate : GetPlayers(true))
		{
			if (!isInFlareArea(candidate.getLocation()))
			{
				candidates.add(candidate);
			}
		}

		return candidates;
	}

	private void playFlareEffect(Location location, String choice)
	{
		String style = choice == null || choice.isEmpty()
				? "classic"
				: choice.toLowerCase(java.util.Locale.ROOT);
		Type type = Type.BURST;
		Color color = Color.YELLOW;

		switch (style)
		{
			case "ember":
				type = Type.BALL_LARGE;
				color = Color.ORANGE;
				break;
			case "frost":
				type = Type.STAR;
				color = Color.AQUA;
				break;
			case "ender":
				type = Type.CREEPER;
				color = Color.PURPLE;
				break;
			case "solar":
				type = Type.BALL_LARGE;
				color = Color.YELLOW;
				break;
			case "aurora":
				type = Type.STAR;
				color = Color.LIME;
				break;
			default:
				break;
		}

		// Firework entities are server spawned, so nearby players see the same equipped effect.
		Location effectLocation = location.clone().add(0, 1, 0);
		UtilFirework.playFirework(effectLocation, type, color, true, true);
		UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK, effectLocation,
				0.45F, 0.35F, 0.45F, 0.04F, 16, ViewDist.NORMAL);
	}

	@EventHandler
	public void dragonSpawn(UpdateEvent event)
	{
		if (event.getType() != UpdateType.SEC_05 || !IsLive())
		{
			return;
		}

		_dragons.removeIf(dragon -> !dragon.getEntity().isValid());

		if (_dragons.size() < MAX_DRAGONS)
		{
			CreatureAllowOverride = true;

			Location location = UtilAlg.Random(_dragonSpawns);

			if (location == null)
			{
				return;
			}

			EnderDragon dragon = location.getWorld().spawn(location, EnderDragon.class);
			UtilEnt.vegetate(dragon);
			UtilEnt.ghost(dragon, true, false);
			UtilEnt.setTickWhenFarAway(dragon, true);

			location.getWorld().playSound(location, Sound.ENDERDRAGON_GROWL, 20f, 1f);

			_dragons.add(new MineplexDragon(this, dragon));

			CreatureAllowOverride = false;
		}
	}

	@EventHandler
	public void dragonTarget(UpdateEvent event)
	{
		if (event.getType() != UpdateType.FASTEST)
		{
			return;
		}

		if (!IsLive())
		{
			_flareZones.clear();
			return;
		}

		List<Player> alive = GetPlayers(true);

		_dragons.forEach(dragon ->
		{
			dragon.updateTarget();

			for (Player player : UtilEnt.getPlayersInsideEntity(dragon.getEntity(), alive))
			{
				if (!Recharge.Instance.use(player, "Hit By " + dragon.getEntity().getEntityId(), 2000, false, false))
				{
					continue;
				}

				player.playEffect(EntityEffect.HURT);
				UtilAction.velocity(player, UtilAlg.getTrajectory(dragon.getEntity(), player), 1, false, 0, 0.6, 2, true);
			}
		});

		for (Item item : _sparkler.GetItems())
		{
			Location location = item.getLocation();

			if (location.getY() < 4 || location.getBlock().isLiquid())
			{
				continue;
			}

			_dragons.forEach(dragon -> dragon.targetSparkler(location));
		}
	}

	@EventHandler
	public void dragonTarget(EntityTargetEvent event)
	{
		if (!IsLive())
		{
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler
	public void dragonArrowDamage(CustomDamageEvent event)
	{
		if (event.IsCancelled())
		{
			return;
		}

		for (MineplexDragon dragon : _dragons)
		{
			if (dragon.getEntity().equals(event.GetDamageeEntity()))
			{
				if (event.GetProjectile() == null)
				{
					dragon.targetSky();
				}

				event.SetCancelled("Dragon");
				dragon.getEntity().playEffect(EntityEffect.HURT);
				return;
			}
		}
	}

	@EventHandler
	public void fallDamage(CustomDamageEvent event)
	{
		if (event.IsCancelled() || event.GetCause() != DamageCause.FALL)
		{
			return;
		}

		event.AddMod(GetName(), "Fall Reduction", -1, false);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void itemSpawn(ItemSpawnEvent event)
	{
		if (event.getEntity().getItemStack().getType() == Material.EMERALD)
		{
			return;
		}

		event.setCancelled(true);
	}

	@EventHandler
	public void playerOut(PlayerDeathOutEvent event)
	{
		giveSurvivedGems(event.GetPlayer());
	}

	protected void giveSurvivedGems(Player player)
	{
		long time = (System.currentTimeMillis() - GetStateTime());
		double gems = time / 10000d;
		String reason = "Survived for " + UtilTime.MakeStr(time);

		AddGems(player, gems, reason, false, false);
	}

	@Override
	public void AnnounceEnd(List<Player> places)
	{
		// Give the winner gems for surviving the latest
		giveSurvivedGems(places.get(0));

		super.AnnounceEnd(places);
	}
}
