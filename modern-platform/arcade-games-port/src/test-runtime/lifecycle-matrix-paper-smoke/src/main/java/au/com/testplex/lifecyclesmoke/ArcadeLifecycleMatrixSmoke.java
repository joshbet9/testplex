package au.com.testplex.lifecyclesmoke;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Opt-in, disposable Paper acceptance harness. Four real protocol clients are
 * reused for every enabled game; the production Arcade start/stop lifecycle is
 * exercised without spawning 16 FakePlayerPlugin fixtures for each round.
 * This tests lifecycle entry/exit, not each game's full combat mechanics.
 */
public final class ArcadeLifecycleMatrixSmoke extends JavaPlugin {
    private static final List<String> CLIENT_NAMES =
            List.of("jn_playzmc", "HoopsSmoke2", "HoopsSmoke3", "HoopsSmoke4");
    private static final int LIVE_WAIT_TICKS = 300;
    private static final int MONSTER_MAZE_LIVE_WAIT_TICKS = 1_800;
    private static final int RESTORE_WAIT_TICKS = 100;

    private final List<String> results = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<Baseline> baselines = new ArrayList<>();
    private List<String> gameIds = List.of();
    private BukkitTask task;
    private JavaPlugin arcade;
    private Object manager;
    private Class<?> gameClass;
    private Object currentGame;
    private String currentId;
    private String lastMonsterMazeTrace = "";
    private int index;
    private int phase;
    private int elapsedTicks;
    private int failures;
    private long startedAt;
    private boolean closing;

    private record Baseline(UUID id, String world, GameMode mode, boolean canFly) {
        static Baseline capture(Player player) {
            return new Baseline(player.getUniqueId(), player.getWorld().getName(),
                    player.getGameMode(), player.getAllowFlight());
        }

