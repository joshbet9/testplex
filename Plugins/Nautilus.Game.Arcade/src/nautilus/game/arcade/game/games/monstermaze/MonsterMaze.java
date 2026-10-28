package nautilus.game.arcade.game.games.monstermaze;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;

import mineplex.core.common.util.C;
import mineplex.core.common.util.UtilBlock;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilInv;
import mineplex.core.common.util.UtilTextMiddle;
import mineplex.core.common.util.UtilTime;
import mineplex.core.itemstack.ItemStackFactory;
import mineplex.core.recharge.Recharge;
import mineplex.core.updater.UpdateType;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.MobEffectList;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutRemoveEntityEffect;
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.condition.Condition.ConditionType;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.GameTeam;
import nautilus.game.arcade.game.SoloGame;
import nautilus.game.arcade.game.games.monstermaze.MMMazes.MazePreset;
import nautilus.game.arcade.game.games.monstermaze.MazeBlockData.MazeBlock;
import nautilus.game.arcade.game.games.monstermaze.events.AbilityUseEvent;
import nautilus.game.arcade.game.games.monstermaze.events.MonsterBumpPlayerEvent;
import nautilus.game.arcade.game.games.monstermaze.kits.KitBodyBuilder;
import nautilus.game.arcade.game.games.monstermaze.kits.KitJumper;
import nautilus.game.arcade.game.games.monstermaze.kits.KitJumperModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitJumperSpeed;
import nautilus.game.arcade.game.games.monstermaze.kits.KitBodyBuilderModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitBodyBuilderSpeed;
import nautilus.game.arcade.game.games.monstermaze.kits.KitMaverick;
import nautilus.game.arcade.game.games.monstermaze.kits.KitMaverickSpeed;
import nautilus.game.arcade.game.games.monstermaze.kits.KitRepulsorModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitRepulsorSpeed;
import nautilus.game.arcade.game.games.monstermaze.kits.KitSlowballModern;
import nautilus.game.arcade.game.games.monstermaze.kits.KitSlowballSpeed;
import nautilus.game.arcade.game.games.monstermaze.kits.KitRepulsor;
import nautilus.game.arcade.game.games.monstermaze.kits.KitSlowball;
import nautilus.game.arcade.game.games.monstermaze.trackers.AbilityUseTracker;
import nautilus.game.arcade.game.games.monstermaze.trackers.FirstToSafepadTracker;
import nautilus.game.arcade.game.games.monstermaze.trackers.PilotTracker;
import nautilus.game.arcade.game.games.monstermaze.trackers.SnowmanHitTracker;
import nautilus.game.arcade.game.games.monstermaze.trackers.SurvivePast10thSafepadTracker;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.managers.chat.ChatStatData;
import nautilus.game.arcade.scoreboard.GameScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class MonsterMaze extends SoloGame
{
	private Maze _maze;
	private EntityType _monsterType;
	
	private MazePreset _preset;
	
	private Location _center;

	// Each player needs their own sidebar because distance and Safe Pad status are viewer-specific.
	private final Map<UUID, GameScoreboard> _viewerScoreboards = new HashMap<UUID, GameScoreboard>();
	private final Map<UUID, org.bukkit.scoreboard.Scoreboard> _previousScoreboards = new HashMap<UUID, org.bukkit.scoreboard.Scoreboard>();
	private final Map<UUID, ItemStack> _previousCompassItems = new HashMap<UUID, ItemStack>();
	private final Map<UUID, ItemStack> _previousMobThemeItems = new HashMap<UUID, ItemStack>();
	private final Map<UUID, ItemStack> _previousVisibilityItems = new HashMap<UUID, ItemStack>();
	private final Map<UUID, Integer> _visibilityModeData = new HashMap<UUID, Integer>();
	private static final int VISIBILITY_SLOT = 7;
	private static final int MOB_THEME_SLOT = 6;
	private static final String MOB_THEME_ITEM_NAME = "Mob Theme:";
	private static final int SAFE_PAD_COMPASS_SLOT = 4;
	private static final int LEGACY_DUPLICATE_COMPASS_SLOT = 1;
	private static final String VISIBILITY_ITEM_NAME = "Player Visibility:";
	private static final String SAFE_PAD_COMPASS_NAME = ChatColor.GREEN + "Safe Pad Locator";
	
	private HashMap<Player, Long> _launched = new HashMap<Player, Long>();
	// Retain the bump window even after the velocity/grounded tracker clears.
	private HashMap<Player, Long> _recentMonsterBump = new HashMap<Player, Long>();
	
	private static final int JUMP_POTION_AMPLIFIER = -10;
	private int _potionMult = JUMP_POTION_AMPLIFIER;
	private final MonsterMazeMode _mode;
		
	@SuppressWarnings("unchecked")
	public MonsterMaze(ArcadeManager manager)
	{
		this(manager, GameType.MonsterMaze, MonsterMazeMode.ORIGINAL);
	}

	/**
	 * Shared native Reborn game lifecycle. The modern variant differs by its
	 * kit roster and enhanced mechanics; original Monster Maze keeps its legacy kit roster.
	 */
	protected MonsterMaze(ArcadeManager manager, GameType gameType, MonsterMazeMode mode)
	{
		super(manager, gameType,
				mode == MonsterMazeMode.ORIGINAL
						? new Kit[]
								{
									new KitJumper(manager),
									new KitSlowball(manager),
									new KitBodyBuilder(manager),
									new KitRepulsor(manager)
								}
						: mode == MonsterMazeMode.SPEED
								? new Kit[]
									{
										new KitJumperSpeed(manager),
										new KitSlowballSpeed(manager),
										new KitBodyBuilderSpeed(manager),
										new KitRepulsorSpeed(manager),
										new KitMaverickSpeed(manager)
									}
								: new Kit[]
									{
										new KitJumperModern(manager),
										new KitSlowballModern(manager),
										new KitBodyBuilderModern(manager),
										new KitRepulsorModern(manager),
										new KitMaverick(manager)
									},
				new String[]
							{
								"Run over the maze and don't fall off,",
								"but make sure you avoid the monsters!",
								"Make it to a Safe Pad or be killed!"
							});

		_mode = mode;
		DamagePvP = false;
		DamagePvE = false;
		DamageFall = false;
		HungerSet = 20;
		PrepareFreeze = false;

		registerStatTrackers(
				new SnowmanHitTracker(this),
				new AbilityUseTracker(this),
				new FirstToSafepadTracker(this),
				new PilotTracker(this),
				new SurvivePast10thSafepadTracker(this)
		);

		registerChatStats(
				new ChatStatData("Ninja", "Times Hit", true),
				new ChatStatData("Speed", "First to safe pad", true),
				BlankLine,
				new ChatStatData("kit", "Kit", true)
		);

		new CompassModule()
				.setGiveCompass(false)
				.setGiveCompassToSpecs(true)
				.setGiveCompassToAlive(false)
				.register(this);
	}

	public Maze getMaze()
	{
		return _maze;
	}

	public boolean isModernVariant()
	{
		return _mode != MonsterMazeMode.ORIGINAL;
	}

	/** True only for Modern pacing/spawn rules; Speed keeps original pacing and mob counts. */
	public boolean isActualModernMode()
	{
		return _mode == MonsterMazeMode.MODERN;
	}

	public MonsterMazeMode getMazeMode()
	{
		return _mode;
	}

	/** Refill the enhanced Jumper kit when its player reaches a Safe Pad. */
	public void resetModernJumperCharges(Player player)
	{
		if (isModernVariant() && GetKit(player) instanceof KitJumperModern)
			setJumpsLeft(player, 3);
	}

	@Override
	public void generateTeams()
	{
		super.generateTeams();

		// Select the procedural maze before Reborn recruits players. Team spawns are
		// consumed during the normal game-start path, so assigning these in Recruit
		// leaves players at the map's decorative anchor instead of on the start tiles.
		selectMazePreset();

		if (GetTeamList().isEmpty())
		{
			Location fallback = getCentralStartSpawn();
			if (fallback == null)
				fallback = WorldData != null && WorldData.World != null ? WorldData.World.getSpawnLocation() : null;
			if (fallback != null)
			{
				ArrayList<Location> spawns = new ArrayList<Location>();
				spawns.add(fallback);
				AddTeam(new GameTeam(this, "Players", ChatColor.YELLOW, spawns));
				Bukkit.getLogger().warning("[MonsterMaze] Map has no TEAM_SPAWNS metadata; created a fallback team.");
			}
			else
			{
				Bukkit.getLogger().severe("[MonsterMaze] Cannot create a fallback team because the map world is unavailable.");
			}
		}

		Location centralSpawn = getCentralStartSpawn();
		if (centralSpawn != null && !GetTeamList().isEmpty())
		{
			GameTeam playerTeam = GetTeamList().get(0);
			playerTeam.SetColor(ChatColor.YELLOW);
			playerTeam.SetName("Players");
			playerTeam.GetSpawns().clear();
			playerTeam.GetSpawns().add(centralSpawn.clone());
		}
	}

	/**
	 * Pick the map's gameplay maze once per game instance. Both team placement and
	 * Recruit/maze construction use this exact preset so their coordinates cannot
	 * disagree, and random mode cannot silently re-roll after players are placed.
	 */
	private boolean selectMazePreset()
	{
		if (_preset != null)
		{
			if (_center == null && _preset.getCenter() != null)
				_center = _preset.getCenter().clone();
			return _preset.getSpawns() != null && !_preset.getSpawns().isEmpty() && getCentralStartSpawn() != null;
		}

		if (WorldData == null || WorldData.World == null)
		{
			Bukkit.getLogger().severe("[MonsterMaze] Cannot select a maze because the map world is unavailable.");
			return false;
		}

		List<Location> centres = WorldData.GetDataLocs("ORANGE");
		if (centres == null || centres.isEmpty())
		{
			Bukkit.getLogger().severe("[MonsterMaze] The selected map has no DATA_NAME:ORANGE centre.");
			return false;
		}

		_center = centres.get(0).clone();
		int requestedPattern = Manager.getPlugin().getConfig().getInt("monstermaze.pattern", 0);
		_preset = requestedPattern >= 1 && requestedPattern <= 3
				? MMMazes.getPatternMapPreset(_center, getMazeBlockData(), requestedPattern)
				: MMMazes.getRandomMapPreset(_center, getMazeBlockData());

		if (_preset == null || _preset.getSpawns() == null || _preset.getSpawns().isEmpty())
		{
			Bukkit.getLogger().severe("[MonsterMaze] The selected maze preset has no player spawn tiles.");
			return false;
		}
		return getCentralStartSpawn() != null;
	}

	/** The central orange marker is the gameplay start; preset spawn markers are elsewhere in the maze. */
	private Location getCentralStartSpawn()
	{
		Location center = _preset != null ? _preset.getCenter() : _center;
		if (center == null || center.getWorld() == null)
			return null;
		Location spawn = center.clone();
		spawn.setX(center.getBlockX() + 0.5D);
		spawn.setY(center.getBlockY());
		spawn.setZ(center.getBlockZ() + 0.5D);
		return spawn;
	}

	@EventHandler
	public void GameStateChange(GameStateChangeEvent event)
	{
		if(event.GetState() == GameState.Live)
		{	
			_maze.removePlayerContainmentUnit();
			
			UtilTextMiddle.display("", C.cYellow + C.Bold + "Get to the Safe Pad!", 5, 40, 5);

			for (Team team : GetScoreboard().getScoreboard().getTeams())
				team.setCanSeeFriendlyInvisibles(true);
			refreshAllPlayerVisibility();
		}
		else if (event.GetState() == GameState.Dead)
		{
			clearPersonalScoreboardsAndVisibility();
		}
		else if (event.GetState() == GameState.Recruit)
		{
			// Reuse the preset selected before Recruit, so the team spawn locations
			// and the geometry we build here are guaranteed to describe the same maze.
			if (!selectMazePreset())
			{
				Bukkit.getLogger().severe("[MonsterMaze] Could not prepare a valid gameplay maze. Aborting safely.");
				SetState(GameState.Dead);
				return;
			}

			_monsterType = loadEntityType();
			// Clear entities saved in the Mineplex map archive before each generated round.
			clearStaleArenaEntities(WorldData.World);
			_preset.build();
			_maze = new Maze(this, _preset);

			// Use the actual central start tile, not the maze-generation spawn markers.
			Location centralSpawn = getCentralStartSpawn();
			if (centralSpawn == null)
			{
				Bukkit.getLogger().severe("[MonsterMaze] Central maze start is unavailable; aborting this round.");
				SetState(GameState.Dead);
				return;
			}
			if (GetTeamList().isEmpty())
			{
				ArrayList<Location> fallbackSpawns = new ArrayList<Location>();
				fallbackSpawns.add(centralSpawn.clone());
				AddTeam(new GameTeam(this, "Players", ChatColor.YELLOW, fallbackSpawns));
			}
			GameTeam playerTeam = GetTeamList().get(0);
			playerTeam.SetColor(ChatColor.YELLOW);
			playerTeam.SetName("Players");
			playerTeam.GetSpawns().clear();
			playerTeam.GetSpawns().add(centralSpawn.clone());

			// Reborn may have teleported players before the generated maze was built. Re-seat
			// everyone at the centre after the floor and safe-zone walls are ready.
			for (Player player : GetPlayers(false))
			{
				player.teleport(centralSpawn);
				player.setVelocity(new Vector(0, 0, 0));
			}

			// Ensure both gameplay controls exist before the Recruit countdown/first scoreboard tick.
			for (Player player : GetPlayers(false))
				ensureGameplayItems(player);
			refreshAllPlayerVisibility();
			_maze.fillSpawn(isActualModernMode() ? 225 : 150);
		}
	}
	

	/** Eliminate a player who falls under the maze; ordinary fall damage is disabled for this game. */
	@EventHandler
	public void onMonsterMazePlayerMove(PlayerMoveEvent event)
	{
		if (!IsLive() || _center == null || event.getTo() == null)
			return;
		Player player = event.getPlayer();
		if (!IsAlive(player))
			return;

		// Once a player has reached the current Safe Pad, stepping off its edge sends
		// them back to the centre start. Preserve the horizontal direction they walked
		// off in so they immediately face the direction of travel on arrival.
		if (_maze != null && _maze.getSafePad() != null && _maze.hasReachedCurrentSafePad(player)
				&& event.getFrom() != null && event.getTo() != null
				&& _maze.getSafePad().isOn(event.getFrom()) && !_maze.getSafePad().isOn(event.getTo()))
		{
			double dx = event.getTo().getX() - event.getFrom().getX();
			double dz = event.getTo().getZ() - event.getFrom().getZ();
			Location centre = getCentralStartSpawn();
			if (centre != null)
			{
				if (Math.abs(dx) + Math.abs(dz) > 0.0001D)
					centre.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
				centre.setPitch(0.0F);
				player.teleport(centre);
				player.setVelocity(new Vector(0, 0, 0));
				return;
			}
		}

		if (_center == null || event.getTo().getY() >= _center.getY() - 3.0D)
			return;
		Manager.GetDamage().NewDamageEvent(player, null, null,
				EntityDamageEvent.DamageCause.CUSTOM, 500, false, false, false,
				"Game", "Fell off Monster Maze");
	}

	/** Clear all non-player entities saved in the active arena world before each round. */
	private void clearStaleArenaEntities(org.bukkit.World world)
	{
		if (world == null)
			return;
		int removed = 0;
		for (Entity entity : new ArrayList<Entity>(world.getEntities()))
		{
			if (entity instanceof Player)
				continue;
			entity.remove();
			removed++;
		}
		if (removed > 0)
			Bukkit.getLogger().info("[MonsterMaze] Cleared " + removed + " stale entities from arena world " + world.getName());
	}

	/** Also sweep lazy-loaded map chunks so archived entities cannot reappear mid-round. */
	@EventHandler
	public void onMonsterMazeChunkLoad(ChunkLoadEvent event)
	{
		if (WorldData == null || WorldData.World == null || event.getWorld() != WorldData.World)
			return;
		Location centre = getCentralStartSpawn();
		if (centre == null)
			return;
		int minX = centre.getBlockX() - 52, maxX = centre.getBlockX() + 52;
		int minZ = centre.getBlockZ() - 52, maxZ = centre.getBlockZ() + 52;
		int chunkMinX = event.getChunk().getX() << 4, chunkMaxX = chunkMinX + 15;
		int chunkMinZ = event.getChunk().getZ() << 4, chunkMaxZ = chunkMinZ + 15;
		if (chunkMaxX < minX || chunkMinX > maxX || chunkMaxZ < minZ || chunkMinZ > maxZ)
			return;
		for (Entity entity : event.getChunk().getEntities())
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

	private EntityType loadEntityType()
	{
		EntityType en = EntityType.SNOWMAN;
		
		for (String key : WorldData.GetAllCustomLocs().keySet())
		{
			try
			{
				if (key.startsWith("E"))
				{
					en = EntityType.valueOf(key.split(Pattern.quote("="))[1].toUpperCase());
				}
			}
			catch (Exception ex)
			{
				
			}
		}
		return en;
	}
	
	@SuppressWarnings("deprecation")
	private MazeBlockData getMazeBlockData()
	{
		MazeBlock top = null;
		MazeBlock mid = null;
		MazeBlock bottom = null;
		for (String key : WorldData.GetAllCustomLocs().keySet())
		{
			try
			{
				if (key.startsWith("B1"))
				{
					String[] typeData = key.split(Pattern.quote("="))[1].split(Pattern.quote(","));
					top = new MazeBlock(Material.getMaterial(Integer.valueOf(typeData[0])), Byte.valueOf(typeData[1]));
				}
				else if (key.startsWith("B2"))
				{
					String[] typeData = key.split(Pattern.quote("="))[1].split(Pattern.quote(","));
					mid = new MazeBlock(Material.getMaterial(Integer.valueOf(typeData[0])), Byte.valueOf(typeData[1]));
				}
				else if (key.startsWith("B3"))
				{
					String[] typeData = key.split(Pattern.quote("="))[1].split(Pattern.quote(","));
					bottom = new MazeBlock(Material.getMaterial(Integer.valueOf(typeData[0])), Byte.valueOf(typeData[1]));
				}
			}
			catch (Exception ex)
			{

			}
		}
		
		if (top != null && mid != null && bottom != null)
		{
			return new MazeBlockData(top, mid, bottom);
		}
		else
		{
			return new MazeBlockData(new MazeBlock(Material.QUARTZ_BLOCK), new MazeBlock(Material.QUARTZ_BLOCK, (byte) 2), new MazeBlock(Material.STEP, (byte) 15));
		}
	}
	
	private void setJumpsLeft(Player player, int jumps)
	{
		if (jumps <= 0)
		{
			player.getInventory().setItem(8, null);
		}
		else
		{
			player.getInventory().setItem(8, ItemStackFactory.Instance.CreateStack(Material.FEATHER, (byte)0, jumps, C.cYellow + C.Bold + jumps + " Jumps Remaining"));					
		}
	}
	
	@EventHandler
	public void jumpEvent(UpdateEvent event)
	{
		if(event.getType() != UpdateType.TICK) 
			return;
		
		if (!IsLive()) 
			return;
		
		for (Player p : GetPlayers(true))
		{
			if (_maze != null && _maze.isOnPad(p, true))
				continue; // Jumping on any Safe Pad is free in all three native variants.

			if (!UtilInv.contains(p, "Jumps Remaining", Material.FEATHER, (byte) 0, 1) || p.getLocation().getY()-_center.getY() <= 0 || !Recharge.Instance.usable(p, "MM Player Jump") || isLaunched(p))
				continue;
			
			setJumpsLeft(p, p.getInventory().getItem(8).getAmount() - 1);
						
			p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 1.0f, 1.0f);
			
			Recharge.Instance.useForce(p, "MM Player Jump", 750);
			
			//Find blocks below a player
			for (int i = 0 ; i < 3 ; i++)
			{
				Block under = p.getLocation().clone().subtract(0, i, 0).getBlock();
				
				if (under.getType() == Material.AIR)
					continue;
								
				under.getWorld().playEffect(under.getLocation(), Effect.STEP_SOUND, UtilBlock.getStepSoundId(under));
				
				for (Block block : UtilBlock.getSurrounding(under, true))
				{
					if (block.getType() == Material.AIR)
						continue;
					
					block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, UtilBlock.getStepSoundId(block));
				}
				
				break;
			}
						
			Bukkit.getPluginManager().callEvent(new AbilityUseEvent(p));
		}
	}
	
	@EventHandler
	public void onDebug(PlayerCommandPreprocessEvent event)
	{
		if (!event.getPlayer().isOp())
			return;
		
		if (event.getMessage().toLowerCase().contains("/setmult "))
		{
			event.setCancelled(true);
			Integer mult = Integer.parseInt(event.getMessage().toLowerCase().replace("/setmult ", ""));
			_potionMult = mult;
			
			for (Player pl : GetPlayers(true))
			{
				Manager.GetCondition().Clean(pl);
			}
		}
	}
	
	@EventHandler
	public void onBreakJumper(InventoryClickEvent event)
	{
		if (!InProgress())
			return;
		if (!IsAlive(event.getWhoClicked()))
			return;
		
		event.setCancelled(true);
	}
	
	@EventHandler
	public void PotionEffects(UpdateEvent event)
	{
		if(event.getType() != UpdateType.TICK) return;
		if(!InProgress()) return;

		for(Player pl : GetPlayers(true))
		{
			if (IsLive() && (UtilInv.contains(pl, Material.FEATHER, (byte) 0, 1)))
			{
				while (Manager.GetCondition().GetActiveCondition(pl, ConditionType.JUMP) != null)
					Manager.GetCondition().GetActiveCondition(pl, ConditionType.JUMP).Expire();
			}
			else
			{
				if (!Manager.GetCondition().HasCondition(pl, ConditionType.JUMP, null))
					Manager.GetCondition().Factory().Jump("No jumping", pl, null, 9999999, _potionMult, true, false, false);
			}

//			if (!Manager.GetCondition().HasCondition(pl, ConditionType.INVISIBILITY, null))
//				Manager.GetCondition().Factory().Invisible("Hide players", pl, null, 9999999, 2, true, false, false);
		}
	}
	
	public EntityType getMonsterType()
	{
		return _monsterType;
	}
	
	@EventHandler
	public void onPlayerBump(MonsterBumpPlayerEvent event)
	{
		if (!IsLive())
			return;
		
		long now = System.currentTimeMillis();
		_launched.put(event.getPlayer(), now);
		_recentMonsterBump.put(event.getPlayer(), now);
	}
	
	@EventHandler
	public void tickBumps(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK)
			return;
		
		if (!IsLive())
		{
			_launched.clear();
			_recentMonsterBump.clear();
			return;
		}

		Iterator<Map.Entry<Player, Long>> recent = _recentMonsterBump.entrySet().iterator();
		while (recent.hasNext())
		{
			Map.Entry<Player, Long> entry = recent.next();
			Player player = entry.getKey();
			if (player == null || !player.isOnline()
					|| UtilTime.elapsed(entry.getValue(), 2000))
				recent.remove();
		}

		HashMap<Player, Long> copy = new HashMap<Player, Long>();
		copy.putAll(_launched);
		
		for (Iterator<Player> iterator = copy.keySet().iterator() ; iterator.hasNext() ;)
		{
			Player player = iterator.next();
			
			if (player == null || !player.isOnline())
			{
				_launched.remove(player);
				continue;
			}
			
			if (UtilEnt.isGrounded(player) && UtilTime.elapsed(copy.get(player), 500))
			{
				_launched.remove(player);
				continue;
			}
			
			//If there are blocks surrounding the block it's on top of (if it's on the side of a block)
			if (player.getLocation().getY() == player.getLocation().getBlockY() && !UtilBlock.getInBoundingBox(player.getLocation().clone().add(1, -1, 1), player.getLocation().clone().subtract(1, 1, 1), true).isEmpty() && UtilTime.elapsed(copy.get(player), 500))
			{
				_launched.remove(player);
				continue;
			}
			
			//Time out
			if (UtilTime.elapsed(copy.get(player), 3000))
			{
				_launched.remove(player);
				continue;
			}
		}
	}
	
	public boolean isLaunched(Player player)
	{
		if (_launched.containsKey(player))
			return true;

		Long bumpedAt = _recentMonsterBump.get(player);
		return bumpedAt != null && !UtilTime.elapsed(bumpedAt, 2000);
	}
	
	@EventHandler
	public void onRegain(EntityRegainHealthEvent event)
	{
		if (event.getRegainReason() == RegainReason.SATIATED)
		{
			event.setCancelled(true);
		}
	}
	
	//Fix for eye of ender
	@EventHandler
	public void onDamage(EntityDamageEvent event)
	{
		if (event.getEntity() instanceof EnderCrystal)
		{
			event.setCancelled(true);
		}
	}
	
	@Override
	@EventHandler
	public void ScoreboardUpdate(UpdateEvent event)
	{
		if (event.getType() != UpdateType.FAST || _maze == null || GetTeamList().isEmpty())
			return;

		for (Player viewer : GetPlayers(false))
		{
			ensureGameplayItems(viewer);
			GameScoreboard board = getViewerScoreboard(viewer);
			if (viewer.getScoreboard() != board.getScoreboard())
				viewer.setScoreboard(board.getScoreboard());
			syncViewerScoreboardTeams(board, viewer);

			board.reset();
			board.write(C.cYellow + C.Bold + "Players");
			List<Player> alive = new ArrayList<Player>(GetPlayers(true));
			// Keep the viewer's own line at the top; every entry carries its own pad distance
			// and SAFE marker instead of adding a separate Distance section.
			if (alive.remove(viewer))
				alive.add(0, viewer);
			SafePad activePad = _maze.getSafePad();
			int shown = Math.min(9, alive.size());
			for (int i = 0; i < shown; i++)
			{
				Player target = alive.get(i);
				String distanceText = "Unknown";
				if (activePad != null && target.getWorld() == activePad.getLocation().getWorld())
				{
					Location pad = activePad.getLocation();
					double dx = target.getLocation().getX() - (pad.getX() + 0.5D);
					double dz = target.getLocation().getZ() - (pad.getZ() + 0.5D);
					int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
					distanceText = distance + " blocks";
					if (activePad.isOn(target))
						distanceText += " " + C.cGreen + C.Bold + "SAFE";
				}
				board.write(C.cWhite + target.getName() + C.cGray + " " + distanceText);
			}
		if (alive.size() > shown)
			board.write(C.cGray + "+ " + (alive.size() - shown) + " more");

			board.write(C.cGreen + C.Bold + "Safe Pad: " + C.cWhite
					+ (activePad == null ? "None" : _maze.getPhaseTimer() + "s"));
			board.write(C.cGold + C.Bold + "Stage: " + C.cWhite + _maze.getCurrentSafePadCount());
			int pattern = _maze.getPreset() == null ? 0 : _maze.getPreset().getPatternId();
			board.write(C.cAqua + C.Bold + "Maze Pattern: " + C.cWhite + pattern);
			int patternPb = MonsterMazePersonalBestManager.getKitBest(this, viewer, pattern);
			board.write(C.cAqua + C.Bold + "Pattern PB: " + C.cWhite
					+ (patternPb > 0 ? "Stage " + patternPb : "Not set"));
			if (_maze != null)
				_maze.refreshPlayerSafePadCosmetics(viewer);
			board.draw();

		}
	}

	private GameScoreboard getViewerScoreboard(Player player)
	{
		UUID id = player.getUniqueId();
		GameScoreboard board = _viewerScoreboards.get(id);
		if (board == null)
		{
			_previousScoreboards.put(id, player.getScoreboard());
			board = new GameScoreboard(this, player);
			_viewerScoreboards.put(id, board);
			player.setScoreboard(board.getScoreboard());
		}
		return board;
	}

	private void syncViewerScoreboardTeams(GameScoreboard board, Player viewer)
	{
		for (Player target : GetPlayers(false))
		{
			GameTeam targetTeam = GetTeam(target);
			if (targetTeam != null)
				board.setPlayerTeam(target, targetTeam);
			else
				board.setSpectating(target);
		}
		Team ghosts = ensureGhostTeam(board);
		int modeData = getVisibilityModeData(viewer.getUniqueId());
		for (Team team : board.getScoreboard().getTeams())
			team.setCanSeeFriendlyInvisibles("mm_ghosts".equals(team.getName()));
		if (modeData == 5)
			addGhostEntry(ghosts, viewer);
		else
			ghosts.removeEntry(viewer.getName());
		for (Player target : GetPlayers(false))
		{
			if (target.equals(viewer))
				continue;
			if (modeData == 5)
				addGhostEntry(ghosts, target);
			else
				ghosts.removeEntry(target.getName());
		}
	}

	private void ensureGameplayItems(Player player)
	{
		UUID id = player.getUniqueId();
		// Remove the duplicate locator created by the earlier native-port build.
		ItemStack legacyCompass = player.getInventory().getItem(LEGACY_DUPLICATE_COMPASS_SLOT);
		if (isSafePadCompassItem(legacyCompass))
			player.getInventory().setItem(LEGACY_DUPLICATE_COMPASS_SLOT, null);

		ItemStack themeItem = player.getInventory().getItem(MOB_THEME_SLOT);
		ItemStack expectedThemeItem = createMobThemeItem(player);
		if (!isMobThemeItem(themeItem))
		{
			if (!_previousMobThemeItems.containsKey(id))
				_previousMobThemeItems.put(id, themeItem == null ? null : themeItem.clone());
			player.getInventory().setItem(MOB_THEME_SLOT, expectedThemeItem);
		}
		else if (!themeItem.getItemMeta().getDisplayName().equals(expectedThemeItem.getItemMeta().getDisplayName()))
		{
			player.getInventory().setItem(MOB_THEME_SLOT, expectedThemeItem);
		}

		ItemStack compass = player.getInventory().getItem(SAFE_PAD_COMPASS_SLOT);
		if (!isSafePadCompassItem(compass))
		{
			if (!_previousCompassItems.containsKey(id))
				_previousCompassItems.put(id, compass == null ? null : compass.clone());
			ItemStack locator = new ItemStack(Material.COMPASS, 1);
			ItemMeta meta = locator.getItemMeta();
			meta.setDisplayName(SAFE_PAD_COMPASS_NAME);
			meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Points to the active Safe Pad"));
			locator.setItemMeta(meta);
			player.getInventory().setItem(SAFE_PAD_COMPASS_SLOT, locator);
		}

		int modeData = getVisibilityModeData(id);
		ItemStack visibility = player.getInventory().getItem(VISIBILITY_SLOT);
		if (!isVisibilityItem(visibility))
		{
			if (!_previousVisibilityItems.containsKey(id))
				_previousVisibilityItems.put(id, visibility == null ? null : visibility.clone());
			player.getInventory().setItem(VISIBILITY_SLOT, createVisibilityItem(modeData));
		}
	}

	private boolean isSafePadCompassItem(ItemStack item)
	{
		if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName()
				&& meta.getDisplayName().toLowerCase(java.util.Locale.ROOT).contains("safe pad locator");
	}

	private ItemStack createMobThemeItem(Player player)
	{
		String theme = _maze == null
				? ArcadeCosmeticsMenu.getMonsterMazeSelectedMobTheme(player.getUniqueId())
				: _maze.getMobThemeChoiceForPlayer(player);
		ItemStack item;
		if ("random".equals(theme))
			item = new ItemStack(Material.NETHER_STAR, 1);
		else
		{
			EntityType iconType = _maze == null ? _monsterType : _maze.getMobEntityTypeForPlayer(player);
			if (iconType == null)
				iconType = EntityType.CREEPER;
			int entityId = iconType.getTypeId();
			item = new ItemStack(Material.MONSTER_EGG, 1, (short) (entityId < 0 ? 50 : entityId));
		}
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.LIGHT_PURPLE + MOB_THEME_ITEM_NAME + " " + ChatColor.YELLOW + displayMobThemeName(theme));
		meta.setLore(java.util.Arrays.asList(
				ChatColor.GRAY + "Right-click to cycle mob themes",
				ChatColor.GRAY + "Changes your mob appearance only",
				ChatColor.GRAY + "Safe Pad and beacon colours follow the selected mob set"));
		item.setItemMeta(meta);
		return item;
	}

	private String displayMobThemeName(String theme)
	{
		if (theme == null || "map_default".equals(theme))
			return "Map Default";
		if ("random".equals(theme))
			return "Random Mob";
		StringBuilder label = new StringBuilder();
		for (String word : theme.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').split(" "))
		{
			if (word.length() == 0)
				continue;
			if (label.length() > 0)
				label.append(' ');
			label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return label.toString();
	}

	private boolean isMobThemeItem(ItemStack item)
	{
		if (item == null || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName()
				&& ChatColor.stripColor(meta.getDisplayName()).startsWith(MOB_THEME_ITEM_NAME);
	}

	@EventHandler
	public void onMobThemeCycle(PlayerInteractEvent event)
	{
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if (!isMobThemeItem(event.getItem()))
			return;
		Player player = event.getPlayer();
		if (_maze == null || !GetPlayers(false).contains(player))
			return;
		event.setCancelled(true);
		String selected = _maze.cycleMobThemeForPlayer(player);
		player.getInventory().setItem(MOB_THEME_SLOT, createMobThemeItem(player));
		player.playSound(player.getLocation(), Sound.CLICK, 0.8F, 1.2F);
		player.sendMessage(ChatColor.LIGHT_PURPLE + MOB_THEME_ITEM_NAME + " "
				+ ChatColor.YELLOW + displayMobThemeName(selected)
				+ ChatColor.GRAY + " (personal view).");
	}

	@EventHandler
	public void onMobThemeDrop(PlayerDropItemEvent event)
	{
		if (isMobThemeItem(event.getItemDrop().getItemStack()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onMobThemeInventoryClick(InventoryClickEvent event)
	{
		if (!(event.getWhoClicked() instanceof Player))
			return;
		Player player = (Player) event.getWhoClicked();
		if (isMobThemeItem(event.getCurrentItem()) || isMobThemeItem(event.getCursor())
				|| event.getHotbarButton() == MOB_THEME_SLOT
				|| (event.getClickedInventory() == player.getInventory() && event.getSlot() == MOB_THEME_SLOT))
			event.setCancelled(true);
	}

	private ItemStack createVisibilityItem(int modeData)
	{
		ItemStack item = new ItemStack(Material.INK_SACK, 1, (short) modeData);
		ItemMeta meta = item.getItemMeta();
		String state = modeData == 8 ? ChatColor.GRAY + "Hidden"
				: modeData == 5 ? ChatColor.LIGHT_PURPLE + "Transparent" : ChatColor.GREEN + "Visible";
		meta.setDisplayName(state + " " + VISIBILITY_ITEM_NAME);
		meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Right-click to cycle player visibility"));
		item.setItemMeta(meta);
		return item;
	}

	private boolean isVisibilityItem(ItemStack item)
	{
		if (item == null || item.getType() != Material.INK_SACK || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(VISIBILITY_ITEM_NAME);
	}

	@EventHandler
	public void onVisibilityToggle(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.PHYSICAL || !isVisibilityItem(event.getItem()))
			return;
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		Player player = event.getPlayer();
		if (!GetPlayers(false).contains(player))
			return;

		event.setCancelled(true);
		UUID id = player.getUniqueId();
		int current = getVisibilityModeData(id);
		int next = current == 10 ? 8 : current == 8 ? 5 : 10;
		if (next == 10)
			_visibilityModeData.remove(id);
		else
			_visibilityModeData.put(id, next);
		player.getInventory().setItem(VISIBILITY_SLOT, createVisibilityItem(next));
		refreshAllPlayerVisibility();
		player.playSound(player.getLocation(), Sound.CLICK, 0.8F, next == 8 ? 0.8F : next == 5 ? 1.0F : 1.2F);
	}

	private int getVisibilityModeData(UUID id)
	{
		Integer data = _visibilityModeData.get(id);
		return data == null ? 10 : data.intValue();
	}

	private void refreshAllPlayerVisibility()
	{
		for (Player player : GetPlayers(false))
			ensureGameplayItems(player);
		refreshVisibilityViews();
	}

	/** Apply each view mode only to the observer's client. Per-viewer NMS effect packets avoid the global visibility side effects of Bukkit potion effects. */
	private void refreshVisibilityViews()
	{
		List<Player> players = GetPlayers(false);
		for (Player observer : players)
		{
			if (!observer.isOnline())
				continue;
			int modeData = getVisibilityModeData(observer.getUniqueId());
			GameScoreboard board = getViewerScoreboard(observer);
			Team ghosts = ensureGhostTeam(board);

			if (modeData == 5)
				addGhostEntry(ghosts, observer);
			else
				ghosts.removeEntry(observer.getName());

			for (Player target : players)
			{
				if (observer.equals(target) || !target.isOnline())
					continue;

				if (modeData == 8)
					observer.hidePlayer(target);
				else
					observer.showPlayer(target);

				if (modeData == 5)
					addGhostEntry(ghosts, target);
				else
					ghosts.removeEntry(target.getName());

				sendClientInvisibility(observer, target, modeData == 5);
			}
		}
	}

	/**
	 * Invisibility packets are sent to one client only. That keeps VISIBLE players solid,
	 * HIDDEN players absent, and TRANSPARENT players see-through even when another player
	 * chooses a different mode.
	 */
	private void sendClientInvisibility(Player observer, Player target, boolean transparent)
	{
		try
		{
			if (transparent)
			{
				MobEffect invisible = new MobEffect(MobEffectList.INVISIBILITY.id, 9999999, 1, true, false);
				((CraftPlayer) observer).getHandle().playerConnection.sendPacket(
						new PacketPlayOutEntityEffect(target.getEntityId(), invisible));
			}
			else
			{
				((CraftPlayer) observer).getHandle().playerConnection.sendPacket(
						new PacketPlayOutRemoveEntityEffect(target.getEntityId(), new MobEffect(MobEffectList.INVISIBILITY.id, 1, 0)));
			}
		}
		catch (Exception ex)
		{
			Bukkit.getLogger().warning("[MonsterMaze] Could not update per-viewer ghost appearance for "
					+ observer.getName() + " -> " + target.getName() + ": " + ex.getMessage());
		}
	}

	private Team ensureGhostTeam(GameScoreboard board)
	{
		org.bukkit.scoreboard.Scoreboard scoreboard = board.getScoreboard();
		Team ghosts = scoreboard.getTeam("mm_ghosts");
		if (ghosts == null)
			ghosts = scoreboard.registerNewTeam("mm_ghosts");
		ghosts.setCanSeeFriendlyInvisibles(true);
		return ghosts;
	}

	private void addGhostEntry(Team ghosts, Player target)
	{
		// This team only controls whether invisible teammates render as transparent ghosts.
		// Keeping it free of a shared prefix avoids showing every ghost with one player's tag.
		if (!ghosts.hasEntry(target.getName()))
			ghosts.addEntry(target.getName());
	}

	private void clearPersonalScoreboardsAndVisibility()
	{
		// Undo per-viewer hide state even if the game has already removed players from its teams.
		for (UUID id : new ArrayList<UUID>(_visibilityModeData.keySet()))
		{
			Player viewer = Bukkit.getPlayer(id);
			if (viewer != null && viewer.isOnline())
				for (Player target : Bukkit.getOnlinePlayers())
				{
					if (viewer.equals(target))
						continue;
					if (viewer.getWorld() == target.getWorld() && Integer.valueOf(5).equals(_visibilityModeData.get(id)))
						sendClientInvisibility(viewer, target, false);
					viewer.showPlayer(target);
				}
		}

		for (Map.Entry<UUID, org.bukkit.scoreboard.Scoreboard> entry : _previousScoreboards.entrySet())
		{
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null || !player.isOnline())
				continue;
			if (entry.getValue() != null)
				player.setScoreboard(entry.getValue());
		}
		for (Map.Entry<UUID, ItemStack> entry : _previousMobThemeItems.entrySet())
		{
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null && player.isOnline())
				player.getInventory().setItem(MOB_THEME_SLOT, entry.getValue() == null ? null : entry.getValue().clone());
		}
		for (Map.Entry<UUID, ItemStack> entry : _previousVisibilityItems.entrySet())
		{
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null && player.isOnline())
				player.getInventory().setItem(VISIBILITY_SLOT, entry.getValue() == null ? null : entry.getValue().clone());
		}
		for (Map.Entry<UUID, ItemStack> entry : _previousCompassItems.entrySet())
		{
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null && player.isOnline())
				player.getInventory().setItem(SAFE_PAD_COMPASS_SLOT, entry.getValue() == null ? null : entry.getValue().clone());
		}
		_viewerScoreboards.clear();
		_previousScoreboards.clear();
		_previousCompassItems.clear();
		_previousMobThemeItems.clear();
		_previousVisibilityItems.clear();
		_visibilityModeData.clear();
	}
}
