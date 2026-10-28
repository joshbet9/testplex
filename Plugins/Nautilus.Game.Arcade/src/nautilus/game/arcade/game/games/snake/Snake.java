package nautilus.game.arcade.game.games.snake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.MagmaCube;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import mineplex.core.common.util.C;
import mineplex.core.common.util.UtilAlg;
import mineplex.core.common.util.UtilBlock;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilGear;
import mineplex.core.common.util.UtilInv;
import mineplex.core.common.util.UtilMath;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilServer;
import mineplex.core.common.util.UtilTime;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.disguise.disguises.DisguiseBase;
import mineplex.core.disguise.disguises.DisguiseSheep;
import mineplex.core.recharge.Recharge;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
import mineplex.minecraft.game.core.damage.CustomDamageEvent;
import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.events.GamePrepareCountdownCommence;
import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.SoloGame;
import nautilus.game.arcade.game.games.snake.events.SlimeUpgradeEvent;
import nautilus.game.arcade.game.games.snake.events.TailGrowEvent;
import nautilus.game.arcade.game.games.snake.kits.KitInvulnerable;
import nautilus.game.arcade.game.games.snake.kits.KitReverser;
import nautilus.game.arcade.game.games.snake.kits.KitSpeed;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.managers.chat.ChatStatData;
import nautilus.game.arcade.stats.ChooChooStatTracker;
import nautilus.game.arcade.stats.KillsWithinGameStatTracker;
import nautilus.game.arcade.stats.SlimySheepStatTracker;

public class Snake extends SoloGame
{
	private double _maxSpeed = 180;

	private HashMap<Player, ArrayList<Creature>> _tail = new HashMap<Player, ArrayList<Creature>>();
	private HashSet<Entity> _food = new HashSet<Entity>();

	private HashMap<Player, DyeColor> _color = new HashMap<Player, DyeColor>();

	private HashMap<Player, Long> _invul = new HashMap<Player, Long>();
	private HashMap<Player, Long> _speed = new HashMap<Player, Long>();
	private HashMap<Player, Long> _reverse = new HashMap<Player, Long>();

	private HashMap<Player, Location> _move = new HashMap<Player, Location>();
	private HashMap<Player, Long> _moveTime = new HashMap<Player, Long>();

	// Mario-Kart-style roulette: one temporary steering debuff per player, rerolled during long rounds.
	private enum ControlDebuff {
		TURN_LAG("Turn Lag", "Turns trail behind your camera input.", Material.WATCH),
		DRIFT("Drift", "Your snake keeps its momentum through turns.", Material.SLIME_BALL),
		OVERSHOOT("Overshoot", "Sharp turns swing further than intended.", Material.FIREWORK),
		WEAK_SHARP_TURN("Weak Sharp Turning", "Quick, sharp turns are less responsive.", Material.SHEARS);

		final String display;
		final String description;
		final Material icon;
		ControlDebuff(String display, String description, Material icon) {
			this.display = display;
			this.description = description;
			this.icon = icon;
		}
	}
	private final HashMap<Player, ControlDebuff> _controlDebuffs = new HashMap<Player, ControlDebuff>();
	private final HashMap<Player, Long> _controlDebuffUntil = new HashMap<Player, Long>();
	private final HashMap<Player, Integer> _rouletteHitCount = new HashMap<Player, Integer>();
	private final HashSet<Player> _rouletteRollsInProgress = new HashSet<Player>();
	private final HashMap<Player, ItemStack> _rouletteOriginalHotbarItems = new HashMap<Player, ItemStack>();
	private int _abilityVisualTick;
	private final HashMap<Player, Float> _lastRawYaw = new HashMap<Player, Float>();
	private final HashMap<Player, Float> _laggedYaw = new HashMap<Player, Float>();
	private final HashMap<Player, Vector> _lastTravelDirection = new HashMap<Player, Vector>();

	// Magma Cubes are physical item-box hazards; each contact starts a player-specific hotbar roll.
	private final HashSet<MagmaCube> _magmaCubes = new HashSet<MagmaCube>();
	private long _snakeLiveStartedAt;
	private long _nextMagmaHazardAt;
	private int _hazardSteeringTick;
	private static final int ROULETTE_HOTBAR_SLOT = 8;
	private static final int ROULETTE_ANIMATION_STEPS = 12;
	private static final int[] ROULETTE_DEBUFF_SECONDS = new int[] {1, 3, 5, 10, 15, 20, 30};
	private static final double MAX_DRIFT_TURN_RADIANS = Math.toRadians(8.0D);
	private static final long FIRST_MAGMA_HAZARD_MS = 10000L;
	private static final long MAGMA_HAZARD_INTERVAL_MS = 15000L;
	private static final int MAX_MAGMA_HAZARDS = 4;

