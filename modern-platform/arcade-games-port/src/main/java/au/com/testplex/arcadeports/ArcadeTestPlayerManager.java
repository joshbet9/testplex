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

    private final JavaPlugin host;
    private final NamespacedKey markerKey;
    private final Map<UUID, Object> activeHandles = new LinkedHashMap<>();
    private final Map<UUID, Player> activePlayers = new LinkedHashMap<>();
    private List<Object> pendingHandles = List.of();
    private Object pendingApi;
    private BukkitTask pendingSpawnTask;
    private boolean shuttingDown;

    ArcadeTestPlayerManager(JavaPlugin host) {
        this.host = host;
        this.markerKey = new NamespacedKey(host, "session-test-player");
    }

    static int additionalPlayersNeeded(int humanPlayers) {
        return Math.max(0, TARGET_ROSTER_SIZE - Math.max(0, humanPlayers));
    }

    boolean isTestPlayer(UUID id) {
        if (id == null) return false;
        if (activeHandles.containsKey(id)) return true;
        Player player = Bukkit.getPlayer(id);
        return player != null && player.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    boolean isTestPlayer(Player player) {
        return player != null && isTestPlayer(player.getUniqueId());
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
            if (shuttingDown) return;
            try {
                Object api = api();
                Collection<?> bots = collection(invoke(api, "getBots"));
                int removed = 0;
                for (Object bot : bots) {
                    String name = string(invoke(bot, "getName"));
                    if (name != null && name.startsWith(RESERVED_NAME_PREFIX)) {
                        despawn(api, bot);
                        removed++;
                    }
                }
                if (removed > 0) {
                    host.getLogger().warning("Removed " + removed
                            + " stale Testplex game-scoped fake player(s) from a previous session.");
                }
            } catch (Exception exception) {
                host.getLogger().warning("Could not inspect FakePlayerPlugin for stale Testplex bots: "
                        + rootMessage(exception));
            }
        }, 40L);
    }

    boolean isAvailableForDamageablePlayers() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(FPP_PLUGIN_NAME);
            if (plugin == null || !plugin.isEnabled()) return false;
            // FPP defaults this to true. Refuse to start if the server operator has
            // disabled body damage, because the resulting dummies would not be useful
            // for ability/combat testing.
            if (!plugin.getConfig().getBoolean("body.damageable", true)) return false;
            api();
            return true;
        } catch (Exception exception) {
            host.getLogger().warning("FakePlayerPlugin API is unavailable: " + rootMessage(exception));
            return false;
        }
    }

    void spawnForMatch(int count, Location spawn, Player owner, Consumer<SpawnResult> completion) {
        if (completion == null) return;
        if (count <= 0) {
            completion.accept(new SpawnResult(List.of(), null));
            return;
        }
        if (shuttingDown || pendingSpawnTask != null || !activeHandles.isEmpty()) {
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
                String name = RESERVED_NAME_PREFIX
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                Object optional = invoke(api, "spawnBot",
                        new Class<?>[] {Location.class, Player.class, String.class},
                        spawn.clone(), owner, name);
                if (!(optional instanceof Optional<?> result) || result.isEmpty()) {
                    throw new IllegalStateException("FakePlayerPlugin refused to create " + name
                            + " (check its active bot limit and server log).");
                }
                handles.add(result.get());
            }
        } catch (Exception exception) {
            for (Object handle : handles) {
                try { despawn(api, handle); } catch (Exception ignored) { }
            }
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
                despawnBatch(api, handles);
                clearPendingHandles();
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
                    configureAsAfkPlayer(handle, player);
                    ready.add(player);
                }
            } catch (Exception exception) {
                cancelPendingSpawn();
                despawnBatch(api, handles);
                clearPendingHandles();
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
                despawnBatch(api, handles);
                clearPendingHandles();
                completion.accept(new SpawnResult(List.of(),
                        "Timed out waiting for all " + handles.size()
                                + " server-side fake players to complete their join lifecycle."));
            }
        }, 1L, 1L);
    }

    private void configureAsAfkPlayer(Object handle, Player player) throws Exception {
        // FPP's AFK bot type does not run player input/AI. Freeze it as well so the
        // target remains stationary until the game itself moves, teleports or kills it.
        invoke(handle, "setFrozen", new Class<?>[] {boolean.class}, true);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setCanPickupItems(false);
        player.setOp(false);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.setListed(true);
        player.playerListName(Component.text(ChatColor.GRAY + player.getName()));
        player.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    }

    void despawnSessionPlayers() {
        cancelPendingSpawn();
        Object api = pendingApi;
        if (api == null && (!activeHandles.isEmpty() || !pendingHandles.isEmpty())) {
            try { api = api(); } catch (Exception ignored) { }
        }
        if (api != null) {
            despawnBatch(api, new ArrayList<>(pendingHandles));
            despawnBatch(api, new ArrayList<>(activeHandles.values()));
        }
        clearPendingHandles();
        activeHandles.clear();
        activePlayers.clear();
    }

    private void despawnBatch(Object api, Collection<?> handles) {
        if (api == null || handles == null) return;
        for (Object handle : handles) {
            try {
                despawn(api, handle);
            } catch (Exception exception) {
                host.getLogger().warning("Could not despawn a Testplex fake player: "
                        + rootMessage(exception));
            }
        }
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

    private void despawn(Object api, Object handle) throws Exception {
        findSingleArgumentMethod(api.getClass(), "despawnBot", handle).invoke(api, handle);
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
