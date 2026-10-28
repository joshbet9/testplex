package nautilus.game.arcade.game.games.monstermaze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.common.util.UtilAction;
import mineplex.core.common.util.UtilAlg;
import mineplex.core.common.util.UtilBlock;
import mineplex.core.common.util.UtilFirework;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilMath;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.common.util.UtilServer;
import mineplex.core.common.util.UtilTextMiddle;
import mineplex.core.common.util.UtilTime;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.disguise.DisguiseFactory;
import mineplex.core.disguise.disguises.DisguiseBase;
import mineplex.core.disguise.disguises.DisguiseMagmaCube;
import mineplex.core.disguise.disguises.DisguiseSlime;
import mineplex.core.disguise.disguises.DisguiseSnowman;
import mineplex.core.recharge.Recharge;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.Game.GameState;
import nautilus.game.arcade.game.games.monstermaze.MMMazes.MazePreset;
import nautilus.game.arcade.game.games.monstermaze.MazeMobWaypoint.CardinalDirection;
import nautilus.game.arcade.game.games.monstermaze.events.EntityLaunchEvent;
import nautilus.game.arcade.game.games.monstermaze.events.FirstToSafepadEvent;
import nautilus.game.arcade.game.games.monstermaze.events.MonsterBumpPlayerEvent;
import nautilus.game.arcade.game.games.monstermaze.events.SafepadBuildEvent;
import nautilus.game.arcade.game.games.monstermaze.kits.KitBodyBuilder;
import nautilus.game.arcade.game.games.monstermaze.kits.KitBodyBuilderModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitJumperModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitMaverick;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class Maze implements Listener
{
	private MonsterMaze _host;
	private MazePreset _preset;
	
	private Location _location;
	
	private ArrayList<Player> _playersOnPad = new ArrayList<Player>();
	private HashMap<LivingEntity, MazeMobWaypoint> _ents = new HashMap<LivingEntity, MazeMobWaypoint>();
	private final HashMap<LivingEntity, Long> _frozenUntil = new HashMap<LivingEntity, Long>();
	// Keep the selected theme, concrete mob skin and Safe Pad tint consistent per viewer.
	private final Map<UUID, String> _viewerMobThemeChoices = new HashMap<UUID, String>();
	private final Map<UUID, String> _viewerMobSetIds = new HashMap<UUID, String>();
	private final Map<UUID, EntityType> _viewerMobTypes = new HashMap<UUID, EntityType>();
	// Live overrides sit above grouped base disguises so changing one viewer never changes other players.
	private final Map<UUID, Map<UUID, DisguiseBase>> _viewerDisguiseOverrides = new HashMap<UUID, Map<UUID, DisguiseBase>>();
	private static final List<String> MOB_THEME_CYCLE = Collections.unmodifiableList(Arrays.asList(
			"map_default", "creeper", "skeleton", "spider", "zombie", "slime", "zombie_pigman",
			"enderman", "cave_spider", "silverfish", "blaze", "magma_cube", "bat", "witch",
			"endermite", "guardian", "pig", "sheep", "cow", "chicken", "squid", "wolf",
			"mooshroom", "snowman", "ocelot", "horse", "rabbit", "villager", "random"));
	private final LinkedList<Location> _pendingSpawnLocations = new LinkedList<Location>();
	private static final int MAX_MOB_SPAWNS_PER_TICK = 25;

	private HashSet<Block> _movementWaypoints = new HashSet<Block>();
	private HashSet<Block> _movementWaypointsDisabled = new HashSet<Block>();
	private ArrayList<Location> _playerContainmentGlass = new ArrayList<Location>();

	private int _centerSafeZoneDecay = 11;
//	private NautHashMap<Location, Long> _centerSafeZoneDecay = new NautHashMap<Location, Long>();
	
	private SafePad _nextSafePad;
	private SafePad _safePad = null;
	private LinkedList<SafePad> _oldSafePads = new LinkedList<SafePad>();
	private int _curSafe = 1;

	private int _phaseTimer = 60;
	private int _phaseTimerStart = 60;

	// Independent, opt-in objective-beacon repair. Diagnostics remain a separate setting.
	private final boolean _safePadBeaconEnhancementsEnabled;
	private long _initialSafePadCreatedAt;
	private long _lastSafePadBeaconCheckAt;
	private boolean _preRoundBeaconCheckDone;
	private final List<Location> _firstStageParticlePath = new ArrayList<Location>();
	private final Set<Integer> _celebratedPadStages = new HashSet<Integer>();
	private int _firstStageParticleTick;
	private int _padSpawnCount;
	private final Set<UUID> _newPlayerTutorialViewers = new HashSet<UUID>();
			
	@SuppressWarnings("deprecation")
	public Maze(MonsterMaze host, MazePreset maze)
	{
		_host = host;
		_preset = maze;
		_phaseTimerStart = _host.isActualModernMode() ? 35 : 60;
		_phaseTimer = _phaseTimerStart;
		_safePadBeaconEnhancementsEnabled = _host.Manager.getPlugin().getConfig()
				.getBoolean("monstermaze.safe-pad-enhancements.enabled", false);

		// Mob Theme is a per-game toggle, not a persistent preference. Always begin at
		// Map Default; Safe Pad and beacon colour customisation remains saved per mob set.
		// Only first-time players receive the first-stage particle breadcrumb. The first
		// two pad fireworks are shown to everyone in every game.
		for (Player player : _host.GetPlayers(false))
		{
			applyMobThemeChoice(player, "map_default", false);
			if (!MonsterMazePersonalBestManager.hasAnyPersonalBest(_host, player))
				_newPlayerTutorialViewers.add(player.getUniqueId());
		}

		// Stale containment can outlive the old preset's location list.
		clearStaleContainmentGlass();
		
		for (Location loc : getPreset().getMaze())
		{
			_movementWaypoints.add(loc.getBlock());
		}
		
		for (Location loc : maze.getGlassBounds())
		{
			_playerContainmentGlass.add(loc.clone().subtract(0, 1, 0));
			_playerContainmentGlass.add(loc.clone().subtract(0, 2, 0));
		}
		
		for (Location loc : _playerContainmentGlass)
		{
			loc.getBlock().setType(Material.STAINED_GLASS);
			loc.getBlock().setData((byte) 5);
		}
		
		for(Location loc : _preset.getCenterSafeZonePaths())
		{
			_movementWaypointsDisabled.add(loc.getBlock());
		}
		
		_location = maze.getCenter();

		// Entity tracking is process-local; clear any mobs leaked by a previous instance
		// before this run constructs its own mob list and first objective.
		clearStaleMazeEntities();
		spawnSafePad();
		_safePad = _nextSafePad;
		_nextSafePad = null;
		_initialSafePadCreatedAt = System.currentTimeMillis();
		buildFirstStageParticlePath();
		updateCompassTargets();
		
		Bukkit.getPluginManager().registerEvents(this, _host.Manager.getPlugin());
	}
	
	public SafePad getSafePad()
	{
		return _safePad;
	}
	
	public MazePreset getPreset()
	{
		return _preset;
	}
	
	public Location getLocation()
	{
		return _location;
	}
	
	public int getCurrentSafePadCount()
	{
		return _curSafe;
	}

	/** Whether this player has already reached the active pad during this stage. */
	public boolean hasReachedCurrentSafePad(Player player)
	{
		return player != null && _playersOnPad.contains(player);
	}

	/** Reapply client-side pad/beacon tints when a player joins the game or their theme changes. */
	public void refreshPlayerSafePadCosmetics(Player viewer)
	{
		refreshSafePadCosmeticColours(viewer);
	}
	
	public boolean isOnPad(Player player, boolean checkNotActive )
	{
		if (_safePad == null)
			return false;
		
		if (_safePad.isOn(player))
			return true;
		
		if (checkNotActive)
		{
			for (SafePad pad : _oldSafePads)
			{
				if (pad.isOn(player))
				{
					return true;
				}
			}
			
			if (_nextSafePad != null)
			{
				if (_nextSafePad.isOn(player))
				{
					return true;
				}
			}
			
			return false;
		}
		
		return false;
	}
	
	@EventHandler
	public void setCompassTarget(UpdateEvent event)
	{
		if (event.getType() != UpdateType.FASTER)
			return;
		updateCompassTargets();
	}

	/** Retarget immediately when the active objective changes, not only on the next updater tick. */
	private void updateCompassTargets()
	{
		SafePad active = _safePad != null ? _safePad : _nextSafePad;
		if (active == null)
			return;

		for (Player player : _host.GetPlayers(true))
		{
			// When this viewer is safely on the active pad, redirect to the preview pad.
			// Other players still need the compass to point to the pad they must reach.
			SafePad target = active;
			if (_safePad != null && _nextSafePad != null && _safePad.isOn(player))
				target = _nextSafePad;
			player.setCompassTarget(target.getLocation().clone().add(0.5D, 0.0D, 0.5D));
		}
	}
	
	@EventHandler
	public void updateTick(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;
		
		if (!_host.IsLive())
			return;
		
		// The green particle breadcrumb only exists for stage one.
		updateFirstStageParticlePath();
		//Updates
		checkPlayersOnSafePad();
		move();
		bump();
//		removeMobsOnSafePad();
	}

	@EventHandler
	public void updateSecond(UpdateEvent event)
	{
		if (event.getType() != UpdateType.SEC)
			return;
		
		if (!_host.IsLive())
			return;
		
		decrementSafePadTime();
		decrementPhaseTime();
		
		if (UtilTime.elapsed(_host.getGameLiveTime(), 20000))
		{
			deteriorateCenter();
		}
	}
	
	/**
	 * When explicitly enabled, audit only the central objective beacon. During
	 * recruit, run one repair check 2–3 seconds after the first pad is created;
	 * while live, check the active and preview objective beacons once per second.
	 * No stained clay or other pad block is repaired by this feature.
	 */
	@EventHandler
	public void updateSafePadBeaconPersistence(UpdateEvent event)
	{
		if (event.getType() != UpdateType.SEC || !_safePadBeaconEnhancementsEnabled)
			return;

		long now = System.currentTimeMillis();
		if (!_host.IsLive())
		{
			if (!_preRoundBeaconCheckDone && now - _initialSafePadCreatedAt >= 2000L)
			{
				_preRoundBeaconCheckDone = true;
				if (_safePad != null)
					_safePad.restoreObjectiveBeaconIfMissing();
			}
			return;
		}

		if (now - _lastSafePadBeaconCheckAt < 1000L)
			return;
		_lastSafePadBeaconCheckAt = now;

		if (_safePad != null)
			_safePad.restoreObjectiveBeaconIfMissing();
		if (_nextSafePad != null)
			_nextSafePad.restoreObjectiveBeaconIfMissing();
	}

	@EventHandler
	public void updateExperience(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;
		
		if (!_host.IsLive())
			return;
		
//		if (UtilTime.elapsed(Host.getGameLiveTime(), 15000))
//		{
//			deteriorateCenter();
//		}
		
		if (_safePad == null)
		{
			for (Player p : _host.GetPlayers(true))
			{
				p.setExp(0F);
			}
			return;
		}
		
		if (_playersOnPad.isEmpty()) //Nobody has gotten to it yet
		{
			float percent = (float) Math.min(Math.max(_phaseTimer * (1 / _phaseTimerStart), 0), .999);
			
			for(Player p : _host.GetPlayers(true))
			{
				p.setExp(percent);
			}
			return;
		}
		
		float percentage = (float) Math.min(Math.max(_phaseTimer / Math.max(6, 16 - (_curSafe - 1)), 0), 1);
		for (Player p : _host.GetPlayers(true))
		{
			p.setExp(percentage);
		}
	}

//	private void removeMobsOnSafePad()
//	{
//		Iterator<LivingEntity> it = _ents.keySet().iterator();
//		while (it.hasNext()) 
//		{
//			LivingEntity e = it.next();
//			if(_safePad != null)
//			{
//				if(_safePad.isOn(e))
//				{
//					System.out.println("entity on safepad removed");
//					it.remove();
//					e.remove();
//					continue;
//				}
//			}
//			
//			for (SafePad oldSafePad : _oldSafePads)
//			{
//				if(oldSafePad.isOn(e))
//				{
//					System.out.println("entity on old safepad removed");
//					it.remove();
//					e.remove();
//				}
//			}
//		}
//	}

	private void bump()
	{
		//Hit Players
		for (Player player : _host.GetPlayers(true))
		{
			KitBodyBuilderModern bodyRushKit = _host.isModernVariant()
					&& _host.GetKit(player) instanceof KitBodyBuilderModern
					? (KitBodyBuilderModern) _host.GetKit(player) : null;
			boolean bodyRushActive = bodyRushKit != null && bodyRushKit.isBodyRushActive(player);
			if (!Recharge.Instance.usable(player, "Monster Hit") && !bodyRushActive)
				continue;
			
			if (isOnPad(player, true))
				continue;

			//Hit Snowman
			for (LivingEntity ent : _ents.keySet())
			{
				if (isMonsterFrozen(ent) || UtilMath.offset(player, ent) >= 1)
					continue;

				// Body Rush intercepts the contact before normal knockback/damage.
				if (bodyRushKit != null && bodyRushKit.consumeMonsterHit(player))
				{
					ent.playEffect(org.bukkit.EntityEffect.HURT);
					UtilAction.velocity(ent, UtilAlg.getTrajectory2d(player, ent), 1, true, 0, 0.8, 2, true);
					bodyRushKit.trackLaunched(ent);
					Bukkit.getPluginManager().callEvent(new EntityLaunchEvent(ent));
					player.playSound(player.getLocation(), Sound.SUCCESSFUL_HIT, 1.0f, 1.35f);
					// EntityLaunchEvent removes the mob from _ents, so break before
					// the enhanced-for iterator advances.
					break;
				}

				Recharge.Instance.useForce(player, "Monster Hit", 1000);

				// Maverick redirects knockback toward the preview Safe Pad when present.
				if (_host.isModernVariant() && _host.GetKit(player) instanceof KitMaverick)
				{
					Location target = getMobKnockTarget();
					if (target != null && target.getWorld() == player.getWorld())
					{
						org.bukkit.util.Vector direction = target.toVector().subtract(player.getLocation().toVector());
						direction.setY(0);
						if (direction.lengthSquared() > 0.001)
							UtilAction.velocity(player, direction.normalize(), 1, false, 0, 0.75, 1.2, true);
						else
							UtilAction.velocity(player, UtilAlg.getTrajectory(ent, player), 1, false, 0, 0.75, 1.2, true);
					}
					else
						UtilAction.velocity(player, UtilAlg.getTrajectory(ent, player), 1, false, 0, 0.75, 1.2, true);
				}
				else
					UtilAction.velocity(player, UtilAlg.getTrajectory(ent, player), 1, false, 0, 0.75, 1.2, true);

				// Allow only this synchronous mob-contact event through the game damage flags.
				// Monster Maze otherwise keeps generic PvE and fall damage disabled.
				double healthBeforeHit = player.getHealth();
				boolean previousDamagePvE = _host.DamagePvE;
				boolean previousDamageEvP = _host.DamageEvP;
				_host.DamagePvE = true;
				_host.DamageEvP = true;
				try
				{
					_host.Manager.GetDamage().NewDamageEvent(player, ent, null,
							DamageCause.ENTITY_ATTACK, 4, false, true, false,
							"Monster", "Monster Attack");

					// Reborn applies CustomDamageEvent synchronously. If another listener cancels it,
					// still make this confirmed monster collision hurt the player.
					if (player.isOnline() && !player.isDead() && player.getHealth() >= healthBeforeHit)
					{
						player.damage(4.0, ent);
						if (player.isOnline() && !player.isDead() && player.getHealth() >= healthBeforeHit)
							player.setHealth(Math.max(0.0, healthBeforeHit - 4.0));
					}
				}
				finally
				{
					_host.DamagePvE = previousDamagePvE;
					_host.DamageEvP = previousDamageEvP;
				}

				PacketPlayOutAnimation animation = new PacketPlayOutAnimation();
				animation.a = ent.getEntityId();
				animation.b = 0;

				if (_host.Manager.GetDisguise().isDisguised(ent))
					animation.a = _host.Manager.GetDisguise().getDisguise(ent).getEntityId();
				
				for (Player cur : UtilServer.getPlayers())
				{
					for (int i = 0 ; i < 3 ; i++)
					{
						UtilPlayer.sendPacket(cur, animation);
					}
				}

				Bukkit.getPluginManager().callEvent(new MonsterBumpPlayerEvent(player));
				// One contact window must count as a single hit even if several mobs overlap the player.
				break;
			}
		}
	}

	private void move()
	{
		Iterator<Entry<LivingEntity, MazeMobWaypoint>> entIterator = _ents.entrySet().iterator();

		//Move & Die
		while (entIterator.hasNext())
		{
			Entry<LivingEntity, MazeMobWaypoint> data = entIterator.next();
			if (isMonsterFrozen(data.getKey()))
			{
				data.getKey().setVelocity(new org.bukkit.util.Vector(0, 0, 0));
				continue;
			}
			
			//New or Fallen
			if (data.getValue().Target == null || data.getKey().getLocation().getY() < data.getValue().Target.getBlockY())
			{
				Location loc = UtilAlg.findClosest(data.getKey().getLocation(), getPreset().getMaze());
				
				data.getKey().teleport(loc);
				data.getValue().Target = loc;
			}

			//New Waypoint
			if (UtilMath.offset2d(data.getKey().getLocation(), data.getValue().Target) < 0.4)
			{
				ArrayList<Block> nextBlock = new ArrayList<Block>();
				
				Block north = getTarget(data.getKey().getLocation().getBlock(), null, BlockFace.NORTH);
				Block south = getTarget(data.getKey().getLocation().getBlock(), null, BlockFace.SOUTH);
				Block east = getTarget(data.getKey().getLocation().getBlock(), null, BlockFace.EAST);
				Block west = getTarget(data.getKey().getLocation().getBlock(), null, BlockFace.WEST);
				
				if (north != null)		nextBlock.add(north);
				if (south != null)		nextBlock.add(south);
				if (east != null)		nextBlock.add(east);
				if (west != null)		nextBlock.add(west);
				
				if(nextBlock.isEmpty())
				{
					entIterator.remove();
					data.getKey().remove();
					continue;
				}
				
				if(nextBlock.size() > 1 && data.getValue().Direction != CardinalDirection.NULL) // they can do a uturn if they're stuck
				{
					if(data.getValue().Direction == CardinalDirection.NORTH)
					{
						nextBlock.remove(south);
					}
					else if(data.getValue().Direction == CardinalDirection.SOUTH)
					{
						nextBlock.remove(north);
					}
					else if(data.getValue().Direction == CardinalDirection.WEST)
					{
						nextBlock.remove(east);
					}
					else if(data.getValue().Direction == CardinalDirection.EAST)
					{
						nextBlock.remove(west);
					}
				}
				
				if (nextBlock.isEmpty())
				{
					entIterator.remove();
					data.getKey().remove();
					continue;
				}
				
				//Random Direction
				Location nextLoc = UtilAlg.Random(nextBlock).getLocation();
				data.getValue().Target = nextLoc.clone().add(0.5, 0, 0.5);
				if(north != null && nextLoc.equals(north.getLocation()))
				{
					data.getValue().Direction = CardinalDirection.NORTH;
				}
				else if(south != null && nextLoc.equals(south.getLocation()))
				{
					data.getValue().Direction = CardinalDirection.SOUTH;
				}
				else if(east != null && nextLoc.equals(east.getLocation()))
				{
					data.getValue().Direction = CardinalDirection.EAST;
				}
				else if(west != null && nextLoc.equals(west.getLocation()))
				{
					data.getValue().Direction = CardinalDirection.WEST;
				}
			}

			UtilEnt.CreatureMoveFast(data.getKey(), data.getValue().Target, 1.4f);
		}
	}
	
	/** Freeze monsters in a radius for Cryo Blitz. Returns the number affected. */
	public int freezeMonsters(Location origin, double radius, long durationMs)
	{
		if (origin == null || origin.getWorld() == null || durationMs <= 0)
			return 0;
		long until = System.currentTimeMillis() + durationMs;
		double radiusSquared = radius * radius;
		int count = 0;
		for (LivingEntity entity : new ArrayList<LivingEntity>(_ents.keySet()))
		{
			if (entity == null || !entity.isValid() || entity.getWorld() != origin.getWorld())
				continue;
			if (entity.getLocation().distanceSquared(origin) > radiusSquared)
				continue;
			_frozenUntil.put(entity, until);
			entity.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
			count++;
		}
		return count;
	}

	private boolean isMonsterFrozen(LivingEntity entity)
	{
		Long until = _frozenUntil.get(entity);
		if (until == null)
			return false;
		if (until.longValue() > System.currentTimeMillis())
			return true;
		_frozenUntil.remove(entity);
		return false;
	}

	/** Target used by the enhanced Maverick kit, matching standalone behaviour: active pad first. */
	public Location getMobKnockTarget()
	{
		SafePad target = _safePad != null ? _safePad : _nextSafePad;
		return target == null ? null : target.getLocation();
	}

	private Block getTarget(Block start, Block cur, BlockFace face)
	{	
		if (cur == null)
			cur = start;
		
		while (_movementWaypoints.contains(cur.getRelative(face)) && !_movementWaypointsDisabled.contains(cur.getRelative(face)))
		{
			cur = cur.getRelative(face);
			
			//Stop at intersection
			int count = 0;
			
			if (face != BlockFace.NORTH && _movementWaypoints.contains(cur.getRelative(BlockFace.NORTH)) && !_movementWaypointsDisabled.contains(cur.getRelative(BlockFace.NORTH))) count++;
			if (face != BlockFace.SOUTH && _movementWaypoints.contains(cur.getRelative(BlockFace.SOUTH)) && !_movementWaypointsDisabled.contains(cur.getRelative(BlockFace.SOUTH))) count++;
			if (face != BlockFace.EAST && _movementWaypoints.contains(cur.getRelative(BlockFace.EAST)) && !_movementWaypointsDisabled.contains(cur.getRelative(BlockFace.EAST))) count++;
			if (face != BlockFace.WEST && _movementWaypoints.contains(cur.getRelative(BlockFace.WEST)) && !_movementWaypointsDisabled.contains(cur.getRelative(BlockFace.WEST))) count++;
			
			if (count > 1)
				break;
		}
		
		if (cur.equals(start))
			return null;
		
		return cur;
	}
	
//	@SuppressWarnings("deprecation")
//	public void addSafeZone()
//	{
//		if (_safeZones.isEmpty())
//			return;
//		
//		Location zone = _safeZones.remove(UtilMath.r(_safeZones.size()));
//		for (Block b : UtilBlock.getInBoundingBox(zone.clone().add(1, 0, 1), zone.clone().subtract(1, 0, 1), false))
//		{
//			for (int i = 0 ; i < 3 ; i++)
//			{
//				Block cur = b.getLocation().clone().subtract(0, i, 0).getBlock();
//				
//				cur.getRelative(BlockFace.DOWN).setType(Material.STAINED_CLAY);
//				cur.getRelative(BlockFace.DOWN).setData((byte) 13);
//			}
//			
//			_disabledWaypoints.add(b);
//
//			Iterator<Entity> it = _ents.keySet().iterator();
//			while (it.hasNext()) {
//				Entity e = it.next();
//				if (UtilMath.offset(e.getLocation(), b.getLocation()) < 2)
//				{
//					_ents.remove(_ents.get(e));
//					it.remove();
//					e.remove();
//				}
//			}
//		}
//	}
	
	/**
	 * Queue starter/Modern mobs in exact-count batches. Creating 150–225 disguised
	 * entities in the Recruit event's single server tick can create an avoidable
	 * join/startup spike; the standalone 1.8 implementation spreads these over ticks.
	 */
	public void fillSpawn(int numToSpawn)
	{
		if (numToSpawn <= 0 || getPreset() == null)
			return;

		ArrayList<Location> validSpawns = new ArrayList<Location>(getPreset().getMaze());
		Iterator<Location> iter = validSpawns.iterator();
		while (iter.hasNext())
		{
			Location candidate = iter.next();
			if (UtilMath.offset(candidate, _location) < 7.5)
				iter.remove();
		}
		if (validSpawns.isEmpty())
		{
			System.out.println("Monster Maze: no valid maze locations remain for mob spawning.");
			return;
		}

		Collections.shuffle(validSpawns);
		int spawned = 0;
		while (spawned < numToSpawn)
		{
			Location loc = validSpawns.get(spawned % validSpawns.size());
			if (loc == null || loc.getWorld() == null)
			{
				System.out.println("Monster Maze: invalid location in the spawn list; stopping the batch.");
				break;
			}
			_pendingSpawnLocations.add(loc.clone());
			spawned++;
		}
	}

	/** Spawn a bounded number each server tick without capping or discarding the mob population. */
	@EventHandler
	public void processPendingMonsterSpawns(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;

		GameState state = _host.GetState();
		if (state == GameState.End || state == GameState.Dead)
		{
			_pendingSpawnLocations.clear();
			return;
		}

		int spawned = 0;
		while (spawned < MAX_MOB_SPAWNS_PER_TICK && !_pendingSpawnLocations.isEmpty())
		{
			Location loc = _pendingSpawnLocations.removeFirst();
			spawnOne(loc);
			spawned++;
		}
	}

	private void spawnOne(Location loc)
	{
		if (loc == null || loc.getWorld() == null)
			return;

		Map<EntityType, Set<UUID>> viewersByMobType = new HashMap<EntityType, Set<UUID>>();
		for (Player viewer : _host.GetPlayers(false))
		{
			EntityType viewerMobType = getMobTypeForPlayer(viewer);
			Set<UUID> viewers = viewersByMobType.get(viewerMobType);
			if (viewers == null)
			{
				viewers = new HashSet<UUID>();
				viewersByMobType.put(viewerMobType, viewers);
			}
			viewers.add(viewer.getUniqueId());
		}

		try
		{
			_host.CreatureAllowOverride = true;
			Snowman ent = loc.getWorld().spawn(loc, Snowman.class);
			UtilEnt.vegetate(ent, true);
			UtilEnt.ghost(ent, true, false);
			_ents.put(ent, new MazeMobWaypoint(ent.getLocation()));

			if (viewersByMobType.isEmpty())
			{
				registerMobDisguise(ent, _host.getMonsterType(), null);
			}
			else
			{
				for (Entry<EntityType, Set<UUID>> viewerGroup : viewersByMobType.entrySet())
				{
					if (viewerGroup.getKey() == EntityType.SNOWMAN)
						continue; // The backing entity already looks like a snowman to these viewers.
					registerMobDisguise(ent, viewerGroup.getKey(), viewerGroup.getValue());
				}
			}
		}
		finally
		{
			_host.CreatureAllowOverride = false;
		}
	}

	private DisguiseBase registerMobDisguise(Snowman entity, EntityType mobType, final Set<UUID> viewerIds)
	{
		if (mobType == null)
			return null;

		DisguiseBase disguise = DisguiseFactory.createDisguise(entity, mobType);
		if (disguise instanceof DisguiseSlime)
			((DisguiseSlime) disguise).SetSize(3);
		if (disguise instanceof DisguiseMagmaCube)
			((DisguiseMagmaCube) disguise).SetSize(3);
		if (disguise == null)
		{
			Bukkit.getLogger().warning("Monster Maze has no disguise implementation for " + mobType + ".");
			return null;
		}

		if (viewerIds == null)
			_host.Manager.GetDisguise().disguise(disguise);
		else
			_host.Manager.GetDisguise().disguise(disguise, viewer -> viewerIds.contains(viewer.getUniqueId()));
		return disguise;
	}

	private EntityType getMobTypeForPlayer(Player player)
	{
		return getMobEntityTypeForPlayer(player);
	}

	/** Concrete mob skin for this viewer, or the map's source skin. */
	public EntityType getMobEntityTypeForPlayer(Player player)
	{
		if (player == null)
			return _host.getMonsterType();
		getMobThemeChoiceForPlayer(player);
		EntityType selected = _viewerMobTypes.get(player.getUniqueId());
		return selected == null ? _host.getMonsterType() : selected;
	}

	/** Map Default, a named mob theme, or Random Mob, matching the original slot-6 selector. */
	public String getMobThemeChoiceForPlayer(Player player)
	{
		if (player == null)
			return "map_default";
		UUID playerId = player.getUniqueId();
		if (!_viewerMobThemeChoices.containsKey(playerId))
			applyMobThemeChoice(player, "map_default", false);
		String selected = _viewerMobThemeChoices.get(playerId);
		return selected == null ? "map_default" : selected;
	}

	/** The selected concrete mob set is also the key used by Safe Pad/beacon colour cosmetics. */
	public String getMobSetForPlayer(Player player)
	{
		if (player == null)
			return null;
		String choice = getMobThemeChoiceForPlayer(player);
		// The source repo's Map Default palette is lime for both the Safe Pad and
		// its beacon glass. It is a distinct palette key, not the mob model's colour.
		if ("map_default".equals(choice))
			return "map_default";
		return _viewerMobSetIds.get(player.getUniqueId());
	}

	/** Cycle the selector and immediately refresh this viewer's mob models and objective colours. */
	public String cycleMobThemeForPlayer(Player player)
	{
		if (player == null)
			return "map_default";
		String current = getMobThemeChoiceForPlayer(player);
		List<String> options = getMobThemeOptions(player.getUniqueId());
		int index = options.indexOf(current);
		String next = options.get((index < 0 ? 0 : index + 1) % options.size());
		applyMobThemeChoice(player, next, true);
		refreshMobDisguisesForViewer(player);
		refreshSafePadCosmeticColours(player);
		return next;
	}

	/** Reconcile an in-game theme after Mob Sets are enabled or disabled in Cosmetics. */
	public void refreshMobSetSettingsForPlayer(Player player)
	{
		if (player == null || !player.isOnline())
			return;
		UUID playerId = player.getUniqueId();
		String choice = getMobThemeChoiceForPlayer(player);
		List<String> available = getAvailableMobSets(playerId);
		boolean themeChanged = false;
		if ("random".equals(choice))
		{
			String currentConcrete = _viewerMobSetIds.get(playerId);
			if (available.isEmpty())
			{
				applyMobThemeChoice(player, "map_default", true);
				themeChanged = true;
			}
			else if (currentConcrete == null || !available.contains(currentConcrete))
			{
				applyMobThemeChoice(player, "random", false);
				themeChanged = true;
			}
		}
		else if (!"map_default".equals(choice) && !available.contains(choice))
		{
			applyMobThemeChoice(player, "map_default", true);
			themeChanged = true;
		}
		if (themeChanged)
			refreshMobDisguisesForViewer(player);
		refreshSafePadCosmeticColours(player);
	}

	private void applyMobThemeChoice(Player player, String choice, boolean persist)
	{
		UUID playerId = player.getUniqueId();
		String selected = choice == null ? "map_default" : choice.toLowerCase(Locale.ROOT);
		if (!MOB_THEME_CYCLE.contains(selected))
			selected = "map_default";

		String concrete = null;
		EntityType type = _host.getMonsterType();
		if (!"map_default".equals(selected))
		{
			if ("random".equals(selected))
				concrete = randomConcreteMobSet(playerId);
			else
				concrete = selected;

			if (concrete == null || !getAvailableMobSets(playerId).contains(concrete))
			{
				selected = "map_default";
				concrete = null;
			}
			else
			{
				type = entityTypeForMobSet(concrete);
				if (type == null)
				{
					selected = "map_default";
					concrete = null;
					type = _host.getMonsterType();
				}
			}
		}

		_viewerMobThemeChoices.put(playerId, selected);
		_viewerMobSetIds.put(playerId, concrete);
		_viewerMobTypes.put(playerId, type == null ? _host.getMonsterType() : type);
		if (persist)
			ArcadeCosmeticsMenu.setMonsterMazeSelectedMobTheme(playerId, selected);
	}

	private List<String> getAvailableMobSets(UUID playerId)
	{
		List<String> available = new ArrayList<String>();
		boolean customised = ArcadeCosmeticsMenu.hasCustomMonsterMazeMobSets(playerId);
		for (String mobSet : MOB_THEME_CYCLE)
		{
			if ("map_default".equals(mobSet) || "random".equals(mobSet))
				continue;
			if (!customised || ArcadeCosmeticsMenu.isMonsterMazeMobSetEnabled(playerId, mobSet))
				available.add(mobSet);
		}
		return available;
	}

	private List<String> getMobThemeOptions(UUID playerId)
	{
		List<String> options = new ArrayList<String>();
		options.add("map_default");
		List<String> available = getAvailableMobSets(playerId);
		options.addAll(available);
		if (!available.isEmpty())
			options.add("random");
		return options;
	}

	private String randomConcreteMobSet(UUID playerId)
	{
		List<String> available = getAvailableMobSets(playerId);
		if (available.isEmpty())
			return null;
		return available.get(Math.floorMod(playerId.hashCode(), available.size()));
	}

	private void refreshMobDisguisesForViewer(Player viewer)
	{
		if (viewer == null || !viewer.isOnline())
			return;
		UUID viewerId = viewer.getUniqueId();
		Map<UUID, DisguiseBase> overrides = _viewerDisguiseOverrides.get(viewerId);
		if (overrides == null)
		{
			overrides = new HashMap<UUID, DisguiseBase>();
			_viewerDisguiseOverrides.put(viewerId, overrides);
		}
		EntityType selectedType = getMobEntityTypeForPlayer(viewer);
		Set<UUID> onlyThisViewer = Collections.singleton(viewerId);
		for (LivingEntity living : new ArrayList<LivingEntity>(_ents.keySet()))
		{
			if (!(living instanceof Snowman) || !living.isValid())
				continue;
			DisguiseBase previous = overrides.remove(living.getUniqueId());
			if (previous != null)
			{
				try
				{
					_host.Manager.GetDisguise().undisguise(previous);
				}
				catch (Throwable failure)
				{
					Bukkit.getLogger().warning("[MonsterMaze] Could not remove previous Mob Theme for "
							+ viewer.getName() + ": " + failure.getClass().getSimpleName());
				}
			}
			DisguiseBase replacement = registerMobDisguise((Snowman) living, selectedType, onlyThisViewer);
			if (replacement != null)
				overrides.put(living.getUniqueId(), replacement);
		}
	}

	private void refreshSafePadCosmeticColours(Player viewer)
	{
		if (_safePad != null)
			_safePad.refreshViewerCosmeticColours(viewer);
		if (_nextSafePad != null && _nextSafePad != _safePad)
			_nextSafePad.refreshViewerCosmeticColours(viewer);
		for (SafePad pad : _oldSafePads)
			if (pad != _safePad && pad != _nextSafePad)
				pad.refreshViewerCosmeticColours(viewer);

		// Fake glass changes do not invalidate the client's cached beacon beam. Nudge the
		// client once after all active/preview pad overlays have been sent.
		if (_safePad != null || _nextSafePad != null)
		{
			World padWorld = _safePad != null
					? _safePad.getLocation().getWorld()
					: _nextSafePad.getLocation().getWorld();
			SafePad.refreshBeaconBeamForViewer(viewer, _host.Manager.GetPlugin(), padWorld);
		}
	}

	private void clearViewerDisguiseOverrides()
	{
		for (Map<UUID, DisguiseBase> overrides : _viewerDisguiseOverrides.values())
		{
			for (DisguiseBase disguise : new ArrayList<DisguiseBase>(overrides.values()))
			{
				try
				{
					_host.Manager.GetDisguise().undisguise(disguise);
				}
				catch (Throwable ignored)
				{
					// The backing mob may already have been removed during round cleanup.
				}
			}
		}
		_viewerDisguiseOverrides.clear();
	}

	private EntityType entityTypeForMobSet(String mobSet)
	{
		if (mobSet == null)
			return null;
		switch (mobSet.toLowerCase(java.util.Locale.ROOT))
		{
			case "creeper": return EntityType.CREEPER;
			case "skeleton": return EntityType.SKELETON;
			case "spider": return EntityType.SPIDER;
			case "zombie": return EntityType.ZOMBIE;
			case "slime": return EntityType.SLIME;
			case "zombie_pigman": return EntityType.PIG_ZOMBIE;
			case "enderman": return EntityType.ENDERMAN;
			case "cave_spider": return EntityType.CAVE_SPIDER;
			case "silverfish": return EntityType.SILVERFISH;
			case "blaze": return EntityType.BLAZE;
			case "magma_cube": return EntityType.MAGMA_CUBE;
			case "bat": return EntityType.BAT;
			case "witch": return EntityType.WITCH;
			case "endermite": return EntityType.ENDERMITE;
			case "guardian": return EntityType.GUARDIAN;
			case "pig": return EntityType.PIG;
			case "sheep": return EntityType.SHEEP;
			case "cow": return EntityType.COW;
			case "chicken": return EntityType.CHICKEN;
			case "squid": return EntityType.SQUID;
			case "wolf": return EntityType.WOLF;
			case "mooshroom": return EntityType.MUSHROOM_COW;
			case "snowman": return EntityType.SNOWMAN;
			case "ocelot": return EntityType.OCELOT;
			case "horse": return EntityType.HORSE;
			case "rabbit": return EntityType.RABBIT;
			case "villager": return EntityType.VILLAGER;
			default: return null;
		}
	}

	private void spawn(int numToSpawn)
	{
		ArrayList<Location> spawnLocations = new ArrayList<Location>(getPreset().getSpawns());
		if (spawnLocations.isEmpty() || numToSpawn <= 0)
			return;

		Collections.shuffle(spawnLocations);
		int spawned = 0;
		while (spawned < numToSpawn)
		{
			Location loc = spawnLocations.get(spawned % spawnLocations.size());
			spawnOne(loc);
			spawned++;
		}
	}

	private Location pickNextLocForSafePad() // short method name
	{
		if (_safePad != null || !_oldSafePads.isEmpty())
		{
			ArrayList<Location> best = new ArrayList<Location>();
			ArrayList<Location> toAvoid = new ArrayList<Location>();			
			
			for (SafePad pad : _oldSafePads)
			{
				toAvoid.add(pad.getLocation());
			}
			
			if (_safePad != null)
			{
				toAvoid.add(_safePad.getLocation());
			}
			
			for (Location pos : getPreset().getValidSafePadSpawns())
			{
				boolean canAdd = true;
				for (Location avoid : toAvoid)
				{
					if (UtilMath.offset(pos, avoid) < 40)
					{
						canAdd = false;
					}
				}
				
				if (canAdd)
				{
					best.add(pos);
				}
			}
			
			if (best.isEmpty())
			{
				return UtilAlg.findFurthest(_location, getPreset().getValidSafePadSpawns());
			}
			else
			{
				return UtilAlg.Random(best);
			}
		}
		else
		{
			return UtilAlg.findFurthest(_location, getPreset().getValidSafePadSpawns());
		}
	}
	
	private void stopSafePad()
	{
		if (_safePad != null)
		{
			_safePad.turnOffBeacon();
			_oldSafePads.addLast(_safePad);
			_safePad = null;			
		}
	}
	
	public void spawnSafePad()
	{
		Location next = pickNextLocForSafePad();
		
		//Delay 
		_nextSafePad = new SafePad(_host, this, next.clone().subtract(0, 1, 0)); // maybe don't need to clone()
		_nextSafePad.build();
		
		for (Block cur : UtilBlock.getInBoundingBox(next.clone().add(-2, 0, -2), next.clone().add(2, 0, 2), false))
		{
			if (!_movementWaypointsDisabled.contains(cur))
				_movementWaypointsDisabled.add(cur);
		}
		
		for (Iterator<LivingEntity> it = _ents.keySet().iterator() ; it.hasNext() ;)
		{
			LivingEntity en = it.next();
			
			if (!_nextSafePad.isOn(en))
				continue;
			
			it.remove();
			en.remove();
		}
		
		Bukkit.getPluginManager().callEvent(new SafepadBuildEvent());
		// Fireworks mark each pad as soon as it appears, rather than on player arrival.
		playPadSpawnSignal(_nextSafePad);
		updateCompassTargets();
	}
	
	@SuppressWarnings("deprecation")
	public void deteriorateCenter()
	{
		if (getPreset().getCenterSafeZone().isEmpty() || _centerSafeZoneDecay == -1
//				&& _centerSafeZoneDecay.isEmpty()
				)
			return;
		
		_centerSafeZoneDecay--;
		
		ArrayList<Packet> toSend = new ArrayList<Packet>();
		
		int ind = 0;
		
		Iterator<Location> iterator = getPreset().getCenterSafeZone().iterator();
		while (iterator.hasNext())
		{
			Location cur = iterator.next();
			Location loc = cur.clone().subtract(0, 1, 0);

			if(_centerSafeZoneDecay == 10)
			{
				loc.getBlock().setTypeIdAndData(159, (byte) 5, true);
				toSend.add(getBreakPacket(loc, ind, 1));
			}
			else if(_centerSafeZoneDecay == 9)
			{
				toSend.add(getBreakPacket(loc, ind, 2));
			}
			else if(_centerSafeZoneDecay == 8)
			{
				loc.getBlock().setTypeIdAndData(159, (byte) 4, true);
				toSend.add(getBreakPacket(loc, ind, 3));
			}
			else if(_centerSafeZoneDecay == 7)
			{
				toSend.add(getBreakPacket(loc, ind, 4));
			}
			else if(_centerSafeZoneDecay == 6)
			{
				loc.getBlock().setTypeIdAndData(159, (byte) 1, true);
				toSend.add(getBreakPacket(loc, ind, 5));
			}
			else if(_centerSafeZoneDecay == 5)
			{
				toSend.add(getBreakPacket(loc, ind, 6));
			}
			else if(_centerSafeZoneDecay == 4)
			{
				loc.getBlock().setTypeIdAndData(159, (byte) 14, true);
				toSend.add(getBreakPacket(loc, ind, 7));
			}
			else if(_centerSafeZoneDecay == 3)
			{
				toSend.add(getBreakPacket(loc, ind, 8));
			}
			else if(_centerSafeZoneDecay == 2)
			{
				toSend.add(getBreakPacket(loc, ind, 9));
			}
			else if(_centerSafeZoneDecay == 1)
			{
				toSend.add(getBreakPacket(loc, ind, -1));
				iterator.remove();

				if (getPreset().getCenterSafeZonePaths().contains(cur))
				{
					getPreset().getCenterSafeZonePaths().remove(cur);
					getPreset().buildMazeBlockAt(cur);
					_movementWaypointsDisabled.remove(cur.getBlock());
				}
				else
				{
					loc.getBlock().setTypeIdAndData(0, (byte) 0, true);
				}

				getPreset().getMaze().add(cur);				
			}
			
			ind++;
		}

		for (Player p : UtilServer.getPlayers())
		{
			UtilPlayer.sendPacket(p, toSend.toArray(new Packet[toSend.size()]));
		}
		
		if (_centerSafeZoneDecay == 1)
		{
			_centerSafeZoneDecay = -1;
		}
		
//		if (!_centerSafeZone.isEmpty())
//		{
//			Location toBreak = _centerSafeZone.remove(UtilMath.r(_centerSafeZone.size()));
//			
//			if (_centerSafeZonePaths.contains(toBreak))
//			{
//				if (!_waypoints.contains(toBreak.getBlock()))
//					_waypoints.add(toBreak.getBlock());
//				
//				if (!_map.contains(toBreak))
//					_map.add(toBreak);
//				
//				_centerSafeZonePaths.remove(toBreak);
//				_preset.buildMazeBlockAt(toBreak);
//				
//				for (int i = 0 ; i < 3 ; i++)
//				{
//					Location cur = toBreak.clone().subtract(0, i, 0);
//					cur.getWorld().playEffect(cur, Effect.STEP_SOUND, cur.getBlock().getTypeId());
//				}
//			}
//			else
//			{
//				_centerSafeZoneDecay.put(toBreak, System.currentTimeMillis());
//			}
//		}
//
//		if (!_centerSafeZoneDecay.isEmpty())
//		{
//			NautHashMap<Location, Long> copy = new NautHashMap<Location, Long>(_centerSafeZoneDecay);
//
//			for (Entry<Location, Long> entry : copy.entrySet())
//			{
//				if (UtilTime.elapsed(entry.getValue(), 1250)) //break
//				{
//					for (int i = 1 ; i < 4 ; i++)
//					{
//						Location cur = entry.getKey().clone().subtract(0, i, 0);
//						cur.getWorld().playEffect(cur, Effect.STEP_SOUND, 152);
//						cur.getBlock().setTypeIdAndData(0, (byte) 0, true);
//					}
//
//					_centerSafeZonePaths.remove(entry.getKey());
//					_centerSafeZoneDecay.remove(entry.getKey());
//					continue;
//				}
//				else if (UtilTime.elapsed(entry.getValue(), 1000)) //red
//				{
//					for (int i = 1 ; i < 4 ; i++)
//					{
//						Location cur = entry.getKey().clone().subtract(0, i, 0);
//						cur.getBlock().setTypeIdAndData(159, (byte) 14, true);
//					}
//					continue;
//				}
//				else if (UtilTime.elapsed(entry.getValue(), 750)) //orange
//				{
//					for (int i = 1 ; i < 3 ; i++)
//					{
//						Location cur = entry.getKey().clone().subtract(0, i, 0);
//						cur.getBlock().setTypeIdAndData(159, (byte) 1, true);
//					}
//					continue;
//				}
//				else if (UtilTime.elapsed(entry.getValue(), 500)) //yellow
//				{
//					for (int i = 1 ; i < 4 ; i++)
//					{
//						Location cur = entry.getKey().clone().subtract(0, i, 0);
//						cur.getBlock().setTypeIdAndData(159, (byte) 4, true);
//					}
//					continue;
//				}
//				else if (UtilTime.elapsed(entry.getValue(), 250)) //blue
//				{
//					for (int i = 1 ; i < 4 ; i++)
//					{
//						Location cur = entry.getKey().clone().subtract(0, i, 0);
//						cur.getBlock().setTypeIdAndData(159, (byte) 3, true);
//					}
//					continue;
//				}
//			}
//		}
	}
	
	private Packet getBreakPacket(Location location, int index, int progress)
	{
		return new PacketPlayOutBlockBreakAnimation(index, new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()), progress);
	}

	public void decrementSafePadTime()
	{
		Iterator<SafePad> iterator = _oldSafePads.iterator();
		while (iterator.hasNext())
		{
			SafePad pad = iterator.next();

			if (!pad.decay())
				continue;

			for (Block block : UtilBlock.getInBoundingBox(pad.getLocation().clone().add(-2, 1, -2), pad.getLocation().clone().add(2, 1, 2), false))
			{	
				if (_movementWaypointsDisabled.contains(block))
					_movementWaypointsDisabled.remove(block);				
			}

			iterator.remove();
		}
	}
	
	public void decrementPhaseTime()
	{
		if (_safePad == null)
			return;
		
		if(_phaseTimer == -1) return;
		
		_phaseTimer--;
		if(_phaseTimer == 20) // only gets to this by running out of time naturally, not by player
		{
			UtilTextMiddle.display("", C.cGreen + C.Bold + (int) _phaseTimer, 5, 40, 5);
		}
		
		if(_phaseTimer == 15 || _phaseTimer == 10)
		{
			UtilTextMiddle.display("", C.cGreen + C.Bold + (int) _phaseTimer, 5, 40, 5);
		}
			
		if(_phaseTimer == 5 || _phaseTimer == 4)
		{
			UtilTextMiddle.display("", C.cGreen + C.Bold + (int) _phaseTimer, 5, 40, 5);
		}
		
		if(_phaseTimer == 3)
		{
			UtilTextMiddle.display("", C.cYellow + C.Bold + (int) _phaseTimer, 5, 40, 5);
		}
		
		if(_phaseTimer == 2)
		{
			UtilTextMiddle.display("", C.cGold + C.Bold + (int) _phaseTimer, 5, 40, 5);		
			spawnSafePad();
		}
		
		if(_phaseTimer == 1)
		{
			UtilTextMiddle.display("", C.cRed + C.Bold + (int) _phaseTimer, 5, 40, 5);
		}

		if(_phaseTimer == 0)
		{
			for (Player p : new ArrayList<Player>(_host.GetPlayers(true)))
			{
				if (_safePad.isOn(p))
				{
					UtilTextMiddle.display("", C.cYellow + C.Bold + "Get to the Next Safe Pad!", 5, 40, 5, p);
					continue;
				}

				// The timeout is authoritative. A mob collision or ability in this same tick must
				// not let a player evade map damage by moving away before the damage event resolves.
				try
				{
					_host.Manager.GetDamage().NewDamageEvent(p, null, null,
							DamageCause.CUSTOM, 500, false, false, false,
							"Game", "Map damage");
				}
				catch (Throwable failure)
				{
					Bukkit.getLogger().warning("[MonsterMaze] Map-damage event failed for " + p.getName()
							+ ": " + failure.getClass().getSimpleName());
				}
				if (_host.IsAlive(p) && p.isOnline() && !p.isDead() && p.getHealth() > 0.0D)
				{
					// Custom damage is occasionally cancelled or reduced by unrelated handlers.
					// Enforce elimination at the stage boundary even if the damage event partially applies.
					try { p.setHealth(0.0D); } catch (IllegalArgumentException ignored) { }
				}
				UtilTextMiddle.display("", C.cRed + C.Bold + "You weren't on the Safe Pad!", 5, 40, 5, p);
				UtilPlayer.message(p, F.main("Game", "You weren't on the Safe Pad!"));
			}

			// The source's Speed mode retains the original 15-mob wave; Modern adds
			// 30 instead of stacking a legacy 15-mob wave on top of it.
			if (_host.isActualModernMode())
				fillSpawn(30);
			else
				spawn(15);

			stopSafePad();
						
			_playersOnPad.clear();

			if (_host.isActualModernMode())
				_phaseTimerStart = Math.max(15, 35 - (_curSafe * 20 / 9));
			else
				_phaseTimerStart = Math.max(15, 60 - (_curSafe * 2));
			_phaseTimer = _phaseTimerStart;
			
			if (_nextSafePad == null)
			{
				spawnSafePad();
			}

			Iterator<LivingEntity> it = _ents.keySet().iterator();
			while (it.hasNext()) 
			{
				Entity e = it.next();
				if (_nextSafePad.isOn(e))
				{
					it.remove();
					e.remove();
				}
			}
			
			_curSafe++;
			_safePad = _nextSafePad;
			_nextSafePad = null;
			buildFirstStageParticlePath();
			updateCompassTargets();
		}
	}
	
	private void checkPlayersOnSafePad()
	{
		if (_safePad == null) 
			return;

		boolean allOn = true;
		for (Player p : _host.GetPlayers(true))
		{
			if (!_safePad.isOn(p))
			{
				allOn = false;
				
				if (_playersOnPad.contains(p))
				{
					UtilTextMiddle.display("", C.cRed + C.Bold + "Get back to the Safe Pad!", 0, 5, 0, p);
				}
				continue;
			}

			if (_playersOnPad.contains(p))
				continue;

			UtilPlayer.message(p, F.main("Game", "You made it to the Safe Pad!"));
			_playersOnPad.add(p);
			// Redirect this player's compass as soon as they claim the active pad.
			updateCompassTargets();
			// Persist a personal best per native game mode, selected kit, and original maze pattern.
			MonsterMazePersonalBestManager.record(_host, p, _curSafe, _preset.getPatternId());
			if (_host.isModernVariant())
				_host.resetModernJumperCharges(p);

			_host.AddGems(p, 2, "Safe Pads Reached", true, true);

			if (_playersOnPad.size() == 1) // first player
			{
				_host.Announce(F.main("Game", F.name(p.getName()) + " made it to the Safe Pad first!"));

				UtilTextMiddle.display("", C.cYellow + C.Bold + "You got to the Safe Pad first!", 5, 40, 5, p);
				_host.AddGems(p, 7.5, "First Safe Pads", true, true);
				p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1.0f, 1.0f);

				//2 hearts
				UtilPlayer.health(p, 4.0);
				
				if (_host.GetKit(p) instanceof KitBodyBuilder)
				{
					p.setMaxHealth(Math.min(p.getMaxHealth() + 2, 30));
					UtilParticle.PlayParticle(ParticleType.HEART, p.getEyeLocation().clone().add(0, .5, 0), 0F, 0F, 0F, 0, 3, ViewDist.NORMAL, UtilServer.getPlayers());
				}
				
				int decreased = Math.max(6, 16 - (_curSafe - 1));
				_phaseTimer = Math.min(decreased, _phaseTimer);

				for (Player player : _host.GetPlayers(true))
				{
					if (player == p)
						continue;

					UtilPlayer.message(player, F.main("Game", "You have " + F.time(decreased + " Seconds") + " to make it to the Safe Pad!"));
				}
				
				Bukkit.getPluginManager().callEvent(new FirstToSafepadEvent(p));
			} 
			else // not the first
			{
				p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1.0f, 1.0f);
				UtilTextMiddle.display("", C.cYellow + C.Bold + "You got to the Safe Pad!", 5, 40, 5, p);
				
				UtilPlayer.health(p, 2.0);
			}
		}
		
		if (allOn)
		{
			_phaseTimer = Math.min(4, _phaseTimer);
		}
	}
	
	public void removePlayerContainmentUnit()
	{
		for (Location loc : _playerContainmentGlass)
			loc.getBlock().setType(Material.AIR);
		clearStaleContainmentGlass();
	}

	/** Remove stale centre containment layers even when their old preset list is gone. */
	private void clearStaleContainmentGlass()
	{
		if (_preset == null || _preset.getCenter() == null || _preset.getCenter().getWorld() == null)
			return;
		Location centre = _preset.getCenter();
		int cx = centre.getBlockX();
		int cy = centre.getBlockY();
		int cz = centre.getBlockZ();
		for (int x = -50; x <= 50; x++)
		{
			for (int z = -50; z <= 50; z++)
			{
				for (int y = cy; y <= cy + 1; y++)
				{
					Block block = centre.getWorld().getBlockAt(cx + x, y, cz + z);
					if (block.getType() == Material.STAINED_GLASS)
						block.setType(Material.AIR);
				}
			}
		}
	}
	
	public Set<LivingEntity> getMonsters()
	{
		return _ents.keySet();
	}
	
	public int getPhaseTimer()
	{
		return _phaseTimer;
	}
	
	private String pathCellKey(Location location)
	{
		return location.getBlockX() + ":" + location.getBlockZ();
	}

	private Location nearestMazePathCell(Location target, Map<String, Location> locations)
	{
		Location nearest = null;
		double best = Double.MAX_VALUE;
		for (Location candidate : locations.values())
		{
			double dx = candidate.getBlockX() - target.getBlockX();
			double dz = candidate.getBlockZ() - target.getBlockZ();
			double distance = dx * dx + dz * dz;
			if (distance < best)
			{
				best = distance;
				nearest = candidate;
			}
		}
		return nearest;
	}

	/** Build a genuine walkable breadcrumb from the centre to the first Safe Pad. */
	private void buildFirstStageParticlePath()
	{
		_firstStageParticlePath.clear();
		_firstStageParticleTick = 0;
		if (_curSafe != 1 || _safePad == null || _preset == null || _preset.getCenter() == null)
			return;

		Map<String, Location> cells = new HashMap<String, Location>();
		for (Location location : _preset.getMaze())
			cells.put(pathCellKey(location), location.clone());
		if (cells.isEmpty())
			return;

		Location centre = _preset.getCenter();
		Location start = nearestMazePathCell(centre, cells);
		Location target = nearestMazePathCell(_safePad.getLocation(), cells);
		if (start == null || target == null)
			return;

		String startKey = pathCellKey(start);
		String targetKey = pathCellKey(target);
		Queue<Location> queue = new ArrayDeque<Location>();
		Set<String> visited = new HashSet<String>();
		Map<String, String> previous = new HashMap<String, String>();
		queue.add(start);
		visited.add(startKey);
		int[][] directions = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

		while (!queue.isEmpty() && !visited.contains(targetKey))
		{
			Location current = queue.remove();
			String currentKey = pathCellKey(current);
			for (int[] direction : directions)
			{
				String nextKey = (current.getBlockX() + direction[0]) + ":" + (current.getBlockZ() + direction[1]);
				Location next = cells.get(nextKey);
				if (next == null || !visited.add(nextKey))
					continue;
				previous.put(nextKey, currentKey);
				queue.add(next);
			}
		}
		if (!visited.contains(targetKey))
			return;

		List<Location> reversed = new ArrayList<Location>();
		String cursor = targetKey;
		while (cursor != null)
		{
			Location location = cells.get(cursor);
			if (location != null)
				reversed.add(location.clone());
			if (cursor.equals(startKey))
				break;
			cursor = previous.get(cursor);
		}
		if (reversed.isEmpty() || !pathCellKey(reversed.get(reversed.size() - 1)).equals(startKey))
			return;
		Collections.reverse(reversed);

		// Draw a short continuous segment from the exact central marker to the graph.
		int dx = start.getBlockX() - centre.getBlockX();
		int dz = start.getBlockZ() - centre.getBlockZ();
		int steps = Math.max(Math.abs(dx), Math.abs(dz));
		for (int step = 0; step <= steps; step++)
		{
			double fraction = steps == 0 ? 0.0D : (double) step / (double) steps;
			Location point = centre.clone();
			point.setX(centre.getBlockX() + 0.5D + dx * fraction);
			point.setZ(centre.getBlockZ() + 0.5D + dz * fraction);
			point.setY(centre.getY() + 0.2D);
			_firstStageParticlePath.add(point);
		}
		for (Location location : reversed)
			_firstStageParticlePath.add(location.clone().add(0.5D, 0.2D, 0.5D));
	}

	private void updateFirstStageParticlePath()
	{
		// The breadcrumb leads new players to the first Safe Pad. It must not be broadcast
		// to experienced players or shown after the first-pad stage has ended.
		if (_curSafe != 1 || _firstStageParticlePath.isEmpty() || _newPlayerTutorialViewers.isEmpty())
			return;
		if (++_firstStageParticleTick % 10 != 0)
			return;

		List<Player> tutorialViewers = new ArrayList<Player>();
		for (Player viewer : _host.GetPlayers(false))
		{
			if (viewer != null && viewer.isOnline()
					&& _newPlayerTutorialViewers.contains(viewer.getUniqueId()))
				tutorialViewers.add(viewer);
		}
		if (tutorialViewers.isEmpty())
			return;
		Player[] recipients = tutorialViewers.toArray(new Player[0]);
		for (Location point : _firstStageParticlePath)
		{
			if (point.getWorld() != null)
				UtilParticle.PlayParticle(ParticleType.HAPPY_VILLAGER, point,
						0F, 0F, 0F, 0F, 1, ViewDist.NORMAL, recipients);
		}
	}

	private void playPadSpawnSignal(SafePad pad)
	{
		if (pad == null || pad.getLocation() == null || pad.getLocation().getWorld() == null)
			return;

		// Fireworks always signal the first two Safe Pads for every participant/game.
		// The separate particle breadcrumb above remains restricted to new players.
		if (_padSpawnCount >= 2)
			return;
		_padSpawnCount++;

		Location location = pad.getLocation().clone().add(0.5D, 10.0D, 0.5D);
		Color colour = _padSpawnCount == 1 ? Color.LIME : Color.AQUA;
		for (Player viewer : _host.GetPlayers(false))
		{
			if (viewer == null || !viewer.isOnline())
				continue;
			UtilFirework.packetPlayFirework(viewer, location, FireworkEffect.Type.BALL_LARGE,
					colour, true, true);
		}
	}

	/** Clear stale hidden Snowman bodies within the current generated maze footprint. */
	private void clearStaleMazeEntities()
	{
		if (_preset == null || _preset.getCenter() == null || _preset.getCenter().getWorld() == null)
			return;
		Location centre = _preset.getCenter();
		for (Entity entity : new ArrayList<Entity>(centre.getWorld().getEntities()))
		{
			if (entity instanceof Player)
				continue;
			Location location = entity.getLocation();
			if (Math.abs(location.getX() - centre.getX()) <= 52.0D
					&& Math.abs(location.getZ() - centre.getZ()) <= 52.0D
					&& Math.abs(location.getY() - centre.getY()) <= 24.0D)
				entity.remove();
		}
	}

	@EventHandler
	public void onGameStateDead(GameStateChangeEvent event)
	{
		if (event.GetState() != GameState.Dead)
			return;

		// The arena/world may be reused by another round. Remove per-viewer overrides before
		// clearing backing mobs and restoring objective blocks.
		clearViewerDisguiseOverrides();
		for (LivingEntity entity : new ArrayList<LivingEntity>(_ents.keySet()))
		{
			if (entity != null && entity.isValid())
				entity.remove();
		}
		_ents.clear();
		clearStaleMazeEntities();
		_frozenUntil.clear();
		_pendingSpawnLocations.clear();
		_playersOnPad.clear();
		_viewerMobThemeChoices.clear();
		_viewerMobSetIds.clear();
		_viewerMobTypes.clear();
		_firstStageParticlePath.clear();
		_celebratedPadStages.clear();

		if (_safePad != null)
			_safePad.restoreAll();
		if (_nextSafePad != null)
			_nextSafePad.restoreAll();
		for (SafePad oldPad : _oldSafePads)
			oldPad.restoreAll();

		_safePad = null;
		_nextSafePad = null;
		_oldSafePads.clear();
		removePlayerContainmentUnit();
		HandlerList.unregisterAll(this);
	}
	
	@EventHandler
    public void onSnowForm(EntityBlockFormEvent event)
    {
		event.setCancelled(true);
    }
	
	@EventHandler
	public void onEntityCombust(EntityCombustEvent event)
	{
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onEntityLaunch(EntityLaunchEvent event)
	{
		_frozenUntil.remove(event.getEntity());
		if (!_ents.containsKey(event.getEntity()))
			return;
		
		_ents.remove(event.getEntity());
	}
}
