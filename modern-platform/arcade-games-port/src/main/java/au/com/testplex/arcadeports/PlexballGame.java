package au.com.testplex.arcadeports;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Experimental, server-authoritative Plexball game for Paper 26.3.
 *
 * The module intentionally owns only its game session and arena. It is hosted
 * by ArcadeMechanicsModern and returns participants through the shared Arcade
 * lobby when the round finishes.
 */
public final class PlexballGame implements Listener {
    public static final String WORLD_NAME = "testplex_plexball_arena_v1";
    private static final int FLOOR_Y = 64;
    private static final int ARENA_MIN_Z = -100;
    private static final int SNAPSHOT_RETENTION = PlexballRules.PLAYER_HISTORY_TICKS;
    private static final int QUEUE_COUNTDOWN_TICKS = 100;
    private static final int QUICK_COUNTDOWN_TICKS = 40;
    private static final int INTRO_TICKS = 40;
    private static final int TURN_BREAK_TICKS = 12;
    private static final int BALL_MAX_FLIGHT_TICKS = 85;
    private static final long INPUT_GRACE_NANOS = PlexballRules.MAX_REWIND_TICKS * 50_000_000L;
    private static final double BALL_RADIUS = 0.24D;
    private static final double CATCH_REACH = PlexballRules.BASE_CATCH_RADIUS;
    private static final double FIELD_PLAYER_EYE_OFFSET = 1.0D;

    private enum Phase { IDLE, INTRO, BATTING, BALL_FLIGHT, TURN_BREAK }

    public enum Kit {
        SLUGGER("slugger", "Slugger", "Power Dive", Material.IRON_SWORD, 1.07D),
        TECHNICIAN("technician", "Technician", "Sure Hands", Material.SHIELD, 1.00D),
        CURVE_ARTIST("curve-artist", "Curve Artist", "Read Flight", Material.COMPASS, 0.97D),
        SPRINTER("sprinter", "Sprinter", "Burst", Material.SUGAR, 1.00D),
        SKYGUARD("skyguard", "Skyguard", "Leap", Material.FEATHER, 0.99D);

        private final String id;
        private final String displayName;
        private final String abilityName;
        private final Material abilityItem;
        private final double battingPower;

        Kit(String id, String displayName, String abilityName, Material abilityItem, double battingPower) {
            this.id = id;
            this.displayName = displayName;
            this.abilityName = abilityName;
            this.abilityItem = abilityItem;
            this.battingPower = battingPower;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public String abilityName() { return abilityName; }
        public static Kit fromId(String id) {
            if (id == null) return null;
            for (Kit kit : values()) {
                if (kit.id.equalsIgnoreCase(id) || kit.name().equalsIgnoreCase(id.replace('-', '_'))) return kit;
            }
            return null;
        }
    }

    private static final class PlayerResult {
        private double bestDistance;
        private int catches;
        private int homeRuns;
        private int atBats;
    }

    private static final class PlayerFrame {
        private final long tick;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private PlayerFrame(long tick, Location location) {
            this.tick = tick;
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }

        private Vector feet() { return new Vector(x, y, z); }
        private Location eyeLocation(World world, double eyeHeight) {
            return new Location(world, x, y + eyeHeight, z, yaw, pitch);
        }
    }

    private static final class CatchAttempt {
        private final long endTick;
        private final double radius;
        private long nextCheckTick;

        private CatchAttempt(long startTick, int windowTicks, double radius) {
            this.nextCheckTick = startTick;
            this.endTick = startTick + windowTicks;
            this.radius = radius;
        }
    }

    private final ArcadeMechanicsModernPlugin plugin;
    private final NamespacedKey actionKey;
    private final File kitFile;
    private final Map<UUID, Kit> selectedKits = new HashMap<>();
    private final LinkedHashMap<UUID, Player> queue = new LinkedHashMap<>();
    private final List<UUID> matchPlayers = new ArrayList<>();
    private final Map<UUID, PlayerResult> results = new HashMap<>();
    private final Map<UUID, Deque<PlayerFrame>> playerHistory = new HashMap<>();
    private final Map<UUID, CatchAttempt> catchAttempts = new HashMap<>();
    private final Set<UUID> abilityUsedThisBall = new HashSet<>();
    private final Set<UUID> catchUsedThisBall = new HashSet<>();
    private final Set<UUID> sureHandsArmed = new HashSet<>();
    private final LinkedHashSet<UUID> homeRunPlayers = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> finalists = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> roundSurvivors = new LinkedHashSet<>();
    private final Map<UUID, Double> roundDistances = new HashMap<>();
    private final TreeMap<Long, Vector> ballHistory = new TreeMap<>();
    private final Random random = new Random();

    private BukkitTask ticker;
    private World arena;
    private Phase phase = Phase.IDLE;
    private long serverTick;
    private long queueCountdownTick = -1L;
    private long matchEndTick;
    private long matchEndNanos;
    private long phaseEndTick;
    private long battingDeadlineTick;
    private long battingDeadlineNanos;
    private long terminalResolutionTick = -1L;
    private double terminalDistance;
    private boolean terminalHomeRun;
    private String terminalMessage;
    private long battingStartTick;
    private long ballStartTick;
    private int initialTurnIndex;
    private int suddenDeathTurnIndex;
    private int timingCycleTicks;
    private int timingPhaseOffset;
    private int ballFlightTicks;
    private boolean suddenDeath;
    private UUID batterId;
    private Vector teeBallPosition;
    private Vector ballPosition;
    private Vector ballVelocity;
    private Vector curveDirection;
    private double curveAcceleration;
    private ItemDisplay ballDisplay;
    private Interaction teeInteraction;

    public PlexballGame(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "plexball-action");
        this.kitFile = new File(plugin.getDataFolder(), "plexball-player-kits.yml");
        loadKits();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public String status() {
        if (phase == Phase.IDLE) {
            return "queued " + queue.size() + "/" + PlexballRules.MAX_PLAYERS;
        }
        return phase.name().toLowerCase(java.util.Locale.ROOT)
                + ", players=" + matchPlayers.size()
                + ", time=" + Math.max(0L, (matchEndNanos - System.nanoTime()) / 1_000_000_000L) + "s";
    }

    public boolean selectKit(Player player, String id) {
        Kit kit = Kit.fromId(id);
        if (kit == null) return false;
        if (isActive() && matchPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Your Plexball kit is locked for this match.");
            return true;
        }
        selectedKits.put(player.getUniqueId(), kit);
        saveKits();
        player.sendMessage(ChatColor.GREEN + "Plexball kit selected: " + ChatColor.GOLD + kit.displayName()
                + ChatColor.GRAY + " — " + kit.abilityName() + ".");
        return true;
    }

