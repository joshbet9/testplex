package au.com.testplex.arcadeports;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Creates match-local AFK test participants through FakePlayerPlugin's public API.
 *
 * The returned entities are actual Bukkit Player instances backed by server-side
 * ServerPlayer entities. That means the existing Arcade participant, team, kit,
 * damage, projectile, ability, scoreboard and elimination paths see them as
 * players instead of custom mob target dummies.
 *
 * FakePlayerPlugin is deliberately accessed reflectively: it is an optional,
 * separately-installed server plugin with a proprietary distribution licence.
 * This project neither bundles nor copies any of its implementation.
 */
final class ArcadeTestPlayerManager {
    static final int TARGET_ROSTER_SIZE = 16;
    private static final String FPP_PLUGIN_NAME = "FakePlayerPlugin";
    private static final String RESERVED_NAME_PREFIX = "TPXTEST";
    private static final int CLEANUP_TIMEOUT_TICKS = 200;

    private final JavaPlugin host;
    private final NamespacedKey markerKey;
    private final Map<UUID, Object> activeHandles = new LinkedHashMap<>();
    private final Map<UUID, Player> activePlayers = new LinkedHashMap<>();
    private final Map<UUID, RetiringTestPlayer> retiringHandles = new LinkedHashMap<>();
    private final List<Runnable> cleanupCallbacks = new ArrayList<>();
    private List<Object> pendingHandles = List.of();
    private Object pendingApi;
    private Object retiringApi;
    private BukkitTask pendingSpawnTask;
    private BukkitTask cleanupTask;
    private boolean shuttingDown;
    private boolean startupCleanupComplete;
    private boolean startupCleanupSucceeded;

    ArcadeTestPlayerManager(JavaPlugin host) {
        this.host = host;
        this.markerKey = new NamespacedKey(host, "session-test-player");
    }

    enum CleanupAction {
        WAIT_FOR_RESPAWN,
        WAIT_FOR_ENTITY,
        DESPAWN,
        WAIT_FOR_DISCONNECT,
        KICK_ORPHAN,
        COMPLETE
    }

    /** Pure lifecycle decision used by the cleanup loop and its regression tests. */
    static CleanupAction cleanupAction(boolean handleRegistered, boolean respawning,
            boolean bodyAvailable, boolean playerOnline, boolean despawnRequested, boolean timedOut) {
        if (despawnRequested) {
            if (!playerOnline) return CleanupAction.COMPLETE;
            return timedOut ? CleanupAction.KICK_ORPHAN : CleanupAction.WAIT_FOR_DISCONNECT;
        }
        if (handleRegistered) {
            if (respawning) return timedOut ? CleanupAction.KICK_ORPHAN : CleanupAction.WAIT_FOR_RESPAWN;
            if (bodyAvailable && playerOnline) return CleanupAction.DESPAWN;
            return timedOut ? CleanupAction.KICK_ORPHAN : CleanupAction.WAIT_FOR_ENTITY;
        }
        return playerOnline ? CleanupAction.KICK_ORPHAN : CleanupAction.COMPLETE;
    }

    static int additionalPlayersNeeded(int humanPlayers) {
        // Only disposable Paper gameplay harnesses set this JVM property. Real
        // deployments retain the full 16-player test roster by default.
        return additionalPlayersNeeded(humanPlayers,
                Integer.getInteger("testplex.arcade.acceptance-target-roster-size", TARGET_ROSTER_SIZE));
    }

    static int additionalPlayersNeeded(int humanPlayers, int targetRosterSize) {
        int clampedTarget = Math.max(1, Math.min(TARGET_ROSTER_SIZE, targetRosterSize));
        return Math.max(0, clampedTarget - Math.max(0, humanPlayers));
    }

    static boolean canFitHumanPlayers(int humanPlayers) {
        return Math.max(0, humanPlayers) <= TARGET_ROSTER_SIZE;
    }

