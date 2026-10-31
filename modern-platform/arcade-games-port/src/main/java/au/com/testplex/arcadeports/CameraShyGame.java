package au.com.testplex.arcadeports;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative Camera Shy implementation for the shared Paper 26.3 Arcade host.
 * Players use a vanilla spyglass to aim and swap-hands (F) to take a photograph.
 */
public final class CameraShyGame implements Listener {
    public static final String WORLD_NAME = "testplex_camera_shy_arena_v1";
    private static final int MAZE_CELLS = 15;
    private static final int CELL_SIZE = 7;
    private static final int HALF_CELLS = MAZE_CELLS / 2;
    private static final int FLOOR_Y = 64;
    private static final int QUEUE_WINDOW_TICKS = 100;
    private static final long SPOTLIGHT_INTERVAL_MILLIS = 55_000L;
    private static final long GROUP_PICKUP_INTERVAL_MILLIS = 42_000L;
    private static final long GROUP_PICKUP_LIFETIME_MILLIS = 22_000L;
    private static final long DECOY_FLASH_LIFETIME_MILLIS = 6_000L;
    private static final long SMOKE_LIFETIME_MILLIS = 4_500L;
    private static final long PURSUIT_ESCAPE_MILLIS = 2_000L;
    private static final double PURSUIT_ESCAPE_DISTANCE = 8.0D;
    private static final double SPOTTER_RANGE = 18.0D;
    private static final double GROUP_PHOTO_MIN_DOT = 0.985D;
    private static final long ABILITY_SMOKE_COOLDOWN = 25_000L;
    private static final long ABILITY_DECOY_COOLDOWN = 25_000L;
    private static final long ABILITY_RUNNER_COOLDOWN = 20_000L;
    private static final long ABILITY_SPOTTER_COOLDOWN = 25_000L;
    private static final long ABILITY_UNDERSTUDY_COOLDOWN = 30_000L;

    private enum Phase { IDLE, STARTING, LIVE, ENDING }
    public enum Kit {
        SMOKE_ARTIST("smoke-artist", "Smoke Artist", Material.POTION, "Smoke Bomb"),
        DECOY("decoy", "Decoy", Material.FIREWORK_ROCKET, "Fake Snapshot"),
        RUNNER("runner", "Runner", Material.SUGAR, "Adrenaline Rush"),
        SPOTTER("spotter", "Spotter", Material.COMPASS, "Flash Finder"),
        UNDERSTUDY("understudy", "Understudy", Material.ARMOR_STAND, "Body Double");

        private final String id;
        private final String display;
        private final Material icon;
        private final String ability;
        Kit(String id, String display, Material icon, String ability) {
            this.id = id; this.display = display; this.icon = icon; this.ability = ability;
        }
        public String id() { return id; }
        public String display() { return display; }
        public Material icon() { return icon; }
        public String ability() { return ability; }
        static Kit fromId(String requested) {
            if (requested == null) return null;
            String id = requested.toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
            for (Kit kit : values()) if (kit.id.equals(id)) return kit;
            return null;
        }
    }

