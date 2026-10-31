package au.com.testplex.wizardssmoke;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Runs only in a disposable Paper 26.3 CI server. It calls the same private
 * arena-creation path used by Wizards without requiring a real or fake player.
 */
public final class WizardsPaperSmoke extends JavaPlugin {
    private static final List<String> POOLS = List.of("Wizards", "Nano Wizards");
    private static final List<String> MAPS = List.of(
            "haven", "wizardarena", "orbisharbor", "aspen", "hogwarts",
            "industrialwest", "alicudi", "thaumaturgecolony", "magicallypossible", "kalamit");
    private final List<String> results = new ArrayList<>();
    private boolean completed;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTask(this, this::runAll);
    }

    private void runAll() {
        try {
            Plugin arcade = Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
            require(arcade != null && arcade.isEnabled(), "ArcadeMechanicsModern is not enabled");
            Object wizards = field(arcade, "wizards");
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
                        Throwable cause = failure;
                        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
                        String message = cause.getClass().getSimpleName() + ": " + safeMessage(cause);
                        record("FAIL", resource, 0, 0, (System.nanoTime() - started) / 1_000_000L, message);
                        getLogger().severe("WIZARDS_SOURCE_MAP_SMOKE|FAIL|" + resource + "|" + message);
                    }
                }
            }

            long passed = results.stream().filter(line -> line.startsWith("PASS\t")).count();
            long failed = results.size() - passed;
            getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|SUMMARY|passed=" + passed
                    + "|failed=" + failed + "|total=" + total);
        } catch (Throwable failure) {
            record("HARNESS_FAIL", "harness", 0, 0, 0,
                    failure.getClass().getSimpleName() + ": " + safeMessage(failure));
            getLogger().severe("WIZARDS_SOURCE_MAP_SMOKE|HARNESS_FAIL|" + failure);
        } finally {
            writeResults();
            completed = true;
            Bukkit.shutdown();
        }
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null ? failure.toString() : message.replace('\t', ' ').replace('\n', ' ');
    }
}
