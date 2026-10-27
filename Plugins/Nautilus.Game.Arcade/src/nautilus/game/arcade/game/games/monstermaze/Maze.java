package nautilus.game.arcade.game.games.monstermaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.common.util.UtilAction;
import mineplex.core.common.util.UtilAlg;
import mineplex.core.common.util.UtilBlock;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
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
	// Keep one player-specific mob set for the run so Safe Pad colours match it.
	private final Map<UUID, String> _viewerMobSetIds = new HashMap<UUID, String>();
	private final Map<UUID, EntityType> _viewerMobTypes = new HashMap<UUID, EntityType>();
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
			
	@SuppressWarnings("deprecation")
	public Maze(MonsterMaze host, MazePreset maze)
	{
		_host = host;
		_preset = maze;
		_phaseTimerStart = _host.isActualModernMode() ? 35 : 60;
		_phaseTimer = _phaseTimerStart;
		_safePadBeaconEnhancementsEnabled = _host.Manager.getPlugin().getConfig()
				.getBoolean("monstermaze.safe-pad-enhancements.enabled", false);
		
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
		
		spawnSafePad();
		_safePad = _nextSafePad;
		_nextSafePad = null;
		_initialSafePadCreatedAt = System.currentTimeMillis();
		
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
		
		if (_safePad == null)
			return;
		
		for (Player player : _host.GetPlayers(true))
		{
			player.setCompassTarget(_safePad.getLocation());
		}
	}
	
	@EventHandler
	public void updateTick(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;
		
		if (!_host.IsLive())
			return;
		
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

				//Damage Event
				_host.Manager.GetDamage().NewDamageEvent(player, null, null, 
						null, 4, false, false, false,
						"Monster", "Monster Attack");

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

	private void registerMobDisguise(Snowman entity, EntityType mobType, final Set<UUID> viewerIds)
	{
		if (mobType == null || mobType == EntityType.SNOWMAN)
			return;

		DisguiseBase disguise = DisguiseFactory.createDisguise(entity, mobType);
		if (disguise instanceof DisguiseSlime)
			((DisguiseSlime) disguise).SetSize(3);
		if (disguise instanceof DisguiseMagmaCube)
			((DisguiseMagmaCube) disguise).SetSize(3);
		if (disguise == null)
		{
			_host.Manager.GetPlugin().getLogger().warning("Monster Maze has no disguise implementation for " + mobType + ".");
			return;
		}

		if (viewerIds == null)
			_host.Manager.GetDisguise().disguise(disguise);
		else
			_host.Manager.GetDisguise().disguise(disguise, viewer -> viewerIds.contains(viewer.getUniqueId()));
	}

	private EntityType getMobTypeForPlayer(Player player)
	{
		String mobSet = getMobSetForPlayer(player);
		EntityType selected = mobSet == null ? null : _viewerMobTypes.get(player.getUniqueId());
		return selected == null ? _host.getMonsterType() : selected;
	}

	/** Returns this viewer's selected mob set, fixed for one game run. */
	public String getMobSetForPlayer(Player player)
	{
		if (player == null)
			return null;

		UUID playerId = player.getUniqueId();
		if (_viewerMobSetIds.containsKey(playerId))
			return _viewerMobSetIds.get(playerId);

		List<String> enabled = ArcadeCosmeticsMenu.getEnabledMonsterMazeMobSets(playerId);
		if (enabled == null || enabled.isEmpty())
		{
			_viewerMobSetIds.put(playerId, null);
			_viewerMobTypes.put(playerId, _host.getMonsterType());
			return null;
		}

		String selected = enabled.get(UtilMath.r(enabled.size()));
		EntityType type = entityTypeForMobSet(selected);
		if (type == null)
		{
			_viewerMobSetIds.put(playerId, null);
			_viewerMobTypes.put(playerId, _host.getMonsterType());
			return null;
		}

		_viewerMobSetIds.put(playerId, selected);
		_viewerMobTypes.put(playerId, type);
		return selected;
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
			for (Player p : _host.GetPlayers(true))
			{
				if(_safePad.isOn(p))
				{
					UtilTextMiddle.display("", C.cYellow + C.Bold + "Get to the Next Safe Pad!", 5, 40, 5, p);
					// maybe send them a happy message? =)
					//					UtilPlayer.message(p, F.main("Game", "Since you were on the Safe Pad, you didn't die!"));
				}
				else
				{
					_host.Manager.GetDamage().NewDamageEvent(p, null, null, 
							DamageCause.CUSTOM, 500, false, false, false,
							"Game", "Map damage");
					UtilTextMiddle.display("", C.cRed + C.Bold + "You weren't on the Safe Pad!", 5, 40, 5, p);
					UtilPlayer.message(p, F.main("Game", "You weren't on the Safe Pad!"));
				}
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
					_ents.remove(_ents.get(e));
					it.remove();
					e.remove();
				}
			}
			
			_curSafe++;
			
			_safePad = _nextSafePad;
			_nextSafePad = null;

			
			
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
		for(Location loc : _playerContainmentGlass)
		{
			loc.getBlock().setType(Material.AIR);
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
	
	@EventHandler
	public void onGameStateDead(GameStateChangeEvent event)
	{
		if (event.GetState() != GameState.Dead)
			return;

		// The arena/world may be reused by another round. Remove every mob and restore
		// the exact Safe Pad block snapshots so entities, ice states, and decayed pads
		// cannot leak into the next game.
		for (LivingEntity entity : new ArrayList<LivingEntity>(_ents.keySet()))
		{
			if (entity != null && entity.isValid())
				entity.remove();
		}
		_ents.clear();
		_frozenUntil.clear();
		_pendingSpawnLocations.clear();
		_playersOnPad.clear();

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