    private record FocusKey(UUID shooter, UUID target) { }
    private static final class FocusAttempt {
        final long startedTick;
        final Location targetOrigin;
        FocusAttempt(long startedTick, Location targetOrigin) {
            this.startedTick = startedTick; this.targetOrigin = targetOrigin.clone();
        }
    }
    private static final class SmokeCloud {
        final UUID owner;
        final Location center;
        final long expiresAt;
        SmokeCloud(UUID owner, Location center, long expiresAt) {
            this.owner = owner;
            this.center = center.clone();
            this.expiresAt = expiresAt;
        }
    }
    private static final class FakeFlash {
        final UUID owner;
        final Location location;
        final long expiresAt;
        final Set<UUID> fooled = new HashSet<>();
        FakeFlash(UUID owner, Location location, long expiresAt) {
            this.owner = owner; this.location = location.clone(); this.expiresAt = expiresAt;
        }
    }
    private static final class Pursuit {
        final UUID pursuer;
        long lostSightSince = -1L;
        boolean lostSightDueToSmoke;
        Pursuit(UUID pursuer) { this.pursuer = pursuer; }
    }
    private static final class PlayerSnapshot {
        final Location location;
        final ItemStack[] contents;
        final ItemStack[] armor;
        final ItemStack offhand;
        final GameMode mode;
        final List<PotionEffect> effects;
        final float exp;
        final int level;
        final int food;
        final float saturation;
        final double health;
        final float walkSpeed;
        final float flySpeed;
        final boolean allowFlight;
        final boolean flying;
        final boolean invulnerable;
        final boolean collidable;
        final boolean glowing;
        PlayerSnapshot(Player player) {
            location = player.getLocation().clone();
            contents = cloneItems(player.getInventory().getContents());
            armor = cloneItems(player.getInventory().getArmorContents());
            ItemStack off = player.getInventory().getItemInOffHand();
            offhand = off == null ? null : off.clone();
            mode = player.getGameMode();
            effects = new ArrayList<>(player.getActivePotionEffects());
            exp = player.getExp(); level = player.getLevel();
            food = player.getFoodLevel(); saturation = player.getSaturation();
            health = player.getHealth(); walkSpeed = player.getWalkSpeed(); flySpeed = player.getFlySpeed();
            allowFlight = player.getAllowFlight(); flying = player.isFlying();
            invulnerable = player.isInvulnerable(); collidable = player.isCollidable();
            glowing = player.isGlowing();
        }
        PlayerSnapshot(ConfigurationSection section) {
            Object savedLocation = section.get("location");
            location = savedLocation instanceof Location saved ? saved.clone() : null;
            contents = readItems(section.getList("contents"));
            armor = readItems(section.getList("armor"));
            Object savedOffhand = section.get("offhand");
            offhand = savedOffhand instanceof ItemStack item ? item.clone() : null;
            GameMode savedMode;
            try {
                savedMode = GameMode.valueOf(section.getString("mode", GameMode.SURVIVAL.name()));
            } catch (IllegalArgumentException invalidMode) {
                savedMode = GameMode.SURVIVAL;
            }
            mode = savedMode;
            List<PotionEffect> restoredEffects = new ArrayList<>();
            List<?> savedEffects = section.getList("effects");
            if (savedEffects != null) {
                for (Object value : savedEffects) {
                    if (value instanceof PotionEffect effect) restoredEffects.add(effect);
                }
            }
            effects = restoredEffects;
            exp = (float) section.getDouble("exp");
            level = section.getInt("level");
            food = section.getInt("food", 20);
            saturation = (float) section.getDouble("saturation", 20.0D);
            health = section.getDouble("health", 20.0D);
            walkSpeed = (float) section.getDouble("walk-speed", 0.2D);
            flySpeed = (float) section.getDouble("fly-speed", 0.1D);
            allowFlight = section.getBoolean("allow-flight");
            flying = section.getBoolean("flying");
            invulnerable = section.getBoolean("invulnerable");
            collidable = section.getBoolean("collidable", true);
            glowing = section.getBoolean("glowing", false);
        }
        private static ItemStack[] readItems(List<?> values) {
            if (values == null) return new ItemStack[0];
            ItemStack[] result = new ItemStack[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value instanceof ItemStack item) result[i] = item.clone();
                else if (value instanceof Map<?, ?> serialized) {
                    Map<String, Object> itemData = new HashMap<>();
                    for (Map.Entry<?, ?> entry : serialized.entrySet()) {
                        if (entry.getKey() instanceof String key) itemData.put(key, entry.getValue());
                    }
                    try {
                        result[i] = ItemStack.deserialize(itemData);
                    } catch (RuntimeException ignored) {
                        result[i] = null;
                    }
                }
            }
            return result;
        }
        void writeToConfig(JavaPlugin plugin, UUID playerId) {
            String path = "camera-shy-pending-restores." + playerId;
            plugin.getConfig().set(path, null);
            ConfigurationSection section = plugin.getConfig().createSection(path);
            section.set("location", location);
            section.set("contents", Arrays.asList(cloneItems(contents)));
            section.set("armor", Arrays.asList(cloneItems(armor)));
            section.set("offhand", offhand == null ? null : offhand.clone());
            section.set("mode", mode.name());
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
        }
        void persist(JavaPlugin plugin, UUID playerId) {
            writeToConfig(plugin, playerId);
            plugin.saveConfig();
        }
        void restore(Player player) {
            if (player == null || !player.isOnline()) return;
            player.leaveVehicle();
            if (location != null && location.getWorld() != null) player.teleport(location);
            player.getInventory().clear();
            player.getInventory().setContents(cloneItems(contents));
            player.getInventory().setArmorContents(cloneItems(armor));
            player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
            for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) player.addPotionEffect(effect);
            player.setGameMode(mode);
            player.setExp(exp); player.setLevel(level);
            player.setFoodLevel(food); player.setSaturation(saturation);
            player.setWalkSpeed(walkSpeed); player.setFlySpeed(flySpeed);
            player.setInvulnerable(invulnerable); player.setCollidable(collidable);
            player.setGlowing(glowing);
            player.setAllowFlight(allowFlight);
            if (allowFlight) player.setFlying(flying);
            player.setHealth(Math.max(1.0D, Math.min(health, player.getMaxHealth())));
            player.setFireTicks(0);
        }
        private static ItemStack[] cloneItems(ItemStack[] source) {
            ItemStack[] result = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) result[i] = source[i] == null ? null : source[i].clone();
            return result;
        }
    }

    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final ArcadeTestPlayerManager testPlayers;
    private final NamespacedKey abilityKey;
    private final NamespacedKey decoyStandKey;
    private final NamespacedKey decoyOwnerKey;
    private final NamespacedKey groupPickupKey;
    private final LinkedHashMap<UUID, Player> queue = new LinkedHashMap<>();
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    private final Set<UUID> humans = new LinkedHashSet<>();
    private final Set<UUID> bots = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, Integer> exposures = new HashMap<>();
    private final Map<UUID, Integer> photos = new HashMap<>();
    private final Map<UUID, Integer> eliminations = new HashMap<>();
    private final Map<UUID, Integer> revengeEliminations = new HashMap<>();
    private final Map<UUID, Integer> encounterEscapes = new HashMap<>();
    private final Map<UUID, Integer> smokeEscapes = new HashMap<>();
    private final Map<UUID, Set<UUID>> decoyFooledOpponents = new HashMap<>();
    private final Map<UUID, Integer> spotterPhotos = new HashMap<>();
    private final Map<UUID, Set<UUID>> uniquePhotos = new HashMap<>();
    private final Map<UUID, Set<UUID>> photographedBy = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> spotterMarks = new HashMap<>();
    private final Map<UUID, Set<UUID>> spotterPhotoTargets = new HashMap<>();
    private final Map<UUID, Map<UUID, Pursuit>> pursuits = new HashMap<>();
    private final Map<UUID, Long> cameraReadyAt = new HashMap<>();
    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private final Map<UUID, Long> abilityReadyAt = new HashMap<>();
    private final Map<UUID, Long> runnerUntil = new HashMap<>();
    private final Map<UUID, Long> runnerUsedUntil = new HashMap<>();
    private final Map<UUID, Long> smokeUsedUntil = new HashMap<>();
    private final Map<UUID, Long> fourExposureSince = new HashMap<>();
    private final Map<UUID, Long> lastPhotoAt = new HashMap<>();
    private final Map<UUID, UUID> lastPhotoTarget = new HashMap<>();
    private final Map<UUID, Long> lastEliminationAt = new HashMap<>();
    private final Map<UUID, Integer> eliminationStreaks = new HashMap<>();
    private final Map<UUID, Integer> revengeMatchCounts = new HashMap<>();
    private final Map<UUID, String> selectedKits = new HashMap<>();
    private final Map<UUID, Long> aimingSince = new HashMap<>();
    private final Map<FocusKey, FocusAttempt> focusAttempts = new HashMap<>();
    private final Set<FocusKey> focusSeenThisTick = new HashSet<>();
    private final List<SmokeCloud> smokeClouds = new ArrayList<>();
    private final List<FakeFlash> fakeFlashes = new ArrayList<>();
    private final Map<UUID, ArmorStand> understudyStands = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Long> spotlightGlowUntil = new HashMap<>();
    private final Map<UUID, Boolean> spotlightOriginalGlow = new HashMap<>();
    private final Map<UUID, Location> spawnLocations = new HashMap<>();
    private final Set<UUID> groupPhotoCharges = new HashSet<>();
    private final Random random = new Random();

    private Phase phase = Phase.IDLE;
    private World arena;
    private BukkitTask ticker;
    private BukkitTask quickStartTask;
    private BukkitTask pendingStartTask;
    private long serverTick;
    private long startedAt;
    private long endsAt;
    private long nextSpotlightAt;
    private long nextGroupPickupAt;
    private long decoySerial;
    private boolean finalSnapshot;
    private Item currentGroupPickup;
    private List<Player> pendingHumans = List.of();
    private int startWaitTicks;

    public CameraShyGame(JavaPlugin plugin, ArcadePortManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.testPlayers = manager.testPlayerManager();
        this.abilityKey = new NamespacedKey(plugin, "camera-shy-ability");
        this.decoyStandKey = new NamespacedKey(plugin, "camera-shy-understudy");
        this.decoyOwnerKey = new NamespacedKey(plugin, "camera-shy-decoy-owner");
        this.groupPickupKey = new NamespacedKey(plugin, "camera-shy-group-photo");
        this.ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Restore a saved pre-match snapshot after a player reconnects. */
    public boolean restorePendingSnapshot(Player player) {
        if (player == null || !player.isOnline()) return false;
        String path = "camera-shy-pending-restores." + player.getUniqueId();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return false;
        PlayerSnapshot snapshot = new PlayerSnapshot(section);
        // Establish the shared lobby as a safe fallback before restoring the exact saved state.
        manager.returnPlayerToLobby(player);
        snapshot.restore(player);
        plugin.getConfig().set(path, null);
        plugin.saveConfig();
        player.sendMessage(ChatColor.YELLOW + "Your Camera Shy session ended while you were offline. Your pre-match state has been restored.");
        return true;
    }

    public boolean isActive() { return phase == Phase.STARTING || phase == Phase.LIVE || phase == Phase.ENDING; }

    public String status() {
        if (phase == Phase.IDLE) return "idle";
        if (phase == Phase.STARTING) return "starting; queued=" + pendingHumans.size() + " humans";
        long remaining = Math.max(0L, (endsAt - System.currentTimeMillis()) / 1000L);
        return "live; alive=" + alive.size() + "/" + players.size() + "; time=" + remaining + "s";
    }

    public int queuedCount() {
        return (int) queue.values().stream().filter(player -> player != null && player.isOnline()).count();
    }

    void removeFromQueue(UUID playerId) {
        if (playerId == null) return;
        queue.remove(playerId);
        if (queue.isEmpty() && quickStartTask != null) {
            quickStartTask.cancel();
            quickStartTask = null;
        }
    }

    public void queue(Player player, boolean quickStart) {
        if (player == null || !player.isOnline()) return;
        if (isActive()) {
            player.sendMessage(ChatColor.RED + "Camera Shy is already underway. You can join the next round.");
            return;
        }
        if (queue.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already queued for Camera Shy.");
            return;
        }
        if (queue.size() >= CameraShyRules.MAX_PLAYERS) {
            player.sendMessage(ChatColor.RED + "Camera Shy is full (16/16).");
            return;
        }
        for (Player queued : queue.values()) {
            if (queued.getUniqueId().equals(player.getUniqueId())) return;
        }
        manager.returnPlayerToLobby(player);
        queue.put(player.getUniqueId(), player);
        String kit = plugin.getConfig().getString("camera-shy-kit-preferences." + player.getUniqueId(), "smoke-artist");
        selectedKits.put(player.getUniqueId(), Kit.fromId(kit) == null ? "smoke-artist" : kit);
        player.sendMessage(ChatColor.GREEN + "Queued for Camera Shy (" + queue.size() + "/16).");
        if (quickStart && quickStartTask == null) {
            quickStartTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                quickStartTask = null;
                startQueued(Bukkit.getConsoleSender());
            }, QUEUE_WINDOW_TICKS);
            broadcastQueue(ChatColor.YELLOW + "Camera Shy starts in 5 seconds. More players can still join!");
        }
    }

    public void leaveQueue(Player player) {
        if (player == null) return;
        if (queue.remove(player.getUniqueId()) == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not queued for Camera Shy.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "You left the Camera Shy queue.");
        if (queue.isEmpty() && quickStartTask != null) {
            quickStartTask.cancel();
            quickStartTask = null;
        }
        broadcastQueue(ChatColor.YELLOW + player.getName() + " left the Camera Shy queue (" + queue.size() + ").");
    }

    public void selectKit(Player player, String requestedKit) {
        if (player == null || requestedKit == null) return;
        Kit kit = Kit.fromId(requestedKit);
        if (kit == null) {
            player.sendMessage(ChatColor.YELLOW + "Camera Shy kits: smoke-artist, decoy, runner, spotter, understudy.");
            return;
        }
        if (phase == Phase.LIVE && players.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Camera Shy kits cannot be changed during a live match.");
            return;
        }
        selectedKits.put(player.getUniqueId(), kit.id());
        plugin.getConfig().set("camera-shy-kit-preferences." + player.getUniqueId(), kit.id());
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + "Camera Shy kit selected: " + kit.display() + ".");
    }

    public void startFromLobby(org.bukkit.command.CommandSender sender, Collection<Player> lobbyPlayers) {
        if (lobbyPlayers != null) {
            for (Player player : lobbyPlayers) {
                if (player != null && player.isOnline() && queue.size() < CameraShyRules.MAX_PLAYERS) {
                    if (!queue.containsKey(player.getUniqueId())) {
                        queue.put(player.getUniqueId(), player);
                        String kit = plugin.getConfig().getString("camera-shy-kit-preferences." + player.getUniqueId(), "smoke-artist");
                        selectedKits.put(player.getUniqueId(), Kit.fromId(kit) == null ? "smoke-artist" : kit);
                    }
                }
            }
        }
        startQueued(sender);
    }

    public void startQueued(org.bukkit.command.CommandSender sender) {
        if (isActive()) {
            sender.sendMessage(ChatColor.RED + "Camera Shy is already active.");
            return;
        }
        if (!manager.canStartCameraShy()) {
            sender.sendMessage(ChatColor.RED + "Another Arcade game is already running.");
            return;
        }
        List<Player> humansInQueue = queue.values().stream().filter(p -> p != null && p.isOnline())
                .limit(CameraShyRules.MAX_PLAYERS).toList();
        if (humansInQueue.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No players are queued for Camera Shy. Use /arcadeport join camera-shy.");
            return;
        }
        if (quickStartTask != null) {
            quickStartTask.cancel();
            quickStartTask = null;
        }
        if (pendingStartTask != null) {
            sender.sendMessage(ChatColor.YELLOW + "Camera Shy is already preparing its match.");
            return;
        }
        pendingHumans = List.copyOf(humansInQueue);
        queue.clear();
        startWaitTicks = 0;
        manager.stop("Camera Shy is taking over the shared Arcade test lane.");
        phase = Phase.STARTING;
        sender.sendMessage(ChatColor.GOLD + "Preparing Camera Shy for " + pendingHumans.size() + " human player(s)...");
        scheduleStartAttempt();
    }

    public void stop(String reason) {
        if (phase == Phase.IDLE) {
            queue.clear();
            if (quickStartTask != null) quickStartTask.cancel();
            quickStartTask = null;
            return;
        }
        if (pendingStartTask != null) {
            pendingStartTask.cancel();
            pendingStartTask = null;
        }
        finish(null, reason == null || reason.isBlank() ? "Camera Shy was stopped." : reason, false);
    }

    public void shutdown() {
        if (ticker != null) ticker.cancel();
        if (quickStartTask != null) quickStartTask.cancel();
        if (pendingStartTask != null) pendingStartTask.cancel();
        quickStartTask = null;
        pendingStartTask = null;
        if (phase != Phase.IDLE) finish(null, "Camera Shy is shutting down.", false);
        queue.clear();
    }

    private void scheduleStartAttempt() {
        pendingStartTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingStartTask = null;
            if (phase != Phase.STARTING) return;
            startWaitTicks += 20;
            if (!testPlayers.isStartupCleanupComplete()) {
                if (startWaitTicks >= 300) abortStart("Test-player startup cleanup has not completed.");
                else scheduleStartAttempt();
                return;
            }
            if (!testPlayers.isStartupCleanupSuccessful()) {
                abortStart("Testplex could not safely clean up the previous test-player session.");
                return;
            }
            if (testPlayers.isCleanupInProgress()) {
                if (startWaitTicks >= 300) abortStart("The previous test-player roster is still being cleaned up.");
                else scheduleStartAttempt();
                return;
            }
            ensureArena();
            if (arena == null) {
                abortStart("The Camera Shy maze world could not be loaded.");
                return;
            }
            int missing = CameraShyRules.MAX_PLAYERS - pendingHumans.size();
            if (missing <= 0) {
                startMatch(pendingHumans, List.of());
                return;
            }
            if (!testPlayers.isAvailableForDamageablePlayers()) {
                if (pendingHumans.size() >= CameraShyRules.MIN_PLAYERS) {
                    plugin.getLogger().warning("Camera Shy is starting without AFK test players: FakePlayerPlugin is unavailable.");
                    startMatch(pendingHumans, List.of());
                } else {
                    abortStart("Camera Shy needs at least four players. Install/enable FakePlayerPlugin to fill the test roster.");
                }
                return;
            }
            Location botSpawn = mazeSpawnLocations().get(0);
            Player owner = pendingHumans.get(0);
            testPlayers.spawnForMatch(missing, botSpawn, owner, result -> {
                if (phase != Phase.STARTING) {
                    testPlayers.despawnSessionPlayers();
                    return;
                }
                if (result.succeeded()) {
                    List<Player> botsReady = new ArrayList<>(result.players());
                    startMatch(pendingHumans, botsReady);
                } else if (pendingHumans.size() >= CameraShyRules.MIN_PLAYERS) {
                    plugin.getLogger().warning("Camera Shy test-player fill failed; starting with humans only: " + result.error());
                    startMatch(pendingHumans, List.of());
                } else {
                    testPlayers.despawnSessionPlayers();
                    abortStart("Could not fill the Camera Shy test roster: " + result.error());
                }
            });
        }, 20L);
    }

    private void abortStart(String reason) {
        phase = Phase.IDLE;
        pendingHumans = List.of();
        queue.clear();
        testPlayers.despawnSessionPlayers();
        plugin.getLogger().warning("Camera Shy start aborted: " + reason);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline()) player.sendMessage(ChatColor.RED + "Camera Shy could not start: " + reason);
        }
    }

    private void startMatch(List<Player> humansInMatch, List<Player> testBots) {
        if (phase != Phase.STARTING || humansInMatch.isEmpty()) return;
        List<Player> connectedHumans = humansInMatch.stream()
                .filter(player -> player != null && player.isOnline()).toList();
        if (connectedHumans.isEmpty()) {
            abortStart("All queued human players disconnected before the match began.");
            return;
        }
        humansInMatch = connectedHumans;
        players.clear(); humans.clear(); bots.clear(); alive.clear(); snapshots.clear();
        exposures.clear(); photos.clear(); eliminations.clear(); revengeEliminations.clear();
        encounterEscapes.clear(); smokeEscapes.clear(); decoyFooledOpponents.clear(); spotterPhotos.clear();
        uniquePhotos.clear(); photographedBy.clear(); spotterMarks.clear(); spotterPhotoTargets.clear();
        pursuits.clear(); cameraReadyAt.clear(); protectedUntil.clear(); abilityReadyAt.clear();
        runnerUntil.clear(); runnerUsedUntil.clear(); smokeUsedUntil.clear(); fourExposureSince.clear(); lastPhotoAt.clear();
        lastPhotoTarget.clear(); lastEliminationAt.clear(); eliminationStreaks.clear(); revengeMatchCounts.clear();
        focusAttempts.clear(); focusSeenThisTick.clear(); smokeClouds.clear(); fakeFlashes.clear();
        groupPhotoCharges.clear(); spawnLocations.clear(); aimingSince.clear();
        spotlightGlowUntil.clear(); spotlightOriginalGlow.clear();
        for (ArmorStand stand : understudyStands.values()) if (stand != null && stand.isValid()) stand.remove();
        understudyStands.clear();
        clearGroupPickup();

        List<Player> all = new ArrayList<>(humansInMatch);
        all.addAll(testBots);
        if (all.size() < CameraShyRules.MIN_PLAYERS) {
            abortStart("Camera Shy needs at least four total players.");
            return;
        }
        Collections.shuffle(all, random);
        List<Location> spawns = mazeSpawnLocations();
        startedAt = System.currentTimeMillis();
        endsAt = startedAt + CameraShyRules.MATCH_SECONDS * 1_000L;
        nextSpotlightAt = startedAt + SPOTLIGHT_INTERVAL_MILLIS;
        nextGroupPickupAt = startedAt + GROUP_PICKUP_INTERVAL_MILLIS;
        finalSnapshot = false;
        phase = Phase.LIVE;

        for (int i = 0; i < all.size(); i++) {
            Player player = all.get(i);
            UUID id = player.getUniqueId();
            players.put(id, player);
            if (testPlayers.isTestPlayer(player)) bots.add(id);
            else {
                humans.add(id);
                PlayerSnapshot snapshot = new PlayerSnapshot(player);
                snapshots.put(id, snapshot);
                // Persist before changing any inventory, game mode or location so an
                // unclean server stop can still restore the exact pre-match player.
                snapshot.writeToConfig(plugin, id);
            }
            alive.add(id);
            exposures.put(id, 0);
            photos.put(id, 0);
            eliminations.put(id, 0);
            uniquePhotos.put(id, new LinkedHashSet<>());
            photographedBy.put(id, new LinkedHashSet<>());
            spawnLocations.put(id, spawns.get(i % spawns.size()).clone());
        }
        try {
            plugin.saveConfig();
            for (UUID id : players.keySet()) {
                Player player = players.get(id);
                if (player != null && player.isOnline()) setupPlayer(player, spawnLocations.get(id));
            }
        } catch (RuntimeException startupFailure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Camera Shy could not finish preparing every player; restoring the captured pre-match state.",
                    startupFailure);
            finish(null, "Camera Shy could not initialise. No round was played.", false);
            return;
        }

        broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + "CAMERA SHY"
                + ChatColor.YELLOW + " — photograph your opponents five times to eliminate them!");
        for (UUID id : humans) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendTitle(ChatColor.GOLD + "CAMERA SHY",
                        ChatColor.YELLOW + "Right-click to scope • Press F to take a photo", 5, 60, 10);
                player.sendMessage(ChatColor.GRAY + "Hold your spyglass to aim. Press " + ChatColor.AQUA + "F"
                        + ChatColor.GRAY + " to take the photograph.");
                player.sendMessage(ChatColor.GRAY + "Five photographs eliminate a player. Exposure never decays.");
            }
        }
        plugin.getLogger().info("Camera Shy started with " + humans.size() + " humans and " + bots.size() + " AFK test players.");
    }

    private void setupPlayer(Player player, Location spawn) {
        player.closeInventory();
        player.leaveVehicle();
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.setInvulnerable(true);
        player.setCollidable(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
        giveKit(player);
        BossBar bar = Bukkit.createBossBar(ChatColor.GOLD + "CAMERA SHY", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        bar.addPlayer(player);
        bar.setVisible(true);
        bossBars.put(player.getUniqueId(), bar);
        if (testPlayers.isTestPlayer(player)) testPlayers.syncMatchRespawnLocation(player);
    }

    private void giveKit(Player player) {
        String selected = selectedKits.getOrDefault(player.getUniqueId(),
                plugin.getConfig().getString("camera-shy-kit-preferences." + player.getUniqueId(), "smoke-artist"));
        Kit kit = Kit.fromId(selected);
        if (kit == null) kit = Kit.SMOKE_ARTIST;
        selectedKits.put(player.getUniqueId(), kit.id());
        ItemStack camera = namedItem(Material.SPYGLASS, ChatColor.AQUA + "Camera",
                List.of(ChatColor.GRAY + "Right-click to look through the lens.",
                        ChatColor.GRAY + "Press F (swap hands) to take a photograph."));
        ItemStack ability = namedItem(kit.icon(), ChatColor.LIGHT_PURPLE + kit.ability(),
                List.of(ChatColor.GRAY + "Kit: " + ChatColor.WHITE + kit.display(),
                        ChatColor.YELLOW + "Right-click to activate."));
        ItemMeta meta = ability.getItemMeta();
        meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, kit.id());
        ability.setItemMeta(meta);
        player.getInventory().setItem(0, camera);
        player.getInventory().setItem(1, ability);
        player.getInventory().setHeldItemSlot(0);
    }

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void tick() {
        serverTick++;
        if (phase == Phase.IDLE) return;
        if (phase == Phase.STARTING) return;
        if (phase != Phase.LIVE) return;

        long now = System.currentTimeMillis();
        if (now >= endsAt) {
            finish(selectTimeoutWinner(), "Time expired. The player with the fewest exposures wins.", true);
            return;
        }
        if (!finalSnapshot && endsAt - now <= 30_000L) {
            finalSnapshot = true;
            broadcastMatch(ChatColor.GOLD + "" + ChatColor.BOLD + "FINAL SNAPSHOT!"
                    + ChatColor.YELLOW + " Camera recharge is faster for the final 30 seconds.");
            for (UUID id : humans) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9F, 1.4F);
            }
        }

        removeOfflinePlayers();
        if (phase != Phase.LIVE) return;
        updateFocusTracking(now);
        if (serverTick % 10L == 0L) updateSpotterMarkers(now);
        updatePursuits(now);
        updateFakeFlashes(now);
        updateSpotlight(now);
        updateGroupPickup(now);
        updateStageFright(now);
        cleanupTemporaryEffects(now);
        if (serverTick % 5L == 0L) refreshHud(now);

        long livingHumans = alive.stream().filter(humans::contains).count();
        if (CameraShyRules.shouldEndMatch(alive.size(), (int) livingHumans, humans.size())) {
            UUID winner = alive.stream().filter(humans::contains).findFirst().orElse(null);
            String endReason = winner == null ? "All human players were eliminated."
                    : alive.size() <= 1 ? "Last player standing!" : "Last human player standing!";
            finish(winner, endReason, true);
        }
    }

    private void removeOfflinePlayers() {
        for (UUID id : new ArrayList<>(alive)) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) {
                alive.remove(id);
                if (humans.contains(id)) {
                    PlayerSnapshot snapshot = snapshots.remove(id);
                    if (snapshot != null) snapshot.persist(plugin, id);
                }
            }
        }
    }

    private UUID selectTimeoutWinner() {
        return alive.stream().filter(humans::contains)
                .max((first, second) -> {
                    int exposureCompare = Integer.compare(exposures.getOrDefault(second, 0), exposures.getOrDefault(first, 0));
                    if (exposureCompare != 0) return exposureCompare;
                    int photoCompare = Integer.compare(photos.getOrDefault(first, 0), photos.getOrDefault(second, 0));
                    if (photoCompare != 0) return photoCompare;
                    return Integer.compare(eliminations.getOrDefault(first, 0), eliminations.getOrDefault(second, 0));
                }).orElse(null);
    }

    private void refreshHud(long now) {
        long seconds = Math.max(0L, (endsAt - now) / 1_000L);
        String time = String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
        for (UUID id : humans) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) continue;
            BossBar bar = bossBars.get(id);
            if (bar != null) {
                bar.setTitle(ChatColor.GOLD + "CAMERA SHY " + ChatColor.WHITE + time
                        + ChatColor.GRAY + " | " + ChatColor.RED + exposures.getOrDefault(id, 0) + "/5");
                bar.setProgress(Math.max(0.0D, Math.min(1.0D, (double) (endsAt - now)
                        / (CameraShyRules.MATCH_SECONDS * 1_000.0D))));
            }
            String cameraState = now < cameraReadyAt.getOrDefault(id, 0L)
                    ? "RECHARGING" : "READY";
            player.sendActionBar(Component.text("Photos " + photos.getOrDefault(id, 0)
                    + "  |  Exposure " + exposures.getOrDefault(id, 0) + "/5"
                    + "  |  Camera " + cameraState
                    + "  |  Alive " + alive.stream().filter(humans::contains).count()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (phase != Phase.LIVE || !isAliveParticipant(event.getPlayer())) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String kit = item.getItemMeta().getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if (kit == null) return;
        event.setCancelled(true);
        activateAbility(event.getPlayer(), Kit.fromId(kit));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (phase != Phase.LIVE || !isAliveParticipant(player)) return;
        if (player.getInventory().getItemInMainHand().getType() != Material.SPYGLASS) return;
        event.setCancelled(true);
        if (!isUsingSpyglass(player)) {
            player.sendActionBar(Component.text("Right-click and hold the spyglass before pressing F to take a photo."));
            return;
        }
        takePhoto(player);
    }

    @EventHandler
    public void onHeldItem(PlayerItemHeldEvent event) {
        if (phase == Phase.LIVE && players.containsKey(event.getPlayer().getUniqueId())) {
            aimingSince.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (phase == Phase.LIVE && players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (phase != Phase.LIVE || !(event.getEntity() instanceof Player player) || !isAliveParticipant(player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (stack.hasItemMeta() && stack.getItemMeta().getPersistentDataContainer()
                .has(groupPickupKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            event.getItem().remove();
            groupPhotoCharges.add(player.getUniqueId());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Group Photo acquired! Your next photograph can capture two players.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
            clearGroupPickup();
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (phase == Phase.LIVE && event.getEntity() instanceof Player player
                && players.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (phase == Phase.LIVE && event.getEntity() instanceof Player player
                && players.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (phase == Phase.LIVE && event.getPlayer().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (phase == Phase.LIVE && event.getPlayer().getWorld() == arena) event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (phase != Phase.LIVE || !players.containsKey(event.getPlayer().getUniqueId())
                || event.getTo() == null) return;
        if (event.getFrom().getWorld() == arena && event.getTo().getWorld() != arena) {
            Location spawn = spawnLocations.get(event.getPlayer().getUniqueId());
            if (spawn != null) event.setTo(spawn);
            return;
        }
        if (event.getTo().getWorld() != arena) return;
        if (Math.abs(event.getTo().getX()) > 56.0D || Math.abs(event.getTo().getZ()) > 56.0D
                || event.getTo().getY() < FLOOR_Y - 2) {
            Location spawn = spawnLocations.get(event.getPlayer().getUniqueId());
            if (spawn != null) event.getPlayer().teleport(spawn);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        removeHumanOnDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeHumanOnDisconnect(event.getPlayer());
    }

    private void removeHumanOnDisconnect(Player player) {
        if (player == null) return;
        UUID id = player.getUniqueId();
        queue.remove(id);
        if (phase != Phase.LIVE || !humans.contains(id) || !alive.remove(id)) return;
        ArmorStand understudy = understudyStands.remove(id);
        if (understudy != null && understudy.isValid()) understudy.remove();
        clearSpotlight(id);
        PlayerSnapshot snapshot = snapshots.remove(id);
        if (snapshot != null) {
            if (player.isOnline()) {
                manager.returnPlayerToLobby(player);
                snapshot.restore(player);
                plugin.getConfig().set("camera-shy-pending-restores." + id, null);
                plugin.saveConfig();
            } else {
                snapshot.persist(plugin, id);
            }
        }
        removePlayerPresentation(id);
        long remainingHumans = alive.stream().filter(humans::contains).count();
        if (remainingHumans == 0L || (humans.size() > 1 && remainingHumans == 1L)) {
            finish(alive.stream().filter(humans::contains).findFirst().orElse(null),
                    "A player disconnected; the match has ended.", true);
        }
    }

    private boolean isAliveParticipant(Player player) {
        return player != null && player.isOnline() && players.containsKey(player.getUniqueId())
                && alive.contains(player.getUniqueId()) && player.getWorld() == arena;
    }

    private boolean isUsingSpyglass(Player player) {
        if (player == null || !player.isOnline()) return false;
        try {
            ItemStack active = player.getActiveItem();
            return player.hasActiveItem() && player.getActiveItemHand() == EquipmentSlot.HAND
                    && active != null && active.getType() == Material.SPYGLASS
                    && player.getInventory().getItemInMainHand().getType() == Material.SPYGLASS;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void takePhoto(Player shooter) {
        UUID shooterId = shooter.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = finalSnapshot ? CameraShyRules.FINAL_SNAPSHOT_COOLDOWN_MILLIS
                : CameraShyRules.PHOTO_COOLDOWN_MILLIS;
        if (runnerUntil.getOrDefault(shooterId, 0L) > now) {
            shooter.sendActionBar(Component.text("You cannot take a photograph during Adrenaline Rush."));
            return;
        }
        if (now < cameraReadyAt.getOrDefault(shooterId, 0L)) {
            shooter.sendActionBar(Component.text("Camera recharging..."));
            return;
        }
        RayTraceResult trace = arena.rayTraceEntities(shooter.getEyeLocation(),
                shooter.getEyeLocation().getDirection(), CameraShyRules.CAMERA_RANGE_BLOCKS, 0.35D,
                entity -> isPhotoTarget(shooter, entity));
        if (trace == null || trace.getHitEntity() == null) {
            shooter.sendActionBar(Component.text("No opponent in focus."));
            return;
        }
        Entity hit = trace.getHitEntity();
        Location camera = shooter.getEyeLocation();
        Location targetLocation = hit.getLocation().clone().add(0.0D, 0.9D, 0.0D);
        if (!shooter.hasLineOfSight(hit) || isBlockedBySmoke(camera, targetLocation)) {
            shooter.sendActionBar(Component.text("The shot is blocked."));
            return;
        }
        if (hit instanceof ArmorStand stand && stand.getPersistentDataContainer()
                .has(decoyStandKey, PersistentDataType.BYTE)) {
            cameraReadyAt.put(shooterId, now + cooldown);
            takeUnderstudyPhoto(shooter, stand);
            emitCameraFlash(shooter.getLocation());
            return;
        }
        if (!(hit instanceof Player target)) return;
        UUID targetId = target.getUniqueId();
        if (!CameraShyRules.mayPhotograph(now, cameraReadyAt.getOrDefault(shooterId, 0L),
                protectedUntil.getOrDefault(targetId, 0L))) {
            shooter.sendActionBar(Component.text("That player is recovering from a recent photograph."));
            return;
        }

        List<Player> targets = new ArrayList<>();
        targets.add(target);
        boolean group = groupPhotoCharges.contains(shooterId);
        if (group) {
            Player second = findGroupPhotoTarget(shooter, target);
            if (second != null) targets.add(second);
        }
        cameraReadyAt.put(shooterId, now + cooldown);
        if (group && targets.size() >= 2) groupPhotoCharges.remove(shooterId);
        emitCameraFlash(shooter.getLocation());
        for (Player photographed : targets) registerPhoto(shooter, photographed, now);
        if (targets.size() >= 2 && humans.contains(shooterId)) award(shooterId, "group-portrait");
        shooter.playSound(shooter.getLocation(), Sound.ITEM_SPYGLASS_USE, 1.0F, 1.35F);
        shooter.sendActionBar(Component.text("SNAP! " + targets.size() + " target(s) photographed."));
    }

    private boolean isPhotoTarget(Player shooter, Entity entity) {
        if (entity instanceof Player target) {
            return !target.getUniqueId().equals(shooter.getUniqueId())
                    && players.containsKey(target.getUniqueId()) && alive.contains(target.getUniqueId());
        }
        return entity instanceof ArmorStand stand
                && stand.getPersistentDataContainer().has(decoyStandKey, PersistentDataType.BYTE)
                && stand.getWorld() == arena;
    }

    private Player findGroupPhotoTarget(Player shooter, Player primary) {
        Vector direction = shooter.getEyeLocation().getDirection();
        Collection<Entity> nearby = arena.getNearbyEntities(primary.getLocation(), 4.0D, 3.0D, 4.0D);
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (Entity entity : nearby) {
            if (!(entity instanceof Player candidate) || candidate.equals(primary)
                    || candidate.equals(shooter) || !isAliveParticipant(candidate)) continue;
            double distance = shooter.getEyeLocation().distance(candidate.getEyeLocation());
            if (distance > CameraShyRules.CAMERA_RANGE_BLOCKS
                    || !CameraShyRules.mayPhotograph(now, cameraReadyAt.getOrDefault(shooter.getUniqueId(), 0L),
                            protectedUntil.getOrDefault(candidate.getUniqueId(), 0L))) continue;
            Vector toTarget = candidate.getEyeLocation().toVector().subtract(shooter.getEyeLocation().toVector());
            if (!CameraShyRules.insidePhotoCone(direction, toTarget, GROUP_PHOTO_MIN_DOT)) continue;
            if (!shooter.hasLineOfSight(candidate)
                    || isBlockedBySmoke(shooter.getEyeLocation(), candidate.getEyeLocation())) continue;
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void registerPhoto(Player shooter, Player target, long now) {
        UUID shooterId = shooter.getUniqueId();
        UUID targetId = target.getUniqueId();
        double distance = shooter.getEyeLocation().distance(target.getEyeLocation());
        int count = photos.merge(shooterId, 1, Integer::sum);
        uniquePhotos.computeIfAbsent(shooterId, ignored -> new LinkedHashSet<>()).add(targetId);
        photographedBy.computeIfAbsent(targetId, ignored -> new LinkedHashSet<>()).add(shooterId);
        protectedUntil.put(targetId, now + CameraShyRules.EXPOSURE_PROTECTION_MILLIS);
        pursuits.computeIfAbsent(targetId, ignored -> new HashMap<>()).put(shooterId, new Pursuit(shooterId));

        if (humans.contains(shooterId)) {
            if (CameraShyRules.isClosePhoto(distance)) award(shooterId, "close-encounter");
            if (CameraShyRules.isLongDistancePhoto(distance)) award(shooterId, "long-distance-photography");
            if (isUsingSpyglass(target)) award(shooterId, "candid-camera");
            Long previous = lastPhotoAt.get(shooterId);
            UUID previousTarget = lastPhotoTarget.get(shooterId);
            if (previousTarget != null && !previousTarget.equals(targetId)
                    && CameraShyRules.withinWindow(now, previous == null ? -1L : previous,
                            CameraShyRules.DOUBLE_TAKE_WINDOW_MILLIS)) {
                award(shooterId, "double-take");
            }
            lastPhotoAt.put(shooterId, now);
            lastPhotoTarget.put(shooterId, targetId);
            if (uniquePhotos.getOrDefault(shooterId, Set.of()).size() >= 5) award(shooterId, "paparazzi");
            if (humans.contains(targetId) && spotterMarks.getOrDefault(shooterId, Map.of()).getOrDefault(targetId, 0L) > now) {
                Set<UUID> spotted = spotterPhotoTargets.computeIfAbsent(shooterId, ignored -> new HashSet<>());
                spotted.add(targetId);
                if (spotted.size() >= 3) award(shooterId, "private-investigator");
            }
        }

        int exposure = exposures.merge(targetId, 1, Integer::sum);
        target.sendTitle(ChatColor.RED + "SNAP!", ChatColor.YELLOW + "Exposure " + exposure + "/5", 0, 18, 5);
        target.sendMessage(ChatColor.RED + "You've been photographed! " + ChatColor.WHITE + exposure + "/5 exposures.");
        target.playSound(target.getLocation(), Sound.ITEM_SPYGLASS_USE, 0.8F, 0.8F);
        shooter.sendMessage(ChatColor.GREEN + "Photographed " + target.getName() + " (" + exposure + "/5).");

        if (humans.contains(targetId)) {
            if (exposure == 4) fourExposureSince.put(targetId, now);
            if (exposure < 4) fourExposureSince.remove(targetId);
        }
        if (CameraShyRules.eliminatedAt(exposure)) {
            eliminate(target, shooter, now);
        }
    }

    private void eliminate(Player target, Player killer, long now) {
        UUID targetId = target.getUniqueId();
        UUID killerId = killer.getUniqueId();
        if (!alive.remove(targetId)) return;
        target.setGameMode(GameMode.SPECTATOR);
        target.teleport(new Location(arena, 0.5D, FLOOR_Y + 18.0D, 0.5D));
        target.sendTitle(ChatColor.DARK_RED + "ELIMINATED", ChatColor.GRAY + "Five photographs. Your match is over.", 5, 45, 10);
        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.65F, 1.5F);
        removePlayerPresentation(targetId);
        clearSpotlight(targetId);
        ArmorStand understudy = understudyStands.remove(targetId);
        if (understudy != null && understudy.isValid()) understudy.remove();

        eliminations.merge(killerId, 1, Integer::sum);
        Long previous = lastEliminationAt.get(killerId);
        int streak = previous != null && CameraShyRules.withinWindow(now, previous,
                CameraShyRules.DOUBLE_ELIMINATION_WINDOW_MILLIS)
                ? eliminationStreaks.getOrDefault(killerId, 1) + 1 : 1;
        eliminationStreaks.put(killerId, streak);
        lastEliminationAt.put(killerId, now);
        if (streak >= 2 && humans.contains(killerId)) award(killerId, "double-elimination");

        if (photographedBy.getOrDefault(killerId, Set.of()).contains(targetId)) {
            int revenge = revengeMatchCounts.merge(killerId, 1, Integer::sum);
            if (revenge >= 3 && humans.contains(killerId)) award(killerId, "revenge-is-sweet");
        }

        if (humans.contains(killerId)) {
            killer.sendMessage(ChatColor.GOLD + "ELIMINATION! " + ChatColor.WHITE + target.getName());
        }
        broadcastMatch(ChatColor.GOLD + target.getName() + ChatColor.YELLOW + " was eliminated by "
                + ChatColor.GOLD + killer.getName() + ChatColor.YELLOW + ".");
        pursuits.remove(targetId);
    }

    private void takeUnderstudyPhoto(Player photographer, ArmorStand stand) {
        String ownerRaw = stand.getPersistentDataContainer().get(decoyOwnerKey, PersistentDataType.STRING);
        if (ownerRaw != null) {
            try {
                UUID owner = UUID.fromString(ownerRaw);
                boolean fooledOpponent = CameraShyRules.shouldAwardUnderstudy(owner, photographer.getUniqueId(),
                        humans.contains(owner), humans.contains(photographer.getUniqueId()));
                if (fooledOpponent) award(owner, "method-actor");
                Player creator = Bukkit.getPlayer(owner);
                if (creator != null && creator.isOnline() && fooledOpponent) {
                    creator.sendMessage(ChatColor.LIGHT_PURPLE + "Your Understudy fooled " + photographer.getName() + "!");
                } else if (creator != null && creator.isOnline() && owner.equals(photographer.getUniqueId())) {
                    creator.sendMessage(ChatColor.GRAY + "Photographing your own Understudy does not fool an opponent.");
                }
            } catch (IllegalArgumentException ignored) { }
        }
        photographer.sendTitle(ChatColor.LIGHT_PURPLE + "FAKE OUT!", ChatColor.GRAY + "That was an Understudy decoy.", 0, 25, 5);
        photographer.playSound(photographer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.65F);
        stand.remove();
        understudyStands.values().removeIf(value -> value == stand);
        broadcastNearby(photographer.getLocation(), 18.0D,
                ChatColor.LIGHT_PURPLE + photographer.getName() + " photographed a stand-in!");
    }

    private void emitCameraFlash(Location location) {
        if (location == null || location.getWorld() == null) return;
        World world = location.getWorld();
        world.spawnParticle(Particle.FLASH, location.clone().add(0.0D, 1.2D, 0.0D), 1);
        world.spawnParticle(Particle.ELECTRIC_SPARK, location.clone().add(0.0D, 1.0D, 0.0D), 12, 0.35D, 0.35D, 0.35D, 0.01D);
        for (UUID id : alive) {
            Player player = players.get(id);
            if (player != null && player.isOnline() && player.getWorld() == world
                    && player.getLocation().distanceSquared(location) <= 24.0D * 24.0D) {
                player.playSound(location, Sound.ITEM_SPYGLASS_USE, 0.5F, 0.75F);
                player.sendActionBar(Component.text("A camera flash just exposed someone's position!"));
            }
        }
    }

    private void activateAbility(Player player, Kit kit) {
        if (kit == null) return;
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = switch (kit) {
            case SMOKE_ARTIST -> ABILITY_SMOKE_COOLDOWN;
            case DECOY -> ABILITY_DECOY_COOLDOWN;
            case RUNNER -> ABILITY_RUNNER_COOLDOWN;
            case SPOTTER -> ABILITY_SPOTTER_COOLDOWN;
            case UNDERSTUDY -> ABILITY_UNDERSTUDY_COOLDOWN;
        };
        if (now < abilityReadyAt.getOrDefault(id, 0L)) {
            long seconds = Math.max(1L, (abilityReadyAt.get(id) - now + 999L) / 1_000L);
            player.sendActionBar(Component.text("Ability recharging: " + seconds + "s"));
            return;
        }
        abilityReadyAt.put(id, now + cooldown);
        switch (kit) {
            case SMOKE_ARTIST -> {
                Location center = player.getLocation().clone().add(0.0D, 1.0D, 0.0D);
                smokeClouds.add(new SmokeCloud(id, center, now + SMOKE_LIFETIME_MILLIS));
                smokeUsedUntil.put(id, now + 7_000L);
                player.getWorld().spawnParticle(Particle.CLOUD, center, 65, 1.4D, 1.0D, 1.4D, 0.025D);
                player.getWorld().spawnParticle(Particle.SMOKE, center, 55, 1.2D, 0.9D, 1.2D, 0.025D);
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.9F, 0.7F);
                player.sendMessage(ChatColor.GRAY + "Smoke screen deployed. Break the photographer's line of sight!");
            }
            case DECOY -> {
                Location fake = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(7.0D));
                fake.setY(player.getLocation().getY() + 0.8D);
                fakeFlashes.add(new FakeFlash(id, fake, now + DECOY_FLASH_LIFETIME_MILLIS));
                fake.getWorld().spawnParticle(Particle.FLASH, fake, 1);
                fake.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, fake, 16, 0.35D, 0.35D, 0.35D, 0.02D);
                fake.getWorld().playSound(fake, Sound.ITEM_SPYGLASS_USE, 1.0F, 1.0F);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Fake snapshot deployed. Make someone investigate it!");
            }
            case RUNNER -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, true, true));
                runnerUntil.put(id, now + 3_000L);
                runnerUsedUntil.put(id, now + 10_000L);
                player.sendMessage(ChatColor.GREEN + "Adrenaline Rush! You cannot take photographs while sprinting.");
            }
            case SPOTTER -> {
                List<Player> detected = new ArrayList<>();
                for (UUID otherId : alive) {
                    if (otherId.equals(id)) continue;
                    Player target = players.get(otherId);
                    if (target == null || !target.isOnline()) continue;
                    double distance = player.getEyeLocation().distance(target.getEyeLocation());
                    if (distance <= SPOTTER_RANGE && player.hasLineOfSight(target)
                            && !isBlockedBySmoke(player.getEyeLocation(), target.getEyeLocation())) {
                        detected.add(target);
                        spotterMarks.computeIfAbsent(id, ignored -> new HashMap<>()).put(otherId, now + 15_000L);
                        player.spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0.0D, 1.5D, 0.0D),
                                8, 0.2D, 0.4D, 0.2D, 0.01D);
                    }
                }
                if (detected.isEmpty()) player.sendMessage(ChatColor.YELLOW + "Flash Finder found nobody in clear sight within 18 blocks.");
                else player.sendMessage(ChatColor.AQUA + "Flash Finder detected " + detected.size()
                        + " nearby opponent(s). Their markers are visible only to you.");
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.7F);
            }
            case UNDERSTUDY -> {
                ArmorStand old = understudyStands.remove(id);
                if (old != null && old.isValid()) old.remove();
                Location at = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(1.6D));
                at.setY(player.getLocation().getY());
                ArmorStand stand = arena.spawn(at, ArmorStand.class);
                stand.setVisible(true);
                stand.setArms(true);
                stand.setBasePlate(false);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.setCollidable(false);
                stand.setPersistent(false);
                stand.setCustomNameVisible(false);
                stand.getPersistentDataContainer().set(decoyStandKey, PersistentDataType.BYTE, (byte) 1);
                stand.getPersistentDataContainer().set(decoyOwnerKey, PersistentDataType.STRING, id.toString());
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skull = (SkullMeta) head.getItemMeta();
                skull.setOwningPlayer(player);
                head.setItemMeta(skull);
                stand.getEquipment().setHelmet(head);
                stand.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                stand.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                stand.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));
                understudyStands.put(id, stand);
                player.getWorld().spawnParticle(Particle.SMOKE, at, 15, 0.3D, 0.7D, 0.3D, 0.01D);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Understudy deployed. A photographer may mistake it for you!");
            }
        }
    }

    /** Repeats each marked opponent's location only to the Spotter for the full mark duration. */
    private void updateSpotterMarkers(long now) {
        for (Map.Entry<UUID, Map<UUID, Long>> spotterEntry : new ArrayList<>(spotterMarks.entrySet())) {
            UUID spotterId = spotterEntry.getKey();
            Player spotter = players.get(spotterId);
            Map<UUID, Long> marks = spotterEntry.getValue();
            if (spotter == null || !spotter.isOnline() || !alive.contains(spotterId)) {
                spotterMarks.remove(spotterId);
                continue;
            }
            for (Map.Entry<UUID, Long> mark : new ArrayList<>(marks.entrySet())) {
                UUID targetId = mark.getKey();
                if (mark.getValue() <= now) {
                    marks.remove(targetId);
                    continue;
                }
                Player target = players.get(targetId);
                if (target == null || !target.isOnline() || !alive.contains(targetId)
                        || target.getWorld() != arena) {
                    marks.remove(targetId);
                    continue;
                }
                Location marker = target.getLocation().clone().add(0.0D, 1.25D, 0.0D);
                spotter.spawnParticle(Particle.HAPPY_VILLAGER, marker, 4, 0.22D, 0.4D, 0.22D, 0.005D);
            }
            if (marks.isEmpty()) spotterMarks.remove(spotterId);
        }
    }

    private boolean isBlockedBySmoke(Location from, Location to) {
        return isBlockedBySmoke(from, to, null);
    }

    private boolean isBlockedBySmokeOwnedBy(Location from, Location to, UUID owner) {
        return isBlockedBySmoke(from, to, owner);
    }

    private boolean isBlockedBySmoke(Location from, Location to, UUID requiredOwner) {
        Vector start = from.toVector();
        Vector end = to.toVector();
        long now = System.currentTimeMillis();
        for (SmokeCloud cloud : smokeClouds) {
            if (cloud.expiresAt > now && cloud.center.getWorld() == from.getWorld()
                    && CameraShyRules.smokeBelongsTo(cloud.owner, requiredOwner)
                    && CameraShyRules.segmentIntersectsSphere(start, end, cloud.center.toVector(), 2.4D)) return true;
        }
        return false;
    }

    private void updateFocusTracking(long now) {
        focusSeenThisTick.clear();
        for (UUID id : alive) {
            Player shooter = players.get(id);
            if (shooter == null || !shooter.isOnline() || !isUsingSpyglass(shooter)) continue;
            aimingSince.putIfAbsent(id, now);
            RayTraceResult trace = arena.rayTraceEntities(shooter.getEyeLocation(),
                    shooter.getEyeLocation().getDirection(), CameraShyRules.CAMERA_RANGE_BLOCKS, 0.35D,
                    entity -> entity instanceof Player target && !target.getUniqueId().equals(id)
                            && players.containsKey(target.getUniqueId()) && alive.contains(target.getUniqueId()));
            if (trace == null || !(trace.getHitEntity() instanceof Player target)) continue;
            Location focusedLocation = target.getLocation().clone().add(0.0D, 0.9D, 0.0D);
            if (!shooter.hasLineOfSight(target) || isBlockedBySmoke(shooter.getEyeLocation(), focusedLocation)) continue;
            FocusKey key = new FocusKey(id, target.getUniqueId());
            focusSeenThisTick.add(key);
            focusAttempts.computeIfAbsent(key, ignored -> new FocusAttempt(serverTick, target.getLocation()));
            shooter.sendActionBar(Component.text(ChatColor.GREEN + "FOCUS LOCKED — " + target.getName()
                    + ChatColor.GRAY + " | press F to photograph"));
        }
        for (Map.Entry<FocusKey, FocusAttempt> entry : new ArrayList<>(focusAttempts.entrySet())) {
            FocusKey key = entry.getKey();
            if (focusSeenThisTick.contains(key)) continue;
            FocusAttempt attempt = entry.getValue();
            if (serverTick - attempt.startedTick < 10L) {
                focusAttempts.remove(key);
                continue;
            }
            Player shooter = players.get(key.shooter());
            Player target = players.get(key.target());
            if (shooter != null && target != null && shooter.isOnline() && target.isOnline()
                    && alive.contains(key.shooter()) && alive.contains(key.target())
                    && shooter.getWorld() == arena && target.getWorld() == arena
                    && (!shooter.hasLineOfSight(target)
                    || target.getLocation().distanceSquared(attempt.targetOrigin) >= 16.0D)) {
                if (humans.contains(key.target())) award(key.target(), "out-of-focus");
            }
            focusAttempts.remove(key);
        }
    }

    private void updatePursuits(long now) {
        for (Map.Entry<UUID, Map<UUID, Pursuit>> victimEntry : new ArrayList<>(pursuits.entrySet())) {
            UUID victimId = victimEntry.getKey();
            Player victim = players.get(victimId);
            if (victim == null || !victim.isOnline() || !alive.contains(victimId)) {
                pursuits.remove(victimId);
                continue;
            }
            for (Map.Entry<UUID, Pursuit> pursuitEntry : new ArrayList<>(victimEntry.getValue().entrySet())) {
                UUID pursuerId = pursuitEntry.getKey();
                Player pursuer = players.get(pursuerId);
                Pursuit pursuit = pursuitEntry.getValue();
                boolean sameWorld = pursuer != null && pursuer.isOnline()
                        && pursuer.getWorld() == victim.getWorld();
                boolean smokeObscured = sameWorld
                        && isBlockedBySmoke(victim.getEyeLocation(), pursuer.getEyeLocation());
                boolean ownSmokeObscured = sameWorld
                        && isBlockedBySmokeOwnedBy(victim.getEyeLocation(), pursuer.getEyeLocation(), victimId);
                boolean withinPursuitRange = sameWorld
                        && victim.getLocation().distanceSquared(pursuer.getLocation()) <= 24.0D * 24.0D;
                boolean visible = sameWorld && alive.contains(pursuerId)
                        && withinPursuitRange && victim.hasLineOfSight(pursuer) && !smokeObscured;
                if (visible) {
                    pursuit.lostSightSince = -1L;
                    pursuit.lostSightDueToSmoke = false;
                    continue;
                }
                if (pursuit.lostSightSince < 0L) {
                    pursuit.lostSightSince = now;
                    pursuit.lostSightDueToSmoke = alive.contains(pursuerId) && withinPursuitRange
                            && CameraShyRules.shouldAwardSmokeEscape(ownSmokeObscured, now,
                                    smokeUsedUntil.getOrDefault(victimId, 0L));
                }
                boolean escaped = pursuer == null || !pursuer.isOnline() || !alive.contains(pursuerId)
                        || victim.getWorld() != pursuer.getWorld()
                        || victim.getLocation().distanceSquared(pursuer.getLocation()) >= PURSUIT_ESCAPE_DISTANCE * PURSUIT_ESCAPE_DISTANCE;
                if (escaped && now - pursuit.lostSightSince >= PURSUIT_ESCAPE_MILLIS) {
                    victimEntry.getValue().remove(pursuerId);
                    if (humans.contains(victimId)) {
                        int escapes = encounterEscapes.merge(victimId, 1, Integer::sum);
                        if (escapes >= 3) award(victimId, "now-you-see-me");
                        if (runnerUsedUntil.getOrDefault(victimId, 0L) > now) award(victimId, "catch-me-if-you-can");
                        if (pursuit.lostSightDueToSmoke) {
                            int smoke = smokeEscapes.merge(victimId, 1, Integer::sum);
                            if (smoke >= 3) award(victimId, "smoke-and-mirrors");
                        }
                    }
                }
            }
            if (victimEntry.getValue().isEmpty()) pursuits.remove(victimId);
        }
    }

    private void updateFakeFlashes(long now) {
        for (FakeFlash flash : new ArrayList<>(fakeFlashes)) {
            if (flash.expiresAt <= now) {
                fakeFlashes.remove(flash);
                continue;
            }
            for (UUID id : alive) {
                if (!humans.contains(id) || id.equals(flash.owner) || flash.fooled.contains(id)) continue;
                Player player = players.get(id);
                if (player == null || !player.isOnline() || player.getWorld() != flash.location.getWorld()) continue;
                double distanceSquared = player.getLocation().distanceSquared(flash.location);
                boolean approached = distanceSquared <= 4.0D * 4.0D;
                Vector toward = flash.location.toVector().subtract(player.getEyeLocation().toVector());
                boolean lookedAt = toward.lengthSquared() > 0.01D
                        && player.getEyeLocation().getDirection().normalize().dot(toward.normalize()) >= 0.70D
                        && distanceSquared <= 11.0D * 11.0D
                        && hasClearSightToLocation(player, flash.location);
                if (approached || lookedAt) {
                    flash.fooled.add(id);
                    if (humans.contains(flash.owner)) {
                        Set<UUID> fooledTargets = decoyFooledOpponents.computeIfAbsent(flash.owner, ignored -> new HashSet<>());
                        if (fooledTargets.add(id) && fooledTargets.size() >= 3) award(flash.owner, "false-alarm");
                        Player owner = Bukkit.getPlayer(flash.owner);
                        if (owner != null) owner.sendMessage(ChatColor.LIGHT_PURPLE + "Your fake flash fooled " + player.getName() + "!");
                    }
                }
            }
        }
    }

    /** A fake flash only fools a player looking at it when solid blocks do not occlude it. */
    private boolean hasClearSightToLocation(Player player, Location target) {
        if (player == null || target == null || !player.isOnline()
                || player.getWorld() != target.getWorld()) return false;
        Location eye = player.getEyeLocation();
        Vector toward = target.toVector().subtract(eye.toVector());
        double distance = toward.length();
        if (distance < 0.1D) return true;
        RayTraceResult obstruction = player.getWorld().rayTraceBlocks(
                eye, toward.multiply(1.0D / distance), distance, FluidCollisionMode.NEVER, true);
        return obstruction == null
                || obstruction.getHitPosition().distanceSquared(target.toVector()) <= 1.0D;
    }

    private void updateSpotlight(long now) {
        if (now < nextSpotlightAt) return;
        nextSpotlightAt = now + SPOTLIGHT_INTERVAL_MILLIS;
        List<UUID> candidates = alive.stream().filter(humans::contains).toList();
        if (candidates.isEmpty()) return;
        UUID selected = candidates.get(random.nextInt(candidates.size()));
        Player target = players.get(selected);
        if (target == null || !target.isOnline()) return;
        broadcastMatch(ChatColor.GOLD + "DIRECTOR'S SPOTLIGHT: " + ChatColor.YELLOW + target.getName()
                + ChatColor.GRAY + " has been caught in the light!");
        spotlightOriginalGlow.put(selected, target.isGlowing());
        spotlightGlowUntil.put(selected, now + 3_000L);
        target.setGlowing(true);
        arena.spawnParticle(Particle.FLASH, target.getLocation().add(0.0D, 1.0D, 0.0D), 1);
    }

    private void updateGroupPickup(long now) {
        if (now < nextGroupPickupAt || (currentGroupPickup != null && currentGroupPickup.isValid())) return;
        nextGroupPickupAt = now + GROUP_PICKUP_INTERVAL_MILLIS;
        List<Location> locations = mazeSpawnLocations();
        Location location = locations.get(random.nextInt(locations.size())).clone().add(0.0D, 0.25D, 0.0D);
        ItemStack pickup = namedItem(Material.FIREWORK_STAR, ChatColor.LIGHT_PURPLE + "GROUP PHOTO",
                List.of(ChatColor.GRAY + "Collect this for one double-target photograph."));
        ItemMeta meta = pickup.getItemMeta();
        meta.getPersistentDataContainer().set(groupPickupKey, PersistentDataType.BYTE, (byte) 1);
        pickup.setItemMeta(meta);
        Item spawnedPickup = arena.dropItem(location, pickup);
        currentGroupPickup = spawnedPickup;
        spawnedPickup.setPickupDelay(0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentGroupPickup == spawnedPickup) clearGroupPickup();
        }, GROUP_PICKUP_LIFETIME_MILLIS / 50L);
        arena.spawnParticle(Particle.HAPPY_VILLAGER, location, 16, 0.4D, 0.5D, 0.4D, 0.02D);
        broadcastMatch(ChatColor.LIGHT_PURPLE + "A Group Photo pickup has appeared somewhere in the maze!");
    }

    private void updateStageFright(long now) {
        for (UUID id : new ArrayList<>(fourExposureSince.keySet())) {
            if (!alive.contains(id) || exposures.getOrDefault(id, 0) != 4) {
                fourExposureSince.remove(id);
                continue;
            }
            if (now - fourExposureSince.get(id) >= 60_000L && humans.contains(id)) {
                award(id, "stage-fright");
                fourExposureSince.remove(id);
            }
        }
    }

    private void cleanupTemporaryEffects(long now) {
        smokeClouds.removeIf(cloud -> cloud.expiresAt <= now);
        fakeFlashes.removeIf(flash -> flash.expiresAt <= now);
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(spotlightGlowUntil.entrySet())) {
            if (entry.getValue() <= now) clearSpotlight(entry.getKey());
        }
    }

    private void clearSpotlight(UUID playerId) {
        spotlightGlowUntil.remove(playerId);
        Boolean originalGlow = spotlightOriginalGlow.remove(playerId);
        Player target = players.get(playerId);
        if (originalGlow != null && target != null && target.isOnline()) target.setGlowing(originalGlow);
    }

    private void clearGroupPickup() {
        if (currentGroupPickup != null && currentGroupPickup.isValid()) currentGroupPickup.remove();
        currentGroupPickup = null;
    }

    private void broadcastQueue(String message) {
        for (Player player : queue.values()) if (player != null && player.isOnline()) player.sendMessage(message);
    }

    private void broadcastMatch(String message) {
        for (UUID id : humans) {
            Player player = players.get(id);
            if (player != null && player.isOnline()) player.sendMessage(message);
        }
    }

    private void broadcastNearby(Location location, double radius, String message) {
        for (UUID id : alive) {
            Player player = players.get(id);
            if (player != null && player.isOnline() && player.getWorld() == location.getWorld()
                    && player.getLocation().distanceSquared(location) <= radius * radius) player.sendMessage(message);
        }
    }

    private void removePlayerPresentation(UUID id) {
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    private void finish(UUID winnerId, String reason, boolean recordProgress) {
        if (phase == Phase.IDLE || phase == Phase.ENDING) return;
        phase = Phase.ENDING;
        if (recordProgress) recordMatchProgress(winnerId);
        clearGroupPickup();
        for (ArmorStand stand : understudyStands.values()) if (stand != null && stand.isValid()) stand.remove();
        understudyStands.clear();
        for (UUID id : new ArrayList<>(bossBars.keySet())) removePlayerPresentation(id);
        boolean pendingRestoreChanged = false;
        for (UUID id : players.keySet()) {
            Player player = players.get(id);
            PlayerSnapshot snapshot = snapshots.get(id);
            if (player == null || !player.isOnline()) {
                if (humans.contains(id) && snapshot != null) snapshot.persist(plugin, id);
                continue;
            }
            clearSpotlight(id);
            if (humans.contains(id)) {
                manager.returnPlayerToLobby(player);
                if (snapshot != null) {
                    snapshot.restore(player);
                    plugin.getConfig().set("camera-shy-pending-restores." + id, null);
                    pendingRestoreChanged = true;
                }
                player.sendMessage(ChatColor.GOLD + "Camera Shy ended: " + ChatColor.YELLOW + reason);
                if (winnerId != null && winnerId.equals(id)) {
                    player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "VICTORY!",
                            ChatColor.YELLOW + "You were the last photographer standing.", 5, 60, 15);
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                }
            }
        }
        if (pendingRestoreChanged) plugin.saveConfig();
        testPlayers.despawnSessionPlayers();
        phase = Phase.IDLE;
        players.clear(); humans.clear(); bots.clear(); alive.clear(); snapshots.clear();
        exposures.clear(); photos.clear(); eliminations.clear(); revengeEliminations.clear();
        encounterEscapes.clear(); smokeEscapes.clear(); decoyFooledOpponents.clear(); spotterPhotos.clear();
        uniquePhotos.clear(); photographedBy.clear(); spotterMarks.clear(); spotterPhotoTargets.clear();
        pursuits.clear(); cameraReadyAt.clear(); protectedUntil.clear(); abilityReadyAt.clear();
        runnerUntil.clear(); runnerUsedUntil.clear(); smokeUsedUntil.clear(); fourExposureSince.clear(); lastPhotoAt.clear();
        lastPhotoTarget.clear(); lastEliminationAt.clear(); eliminationStreaks.clear(); revengeMatchCounts.clear();
        focusAttempts.clear(); focusSeenThisTick.clear(); smokeClouds.clear(); fakeFlashes.clear();
        groupPhotoCharges.clear(); spawnLocations.clear(); aimingSince.clear();
        spotlightGlowUntil.clear(); spotlightOriginalGlow.clear();
        selectedKits.keySet().removeIf(id -> !Bukkit.getOfflinePlayer(id).hasPlayedBefore());
        pendingHumans = List.of();
        finalSnapshot = false;
        broadcastQueue(ChatColor.GRAY + "Camera Shy is ready for another queue.");
    }

    /** Show a player's persisted Camera Shy progression without requiring an active match. */
    public void sendStats(Player viewer, Player subject, int requestedPage) {
        if (viewer == null || !viewer.isOnline() || subject == null) return;
        int page = Math.max(1, Math.min(requestedPage, CameraShyProgressCatalog.pageCount()));
        UUID subjectId = subject.getUniqueId();
        String path = "local-stats." + subjectId + ".camera-shy.";
        int games = plugin.getConfig().getInt(path + "games-played", 0);
        int wins = plugin.getConfig().getInt(path + "wins", 0);
        int photosTaken = plugin.getConfig().getInt(path + "photos", 0);
        int eliminationsMade = plugin.getConfig().getInt(path + "eliminations", 0);
        int exposuresReceived = plugin.getConfig().getInt(path + "exposures-received", 0);
        long unlocked = CameraShyProgressCatalog.achievements().stream()
                .filter(achievement -> plugin.getConfig().getBoolean(
                        "local-achievements." + subjectId + ".camera-shy." + achievement.id(), false))
                .count();

        viewer.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CAMERA SHY STATS"
                + ChatColor.GRAY + " — " + ChatColor.WHITE + subject.getName());
        viewer.sendMessage(ChatColor.YELLOW + "Games: " + ChatColor.WHITE + games
                + ChatColor.GRAY + "  |  " + ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + wins);
        viewer.sendMessage(ChatColor.YELLOW + "Photographs: " + ChatColor.WHITE + photosTaken
                + ChatColor.GRAY + "  |  " + ChatColor.YELLOW + "Eliminations: " + ChatColor.WHITE + eliminationsMade);
        viewer.sendMessage(ChatColor.YELLOW + "Exposures received: " + ChatColor.WHITE + exposuresReceived
                + ChatColor.GRAY + "  |  " + ChatColor.YELLOW + "Achievements: " + ChatColor.WHITE
                + unlocked + "/" + CameraShyProgressCatalog.achievements().size());

        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "LIFETIME MILESTONES");
        for (CameraShyProgressCatalog.Milestone milestone : CameraShyProgressCatalog.milestones()) {
            int current = plugin.getConfig().getInt(path + milestone.statKey(), 0);
            boolean complete = current >= milestone.target()
                    || plugin.getConfig().getBoolean(
                            "local-milestones." + subjectId + ".camera-shy." + milestone.id(), false);
            viewer.sendMessage((complete ? ChatColor.GREEN + "✓ " : ChatColor.YELLOW + "• ")
                    + milestone.name() + ChatColor.GRAY + " — "
                    + Math.min(current, milestone.target()) + "/" + milestone.target() + " " + milestone.unit()
                    + (complete ? ChatColor.GREEN + " COMPLETE" : ""));
        }

        viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACHIEVEMENTS"
                + ChatColor.GRAY + " — page " + page + "/" + CameraShyProgressCatalog.pageCount());
        for (CameraShyProgressCatalog.Achievement achievement : CameraShyProgressCatalog.page(page)) {
            boolean complete = plugin.getConfig().getBoolean(
                    "local-achievements." + subjectId + ".camera-shy." + achievement.id(), false);
            viewer.sendMessage((complete ? ChatColor.GREEN + "✓ " : ChatColor.DARK_GRAY + "✗ ")
                    + (complete ? ChatColor.WHITE : ChatColor.GRAY) + achievement.name()
                    + ChatColor.DARK_GRAY + " — " + ChatColor.GRAY + achievement.requirement());
        }
        String command = viewer.equals(subject)
                ? "/stats camera-shy <page>"
                : "/stats " + subject.getName() + " camera-shy <page>";
        viewer.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + command
                + ChatColor.GRAY + " to view another achievement page.");
        viewer.sendMessage(ChatColor.DARK_GRAY + "----------------------------------------");
    }

    private void recordMatchProgress(UUID winnerId) {
        for (UUID id : humans) {
            String path = "local-stats." + id + ".camera-shy.";
            int matchPhotos = photos.getOrDefault(id, 0);
            int matchEliminations = eliminations.getOrDefault(id, 0);
            boolean won = id.equals(winnerId);
            int previousWins = plugin.getConfig().getInt(path + "wins", 0);
            int previousEliminations = plugin.getConfig().getInt(path + "eliminations", 0);
            int previousPhotos = plugin.getConfig().getInt(path + "photos", 0);
            int totalWins = previousWins + (won ? 1 : 0);
            int totalEliminations = previousEliminations + matchEliminations;
            int totalPhotos = previousPhotos + matchPhotos;

            plugin.getConfig().set(path + "games-played", plugin.getConfig().getInt(path + "games-played", 0) + 1);
            plugin.getConfig().set(path + "score", plugin.getConfig().getInt(path + "score", 0) + matchPhotos);
            plugin.getConfig().set(path + "knockouts", plugin.getConfig().getInt(path + "knockouts", 0) + matchEliminations);
            plugin.getConfig().set(path + "photos", totalPhotos);
            plugin.getConfig().set(path + "eliminations", totalEliminations);
            plugin.getConfig().set(path + "exposures-received", plugin.getConfig().getInt(path + "exposures-received", 0)
                    + exposures.getOrDefault(id, 0));
            if (won) {
                plugin.getConfig().set(path + "wins", totalWins);
                int streak = CameraShyRules.consecutiveWinsAfter(plugin.getConfig().getInt(path + "consecutive-wins", 0), true);
                plugin.getConfig().set(path + "consecutive-wins", streak);
                if (streak >= 3) award(id, "untouchable");
                if (exposures.getOrDefault(id, 0) == 0) award(id, "camera-shy");
                if (exposures.getOrDefault(id, 0) == 4) award(id, "narrow-escape");
                if (matchPhotos >= 10) award(id, "picture-perfect");
            } else {
                plugin.getConfig().set(path + "consecutive-wins", 0);
            }
            checkMilestone(id, "wins", previousWins, totalWins);
            checkMilestone(id, "eliminations", previousEliminations, totalEliminations);
            checkMilestone(id, "photos", previousPhotos, totalPhotos);
        }
        plugin.saveConfig();
    }

    private void checkMilestone(UUID id, String statKey, int previous, int current) {
        if (previous >= current) return;
        for (CameraShyProgressCatalog.Milestone milestone : CameraShyProgressCatalog.milestones()) {
            if (!milestone.statKey().equals(statKey) || previous >= milestone.target()
                    || current < milestone.target()) continue;
            String key = "local-milestones." + id + ".camera-shy." + milestone.id();
            if (plugin.getConfig().getBoolean(key, false)) continue;
            plugin.getConfig().set(key, true);
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "MILESTONE COMPLETE: "
                        + ChatColor.YELLOW + milestone.name() + ChatColor.GRAY
                        + " (" + milestone.target() + " " + milestone.unit() + ")");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.2F);
            }
        }
    }

    private void award(UUID id, String achievementId) {
        if (id == null || !humans.contains(id)) return;
        String key = "local-achievements." + id + ".camera-shy." + achievementId;
        if (plugin.getConfig().getBoolean(key, false)) return;
        plugin.getConfig().set(key, true);
        String name = CameraShyProgressCatalog.achievements().stream()
                .filter(achievement -> achievement.id().equals(achievementId))
                .map(CameraShyProgressCatalog.Achievement::name).findFirst().orElse(achievementId);
        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACHIEVEMENT UNLOCKED: "
                    + ChatColor.YELLOW + name);
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.9F, 1.25F);
        }
        plugin.saveConfig();
    }

    private void ensureArena() {
        if (arena == null) {
            World loaded = Bukkit.getWorld(WORLD_NAME);
            if (loaded != null) {
                arena = loaded;
            } else {
                WorldCreator creator = new WorldCreator(WORLD_NAME);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                creator.environment(World.Environment.NORMAL);
                arena = creator.createWorld();
            }
        }
        if (arena == null) return;
        arena.setDifficulty(Difficulty.PEACEFUL);
        arena.setTime(18000L);
        arena.setStorm(false);
        arena.setThundering(false);
        arena.setPVP(false);
        arena.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        arena.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        arena.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        arena.setGameRule(org.bukkit.GameRule.FALL_DAMAGE, false);
        arena.getWorldBorder().setCenter(0.0D, 0.0D);
        arena.getWorldBorder().setSize(120.0D);
        boolean buildFlag = plugin.getConfig().getBoolean("camera-shy.arena-built", false);
        Material foundationMarker = arena.getBlockAt(0, FLOOR_Y - 1, 0).getType();
        Material floorMarker = arena.getBlockAt(0, FLOOR_Y, 0).getType();
        if (CameraShyRules.arenaBuildRequired(buildFlag, foundationMarker, floorMarker)) {
            buildMaze(arena);
            plugin.getConfig().set("camera-shy.arena-built", true);
            plugin.saveConfig();
        }
    }

    private void buildMaze(World world) {
        int min = -HALF_CELLS * CELL_SIZE - CELL_SIZE / 2;
        int max = HALF_CELLS * CELL_SIZE + CELL_SIZE / 2;
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                world.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.DEEPSLATE_TILES, false);
                world.getBlockAt(x, FLOOR_Y, z).setType(Material.COARSE_DIRT, false);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 6; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        boolean[][][] walls = new boolean[MAZE_CELLS][MAZE_CELLS][4];
        boolean[][] visited = new boolean[MAZE_CELLS][MAZE_CELLS];
        for (int row = 0; row < MAZE_CELLS; row++) {
            for (int col = 0; col < MAZE_CELLS; col++) {
                for (int dir = 0; dir < 4; dir++) walls[row][col][dir] = true;
            }
        }
        record Cell(int row, int col) { }
        Deque<Cell> stack = new ArrayDeque<>();
        stack.push(new Cell(random.nextInt(MAZE_CELLS), random.nextInt(MAZE_CELLS)));
        visited[stack.peek().row()][stack.peek().col()] = true;
        int[][] directions = {{-1,0,0,2},{0,1,1,3},{1,0,2,0},{0,-1,3,1}};
        while (!stack.isEmpty()) {
            Cell current = stack.peek();
            List<int[]> candidates = new ArrayList<>();
            for (int[] direction : directions) {
                int nr = current.row() + direction[0];
                int nc = current.col() + direction[1];
                if (nr >= 0 && nr < MAZE_CELLS && nc >= 0 && nc < MAZE_CELLS && !visited[nr][nc]) {
                    candidates.add(direction);
                }
            }
            if (candidates.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] direction = candidates.get(random.nextInt(candidates.size()));
            int nr = current.row() + direction[0], nc = current.col() + direction[1];
            walls[current.row()][current.col()][direction[2]] = false;
            walls[nr][nc][direction[3]] = false;
            visited[nr][nc] = true;
            stack.push(new Cell(nr, nc));
        }

        // Carve optional side routes so the maze has loops instead of one perfect-tree route.
        for (int row = 0; row < MAZE_CELLS; row++) {
            for (int col = 0; col < MAZE_CELLS; col++) {
                if (random.nextDouble() >= 0.18D) continue;
                int[][] candidates = {{-1,0,0,2},{0,1,1,3},{1,0,2,0},{0,-1,3,1}};
                int[] direction = candidates[random.nextInt(candidates.length)];
                int nr = row + direction[0], nc = col + direction[1];
                if (nr >= 0 && nr < MAZE_CELLS && nc >= 0 && nc < MAZE_CELLS) {
                    walls[row][col][direction[2]] = false;
                    walls[nr][nc][direction[3]] = false;
                }
            }
        }

        for (int row = 0; row < MAZE_CELLS; row++) {
            for (int col = 0; col < MAZE_CELLS; col++) {
                int centerX = (col - HALF_CELLS) * CELL_SIZE;
                int centerZ = (row - HALF_CELLS) * CELL_SIZE;
                if (walls[row][col][0]) buildWall(world, centerX, centerZ - CELL_SIZE / 2, true);
                if (walls[row][col][1]) buildWall(world, centerX + CELL_SIZE / 2, centerZ, false);
                if (row == MAZE_CELLS - 1 && walls[row][col][2]) buildWall(world, centerX, centerZ + CELL_SIZE / 2, true);
                if (col == MAZE_CELLS - 1 && walls[row][col][3]) buildWall(world, centerX - CELL_SIZE / 2, centerZ, false);
            }
        }

        // Add a few low cover props without blocking the cell centres used for spawns.
        for (int i = 0; i < 28; i++) {
            int col = random.nextInt(MAZE_CELLS), row = random.nextInt(MAZE_CELLS);
            int x = (col - HALF_CELLS) * CELL_SIZE + (random.nextBoolean() ? -2 : 2);
            int z = (row - HALF_CELLS) * CELL_SIZE + (random.nextBoolean() ? -2 : 2);
            world.getBlockAt(x, FLOOR_Y, z).setType(Material.MOSS_BLOCK, false);
            world.getBlockAt(x, FLOOR_Y + 1, z).setType(Material.HAY_BLOCK, false);
            world.getBlockAt(x, FLOOR_Y + 2, z).setType(Material.HAY_BLOCK, false);
        }
        world.setSpawnLocation(0, FLOOR_Y + 1, 0);
        plugin.getLogger().info("Built the Camera Shy 15x15 hedge maze in " + WORLD_NAME + ".");
    }

    private void buildWall(World world, int x, int z, boolean horizontal) {
        for (int offset = -CELL_SIZE / 2; offset <= CELL_SIZE / 2; offset++) {
            int bx = horizontal ? x + offset : x;
            int bz = horizontal ? z : z + offset;
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
                org.bukkit.block.Block block = world.getBlockAt(bx, y, bz);
                block.setType(Material.OAK_LEAVES, false);
                Leaves leaves = (Leaves) block.getBlockData();
                leaves.setPersistent(true);
                block.setBlockData(leaves, false);
            }
        }
    }

    private List<Location> mazeSpawnLocations() {
        List<Location> locations = new ArrayList<>();
        for (int row = 0; row < MAZE_CELLS; row++) {
            for (int col = 0; col < MAZE_CELLS; col++) {
                int x = (col - HALF_CELLS) * CELL_SIZE;
                int z = (row - HALF_CELLS) * CELL_SIZE;
                locations.add(new Location(arena, x + 0.5D, FLOOR_Y + 1.0D, z + 0.5D));
            }
        }
        Collections.shuffle(locations, random);
        List<Location> separated = new ArrayList<>();
        for (Location candidate : locations) {
            if (separated.stream().allMatch(existing -> existing.distanceSquared(candidate) >= 12.0D * 12.0D)) {
                separated.add(candidate);
                if (separated.size() >= CameraShyRules.MAX_PLAYERS) break;
            }
        }
        if (separated.size() < CameraShyRules.MAX_PLAYERS) return locations.subList(0, CameraShyRules.MAX_PLAYERS);
        return separated;
    }
}
