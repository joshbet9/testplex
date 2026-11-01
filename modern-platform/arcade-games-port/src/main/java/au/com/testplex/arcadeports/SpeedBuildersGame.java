package au.com.testplex.arcadeports;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.LivingEntity;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.WorldCreator;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Source-mapped Speed Builders mechanics hosted by ArcadeMechanicsModern.
 *
 * Templates, the eight active builder plots, central judging plot, spawn points and
 * per-template ground layers are read from the archived Reborn Shroomland Lake
 * map. The module remains an isolated WIP test entry, not a rotation-enabled game.
 */
public final class SpeedBuildersGame implements Listener {
    private static final String SOURCE_WORLD_NAME = "arcadeport_source_speedbuilders_v1";
    private static final String MIRROR_WORLD_NAME = "arcadeport_source_speedbuilders_v2";
    private static final String SOURCE_MAP_RESOURCE = "source-maps/Speed Builders/SpeedBuilders_Shroomland_Lake.zip";
    private static final int DEFAULT_MATCH_ROUNDS = 5;
    private static final int FINAL_RESULTS_SECONDS = 5;
    private static final long NORMAL_QUEUE_TICKS = 100L;
    private static final long QUICK_QUEUE_TICKS = 40L;

    private enum Phase { IDLE, VIEWING, BUILDING, INTERMISSION, RESULTS }

    private final ArcadeMechanicsModernPlugin plugin;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private final LinkedHashMap<UUID, PlayerState> players = new LinkedHashMap<>();
    private final List<PlayerState> matchHistory = new ArrayList<>();
    private List<Template> templates = List.of();
    private final List<Template> unusedTemplates = new ArrayList<>();
    private BukkitTask ticker;
    private World arena;
    private World sourceWorld;
    private final List<World> arenaWorlds = new ArrayList<>();
    private List<Plot> playerPlots = List.of();
    private final Map<Plot, Location> plotSpawns = new LinkedHashMap<>();
    private Phase phase = Phase.IDLE;
    private long tick;
    private long queueDeadlineTick = -1L;
    private long phaseDeadlineTick;
    private int round;
    private int matchRoundLimit = DEFAULT_MATCH_ROUNDS;
    private int buildTimeTracker = SpeedBuildersRules.DEFAULT_BUILD_SECONDS;
    private long buildPhaseStartedTick;
    private boolean firstPerfectBuildAwarded;
    private int buildSeconds = SpeedBuildersRules.DEFAULT_BUILD_SECONDS;
    private Template currentTemplate;
    private String stopReason = "";
    private boolean shuttingDown;
    private boolean standaloneSessionLockAcquired;

