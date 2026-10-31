package au.com.testplex.wizardssmoke;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Disposable Paper 26.3 acceptance harness:
 *  - loads every source snapshot through WizardsGameManager.createArena();
 *  - then waits for two real protocol clients and starts a two-player Wizards round;
 *  - verifies kit/spawn/loot state and leave/winner/player-snapshot restoration.
 *
 * This plugin is only packaged in the temporary CI smoke server, never in Testplex jars.
 */
public final class WizardsPaperSmoke extends JavaPlugin {
    private static final List<String> POOLS = List.of("Wizards", "Nano Wizards");
    private static final List<String> MAPS = List.of(
            "haven", "wizardarena", "orbisharbor", "aspen", "hogwarts",
            "industrialwest", "alicudi", "thaumaturgecolony", "magicallypossible", "kalamit");
    private static final String GAMEPLAY_MAP = "source-maps/Wizards/haven.zip";
    private static final String OWNER_NAME = "jn_playzmc";
    private static final int MAX_CLIENT_WAIT_TICKS = 300 * 20 / 5;
    private static final int MAX_START_WAIT_TICKS = 60 * 20 / 5;

    private final List<String> results = new ArrayList<>();
    private Plugin arcade;
    private Object wizards;
    private BukkitTask gameplayTask;
    private Player owner;
    private Player rival;
    private World gameplayArena;
    private PlayerBaseline ownerBaseline;
    private PlayerBaseline rivalBaseline;
    private int gameplayStage;
    private int gameplayTicks;
    private boolean shuttingDown;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTask(this, this::runAll);
    }

    private void runAll() {
        try {
            arcade = Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
            require(arcade != null && arcade.isEnabled(), "ArcadeMechanicsModern is not enabled");
            wizards = field(arcade, "wizards");
            require(wizards != null, "The shared Arcade Wizards controller was not initialised");
            Method createArena = method(wizards.getClass(), "createArena");

            int total = POOLS.size() * MAPS.size();
            for (String pool : POOLS) {
                for (String map : MAPS) {
                    String resource = "source-maps/" + pool + "/" + map + ".zip";
                    long started = System.nanoTime();
                    try {
                        arcade.getConfig().set("selected-maps.wizards", resource);
                        arcade.saveConfig();

                        Object value = createArena.invoke(wizards);
                        require(value instanceof World, "createArena() did not return a loaded world");
                        World world = (World) value;

                        String activeResource = (String) field(wizards, "activeMapResource");
                        require(resource.equals(activeResource),
                                "source archive was rejected or generated-arena fallback was used (active="
                                        + activeResource + ")");

                        @SuppressWarnings("unchecked")
                        List<Location> starts = (List<Location>) field(wizards, "sourceSpawnLocations");
                        @SuppressWarnings("unchecked")
                        List<Location> chests = (List<Location>) field(wizards, "chests");
                        require(starts != null && starts.size() >= 4,
                                "only " + (starts == null ? 0 : starts.size()) + " terrain-safe starts were recovered");
                        require(chests != null && chests.size() >= 8,
                                "only " + (chests == null ? 0 : chests.size()) + " usable chest locations were recovered");

                        validateStarts(world, starts);
                        validateChests(world, chests);
                        double borderSize = world.getWorldBorder().getSize();
                        require(borderSize >= 32.0D && borderSize <= 8192.0D,
                                "invalid or implausible world border size: " + borderSize);
                        require("arcadeport_source_wizards".equals(world.getName()),
                                "source map loaded into unexpected world " + world.getName());

                        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                        record("PASS", resource, starts.size(), chests.size(), elapsedMs, "loaded");
                        getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|PASS|" + resource
                                + "|starts=" + starts.size() + "|chests=" + chests.size()
                                + "|border=" + Math.round(borderSize) + "|ms=" + elapsedMs);
                    } catch (Throwable failure) {
                        Throwable cause = rootCause(failure);
                        String message = cause.getClass().getSimpleName() + ": " + safeMessage(cause);
                        record("FAIL", resource, 0, 0, (System.nanoTime() - started) / 1_000_000L, message);
                        getLogger().severe("WIZARDS_SOURCE_MAP_SMOKE|FAIL|" + resource + "|" + message);
                    }
                }
            }

            long passed = results.stream().filter(line -> line.startsWith("PASS\t")).count();
            long failed = results.stream().filter(line -> line.startsWith("FAIL\t")).count();
            getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|SUMMARY|passed=" + passed
                    + "|failed=" + failed + "|total=" + total);
            require(passed == total && failed == 0,
                    "one or more source maps failed before player-lifecycle acceptance");
            beginGameplaySmoke();
        } catch (Throwable failure) {
            failAndShutdown("HARNESS_FAIL", "harness", failure);
        }
    }

    private void beginGameplaySmoke() {
        arcade.getConfig().set("selected-maps.wizards", GAMEPLAY_MAP);
        arcade.saveConfig();
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|WAITING|two offline protocol clients ("
                + OWNER_NAME + " and WizardSmoke2)");
        gameplayTask = Bukkit.getScheduler().runTaskTimer(this, this::tickGameplaySmoke, 20L, 5L);
    }

    private void tickGameplaySmoke() {
        if (shuttingDown) return;
        gameplayTicks++;
        try {
            switch (gameplayStage) {
                case 0 -> waitForClientsAndQueue();
                case 1 -> verifyLiveRoundAndRequestLeave();
                case 2 -> verifyPlayerRestorationAndFinish();
                default -> failAndShutdown("GAMEPLAY_FAIL", "lifecycle",
                        new IllegalStateException("unknown gameplay smoke stage " + gameplayStage));
            }
        } catch (Throwable failure) {
            failAndShutdown("GAMEPLAY_FAIL", "lifecycle", rootCause(failure));
        }
    }

    private void waitForClientsAndQueue() {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        owner = online.stream().filter(player -> OWNER_NAME.equalsIgnoreCase(player.getName())).findFirst().orElse(null);
        rival = online.stream().filter(player -> !player.equals(owner)).findFirst().orElse(null);
        if (owner == null || rival == null) {
            if (gameplayTicks >= MAX_CLIENT_WAIT_TICKS) {
                throw new IllegalStateException("timed out waiting for two real clients; online="
                        + online.stream().map(Player::getName).toList());
            }
            return;
        }

        // Settle the shared Arcade session before recording the lobby baseline;
        // Wizards performs this same preflight before it snapshots players at round start.
        method(arcade.getClass(), "prepareForWizards").invoke(arcade);
        ownerBaseline = captureBaseline(owner);
        rivalBaseline = captureBaseline(rival);
        require(GAMEPLAY_MAP.equals(arcade.getConfig().getString("selected-maps.wizards")),
                "gameplay map selection was not persisted");
        require(Bukkit.dispatchCommand(owner, "arcadeport join wizards"),
                "owner could not join the Wizards queue");
        require(Bukkit.dispatchCommand(rival, "arcadeport join wizards"),
                "second client could not join the Wizards queue");
        require(Bukkit.dispatchCommand(owner, "arcadeport kit wizards mage"),
                "Mage kit selection command was rejected");
        require(Bukkit.dispatchCommand(rival, "arcadeport kit wizards sorcerer"),
                "Sorcerer kit selection command was rejected");

        @SuppressWarnings("unchecked")
        Map<Object, Object> queue = (Map<Object, Object>) field(wizards, "queue");
        require(queue.size() == 2, "expected two queued clients, found " + queue.size());
        require(Bukkit.dispatchCommand(owner, "arcadeport start wizards"),
                "owner could not force-start the Wizards round");
        gameplayStage = 1;
        gameplayTicks = 0;
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|QUEUED|players=" + owner.getName() + "," + rival.getName()
                + "|map=" + GAMEPLAY_MAP + "|kits=MAGE,SORCERER");
    }

    private void verifyLiveRoundAndRequestLeave() throws Exception {
        boolean running = (Boolean) field(wizards, "running");
        if (!running) {
            if (gameplayTicks >= MAX_START_WAIT_TICKS) {
                throw new IllegalStateException("Wizards did not start after both clients queued");
            }
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        @SuppressWarnings("unchecked")
        Map<Object, Object> snapshots = (Map<Object, Object>) field(wizards, "snapshots");
        @SuppressWarnings("unchecked")
        List<Location> chests = (List<Location>) field(wizards, "chests");
        gameplayArena = (World) field(wizards, "arena");
        String activeResource = (String) field(wizards, "activeMapResource");
        require(GAMEPLAY_MAP.equals(activeResource),
                "actual match fell back from selected map (active=" + activeResource + ")");
        require(gameplayArena != null && "arcadeport_source_wizards".equals(gameplayArena.getName()),
                "actual Wizards round is not using the selected source world");
        require(states.size() == 2, "expected two active wizard states, found " + states.size());
        require(snapshots.size() == 2, "player snapshots were not captured for both clients");
        require(chests.size() >= 8, "live round has fewer than eight loot chests");

        validateLivePlayer(owner, states, "MAGE");
        validateLivePlayer(rival, states, "SORCERER");
        validateLootRefilled(chests);

        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|ROUND_STARTED|players=2|map=" + GAMEPLAY_MAP
                + "|chests=" + chests.size() + "|snapshotCount=" + snapshots.size());
        require(Bukkit.dispatchCommand(rival, "arcadeport leave wizards"),
                "second client could not leave the live Wizards round");
        gameplayStage = 2;
        gameplayTicks = 0;
    }

    private void verifyPlayerRestorationAndFinish() throws Exception {
        boolean running = (Boolean) field(wizards, "running");
        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        @SuppressWarnings("unchecked")
        Map<Object, Object> snapshots = (Map<Object, Object>) field(wizards, "snapshots");
        @SuppressWarnings("unchecked")
        Map<Object, Object> queue = (Map<Object, Object>) field(wizards, "queue");

        if (running || !states.isEmpty() || !snapshots.isEmpty() || !queue.isEmpty()) {
            if (gameplayTicks >= 100) {
                throw new IllegalStateException("round/player state was not fully restored after a player left"
                        + " (running=" + running + ", states=" + states.size()
                        + ", snapshots=" + snapshots.size() + ", queue=" + queue.size() + ")");
            }
            return;
        }

        require(owner.isOnline() && rival.isOnline(), "a player disconnected during leave/restore acceptance");
        require(!owner.getWorld().equals(gameplayArena), "remaining player was not restored out of the match world");
        require(!rival.getWorld().equals(gameplayArena), "leaving player was not restored out of the match world");
        validateRestoredPlayer(owner, ownerBaseline);
        validateRestoredPlayer(rival, rivalBaseline);

        record("GAMEPLAY_PASS", GAMEPLAY_MAP, 2, 0, 0,
                "two real clients; Mage/Sorcerer; live start and chest refill; leave triggered winner cleanup; both snapshots restored");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|PASS|players=2|kits=MAGE,SORCERER"
                + "|roundStarted=true|lootRefilled=true|leaveCleanup=true|snapshotsRestored=true");
        long mapPasses = results.stream().filter(line -> line.startsWith("PASS\t")).count();
        getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|FINAL|mapsPassed=" + mapPasses
                + "|gameplay=PASS|twoClientRound=PASS");
        shutdownServer();
    }

    private PlayerBaseline captureBaseline(Player player) {
        return new PlayerBaseline(player.getWorld(), player.getLocation().clone(),
                player.getInventory().getContents().clone(), player.getGameMode(),
                player.getScoreboard(), player.getHealth(), player.getFoodLevel(), player.getSaturation());
    }

    private void validateRestoredPlayer(Player player, PlayerBaseline baseline) {
        require(baseline != null, "missing original lobby baseline for " + player.getName());
        require(player.getWorld().equals(baseline.world()), player.getName() + " was not restored to the original world");
        require(player.getLocation().distanceSquared(baseline.location()) <= 1.0D,
                player.getName() + " was not restored to the original lobby location");
        require(player.getGameMode() == baseline.gameMode(),
                player.getName() + " game mode was not restored");
        require(player.getScoreboard() == baseline.scoreboard(),
                player.getName() + " scoreboard was not restored");
        require(Math.abs(player.getHealth() - baseline.health()) < 0.01D,
                player.getName() + " health was not restored");
        require(player.getFoodLevel() == baseline.foodLevel()
                        && Math.abs(player.getSaturation() - baseline.saturation()) < 0.01F,
                player.getName() + " food/saturation was not restored");

        ItemStack[] actual = player.getInventory().getContents();
        ItemStack[] expected = baseline.inventory();
        require(actual.length == expected.length, player.getName() + " inventory size changed after restoration");
        for (int slot = 0; slot < expected.length; slot++) {
            ItemStack before = expected[slot];
            ItemStack after = actual[slot];
            if (before == null || before.getType().isAir()) {
                require(after == null || after.getType().isAir(),
                        player.getName() + " inventory slot " + slot + " was not restored to empty");
            } else {
                require(after != null && before.isSimilar(after) && before.getAmount() == after.getAmount(),
                        player.getName() + " inventory slot " + slot + " was not restored");
            }
        }
    }

    private record PlayerBaseline(
            World world, Location location, ItemStack[] inventory, GameMode gameMode,
            Scoreboard scoreboard, double health, int foodLevel, float saturation) {}

    private void validateLivePlayer(Player player, Map<Object, Object> states, String expectedKit) throws Exception {
        require(player != null && player.isOnline(), "a queued test client is no longer online");
        require(player.getWorld().equals(gameplayArena), player.getName() + " was not teleported into source arena");
        require(gameplayArena.getWorldBorder().isInside(player.getLocation()),
                player.getName() + " spawned outside the source-map border");
        Object state = states.get(player.getUniqueId());
        require(state != null, "missing live WizardState for " + player.getName());
        require(Boolean.TRUE.equals(field(state, "alive")), player.getName() + " is not marked alive");
        require(expectedKit.equals(String.valueOf(field(state, "kit"))),
                player.getName() + " has kit " + field(state, "kit") + ", expected " + expectedKit);
        ItemStack wand = player.getInventory().getItem(0);
        require(wand != null && !wand.getType().isAir(), player.getName() + " did not receive their first wand");
        ItemStack menu = player.getInventory().getItem(8);
        require(menu != null && !menu.getType().isAir(), player.getName() + " did not receive the spell menu item");
        require(player.getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null,
                player.getName() + " did not receive the Wizards match scoreboard");
    }

    private void validateLootRefilled(List<Location> chests) {
        int nonEmpty = 0;
        for (Location location : chests) {
            Block block = location.getBlock();
            require(block.getType() == Material.CHEST && block.getState() instanceof Chest,
                    "live round lost a chest block entity at " + location);
            Inventory inventory = ((Chest) block.getState()).getBlockInventory();
            long occupied = java.util.Arrays.stream(inventory.getContents())
                    .filter(item -> item != null && !item.getType().isAir()).count();
            if (occupied > 0) nonEmpty++;
        }
        require(nonEmpty >= 8, "fewer than eight chests were refilled with actual Wizards loot");
    }

    private void validateStarts(World world, List<Location> starts) {
        Set<String> unique = new HashSet<>();
        for (Location start : starts) {
            require(start != null && world.equals(start.getWorld()), "spawn marker belongs to another world");
            require(world.getWorldBorder().isInside(start), "spawn is outside the configured world border: " + start);
            int x = start.getBlockX(), y = start.getBlockY(), z = start.getBlockZ();
            Block floor = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            require(floor.getType().isSolid(), "spawn has no solid floor: " + start);
            require(feet.isPassable() && head.isPassable(), "spawn has insufficient headroom: " + start);
            Material ground = floor.getType();
            require(ground != Material.MAGMA_BLOCK && ground != Material.CACTUS
                            && ground != Material.CAMPFIRE && ground != Material.SOUL_CAMPFIRE
                            && ground != Material.FIRE && ground != Material.SOUL_FIRE
                            && ground != Material.POWDER_SNOW,
                    "spawn is on hazardous terrain " + ground + ": " + start);
            require(unique.add(x + ":" + y + ":" + z), "duplicate start location: " + start);
        }
    }

    private void validateChests(World world, List<Location> chests) {
        Set<String> unique = new HashSet<>();
        for (Location location : chests) {
            require(location != null && world.equals(location.getWorld()), "chest marker belongs to another world");
            require(world.getWorldBorder().isInside(location), "chest is outside the world border: " + location);
            Block block = location.getBlock();
            require(block.getType() == Material.CHEST, "chest position is not a chest block: " + location);
            require(block.getState() instanceof Chest, "chest block entity was not recreated: " + location);
            Chest chest = (Chest) block.getState();
            require(chest.getBlockInventory().getSize() >= 27, "chest inventory is not usable: " + location);
            require(unique.add(location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ()),
                    "duplicate chest location: " + location);
        }
    }

    private void failAndShutdown(String status, String resource, Throwable failure) {
        Throwable cause = rootCause(failure);
        String message = cause.getClass().getSimpleName() + ": " + safeMessage(cause);
        getLogger().severe("WIZARDS_GAMEPLAY_SMOKE|" + status + "|" + message);
        record(status, resource, 0, 0, gameplayTicks * 50L, message);
        shutdownServer();
    }

    private void shutdownServer() {
        if (shuttingDown) return;
        shuttingDown = true;
        if (gameplayTask != null) gameplayTask.cancel();
        writeResults();
        Bukkit.shutdown();
    }

    private void record(String status, String resource, int starts, int chests, long elapsedMs, String detail) {
        String safeDetail = detail == null ? "" : detail.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        results.add(status + "\t" + resource + "\t" + starts + "\t" + chests
                + "\t" + elapsedMs + "\t" + safeDetail);
        writeResults();
    }

    private void writeResults() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            Path output = getDataFolder().toPath().resolve("results.tsv");
            Files.writeString(output, String.join("\n", results) + "\n", StandardCharsets.UTF_8);
        } catch (Exception exception) {
            getLogger().severe("Could not persist Wizards source-map smoke results: " + exception);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // Search the parent type.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, parameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Search the parent type.
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        return cause;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null ? failure.toString() : message.replace('\t', ' ').replace('\n', ' ');
    }
}
