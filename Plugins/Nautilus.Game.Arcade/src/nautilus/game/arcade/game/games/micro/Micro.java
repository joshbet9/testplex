package nautilus.game.arcade.game.games.micro;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import mineplex.core.common.util.C;
import mineplex.core.common.util.MapUtil;
import mineplex.core.common.util.UtilAlg;
import mineplex.core.common.util.UtilItem;
import mineplex.core.common.util.UtilTime;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.damage.CustomDamageEvent;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.game.GameTeam;
import nautilus.game.arcade.game.TeamGame;
import nautilus.game.arcade.game.games.micro.kits.KitArcher;
import nautilus.game.arcade.game.games.micro.kits.KitFighter;
import nautilus.game.arcade.game.games.micro.kits.KitWorker;
import nautilus.game.arcade.game.games.micro.mission.Last2Tracker;
import nautilus.game.arcade.game.modules.MapCrumbleModule;
import nautilus.game.arcade.game.modules.TeamArmorModule;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.stats.KillsWithinGameStatTracker;

public class Micro extends TeamGame
{

	private static final String[] DESCRIPTION =
			{
					"Gather some " + C.cGreen + "Blocks" + C.Reset + ".",
					"The " + C.cAqua + "Glass Barrier" + C.Reset + " will disappear in " + C.cRed + "10 seconds" + C.Reset + ".",
					"Be the " + C.cYellow + "Last Team" + C.Reset + " standing!"
			};
	private static final long BARRIER_TIME = TimeUnit.SECONDS.toMillis(10);
	private static final long CHEAT_TIME = TimeUnit.SECONDS.toMillis(4);
	private static final double BORDER_WARNING_DISTANCE = 3.0;

	private final Set<Block> _glass = new HashSet<>();
	private final Map<GameTeam, Location> _teamCenters = new HashMap<>();
	private final Map<UUID, ArmorStand> _borderIndicators = new HashMap<>();
	private final Map<UUID, Integer> _glassColoursSent = new HashMap<>();
	private int _visualTick;

	public Micro(ArcadeManager manager)
	{
		super(manager, GameType.Micro, new Kit[]
				{
						new KitArcher(manager),
						new KitWorker(manager),
						new KitFighter(manager)
				}, DESCRIPTION);

		StrictAntiHack = true;
		InventoryClick = true;
		ItemDrop = true;
		ItemPickup = true;
		BlockBreak = true;
		BlockPlace = true;

		new CompassModule()
				.register(this);

		new TeamArmorModule()
				.giveTeamArmor()
				.giveHotbarItem()
				.register(this);

		new MapCrumbleModule()
				.setRate(3)
				.register(this);

		registerStatTrackers(
				new KillsWithinGameStatTracker(this, 8, "Annihilation")
		);

		registerMissions(
				new Last2Tracker(this)
		);

		registerChatStats(
				Kills,
				Assists,
				BlankLine,
				DamageTaken,
				DamageDealt
		);
	}

	@Override
	public void ParseData()
	{
		for (Location location : WorldData.GetCustomLocs("20"))
		{
			Block block = location.getBlock();

			_glass.add(block);
			block.setType(Material.STAINED_GLASS);
		}

		for (GameTeam team : GetTeamList())
		{
			_teamCenters.put(team, UtilAlg.getAverageLocation(team.GetSpawns()));
		}
	}

	@EventHandler
	public void preventGrow(BlockGrowEvent event)
	{
		event.setCancelled(true);
	}

	@EventHandler
	public void preventFlow(BlockFromToEvent event)
	{
		event.setCancelled(true);
	}

	@EventHandler
	public void playerDropItem(PlayerDropItemEvent event)
	{
		if (!IsLive())
		{
			return;
		}

		ItemStack itemStack = event.getItemDrop().getItemStack();

		if (UtilItem.isSword(itemStack))
		{
			event.setCancelled(true);
		}
	}