    SpeedBuildersGame(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public String status() {
        if (phase == Phase.IDLE) return "queue=" + queue.size();
        return phase.name() + " round=" + round + "/" + matchRoundLimit + " builders=" + activePlayers().size()
                + " templates=" + templates.size()
                + " build=" + (currentTemplate == null ? "none" : currentTemplate.name());
    }

    public static boolean isStatsRequest(String[] args) {
        if (args == null || args.length == 0) return false;
        return isSpeedBuildersId(args[0]) || (args.length > 1 && isSpeedBuildersId(args[1]));
    }

    public void showStats(Player viewer, String[] args) {
        if (viewer == null || !viewer.isOnline()) return;
        String subjectName = viewer.getName();
        UUID subjectId = viewer.getUniqueId();
        if (args != null && args.length > 1 && isSpeedBuildersId(args[1])) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                viewer.sendMessage(ChatColor.RED + "That player must be online to view their Speed Builders stats.");
                return;
            }
            subjectName = target.getName();
            subjectId = target.getUniqueId();
        }
        String root = "local-stats." + subjectId + ".speed-builders-arcade.";
        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + subjectName + " — SPEED BUILDERS");
        viewer.sendMessage(ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + plugin.getConfig().getLong(root + "wins", 0L)
                + ChatColor.YELLOW + " | Perfect builds: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "perfect-builds", 0L));
        viewer.sendMessage(ChatColor.YELLOW + "Blocks placed: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "blocks-placed", 0L)
                + ChatColor.YELLOW + " | Blocks broken: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "blocks-broken", 0L));
        viewer.sendMessage(ChatColor.YELLOW + "Mobs built: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "mobs-built", 0L)
                + ChatColor.YELLOW + " | First perfect: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "perfect-first", 0L));
        viewer.sendMessage(ChatColor.YELLOW + "Perfect wins: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "perfectionist-wins", 0L)
                + ChatColor.YELLOW + " | Builds under 10s: " + ChatColor.WHITE
                + plugin.getConfig().getLong(root + "speediest-builderizer", 0L));
    }

    private static boolean isSpeedBuildersId(String requested) {
        if (requested == null) return false;
        String normalized = requested.toLowerCase(java.util.Locale.ROOT)
                .replace('_', '-').replace(' ', '-');
        return normalized.equals("speed-builders") || normalized.equals("speedbuilder")
                || normalized.equals("speedbuilders") || normalized.equals("speed-builder");
    }


    public void queue(Player player, boolean quickStart) {
        if (player == null || !player.isOnline()) return;
        if (isActive()) {
            player.sendMessage(ChatColor.RED + "Speed Builders is already in progress.");
            return;
        }
        if (queue.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You're already queued for Speed Builders.");
            return;
        }
        if (queue.size() >= SpeedBuildersRules.MAX_PLAYERS) {
            player.sendMessage(ChatColor.RED + "The Speed Builders queue is full (" + SpeedBuildersRules.MAX_PLAYERS + " builders).");
            return;
        }
        if (plugin.getArcadePortManager() != null) {
            plugin.getArcadePortManager().removePlayerFromOtherQueues(player);
        }
        queue.add(player.getUniqueId());
        long requestedDeadline = tick + (quickStart ? QUICK_QUEUE_TICKS : NORMAL_QUEUE_TICKS);
        if (queueDeadlineTick < 0L || requestedDeadline < queueDeadlineTick) {
            queueDeadlineTick = requestedDeadline;
        }
        player.sendMessage(ChatColor.GREEN + "Queued for Speed Builders "
                + ChatColor.GRAY + "(" + queue.size() + "/" + SpeedBuildersRules.MAX_PLAYERS + ").");
        if (queue.size() < SpeedBuildersRules.MIN_PLAYERS) {
            player.sendMessage(ChatColor.GRAY + "Waiting for at least one more player.");
        }
        announceQueue();
    }

    public void leaveQueue(Player player) {
        if (player == null) return;
        if (queue.remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You left the Speed Builders queue.");
            if (queue.size() < SpeedBuildersRules.MIN_PLAYERS) queueDeadlineTick = -1L;
            announceQueue();
        } else {
            player.sendMessage(ChatColor.GRAY + "You're not in the Speed Builders queue.");
        }
    }

    public void startQueued() {
        if (isActive()) {
            plugin.getLogger().warning("Rejected Speed Builders start because another Speed Builders match is active.");
            return;
        }
        List<Player> participants = new ArrayList<>();
        for (UUID id : queue) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) participants.add(player);
            if (participants.size() == SpeedBuildersRules.MAX_PLAYERS) break;
        }
        queue.removeIf(id -> Bukkit.getPlayer(id) == null || !Bukkit.getPlayer(id).isOnline());
        if (participants.size() < SpeedBuildersRules.MIN_PLAYERS) {
            for (Player player : participants) {
                player.sendMessage(ChatColor.YELLOW + "Speed Builders is waiting for another player; your queue place is retained.");
            }
            queueDeadlineTick = queue.isEmpty() ? -1L : tick + NORMAL_QUEUE_TICKS;
            return;
        }
        if (!plugin.canStartSpeedBuilders()) {
            for (Player player : participants) {
                player.sendMessage(ChatColor.RED + "Another Arcade game is currently active. Your Speed Builders queue is retained; an operator can start it after that match ends.");
            }
            queueDeadlineTick = -1L;
            return;
        }
        queue.clear();
        queueDeadlineTick = -1L;
        startMatch(participants);
    }

    public void stop(String reason) {
        if (!isActive()) {
            queue.clear();
            queueDeadlineTick = -1L;
            return;
        }
        stopReason = reason == null ? "Stopped by an operator." : reason;
        finishMatch(null, stopReason);
    }

    public void shutdown() {
        shuttingDown = true;
        stop("Arcade mechanics plugin is shutting down.");
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        queue.clear();
    }

    /** Restore a pending Speed Builders snapshot after reconnect or an unclean server stop. */
    public boolean restorePendingSnapshot(Player player) {
        if (player == null || !player.isOnline()) return false;
        String path = "speed-builders-pending-restores." + player.getUniqueId();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return false;
        try {
            PlayerSnapshot snapshot = new PlayerSnapshot(section);
            restoreAfterReturningToLobby(player, snapshot);
            return true;
        } catch (RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not restore Speed Builders player snapshot for " + player.getUniqueId(), failure);
            return false;
        }
    }

    /** Release only a lock acquired by this match, after queued restoration tasks have run. */
    private void releaseSessionLockAfterRestoration() {
        if (!standaloneSessionLockAcquired) return;
        standaloneSessionLockAcquired = false;
        if (shuttingDown || !plugin.isEnabled()) {
            plugin.releaseSpeedBuildersLock();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, plugin::releaseSpeedBuildersLock);
    }

    private void restoreAfterReturningToLobby(Player player, PlayerSnapshot snapshot) {
        if (player == null || snapshot == null || !player.isOnline()) return;
        if (shuttingDown) {
            try {
                snapshot.restore(player);
                plugin.getConfig().set("speed-builders-pending-restores." + player.getUniqueId(), null);
                plugin.saveConfig();
            } catch (RuntimeException failure) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Speed Builders could not synchronously restore player state during shutdown for "
                                + player.getUniqueId(), failure);
            }
            return;
        }
        plugin.returnPlayerToArcadeLobby(player);
        Runnable restore = () -> {
            if (!player.isOnline()) return;
            try {
                snapshot.restore(player);
                String path = "speed-builders-pending-restores." + player.getUniqueId();
                plugin.getConfig().set(path, null);
                plugin.saveConfig();
            } catch (RuntimeException failure) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Speed Builders could not restore pre-match state for " + player.getUniqueId()
                                + "; the pending snapshot is retained for retry.", failure);
            }
        };
        if (shuttingDown || !plugin.isEnabled()) restore.run();
        else Bukkit.getScheduler().runTask(plugin, restore);
    }

    private void announceQueue() {
        String message = ChatColor.GOLD + "Speed Builders queue: " + ChatColor.WHITE
                + queue.size() + "/" + SpeedBuildersRules.MAX_PLAYERS;
        for (UUID id : queue) {
            Player queued = Bukkit.getPlayer(id);
            if (queued != null && queued.isOnline()) queued.sendMessage(message);
        }
    }

    private void tick() {
        tick++;
        if (phase == Phase.IDLE) {
            if (queueDeadlineTick >= 0L && tick >= queueDeadlineTick) startQueued();
            return;
        }

        if (tick >= phaseDeadlineTick) {
            switch (phase) {
                case VIEWING -> beginBuildingPhase();
                case BUILDING -> finishBuildingPhase();
                case INTERMISSION -> beginRound();
                case RESULTS -> finishScoredMatch();
                default -> { }
            }
        }
        if (tick % 20L == 0L) refreshHud();
    }

    private void startMatch(List<Player> participants) {
        try {
            plugin.prepareForSpeedBuilders();
            standaloneSessionLockAcquired = true;
            loadSourceTemplates(participants.size());
            arena = ensureArena();
            buildArenaFloor();
            matchRoundLimit = Math.max(1, Math.min(templates.size(),
                    plugin.getConfig().getInt("speed-builders.match-rounds", DEFAULT_MATCH_ROUNDS)));
            players.clear();
            matchHistory.clear();
            round = 0;
            buildTimeTracker = SpeedBuildersRules.DEFAULT_BUILD_SECONDS;
            buildSeconds = SpeedBuildersRules.DEFAULT_BUILD_SECONDS;
            firstPerfectBuildAwarded = false;
            unusedTemplates.clear();
            unusedTemplates.addAll(templates);
            Collections.shuffle(unusedTemplates);
            stopReason = "";

            for (int i = 0; i < participants.size(); i++) {
                Player player = participants.get(i);
                Plot plot = plotFor(i);
                PlayerState state = new PlayerState(player, plot, plugin);
                players.put(player.getUniqueId(), state);
                matchHistory.add(state);
                preparePlayerForRound(state);
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "SPEED BUILDERS");
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Memorise the model, then rebuild it in your own plot.");
            beginRound();
        } catch (RuntimeException | Error failure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not start the Speed Builders test module.", failure);
            for (Player player : participants) {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Speed Builders could not start: " + failure.getMessage());
                    PlayerState state = players.get(player.getUniqueId());
                    if (state != null) restoreAfterReturningToLobby(player, state.snapshot);
                    else plugin.returnPlayerToArcadeLobby(player);
                }
            }
            players.clear();
            matchHistory.clear();
                phase = Phase.IDLE;
            currentTemplate = null;
            releaseSessionLockAfterRestoration();
        }
    }

    /** Load the real Mineplex Reborn build definitions from the original Shroomland Lake map. */
    /** Load two independent copies of the authentic eight-plot map for up to sixteen builders. */
    private void loadSourceTemplates(int participantCount) {
        arenaWorlds.clear();
        plotSpawns.clear();
        sourceWorld = loadArenaWorld(SOURCE_WORLD_NAME);
        arenaWorlds.add(sourceWorld);

        SourceMapCatalog.MapData mapData = SourceMapCatalog.readResource(
                plugin, "Speed Builders", SOURCE_MAP_RESOURCE, sourceWorld);
        if (mapData == null) {
            throw new IllegalStateException("The original Speed Builders source map could not be read.");
        }

        int requiredArenas = SpeedBuildersRules.arenaCountForPlayers(participantCount);
        List<Plot> allocatedPlots = new ArrayList<>(requiredArenas * SpeedBuildersRules.PLOTS_PER_ARENA);
        appendArenaPlots(mapData, sourceWorld, allocatedPlots);
        if (requiredArenas > 1) {
            World mirrorWorld = loadArenaWorld(MIRROR_WORLD_NAME);
            arenaWorlds.add(mirrorWorld);
            SourceMapCatalog.MapData mirrorData = SourceMapCatalog.readResource(
                    plugin, "Speed Builders", SOURCE_MAP_RESOURCE, mirrorWorld);
            if (mirrorData == null) {
                throw new IllegalStateException("The mirrored Speed Builders map could not be read.");
            }
            appendArenaPlots(mirrorData, mirrorWorld, allocatedPlots);
        }
        if (allocatedPlots.size() != requiredArenas * SpeedBuildersRules.PLOTS_PER_ARENA) {
            throw new IllegalStateException("Expected " + (requiredArenas * SpeedBuildersRules.PLOTS_PER_ARENA)
                    + " authored build plots across " + requiredArenas + " arena(s); found "
                    + allocatedPlots.size() + ".");
        }
        playerPlots = List.copyOf(allocatedPlots);

        List<Template> loaded = new ArrayList<>();
        for (Map.Entry<String, List<Location>> entry : mapData.customLocationGroups().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).toList()) {
            if (entry.getValue().isEmpty()) continue;
            SpeedBuildersRules.TemplateTiming timing = SpeedBuildersRules.parseTemplateName(entry.getKey());
            String displayName = ChatColor.translateAlternateColorCodes('&', timing.displayName());
            Location origin = entry.getValue().getFirst().clone().subtract(0.5D, 0.0D, 0.5D);
            Location buildMin = origin.clone().subtract(3.0D, 10.0D, 3.0D);
            Location groundMin = origin.clone().subtract(3.0D, 11.0D, 3.0D);
            List<SpeedBuildersRules.Cell> cells = new ArrayList<>();
            List<GroundCell> ground = new ArrayList<>();
            for (int x = 0; x < SpeedBuildersRules.BUILD_WIDTH; x++) {
                for (int z = 0; z < SpeedBuildersRules.BUILD_WIDTH; z++) {
                    Block floor = sourceWorld.getBlockAt(groundMin.getBlockX() + x,
                            groundMin.getBlockY(), groundMin.getBlockZ() + z);
                    ground.add(new GroundCell(x, z, floor.getType(), floor.getBlockData().getAsString()));
                }
            }
            List<MobCell> mobs = new ArrayList<>();
            for (int x = 0; x < SpeedBuildersRules.BUILD_WIDTH; x++) {
                for (int y = 0; y < SpeedBuildersRules.BUILD_HEIGHT; y++) {
                    for (int z = 0; z < SpeedBuildersRules.BUILD_WIDTH; z++) {
                        Block block = sourceWorld.getBlockAt(
                                buildMin.getBlockX() + x, buildMin.getBlockY() + y, buildMin.getBlockZ() + z);
                        if (block.getState() instanceof Sign sign) {
                            EntityType mobType = parseMobMarker(sign.getLine(0));
                            if (mobType != null) {
                                mobs.add(new MobCell(mobType, x, y, z));
                                continue;
                            }
                        }
                        if (!block.getType().isAir()) {
                            cells.add(new SpeedBuildersRules.Cell(x, y, z,
                                    block.getType(), block.getBlockData().getAsString()));
                        }
                    }
                }
            }
            if (!cells.isEmpty() || !mobs.isEmpty()) {
                String id = entry.getKey().toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
                loaded.add(new Template(id, displayName, List.copyOf(cells),
                        List.copyOf(mobs), List.copyOf(ground), timing));
            }
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("No playable templates were found in the original Speed Builders map.");
        }
        templates = List.copyOf(loaded);
        plugin.getLogger().info("Loaded " + templates.size() + " original Speed Builders templates across "
                + arenaWorlds.size() + " mirrored arenas with " + playerPlots.size() + " authored build plots.");
    }

    private World loadArenaWorld(String worldName) {
        if (!SourceMapCatalog.resetTestWorld(plugin, worldName)) {
            throw new IllegalStateException("Could not safely reset Speed Builders arena world " + worldName + ".");
        }
        if (!SourceMapCatalog.installResource(plugin, "Speed Builders", SOURCE_MAP_RESOURCE, worldName)) {
            throw new IllegalStateException("The original Speed Builders map archive could not be installed in " + worldName + ".");
        }
        World world = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .createWorld();
        if (world == null) {
            throw new IllegalStateException("Paper could not load Speed Builders arena world " + worldName + ".");
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        return world;
    }

    private void appendArenaPlots(SourceMapCatalog.MapData mapData, World world, List<Plot> target) {
        List<Location> authoredPlots = mapData.dataLocations("YELLOW");
        if (authoredPlots.size() < SpeedBuildersRules.PLOTS_PER_ARENA) {
            throw new IllegalStateException("Speed Builders map " + world.getName()
                    + " contains only " + authoredPlots.size() + " YELLOW plots; eight are required.");
        }
        List<Plot> arenaPlots = authoredPlots.stream()
                .limit(SpeedBuildersRules.PLOTS_PER_ARENA)
                .map(marker -> createPlot(world, marker))
                .toList();
        target.addAll(arenaPlots);

        List<Location> availableSpawns = mapData.spawnLocations().stream()
                .map(spawn -> new Location(world, spawn.getX(), spawn.getY(), spawn.getZ(),
                        spawn.getYaw(), spawn.getPitch()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (availableSpawns.isEmpty()) {
            plugin.getLogger().warning("Speed Builders map " + world.getName()
                    + " has no TEAM_SPAWNS markers; using per-plot fallback spawns.");
        }
        for (Plot plot : arenaPlots) {
            if (availableSpawns.isEmpty()) break;
            Location plotCentre = new Location(world, plot.centerX() + 0.5D,
                    plot.baseY(), plot.centerZ() + 0.5D);
            Location closest = availableSpawns.stream()
                    .min(java.util.Comparator.comparingDouble(spawn -> spawn.distanceSquared(plotCentre)))
                    .orElse(null);
            if (closest != null) {
                plotSpawns.put(plot, closest.clone());
                availableSpawns.remove(closest);
            }
        }

        if (mapData.hasBounds()) {
            double centerX = (mapData.minX() + mapData.maxX()) / 2.0D;
            double centerZ = (mapData.minZ() + mapData.maxZ()) / 2.0D;
            double size = Math.max(mapData.maxX() - mapData.minX(), mapData.maxZ() - mapData.minZ()) + 16.0D;
            world.getWorldBorder().setCenter(centerX, centerZ);
            world.getWorldBorder().setSize(Math.max(32.0D, size));
        }
    }

    private EntityType parseMobMarker(String marker) {
        if (marker == null || marker.isBlank()) return null;
        try {
            return EntityType.valueOf(marker.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private World ensureArena() {
        if (sourceWorld == null || Bukkit.getWorld(SOURCE_WORLD_NAME) == null) {
            throw new IllegalStateException("The authentic Speed Builders source map is not loaded.");
        }
        sourceWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        sourceWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        sourceWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        sourceWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        sourceWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        sourceWorld.setTime(6000L);
        sourceWorld.setStorm(false);
        sourceWorld.setThundering(false);
        sourceWorld.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        return sourceWorld;
    }

    private Plot createPlot(World world, Location marker) {
        Location origin = marker.clone().subtract(0.5D, 0.0D, 0.5D);
        List<GroundCell> originalGround = new ArrayList<>();
        for (int x = 0; x < SpeedBuildersRules.BUILD_WIDTH; x++) {
            for (int z = 0; z < SpeedBuildersRules.BUILD_WIDTH; z++) {
                Block floor = world.getBlockAt(origin.getBlockX() - 3 + x,
                        origin.getBlockY() - 1, origin.getBlockZ() - 3 + z);
                originalGround.add(new GroundCell(x, z, floor.getType(), floor.getBlockData().getAsString()));
            }
        }
        return new Plot(world, origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), List.copyOf(originalGround));
    }

    private Location nearestSourceSpawn(Plot plot) {
        Location assigned = plotSpawns.get(plot);
        if (assigned != null) return assigned.clone();
        return new Location(plot.world(), plot.centerX() + 0.5D,
                plot.baseY(), plot.centerZ() + 5.0D);
    }

    private void buildArenaFloor() {
        // The authentic map already contains all eight authored yellow build plots and the red judge display.
        // Preserve its architecture; only the per-round BuildData ground layer is replaced.
    }

    private void beginRound() {
        List<PlayerState> builders = activePlayers();
        if (builders.isEmpty()) {
            finishMatch(null, "No builders remain connected.");
            return;
        }
        if (round >= matchRoundLimit) {
            finishScoredMatch();
            return;
        }
        if (unusedTemplates.isEmpty()) {
            unusedTemplates.addAll(templates);
            Collections.shuffle(unusedTemplates);
        }
        currentTemplate = unusedTemplates.removeFirst();
        round++;
        buildSeconds = currentTemplate.timing().buildSeconds(buildTimeTracker);
        // Reborn computes the next round's time before decrementing the tracker.
        if (round > 1) buildTimeTracker = Math.max(1, buildTimeTracker - 1);
        phase = Phase.VIEWING;
        phaseDeadlineTick = tick + SpeedBuildersRules.DEFAULT_VIEW_SECONDS * 20L;

        for (PlayerState state : builders) {
            state.perfect = false;
            state.score = 0;
            state.player.setGameMode(GameMode.SURVIVAL);
            state.player.getInventory().clear();
            removeMobs(state.mobs);
            restorePlot(state.plot);
            pasteTemplate(state.plot, currentTemplate);
            spawnTemplateMobs(state, state.plot, currentTemplate);
            teleportToPlot(state);
            state.player.sendTitle(ChatColor.GOLD + currentTemplate.name(),
                    ChatColor.YELLOW + "Memorise the model — " + SpeedBuildersRules.DEFAULT_VIEW_SECONDS + " seconds",
                    5, 50, 10);
            state.player.playSound(state.player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.15F);
        }
        Bukkit.broadcastMessage(ChatColor.GOLD + "Round " + round + ": " + ChatColor.YELLOW + currentTemplate.name());
        refreshHud();
    }

    private void beginBuildingPhase() {
        if (activePlayers().isEmpty()) {
            finishMatch(null, "No builders remain connected.");
            return;
        }
        phase = Phase.BUILDING;
        buildPhaseStartedTick = tick;
        phaseDeadlineTick = tick + buildSeconds * 20L;
        for (PlayerState state : activePlayers()) {
            state.roundBuildTimeTicks = 0L;
            state.buildTimeRecorded = false;
            clearPlotBuildVolume(state.plot);
            removeMobs(state.mobs);
            giveTemplateBlocks(state.player, currentTemplate);
            state.player.setGameMode(GameMode.SURVIVAL);
            if (state.player.isOnGround()) state.player.setAllowFlight(true);
            state.player.sendTitle(ChatColor.GREEN + "BUILD!",
                    ChatColor.YELLOW.toString() + buildSeconds + " seconds — recreate the model",
                    0, 30, 5);
            state.player.playSound(state.player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1.3F);
        }
        Bukkit.broadcastMessage(ChatColor.GREEN + "Build phase started. You have " + buildSeconds + " seconds.");
        refreshHud();
    }

    /** Score every player's build automatically as soon as the build timer ends. */
    private void finishBuildingPhase() {
        if (phase != Phase.BUILDING) return;
        List<PlayerState> builders = activePlayers();
        if (builders.isEmpty()) {
            finishMatch(null, "No builders remain connected.");
            return;
        }

        for (PlayerState state : builders) {
            state.score = score(state);
            boolean perfectBuild = state.score == currentTemplate.expectedScore();
            if (perfectBuild && !state.perfect) {
                markPerfectBuild(state);
            } else if (!perfectBuild) {
                state.perfect = false;
                state.perfectedEveryBuild = false;
                state.roundBuildTimeTicks = 0L;
                state.totalBuildTimeTicks = 0L;
            }
            state.roundPoints = SpeedBuildersRules.roundPoints(
                    state.score, currentTemplate.expectedScore());
            state.totalPoints += state.roundPoints;
            state.player.sendMessage(ChatColor.GREEN + "Round " + round + ": "
                    + state.roundPoints + "/100 accuracy ("
                    + state.score + "/" + currentTemplate.expectedScore()
                    + " matching pieces). Match total: " + state.totalPoints + " points.");
            state.player.sendTitle(ChatColor.GOLD + "ROUND " + round + " RESULT",
                    ChatColor.YELLOW.toString() + state.roundPoints + "/100 accuracy · "
                            + state.totalPoints + " total", 0, 45, 10);
        }

        Bukkit.broadcastMessage(ChatColor.YELLOW + "Round " + round + "/" + matchRoundLimit
                + " complete. Scores are calculated automatically; every builder remains in the game.");
        phase = round >= matchRoundLimit ? Phase.RESULTS : Phase.INTERMISSION;
        phaseDeadlineTick = tick + (phase == Phase.RESULTS
                ? FINAL_RESULTS_SECONDS * 20L
                : SpeedBuildersRules.INTERMISSION_SECONDS * 20L);
        refreshHud();
    }

    /** Arcade match end: accuracy first; only all-round perfect builds qualify for the time tie-break. */
    private void finishScoredMatch() {
        if (phase != Phase.RESULTS) return;
        List<PlayerState> builders = activePlayers();
        if (builders.isEmpty()) {
            finishMatch(null, "No builders remain connected.");
            return;
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Long> eligibleBuildTimes = new LinkedHashMap<>();
        for (PlayerState state : builders) {
            String id = state.player.getUniqueId().toString();
            totals.put(id, state.totalPoints);
            if (state.perfectedEveryBuild && state.perfectBuilds == matchRoundLimit) {
                eligibleBuildTimes.put(id, state.totalBuildTimeTicks);
            }
        }
        Set<String> winnerIds = new LinkedHashSet<>(
                SpeedBuildersRules.winnersByScoreThenBuildTime(totals, eligibleBuildTimes));
        List<PlayerState> winners = builders.stream()
                .filter(state -> winnerIds.contains(state.player.getUniqueId().toString()))
                .sorted(java.util.Comparator.comparing(state -> state.player.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        String scoreboard = builders.stream()
                .sorted(java.util.Comparator.comparingInt((PlayerState state) -> state.totalPoints).reversed()
                        .thenComparing((PlayerState state) ->
                                !(state.perfectedEveryBuild && state.perfectBuilds == matchRoundLimit))
                        .thenComparingLong(state -> state.perfectedEveryBuild
                                && state.perfectBuilds == matchRoundLimit
                                ? state.totalBuildTimeTicks : Long.MAX_VALUE)
                        .thenComparing(state -> state.player.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(state -> {
                    String result = state.player.getName() + " " + state.totalPoints + " pts";
                    if (state.perfectedEveryBuild && state.perfectBuilds == matchRoundLimit) {
                        result += " / " + formatBuildTime(state.totalBuildTimeTicks);
                    }
                    return result;
                })
                .collect(java.util.stream.Collectors.joining(ChatColor.GRAY + " · " + ChatColor.WHITE));
        Bukkit.broadcastMessage(ChatColor.GOLD + "Final Speed Builders scores (build time shown only for players who perfected every build): "
                + ChatColor.WHITE + scoreboard);
        finishMatchWithWinners(winners, "Highest cumulative accuracy; only players who perfected every build qualify for the cumulative-time tie-break.");
    }

    private String formatBuildTime(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.2fs", ticks * 0.05D);
    }

    private void finishMatch(Player winner, String reason) {
        Set<UUID> winnerIds = winner == null ? Set.of() : Set.of(winner.getUniqueId());
        finishMatchByIds(winnerIds, reason);
    }

    private void finishMatchWithWinners(List<PlayerState> winners, String reason) {
        Set<UUID> winnerIds = winners.stream()
                .map(state -> state.player.getUniqueId())
                .collect(java.util.stream.Collectors.toSet());
        finishMatchByIds(winnerIds, reason);
    }

    private void finishMatchByIds(Set<UUID> winnerIds, String reason) {
        if (phase == Phase.IDLE) return;
        phase = Phase.IDLE;
        for (PlayerState state : players.values()) removeMobs(state.mobs);
        currentTemplate = null;
        phaseDeadlineTick = 0L;
        queueDeadlineTick = -1L;
        queue.clear();
        stopReason = reason == null ? "" : reason;

        if (!winnerIds.isEmpty()) {
            String winnerNames = players.values().stream()
                    .filter(state -> winnerIds.contains(state.player.getUniqueId()))
                    .map(state -> state.player.getName())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(java.util.stream.Collectors.joining(ChatColor.YELLOW + ", " + ChatColor.GOLD));
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + winnerNames
                    + ChatColor.YELLOW + (winnerIds.size() == 1 ? " wins " : " tie for ")
                    + "Speed Builders!");
        } else {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Speed Builders ended. " + stopReason);
        }
        persistMatchProgress(winnerIds);
        for (PlayerState state : matchHistory) {
            if (round > 0 && state.perfectBuilds == round) {
                SpeedBuildersProgressStore.increment(plugin.getConfig(), state.player.getUniqueId(),
                        "perfectionist-wins", 1L);
            }
        }
        plugin.saveConfig();
        for (PlayerState state : new ArrayList<>(players.values())) {
            Player player = state.player;
            restorePlot(state.plot);
            if (player.isOnline()) {
                if (winnerIds.contains(player.getUniqueId())) {
                    player.sendTitle(ChatColor.GOLD + "VICTORY!",
                            ChatColor.YELLOW.toString() + state.totalPoints + " points", 5, 70, 15);
                }
                restoreAfterReturningToLobby(player, state.snapshot);
            }
        }
        players.clear();
        matchHistory.clear();
        refreshHud();
        releaseSessionLockAfterRestoration();
    }

    private void persistMatchProgress(Set<UUID> winnerIds) {
        for (PlayerState state : players.values()) {
            UUID playerId = state.player.getUniqueId();
            SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "blocks-placed", state.blocksPlaced);
            SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "blocks-broken", state.blocksBroken);
            SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "mobs-built", state.mobsBuilt);
            SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "perfect-builds", state.perfectBuilds);
            if (winnerIds.contains(playerId)) {
                SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "wins", 1L);
            }
        }
        if (!players.isEmpty()) plugin.saveConfig();
    }

    private void persistPlayerProgress(PlayerState state) {
        UUID playerId = state.player.getUniqueId();
        SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "blocks-placed", state.blocksPlaced);
        SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "blocks-broken", state.blocksBroken);
        SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "mobs-built", state.mobsBuilt);
        SpeedBuildersProgressStore.increment(plugin.getConfig(), playerId, "perfect-builds", state.perfectBuilds);
        plugin.saveConfig();
    }

    private int score(PlayerState state) {
        Plot plot = state.plot;
        Map<SpeedBuildersRules.Coordinate, SpeedBuildersRules.BlockState> actual = new LinkedHashMap<>();
        for (SpeedBuildersRules.Cell cell : currentTemplate.cells()) {
            int x = plot.centerX() - 3 + cell.x();
            int y = plot.baseY() + cell.y();
            int z = plot.centerZ() - 3 + cell.z();
            Block block = plot.world().getBlockAt(x, y, z);
            if (!block.getType().isAir()) {
                actual.put(cell.coordinate(), new SpeedBuildersRules.BlockState(
                        block.getType(), block.getBlockData().getAsString()));
            }
        }
        int blockScore = SpeedBuildersRules.scoreBlockStates(currentTemplate.cells(), actual);
        int mobScore = 0;
        List<Entity> unmatched = new ArrayList<>(state.mobs);
        for (MobCell expected : currentTemplate.mobs()) {
            for (Entity entity : new ArrayList<>(unmatched)) {
                Location location = entity.getLocation();
                int x = location.getBlockX() - (plot.centerX() - 3);
                int y = location.getBlockY() - plot.baseY();
                int z = location.getBlockZ() - (plot.centerZ() - 3);
                if (entity.getType() == expected.type() && x == expected.x() && y == expected.y() && z == expected.z()) {
                    mobScore++;
                    unmatched.remove(entity);
                    break;
                }
            }
        }
        return blockScore + mobScore;
    }

    private int percent(int score) {
        return currentTemplate == null || currentTemplate.expectedScore() == 0
                ? 0 : (int) Math.round(score * 100.0D / currentTemplate.expectedScore());
    }

    private void pasteTemplate(Plot plot, Template template) {
        for (GroundCell cell : template.ground()) {
            Block floor = plot.world().getBlockAt(plot.centerX() - 3 + cell.x(), plot.baseY() - 1,
                    plot.centerZ() - 3 + cell.z());
            floor.setBlockData(Bukkit.createBlockData(cell.blockData()), false);
        }
        for (SpeedBuildersRules.Cell cell : template.cells()) {
            Block block = plot.world().getBlockAt(plot.centerX() - 3 + cell.x(), plot.baseY() + cell.y(),
                    plot.centerZ() - 3 + cell.z());
            if (cell.blockData().isBlank()) block.setType(cell.material(), false);
            else block.setBlockData(Bukkit.createBlockData(cell.blockData()), false);
        }
    }

    private Material spawnEggMaterial(EntityType type) {
        return type == null ? null : Material.matchMaterial(type.name() + "_SPAWN_EGG");
    }

    private void spawnTemplateMobs(PlayerState state, Plot plot, Template template) {
        for (MobCell mob : template.mobs()) {
            Location location = new Location(plot.world(),
                    plot.centerX() - 3 + mob.x() + 0.5D,
                    plot.baseY() + mob.y(),
                    plot.centerZ() - 3 + mob.z() + 0.5D);
            Entity entity = spawnDisplayMob(location, mob.type());
            if (entity != null) state.mobs.add(entity);
        }
    }

    private Entity spawnDisplayMob(Location location, EntityType type) {
        try {
            Entity entity = location.getWorld().spawnEntity(location, type);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setPersistent(false);
            if (entity instanceof LivingEntity living) living.setCollidable(false);
            if (entity instanceof Mob mob) mob.setAI(false);
            return entity;
        } catch (RuntimeException spawnFailure) {
            plugin.getLogger().warning("Could not spawn Speed Builders template mob " + type + ": "
                    + spawnFailure.getMessage());
            return null;
        }
    }

    private void removeMobs(List<Entity> mobs) {
        for (Entity entity : new ArrayList<>(mobs)) {
            if (entity != null && entity.isValid()) entity.remove();
        }
        mobs.clear();
    }

    private void giveTemplateBlocks(Player player, Template template) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        boolean needsWaterBucket = false;
        boolean needsLavaBucket = false;
        for (SpeedBuildersRules.Cell cell : template.cells()) {
            BlockData data = Bukkit.createBlockData(cell.blockData());
            if (data instanceof Bisected bisected && bisected.getHalf() == Bisected.Half.TOP) continue;
            if (data instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) continue;
            if (cell.material() == Material.WATER) {
                needsWaterBucket = true;
                continue;
            }
            if (cell.material() == Material.LAVA) {
                needsLavaBucket = true;
                continue;
            }
            counts.merge(cell.material(), 1, Integer::sum);
        }
        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            int remaining = entry.getValue();
            while (remaining > 0) {
                int stack = Math.min(64, remaining);
                player.getInventory().addItem(new ItemStack(entry.getKey(), stack));
                remaining -= stack;
            }
        }
        if (needsWaterBucket) player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
        if (needsLavaBucket) player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
        for (MobCell mob : template.mobs()) {
            Material spawnEgg = spawnEggMaterial(mob.type());
            if (spawnEgg != null) player.getInventory().addItem(new ItemStack(spawnEgg));
        }
    }

    private void clearPlotBuildVolume(Plot plot) {
        if (plot == null) return;
        for (int x = -3; x <= 3; x++) {
            for (int y = 0; y < SpeedBuildersRules.BUILD_HEIGHT; y++) {
                for (int z = -3; z <= 3; z++) {
                    plot.world().getBlockAt(plot.centerX() + x, plot.baseY() + y,
                            plot.centerZ() + z).setType(Material.AIR, false);
                }
            }
        }
    }

    private void restorePlot(Plot plot) {
        if (plot == null || plot.world() == null) return;
        clearPlotBuildVolume(plot);
        for (GroundCell cell : plot.originalGround()) {
            Block floor = plot.world().getBlockAt(plot.centerX() - 3 + cell.x(), plot.baseY() - 1,
                    plot.centerZ() - 3 + cell.z());
            floor.setBlockData(Bukkit.createBlockData(cell.blockData()), false);
        }
    }

    private Plot plotFor(int index) {
        int arenaIndex = SpeedBuildersRules.arenaIndexForPlayer(index);
        int plotIndex = SpeedBuildersRules.plotIndexForPlayer(index);
        int flattenedIndex = arenaIndex * SpeedBuildersRules.PLOTS_PER_ARENA + plotIndex;
        if (flattenedIndex >= playerPlots.size()) {
            throw new IllegalArgumentException("No authored Speed Builders plot for player index " + index);
        }
        return playerPlots.get(flattenedIndex);
    }

    private void preparePlayerForRound(PlayerState state) {
        Player player = state.player;
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        teleportToPlot(state);
    }

    private void teleportToPlot(PlayerState state) {
        Location spawn = nearestSourceSpawn(state.plot);
        Location target = new Location(state.plot.world(), state.plot.centerX() + 0.5D,
                state.plot.baseY(), state.plot.centerZ() + 0.5D);
        spawn.setDirection(target.toVector().subtract(spawn.toVector()));
        state.player.teleport(spawn);
    }

    private List<PlayerState> activePlayers() {
        return players.values().stream().filter(state -> state.player.isOnline()).toList();
    }

    private boolean isInBuildVolume(PlayerState state, org.bukkit.block.Block block) {
        return block.getWorld() == state.plot.world()
                && block.getX() >= state.plot.centerX() - 3
                && block.getX() <= state.plot.centerX() + 3
                && block.getY() >= state.plot.baseY()
                && block.getY() <= state.plot.baseY() + SpeedBuildersRules.BUILD_HEIGHT - 1
                && block.getZ() >= state.plot.centerZ() - 3
                && block.getZ() <= state.plot.centerZ() + 3;
    }

    private void refreshHud() {
        if (phase == Phase.IDLE) return;
        long ticksLeft = Math.max(0L, phaseDeadlineTick - tick);
        long secondsLeft = (ticksLeft + 19L) / 20L;
        for (PlayerState state : players.values()) {
            if (!state.player.isOnline()) continue;
            String phaseLabel = switch (phase) {
                case VIEWING -> "MEMORISE";
                case BUILDING -> "BUILD";
                case INTERMISSION -> "ROUND RESULTS";
                case RESULTS -> "FINAL RESULTS";
                default -> "IDLE";
            };
            String buildName = currentTemplate == null ? "—" : currentTemplate.name();
            String scoreLabel = phase == Phase.INTERMISSION || phase == Phase.RESULTS ? percent(state.score) + "%" : "—";
            state.player.sendActionBar(Component.text(phaseLabel + " | " + buildName
                    + " | Round " + round + "/" + matchRoundLimit + " | " + secondsLeft + "s | Score " + scoreLabel + " | Total " + state.totalPoints));
            drawScoreboard(state, phaseLabel, buildName, secondsLeft);
        }
    }

    private void drawScoreboard(PlayerState state, String phaseLabel, String buildName, long secondsLeft) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("speedbuild", Criteria.DUMMY,
                ChatColor.GOLD + "" + ChatColor.BOLD + "SPEED BUILDERS");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<String> lines = List.of(
                ChatColor.YELLOW + "Round: " + ChatColor.WHITE + round + "/" + matchRoundLimit,
                ChatColor.YELLOW + "Phase: " + ChatColor.WHITE + phaseLabel,
                ChatColor.YELLOW + "Model: " + ChatColor.WHITE + buildName,
                ChatColor.YELLOW + "Time: " + ChatColor.WHITE + secondsLeft + "s",
                ChatColor.YELLOW + "Your score: " + ChatColor.WHITE
                        + (phase == Phase.INTERMISSION || phase == Phase.RESULTS ? percent(state.score) + "%" : "—"),
                ChatColor.YELLOW + "Round points: " + ChatColor.WHITE.toString() + state.roundPoints,
                ChatColor.YELLOW + "Total points: " + ChatColor.WHITE.toString() + state.totalPoints,
                    ChatColor.YELLOW + "Perfect builds: " + ChatColor.WHITE + state.perfectBuilds,
                ChatColor.YELLOW + "Builders: " + ChatColor.WHITE + activePlayers().size()
        );
        for (int i = 0; i < lines.size(); i++) {
            String entry = ChatColor.values()[i + 1].toString();
            Team team = board.registerNewTeam("sb" + i);
            team.addEntry(entry);
            team.setPrefix(lines.get(i));
            objective.getScore(entry).setScore(lines.size() - i);
        }
        state.player.setScoreboard(board);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnEggUse(PlayerInteractEvent event) {
        if (phase != Phase.BUILDING || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null || event.getItem() == null) return;
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || state.perfect) return;
        String itemName = event.getItem().getType().name();
        if (!itemName.endsWith("_SPAWN_EGG")) return;
        String typeName = itemName.substring(0, itemName.length() - "_SPAWN_EGG".length());
        EntityType type;
        try {
            type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException invalidEgg) {
            return;
        }
        if (currentTemplate.mobs().stream().noneMatch(mob -> mob.type() == type)) return;
        Block target = event.getClickedBlock().getRelative(event.getBlockFace());
        if (!isInBuildVolume(state, target)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        Location spawn = target.getLocation().add(0.5D, 0.0D, 0.5D);
        Entity entity = spawnDisplayMob(spawn, type);
        if (entity == null) return;
        // A player-placed mob is hittable for removal, but never wanders away or damages anyone.
        entity.setInvulnerable(false);
        if (entity instanceof LivingEntity living) living.setCollidable(true);
        if (entity instanceof Mob mob) mob.setAI(false);
        state.mobs.add(entity);
        ItemStack held = event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        if (held != null) {
            if (held.getAmount() <= 1) {
                if (event.getHand() == EquipmentSlot.OFF_HAND) event.getPlayer().getInventory().setItemInOffHand(null);
                else event.getPlayer().getInventory().setItemInMainHand(null);
            } else held.setAmount(held.getAmount() - 1);
        }
        state.mobsBuilt++;
        Bukkit.getScheduler().runTask(plugin, () -> checkPerfectBuild(state));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTemplateMobHit(EntityDamageByEntityEvent event) {
        if (phase != Phase.BUILDING || !(event.getDamager() instanceof Player player)) return;
        PlayerState state = players.get(player.getUniqueId());
        if (state == null || state.perfect || !state.mobs.contains(event.getEntity())) return;
        event.setCancelled(true);
        Entity mob = event.getEntity();
        state.mobs.remove(mob);
        EntityType type = mob.getType();
        mob.remove();
        Material egg = spawnEggMaterial(type);
        if (egg != null) player.getInventory().addItem(new ItemStack(egg));
        Bukkit.getScheduler().runTask(plugin, () -> checkPerfectBuild(state));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildInteraction(PlayerInteractEvent event) {
        if (phase != Phase.BUILDING || event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null ) return;
        if (state.perfect) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedBlock() != null) {
            Block target = event.getAction() == Action.LEFT_CLICK_BLOCK
                    ? event.getClickedBlock()
                    : event.getClickedBlock().getRelative(event.getBlockFace());
            if (!isInBuildVolume(state, target)) event.setCancelled(true);
        }
        Bukkit.getScheduler().runTask(plugin, () -> checkPerfectBuild(state));
    }

    @EventHandler
    public void onJumpFlight(PlayerToggleFlightEvent event) {
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || (phase != Phase.BUILDING && phase != Phase.VIEWING)
                || state.perfect) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.playSound(player.getEyeLocation(), org.bukkit.Sound.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
        player.setVelocity(new org.bukkit.util.Vector(0.0D, 1.0D, 0.0D));
    }

    private boolean isWithinPlot(Plot plot, Block block) {
        return plot != null && block.getWorld() == plot.world()
                && block.getX() >= plot.centerX() - 3 && block.getX() <= plot.centerX() + 3
                && block.getY() >= plot.baseY() - 1 && block.getY() <= plot.baseY() + 6
                && block.getZ() >= plot.centerZ() - 3 && block.getZ() <= plot.centerZ() + 3;
    }

    private boolean isArenaWorld(World world) {
        return world != null && arenaWorlds.contains(world);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLiquidLeak(BlockFromToEvent event) {
        if (!isArenaWorld(event.getBlock().getWorld())) return;
        Block from = event.getBlock();
        Block to = event.getToBlock();
        for (Plot plot : playerPlots) {
            if (isWithinPlot(plot, from) != isWithinPlot(plot, to)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildPhysics(BlockPhysicsEvent event) {
        if (isArenaWorld(event.getBlock().getWorld()) && !event.getBlock().isLiquid()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildGrowth(BlockGrowEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildSpread(BlockSpreadEvent event) {
        if (isArenaWorld(event.getBlock().getWorld()) || isArenaWorld(event.getSource().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildForm(BlockFormEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildFade(BlockFadeEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildBurn(BlockBurnEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildLeavesDecay(LeavesDecayEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildStructureGrow(StructureGrowEvent event) {
        if (isArenaWorld(event.getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isArenaWorld(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBuildCombust(EntityCombustEvent event) {
        if (isArenaWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || phase != Phase.BUILDING || state.perfect) {
            if (players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
            return;
        }
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (!isInBuildVolume(state, target)) {
            event.setCancelled(true);
            return;
        }
        if (target.isLiquid()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || phase != Phase.BUILDING || state.perfect) {
            if (players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
            return;
        }
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (!isInBuildVolume(state, target)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || event.getBlockPlaced().getWorld() != state.plot.world()) return;
        if (phase != Phase.BUILDING || state.perfect
                || !isInBuildVolume(state, event.getBlockPlaced())) {
            event.setCancelled(true);
            return;
        }
        if (event.getBlockPlaced().getBlockData() instanceof Bisected bisected) {
            Block companion = event.getBlockPlaced().getRelative(
                    bisected.getHalf() == Bisected.Half.TOP
                            ? org.bukkit.block.BlockFace.DOWN : org.bukkit.block.BlockFace.UP);
            if (!isInBuildVolume(state, companion)) {
                event.setCancelled(true);
                return;
            }
        } else if (event.getBlockPlaced().getBlockData() instanceof Bed bed) {
            Block companion = event.getBlockPlaced().getRelative(bed.getPart() == Bed.Part.HEAD
                    ? bed.getFacing().getOppositeFace() : bed.getFacing());
            if (!isInBuildVolume(state, companion)) {
                event.setCancelled(true);
                return;
            }
        }
        state.blocksPlaced++;
        Bukkit.getScheduler().runTask(plugin, () -> checkPerfectBuild(state));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null || event.getBlock().getWorld() != state.plot.world()) return;
        if (phase != Phase.BUILDING || state.perfect
                || !isInBuildVolume(state, event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        // Reborn runs InstaBreak=true: remove the clicked block immediately and refund its item.
        event.setCancelled(true);
        Block block = event.getBlock();
        Material material = block.getType();
        if (material == Material.AIR) return;

        // Reborn returns dismantled build pieces to the palette regardless of tool tier.
        List<ItemStack> drops = new ArrayList<>();
        boolean upperHalf = false;
        Block otherHalf = null;
        if (block.getBlockData() instanceof Bisected bisected) {
            upperHalf = bisected.getHalf() == Bisected.Half.TOP;
            otherHalf = block.getRelative(upperHalf ? org.bukkit.block.BlockFace.DOWN : org.bukkit.block.BlockFace.UP);
        } else if (block.getBlockData() instanceof Bed bed) {
            upperHalf = bed.getPart() == Bed.Part.HEAD;
            otherHalf = block.getRelative(bed.getPart() == Bed.Part.HEAD
                    ? bed.getFacing().getOppositeFace() : bed.getFacing());
        }
        if (!upperHalf && material != Material.WATER && material != Material.LAVA
                && material != Material.AIR) {
            drops.add(new ItemStack(material));
        }
        if (upperHalf) drops.clear();
        for (ItemStack drop : drops) {
            if (drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0) {
                event.getPlayer().getInventory().addItem(drop);
            }
        }
        block.setType(Material.AIR, false);
        if (otherHalf != null && otherHalf.getType() == material) otherHalf.setType(Material.AIR, false);
        state.blocksBroken++;
        Bukkit.getScheduler().runTask(plugin, () -> checkPerfectBuild(state));
    }

    private void recordBuildTime(PlayerState state, long elapsedTicks) {
        if (state.buildTimeRecorded) return;
        state.roundBuildTimeTicks = Math.max(0L, elapsedTicks);
        state.totalBuildTimeTicks += state.roundBuildTimeTicks;
        state.buildTimeRecorded = true;
    }

    private void markPerfectBuild(PlayerState state) {
        if (state.perfect || !state.player.isOnline()) return;
        recordBuildTime(state, Math.max(0L, tick - buildPhaseStartedTick));
        state.perfect = true;
        state.perfectBuilds++;
        long elapsedMillis = state.roundBuildTimeTicks * 50L;
        if (!firstPerfectBuildAwarded) {
            firstPerfectBuildAwarded = true;
            SpeedBuildersProgressStore.increment(plugin.getConfig(), state.player.getUniqueId(), "perfect-first", 1L);
        }
        if (elapsedMillis < 10_000L) {
            SpeedBuildersProgressStore.increment(plugin.getConfig(), state.player.getUniqueId(), "speediest-builderizer", 1L);
        }
        plugin.saveConfig();
        Bukkit.broadcastMessage(ChatColor.GOLD + state.player.getName()
                + ChatColor.YELLOW + " got a perfect build!");
        state.player.sendTitle(ChatColor.GREEN + "PERFECT BUILD!",
                ChatColor.YELLOW + "You've recreated every block.",
                0, 40, 10);
        state.player.playSound(state.player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                0.8F, 1.15F);
    }

    private void checkPerfectBuild(PlayerState state) {
        if (phase != Phase.BUILDING || state.perfect || !state.player.isOnline()) return;
        int score = score(state);
        if (score == currentTemplate.expectedScore()) {
            markPerfectBuild(state);
            if (activePlayers().stream().allMatch(player -> player.perfect)) finishBuildingPhase();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (phase != Phase.BUILDING && phase != Phase.VIEWING) return;
        PlayerState state = players.get(event.getPlayer().getUniqueId());
        if (state == null ) return;
        Location to = event.getTo();
        if (to == null) return;
        if (event.getFrom().getWorld() == state.plot.world() && to.getWorld() != state.plot.world()) {
            event.setTo(event.getFrom());
            return;
        }
        if ((phase == Phase.BUILDING || phase == Phase.VIEWING) && event.getPlayer().isOnGround()
                && !event.getPlayer().isFlying() && !event.getPlayer().getAllowFlight()) {
            event.getPlayer().setAllowFlight(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && players.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (event.getInventory().getLocation() != null
                && isArenaWorld(event.getInventory().getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        queue.remove(id);
        queueDeadlineTick = queue.size() < SpeedBuildersRules.MIN_PLAYERS ? -1L : queueDeadlineTick;
        PlayerState removed = players.remove(id);
        if (removed == null || phase == Phase.IDLE) return;
        persistPlayerProgress(removed);
        removeMobs(removed.mobs);
        restorePlot(removed.plot);
        if (activePlayers().isEmpty()) finishMatch(null, "No builders remain connected.");
    }

    private record Plot(World world, int centerX, int baseY, int centerZ, List<GroundCell> originalGround) { }
    private record GroundCell(int x, int z, Material material, String blockData) { }

    /** Durable pre-match player snapshot so disconnects and server restarts are safe. */
    private static final class PlayerSnapshot {
        private final Location location;
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offhand;
        private final GameMode gameMode;
        private final List<PotionEffect> effects;
        private final float exp;
        private final int level;
        private final int food;
        private final float saturation;
        private final double health;
        private final float walkSpeed;
        private final float flySpeed;
        private final boolean allowFlight;
        private final boolean flying;
        private final boolean invulnerable;
        private final boolean collidable;
        private final boolean glowing;
        private final int heldSlot;
        private final int fireTicks;
        private final float fallDistance;
        private final Vector velocity;
        private final Scoreboard scoreboard;

        private PlayerSnapshot(Player player) {
            this.location = player.getLocation().clone();
            this.contents = cloneItems(player.getInventory().getContents());
            this.armor = cloneItems(player.getInventory().getArmorContents());
            ItemStack hand = player.getInventory().getItemInOffHand();
            this.offhand = hand == null ? null : hand.clone();
            this.gameMode = player.getGameMode();
            this.effects = new ArrayList<>(player.getActivePotionEffects());
            this.exp = player.getExp();
            this.level = player.getLevel();
            this.food = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.health = player.getHealth();
            this.walkSpeed = player.getWalkSpeed();
            this.flySpeed = player.getFlySpeed();
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
            this.invulnerable = player.isInvulnerable();
            this.collidable = player.isCollidable();
            this.glowing = player.isGlowing();
            this.heldSlot = player.getInventory().getHeldItemSlot();
            this.fireTicks = player.getFireTicks();
            this.fallDistance = player.getFallDistance();
            this.velocity = player.getVelocity().clone();
            this.scoreboard = player.getScoreboard();
        }

        private PlayerSnapshot(ConfigurationSection section) {
            Object savedLocation = section.get("location");
            this.location = savedLocation instanceof Location saved ? saved.clone() : null;
            this.contents = readItems(section.getList("contents"));
            this.armor = readItems(section.getList("armor"));
            Object savedOffhand = section.get("offhand");
            this.offhand = savedOffhand instanceof ItemStack item ? item.clone() : null;
            GameMode savedMode;
            try {
                savedMode = GameMode.valueOf(section.getString("game-mode", GameMode.SURVIVAL.name()));
            } catch (IllegalArgumentException invalidMode) {
                savedMode = GameMode.SURVIVAL;
            }
            this.gameMode = savedMode;
            List<PotionEffect> savedEffects = new ArrayList<>();
            for (Object value : section.getList("effects", List.of())) {
                if (value instanceof PotionEffect effect) savedEffects.add(effect);
            }
            this.effects = savedEffects;
            this.exp = (float) section.getDouble("exp", 0.0D);
            this.level = section.getInt("level", 0);
            this.food = section.getInt("food", 20);
            this.saturation = (float) section.getDouble("saturation", 20.0D);
            this.health = section.getDouble("health", 20.0D);
            this.walkSpeed = (float) section.getDouble("walk-speed", 0.2D);
            this.flySpeed = (float) section.getDouble("fly-speed", 0.1D);
            this.allowFlight = section.getBoolean("allow-flight");
            this.flying = section.getBoolean("flying");
            this.invulnerable = section.getBoolean("invulnerable");
            this.collidable = section.getBoolean("collidable", true);
            this.glowing = section.getBoolean("glowing", false);
            this.heldSlot = section.getInt("held-slot", 0);
            this.fireTicks = section.getInt("fire-ticks", 0);
            this.fallDistance = (float) section.getDouble("fall-distance", 0.0D);
            Object savedVelocity = section.get("velocity");
            this.velocity = savedVelocity instanceof Vector saved ? saved.clone() : new Vector();
            this.scoreboard = null;
        }

        private void persist(ArcadeMechanicsModernPlugin plugin, UUID playerId) {
            String path = "speed-builders-pending-restores." + playerId;
            plugin.getConfig().set(path, null);
            ConfigurationSection section = plugin.getConfig().createSection(path);
            section.set("location", location);
            section.set("contents", java.util.Arrays.asList(cloneItems(contents)));
            section.set("armor", java.util.Arrays.asList(cloneItems(armor)));
            section.set("offhand", offhand == null ? null : offhand.clone());
            section.set("game-mode", gameMode.name());
            section.set("effects", new ArrayList<>(effects));
            section.set("exp", exp);
            section.set("level", level);
            section.set("food", food);
            section.set("saturation", saturation);
            section.set("health", health);
            section.set("walk-speed", walkSpeed);
            section.set("fly-speed", flySpeed);
            section.set("allow-flight", allowFlight);
            section.set("flying", flying);
            section.set("invulnerable", invulnerable);
            section.set("collidable", collidable);
            section.set("glowing", glowing);
            section.set("held-slot", heldSlot);
            section.set("fire-ticks", fireTicks);
            section.set("fall-distance", fallDistance);
            section.set("velocity", velocity.clone());
            plugin.saveConfig();
        }

        private void restore(Player player) {
            if (player == null || !player.isOnline()) return;
            player.leaveVehicle();
            if (location != null && location.getWorld() != null) player.teleport(location);
            player.getInventory().clear();
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armor));
            player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
            player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, heldSlot)));
            for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) player.addPotionEffect(effect);
            player.setGameMode(gameMode);
            player.setExp(exp);
            player.setLevel(level);
            player.setFoodLevel(food);
            player.setSaturation(saturation);
            player.setWalkSpeed(walkSpeed);
            player.setFlySpeed(flySpeed);
            player.setInvulnerable(invulnerable);
            player.setCollidable(collidable);
            player.setGlowing(glowing);
            player.setAllowFlight(allowFlight);
            if (allowFlight) player.setFlying(flying);
            double safeHealth = Math.max(1.0D, Math.min(health, player.getMaxHealth()));
            player.setHealth(safeHealth);
            player.setFireTicks(Math.max(0, fireTicks));
            player.setFallDistance(Math.max(0.0F, fallDistance));
            player.setVelocity(velocity.clone());
            if (scoreboard != null) player.setScoreboard(scoreboard);
        }

        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] result = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) result[i] = source[i] == null ? null : source[i].clone();
            return result;
        }

        private static ItemStack[] readItems(List<?> values) {
            if (values == null) return new ItemStack[0];
            ItemStack[] result = new ItemStack[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value instanceof ItemStack item) result[i] = item.clone();
                else if (value instanceof Map<?, ?> serialized) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : serialized.entrySet()) {
                        if (entry.getKey() instanceof String key) data.put(key, entry.getValue());
                    }
                    try {
                        result[i] = ItemStack.deserialize(data);
                    } catch (RuntimeException ignored) {
                        result[i] = null;
                    }
                }
            }
            return result;
        }
    }

    private static final class PlayerState {
        private final Player player;
        private final Plot plot;
        private final PlayerSnapshot snapshot;
        private boolean perfect;
        private int score;
        private int roundPoints;
        private int totalPoints;
        private long roundBuildTimeTicks;
        private long totalBuildTimeTicks;
        private boolean buildTimeRecorded;
        private boolean perfectedEveryBuild = true;
        private int perfectBuilds;
        private int blocksPlaced;
        private int blocksBroken;
        private int mobsBuilt;
        private final List<Entity> mobs = new ArrayList<>();

        private PlayerState(Player player, Plot plot, ArcadeMechanicsModernPlugin plugin) {
            this.player = player;
            this.plot = plot;
            this.snapshot = new PlayerSnapshot(player);
            this.snapshot.persist(plugin, player.getUniqueId());
        }
    }

    private record Template(String id, String name, List<SpeedBuildersRules.Cell> cells,
                            List<MobCell> mobs, List<GroundCell> ground,
                            SpeedBuildersRules.TemplateTiming timing) {
        private int expectedScore() { return cells.size() + mobs.size(); }
    }

    private record MobCell(EntityType type, int x, int y, int z) { }


}