    public void queue(Player player, boolean quickPlay) {
        if (isActive()) {
            player.sendMessage(ChatColor.RED + "A Plexball match is already underway.");
            return;
        }
        if (queue.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already queued for Plexball.");
            return;
        }
        if (queue.size() >= PlexballRules.MAX_PLAYERS) {
            player.sendMessage(ChatColor.RED + "The Plexball queue is full (8 players).");
            return;
        }
        queue.put(player.getUniqueId(), player);
        Kit kit = selectedKits.getOrDefault(player.getUniqueId(), Kit.TECHNICIAN);
        player.sendMessage(ChatColor.GREEN + "Queued for Plexball. "
                + ChatColor.GRAY + "Kit: " + kit.displayName() + ". "
                + ChatColor.YELLOW + queue.size() + "/" + PlexballRules.MAX_PLAYERS + " players.");
        if (queue.size() >= PlexballRules.MIN_PLAYERS && queueCountdownTick < 0L) {
            queueCountdownTick = serverTick + (quickPlay ? QUICK_COUNTDOWN_TICKS : QUEUE_COUNTDOWN_TICKS);
            broadcastQueue(ChatColor.GOLD + "Plexball starts in " + (quickPlay ? "2" : "5") + " seconds!");
        }
        if (queue.size() == PlexballRules.MAX_PLAYERS) {
            queueCountdownTick = Math.min(queueCountdownTick < 0L ? serverTick + 20L : queueCountdownTick,
                    serverTick + 20L);
        }
    }

    public void leaveQueue(Player player) {
        UUID id = player.getUniqueId();
        if (queue.remove(id) == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not queued for Plexball.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "You left the Plexball queue.");
        if (queue.size() < PlexballRules.MIN_PLAYERS) queueCountdownTick = -1L;
        broadcastQueue(ChatColor.YELLOW + player.getName() + " left Plexball (" + queue.size() + " queued).");
    }

    public void startQueued(org.bukkit.command.CommandSender sender) {
        if (isActive()) {
            sender.sendMessage(ChatColor.RED + "Plexball is already running.");
            return;
        }
        if (queue.size() < PlexballRules.MIN_PLAYERS) {
            sender.sendMessage(ChatColor.RED + "Plexball needs at least two queued players.");
            return;
        }
        startMatch();
    }

    public void stop(String reason) {
        if (phase == Phase.IDLE) {
            queue.clear();
            queueCountdownTick = -1L;
            return;
        }
        String message = reason == null || reason.isBlank() ? "Plexball stopped." : reason;
        finish(List.of(), message, false);
    }

    public void shutdown() {
        if (ticker != null) ticker.cancel();
        if (phase != Phase.IDLE) {
            cleanupBall();
            for (UUID id : matchPlayers) {
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline()) plugin.returnPlayerToArcadeLobby(player);
            }
        }
        queue.clear();
        phase = Phase.IDLE;
    }

    private void tick() {
        serverTick++;
        if (phase == Phase.IDLE) {
            if (queueCountdownTick >= 0L && serverTick >= queueCountdownTick) {
                if (queue.size() >= PlexballRules.MIN_PLAYERS) startMatch();
                else queueCountdownTick = -1L;
            }
            return;
        }

        recordPlayerSnapshots();
        if (System.nanoTime() >= matchEndNanos) {
            finishAtTimeLimit();
            return;
        }

        switch (phase) {
            case INTRO -> {
                if (serverTick >= phaseEndTick) startNormalAtBat();
            }
            case BATTING -> {
                updateBattingHud();
                if (System.nanoTime() >= battingDeadlineNanos + INPUT_GRACE_NANOS) {
                    plugin.getLogger().fine("Plexball batting timeout for " + batterId);
                    resolveAttempt(0.0D, false, false, "Strike! No swing in time.");
                }
            }
            case BALL_FLIGHT -> updateBallFlight();
            case TURN_BREAK -> {
                if (serverTick >= phaseEndTick) advanceAfterAttempt();
            }
            default -> { }
        }
    }

