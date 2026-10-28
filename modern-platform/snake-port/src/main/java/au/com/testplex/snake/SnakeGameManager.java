package au.com.testplex.snake;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standalone Snake mechanics port for Paper 26.3. This deliberately keeps the
 * first pass mechanics-focused; cosmetics and the win scene remain game-specific
 * follow-up work after this loop has been exercised in-game.
 */
public final class SnakeGameManager implements Listener {
    private static final String WORLD_NAME = "snake_arena";
    private static final int ARENA_RADIUS = 48;
    private static final int MAX_HAZARDS = SnakeRules.MAX_HAZARDS;
    private static final int HOTBAR_ROULETTE_SLOT = 8;
    private static final int ROULETTE_STEPS = 12;
    private static final long FIRST_HAZARD_DELAY_MS = SnakeRules.FIRST_HAZARD_DELAY_MILLIS;
    private static final long HAZARD_INTERVAL_MS = SnakeRules.HAZARD_INTERVAL_MILLIS;
    private static final double SEGMENT_SPACING = SnakeRules.SEGMENT_SPACING;

    private final SnakeModernPlugin plugin;
    private final Map<UUID, Player> queue = new LinkedHashMap<>();
    private final Map<UUID, SnakeState> snakes = new LinkedHashMap<>();
    private final Map<UUID, PlayerSnapshot> pendingRestore = new HashMap<>();
    private final Set<Slime> foods = new HashSet<>();
    private final Set<MagmaCube> hazards = new HashSet<>();
    private final World arena;
    private final BukkitTask tickTask;
    private boolean running;
    private int startingPlayerCount;
    private int tick;
    private long startedAt;
    private long nextFoodAt;
    private long nextGrowAt;
    private long nextHazardAt;