    /** Exclude session test players and missing UUIDs from persistent progression and rewards. */
    static boolean shouldPersistProgression(UUID playerId, boolean testPlayer) {
        return playerId != null && !testPlayer;
    }

    boolean isTestPlayer(UUID id) {
        if (id == null) return false;
        if (activeHandles.containsKey(id) || retiringHandles.containsKey(id)) return true;
        Player player = Bukkit.getPlayer(id);
        return player != null && (player.getName().startsWith(RESERVED_NAME_PREFIX)
                || player.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE));
    }

    boolean isTestPlayer(Player player) {
        return player != null && isTestPlayer(player.getUniqueId());
    }

    boolean isStartupCleanupComplete() {
        return startupCleanupComplete;
    }

    boolean isStartupCleanupSuccessful() {
        return startupCleanupSucceeded;
    }

    boolean isManagedTestPlayer(UUID id) {
        return id != null && (activeHandles.containsKey(id) || retiringHandles.containsKey(id));
    }

    boolean isCleanupInProgress() {
        return cleanupTask != null || !retiringHandles.isEmpty() || !activeHandles.isEmpty()
                || pendingSpawnTask != null || !pendingHandles.isEmpty();
    }

    int activeCount() {
        return activeHandles.size();
    }

    /**
     * Removes test players left behind by an interrupted previous process.
     * The reserved TPXTEST name prefix is used only by this integration.
     */
    void cleanupStaleSessionPlayers() {
        Bukkit.getScheduler().runTaskLater(host, () -> {
            if (shuttingDown) {
                startupCleanupComplete = true;
                return;
            }
            boolean cleanupScheduled = false;
            try {
                Object api = api();
                Collection<?> bots = collection(invoke(api, "getBots"));
                List<Object> staleHandles = new ArrayList<>();
                java.util.Set<UUID> staleIds = new java.util.HashSet<>();
                for (Object bot : bots) {
                    String name = string(invoke(bot, "getName"));
                    if (name != null && name.startsWith(RESERVED_NAME_PREFIX)) {
                        staleHandles.add(bot);
                        staleIds.add((UUID) invoke(bot, "getUuid"));
                    }
                }

                // Catch a Bukkit Player whose FakePlayerPlugin registry entry was lost
                // after an interrupted respawn/despawn hand-off.
                List<UUID> orphanIds = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean reservedName = player.getName().startsWith(RESERVED_NAME_PREFIX);
                    boolean marked = player.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
                    if ((reservedName || marked) && !staleIds.contains(player.getUniqueId())) {
                        orphanIds.add(player.getUniqueId());
                    }
                }

                if (!staleHandles.isEmpty() || !orphanIds.isEmpty()) {
                    host.getLogger().warning("Cleaning " + staleHandles.size()
                            + " stale FakePlayerPlugin handle(s) and " + orphanIds.size()
                            + " orphaned Testplex player(s) from a previous session.");
                    cleanupScheduled = true;
                    scheduleCleanup(api, staleHandles, orphanIds, () -> {
                        startupCleanupSucceeded = noOnlineTestPlayersRemain();
                        startupCleanupComplete = true;
                        if (!startupCleanupSucceeded) {
                            host.getLogger().severe("Startup cleanup could not remove every stale TPXTEST player; game starts remain disabled.");
                        }
                    });
                    return;
                }
                startupCleanupSucceeded = true;
            } catch (Exception exception) {
                host.getLogger().warning("Could not inspect FakePlayerPlugin for stale Testplex bots: "
                        + rootMessage(exception));
                startupCleanupSucceeded = false;
            } finally {
                if (!cleanupScheduled) startupCleanupComplete = true;
            }
        }, 40L);
    }

    private boolean noOnlineTestPlayersRemain() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().startsWith(RESERVED_NAME_PREFIX)
                    || player.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) return false;
        }
        return true;
    }

    boolean isAvailableForDamageablePlayers() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(FPP_PLUGIN_NAME);
            if (plugin == null || !plugin.isEnabled()) return false;
            // FPP defaults this to true. Refuse to start if the server operator has
            // disabled body damage, because the resulting dummies would not be useful
            // for ability/combat testing.
            if (!plugin.getConfig().getBoolean("body.damageable", true)) return false;
            configureDedicatedTestServerRuntime(plugin);
            api();
            return true;
        } catch (Exception exception) {
            host.getLogger().warning("FakePlayerPlugin API is unavailable: " + rootMessage(exception));
            return false;
        }
    }

    /**
     * FakePlayerPlugin does not currently expose per-bot respawn or persistence
     * switches in its public API. Testplex runs on a dedicated sandbox server, so
     * set the minimum runtime policy needed for real Player objects to survive a
     * normal death/respawn event and never be restored as a global bot population.
     */
    private void configureDedicatedTestServerRuntime(Plugin plugin) {
        if (!(plugin instanceof JavaPlugin fakePlayerPlugin)) {
            throw new IllegalStateException("FakePlayerPlugin is not a JavaPlugin implementation.");
        }
        var config = fakePlayerPlugin.getConfig();
        boolean changed = false;
        if (!config.getBoolean("death.respawn-on-death", false)) {
            config.set("death.respawn-on-death", true);
            changed = true;
        }
        if (!config.getBoolean("death.suppress-drops", false)) {
            config.set("death.suppress-drops", true);
            changed = true;
        }
        if (config.getInt("death.respawn-delay", 1) != 1) {
            config.set("death.respawn-delay", 1);
            changed = true;
        }
        if (config.getBoolean("persistence.enabled", true)) {
            config.set("persistence.enabled", false);
            changed = true;
        }
        if (!config.getBoolean("body.drop-items-on-despawn", false)) {
            config.set("body.drop-items-on-despawn", true);
            changed = true;
        }
        if (!config.getBoolean("body.pick-up-items", true)) {
            config.set("body.pick-up-items", true);
            changed = true;
        }
        if (config.getInt("persistence.autosave-interval-seconds", 120) != 0) {
            config.set("persistence.autosave-interval-seconds", 0);
            changed = true;
        }
        if (config.getInt("limits.max-bots", 1000) < TARGET_ROSTER_SIZE) {
            config.set("limits.max-bots", TARGET_ROSTER_SIZE);
            changed = true;
        }
        if (changed) {
            fakePlayerPlugin.saveConfig();
            host.getLogger().info("Applied dedicated Testplex FakePlayerPlugin policy: respawn enabled, "
                    + "restart persistence disabled, 16-player capacity.");
        }
    }

    void spawnForMatch(int count, Location spawn, Player owner, Consumer<SpawnResult> completion) {
        if (completion == null) return;
        if (count <= 0) {
            completion.accept(new SpawnResult(List.of(), null));
            return;
        }
        if (shuttingDown || pendingSpawnTask != null || !activeHandles.isEmpty()
                || !retiringHandles.isEmpty() || cleanupTask != null) {
            completion.accept(new SpawnResult(List.of(), "A previous test-player batch is still being cleaned up."));
            return;
        }
        if (spawn == null || spawn.getWorld() == null) {
            completion.accept(new SpawnResult(List.of(), "The Arcade lobby spawn is unavailable."));
            return;
        }

        final Object api;
        try {
            if (!isAvailableForDamageablePlayers()) {
                completion.accept(new SpawnResult(List.of(),
                        "FakePlayerPlugin is required and must have body.damageable: true. "
                                + "Install/enable FakePlayerPlugin for Paper 26.3, then restart Testplex."));
                return;
            }
            api = api();
        } catch (Exception exception) {
            completion.accept(new SpawnResult(List.of(),
                    "Could not initialise FakePlayerPlugin: " + rootMessage(exception)));
            return;
        }

        List<Object> handles = new ArrayList<>(count);
        try {
            for (int i = 0; i < count; i++) {
                // Reuse a small, reserved name pool so FakePlayerPlugin's
                // optional inventory-on-despawn snapshots cannot grow without bound.
                String name = RESERVED_NAME_PREFIX + String.format(Locale.ROOT, "%02d", i + 1);
                if (Bukkit.getPlayerExact(name) != null) {
                    throw new IllegalStateException("Reserved test-player name is already online: " + name);
                }
                Location stagingLocation = spawn.clone().add(
                        ((i % 5) - 2) * 1.25D, 0.0D, ((i / 5) - 1) * 1.25D);
                Object optional = invoke(api, "spawnBot",
                        new Class<?>[] {Location.class, Player.class, String.class},
                        stagingLocation, owner, name);
                if (!(optional instanceof Optional<?> result) || result.isEmpty()) {
                    throw new IllegalStateException("FakePlayerPlugin refused to create " + name
                            + " (check its active bot limit and server log).");
                }
                handles.add(result.get());
            }
        } catch (Exception exception) {
            scheduleCleanup(api, handles, List.of(), null);
            completion.accept(new SpawnResult(List.of(), rootMessage(exception)));
            return;
        }

        pendingHandles = List.copyOf(handles);
        pendingApi = api;
        final int timeoutTicks = 200;
        final int[] elapsed = {0};
        pendingSpawnTask = Bukkit.getScheduler().runTaskTimer(host, () -> {
            if (shuttingDown) {
                cancelPendingSpawn();
                clearPendingHandles();
                scheduleCleanup(api, handles, List.of(), null);
                completion.accept(new SpawnResult(List.of(), "Testplex is shutting down."));
                return;
            }

            List<Player> ready = new ArrayList<>(handles.size());
            boolean allReady = true;
            try {
                for (Object handle : handles) {
                    Object rawPlayer = invoke(handle, "getEntity");
                    if (!(rawPlayer instanceof Player player) || !player.isOnline() || !player.isValid()) {
                        allReady = false;
                        continue;
                    }
                    configureAsAfkPlayer(handle, player, true);
                    ready.add(player);
                }
            } catch (Exception exception) {
                cancelPendingSpawn();
                clearPendingHandles();
                scheduleCleanup(api, handles, List.of(), null);
                completion.accept(new SpawnResult(List.of(),
                        "FakePlayerPlugin created an incomplete player batch: " + rootMessage(exception)));
                return;
            }

            elapsed[0]++;
            if (allReady && ready.size() == handles.size()) {
                cancelPendingSpawn();
                pendingHandles = List.of();
                pendingApi = null;
                for (int i = 0; i < handles.size(); i++) {
                    Player player = ready.get(i);
                    Object handle = handles.get(i);
                    activeHandles.put(player.getUniqueId(), handle);
                    activePlayers.put(player.getUniqueId(), player);
                }
                completion.accept(new SpawnResult(List.copyOf(ready), null));
                return;
            }

            if (elapsed[0] >= timeoutTicks) {
                cancelPendingSpawn();
                clearPendingHandles();
                scheduleCleanup(api, handles, List.of(), null);
                completion.accept(new SpawnResult(List.of(),
                        "Timed out waiting for all " + handles.size()
                                + " server-side fake players to complete their join lifecycle."));
            }
        }, 1L, 1L);
    }

    private void configureAsAfkPlayer(Object handle, Player player, boolean initialSpawn) throws Exception {
        // Disable autonomous input/AI, but leave entity physics enabled so these AFK
        // participants can be moved by ordinary knockback and velocity mechanics.
        invoke(handle, "setFrozen", new Class<?>[] {boolean.class}, false);
        invoke(handle, "setHeadAiEnabled", new Class<?>[] {boolean.class}, false);
        invoke(handle, "setSwimAiEnabled", new Class<?>[] {boolean.class}, false);
        invoke(handle, "setPveEnabled", new Class<?>[] {boolean.class}, false);
        invoke(handle, "setPickUpItemsEnabled", new Class<?>[] {boolean.class}, true);
        invoke(handle, "setPickUpXpEnabled", new Class<?>[] {boolean.class}, true);
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
        player.setCollidable(true);
        player.setCanPickupItems(true);
        player.setOp(false);
        // Preserve a game-assigned respawn mode (for example, spectator).
        if (initialSpawn) player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        // Listed visibility is exposed by some Paper builds and hidden behind the
        // server implementation in others; keep this optional so Player setup still
        // compiles against the exact server API without depending on CraftPlayer.
        try {
            player.getClass().getMethod("setListed", boolean.class).invoke(player, true);
        } catch (ReflectiveOperationException ignored) {
            // The entity remains in Bukkit's online player set even if the TAB flag is unavailable.
        }
        player.playerListName(Component.text(ChatColor.GRAY + player.getName()));
        player.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    }

    /**
     * FakePlayerPlugin uses each bot's last requested teleport as its respawn
     * location. Arcade's normal setup teleports the Bukkit Player directly, so
     * sync that final game spawn through the FPP API after the session is built.
     */
    void syncMatchRespawnLocation(Player player) {
        if (player == null) return;
        Object handle = activeHandles.get(player.getUniqueId());
        if (handle == null || retiringHandles.containsKey(player.getUniqueId()) || !player.isOnline()) return;
        try {
            invoke(handle, "teleport", new Class<?>[] {Location.class}, player.getLocation().clone());
        } catch (Exception exception) {
            host.getLogger().warning("Could not sync the match respawn location for "
                    + player.getName() + ": " + rootMessage(exception));
        }
    }

    /**
     * FakePlayerPlugin replaces the ServerPlayer backing a bot after a death.
     * Poll its public handle until its respawn task has rebound getEntity(), then
     * refresh Testplex's UUID-indexed reference and FakePlayerPlugin's own respawn point.
     */
    void whenRespawned(UUID playerId, Consumer<Player> completion) {
        Object handle = activeHandles.get(playerId);
        if (handle == null || completion == null || retiringHandles.containsKey(playerId)) return;
        Plugin schedulerOwner = cleanupSchedulerOwner();
        if (schedulerOwner == null) {
            host.getLogger().warning("Cannot wait for FakePlayerPlugin respawn: no active scheduler owner.");
            return;
        }
        final int[] elapsed = {0};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(schedulerOwner, () -> {
            if (shuttingDown || retiringHandles.containsKey(playerId)
                    || activeHandles.get(playerId) != handle) {
                task[0].cancel();
                return;
            }
            try {
                boolean respawning = Boolean.TRUE.equals(invoke(handle, "isRespawning"));
                Object rawPlayer = invoke(handle, "getEntity");
                if (!respawning && rawPlayer instanceof Player player && player.isOnline()
                        && player.isValid() && !player.isDead()) {
                    configureAsAfkPlayer(handle, player, false);
                    activePlayers.put(playerId, player);
                    syncMatchRespawnLocation(player);
                    task[0].cancel();
                    completion.accept(player);
                    return;
                }
                if (++elapsed[0] >= CLEANUP_TIMEOUT_TICKS) {
                    host.getLogger().warning("Timed out waiting for FakePlayerPlugin to finish respawning "
                            + "test player " + playerId + ".");
                    task[0].cancel();
                }
            } catch (Exception exception) {
                host.getLogger().warning("Could not rebind respawned Testplex player " + playerId + ": "
                        + rootMessage(exception));
                task[0].cancel();
            }
        }, 1L, 1L);
    }

    void despawnSessionPlayers() {
        cancelPendingSpawn();
        Object api = pendingApi != null ? pendingApi : retiringApi;
        List<Object> handles = new ArrayList<>(pendingHandles);
        handles.addAll(activeHandles.values());
        clearPendingHandles();
        if (api == null && !handles.isEmpty()) {
            try { api = api(); } catch (Exception ignored) { }
        }
        scheduleCleanup(api, handles, List.of(), null);
    }

    private void scheduleCleanup(Object api, Collection<?> handles, Collection<UUID> orphanIds,
            Runnable completion) {
        if (completion != null) cleanupCallbacks.add(completion);
        if (api != null) retiringApi = api;
        if (handles != null) {
            for (Object handle : handles) {
                if (handle == null) continue;
                try {
                    UUID id = (UUID) invoke(handle, "getUuid");
                    retiringHandles.putIfAbsent(id, new RetiringTestPlayer(handle));
                } catch (Exception exception) {
                    host.getLogger().warning("Could not identify a FakePlayerPlugin handle during cleanup: "
                            + rootMessage(exception));
                }
            }
        }
        if (orphanIds != null) {
            for (UUID id : orphanIds) {
                if (id != null) retiringHandles.putIfAbsent(id, new RetiringTestPlayer(null));
            }
        }
        if (retiringHandles.isEmpty()) {
            finishCleanupIfEmpty();
            return;
        }
        if (cleanupTask != null) return;
        Plugin schedulerOwner = cleanupSchedulerOwner();
        if (schedulerOwner == null) {
            host.getLogger().severe("No scheduler is available to finish Testplex fake-player cleanup.");
            forceCleanupWithoutScheduler();
            return;
        }
        cleanupTask = Bukkit.getScheduler().runTaskTimer(schedulerOwner, this::tickCleanup, 1L, 1L);
    }

    private Plugin cleanupSchedulerOwner() {
        Plugin fakePlayerPlugin = Bukkit.getPluginManager().getPlugin(FPP_PLUGIN_NAME);
        if (fakePlayerPlugin != null && fakePlayerPlugin.isEnabled()) return fakePlayerPlugin;
        return host.isEnabled() ? host : null;
    }

    private void tickCleanup() {
        if (retiringHandles.isEmpty()) {
            finishCleanupIfEmpty();
            return;
        }

        java.util.Set<UUID> registeredIds = new java.util.HashSet<>();
        boolean registryReadable = false;
        Object api = retiringApi;
        if (api != null) {
            try {
                for (Object bot : collection(invoke(api, "getBots"))) {
                    registeredIds.add((UUID) invoke(bot, "getUuid"));
                }
                registryReadable = true;
            } catch (Exception exception) {
                host.getLogger().warning("Could not read FakePlayerPlugin handles during teardown: "
                        + rootMessage(exception));
            }
        }

        for (Map.Entry<UUID, RetiringTestPlayer> entry : new ArrayList<>(retiringHandles.entrySet())) {
            UUID id = entry.getKey();
            RetiringTestPlayer state = entry.getValue();
            state.elapsedTicks++;
            Player online = Bukkit.getPlayer(id);
            boolean playerOnline = online != null && online.isOnline();
            Object rawPlayer = null;
            boolean respawning = false;
            if (state.handle != null) {
                try {
                    respawning = Boolean.TRUE.equals(invoke(state.handle, "isRespawning"));
                    rawPlayer = invoke(state.handle, "getEntity");
                } catch (Exception exception) {
                    host.getLogger().warning("Could not inspect retiring test player " + id + ": "
                            + rootMessage(exception));
                }
            }
            boolean bodyAvailable = rawPlayer instanceof Player body && body.isOnline()
                    && body.isValid() && !body.isDead();
            boolean handleRegistered = state.handle != null && retiringApi != null
                    && (!registryReadable || registeredIds.contains(id));
            CleanupAction action = cleanupAction(handleRegistered, respawning, bodyAvailable,
                    playerOnline, state.despawnRequested, state.elapsedTicks >= CLEANUP_TIMEOUT_TICKS);

            switch (action) {
                case WAIT_FOR_RESPAWN, WAIT_FOR_ENTITY, WAIT_FOR_DISCONNECT -> {
                    // FakePlayerPlugin owns the respawn hand-off and the eventual quit.
                }
                case DESPAWN -> {
                    clearDisposablePlayerState((Player) rawPlayer);
                    try {
                        state.despawnRequested = despawn(retiringApi, state.handle);
                        if (state.despawnRequested) state.elapsedTicks = 0;
                    } catch (Exception exception) {
                        host.getLogger().warning("Could not despawn Testplex fake player " + id + ": "
                                + rootMessage(exception));
                    }
                }
                case KICK_ORPHAN -> {
                    if (playerOnline && online.getName().startsWith(RESERVED_NAME_PREFIX)) {
                        clearDisposablePlayerState(online);
                        online.kick(Component.empty());
                    }
                    if (handleRegistered && retiringApi != null && !state.despawnRequested) {
                        try {
                            state.despawnRequested = despawn(retiringApi, state.handle);
                        } catch (Exception exception) {
                            host.getLogger().warning("Fallback FakePlayerPlugin despawn failed for " + id + ": "
                                    + rootMessage(exception));
                        }
                    } else {
                        state.despawnRequested = true;
                    }
                    state.elapsedTicks = 0;
                }
                case COMPLETE -> {
                    retiringHandles.remove(id);
                    activeHandles.remove(id);
                    activePlayers.remove(id);
                }
            }
        }
        finishCleanupIfEmpty();
    }

    private void clearDisposablePlayerState(Player player) {
        if (player == null || !player.isOnline()) return;
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0.0F);
    }

    private void finishCleanupIfEmpty() {
        if (!retiringHandles.isEmpty()) return;
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        retiringApi = null;
        clearPendingHandles();
        List<Runnable> callbacks = new ArrayList<>(cleanupCallbacks);
        cleanupCallbacks.clear();
        for (Runnable callback : callbacks) {
            try { callback.run(); }
            catch (RuntimeException exception) {
                host.getLogger().warning("A Testplex fake-player cleanup callback failed: "
                        + rootMessage(exception));
            }
        }
    }

    private void forceCleanupWithoutScheduler() {
        for (UUID id : new ArrayList<>(retiringHandles.keySet())) {
            Player online = Bukkit.getPlayer(id);
            if (online != null && online.isOnline() && online.getName().startsWith(RESERVED_NAME_PREFIX)) {
                clearDisposablePlayerState(online);
                online.kick(Component.empty());
            }
            activeHandles.remove(id);
            activePlayers.remove(id);
            retiringHandles.remove(id);
        }
        finishCleanupIfEmpty();
    }

    private static final class RetiringTestPlayer {
        final Object handle;
        int elapsedTicks;
        boolean despawnRequested;
        RetiringTestPlayer(Object handle) { this.handle = handle; }
    }

    private void clearPendingHandles() {
        pendingHandles = List.of();
        pendingApi = null;
    }

    void shutdown() {
        shuttingDown = true;
        despawnSessionPlayers();
    }

    private Object api() throws Exception {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(FPP_PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException(FPP_PLUGIN_NAME + " is not installed/enabled.");
        }
        Object api = invoke(plugin, "getFppApi");
        if (api == null) throw new IllegalStateException("FakePlayerPlugin returned a null API.");
        return api;
    }

    private boolean despawn(Object api, Object handle) throws Exception {
        Object result = findSingleArgumentMethod(api.getClass(), "despawnBot", handle).invoke(api, handle);
        return !(result instanceof Boolean accepted) || accepted;
    }

    private static Method findSingleArgumentMethod(Class<?> type, String name, Object argument)
            throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isInstance(argument)) return method;
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "(" + argument.getClass().getName() + ")");
    }

    private static Object invoke(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        return target.getClass().getMethod(name, parameterTypes).invoke(target, arguments);
    }

    private static Collection<?> collection(Object value) {
        return value instanceof Collection<?> collection ? collection : List.of();
    }

    private static String string(Object value) {
        return value instanceof String text ? text : null;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    private void cancelPendingSpawn() {
        if (pendingSpawnTask != null) {
            pendingSpawnTask.cancel();
            pendingSpawnTask = null;
        }
    }

    record SpawnResult(List<Player> players, String error) {
        boolean succeeded() { return error == null; }
    }
}