        boolean restored() {
            Player player = Bukkit.getPlayer(id);
            return player != null && player.isOnline() && player.getWorld().getName().equals(world)
                    && player.getGameMode() == mode && player.getAllowFlight() == canFly;
        }
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getLogger().info("LIFECYCLE_MATRIX|WAITING|four Paper 26.3 protocol clients");
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 40L, 5L);
    }

    private void tick() {
        if (closing) return;
        elapsedTicks += 5;
        try {
            switch (phase) {
                case 0 -> awaitClients();
                case 1 -> beginNextGame();
                case 2 -> awaitLive();
                case 3 -> awaitRestoration();
                default -> throw new IllegalStateException("Unknown lifecycle matrix phase: " + phase);
            }
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "LIFECYCLE_MATRIX|EXCEPTION|" + currentId
                    + "|root=" + rootMessage(exception), exception);
            if (phase == 0) {
                finishWithFailure("Could not initialise clients or Arcade integration: " + exception);
            } else {
                failCase("Exception: " + rootMessage(exception));
            }
        }
    }

    private void awaitClients() throws Exception {
        if (elapsedTicks > 1_000) {
            finishWithFailure("Four protocol clients did not join in 50 seconds");
            return;
        }
        if (CLIENT_NAMES.stream().anyMatch(name -> Bukkit.getPlayerExact(name) == null)) return;
        for (String name : CLIENT_NAMES) {
            Player player = Bukkit.getPlayerExact(name);
            if (player == null || !player.isOnline()) return;
            players.add(player);
        }
        arcade = (JavaPlugin) Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
        if (arcade == null || !arcade.isEnabled()) throw new IllegalStateException("Arcade plugin is unavailable");
        manager = invoke(arcade, "getArcadePortManager", new Class<?>[0]);
        if (manager == null) throw new IllegalStateException("ArcadePortManager is unavailable");
        gameClass = Class.forName("au.com.testplex.arcadeports.ArcadePortGame");
        Set<String> enabled = new LinkedHashSet<>(arcade.getConfig().getStringList("enabled-games"));
        if (enabled.size() < 32) throw new IllegalStateException("Only " + enabled.size()
                + " enabled selections in the production catalogue; expected all 32 ready selections");
        if (!enabled.contains("evolution")) throw new IllegalStateException(
                "Evolution must be enabled and covered by the full-game lifecycle matrix");
        String selectedGames = System.getenv("TESTPLEX_LIFECYCLE_GAMES");
        if (selectedGames == null || selectedGames.isBlank()) {
            gameIds = List.copyOf(enabled);
        } else {
            List<String> requested = java.util.Arrays.stream(selectedGames.split(","))
                    .map(String::trim).filter(value -> !value.isEmpty()).toList();
            if (requested.isEmpty() || requested.stream().anyMatch(id -> !enabled.contains(id))) {
                throw new IllegalArgumentException("TESTPLEX_LIFECYCLE_GAMES must be a comma-separated subset "
                        + "of enabled game IDs: " + selectedGames);
            }
            gameIds = List.copyOf(requested);
        }
        // This disposable harness has four real clients and intentionally needs
        // no AFK roster. Speed Builders owns its bot-filling start path, unlike
        // the shared startQueuedInternal games below.
        Object speedBuilders = invoke(gameClass, null, "fromId", new Class<?>[]{String.class}, "speed-builders");
        arcade.getConfig().set("test-bot-loadouts.speed-builders.count", 0);
        invoke(manager, "setConfiguredTestBotCount", new Class<?>[]{gameClass, int.class}, speedBuilders, 0);
        for (Player player : players) baselines.add(Baseline.capture(player));
        getLogger().info("LIFECYCLE_MATRIX|READY|games=" + gameIds.size() + "|clients=" + players.size());
        phase = 1;
        elapsedTicks = 0;
    }

    private void beginNextGame() throws Exception {
        if (index >= gameIds.size()) {
            complete();
            return;
        }
        if (elapsedTicks < 20) return; // Allow all four clients to settle between sessions.
        currentId = gameIds.get(index);
        currentGame = invoke(gameClass, null, "fromId", new Class<?>[]{String.class}, currentId);
        if (currentGame == null) {
            failCase("Enabled game has no ArcadePortGame registration");
            return;
        }
        startedAt = System.nanoTime();
        lastMonsterMazeTrace = "";
        // SkyWars is intentionally available only through its Mixed Arcade
        // 16-player selection, not the standard queue variant.
        boolean mixedArcadeVariant = "skywars".equals(currentId);
        for (Player player : players) {
            if (!player.isOnline()) {
                finishWithFailure("Protocol client disconnected before " + currentId);
                return;
            }
            invoke(manager, "queue", new Class<?>[]{gameClass, Player.class, boolean.class, boolean.class},
                    currentGame, player, false, mixedArcadeVariant);
        }
        CommandSender console = Bukkit.getConsoleSender();
        if ("camera-shy".equals(currentId) || "speed-builders".equals(currentId)) {
            invoke(manager, "startQueued", new Class<?>[]{gameClass, Player.class, CommandSender.class, boolean.class},
                    currentGame, null, console, false);
        } else {
            // Bypass only the production 16-bot injection layer. The shared
            // readiness, map, roster and phase checks stay in startQueuedInternal.
            invoke(manager, "startQueuedInternal", new Class<?>[]{gameClass, Player.class, CommandSender.class, boolean.class},
                    currentGame, null, console, mixedArcadeVariant);
        }
        phase = 2;
        elapsedTicks = 0;
    }

    private void awaitLive() throws Exception {
        Object active = invoke(manager, "activeDebugGame", new Class<?>[0]);
        Object session = field(manager, "active");
        boolean live = false;
        if (currentId != null && currentId.startsWith("monster-maze")) {
            // Monster Maze is an in-process module with its own lifecycle state.
            // The shared manager's Session is intentionally not its source of truth.
            Object module = invoke(arcade, "getMonsterMazeModule", new Class<?>[0]);
            Object gameManager = module == null ? null
                    : invoke(module, "getGameManager", new Class<?>[0]);
            // isGameRunning includes STARTING. Require the actual LIVE phase before
            // stopping, otherwise the test can cancel async generation/countdown and
            // contaminate the following Monster Maze mode selection.
            if (gameManager != null) {
                Object state = field(gameManager, "state");
                Object alive = field(gameManager, "alive");
                Object roundParticipants = field(gameManager, "roundParticipants");
                Object pending = field(gameManager, "pendingTestplexParticipants");
                Object center = field(gameManager, "center");
                int aliveCount = alive instanceof java.util.Collection<?> values ? values.size() : -1;
                int participantCount = roundParticipants instanceof java.util.Collection<?> values ? values.size() : -1;
                int pendingCount = pending instanceof java.util.Collection<?> values ? values.size() : -1;
                String playerStates = players.stream().map(player -> player.getName() + ":"
                        + player.getGameMode() + "@" + player.getWorld().getName())
                        .collect(java.util.stream.Collectors.joining(","));
                String trace = "phase=" + state + ",alive=" + aliveCount + ",participants="
                        + participantCount + ",pending=" + pendingCount + ",managed="
                        + field(gameManager, "testplexManagedRound") + ",center="
                        + (center instanceof org.bukkit.Location location && location.getWorld() != null
                                ? location.getWorld().getName() + "@" + location.getBlockX() + ","
                                        + location.getBlockY() + "," + location.getBlockZ() : center)
                        + ",players=" + playerStates;
                if (!trace.equals(lastMonsterMazeTrace)) {
                    getLogger().info("LIFECYCLE_MATRIX|MONSTER_MAZE_TRACE|" + currentId + "|" + trace);
                    lastMonsterMazeTrace = trace;
                }
            }
            live = gameManager != null && "LIVE".equals(String.valueOf(field(gameManager, "state")));
        } else if (currentGame.equals(active)) {
            if ("plexball".equals(currentId)) {
                // Plexball owns its own Live phase after the shared manager has
                // captured snapshots; its shared Session.liveStarted is unused.
                Object module = invoke(arcade, "getPlexballGame", new Class<?>[0]);
                live = module != null && Boolean.TRUE.equals(invoke(module, "isActive", new Class<?>[0]));
            } else if (session != null) {
                live = Boolean.TRUE.equals(field(session, "liveStarted"));
            } else {
                // Standalone Arcade-owned modules expose an active game but no
                // shared Session; allow a few ticks for their lifecycle to settle.
                live = elapsedTicks >= 20;
            }
        }
        if (live) {
            getLogger().info("LIFECYCLE_MATRIX|LIVE|" + currentId + "|players=" + players.size());
            invoke(manager, "stop", new Class<?>[]{String.class}, "automated lifecycle matrix: " + currentId);
            phase = 3;
            elapsedTicks = 0;
            return;
        }
        int liveWaitTicks = currentId != null && currentId.startsWith("monster-maze")
                ? MONSTER_MAZE_LIVE_WAIT_TICKS : LIVE_WAIT_TICKS;
        if (elapsedTicks >= liveWaitTicks) {
            String moduleState = "";
            if (currentId != null && currentId.startsWith("monster-maze")) {
                Object module = invoke(arcade, "getMonsterMazeModule", new Class<?>[0]);
                Object gameManager = module == null ? null
                        : invoke(module, "getGameManager", new Class<?>[0]);
                moduleState = "; monsterMazeMode="
                        + (module == null ? "missing" : invoke(module, "getMode", new Class<?>[0]))
                        + "; monsterMazeRunning="
                        + (module != null && Boolean.TRUE.equals(
                                invoke(module, "isGameRunning", new Class<?>[0])))
                        + "; monsterMazeState="
                        + (gameManager == null ? "missing" : field(gameManager, "state"))
                    + "; lastTrace=" + lastMonsterMazeTrace;
            }
            failCase("No Live transition within " + (liveWaitTicks / 20)
                    + " seconds; active=" + active + moduleState);
        }
    }

    private void awaitRestoration() throws Exception {
        boolean idle = invoke(manager, "activeDebugGame", new Class<?>[0]) == null;
        if (currentId != null && currentId.startsWith("monster-maze")) {
            Object module = invoke(arcade, "getMonsterMazeModule", new Class<?>[0]);
            idle = idle && (module == null || !Boolean.TRUE.equals(
                    invoke(module, "isGameRunning", new Class<?>[0])));
        }
        boolean restored = baselines.stream().allMatch(Baseline::restored);
        if (idle && restored) {
            addResult("PASS", "Live/stop/restore");
            advance();
            return;
        }
        if (elapsedTicks >= RESTORE_WAIT_TICKS) {
            failCase("Stop did not return every client to the lobby; idle=" + idle
                    + ", restored=" + restored);
        }
    }

    private void failCase(String reason) {
        failures++;
        addResult("FAIL", reason);
        try { if (manager != null) invoke(manager, "stop", new Class<?>[]{String.class}, "lifecycle smoke failure"); }
        catch (Exception stopFailure) { getLogger().warning("Matrix cleanup failed: " + stopFailure); }
        advance();
    }

    private void addResult(String status, String reason) {
        long millis = startedAt == 0L ? 0 : (System.nanoTime() - startedAt) / 1_000_000L;
        String safe = reason.replace('\t', ' ').replace('\n', ' ');
        results.add(status + "\t" + currentId + "\t" + players.size() + "\t" + millis + "ms\t" + safe);
        getLogger().info("LIFECYCLE_MATRIX|" + status + "|" + currentId + "|" + millis + "ms|" + safe);
    }

    private void advance() {
        index++;
        currentId = null;
        currentGame = null;
        startedAt = 0L;
        elapsedTicks = 0;
        phase = 1;
    }

    private void finishWithFailure(String reason) {
        failures++;
        currentId = currentId == null ? "harness" : currentId;
        addResult("FAIL", reason);
        complete();
    }

    private void complete() {
        if (closing) return;
        closing = true;
        if (task != null) task.cancel();
        results.add("SUMMARY\t" + gameIds.size() + "\t" + (gameIds.size() - failures)
                + "\t" + failures + "\t" + (failures == 0 ? "PASS" : "FAIL"));
        Path output = getDataFolder().toPath().resolve("results.tsv");
        try {
            Files.writeString(output, String.join("\n", results) + "\n", StandardCharsets.UTF_8);
        } catch (Exception exception) {
            getLogger().severe("Could not write lifecycle matrix result: " + exception);
        }
        getLogger().info("LIFECYCLE_MATRIX|DONE|games=" + gameIds.size() + "|failed=" + failures);
        Bukkit.getScheduler().runTaskLater(this, Bukkit::shutdown, 20L);
    }

    private static Object field(Object target, String name) throws Exception {
        // Session's state may live in an extracted superclass. Walk the class
        // hierarchy so structural refactors do not invalidate this smoke test.
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Continue into the superclass; other reflection failures are real errors.
            }
        }
        throw new NoSuchFieldException(name + " on " + target.getClass().getName());
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        return invoke(target.getClass(), target, name, types, arguments);
    }

    private static Object invoke(Class<?> clazz, Object target, String name, Class<?>[] types,
                                 Object... arguments) throws Exception {
        Method method = clazz.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }

    private static String rootMessage(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return String.valueOf(cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
    }
}
