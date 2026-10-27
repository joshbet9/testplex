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
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.condition.Condition.ConditionType;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
	private final Map<UUID, Integer> _visibilityModeData = new HashMap<UUID, Integer>();
	private final Set<UUID> _visibilityInvisibilityTargets = new HashSet<UUID>();
	private static final int VISIBILITY_SLOT = 7;
	private static final int SAFE_PAD_COMPASS_SLOT = 1;
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
				.setGiveCompass(true)
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

			refreshAllPlayerVisibility();
			_maze.fillSpawn(isActualModernMode() ? 225 : 150);
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
			if (isModernVariant() && _maze != null && _maze.isOnPad(p, true))
				continue; // Speed/QOL variant: jumps on any Safe Pad are free.

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
			List<Player> alive = GetPlayers(true);
			if (alive.size() <= 3)
			{
				for (Player alivePlayer : alive)
					board.write(C.cWhite + alivePlayer.getName());
			}
			else
			{
				board.write(C.cWhite + alive.size() + " Alive");
			}

			board.writeNewLine();
			board.write(C.cGreen + C.Bold + "Safe Pad");
			if (_maze.getSafePad() != null)
				board.write(C.cWhite + _maze.getPhaseTimer() + " Seconds");
			else
				board.write(C.cWhite + "None");

			board.write(C.cAqua + C.Bold + "Distance");
			if (_maze.getSafePad() != null)
			{
				Location pad = _maze.getSafePad().getLocation();
				double dx = viewer.getLocation().getX() - (pad.getX() + 0.5D);
				double dz = viewer.getLocation().getZ() - (pad.getZ() + 0.5D);
				int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
				board.write(C.cWhite + distance + " blocks");
				boolean safe = _maze.getSafePad().isOn(viewer);
				board.write((safe ? C.cGreen : C.cRed) + (safe ? "SAFE" : "UNSAFE"));
			}
			else
			{
				board.write(C.cWhite + "Unknown");
				board.write(C.cRed + "UNSAFE");
			}

			board.writeNewLine();
			board.write(C.cGold + C.Bold + "Stage");
			board.write(C.cWhite + _maze.getCurrentSafePadCount());
			board.write(C.cAqua + C.Bold + "Maze Pattern");
			board.write(C.cWhite + _maze.getPreset().getPatternId());
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
		for (Team team : board.getScoreboard().getTeams())
			team.setCanSeeFriendlyInvisibles(true);

		Team ghosts = ensureGhostTeam(board);
		int modeData = getVisibilityModeData(viewer.getUniqueId());
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
		ItemStack compass = player.getInventory().getItem(SAFE_PAD_COMPASS_SLOT);
		if (compass == null)
		{
			ItemStack locator = new ItemStack(Material.COMPASS, 1);
			ItemMeta meta = locator.getItemMeta();
			meta.setDisplayName(SAFE_PAD_COMPASS_NAME);
			meta.setLore(java.util.Collections.singletonList(ChatColor.GRAY + "Points to the active Safe Pad"));
			locator.setItemMeta(meta);
			player.getInventory().setItem(SAFE_PAD_COMPASS_SLOT, locator);
		}

		int modeData = getVisibilityModeData(player.getUniqueId());
		ItemStack visibility = player.getInventory().getItem(VISIBILITY_SLOT);
		if (visibility == null)
			player.getInventory().setItem(VISIBILITY_SLOT, createVisibilityItem(modeData));
	}

	private boolean isSafePadCompassItem(ItemStack item)
	{
		if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		return meta != null && SAFE_PAD_COMPASS_NAME.equals(meta.getDisplayName());
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

	/** Keep the per-viewer visible/hidden/ghost view matrix in sync with the global invisibility effect. */
	private void refreshVisibilityViews()
	{
		List<Player> players = GetPlayers(false);
		Set<UUID> needInvisibility = new HashSet<UUID>();
		for (Player observer : players)
		{
			if (getVisibilityModeData(observer.getUniqueId()) != 5)
				continue;
			for (Player target : players)
				if (!observer.equals(target))
					needInvisibility.add(target.getUniqueId());
		}

		for (Player target : players)
		{
			UUID targetId = target.getUniqueId();
			if (needInvisibility.contains(targetId))
			{
				target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 1, true, false), true);
				_visibilityInvisibilityTargets.add(targetId);
			}
			else if (_visibilityInvisibilityTargets.remove(targetId))
			{
				target.removePotionEffect(PotionEffectType.INVISIBILITY);
			}
		}

		for (Player observer : players)
		{
			int modeData = getVisibilityModeData(observer.getUniqueId());
			GameScoreboard board = getViewerScoreboard(observer);
			Team ghosts = ensureGhostTeam(board);
			for (Player target : players)
			{
				if (observer.equals(target))
					continue;
				if (modeData == 8)
					observer.hidePlayer(target);
				else
					observer.showPlayer(target);
				if (modeData == 5)
					addGhostEntry(ghosts, target);
				else
					ghosts.removeEntry(target.getName());
			}
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
		GameTeam targetTeam = GetTeam(target);
		if (targetTeam != null && targetTeam.GetDisplaytag())
		{
			ghosts.setPrefix(targetTeam.GetColor() + C.Bold + targetTeam.GetName() + targetTeam.GetColor() + " ");
			ghosts.setSuffix(C.Reset);
		}
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
					if (!viewer.equals(target))
						viewer.showPlayer(target);
		}

		for (UUID id : new ArrayList<UUID>(_visibilityInvisibilityTargets))
		{
			Player player = Bukkit.getPlayer(id);
			if (player != null && player.isOnline())
				player.removePotionEffect(PotionEffectType.INVISIBILITY);
		}
		_visibilityInvisibilityTargets.clear();

		for (Map.Entry<UUID, org.bukkit.scoreboard.Scoreboard> entry : _previousScoreboards.entrySet())
		{
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null || !player.isOnline())
				continue;
			if (entry.getValue() != null)
				player.setScoreboard(entry.getValue());
			ItemStack visibility = player.getInventory().getItem(VISIBILITY_SLOT);
			if (isVisibilityItem(visibility))
				player.getInventory().setItem(VISIBILITY_SLOT, null);
			ItemStack compass = player.getInventory().getItem(SAFE_PAD_COMPASS_SLOT);
			if (isSafePadCompassItem(compass))
				player.getInventory().setItem(SAFE_PAD_COMPASS_SLOT, null);
		}
		_viewerScoreboards.clear();
		_previousScoreboards.clear();
		_visibilityModeData.clear();
	}
}
