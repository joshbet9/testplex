package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Optional adapter to Mineplex Hub's authoritative CoreClient rank service.
 *
 * The modern Paper module must remain independently buildable, so it never links
 * against the legacy Mineplex classes at compile time. When Hub is installed we
 * resolve its CoreClientManager from Hub's classloader and read only fully-loaded
 * account records. Missing Hub or an account which is not loaded is represented
 * as an empty Optional so the isolated Testplex rank configuration remains usable.
 */
final class MineplexCoreRankBridge {
    private final JavaPlugin plugin;
    private final Object clientManager;
    private final Method getClient;
    private final Method getAccountId;
    private final Method getPrimaryGroup;
    private final Method getCanBePrimary;
    private final Method getDisplay;
    private final Method inheritsFrom;
    private final Object adminGroup;
    private final AtomicBoolean warned = new AtomicBoolean();

    private MineplexCoreRankBridge(
            JavaPlugin plugin,
            Object clientManager,
            Method getClient,
            Method getAccountId,
            Method getPrimaryGroup,
            Method getCanBePrimary,
            Method getDisplay,
            Method inheritsFrom,
            Object adminGroup) {
        this.plugin = plugin;
        this.clientManager = clientManager;
        this.getClient = getClient;
        this.getAccountId = getAccountId;
        this.getPrimaryGroup = getPrimaryGroup;
        this.getCanBePrimary = getCanBePrimary;
        this.getDisplay = getDisplay;
        this.inheritsFrom = inheritsFrom;
        this.adminGroup = adminGroup;
    }