    public SnakeGameManager(SnakeModernPlugin plugin) {
        this.plugin = plugin;
        this.arena = createArena();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private World createArena() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            world = creator.createWorld();
        }
        if (world == null) {
            plugin.getLogger().severe("Could not create Snake test world.");
            return null;
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.getWorldBorder().setCenter(0.0D, 0.0D);
        world.getWorldBorder().setSize((ARENA_RADIUS + 2) * 2.0D);
        world.getWorldBorder().setDamageAmount(0.0D);
        world.setSpawnLocation(0, 64, 0);

        // This world belongs only to SnakeModern. Construct a deterministic test arena
        // at a known height instead of relying on a supplied legacy map ZIP.
        for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++) {
            for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
                world.getBlockAt(x, 63, z).setType(Material.MOSS_BLOCK, false);
                if (Math.abs(x) == ARENA_RADIUS || Math.abs(z) == ARENA_RADIUS) {
                    for (int y = 64; y <= 66; y++) {
                        world.getBlockAt(x, y, z).setType(Material.BLACKSTONE, false);
                    }
                }
            }
        }
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) entity.remove();
        }
        return world;
    }

    public void join(Player player) {
        if (running) {
            SnakeState state = snakes.get(player.getUniqueId());
            player.sendMessage(state != null && state.alive
                    ? ChatColor.GREEN + "You are already in the current Snake round."
                    : ChatColor.RED + "A Snake round is already running.");
            return;
        }
        if (queue.putIfAbsent(player.getUniqueId(), player) != null) {
            player.sendMessage(ChatColor.YELLOW + "You are already queued for Snake.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Queued for Snake. An operator or any queued player can run /snake start.");
    }

    public void start(Player requester, org.bukkit.command.CommandSender sender) {
        if (running) {
            sender.sendMessage(ChatColor.RED + "Snake is already running.");
            return;
        }
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Snake arena world failed to load. Check the server log.");
            return;
        }
        if (requester != null) queue.putIfAbsent(requester.getUniqueId(), requester);
        List<Player> players = new ArrayList<>();
        for (Player player : queue.values()) {
            if (player != null && player.isOnline()) players.add(player);
        }
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No online players are queued. Use /snake join first.");
            return;
        }

        queue.clear();
        running = true;
        tick = 0;
        startingPlayerCount = players.size();
        long now = System.currentTimeMillis();
        startedAt = now;
        nextFoodAt = now + 1_000L;
        nextGrowAt = now + 1_000L;
        nextHazardAt = now + FIRST_HAZARD_DELAY_MS;
        foods.clear();
        hazards.clear();
        snakes.clear();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            PlayerSnapshot snapshot = new PlayerSnapshot(player);
            double angle = (Math.PI * 2.0D * i) / Math.max(1, players.size());
            double x = Math.cos(angle) * (players.size() == 1 ? 0.0D : 10.0D);
            double z = Math.sin(angle) * (players.size() == 1 ? 0.0D : 10.0D);
            float yaw = (float) (angle * (180.0D / Math.PI) + 90.0D);
            Location spawn = new Location(arena, x, 64.0D, z, yaw, 0.0F);
            String kit = plugin.getConfig().getString("kits." + player.getUniqueId(), "speed");
            if (!isKnownKit(kit)) kit = "speed";

            player.closeInventory();
            player.leaveVehicle();
            player.teleport(spawn);
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(5.0F);
            player.setExhaustion(0.0F);
            player.setFireTicks(0);
            player.setWalkSpeed(0.2F);
            // The legacy source disguises the player as a sheep. Without a tested 26.3
            // packet-disguise plugin, hide the passenger model so the sheep head/tail show.
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    Integer.MAX_VALUE, 0, false, false, false));
            player.getInventory().clear();
            player.setExp(0.0F);
            player.setLevel(0);

            DyeColor snakeColor = colorFor(i);
            SnakeState state = new SnakeState(player, snapshot, kit, spawn, snakeColor);
            Sheep head = spawnSegment(spawn, snakeColor);
            state.segments.add(head);
            state.path.addFirst(spawn.clone());
            head.addPassenger(player);
            snakes.put(player.getUniqueId(), state);
            grantKit(player, kit);
        }

        announce(ChatColor.GOLD + "Snake started! Avoid all tails, eat green slimes, and watch for magma-cube item boxes.");
        if (startingPlayerCount == 1) announce(ChatColor.GRAY + "Solo mechanics test: the round stays live until /snake stop or you leave.");
        sender.sendMessage(ChatColor.GREEN + "Started Snake with " + players.size() + (players.size() == 1 ? " player." : " players."));
    }

    public void setKit(Player player, String rawKit) {
        if (running && snakes.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot change Snake kit during a live round.");
            return;
        }
        String kit = rawKit.toLowerCase(java.util.Locale.ROOT);
        if (!isKnownKit(kit)) {
            player.sendMessage(ChatColor.RED + "Unknown kit. Choose speed, reverse, or invulnerable.");
            return;
        }
        plugin.getConfig().set("kits." + player.getUniqueId(), kit);
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + "Snake kit saved: " + ChatColor.WHITE + kit + ChatColor.GREEN + ".");
    }

    public String status() {
        if (!running) return "IDLE; " + queue.size() + " queued; arena=" + (arena == null ? "unavailable" : WORLD_NAME);
        int alive = 0;
        for (SnakeState state : snakes.values()) if (state.alive && state.player.isOnline()) alive++;
        long elapsed = startedAt > 0L ? Math.max(0L, System.currentTimeMillis() - startedAt) : 0L;
        return "LIVE; alive=" + alive + "/" + startingPlayerCount + "; food=" + foods.size()
                + "; hazards=" + hazards.size() + "; age~" + (elapsed / 1000L) + "s"
                + (startingPlayerCount == 1 ? "; solo test" : "");
    }

    public void leave(Player player) {
        SnakeState state = snakes.get(player.getUniqueId());
        if (state == null) {
            queue.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GRAY + "Removed from Snake queue.");
            return;
        }
        eliminate(state, null, "left the game");
        PlayerSnapshot snapshot = state.snapshot;
        snakes.remove(player.getUniqueId());
        if (snapshot != null && player.isOnline()) snapshot.restore(player);
        player.sendMessage(ChatColor.YELLOW + "You left Snake.");
        checkEndCondition();
    }

    public void stop(String message) {
        if (!running) {
            queue.clear();
            announce(ChatColor.GRAY + "Snake is already idle.");
            return;
        }
        if (message != null && !message.isEmpty()) announce(ChatColor.YELLOW + message);
        finish(null, false);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        if (running) finish(null, false);
        for (Map.Entry<UUID, PlayerSnapshot> entry : new ArrayList<>(pendingRestore.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) entry.getValue().restore(player);
        }
        pendingRestore.clear();
    }

    private void tick() {
        if (!running || arena == null) return;
        tick++;
        long now = System.currentTimeMillis();

        if (now >= nextFoodAt) {
            spawnFood();
            nextFoodAt = now + 1_000L;
        }
        if (now >= nextGrowAt) {
            for (SnakeState state : snakes.values()) {
                if (state.alive && state.player.isOnline()) grow(state, 1);
            }
            nextGrowAt = now + 1_000L;
        }
        if (now >= nextHazardAt && hazards.size() < MAX_HAZARDS) {
            boolean spawned = spawnHazard();
            nextHazardAt = now + (spawned ? HAZARD_INTERVAL_MS : 5_000L);
        }

        for (SnakeState state : new ArrayList<>(snakes.values())) {
            if (!state.alive) continue;
            if (!state.player.isOnline()) {
                eliminate(state, null, "disconnected");
                continue;
            }
            if (state.segments.isEmpty()) {
                eliminate(state, null, "missing snake head");
                continue;
            }
            Sheep head = state.segments.get(0);
            if (SnakeRules.shouldResitPlayer(head.isValid(),
                    state.player.getVehicle() == head)) {
                state.player.leaveVehicle();
                head.addPassenger(state.player);
            }
            expireEffects(state, now);
            if (state.reverseUntil > now) continue;
            moveSnake(state, now);
        }
        processFood();
        moveAndProcessHazards();
        processCollisions();
        checkEndCondition();
    }

    private void moveSnake(SnakeState state, long now) {
        Player player = state.player;
        if (state.segments.isEmpty()) {
            eliminate(state, null, "missing snake");
            return;
        }
        Location headLocation = state.segments.get(0).getLocation();
        Vector direction = steeringDirection(state);
        double speed = SnakeRules.movementSpeed(state.segments.size(), state.speedUntil > now);
        Location next = headLocation.clone().add(direction.multiply(speed));
        next.setY(64.0D);
        next.setYaw(player.getLocation().getYaw());
        next.setPitch(0.0F);
        if (Math.abs(next.getX()) >= ARENA_RADIUS - 1 || Math.abs(next.getZ()) >= ARENA_RADIUS - 1) {
            eliminate(state, null, "hit the arena wall");
            return;
        }
        state.segments.get(0).teleport(next);
        state.path.addFirst(next.clone());
        // Retain enough route history for the complete snake even after long source-style rounds.
        int maxPathPoints = Math.max(2400, state.segments.size() * 8);
        while (state.path.size() > maxPathPoints) state.path.removeLast();
        moveBodyAlongPath(state);
        player.setExp((float) Math.min(0.9999D,
                (double) state.segments.size() / SnakeRules.SPEED_REFERENCE_LENGTH));
    }

    private void moveBodyAlongPath(SnakeState state) {
        if (state.segments.size() <= 1 || state.path.isEmpty()) return;
        List<Location> points = new ArrayList<>(state.path);
        int cursor = 0;
        double distance = 0.0D;
        for (int i = 1; i < state.segments.size(); i++) {
            double target = i * SEGMENT_SPACING;
            while (cursor < points.size() - 1 && distance < target) {
                Location current = points.get(cursor);
                Location next = points.get(cursor + 1);
                distance += Math.sqrt(Math.pow(current.getX() - next.getX(), 2) + Math.pow(current.getZ() - next.getZ(), 2));
                cursor++;
            }
            state.segments.get(i).teleport(points.get(cursor));
        }
    }

    private Vector steeringDirection(SnakeState state) {
        float rawYaw = state.player.getLocation().getYaw();
        float previousYaw = state.lastRawYaw;
        state.lastRawYaw = rawYaw;
        float effectiveYaw = rawYaw;
        ControlDebuff debuff = state.debuff;

        if (debuff == ControlDebuff.TURN_LAG) {
            float delta = SnakeRules.wrapYaw(rawYaw - state.laggedYaw);
            state.laggedYaw += clamp(delta, -6.0F, 6.0F);
            effectiveYaw = state.laggedYaw;
        } else if (debuff == ControlDebuff.OVERSHOOT) {
            effectiveYaw = rawYaw + SnakeRules.wrapYaw(rawYaw - previousYaw) * 0.35F;
        } else if (debuff == ControlDebuff.WEAK_SHARP_TURN) {
            float remaining = SnakeRules.wrapYaw(rawYaw - state.laggedYaw);
            if (Math.abs(remaining) > 15.0F) {
                state.laggedYaw += clamp(remaining, -4.0F, 4.0F);
                effectiveYaw = state.laggedYaw;
            } else {
                state.laggedYaw = rawYaw;
            }
        }

        Location heading = state.player.getLocation().clone();
        heading.setYaw(effectiveYaw);
        Vector current = heading.getDirection().setY(0.0D);
        if (current.lengthSquared() < 1.0E-6D) current = new Vector(0, 0, 1);
        current.normalize();

        if (debuff == ControlDebuff.DRIFT) {
            Vector previous = state.lastTravelDirection;
            if (previous == null || previous.lengthSquared() < 1.0E-6D) previous = current.clone();
            previous = previous.clone().normalize();
            double last = Math.atan2(previous.getZ(), previous.getX());
            double requested = Math.atan2(current.getZ(), current.getX());
            double turn = SnakeRules.clampTurnRadians(SnakeRules.wrapRadians(requested - last));
            double angle = last + turn;
            Vector drift = new Vector(Math.cos(angle), 0.0D, Math.sin(angle)).normalize();
            state.lastTravelDirection = drift.clone();
            return drift;
        }
        state.lastTravelDirection = current.clone();
        return current;
    }

    private void spawnFood() {
        if (arena == null) return;
        Location origin = arena.getSpawnLocation();
        double baseY = origin.getY();
        for (int attempt = 0; attempt < SnakeRules.FOOD_SPAWN_ATTEMPTS; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
            double distance = SnakeRules.foodSpawnDistance(ThreadLocalRandom.current().nextDouble());
            int blockX = origin.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int blockZ = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (Math.abs(blockX - origin.getBlockX()) >= ARENA_RADIUS - 1
                    || Math.abs(blockZ - origin.getBlockZ()) >= ARENA_RADIUS - 1) continue;
            if (!arena.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                arena.getChunkAt(blockX >> 4, blockZ >> 4).load();
            }
            int minY = Math.max(1, (int) baseY - 8);
            int maxY = Math.min(arena.getMaxHeight() - 3, (int) baseY + 4);
            for (int y = maxY; y >= minY; y--) {
                Block floor = arena.getBlockAt(blockX, y - 1, blockZ);
                Block feet = arena.getBlockAt(blockX, y, blockZ);
                Block head = arena.getBlockAt(blockX, y + 1, blockZ);
                if (!floor.getType().isSolid() || !feet.getType().isAir() || !head.getType().isAir()) continue;
                Location spawn = new Location(arena, blockX + 0.5D, y, blockZ + 0.5D);
                Slime slime = arena.spawn(spawn, Slime.class);
                slime.setSize(2);
                slime.setAI(false);
                slime.setGravity(false);
                slime.setSilent(true);
                slime.setRemoveWhenFarAway(false);
                foods.add(slime);
                return;
            }
        }
    }

    private void processFood() {
        Iterator<Slime> iterator = foods.iterator();
        while (iterator.hasNext()) {
            Slime slime = iterator.next();
            if (slime == null || !slime.isValid() || slime.isDead()) {
                iterator.remove();
                continue;
            }
            boolean eaten = false;
            for (SnakeState state : snakes.values()) {
                if (!state.alive || !state.player.isOnline()) continue;
                if (SnakeRules.isFoodCollected(slime.getLocation().distanceSquared(state.player.getLocation()))) {
                    grow(state, 2);
                    state.player.playSound(state.player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0F, 1.0F);
                    state.player.sendMessage(ChatColor.GREEN + "Slime eaten! Snake length: " + state.segments.size());
                    slime.remove();
                    iterator.remove();
                    eaten = true;
                    break;
                }
            }
            if (eaten) continue;
        }
    }

    private void grow(SnakeState state, int amount) {
        if (!state.alive || state.segments.isEmpty()) return;
        for (int n = 0; n < amount; n++) {
            Sheep last = state.segments.get(state.segments.size() - 1);
            Location loc = last.getLocation().clone();
            Vector previous = state.segments.size() > 1
                    ? state.segments.get(state.segments.size() - 2).getLocation().toVector() : null;
            Vector extension = SnakeRules.tailExtensionDirection(
                    previous, last.getLocation().toVector(), state.player.getLocation().getDirection());
            loc.add(extension.multiply(SEGMENT_SPACING));
            Sheep body = spawnSegment(loc, state.color);
            state.segments.add(body);
        }
    }

    private Sheep spawnSegment(Location location, DyeColor color) {
        Sheep sheep = arena.spawn(location, Sheep.class);
        sheep.setColor(color);
        sheep.setAI(false);
        sheep.setGravity(false);
        sheep.setSilent(true);
        sheep.setInvulnerable(true);
        sheep.setRemoveWhenFarAway(false);
        return sheep;
    }

    private boolean spawnHazard() {
        List<SnakeState> alive = snakes.values().stream().filter(s -> s.alive && s.player.isOnline()).toList();
        if (alive.isEmpty() || arena == null) return false;
        SnakeState target = alive.get(ThreadLocalRandom.current().nextInt(alive.size()));
        Location spawn = findHazardSpawn(target.player);
        if (spawn == null) {
            plugin.getLogger().warning("[Snake] No clear floor tile found for a magma-cube item box; retrying soon.");
            return false;
        }

        MagmaCube cube = arena.spawn(spawn, MagmaCube.class);
        cube.setSize(Math.min(4, hazards.size() + 1));
        cube.setCustomName(ChatColor.RED + "Snake Hazard");
        cube.setCustomNameVisible(false);
        // Keep the source magma cube's native hop/gravity; only its target-seeking
        // steering is scripted by moveAndProcessHazards().
        cube.setAI(true);
        cube.setGravity(true);
        cube.setSilent(false);
        cube.setRemoveWhenFarAway(false);
        hazards.add(cube);
        announce(ChatColor.RED + "" + ChatColor.BOLD + "MAGMA CUBE! " + ChatColor.GRAY
                + "A size " + cube.getSize() + " hazard has spawned in the arena ("
                + hazards.size() + "/" + MAX_HAZARDS + ").");
        for (SnakeState state : alive) {
            state.player.playSound(state.player.getLocation(), Sound.ENTITY_MAGMA_CUBE_JUMP, 0.8F, 0.8F);
        }
        return true;
    }

    /**
     * Mirror the legacy 48-point golden-angle search and five-block clearance test.
     * Item boxes spawn around a live target, but are clamped away from the arena wall
     * and never placed into an overhead obstruction or embedded floor.
     */
    private Location findHazardSpawn(Player target) {
        if (target == null || !target.isOnline() || target.getWorld() != arena) return null;
        Location center = arena.getSpawnLocation();
        Location playerLocation = target.getLocation();
        double startAngle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
        int baseY = playerLocation.getBlockY();
        for (int attempt = 0; attempt < SnakeRules.HAZARD_SPAWN_ATTEMPTS; attempt++) {
            double angle = startAngle + attempt * 2.399963229728653D;
            double distance = SnakeRules.hazardSpawnDistance(attempt);
            double x = playerLocation.getX() + Math.cos(angle) * distance;
            double z = playerLocation.getZ() + Math.sin(angle) * distance;
            x = Math.max(center.getX() - ARENA_RADIUS + 6.0D,
                    Math.min(center.getX() + ARENA_RADIUS - 6.0D, x));
            z = Math.max(center.getZ() - ARENA_RADIUS + 6.0D,
                    Math.min(center.getZ() + ARENA_RADIUS - 6.0D, z));
            int blockX = (int) Math.floor(x);
            int blockZ = (int) Math.floor(z);
            if (!arena.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                arena.getChunkAt(blockX >> 4, blockZ >> 4).load();
            }
            int minY = Math.max(1, baseY - 7);
            int maxY = Math.min(arena.getMaxHeight() - 5, baseY + 3);
            for (int y = maxY; y >= minY; y--) {
                Block ground = arena.getBlockAt(blockX, y - 1, blockZ);
                if (!ground.getType().isSolid()) continue;
                boolean clear = true;
                for (int offset = 0; offset <= 4; offset++) {
                    Block space = arena.getBlockAt(blockX, y + offset, blockZ);
                    if (!space.getType().isAir()) {
                        clear = false;
                        break;
                    }
                }
                if (!clear) continue;
                return new Location(arena, blockX + 0.5D, y, blockZ + 0.5D,
                        (float) ThreadLocalRandom.current().nextDouble(0.0D, 360.0D), 0.0F);
            }
        }
        return null;
    }

    private void moveAndProcessHazards() {
        Iterator<MagmaCube> iterator = hazards.iterator();
        while (iterator.hasNext()) {
            MagmaCube cube = iterator.next();
            if (cube == null || !cube.isValid() || cube.isDead()) {
                iterator.remove();
                continue;
            }
            SnakeState nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (SnakeState state : snakes.values()) {
                if (!state.alive || !state.player.isOnline()) continue;
                double distance = cube.getLocation().distanceSquared(state.player.getLocation());
                if (distance < SnakeRules.HAZARD_CONTACT_RADIUS * SnakeRules.HAZARD_CONTACT_RADIUS) {
                    if (!SnakeRules.shouldConsumeHazardOnContact(state.rouletteActive)) {
                        // Reborn leaves this cube in the arena while the current roll finishes.
                        nearest = null;
                        break;
                    }
                    beginRoulette(state);
                    cube.remove();
                    iterator.remove();
                    nearest = null;
                    break;
                }
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = state;
                }
            }
            if (nearest != null && tick % SnakeRules.HAZARD_STEERING_INTERVAL_TICKS == 0) {
                Vector direction = nearest.player.getLocation().toVector().subtract(cube.getLocation().toVector());
                direction.setY(0.0D);
                if (direction.lengthSquared() > 1.0E-6D) {
                    Vector velocity = direction.normalize().multiply(SnakeRules.HAZARD_STEERING_SPEED);
                    velocity.setY(cube.getVelocity().getY());
                    cube.setVelocity(velocity);
                }
            }
        }
    }

    private void beginRoulette(SnakeState state) {
        if (!running || !state.alive || !state.player.isOnline() || state.rouletteActive) return;
        Player player = state.player;
        ControlDebuff[] outcomes = ControlDebuff.values();
        ControlDebuff result = outcomes[ThreadLocalRandom.current().nextInt(outcomes.length)];
        state.rouletteHits++;
        int duration = SnakeRules.rouletteDurationSecondsForHit(state.rouletteHits);
        ItemStack original = player.getInventory().getItem(HOTBAR_ROULETTE_SLOT);
        state.rouletteOriginalItem = original == null ? null : original.clone();
        state.rouletteOriginalCaptured = true;
        state.rouletteActive = true;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.2F);

        BukkitRunnable animation = new BukkitRunnable() {
            private int step;
            @Override public void run() {
                if (!running || !player.isOnline() || !state.alive) {
                    restoreRouletteSlot(state);
                    state.rouletteTask = null;
                    cancel();
                    return;
                }
                if (step < ROULETTE_STEPS) {
                    ControlDebuff preview = outcomes[ThreadLocalRandom.current().nextInt(outcomes.length)];
                    player.getInventory().setItem(HOTBAR_ROULETTE_SLOT, rouletteItem(preview));
                    player.updateInventory();
                    step++;
                    return;
                }
                restoreRouletteSlot(state);
                state.debuff = result;
                state.debuffUntil = System.currentTimeMillis() + duration * 1000L;
                state.lastRawYaw = player.getLocation().getYaw();
                state.laggedYaw = state.lastRawYaw;
                Vector initial = player.getLocation().getDirection().setY(0.0D);
                if (initial.lengthSquared() < 1.0E-6D) initial = new Vector(0, 0, 1);
                state.lastTravelDirection = initial.normalize();
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "ITEM ROULETTE! "
                        + ChatColor.LIGHT_PURPLE + result.display + ChatColor.GRAY + " for "
                        + ChatColor.YELLOW + duration + "s" + ChatColor.GRAY + " — " + result.description);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.8F);
                state.rouletteTask = null;
                cancel();
            }
        };
        state.rouletteTask = animation.runTaskTimer(plugin, 0L, 2L);
    }

    private ItemStack rouletteItem(ControlDebuff effect) {
        ItemStack item = new ItemStack(effect.icon, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + effect.display);
            meta.setLore(List.of(ChatColor.GRAY + effect.description));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void restoreRouletteSlot(SnakeState state) {
        if (state.rouletteOriginalCaptured && state.player.isOnline()) {
            state.player.getInventory().setItem(HOTBAR_ROULETTE_SLOT,
                    state.rouletteOriginalItem == null ? null : state.rouletteOriginalItem.clone());
            state.player.updateInventory();
        }
        state.rouletteOriginalCaptured = false;
        state.rouletteOriginalItem = null;
        state.rouletteActive = false;
    }

    private void expireEffects(SnakeState state, long now) {
        if (state.speedUntil > 0 && now >= state.speedUntil) state.speedUntil = 0L;
        if (state.invulnerableUntil > 0 && now >= state.invulnerableUntil) state.invulnerableUntil = 0L;
        if (state.reverseUntil > 0 && now >= state.reverseUntil) state.reverseUntil = 0L;
        if (state.debuffUntil > 0 && now >= state.debuffUntil) {
            state.debuff = null;
            state.debuffUntil = 0L;
            state.laggedYaw = state.player.getLocation().getYaw();
            state.lastRawYaw = state.laggedYaw;
            state.lastTravelDirection = null;
            state.player.sendMessage(ChatColor.GREEN + "Snake steering effect expired.");
        }
    }

    private void processCollisions() {
        List<SnakeState> live = snakes.values().stream().filter(s -> s.alive && s.player.isOnline()).toList();
        for (SnakeState playerState : live) {
            if (playerState.invulnerableUntil > System.currentTimeMillis()
                    || playerState.reverseUntil > System.currentTimeMillis()
                    || playerState.segments.isEmpty()) continue;
            Location head = playerState.segments.get(0).getLocation();
            boolean hit = false;
            for (SnakeState otherState : live) {
                for (int i = 0; i < otherState.segments.size(); i++) {
                    if (otherState == playerState && i < 3) continue;
                    Sheep segment = otherState.segments.get(i);
                    if (!segment.isValid()) continue;
                    if (SnakeRules.isTailCollision(head.distanceSquared(segment.getLocation()))) {
                        eliminate(playerState, otherState == playerState ? null : otherState.player,
                                otherState == playerState ? "hit your own tail" : "hit " + otherState.player.getName() + "'s tail");
                        hit = true;
                        break;
                    }
                }
                if (hit) break;
            }
        }
    }

    private void eliminate(SnakeState state, Player killer, String reason) {
        if (state == null || !state.alive) return;
        state.alive = false;
        if (state.rouletteTask != null) {
            state.rouletteTask.cancel();
            state.rouletteTask = null;
        }
        restoreRouletteSlot(state);
        removeSegments(state);
        if (state.player.isOnline()) {
            state.player.leaveVehicle();
            state.player.removePotionEffect(PotionEffectType.INVISIBILITY);
            state.player.setGameMode(GameMode.SPECTATOR);
            state.player.sendMessage(ChatColor.RED + "You were eliminated: " + reason + ".");
            if (killer != null) {
                killer.sendMessage(ChatColor.GREEN + "Your Snake tail eliminated " + state.player.getName() + ".");
            }
            announce(ChatColor.GRAY + state.player.getName() + " was eliminated.");
        }
    }

    private void removeSegments(SnakeState state) {
        for (Sheep segment : state.segments) if (segment != null && segment.isValid()) segment.remove();
        state.segments.clear();
        state.path.clear();
    }

    private void checkEndCondition() {
        if (!running) return;
        List<SnakeState> live = snakes.values().stream().filter(s -> s.alive && s.player.isOnline()).toList();
        if (startingPlayerCount > 1 && live.size() <= 1) {
            finish(live.isEmpty() ? null : live.get(0).player, true);
        } else if (startingPlayerCount == 1 && live.isEmpty()) {
            finish(null, false);
        }
    }

    private void finish(Player winner, boolean announceWinner) {
        if (!running) return;
        running = false;
        if (announceWinner && winner != null) announce(ChatColor.GOLD + "" + ChatColor.BOLD + winner.getName() + " won Snake!");
        for (SnakeState state : new ArrayList<>(snakes.values())) {
            if (state.rouletteTask != null) {
                state.rouletteTask.cancel();
                state.rouletteTask = null;
            }
            restoreRouletteSlot(state);
            removeSegments(state);
            if (state.player.isOnline()) {
                state.snapshot.restore(state.player);
            } else {
                pendingRestore.put(state.player.getUniqueId(), state.snapshot);
            }
        }
        for (Slime slime : foods) if (slime != null && slime.isValid()) slime.remove();
        for (MagmaCube cube : hazards) if (cube != null && cube.isValid()) cube.remove();
        foods.clear();
        hazards.clear();
        snakes.clear();
        queue.clear();
        startingPlayerCount = 0;
        startedAt = 0L;
    }

    private void grantKit(Player player, String kit) {
        switch (kit) {
            case "reverse": player.getInventory().addItem(new ItemStack(Material.COOKIE, 3)); break;
            case "invulnerable": player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, 2)); break;
            default: player.getInventory().addItem(new ItemStack(Material.FEATHER, 5)); break;
        }
    }

    private DyeColor colorFor(int index) {
        DyeColor[] values = DyeColor.values();
        return values[SnakeRules.colorIndexForParticipant(index, values.length)];
    }

    private boolean isKnownKit(String kit) {
        if (kit == null) return false;
        String normalized = kit.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("speed") || normalized.equals("reverse") || normalized.equals("invulnerable");
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void announce(String message) {
        for (SnakeState state : snakes.values()) {
            if (state.player.isOnline()) state.player.sendMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAbilityInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;
        SnakeState state = snakes.get(event.getPlayer().getUniqueId());
        if (!running || state == null || !state.alive) return;
        if (state.rouletteActive) {
            event.setCancelled(true);
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;
        Material material = item.getType();
        long now = System.currentTimeMillis();
        if (now < state.abilityCooldownUntil) return;

        if (material == Material.FEATHER && "speed".equals(state.kit)) {
            event.setCancelled(true);
            consumeMainHand(event.getPlayer());
            state.speedUntil = now + 2_000L;
            state.abilityCooldownUntil = now + 1_000L;
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Speed boost!");
        } else if (material == Material.COOKIE && "reverse".equals(state.kit)) {
            event.setCancelled(true);
            consumeMainHand(event.getPlayer());
            event.getPlayer().leaveVehicle();
            Collections.reverse(state.segments);
            List<Location> reversedPath = new ArrayList<>();
            for (Sheep segment : state.segments) reversedPath.add(segment.getLocation());
            state.path.clear();
            state.path.addAll(SnakeRules.seedReversedBodyPath(reversedPath));
            if (!state.segments.isEmpty()) {
                state.segments.get(0).addPassenger(event.getPlayer());
            }
            state.reverseUntil = now + 2_000L;
            state.abilityCooldownUntil = now + 1_000L;
            event.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Reverse!");
        } else if (material == Material.NETHER_STAR && "invulnerable".equals(state.kit)) {
            event.setCancelled(true);
            consumeMainHand(event.getPlayer());
            state.invulnerableUntil = now + 2_000L;
            state.abilityCooldownUntil = now + 1_000L;
            event.getPlayer().sendMessage(ChatColor.AQUA + "Super Snake: tail collisions ignored briefly.");
        }
    }

    private void consumeMainHand(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld() != arena) return;
        if (event.getEntity() instanceof Player player) {
            SnakeState state = snakes.get(player.getUniqueId());
            if (state != null && state.alive
                    && SnakeRules.shouldEliminateFromDamageCause(event.getCause().name())) {
                event.setCancelled(true);
                eliminate(state, null, "fell into lava");
                checkEndCondition();
                return;
            }
            if (state != null) event.setCancelled(true);
        } else if (event.getEntity() instanceof Sheep || event.getEntity() instanceof Slime || event.getEntity() instanceof MagmaCube) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        // Reborn cancels vanilla entity targeting throughout Snake. Item-box cubes
        // are steered by moveAndProcessHazards(), not by hostile AI target selection.
        if (event.getEntity().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (running && snakes.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player
                && running && snakes.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        queue.remove(player.getUniqueId());
        SnakeState state = snakes.get(player.getUniqueId());
        if (state != null && state.alive) {
            eliminate(state, null, "disconnected");
            pendingRestore.put(player.getUniqueId(), state.snapshot);
            checkEndCondition();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerSnapshot snapshot = pendingRestore.remove(event.getPlayer().getUniqueId());
        if (snapshot != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (player.isOnline()) snapshot.restore(player);
            });
        }
    }

    private enum ControlDebuff {
        TURN_LAG("Turn Lag", "Turns trail behind your camera input.", Material.CLOCK),
        DRIFT("Drift", "Your snake keeps its momentum through turns.", Material.SLIME_BALL),
        OVERSHOOT("Overshoot", "Sharp turns swing further than intended.", Material.FIREWORK_ROCKET),
        WEAK_SHARP_TURN("Weak Sharp Turning", "Quick turns are less responsive.", Material.SHEARS);

        final String display;
        final String description;
        final Material icon;
        ControlDebuff(String display, String description, Material icon) {
            this.display = display;
            this.description = description;
            this.icon = icon;
        }
    }

    private static final class SnakeState {
        final Player player;
        final PlayerSnapshot snapshot;
        final String kit;
        final DyeColor color;
        final List<Sheep> segments = new ArrayList<>();
        final Deque<Location> path = new ArrayDeque<>();
        boolean alive = true;
        long speedUntil;
        long invulnerableUntil;
        long reverseUntil;
        long abilityCooldownUntil;
        long debuffUntil;
        int rouletteHits;
        float lastRawYaw;
        float laggedYaw;
        Vector lastTravelDirection;
        ControlDebuff debuff;
        boolean rouletteActive;
        boolean rouletteOriginalCaptured;
        ItemStack rouletteOriginalItem;
        BukkitTask rouletteTask;

        SnakeState(Player player, PlayerSnapshot snapshot, String kit, Location spawn, DyeColor color) {
            this.player = player;
            this.snapshot = snapshot;
            this.kit = kit;
            this.color = color;
            this.lastRawYaw = spawn.getYaw();
            this.laggedYaw = spawn.getYaw();
            this.lastTravelDirection = spawn.getDirection().setY(0).normalize();
        }
    }

    private static final class PlayerSnapshot {
        final Location location;
        final ItemStack[] contents;
        final ItemStack[] armor;
        final ItemStack offhand;
        final GameMode gameMode;
        final float exp;
        final int level;
        final boolean allowFlight;
        final boolean flying;
        final List<PotionEffect> potionEffects;
        final double health;
        final double maxHealthBase;
        final int food;
        final float saturation;
        final float exhaustion;
        final int fireTicks;
        final float walkSpeed;

        PlayerSnapshot(Player player) {
            location = player.getLocation().clone();
            contents = cloneItems(player.getInventory().getContents());
            armor = cloneItems(player.getInventory().getArmorContents());
            ItemStack held = player.getInventory().getItemInOffHand();
            offhand = held == null ? null : held.clone();
            gameMode = player.getGameMode();
            exp = player.getExp();
            level = player.getLevel();
            allowFlight = player.getAllowFlight();
            flying = player.isFlying();
            potionEffects = new ArrayList<>(player.getActivePotionEffects());
            health = player.getHealth();
            var maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            maxHealthBase = maxHealthAttribute == null ? player.getMaxHealth() : maxHealthAttribute.getBaseValue();
            food = player.getFoodLevel();
            saturation = player.getSaturation();
            exhaustion = player.getExhaustion();
            fireTicks = player.getFireTicks();
            walkSpeed = player.getWalkSpeed();
        }

        void restore(Player player) {
            if (player == null || !player.isOnline()) return;
            player.leaveVehicle();
            if (location != null && location.getWorld() != null) player.teleport(location);
            player.getInventory().clear();
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armor));
            player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
            player.setGameMode(gameMode);
            player.setExp(exp);
            player.setLevel(level);
            var maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(maxHealthBase);
            player.setHealth(Math.max(1.0D, Math.min(health, player.getMaxHealth())));
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
            player.setFireTicks(fireTicks);
            player.setWalkSpeed(walkSpeed);
            for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : potionEffects) player.addPotionEffect(effect);
            player.setAllowFlight(allowFlight);
            if (allowFlight) player.setFlying(flying);
        }

        private static ItemStack[] cloneItems(ItemStack[] items) {
            ItemStack[] copy = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) copy[i] = items[i] == null ? null : items[i].clone();
            return copy;
        }
    }
}