    private void startMatch() {
        List<Player> participants = new ArrayList<>(queue.values().stream()
                .filter(Player::isOnline)
                .limit(PlexballRules.MAX_PLAYERS)
                .toList());
        Collections.shuffle(participants, random);
        queue.clear();
        queueCountdownTick = -1L;
        if (participants.size() < PlexballRules.MIN_PLAYERS) return;

        // Plexball owns the shared Arcade test lane for the duration of its round.
        plugin.prepareForPlexball();
        arena = ensureArena();
        matchPlayers.clear();
        results.clear();
        homeRunPlayers.clear();
        finalists.clear();
        roundSurvivors.clear();
        roundDistances.clear();
        playerHistory.clear();
        catchAttempts.clear();
        ballHistory.clear();
        abilityUsedThisBall.clear();
        catchUsedThisBall.clear();
        sureHandsArmed.clear();
        initialTurnIndex = 0;
        suddenDeathTurnIndex = 0;
        suddenDeath = false;
        batterId = null;
        matchEndTick = serverTick + PlexballRules.MATCH_LIMIT_TICKS;
        matchEndNanos = System.nanoTime() + 180_000_000_000L;
        phase = Phase.INTRO;
        phaseEndTick = serverTick + INTRO_TICKS;

        for (int i = 0; i < participants.size(); i++) {
            Player player = participants.get(i);
            UUID id = player.getUniqueId();
            matchPlayers.add(id);
            results.put(id, new PlayerResult());
            setupPlayer(player);
            if (i == 0) teleportToBatter(player);
            else teleportToField(player, i);
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PLEXBALL"
                    + ChatColor.GRAY + " — " + selectedKits.getOrDefault(id, Kit.TECHNICIAN).displayName());
            player.sendMessage(ChatColor.YELLOW + "You get one 10-second batting turn. Everyone else fields.");
        }
        broadcastMatch(ChatColor.GREEN + "Plexball is starting! " + ChatColor.YELLOW
                + "The batter has 10 seconds; everyone else is in the outfield.");
    }

    private void startNormalAtBat() {
        while (initialTurnIndex < matchPlayers.size()) {
            UUID candidate = matchPlayers.get(initialTurnIndex);
            Player player = Bukkit.getPlayer(candidate);
            if (player != null && player.isOnline()) {
                startAtBat(candidate, false);
                return;
            }
            initialTurnIndex++;
        }
        completeInitialRound();
    }

    private void startAtBat(UUID playerId, boolean playoff) {
        Player batter = Bukkit.getPlayer(playerId);
        if (batter == null || !batter.isOnline()) {
            resolveAttempt(0.0D, false, false, "Batter unavailable.");
            return;
        }
        batterId = playerId;
        suddenDeath = playoff;
        abilityUsedThisBall.clear();
        catchUsedThisBall.clear();
        sureHandsArmed.clear();
        catchAttempts.clear();
        ballHistory.clear();
        for (UUID id : matchPlayers) {
            Player participant = Bukkit.getPlayer(id);
            if (participant == null || !participant.isOnline()) continue;
            if (!id.equals(playerId) && participant.getWorld() != arena) {
                teleportToField(participant, matchPlayers.indexOf(id));
            }
            if (id.equals(playerId)) {
                teleportToBatter(participant);
                giveBat(participant);
            } else {
                giveFieldingItems(participant);
            }
            // Seed a server-captured baseline after turn positions are final.
            // This prevents a newly teleported batter's old field position/yaw
            // contaminating a ping-compensated swing.
            seedPlayerHistory(participant);
        }

        createTeeBall();
        battingStartTick = serverTick;
        timingCycleTicks = 30 + random.nextInt(13);
        timingPhaseOffset = random.nextInt(timingCycleTicks);
        int battingTicks = playoff ? PlexballRules.SUDDEN_DEATH_BATTING_TICKS
                : PlexballRules.NORMAL_BATTING_TICKS;
        battingDeadlineTick = serverTick + battingTicks;
        battingDeadlineNanos = System.nanoTime() + battingTicks * 50_000_000L;
        phase = Phase.BATTING;
        broadcastMatch(ChatColor.GOLD + batter.getName() + ChatColor.YELLOW
                + (playoff ? " — sudden death! Hit another 100m home run." : " is batting!"));
        updateBattingHud();
    }

    private void completeInitialRound() {
        homeRunPlayers.removeIf(id -> {
            Player player = Bukkit.getPlayer(id);
            return player == null || !player.isOnline();
        });
        if (homeRunPlayers.size() == 1) {
            finish(List.of(homeRunPlayers.iterator().next()), "Home run!", true);
            return;
        }
        if (homeRunPlayers.size() > 1) {
            finalists.clear();
            finalists.addAll(homeRunPlayers);
            roundSurvivors.clear();
            roundDistances.clear();
            suddenDeathTurnIndex = 0;
            suddenDeath = true;
            broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + "SUDDEN DEATH"
                    + ChatColor.YELLOW + " — " + finalists.size() + " players hit 100m. Keep hitting home runs!");
            startPlayoffAtBat();
            return;
        }
        finish(rankPlayers(matchPlayers), "Longest uncaught hit wins!", true);
    }

    private void startPlayoffAtBat() {
        List<UUID> order = new ArrayList<>(finalists);
        while (suddenDeathTurnIndex < order.size()) {
            Player candidate = Bukkit.getPlayer(order.get(suddenDeathTurnIndex));
            if (candidate != null && candidate.isOnline()) break;
            suddenDeathTurnIndex++;
        }
        if (suddenDeathTurnIndex >= order.size()) {
            roundSurvivors.removeIf(id -> {
                Player player = Bukkit.getPlayer(id);
                return player == null || !player.isOnline();
            });
            if (roundSurvivors.size() == 1) {
                finish(List.of(roundSurvivors.iterator().next()), "Survived sudden death!", true);
                return;
            }
            if (roundSurvivors.isEmpty()) {
                List<UUID> completed = order.stream().filter(id -> Bukkit.getPlayer(id) != null
                        && Bukkit.getPlayer(id).isOnline()).toList();
                finish(completed.isEmpty() ? List.of() : rankPlayersByRound(completed),
                        completed.isEmpty() ? "All finalists disconnected." : "No finalist hit another home run. Longest playoff hit wins.", true);
                return;
            }
            finalists.clear();
            finalists.addAll(roundSurvivors);
            roundSurvivors.clear();
            roundDistances.clear();
            suddenDeathTurnIndex = 0;
            order = new ArrayList<>(finalists);
            broadcastMatch(ChatColor.GOLD + "Sudden-death round continues: "
                    + finalists.size() + " home-run hitters remain.");
            while (suddenDeathTurnIndex < order.size()) {
                Player candidate = Bukkit.getPlayer(order.get(suddenDeathTurnIndex));
                if (candidate != null && candidate.isOnline()) break;
                suddenDeathTurnIndex++;
            }
            if (suddenDeathTurnIndex >= order.size()) {
                finish(List.of(), "All remaining finalists disconnected.", true);
                return;
            }
        }
        startAtBat(order.get(suddenDeathTurnIndex), true);
    }

    private void advanceAfterAttempt() {
        UUID previousBatter = batterId;
        if (previousBatter != null) {
            Player player = Bukkit.getPlayer(previousBatter);
            if (player != null && player.isOnline()) {
                teleportToField(player, Math.max(0, matchPlayers.indexOf(previousBatter)));
                giveFieldingItems(player);
            }
        }
        cleanupTeeInteraction();
        if (!suddenDeath) {
            initialTurnIndex++;
            batterId = null;
            if (initialTurnIndex >= matchPlayers.size()) completeInitialRound();
            else startNormalAtBat();
            return;
        }
        suddenDeathTurnIndex++;
        batterId = null;
        startPlayoffAtBat();
    }

    private void resolveAttempt(double distance, boolean caught, boolean homeRun, String message) {
        if (phase != Phase.BATTING && phase != Phase.BALL_FLIGHT) return;
        UUID battingPlayer = batterId;
        if (battingPlayer == null) return;
        PlayerResult result = results.computeIfAbsent(battingPlayer, ignored -> new PlayerResult());
        result.atBats++;
        double recordedDistance = caught ? 0.0D : PlexballRules.clamp(distance, 0.0D, PlexballRules.HOME_RUN_METRES);
        result.bestDistance = Math.max(result.bestDistance, recordedDistance);
        if (homeRun && !caught) {
            result.homeRuns++;
            if (!suddenDeath) homeRunPlayers.add(battingPlayer);
            else roundSurvivors.add(battingPlayer);
        }
        if (suddenDeath) roundDistances.put(battingPlayer, recordedDistance);
        if (message != null && !message.isBlank()) broadcastMatch(message);
        if (caught) {
            broadcastMatch(ChatColor.RED + "CAUGHT! " + ChatColor.YELLOW + "The batting attempt is void.");
        } else if (homeRun) {
            broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + "HOME RUN! "
                    + ChatColor.YELLOW + "100.0m");
        } else if (distance > 0.0D) {
            broadcastMatch(ChatColor.GREEN + "Uncaught hit: " + ChatColor.WHITE
                    + String.format(java.util.Locale.ROOT, "%.1fm", distance));
        }
        cleanupBall();
        broadcastLeaderboard();
        phase = Phase.TURN_BREAK;
        phaseEndTick = serverTick + TURN_BREAK_TICKS;
    }

    private void broadcastLeaderboard() {
        List<UUID> ordered = matchPlayers.stream()
                .sorted((first, second) -> {
                    PlayerResult a = results.getOrDefault(first, new PlayerResult());
                    PlayerResult b = results.getOrDefault(second, new PlayerResult());
                    int distance = Double.compare(b.bestDistance, a.bestDistance);
                    return distance != 0 ? distance : Integer.compare(b.catches, a.catches);
                }).toList();
        broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + "PLEXBALL STANDINGS");
        int rank = 1;
        for (UUID id : ordered) {
            Player participant = Bukkit.getPlayer(id);
            if (participant == null) continue;
            PlayerResult result = results.getOrDefault(id, new PlayerResult());
            broadcastMatch(ChatColor.YELLOW + "" + rank++ + ". " + ChatColor.WHITE + participant.getName()
                    + ChatColor.GRAY + " — " + ChatColor.GREEN
                    + String.format(java.util.Locale.ROOT, "%.1fm", result.bestDistance)
                    + ChatColor.GRAY + " | catches: " + ChatColor.AQUA + result.catches);
        }
    }

    private void finishAtTimeLimit() {
        if (batterId != null && phase == Phase.BALL_FLIGHT && ballPosition != null) {
            PlayerResult current = results.computeIfAbsent(batterId, ignored -> new PlayerResult());
            current.atBats++;
            double distance = PlexballRules.horizontalDistance(teeBallPosition, ballPosition);
            current.bestDistance = Math.max(current.bestDistance,
                    Math.min(PlexballRules.HOME_RUN_METRES, distance));
        } else if (batterId != null && phase == Phase.BATTING) {
            results.computeIfAbsent(batterId, ignored -> new PlayerResult()).atBats++;
        }

        if (suddenDeath && !finalists.isEmpty()) {
            List<UUID> candidates;
            if (!roundSurvivors.isEmpty()) candidates = new ArrayList<>(roundSurvivors);
            else if (!roundDistances.isEmpty()) candidates = new ArrayList<>(roundDistances.keySet());
            else candidates = new ArrayList<>(finalists);
            List<UUID> winners = !roundDistances.isEmpty() && !roundSurvivors.isEmpty()
                    ? rankPlayersByRound(candidates)
                    : (!roundDistances.isEmpty() ? rankPlayersByRound(candidates) : rankPlayers(candidates));
            finish(winners, "Three-minute time limit reached during sudden death.", true);
            return;
        }

        List<UUID> completedAttempts = matchPlayers.stream()
                .filter(id -> results.getOrDefault(id, new PlayerResult()).atBats > 0)
                .toList();
        finish(rankPlayers(completedAttempts), "Three-minute time limit reached.", true);
    }

    private List<UUID> rankPlayers(List<UUID> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        List<UUID> activeCandidates = candidates.stream().filter(id -> {
            Player player = Bukkit.getPlayer(id);
            return player != null && player.isOnline();
        }).toList();
        if (activeCandidates.isEmpty()) return List.of();
        double best = activeCandidates.stream().map(results::get).filter(java.util.Objects::nonNull)
                .mapToDouble(result -> result.bestDistance).max().orElse(0.0D);
        List<UUID> distanceTied = activeCandidates.stream()
                .filter(id -> Math.abs(results.getOrDefault(id, new PlayerResult()).bestDistance - best) < 0.05D)
                .toList();
        int mostCatches = distanceTied.stream()
                .map(results::get).filter(java.util.Objects::nonNull)
                .mapToInt(result -> result.catches).max().orElse(0);
        return distanceTied.stream()
                .filter(id -> results.getOrDefault(id, new PlayerResult()).catches == mostCatches)
                .toList();
    }

    private List<UUID> rankPlayersByRound(List<UUID> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        List<UUID> activeCandidates = candidates.stream().filter(id -> {
            Player player = Bukkit.getPlayer(id);
            return player != null && player.isOnline();
        }).toList();
        if (activeCandidates.isEmpty()) return List.of();
        double best = activeCandidates.stream().mapToDouble(id -> roundDistances.getOrDefault(id, 0.0D)).max().orElse(0.0D);
        List<UUID> distanceTied = activeCandidates.stream()
                .filter(id -> Math.abs(roundDistances.getOrDefault(id, 0.0D) - best) < 0.05D)
                .toList();
        int mostCatches = distanceTied.stream()
                .map(results::get).filter(java.util.Objects::nonNull)
                .mapToInt(result -> result.catches).max().orElse(0);
        return distanceTied.stream()
                .filter(id -> results.getOrDefault(id, new PlayerResult()).catches == mostCatches)
                .toList();
    }

    private void finish(List<UUID> winners, String reason, boolean announce) {
        List<UUID> participants = new ArrayList<>(matchPlayers);
        cleanupBall();
        if (announce && !participants.isEmpty()) {
            if (winners == null || winners.isEmpty()) {
                broadcastMatch(ChatColor.RED + "Plexball ended. " + reason);
            } else {
                String names = winners.stream().map(Bukkit::getPlayer)
                        .filter(java.util.Objects::nonNull).map(Player::getName)
                        .reduce((a, b) -> a + ChatColor.GRAY + ", " + ChatColor.GOLD + b).orElse("Unknown");
                String title = winners.size() == 1 ? "PLEXBALL CHAMPION" : "TIED WINNERS";
                broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + title + ChatColor.YELLOW + ": " + names);
                broadcastMatch(ChatColor.GRAY + reason);
                for (UUID id : winners) {
                    Player winner = Bukkit.getPlayer(id);
                    if (winner != null && winner.isOnline()) {
                        winner.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "WINNER!",
                                ChatColor.YELLOW + "Plexball", 5, 35, 10);
                    }
                }
            }
        }
        phase = Phase.IDLE;
        queueCountdownTick = -1L;
        batterId = null;
        matchPlayers.clear();
        playerHistory.clear();
        catchAttempts.clear();
        abilityUsedThisBall.clear();
        sureHandsArmed.clear();
        homeRunPlayers.clear();
        finalists.clear();
        roundSurvivors.clear();
        roundDistances.clear();
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) plugin.returnPlayerToArcadeLobby(player);
        }
    }

    private void seedPlayerHistory(Player player) {
        Deque<PlayerFrame> frames = playerHistory.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        frames.clear();
        Location location = player.getLocation().clone();
        long first = serverTick - PlexballRules.MAX_REWIND_TICKS;
        for (long tick = first; tick <= serverTick; tick++) frames.addLast(new PlayerFrame(tick, location));
    }

    private void recordPlayerSnapshots() {
        if (arena == null) return;
        for (UUID id : matchPlayers) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.getWorld() != arena) continue;
            Deque<PlayerFrame> frames = playerHistory.computeIfAbsent(id, ignored -> new ArrayDeque<>());
            frames.addLast(new PlayerFrame(serverTick, player.getLocation()));
            while (frames.size() > SNAPSHOT_RETENTION) frames.removeFirst();
        }
    }

    private PlayerFrame frameAt(UUID id, long tick) {
        Deque<PlayerFrame> frames = playerHistory.get(id);
        if (frames == null || frames.isEmpty()) return null;
        PlayerFrame selected = null;
        for (PlayerFrame frame : frames) {
            if (frame.tick > tick) break;
            selected = frame;
        }
        return selected == null ? frames.peekFirst() : selected;
    }

    private void updateBattingHud() {
        Player batter = batterId == null ? null : Bukkit.getPlayer(batterId);
        if (batter == null || !batter.isOnline()) return;
        long effectiveTick = serverTick;
        int phase = PlexballRules.timingDistanceTicks(effectiveTick, battingStartTick,
                timingCycleTicks, timingPhaseOffset);
        int remaining = (int) Math.max(0L, (battingDeadlineNanos - System.nanoTime() + 999_999_999L) / 1_000_000_000L);
        int beatPhase = Math.floorMod((int) (serverTick - battingStartTick - timingPhaseOffset), timingCycleTicks);
        int markerPosition = Math.min(20, beatPhase * 21 / timingCycleTicks);
        StringBuilder beatBar = new StringBuilder(".....................");
        beatBar.setCharAt(10, '|');
        beatBar.setCharAt(markerPosition, '●');
        String timerLabel = remaining == 0 ? "INPUT BUFFER" : remaining + "s";
        batter.sendActionBar(Component.text("BAT  " + timerLabel + "  " + beatBar
                + "  AIM AT BALL + SWING ON THE CENTRE"));
        for (UUID id : matchPlayers) {
            if (id.equals(batterId)) continue;
            Player fielder = Bukkit.getPlayer(id);
            if (fielder != null && fielder.isOnline()) {
                fielder.sendActionBar(Component.text("FIELD  |  Move to the ball  |  Right-click glove to catch"));
            }
        }
    }

    private void updateBallFlight() {
        if (terminalResolutionTick >= 0L) {
            checkCatchWindows();
            if (phase == Phase.BALL_FLIGHT && serverTick >= terminalResolutionTick) {
                resolveAttempt(terminalDistance, false, terminalHomeRun, terminalMessage);
            }
            return;
        }
        if (ballPosition == null || ballVelocity == null) {
            resolveAttempt(0.0D, false, false, "Ball simulation was interrupted.");
            return;
        }
        Vector previous = ballPosition.clone();
        ballVelocity.setY(ballVelocity.getY() - PlexballRules.BALL_GRAVITY_PER_TICK);
        if (curveAcceleration != 0.0D && curveDirection != null) {
            ballVelocity.add(curveDirection.clone().multiply(curveAcceleration));
        }
        ballPosition.add(ballVelocity);
        ballFlightTicks++;
        ballHistory.put(serverTick, ballPosition.clone());
        while (ballHistory.size() > SNAPSHOT_RETENTION + 2) ballHistory.pollFirstEntry();
        if (ballDisplay != null && ballDisplay.isValid()) {
            ballDisplay.teleport(new Location(arena, ballPosition.getX(), ballPosition.getY(), ballPosition.getZ()));
        }

        checkCatchWindows();
        if (phase != Phase.BALL_FLIGHT) return;

        double distance = PlexballRules.horizontalDistance(teeBallPosition, ballPosition);
        boolean inFairTriangle = PlexballRules.isInsideField(ballPosition.getX(), ballPosition.getZ());
        boolean fairFenceCrossing = ballPosition.getZ() <= -PlexballRules.HOME_RUN_METRES
                && Math.abs(ballPosition.getX()) <= PlexballRules.fieldHalfWidthAtZ(-PlexballRules.HOME_RUN_METRES);
        if (distance >= PlexballRules.HOME_RUN_METRES && (inFairTriangle || fairFenceCrossing)) {
            scheduleTerminalResolution(PlexballRules.HOME_RUN_METRES, true, null);
            return;
        }
        if (!inFairTriangle) {
            scheduleTerminalResolution(0.0D, false, "Foul ball — outside the fair triangle.");
            return;
        }
        if (ballPosition.getY() <= FLOOR_Y + 0.28D || ballFlightTicks >= BALL_MAX_FLIGHT_TICKS) {
            scheduleTerminalResolution(distance, false, null);
            return;
        }
        if (serverTick % 4L == 0L) {
            for (UUID id : matchPlayers) {
                if (id.equals(batterId)) continue;
                Player fielder = Bukkit.getPlayer(id);
                if (fielder != null && fielder.isOnline()) {
                    fielder.sendActionBar(Component.text("BALL  |  "
                            + String.format(java.util.Locale.ROOT, "%.1fm", distance)
                            + "  |  Catch window closes when the ball lands"));
                }
            }
        }
    }

    private void scheduleTerminalResolution(double distance, boolean homeRun, String message) {
        terminalDistance = distance;
        terminalHomeRun = homeRun;
        terminalMessage = message;
        // Wait only long enough for the maximum permitted network rewind.
        // A fielder's historical click can still claim a valid pre-terminal catch.
        terminalResolutionTick = serverTick + PlexballRules.MAX_REWIND_TICKS + 1L;
        ballVelocity = new Vector();
    }

    private void checkCatchWindows() {
        if (catchAttempts.isEmpty()) return;
        for (Map.Entry<UUID, CatchAttempt> entry : new ArrayList<>(catchAttempts.entrySet())) {
            UUID catcherId = entry.getKey();
            CatchAttempt attempt = entry.getValue();
            while (attempt.nextCheckTick <= serverTick && attempt.nextCheckTick <= attempt.endTick) {
                long checkTick = attempt.nextCheckTick++;
                if (checkCatchAt(catcherId, checkTick, attempt.radius)) {
                    Player catcher = Bukkit.getPlayer(catcherId);
                    PlayerResult result = results.get(catcherId);
                    if (result != null) result.catches++;
                    String name = catcher == null ? "A fielder" : catcher.getName();
                    resolveAttempt(0.0D, true, false, ChatColor.GREEN + name + " made the catch!");
                    return;
                }
            }
            if (attempt.nextCheckTick > attempt.endTick) catchAttempts.remove(catcherId);
        }
    }

    private boolean checkCatchAt(UUID catcherId, long tick, double radius) {
        Player catcher = Bukkit.getPlayer(catcherId);
        if (catcher == null || !catcher.isOnline() || catcherId.equals(batterId)) return false;
        PlayerFrame frame = frameAt(catcherId, tick);
        Vector ballNow = ballHistory.get(tick);
        Vector ballBefore = ballHistory.get(tick - 1L);
        if (frame == null || ballNow == null) return false;
        if (ballBefore == null) ballBefore = ballNow;
        double yaw = Math.toRadians(frame.yaw);
        Vector facing = new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw)).normalize();
        Vector glovePoint = frame.feet().add(new Vector(0.0D, FIELD_PLAYER_EYE_OFFSET, 0.0D))
                .add(facing.multiply(0.35D));
        return PlexballRules.distanceSquaredToSegment(glovePoint, ballBefore, ballNow) <= radius * radius;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeeBallInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || teeInteraction == null) return;
        if (!event.getRightClicked().getUniqueId().equals(teeInteraction.getUniqueId())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (phase != Phase.BATTING || batterId == null || !batterId.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You're not the batter.");
            return;
        }
        swing(player);
    }

    private void swing(Player player) {
        if (phase != Phase.BATTING || !player.getUniqueId().equals(batterId)) return;
        int pingEstimate = Math.max(0, player.getPing());
        int rewind = PlexballRules.latencyRewindTicks(pingEstimate);
        long oneWayNanos = Math.min(INPUT_GRACE_NANOS, pingEstimate * 500_000L);
        long estimatedInputNanos = System.nanoTime() - oneWayNanos;
        if (estimatedInputNanos > battingDeadlineNanos) {
            player.sendActionBar(Component.text("TIME — your swing arrived outside the batting window."));
            return;
        }
        long effectiveTick = serverTick - rewind;
        PlayerFrame aimFrame = frameAt(player.getUniqueId(), effectiveTick);
        if (aimFrame == null) {
            player.sendMessage(ChatColor.RED + "Swing rejected: aim history is not ready.");
            return;
        }
        if (!PlexballRules.isFairYaw(aimFrame.yaw)) {
            resolveAttempt(0.0D, false, false, "Foul swing — the one batting attempt is used.");
            return;
        }

        int timingDistance = PlexballRules.timingDistanceTicks(effectiveTick, battingStartTick,
                timingCycleTicks, timingPhaseOffset);
        double timing = PlexballRules.timingMultiplier(timingDistance);
        Vector eyeDirection = aimFrame.eyeLocation(arena, player.getEyeHeight()).getDirection().normalize();
        Vector eyeOrigin = aimFrame.eyeLocation(arena, player.getEyeHeight()).toVector();
        Vector normal = contactNormal(eyeOrigin, eyeDirection, teeBallPosition, BALL_RADIUS);
        if (normal == null) {
            player.sendActionBar(Component.text("SWING AND A MISS!"));
            resolveAttempt(0.0D, false, false, "Swing and a miss — the one batting attempt is used.");
            return;
        }
        double contactY = PlexballRules.clamp(normal.getY(), -1.0D, 1.0D);
        double yawRadians = Math.toRadians(aimFrame.yaw);
        Vector forward = new Vector(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians)).normalize();
        Vector right = new Vector(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians)).normalize();
        double contactSide = PlexballRules.clamp(normal.dot(right), -1.0D, 1.0D);
        Kit kit = selectedKits.getOrDefault(player.getUniqueId(), Kit.TECHNICIAN);

        int gradedTimingDistance = kit == Kit.TECHNICIAN ? Math.max(0, timingDistance - 2) : timingDistance;
        timing = PlexballRules.timingMultiplier(gradedTimingDistance);
        // Server-tick physics are tuned as a pair: lower contact is a loft,
        // upper contact is a faster line drive, and good timing preserves power.
        double horizontalSpeed = (3.0D + 1.1D * contactY)
                * (0.65D + 0.35D * timing) * kit.battingPower;
        double verticalSpeed = PlexballRules.clamp(0.47D - 0.25D * contactY, 0.20D, 0.75D);
        if (kit == Kit.SPRINTER) {
            horizontalSpeed *= 1.08D;
            verticalSpeed = Math.min(verticalSpeed, 0.30D);
        } else if (kit == Kit.SKYGUARD) {
            horizontalSpeed *= 0.94D;
            verticalSpeed = Math.min(0.78D, verticalSpeed + 0.08D);
        }
        double sideSpeed = contactSide * (kit == Kit.CURVE_ARTIST ? 0.25D : 0.10D);
        ballVelocity = forward.multiply(horizontalSpeed).add(right.multiply(sideSpeed));
        ballVelocity.setY(verticalSpeed);
        curveDirection = right.clone().multiply(contactSide);
        curveAcceleration = kit == Kit.CURVE_ARTIST ? 0.0035D : 0.0D;
        ballPosition = teeBallPosition.clone();
        ballStartTick = serverTick;
        ballFlightTicks = 0;
        ballHistory.clear();
        ballHistory.put(serverTick, ballPosition.clone());
        catchAttempts.clear();
        abilityUsedThisBall.clear();
        sureHandsArmed.clear();
        cleanupTeeInteraction();
        phase = Phase.BALL_FLIGHT;
        if (ballDisplay != null && ballDisplay.isValid()) ballDisplay.teleport(
                new Location(arena, ballPosition.getX(), ballPosition.getY(), ballPosition.getZ()));
        broadcastMatch(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " hit the ball!");
        player.sendActionBar(Component.text("HIT!  " + (timing >= 0.94D ? "GREAT CONTACT" : "CONTACT")));
    }

    private Vector contactNormal(Vector origin, Vector direction, Vector centre, double radius) {
        // The Interaction entity is only an event target. The server-side aim
        // ray must also intersect the actual ball sphere to produce a valid hit.
        Vector offset = origin.clone().subtract(centre);
        double b = offset.dot(direction);
        double c = offset.lengthSquared() - radius * radius;
        double discriminant = b * b - c;
        if (discriminant < 0.0D) return null;
        double t = -b - Math.sqrt(discriminant);
        if (t < 0.0D) return null;
        return origin.clone().add(direction.clone().multiply(t)).subtract(centre).normalize();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFieldItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (phase != Phase.BALL_FLIGHT || batterId == null || batterId.equals(player.getUniqueId())) return;
        if (!matchPlayers.contains(player.getUniqueId())) return;
        if ("glove".equals(action)) startCatch(player);
        else if ("ability".equals(action)) useAbility(player);
    }

    private void startCatch(Player player) {
        UUID id = player.getUniqueId();
        if (!catchUsedThisBall.add(id)) {
            player.sendActionBar(Component.text("You have already used your catch attempt on this ball."));
            return;
        }
        int rewind = PlexballRules.latencyRewindTicks(Math.max(0, player.getPing()));
        long startTick = Math.max(ballStartTick, serverTick - rewind);
        boolean sureHands = sureHandsArmed.remove(id);
        int window = PlexballRules.BASE_CATCH_WINDOW_TICKS + (sureHands ? 4 : 0);
        double radius = CATCH_REACH + (sureHands ? 0.25D : 0.0D);
        CatchAttempt attempt = new CatchAttempt(startTick, window, radius);
        catchAttempts.put(id, attempt);
        player.sendActionBar(Component.text(sureHands ? "SURE HANDS — catch window extended" : "CATCH!"));
        checkCatchWindows();
    }

    private void useAbility(Player player) {
        UUID id = player.getUniqueId();
        if (!abilityUsedThisBall.add(id)) {
            player.sendActionBar(Component.text("Your kit ability is already used for this ball."));
            return;
        }
        Kit kit = selectedKits.getOrDefault(id, Kit.TECHNICIAN);
        Vector facing = player.getLocation().getDirection().setY(0.0D);
        if (facing.lengthSquared() < 1.0E-5D) facing = new Vector(0.0D, 0.0D, 1.0D);
        facing.normalize();
        switch (kit) {
            case SLUGGER -> {
                player.setVelocity(facing.multiply(0.62D).setY(0.12D));
                player.sendActionBar(Component.text("POWER DIVE!"));
            }
            case TECHNICIAN -> {
                sureHandsArmed.add(id);
                player.sendActionBar(Component.text("SURE HANDS armed — use your glove to catch."));
            }
            case CURVE_ARTIST -> {
                showProjectedFlight(player);
                player.sendActionBar(Component.text("READ FLIGHT — projected path revealed."));
            }
            case SPRINTER -> {
                player.setVelocity(player.getVelocity().add(facing.multiply(0.78D)));
                player.sendActionBar(Component.text("BURST!"));
            }
            case SKYGUARD -> {
                player.setVelocity(player.getVelocity().add(new Vector(0.0D, 0.48D, 0.0D)));
                player.sendActionBar(Component.text("LEAP!"));
            }
        }
    }

    private void showProjectedFlight(Player player) {
        if (ballPosition == null || ballVelocity == null || arena == null) return;
        Vector position = ballPosition.clone();
        Vector velocity = ballVelocity.clone();
        for (int step = 0; step < 24; step += 2) {
            for (int i = 0; i < 2; i++) {
                velocity.setY(velocity.getY() - PlexballRules.BALL_GRAVITY_PER_TICK);
                if (curveAcceleration != 0.0D && curveDirection != null) {
                    velocity.add(curveDirection.clone().multiply(curveAcceleration));
                }
                position.add(velocity);
            }
            if (!PlexballRules.isInsideField(position.getX(), position.getZ())
                    || position.getY() <= FLOOR_Y) break;
            player.spawnParticle(Particle.END_ROD, position.getX(), position.getY(), position.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onParticipantMove(PlayerMoveEvent event) {
        if (!isActive() || !matchPlayers.contains(event.getPlayer().getUniqueId())) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != arena || to.getWorld() != arena) return;
        if (event.getPlayer().getUniqueId().equals(batterId)) {
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(new Location(arena, 0.5D, FLOOR_Y + 1.0D, 4.5D, to.getYaw(), to.getPitch()));
            }
            return;
        }
        if (!PlexballRules.isInsideField(to.getX(), to.getZ()) || to.getY() < FLOOR_Y || to.getY() > FLOOR_Y + 12.0D) {
            event.setTo(from);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onParticipantDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && matchPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && matchPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (matchPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (matchPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeldSlotChange(PlayerItemHeldEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!matchPlayers.contains(id)) return;
        // Slot 0 is the glove and slot 1 is the kit ability. Let fielders switch
        // between those two actions, while preventing access to empty/admin items.
        if (id.equals(batterId) ? event.getNewSlot() != 0 : event.getNewSlot() > 1) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        queue.remove(id);
        catchAttempts.remove(id);
        if (phase == Phase.IDLE || !matchPlayers.contains(id)) return;
        if (id.equals(batterId)) resolveAttempt(0.0D, false, false, "Batter disconnected.");
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setWalkSpeed(0.2F);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setSprinting(false);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector());
    }

    private void giveBat(Player player) {
        player.getInventory().clear();
        ItemStack bat = namedItem(Material.WOODEN_SWORD, ChatColor.GOLD + "Baseball Bat",
                "Right-click the ball with your crosshair on its surface.", null);
        player.getInventory().setItem(0, bat);
    }

    private void giveFieldingItems(Player player) {
        player.getInventory().clear();
        Kit kit = selectedKits.getOrDefault(player.getUniqueId(), Kit.TECHNICIAN);
        player.getInventory().setItem(0, namedItem(Material.LEATHER, ChatColor.GREEN + "Catcher's Glove",
                "Right-click to attempt one timed catch.", "glove"));
        player.getInventory().setItem(1, namedItem(kit.abilityItem, ChatColor.AQUA + kit.abilityName,
                "One use per ball. " + kit.displayName() + " ability.", "ability"));
    }

    private ItemStack namedItem(Material material, String name, String lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void teleportToBatter(Player player) {
        player.teleport(new Location(arena, 0.5D, FLOOR_Y + 1.0D, 4.5D, 180.0F, 7.0F));
    }

    private void teleportToField(Player player, int playerIndex) {
        int slot = Math.floorMod(playerIndex, 8);
        double[] lane = {-0.72D, 0.0D, 0.72D, -0.72D, 0.0D, 0.72D, -0.45D, 0.45D};
        double[] depth = {-20.0D, -20.0D, -20.0D, -46.0D, -46.0D, -46.0D, -76.0D, -76.0D};
        double z = depth[slot];
        double x = lane[slot] * PlexballRules.fieldHalfWidthAtZ(z);
        player.teleport(new Location(arena, x + 0.5D, FLOOR_Y + 1.0D, z + 0.5D, 0.0F, 0.0F));
    }

    private void createTeeBall() {
        cleanupBall();
        // Small per-at-bat tee offset stops a fixed-coordinate click macro from
        // getting reliable contact without tracking the visible ball.
        teeBallPosition = new Vector(0.5D + (random.nextDouble() - 0.5D) * 0.36D,
                FLOOR_Y + 2.1D, (random.nextDouble() - 0.5D) * 0.36D);
        Location ballLocation = new Location(arena, teeBallPosition.getX(), teeBallPosition.getY(), teeBallPosition.getZ());
        ballDisplay = arena.spawn(ballLocation, ItemDisplay.class);
        ballDisplay.setItemStack(new ItemStack(Material.SNOWBALL));
        ballDisplay.setGravity(false);
        ballDisplay.setInvulnerable(true);
        ballDisplay.setPersistent(false);
        teeInteraction = arena.spawn(ballLocation, Interaction.class);
        teeInteraction.setInteractionWidth(0.72F);
        teeInteraction.setInteractionHeight(0.72F);
        teeInteraction.setResponsive(true);
        teeInteraction.setInvulnerable(true);
        teeInteraction.setPersistent(false);
    }

    private void cleanupTeeInteraction() {
        if (teeInteraction != null) {
            teeInteraction.remove();
            teeInteraction = null;
        }
    }

    private void cleanupBall() {
        cleanupTeeInteraction();
        if (ballDisplay != null) {
            ballDisplay.remove();
            ballDisplay = null;
        }
        ballPosition = null;
        ballVelocity = null;
        curveDirection = null;
        curveAcceleration = 0.0D;
        terminalResolutionTick = -1L;
        terminalDistance = 0.0D;
        terminalHomeRun = false;
        terminalMessage = null;
        ballHistory.clear();
        catchAttempts.clear();
    }

    private World ensureArena() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generator(new PlexballChunkGenerator());
            creator.generateStructures(false);
            creator.environment(World.Environment.NORMAL);
            world = creator.createWorld();
        }
        if (world == null) throw new IllegalStateException("Could not create the isolated Plexball arena world.");

        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.getWorldBorder().setCenter(0.0D, -50.0D);
        world.getWorldBorder().setSize(260.0D);
        world.getWorldBorder().setDamageAmount(0.0D);

        for (int chunkX = -5; chunkX <= 4; chunkX++) {
            for (int chunkZ = -7; chunkZ <= 0; chunkZ++) world.getChunkAt(chunkX, chunkZ);
        }
        buildFieldSurface(world);
        return world;
    }

    private void buildFieldSurface(World world) {
        File marker = new File(world.getWorldFolder(), "TESTPLEX_PLEXBALL_FIELD_V1");
        if (marker.isFile() && hasExpectedFieldSamples(world)) return;
        for (int z = ARENA_MIN_Z; z <= 0; z++) {
            int halfWidth = (int) Math.ceil(PlexballRules.fieldHalfWidthAtZ(z));
            for (int x = -halfWidth; x <= halfWidth; x++) {
                world.getBlockAt(x, FLOOR_Y - 2, z).setType(Material.BEDROCK, false);
                world.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.STONE, false);
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.GRASS_BLOCK, false);
            }
            int left = -(int) Math.round(PlexballRules.fieldHalfWidthAtZ(z));
            int right = -left;
            world.getBlockAt(left, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
            world.getBlockAt(right, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
        }
        for (int x = -65; x <= 65; x++) {
            world.getBlockAt(x, FLOOR_Y, -100).setType(Material.GOLD_BLOCK, false);
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = 1; z <= 3; z++) world.getBlockAt(x, FLOOR_Y, z).setType(Material.WHITE_CONCRETE, false);
        }
        world.getBlockAt(0, FLOOR_Y + 1, 0).setType(Material.OAK_FENCE, false);
        world.getBlockAt(0, FLOOR_Y + 2, 0).setType(Material.OAK_FENCE, false);
        if (!hasExpectedFieldSamples(world)) throw new IllegalStateException("Plexball triangular field generation did not validate.");
        try {
            Files.writeString(marker.toPath(), "TESTPLEX_PLEXBALL_FIELD_V1" + System.lineSeparator()
                    + "Triangle field X/Z -100..0; grass surface Y=64; home-run fence 100m.", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not write Plexball field marker: " + exception.getMessage());
        }
    }

    private boolean hasExpectedFieldSamples(World world) {
        int[][] samples = {{0, -1}, {0, -50}, {0, -99}, {-10, -50}, {10, -50}};
        for (int[] sample : samples) {
            if (world.getBlockAt(sample[0], FLOOR_Y, sample[1]).getType() != Material.GRASS_BLOCK
                    || world.getBlockAt(sample[0], FLOOR_Y - 1, sample[1]).getType() != Material.STONE
                    || world.getBlockAt(sample[0], FLOOR_Y - 2, sample[1]).getType() != Material.BEDROCK) return false;
        }
        return true;
    }

    private void updateBattingHudIfNeeded() {
        if (serverTick % 2L == 0L) updateBattingHud();
    }

    private void broadcastQueue(String message) {
        for (Player player : queue.values()) if (player.isOnline()) player.sendMessage(message);
    }

    private void broadcastMatch(String message) {
        for (UUID id : matchPlayers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) player.sendMessage(message);
        }
    }

    private void loadKits() {
        if (!kitFile.isFile()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(kitFile);
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                Kit kit = Kit.fromId(config.getString(key, "technician"));
                if (kit != null) selectedKits.put(id, kit);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring malformed Plexball kit entry: " + key);
            }
        }
    }

    private void saveKits() {
        YamlConfiguration config = new YamlConfiguration();
        selectedKits.forEach((id, kit) -> config.set(id.toString(), kit.id()));
        try {
            File parent = kitFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Could not create Plexball data directory.");
            }
            config.save(kitFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save Plexball kits: " + exception.getMessage());
        }
    }

    private static final class PlexballChunkGenerator extends ChunkGenerator {
        @Override
        public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
            ChunkData data = createChunkData(world);
            for (int localX = 0; localX < 16; localX++) {
                int x = chunkX * 16 + localX;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int z = chunkZ * 16 + localZ;
                    if (!PlexballRules.isInsideField(x, z)) continue;
                    data.setBlock(localX, FLOOR_Y - 2, localZ, Material.BEDROCK);
                    data.setBlock(localX, FLOOR_Y - 1, localZ, Material.STONE);
                    data.setBlock(localX, FLOOR_Y, localZ, Material.GRASS_BLOCK);
                }
            }
            return data;
        }

        @Override
        public List<BlockPopulator> getDefaultPopulators(World world) {
            return Collections.emptyList();
        }

        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5D, FLOOR_Y + 1.0D, 4.5D);
        }

        @Override
        public boolean canSpawn(World world, int x, int z) {
            return true;
        }
    }
}