    static Optional<MineplexCoreRankBridge> connect(
            JavaPlugin plugin, Consumer<Player> rankUpdateListener) {
        Plugin hub = plugin.getServer().getPluginManager().getPlugin("Hub");
        if (hub == null || !hub.isEnabled()) return Optional.empty();

        try {
            ClassLoader loader = hub.getClass().getClassLoader();
            Class<?> registryType = Class.forName("mineplex.core.Managers", true, loader);
            Class<?> managerType = Class.forName(
                    "mineplex.core.account.CoreClientManager", true, loader);
            Class<?> clientType = Class.forName(
                    "mineplex.core.account.CoreClient", true, loader);
            Class<?> groupType = Class.forName(
                    "mineplex.core.account.permissions.PermissionGroup", true, loader);

            Object manager = registryType.getMethod("get", Class.class).invoke(null, managerType);
            if (manager == null) return Optional.empty();

            Method getClient = managerType.getMethod("Get", Player.class);
            Method getAccountId = clientType.getMethod("getAccountId");
            Method getPrimaryGroup = clientType.getMethod("getPrimaryGroup");
            Method getCanBePrimary = groupType.getMethod("canBePrimary");
            Method getDisplay = groupType.getMethod(
                    "getDisplay", boolean.class, boolean.class, boolean.class, boolean.class);
            Method inheritsFrom = clientType.getMethod("inheritsFrom", groupType);
            Object adminGroup = null;
            for (Object candidate : groupType.getEnumConstants()) {
                if (candidate instanceof Enum<?> group && "ADMIN".equals(group.name())) {
                    adminGroup = candidate;
                    break;
                }
            }
            if (adminGroup == null) {
                throw new IllegalStateException("Mineplex PermissionGroup.ADMIN was not found.");
            }

            MineplexCoreRankBridge bridge = new MineplexCoreRankBridge(
                    plugin, manager, getClient, getAccountId,
                    getPrimaryGroup, getCanBePrimary, getDisplay, inheritsFrom, adminGroup);
            bridge.registerPrimaryRankUpdateListener(loader, rankUpdateListener);
            return Optional.of(bridge);
        } catch (ReflectiveOperationException | LinkageError failure) {
            plugin.getLogger().warning("Mineplex Hub is present, but its CoreClient rank API "
                    + "could not be connected; using isolated Testplex rank overrides. "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
            return Optional.empty();
        }
    }

    Optional<String> rankId(Player player) {
        return rankPresentation(player).map(RankPresentation::rankId);
    }

    /**
     * Mirrors the legacy Arcade scoreboard's PermissionGroup teams and display prefix.
     * The old scoreboard calls getDisplay(true, true, true, false) for every
     * primary group rather than collapsing staff/content ranks into broad categories.
     */
    Optional<RankPresentation> rankPresentation(Player player) {
        Object client = loadedClient(player);
        if (client == null) return Optional.empty();

        try {
            Object group = getPrimaryGroup.invoke(client);
            if (!(group instanceof Enum<?> rank)
                    || !Boolean.TRUE.equals(getCanBePrimary.invoke(group))) {
                return Optional.empty();
            }
            String plainDisplay = (String) getDisplay.invoke(group, false, false, false, false);
            String formattedDisplay = (String) getDisplay.invoke(group, true, true, true, false);
            return Optional.of(presentationForGroup(rank.name(), plainDisplay, formattedDisplay));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            warnOnce("Could not read a loaded CoreClient primary rank.", failure);
            return Optional.empty();
        }
    }

    Optional<Boolean> hasAdminAccess(Player player) {
        Object client = loadedClient(player);
        if (client == null) return Optional.empty();

        try {
            // Ensure a loaded client's primary group has been materialised before the
            // legacy inheritsFrom method dereferences it.
            if (getPrimaryGroup.invoke(client) == null) return Optional.empty();
            return Optional.of(Boolean.TRUE.equals(inheritsFrom.invoke(client, adminGroup)));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            warnOnce("Could not evaluate CoreClient ADMIN inheritance.", failure);
            return Optional.empty();
        }
    }

    private Object loadedClient(Player player) {
        if (player == null || !player.isOnline()) return null;
        try {
            Object client = getClient.invoke(clientManager, player);
            if (client == null) return null;
            Object accountId = getAccountId.invoke(client);
            if (!(accountId instanceof Number number) || number.intValue() <= 0) {
                // CoreClientManager.Get(Player) may return an unloaded placeholder.
                return null;
            }
            return client;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            warnOnce("Could not retrieve the player's CoreClient.", failure);
            return null;
        }
    }

    private void registerPrimaryRankUpdateListener(
            ClassLoader loader, Consumer<Player> rankUpdateListener) throws ReflectiveOperationException {
        Class<?> eventType = Class.forName(
                "mineplex.core.account.event.OnlinePrimaryGroupUpdateEvent", true, loader);
        Class<? extends Event> bukkitEventType = eventType.asSubclass(Event.class);
        Method getPlayer = eventType.getMethod("getPlayer");
        Listener listener = new Listener() { };
        EventExecutor executor = (registeredListener, event) -> {
            try {
                Object target = getPlayer.invoke(event);
                if (target instanceof Player player && player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> rankUpdateListener.accept(player));
                }
            } catch (ReflectiveOperationException | RuntimeException failure) {
                warnOnce("Could not process a CoreClient primary-rank update event.", failure);
            }
        };
        plugin.getServer().getPluginManager().registerEvent(
                bukkitEventType, listener, EventPriority.MONITOR, executor, plugin, true);
    }

    private void warnOnce(String message, Throwable failure) {
        if (warned.compareAndSet(false, true)) {
            plugin.getLogger().warning(message + " Falling back to isolated rank configuration. "
                    + failure.getClass().getSimpleName() + ": " + failure.getMessage());
        }
    }

    record RankPresentation(String groupName, String prefix, String rankId) { }

    static RankPresentation presentationForGroup(
            String groupName, String plainDisplay, String formattedDisplay) {
        String prefix = plainDisplay == null || plainDisplay.isEmpty()
                ? "" : formattedDisplay + ChatColor.RESET + " ";
        return new RankPresentation(groupName, prefix, rankIdForGroup(groupName));
    }

    static String rankIdForGroup(String groupName) {
        if (groupName == null || groupName.isBlank()) return "player";
        return switch (groupName.trim().toLowerCase(Locale.ROOT)) {
            case "owner" -> "owner";
            case "admin", "dev", "lt" -> "admin";
            case "mod", "srmod" -> "mod";
            case "trainee", "support" -> "helper";
            case "ultra", "hero", "legend", "titan", "eternal" -> "ultra";
            default -> "player";
        };
    }
}