	/**
	 * Applies Micro Battles visual cosmetics and the border-proximity warning.
	 * Glass changes are sent as per-player block updates, so one player's colour
	 * never changes the blocks displayed to another player.
	 */
	@EventHandler
	public void updatePlayerVisuals(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
		{
			return;
		}

		if (!IsLive())
		{
			clearPlayerVisuals();
			return;
		}

		if (++_visualTick % 4 != 0)
		{
			return;
		}

		Set<UUID> activePlayers = new HashSet<>();

		for (Player player : GetPlayers(true))
		{
			UUID playerId = player.getUniqueId();
			activePlayers.add(playerId);
			applyMiddleGlassColour(player);
			updateBorderWarning(player);
		}

		for (UUID playerId : new HashSet<>(_borderIndicators.keySet()))
		{
			if (!activePlayers.contains(playerId))
			{
				removeBorderIndicator(playerId);
			}
		}
	}

	private void applyMiddleGlassColour(Player player)
	{
		if (_glass.isEmpty())
		{
			_glassColoursSent.remove(player.getUniqueId());
			return;
		}

		int selectedColour = ArcadeCosmeticsMenu.getEquippedColourData(player.getUniqueId(), "microbattles");
		int glassColour = selectedColour < 0 ? 0 : selectedColour;
		Integer previouslySent = _glassColoursSent.get(player.getUniqueId());

		if (previouslySent != null && previouslySent == glassColour)
		{
			return;
		}

		for (Block glass : _glass)
		{
			if (glass.getType() == Material.STAINED_GLASS)
			{
				player.sendBlockChange(glass.getLocation(), Material.STAINED_GLASS, (byte) glassColour);
			}
		}

		_glassColoursSent.put(player.getUniqueId(), glassColour);
	}

	private void updateBorderWarning(Player player)
	{
		int nearestSide = getNearestBorderSide(player.getLocation());
		UUID playerId = player.getUniqueId();

		if (nearestSide < 0)
		{
			removeBorderIndicator(playerId);
			return;
		}

		updateBorderIndicator(player);

		String style = ArcadeCosmeticsMenu.getEquippedChoice(playerId, "microbattles", "border-particles");
		if (style == null || style.isEmpty())
		{
			style = "matching";
		}
		if ("off".equalsIgnoreCase(style))
		{
			return;
		}

		int colour = ArcadeCosmeticsMenu.getEquippedColourData(playerId, "microbattles");
		if (colour < 0)
		{
			colour = 3;
		}

		ParticleType particle = "spark".equalsIgnoreCase(style) ? ParticleType.FIREWORKS_SPARK
				: "soft".equalsIgnoreCase(style) ? ParticleType.CLOUD : ParticleType.BLOCK_DUST;

		Location playerLocation = player.getLocation();
		for (double height = 0.25; height <= 2.25; height += 0.5)
		{
			Location particleLocation = getBorderParticleLocation(playerLocation, nearestSide, playerLocation.getY() + height);

			if (particle == ParticleType.BLOCK_DUST)
			{
				UtilParticle.PlayParticle(ParticleType.BLOCK_DUST.getParticle(Material.STAINED_GLASS, (byte) colour),
						particleLocation, 0.025F, 0.025F, 0.025F, 0.005F, 1, ViewDist.NORMAL, player);
			}
			else
			{
				UtilParticle.PlayParticle(particle, particleLocation,
						0.025F, 0.025F, 0.025F, 0.005F, 1, ViewDist.NORMAL, player);
			}
		}
	}

	/**
	 * Returns the closest map edge when it is within three horizontal blocks.
	 * The four edges are based on the map's MineplexWorld bounds, rather than
	 * the temporary glass barrier, which disappears shortly after a round starts.
	 */
	private int getNearestBorderSide(Location location)
	{
		double west = Math.abs(location.getX() - WorldData.MinX);
		double east = Math.abs((WorldData.MaxX - 1) - location.getX());
		double north = Math.abs(location.getZ() - WorldData.MinZ);
		double south = Math.abs((WorldData.MaxZ - 1) - location.getZ());
		double nearest = Math.min(Math.min(west, east), Math.min(north, south));

		if (nearest > BORDER_WARNING_DISTANCE)
		{
			return -1;
		}
		if (nearest == west)
		{
			return 0;
		}
		if (nearest == east)
		{
			return 1;
		}
		if (nearest == north)
		{
			return 2;
		}
		return 3;
	}

