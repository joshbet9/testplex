package nautilus.game.arcade.game.games.monstermaze;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scoreboard.Team;

public class MonsterMaze extends SoloGame
{
	private Maze _maze;
	private EntityType _monsterType;
	
	private MazePreset _preset;
	
	private Location _center;
	
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

	@EventHandler
	public void GameStateChange(GameStateChangeEvent event)
	{
		if(event.GetState() == GameState.Live)
		{	
			_maze.removePlayerContainmentUnit();
			
			UtilTextMiddle.display("", C.cYellow + C.Bold + "Get to the Safe Pad!", 5, 40, 5);

			for (Team team : GetScoreboard().getScoreboard().getTeams())
				team.setCanSeeFriendlyInvisibles(true);
		}
		else if(event.GetState() == GameState.Recruit)
		{
			_monsterType = loadEntityType();
			_center = WorldData.GetDataLocs("ORANGE").get(0);
			int requestedPattern = Manager.getPlugin().getConfig().getInt("monstermaze.pattern", 0);
			_preset = requestedPattern >= 1 && requestedPattern <= 3
					? MMMazes.getPatternMapPreset(_center, getMazeBlockData(), requestedPattern)
					: MMMazes.getRandomMapPreset(_center, getMazeBlockData());
			_preset.build();
			_maze = new Maze(this, _preset);
			
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
		if (event.getType() != UpdateType.FAST)
			return;

		if (GetTeamList().isEmpty())
			return;

		Scoreboard.reset();

		Scoreboard.writeNewLine();

		Scoreboard.write(C.cYellow + C.Bold + "Players");

		if (GetPlayers(true).size() > 5)
		{	
			Scoreboard.write(C.cWhite + GetPlayers(true).size() + " Alive");
		}
		else
		{
			for (Player p : GetPlayers(true))
			{
				Scoreboard.write(C.cWhite + p.getName());
			}
		}
		
		Scoreboard.writeNewLine();
		
		Scoreboard.write(C.cGreen + C.Bold + "Safe Pad");
		
		if (_maze.getSafePad() != null)
		{
			Scoreboard.write(C.cWhite + _maze.getPhaseTimer() + " Seconds");
		}
		else
		{
			Scoreboard.write("None");
		}
		
		Scoreboard.writeNewLine();
		
		Scoreboard.write(C.cGold + C.Bold + "Stage");
		
		Scoreboard.write(C.cWhite + getMaze().getCurrentSafePadCount());

		Scoreboard.writeNewLine();
		Scoreboard.write(C.cAqua + C.Bold + "Maze Pattern");
		Scoreboard.write(C.cWhite + getMaze().getPreset().getPatternId());
		
		Scoreboard.draw();
	}
}