	public Snake(ArcadeManager manager)
	{
		super(manager, GameType.Snake,

				new Kit[]
						{
								new KitSpeed(manager),
								new KitInvulnerable(manager),
								new KitReverser(manager)
						},

				new String[]
						{
								"Avoid hitting snake tails",
								"You get faster as you grow longer",
								"Eat slimes to grow faster",
								"Last one alive wins!"
						});

		this.DamageTeamSelf = true;

		this.HungerSet = 2;

		this.GemMultiplier = 0.5;

		registerStatTrackers(
				new KillsWithinGameStatTracker(this, 6, "Cannibal"),
				new ChooChooStatTracker(this),
				new SlimySheepStatTracker(this)
		);

		registerChatStats(//slimes eaten, kills, length
				new ChatStatData("Cannibal", "Kills", true),
				BlankLine,
				new ChatStatData("ChooChoo", "Tail Length", true),
				new ChatStatData("SlimySheep", "Slimes Eaten", true),
				BlankLine,
				new ChatStatData("kit", "Kit", true)
		);

		new CompassModule()
				.setGiveCompass(true)
				.setGiveCompassToSpecs(true)
				.setGiveCompassToAlive(false)
				.register(this);
	}

	@EventHandler
	public void CreateSheep(GamePrepareCountdownCommence event)
	{
		for (int i = 0; i < GetPlayers(true).size(); i++)
		{
			Player player = GetPlayers(true).get(i);
			_color.put(player, DyeColor.getByDyeData((byte) (i % 16)));

			this.CreatureAllowOverride = true;
			Sheep sheep = player.getWorld().spawn(player.getLocation(), Sheep.class);
			UtilEnt.addFlag(sheep, UtilEnt.FLAG_ENTITY_COMPONENT);
			this.CreatureAllowOverride = false;

			sheep.setColor(DyeColor.getByDyeData((byte) (i % 16)));
			sheep.setPassenger(player);

			UtilEnt.vegetate(sheep);

			_tail.put(player, new ArrayList<Creature>());
			_tail.get(player).add(sheep);
		}

		for (Player player : GetPlayers(true))
		{
			player.playEffect(this.GetSpectatorLocation(), Effect.RECORD_PLAY, 2259);
		}
	}

	@EventHandler
	public void ThirdPerson(GameStateChangeEvent event)
	{
		if (event.GetState() != GameState.Live)
			return;

		_snakeLiveStartedAt = System.currentTimeMillis();
		_nextMagmaHazardAt = _snakeLiveStartedAt + FIRST_MAGMA_HAZARD_MS;
		_hazardSteeringTick = 0;
		restoreAllRouletteHotbarItems();
		_controlDebuffs.clear();
		_controlDebuffUntil.clear();
		_rouletteHitCount.clear();
		_rouletteRollsInProgress.clear();
		_lastRawYaw.clear();
		_laggedYaw.clear();
		_lastTravelDirection.clear();
		removeMagmaCubes();

		this.Announce(C.cYellow + C.Scramble + "@@" + C.cAqua + C.Bold + " Snake is best played in 3rd Person! (Push F5) " + C.cYellow + C.Scramble + "@@");
		this.Announce(C.cGold + C.Bold + "Hazards and the steering roulette ramp up as the round goes on!");
	}

	@EventHandler
	public void AutoGrow(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.SEC)
			return;