	private Location getBorderParticleLocation(Location playerLocation, int side, double y)
	{
		double minX = WorldData.MinX + 0.15;
		double maxX = WorldData.MaxX - 0.85;
		double minZ = WorldData.MinZ + 0.15;
		double maxZ = WorldData.MaxZ - 0.85;
		double x = Math.max(minX, Math.min(maxX, playerLocation.getX()));
		double z = Math.max(minZ, Math.min(maxZ, playerLocation.getZ()));

		if (side == 0)
		{
			x = minX;
		}
		else if (side == 1)
		{
			x = maxX;
		}
		else if (side == 2)
		{
			z = minZ;
		}
		else
		{
			z = maxZ;
		}

		return new Location(playerLocation.getWorld(), x, y, z);
	}

	private void updateBorderIndicator(Player player)
	{
		UUID playerId = player.getUniqueId();
		ArmorStand indicator = _borderIndicators.get(playerId);
		Location location = player.getLocation().clone().add(0, 2.6, 0);

		if (indicator == null || !indicator.isValid())
		{
			indicator = player.getWorld().spawn(location, ArmorStand.class);
			indicator.setVisible(false);
			indicator.setGravity(false);
			indicator.setBasePlate(false);
			indicator.setMarker(true);
			indicator.setSmall(true);
			indicator.setRemoveWhenFarAway(false);
			indicator.setCustomName(ChatColor.RED + "" + ChatColor.BOLD + "! " + ChatColor.YELLOW + "BORDER");
			indicator.setCustomNameVisible(true);
			_borderIndicators.put(playerId, indicator);
		}
		else
		{
			indicator.teleport(location);
		}
	}

	private void removeBorderIndicator(UUID playerId)
	{
		ArmorStand indicator = _borderIndicators.remove(playerId);
		if (indicator != null && indicator.isValid())
		{
			indicator.remove();
		}
	}

	private void clearPlayerVisuals()
	{
		for (UUID playerId : new HashSet<>(_borderIndicators.keySet()))
		{
			removeBorderIndicator(playerId);
		}
		_glassColoursSent.clear();
	}

	@EventHandler
	public void updateHunger(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
		{
			return;
		}

		for (Player player : GetPlayers(true))
		{
			if (player.getFoodLevel() < 2)
			{
				player.setFoodLevel(2);
			}
		}
	}

	@EventHandler
	public void updateGlassBarrier(UpdateEvent event)
	{
		if (event.getType() != UpdateType.FASTER || !IsLive() || _glass.isEmpty())
		{
			return;
		}

		if (!UtilTime.elapsed(GetStateTime(), CHEAT_TIME))
		{
			for (GameTeam team : GetTeamList())
			{
				Location expected = _teamCenters.get(team);

				for (Player player : team.GetPlayers(true))
				{
					Location location = UtilAlg.findClosest(player.getLocation(), _teamCenters.values());

					if (!location.equals(expected))
					{
						Manager.GetDamage().NewDamageEvent(player, null, null, DamageCause.CUSTOM, 500, false, true, true, GetName(), "Cheating");
					}
				}
			}
		}

		if (UtilTime.elapsed(GetStateTime(), BARRIER_TIME))
		{
			for (Block block : _glass)
			{
				MapUtil.QuickChangeBlockAt(block.getLocation(), Material.AIR);
			}

			_glass.clear();
			_glassColoursSent.clear();
			_teamCenters.clear();
		}
	}

	@EventHandler
	public void blockBreak(BlockBreakEvent event)
	{
		if (_glass.contains(event.getBlock()))
		{
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void arrowDamage(CustomDamageEvent event)
	{
		if (event.GetProjectile() == null)
		{
			return;
		}

		event.AddKnockback("Increase", 1.6d);
	}
}