		for (Player player : GetPlayers(true))
		{
			Grow(player, 1, false);
		}
	}

	@EventHandler
	public void ReSit(UpdateEvent event)
	{
		if (!InProgress())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		for (Player player : GetPlayers(true))
		{
			if (player.getVehicle() != null)
				continue;

			if (!_tail.containsKey(player))
				continue;

			if (_tail.get(player).isEmpty())
				continue;

			_tail.get(player).get(0).setPassenger(player);
		}
	}

	@EventHandler
	public void Move(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		for (Player player : GetPlayers(true))
		{
			if (!_tail.containsKey(player))
				continue;
			
			if (_reverse.containsKey(player))
				continue;

			double mult = 0.4;

			if (_tail.containsKey(player))
				mult += Math.min(0.7, (double) _tail.get(player).size() / _maxSpeed);

			if (_speed.containsKey(player))
				mult = mult * 1.5;

			Vector vel = getDebuffedDirection(player).multiply(4);

			Creature before = null;
			for (int i = 0; i < _tail.get(player).size(); i++)
			{
				Creature tail = _tail.get(player).get(i);

				Location loc = player.getLocation().add(vel);

				//First
				if (i == 0)
					loc = tail.getLocation().add(vel);

				//Others
				if (before != null)
					loc = before.getLocation();

				//Teleport
				if (before != null)
				{
					Location tp = before.getLocation().add(UtilAlg.getTrajectory2d(before, tail).multiply(1.4));
					tp.setPitch(tail.getLocation().getPitch());
					tp.setYaw(tail.getLocation().getYaw());
					tail.teleport(tp);
				}

				//Move
				UtilEnt.CreatureMoveFast(tail, loc, (float) (1.2 + 2f * mult));

				//Store
				before = tail;
			}
		}
	}

	/**
	 * Roulette effects are per-player and last until the next 25-second roll.
	 * Magma Cubes start after 45 seconds and additional hazards arrive every 30 seconds.
	 */
	@EventHandler
	public void SnakeRouletteAndHazardSpawn(UpdateEvent event)
	{
		if (!IsLive() || event.getType() != UpdateType.SEC || _snakeLiveStartedAt <= 0L)
			return;

		long now = System.currentTimeMillis();
		if (now >= _nextMagmaHazardAt && _magmaCubes.size() < MAX_MAGMA_HAZARDS)
		{
			boolean spawned = spawnMagmaCubeHazard();
			// Retry shortly if the current map has no safe, loaded floor point near the
			// players. Do not announce or consume a full interval for a failed spawn.
			_nextMagmaHazardAt = now + (spawned ? MAGMA_HAZARD_INTERVAL_MS : 5000L);
		}
	}

	/** Apply one of the roulette's steering changes without changing the player's camera input. */
	private Vector getDebuffedDirection(Player player)
	{
		float rawYaw = player.getLocation().getYaw();
		Float previousYawValue = _lastRawYaw.put(player, Float.valueOf(rawYaw));
		float previousYaw = previousYawValue == null ? rawYaw : previousYawValue.floatValue();
		ControlDebuff debuff = _controlDebuffs.get(player);
		float effectiveYaw = rawYaw;

		if (debuff == ControlDebuff.TURN_LAG)
		{
			Float lagValue = _laggedYaw.get(player);
			float lagYaw = lagValue == null ? rawYaw : lagValue.floatValue();
			float delta = wrapYaw(rawYaw - lagYaw);
			lagYaw += clamp(delta, -6.0f, 6.0f);
			_laggedYaw.put(player, Float.valueOf(lagYaw));
			effectiveYaw = lagYaw;
		}
		else if (debuff == ControlDebuff.OVERSHOOT)
		{
			float delta = wrapYaw(rawYaw - previousYaw);
			effectiveYaw = rawYaw + (delta * 0.35f);
		}
		else if (debuff == ControlDebuff.WEAK_SHARP_TURN)
		{
			// Keep this state across ticks so a sharp turn stays sluggish until the
			// snake catches up, rather than weakening only the first input packet.
			Float appliedValue = _laggedYaw.get(player);
			float appliedYaw = appliedValue == null ? previousYaw : appliedValue.floatValue();
			float remaining = wrapYaw(rawYaw - appliedYaw);
			if (Math.abs(remaining) > 15.0f)
			{
				appliedYaw += clamp(remaining, -4.0f, 4.0f);
				_laggedYaw.put(player, Float.valueOf(appliedYaw));
				effectiveYaw = appliedYaw;
			}
			else
			{
				_laggedYaw.put(player, Float.valueOf(rawYaw));
				effectiveYaw = rawYaw;
			}
		}

		Location heading = player.getLocation().clone();
		heading.setYaw(effectiveYaw);
		Vector current = heading.getDirection().setY(0);
		if (current.lengthSquared() < 1.0e-6) current = new Vector(0, 0, 1);
		current.normalize();

		if (debuff == ControlDebuff.DRIFT)
		{
			Vector previous = _lastTravelDirection.get(player);
			if (previous == null || previous.lengthSquared() < 1.0e-6)
				previous = current.clone();
			previous.normalize();
			// Drift is inertia, not a tiny blend with camera heading. Limit the snake's
			// heading change each tick while still allowing the player to steer.
			double previousAngle = Math.atan2(previous.getZ(), previous.getX());
			double requestedAngle = Math.atan2(current.getZ(), current.getX());
			double delta = wrapRadians(requestedAngle - previousAngle);
			delta = Math.max(-MAX_DRIFT_TURN_RADIANS, Math.min(MAX_DRIFT_TURN_RADIANS, delta));
			double driftAngle = previousAngle + delta;
			Vector drifting = new Vector(Math.cos(driftAngle), 0.0D, Math.sin(driftAngle)).normalize();
			_lastTravelDirection.put(player, drifting.clone());
			return drifting;
		}
		return current;
	}

	private float wrapYaw(float angle)
	{
		while (angle <= -180.0f) angle += 360.0f;
		while (angle > 180.0f) angle -= 360.0f;
		return angle;
	}

	private float clamp(float value, float min, float max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private double wrapRadians(double angle)
	{
		while (angle <= -Math.PI) angle += Math.PI * 2.0D;
		while (angle > Math.PI) angle -= Math.PI * 2.0D;
		return angle;
	}

	/**
	 * Spawns a progressively larger visible hazard on a real floor tile near the edge
	 * of the play area. The previous random X/Z + player's current Y could put the cube
	 * inside a wall or over the void; the chat announcement then looked like a random
	 * text-only Mario-Kart event even though the entity had no usable spawn point.
	 */
	private boolean spawnMagmaCubeHazard()
	{
		ArrayList<Player> players = GetPlayers(true);
		if (players.isEmpty())
			return false;

		Player target = players.get((int) (Math.random() * players.size()));
		Location center = GetSpectatorLocation();
		if (center == null || center.getWorld() == null || !center.getWorld().equals(target.getWorld()))
		{
			if (!GetTeamList().isEmpty() && !GetTeamList().get(0).GetSpawns().isEmpty())
				center = GetTeamList().get(0).GetSpawns().get(0).clone();
			else
				center = target.getLocation().clone();
		}

		Location spawn = findMagmaCubeSpawn(target, center);
		if (spawn == null)
		{
			Manager.getPlugin().getLogger().warning("[Snake] Could not find a clear floor tile for a Magma Cube hazard; retrying soon.");
			return false;
		}

		MagmaCube cube;
		boolean previousCreatureOverride = CreatureAllowOverride;
		try
		{
			CreatureAllowOverride = true;
			cube = spawn.getWorld().spawn(spawn, MagmaCube.class);
		}
		catch (RuntimeException ex)
		{
			Manager.getPlugin().getLogger().warning("[Snake] Magma Cube spawn failed: " + ex.getMessage());
			return false;
		}
		finally
		{
			CreatureAllowOverride = previousCreatureOverride;
		}
		if (cube == null || !cube.isValid() || cube.isDead())
		{
			if (cube != null && cube.isValid())
				cube.remove();
			Manager.getPlugin().getLogger().warning("[Snake] Bukkit did not create a valid Magma Cube hazard.");
			return false;
		}

		cube.setSize(Math.min(4, _magmaCubes.size() + 1));
		cube.setCustomName(ChatColor.RED + "Snake Hazard");
		cube.setCustomNameVisible(false);
		cube.setRemoveWhenFarAway(false);
		_magmaCubes.add(cube);

		String count = String.valueOf(_magmaCubes.size());
		for (Player player : GetPlayers(true))
		{
			player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "MAGMA CUBE! "
					+ ChatColor.GRAY + "A size " + cube.getSize() + " hazard has spawned in the arena ("
					+ count + "/" + MAX_MAGMA_HAZARDS + ").");
			player.playSound(player.getLocation(), Sound.MAGMACUBE_JUMP, 0.8f, 0.8f);
		}
		return true;
	}

	/** Find a clear three-block-high spawn point with a solid floor, at a random angle from a player. */
	private Location findMagmaCubeSpawn(Player target, Location center)
	{
		if (target == null || !target.isOnline() || center == null || center.getWorld() == null)
			return null;

		org.bukkit.World world = target.getWorld();
		Location playerLocation = target.getLocation();
		double startAngle = Math.random() * Math.PI * 2.0D;
		int baseY = playerLocation.getBlockY();

		for (int attempt = 0; attempt < 48; attempt++)
		{
			double angle = startAngle + attempt * 2.399963229728653D;
			double distance = 12.0D + ((attempt % 5) * 2.0D);
			double x = playerLocation.getX() + Math.cos(angle) * distance;
			double z = playerLocation.getZ() + Math.sin(angle) * distance;
			x = Math.max(center.getX() - 42.0D, Math.min(center.getX() + 42.0D, x));
			z = Math.max(center.getZ() - 42.0D, Math.min(center.getZ() + 42.0D, z));

			int blockX = (int) Math.floor(x);
			int blockZ = (int) Math.floor(z);
			// Load the chunk before inspecting blocks; otherwise valid, unloaded floor
			// tiles can look like failed spawn locations.
			if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4))
				world.getChunkAt(blockX >> 4, blockZ >> 4).load();
			int minY = Math.max(1, baseY - 7);
			int maxY = Math.min(world.getMaxHeight() - 4, baseY + 3);
			for (int y = maxY; y >= minY; y--)
			{
				org.bukkit.block.Block ground = world.getBlockAt(blockX, y - 1, blockZ);
				org.bukkit.block.Block feet = world.getBlockAt(blockX, y, blockZ);
				org.bukkit.block.Block head = world.getBlockAt(blockX, y + 1, blockZ);
				org.bukkit.block.Block top = world.getBlockAt(blockX, y + 2, blockZ);
				if (ground.getType().isSolid()
						&& feet.getType() == Material.AIR
						&& head.getType() == Material.AIR
						&& top.getType() == Material.AIR)
				{
					Location result = new Location(world, blockX + 0.5D, y, blockZ + 0.5D,
							(float) (Math.random() * 360.0D), 0.0F);
					if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4))
						world.getChunkAt(blockX >> 4, blockZ >> 4).load();
					return result;
				}
			}
		}
		return null;
	}

	/** Track, steer and collide the hazards. Invulnerability still protects against a cube hit. */
	@EventHandler
	public void SnakeMagmaCubeTick(UpdateEvent event)
	{
		if (!IsLive() || event.getType() != UpdateType.TICK || _magmaCubes.isEmpty())
			return;

		_hazardSteeringTick++;
		Iterator<MagmaCube> iterator = _magmaCubes.iterator();
		while (iterator.hasNext())
		{
			MagmaCube cube = iterator.next();
			if (cube == null || !cube.isValid() || cube.isDead())
			{
				if (cube != null && cube.isValid()) cube.remove();
				iterator.remove();
				continue;
			}

			Player nearest = null;
			double nearestDistance = Double.MAX_VALUE;
			for (Player player : GetPlayers(true))
			{
				double distance = UtilMath.offset(cube, player);
				if (distance < nearestDistance)
				{
					nearestDistance = distance;
					nearest = player;
				}
				if (distance < 2.1D)
				{
					beginMagmaRoulette(player);
					cube.remove();
					iterator.remove();
					nearest = null;
					break;
				}
			}
			if (nearest != null && (_hazardSteeringTick % 4 == 0))
			{
				Vector direction = nearest.getLocation().toVector().subtract(cube.getLocation().toVector());
				direction.setY(0);
				if (direction.lengthSquared() > 1.0e-6)
				{
					Vector velocity = direction.normalize().multiply(0.12);
					velocity.setY(cube.getVelocity().getY());
					cube.setVelocity(velocity);
				}
			}
		}
	}

	private void removeMagmaCubes()
	{
		for (MagmaCube cube : _magmaCubes)
		{
			if (cube != null && cube.isValid()) cube.remove();
		}
		_magmaCubes.clear();
	}

	/** Starts a Mario-Kart-style item roll when a player touches a Magma Cube. */
	private void beginMagmaRoulette(final Player player)
	{
		if (player == null || !player.isOnline() || !IsAlive(player) || !_tail.containsKey(player)
				|| _rouletteRollsInProgress.contains(player))
			return;

		final ControlDebuff[] outcomes = ControlDebuff.values();
		final ControlDebuff result = outcomes[(int) (Math.random() * outcomes.length)];
		Integer oldCount = _rouletteHitCount.get(player);
		final int hitCount = (oldCount == null ? 0 : oldCount.intValue()) + 1;
		_rouletteHitCount.put(player, Integer.valueOf(hitCount));
		final int durationSeconds = ROULETTE_DEBUFF_SECONDS[Math.min(hitCount - 1, ROULETTE_DEBUFF_SECONDS.length - 1)];

		ItemStack current = player.getInventory().getItem(ROULETTE_HOTBAR_SLOT);
		_rouletteOriginalHotbarItems.put(player, current == null ? null : current.clone());
		_rouletteRollsInProgress.add(player);
		player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.2F);

		new BukkitRunnable()
		{
			private int step;

			@Override
			public void run()
			{
				if (!IsLive() || !player.isOnline() || !IsAlive(player)
						|| !_rouletteRollsInProgress.contains(player))
				{
					restoreRouletteHotbarItem(player);
					cancel();
					return;
				}

				if (step < ROULETTE_ANIMATION_STEPS)
				{
					ControlDebuff preview = outcomes[(step + (int) (Math.random() * outcomes.length)) % outcomes.length];
					player.getInventory().setItem(ROULETTE_HOTBAR_SLOT, rouletteItem(preview));
					player.updateInventory();
					step++;
					return;
				}

				restoreRouletteHotbarItem(player);
				_controlDebuffs.put(player, result);
				_controlDebuffUntil.put(player, Long.valueOf(System.currentTimeMillis() + durationSeconds * 1000L));
				float yaw = player.getLocation().getYaw();
				_lastRawYaw.put(player, Float.valueOf(yaw));
				_laggedYaw.put(player, Float.valueOf(yaw));
				Vector initialDirection = player.getLocation().getDirection().setY(0);
				if (initialDirection.lengthSquared() < 1.0e-6)
					initialDirection = new Vector(0, 0, 1);
				_lastTravelDirection.put(player, initialDirection.normalize());
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "ITEM ROULETTE! "
						+ ChatColor.LIGHT_PURPLE + result.display + ChatColor.GRAY + " for "
						+ ChatColor.YELLOW + durationSeconds + "s" + ChatColor.GRAY + " — " + result.description);
				player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0F, 1.8F);
				cancel();
			}
		}.runTaskTimer(Manager.getPlugin(), 0L, 2L);
	}

	private ItemStack rouletteItem(ControlDebuff effect)
	{
		ItemStack item = new ItemStack(effect.icon, 1);
		ItemMeta meta = item.getItemMeta();
		if (meta != null)
		{
			meta.setDisplayName(ChatColor.LIGHT_PURPLE + effect.display);
			item.setItemMeta(meta);
		}
		return item;
	}

	private void restoreRouletteHotbarItem(Player player)
	{
		if (player == null || !_rouletteOriginalHotbarItems.containsKey(player))
			return;
		ItemStack original = _rouletteOriginalHotbarItems.remove(player);
		if (player.isOnline())
		{
			player.getInventory().setItem(ROULETTE_HOTBAR_SLOT, original == null ? null : original.clone());
			player.updateInventory();
		}
		_rouletteRollsInProgress.remove(player);
	}

	private void restoreAllRouletteHotbarItems()
	{
		for (Player player : new ArrayList<Player>(_rouletteOriginalHotbarItems.keySet()))
			restoreRouletteHotbarItem(player);
	}

	/** Expire each debuff independently; hits escalate through 1, 3, 5, 10 seconds and beyond. */
	@EventHandler
	public void ExpireRouletteDebuffs(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK || _controlDebuffUntil.isEmpty())
			return;
		long now = System.currentTimeMillis();
		Iterator<java.util.Map.Entry<Player, Long>> iterator = _controlDebuffUntil.entrySet().iterator();
		while (iterator.hasNext())
		{
			java.util.Map.Entry<Player, Long> entry = iterator.next();
			Player player = entry.getKey();
			if (player == null || !player.isOnline() || now < entry.getValue().longValue())
				continue;
			iterator.remove();
			_controlDebuffs.remove(player);
			_laggedYaw.remove(player);
			_lastRawYaw.put(player, Float.valueOf(player.getLocation().getYaw()));
			_lastTravelDirection.remove(player);
			player.sendMessage(ChatColor.GREEN + "Snake steering effect expired.");
		}
	}

	@EventHandler
	public void Idle(UpdateEvent event)
	{
		if (!IsLive())
			return;

		for (Player player : GetPlayers(true))
		{
			if (!_tail.containsKey(player))
			{
				KillPlayer(player, null, "No Tail");
				continue;
			}

			if (!_move.containsKey(player) || UtilMath.offset(_tail.get(player).get(0).getLocation(), _move.get(player)) > 2)
			{
				_move.put(player, _tail.get(player).get(0).getLocation());
				_moveTime.put(player, System.currentTimeMillis());
				continue;
			}

			if (UtilTime.elapsed(_moveTime.get(player), 2000))
				KillPlayer(player, null, "Idle");
		}
	}

	@EventHandler
	public void Collide(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		//Hit Tail
		for (Player player : GetPlayers(true))
		{
			if (_invul.containsKey(player))
				continue;

			if (_reverse.containsKey(player))
				continue;
			
			boolean done = false;
			for (Player other : _tail.keySet())
			{
				int start = 0;
				if (other.equals(player))
					start = 3;

				for (int i = start; i < _tail.get(other).size(); i++)
				{
					Creature tail = _tail.get(other).get(i);

					if (UtilMath.offset(_tail.get(player).get(0), tail) < 1.2)
					{
						UtilParticle.PlayParticle(ParticleType.EXPLODE, tail.getLocation().add(0, 1, 0), 0, 0, 0, 0, 1,
								ViewDist.LONG, UtilServer.getPlayers());
						
						KillPlayer(player, other, null);

						done = true;
						break;
					}
				}

				if (done)
					break;
			}
		}
	}

	public void KillPlayer(Player player, Player killer, String type)
	{
		if (killer != null)
		{
			//Damage Event
			Manager.GetDamage().NewDamageEvent(player, killer, null,
					DamageCause.CUSTOM, 500, false, true, false,
					killer.getName(), "Snake Tail");
		}
		else
		{
			//Damage Event
			Manager.GetDamage().NewDamageEvent(player, null, null,
					DamageCause.CUSTOM, 500, false, true, false,
					type, type);
		}

		if (_tail.containsKey(player))
		{
			for (Creature cur : _tail.get(player))
			{
				cur.playEffect(EntityEffect.HURT);
				cur.remove();
			}

			_tail.get(player).clear();
		}
	}

	@EventHandler
	public void SpawnFood(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.SEC)
			return;

		if (GetTeamList().isEmpty() || GetTeamList().get(0).GetSpawns().isEmpty())
			return;
		Location origin = GetTeamList().get(0).GetSpawns().get(0).clone();
		Location loc = findFoodSpawnLocation(origin);
		if (loc == null)
			return;

		// Preserve normal green slime food. Clone the start position rather than
		// mutating the game's actual spawn coordinates when randomising X/Z.
		Slime slime;
		boolean previousCreatureOverride = CreatureAllowOverride;
		try
		{
			CreatureAllowOverride = true;
			slime = loc.getWorld().spawn(loc, Slime.class);
		}
		catch (RuntimeException ex)
		{
			Manager.getPlugin().getLogger().warning("[Snake] Green slime spawn failed: " + ex.getMessage());
			return;
		}
		finally
		{
			CreatureAllowOverride = previousCreatureOverride;
		}
		if (slime == null || !slime.isValid() || slime.isDead())
		{
			if (slime != null && slime.isValid()) slime.remove();
			return;
		}
		slime.setSize(2);
		slime.setRemoveWhenFarAway(false);
		UtilEnt.vegetate(slime);
		_food.add(slime);
	}

	/** Find a real floor tile inside the Snake arena bounds for food slimes. */
	private Location findFoodSpawnLocation(Location origin)
	{
		if (origin == null || origin.getWorld() == null)
			return null;
		org.bukkit.World world = origin.getWorld();
		int baseY = origin.getBlockY();
		for (int attempt = 0; attempt < 36; attempt++)
		{
			double angle = Math.random() * Math.PI * 2.0D;
			double distance = Math.sqrt(Math.random()) * 46.0D;
			int blockX = origin.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
			int blockZ = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
			if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4))
				world.getChunkAt(blockX >> 4, blockZ >> 4).load();

			int minY = Math.max(1, baseY - 8);
			int maxY = Math.min(world.getMaxHeight() - 3, baseY + 4);
			for (int y = maxY; y >= minY; y--)
			{
				org.bukkit.block.Block ground = world.getBlockAt(blockX, y - 1, blockZ);
				org.bukkit.block.Block feet = world.getBlockAt(blockX, y, blockZ);
				org.bukkit.block.Block head = world.getBlockAt(blockX, y + 1, blockZ);
				if (ground.getType().isSolid() && feet.getType() == Material.AIR && head.getType() == Material.AIR)
					return new Location(world, blockX + 0.5D, y, blockZ + 0.5D);
			}
		}
		return null;
	}

	@EventHandler
	public void EatFood(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		Iterator<Entity> foodIterator = _food.iterator();

		while (foodIterator.hasNext())
		{
			Entity food = foodIterator.next();

			if (!food.isValid())
			{
				food.remove();
				foodIterator.remove();
				continue;
			}

			for (Player player : GetPlayers(true))
			{
				if (UtilMath.offset(food, player) < 2)
				{
					int amount = 2;

					Grow(player, amount, true);
					foodIterator.remove();
					food.remove();

					Bukkit.getPluginManager().callEvent(new SlimeUpgradeEvent(player));

					break;
				}
			}
		}
	}

	public void Grow(Player player, int amount, boolean sound)
	{
		while (amount > 0)
		{
			//Ensure
			Location loc = player.getLocation();
			if (!_tail.get(player).isEmpty())
				loc = _tail.get(player).get(_tail.get(player).size() - 1).getLocation();

			if (_tail.get(player).size() > 1)
				loc.add(UtilAlg.getTrajectory2d(_tail.get(player).get(_tail.get(player).size() - 2), _tail.get(player).get(_tail.get(player).size() - 1)));
			else
				loc.subtract(player.getLocation().getDirection().setY(0));

			//Spawn
			this.CreatureAllowOverride = true;
			Sheep tail = loc.getWorld().spawn(loc, Sheep.class);
			UtilEnt.addFlag(tail, UtilEnt.FLAG_ENTITY_COMPONENT);
			this.CreatureAllowOverride = false;

			tail.setRemoveWhenFarAway(false);
			tail.setColor(_color.get(player));

			//Sets yaw/pitch
			tail.teleport(loc);

			UtilEnt.vegetate(tail);
			UtilEnt.ghost(tail, true, false);

			_tail.get(player).add(tail);

			//Audio
			if (sound)
				player.getWorld().playSound(player.getLocation(), Sound.EAT, 2f, 1f);

			amount--;

			Bukkit.getPluginManager().callEvent(new TailGrowEvent(player, _tail.get(player).size()));
		}

		player.setExp((float) Math.min(0.9999f, (double) _tail.get(player).size() / _maxSpeed));
	}

	@EventHandler
	public void DamageCancel(CustomDamageEvent event)
	{
		if (event.GetCause() == DamageCause.LAVA)
		{
			if (event.GetDamageePlayer() != null)
			{
				KillPlayer(event.GetDamageePlayer(), null, "Lava");
				return;
			}
		}

		if (event.GetCause() != DamageCause.CUSTOM)
			event.SetCancelled("Snake Damage");
	}

	@EventHandler
	public void TargetCancel(EntityTargetEvent event)
	{
		event.setCancelled(true);
	}

	@EventHandler
	public void CombustCancel(EntityCombustEvent event)
	{
		event.setCancelled(true);
	}

	@EventHandler
	public void InvulnerabilityUse(PlayerInteractEvent event)
	{
		if (!IsLive())
			return;

		Player player = event.getPlayer();

		if (!IsAlive(player))
			return;

		if (!UtilGear.isMat(event.getPlayer().getItemInHand(), Material.NETHER_STAR))
			return;

		if (!Recharge.Instance.use(player, "Snake Item", 1000, false, false))
			return;

		UtilInv.remove(player, Material.NETHER_STAR, (byte) 0, 1);

		_invul.put(player, System.currentTimeMillis());
	}

	@EventHandler
	public void InvulnerabilityUpdate(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		Iterator<Player> invIterator = _invul.keySet().iterator();

		while (invIterator.hasNext())
		{
			Player player = invIterator.next();

			if (UtilTime.elapsed(_invul.get(player), 2000))
			{
				applySnakeColour(player, _color.get(player));
				invIterator.remove();
				continue;
			}

			// Without an equipped Snake line, preserve the original random rainbow.
			// An equipped line replaces the rainbow with its Super Snake colour.
			DyeColor col = abilityColour(player, "super");
			if (col == null) col = GetColor();
			applySnakeColour(player, col);
		}
	}

	public DyeColor GetColor()
	{
		double r = Math.random();

		if (r > 0.75) return DyeColor.RED;
		else if (r > 0.5) return DyeColor.YELLOW;
		else if (r > 0.25) return DyeColor.GREEN;
		else return DyeColor.BLUE;
	}

	/** Returns the selected line's server-visible colour for an active ability. */
	private DyeColor abilityColour(Player player, String ability)
	{
		String line = ArcadeCosmeticsMenu.getEquippedChoice(player.getUniqueId(), "snake");
		if ("elemental".equals(line))
		{
			if ("super".equals(ability)) return DyeColor.BLUE;
			if ("speed".equals(ability)) return DyeColor.YELLOW;
			if ("reverse".equals(ability)) return DyeColor.PURPLE;
		}
		else if ("neon".equals(line))
		{
			if ("super".equals(ability)) return DyeColor.CYAN;
			if ("speed".equals(ability)) return DyeColor.LIME;
			if ("reverse".equals(ability)) return DyeColor.MAGENTA;
		}
		else if ("sunset".equals(line))
		{
			if ("super".equals(ability)) return DyeColor.RED;
			if ("speed".equals(ability)) return DyeColor.PINK;
			if ("reverse".equals(ability)) return DyeColor.ORANGE;
		}
		return null;
	}

	/** Applies sheep colours through Bukkit entities and the player disguise so every viewer sees them. */
	private void applySnakeColour(Player player, DyeColor colour)
	{
		if (player == null || colour == null) return;
		DisguiseBase disguise = Manager.GetDisguise().getDisguise(player);
		if (disguise instanceof DisguiseSheep) ((DisguiseSheep) disguise).setColor(colour);
		if (_tail.containsKey(player))
		{
			for (Creature ent : _tail.get(player))
			{
				if (ent instanceof Sheep && ent.isValid()) ((Sheep) ent).setColor(colour);
			}
		}
	}

	/** Small server-visible particles for the active Snake ability's selected style. */
	@EventHandler
	public void abilityVisualParticles(UpdateEvent event)
	{
		if (!IsLive() || event.getType() != UpdateType.TICK || ++_abilityVisualTick % 4 != 0) return;
		for (Player player : GetPlayers(true))
		{
			String style = ArcadeCosmeticsMenu.getEquippedChoice(player.getUniqueId(), "snake", "ability-visuals");
			if (style == null || style.isEmpty() || "classic".equalsIgnoreCase(style)) continue;
			String ability = _invul.containsKey(player) ? "super" : _speed.containsKey(player) ? "speed" : _reverse.containsKey(player) ? "reverse" : null;
			if (ability == null) continue;
			ParticleType particle;
			if ("subtle".equalsIgnoreCase(style)) particle = ParticleType.CLOUD;
			else if ("neon".equalsIgnoreCase(style)) particle = "reverse".equals(ability) ? ParticleType.WITCH_MAGIC : ParticleType.FIREWORKS_SPARK;
			else if ("colour-coded".equalsIgnoreCase(style)) particle = "super".equals(ability) ? ParticleType.FIREWORKS_SPARK : "speed".equals(ability) ? ParticleType.HAPPY_VILLAGER : ParticleType.PORTAL;
			else continue;
			UtilParticle.PlayParticle(particle, player.getLocation().clone().add(0, 0.25, 0), 0.12F, 0.18F, 0.12F, 0.01F, 3, ViewDist.NORMAL);
		}
	}

	@EventHandler
	public void AbilityCosmeticColours(UpdateEvent event)
	{
		if (!IsLive() || event.getType() != UpdateType.TICK) return;
		for (Player player : GetPlayers(true))
		{
			if (!_tail.containsKey(player) || _tail.get(player).isEmpty() || _invul.containsKey(player)) continue;
			DyeColor colour = null;
			if (_speed.containsKey(player)) colour = abilityColour(player, "speed");
			else if (_reverse.containsKey(player)) colour = abilityColour(player, "reverse");
			if (colour == null) colour = _color.get(player);
			applySnakeColour(player, colour);
		}
	}

	@EventHandler
	public void SpeedUse(PlayerInteractEvent event)
	{
		if (!IsLive())
			return;

		Player player = event.getPlayer();

		if (!IsAlive(player))
			return;

		if (!UtilGear.isMat(event.getPlayer().getItemInHand(), Material.FEATHER))
			return;

		if (!Recharge.Instance.use(player, "Snake Item", 1000, false, false))
			return;

		UtilInv.remove(player, Material.FEATHER, (byte) 0, 1);

		_speed.put(player, System.currentTimeMillis());
	}

	@EventHandler
	public void SpeedUpdate(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		Iterator<Player> speedIterator = _speed.keySet().iterator();

		while (speedIterator.hasNext())
		{
			Player player = speedIterator.next();

			if (UtilTime.elapsed(_speed.get(player), 2000))
			{
				speedIterator.remove();
				continue;
			}
		}
	}
	
	@EventHandler
	public void ReverseUse(PlayerInteractEvent event)
	{
		if (!IsLive())
			return;

		Player player = event.getPlayer();

		if (!IsAlive(player))
			return;

		if (!UtilGear.isMat(event.getPlayer().getItemInHand(), Material.COOKIE))
			return;

		if (!Recharge.Instance.use(player, "Snake Item", 1000, false, false))
			return;

		UtilInv.remove(player, Material.COOKIE, (byte) 0, 1);

		_reverse.put(player, System.currentTimeMillis());
		
		//Leave
		player.leaveVehicle();
		
		//Reverse Tail
		ArrayList<Creature> newTail = new ArrayList<Creature>();
		for (int i=_tail.get(player).size()-1 ; i>=0 ; i--)
			newTail.add(_tail.get(player).get(i));
		_tail.put(player, newTail);

		//Resit
		_tail.get(player).get(0).setPassenger(player);
	}

	@EventHandler
	public void ReverseUpdate(UpdateEvent event)
	{
		if (!IsLive())
			return;

		if (event.getType() != UpdateType.TICK)
			return;

		Iterator<Player> speedIterator = _reverse.keySet().iterator();

		while (speedIterator.hasNext())
		{
			Player player = speedIterator.next();

			//Dont idle kill
			_move.put(player, _tail.get(player).get(0).getLocation());
			_moveTime.put(player, System.currentTimeMillis());
			
			if (UtilTime.elapsed(_reverse.get(player), 2000))
			{
				speedIterator.remove();
				continue;
			}
		}
	}
	
	@EventHandler
	public void handleInteractEntityPacket(GameStateChangeEvent event)
	{
		if (event.GetState() == GameState.Dead)
		{
			restoreAllRouletteHotbarItems();
			_tail.clear();
			for (Entity food : _food)
				if (food != null && food.isValid()) food.remove();
			_food.clear();
			_color.clear();
			_invul.clear();
			_speed.clear();
			_reverse.clear();
			_move.clear();
			_moveTime.clear();
			_controlDebuffs.clear();
			_controlDebuffUntil.clear();
			_rouletteHitCount.clear();
			_rouletteRollsInProgress.clear();
			_lastRawYaw.clear();
			_laggedYaw.clear();
			_lastTravelDirection.clear();
			_snakeLiveStartedAt = 0L;
			_nextMagmaHazardAt = 0L;
			removeMagmaCubes();

			HandlerList.unregisterAll(this);
		}
	}
}
