package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Loads archived Reborn world maps into isolated modern Arcade test worlds. */
public final class SourceMapCatalog {
    private SourceMapCatalog() {}

    public static String resourceFor(ArcadePortGame game) {
        if (game == null) return null;
        return switch (game) {
            case BACON_BRAWL -> "maps/bacon-brawl.zip";
            case BOMB_LOBBERS -> "maps/bomb-lobbers.zip";
            case DEATH_TAG -> "maps/death-tag.zip";
            case DRAGON_ESCAPE -> "maps/dragon-escape.zip";
            case DRAGONS -> "maps/dragons.zip";
            case GLADIATORS -> "maps/gladiators.zip";
            case MICRO_BATTLES -> "maps/micro-battles.zip";
            case ONE_IN_THE_QUIVER -> "maps/one-in-the-quiver.zip";
            case PAINTBALL -> "maps/paintball.zip";
            case RUNNER -> "maps/runner.zip";
            case SHEEP_QUEST -> "maps/sheep-quest.zip";
            case SPLEEF -> "maps/spleef.zip";
            case TURF_WARS -> "maps/turf-wars.zip";
            default -> null;
        };
    }

    public static boolean hasMap(ArcadePortGame game) {
        return resourceFor(game) != null;
    }

    public static boolean isReservedSourceWorldName(String worldName) {
        return worldName != null && worldName.startsWith("arcadeport_source_")
                && worldName.indexOf('/') < 0 && worldName.indexOf('\\') < 0 && !worldName.contains("..");
    }

    /** Reset only a reserved archived-map world. */
    public static boolean resetTestWorld(JavaPlugin plugin, String worldName) {
        if (!isReservedSourceWorldName(worldName)) {
            plugin.getLogger().severe("Refusing to reset non-source test world: " + worldName);
            return false;
        }
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path root = container.resolve(worldName).normalize();
        if (!root.startsWith(container) || root.equals(container)) return false;
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null && !Bukkit.unloadWorld(loaded, false)) {
            plugin.getLogger().warning("Could not unload source test world " + worldName + "; it is still in use.");
            return false;
        }
        if (!Files.exists(root)) return true;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not reset source test world " + worldName + ": " + ex.getMessage());
            return false;
        }
    }

    /** Extract only into this module's dedicated arcadeport_source_* test world. */
    public static boolean install(JavaPlugin plugin, ArcadePortGame game, String worldName) {
        String resource = resourceFor(game);
        if (resource == null) return false;
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path root = container.resolve(worldName).normalize();
        if (!root.startsWith(container)) throw new IllegalArgumentException("World path escapes world container.");
        if (Files.isRegularFile(root.resolve("level.dat"))
                && Files.isRegularFile(root.resolve("WorldConfig.dat"))) return true;
        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) {
                plugin.getLogger().warning("Missing embedded original map resource: " + resource);
                return false;
            }
            Files.createDirectories(root);
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path output = root.resolve(entry.getName()).normalize();
                    if (!output.startsWith(root)) throw new IOException("Unsafe archive entry: " + entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(output);
                    } else {
                        Path parent = output.getParent();
                        if (parent != null) Files.createDirectories(parent);
                        try (OutputStream stream = Files.newOutputStream(output)) {
                            zip.transferTo(stream);
                        }
                    }
                    zip.closeEntry();
                }
            }
            boolean valid = Files.isRegularFile(root.resolve("level.dat"))
                    && Files.isRegularFile(root.resolve("WorldConfig.dat"));
            if (!valid) plugin.getLogger().warning("Original map archive lacks world metadata: " + resource);
            return valid;
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not extract original " + game.displayName() + " map: " + ex.getMessage());
            return false;
        }
    }

    public static MapData read(World world) {
        Path config = world.getWorldFolder().toPath().resolve("WorldConfig.dat");
        if (!Files.isRegularFile(config)) return null;
        MapData data = new MapData();
        String currentTeam = "Unassigned";
        String currentDataName = "Unassigned";
        String currentCustomName = "Unassigned";
        try {
            for (String line : Files.readAllLines(config, StandardCharsets.UTF_8)) {
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String key = line.substring(0, colon).trim().toUpperCase(java.util.Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                switch (key) {
                    case "MAP_NAME" -> data.mapName = value;
                    case "MIN_X" -> { data.minX = parseInt(value, data.minX); data.hasMinX = true; }
                    case "MAX_X" -> { data.maxX = parseInt(value, data.maxX); data.hasMaxX = true; }
                    case "MIN_Z" -> { data.minZ = parseInt(value, data.minZ); data.hasMinZ = true; }
                    case "MAX_Z" -> { data.maxZ = parseInt(value, data.maxZ); data.hasMaxZ = true; }
                    case "TEAM_NAME" -> {
                        currentTeam = value.isEmpty() ? "Unassigned" : value;
                        data.teamSpawns.computeIfAbsent(currentTeam, ignored -> new ArrayList<>());
                    }
                    case "TEAM_SPAWNS" -> data.addLocations(world, data.teamSpawns, currentTeam, value, true);
                    case "DATA_NAME" -> currentDataName = value.isEmpty() ? "Unassigned" : value;
                    case "DATA_LOCS" -> data.addLocations(world, data.dataLocations, currentDataName, value, false);
                    case "CUSTOM_NAME" -> currentCustomName = value.isEmpty() ? "Unassigned" : value;
                    case "CUSTOM_LOCS" -> data.addLocations(world, data.customLocations, currentCustomName, value, false);
                    default -> { }
                }
            }
            return data;
        } catch (IOException ex) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static final class MapData {
        private String mapName = "Unknown";
        private boolean hasMinX, hasMaxX, hasMinZ, hasMaxZ;
        private int minX = -40, maxX = 40, minZ = -40, maxZ = 40;
        private final Map<String, List<Location>> teamSpawns = new LinkedHashMap<>();
        private final Map<String, List<Location>> dataLocations = new LinkedHashMap<>();
        private final Map<String, List<Location>> customLocations = new LinkedHashMap<>();

        private void addLocations(World world, Map<String, List<Location>> target, String name,
                String encoded, boolean centerBlock) {
            List<Location> locations = target.computeIfAbsent(name.toUpperCase(java.util.Locale.ROOT),
                    ignored -> new ArrayList<>());
            for (String token : encoded.split(":")) {
                String[] coordinates = token.trim().split(",");
                if (coordinates.length < 3) continue;
                try {
                    double x = Double.parseDouble(coordinates[0].trim());
                    double y = Double.parseDouble(coordinates[1].trim());
                    double z = Double.parseDouble(coordinates[2].trim());
                    locations.add(new Location(world, x + (centerBlock ? 0.5D : 0.0D), y,
                            z + (centerBlock ? 0.5D : 0.0D)));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed marker coordinates only.
                }
            }
        }

        public String mapName() { return mapName; }
        public boolean hasBounds() { return hasMinX && hasMaxX && hasMinZ && hasMaxZ; }
        public double centerX() { return (minX + maxX) / 2.0D; }
        public double centerZ() { return (minZ + maxZ) / 2.0D; }
        public double width() { return Math.max(1, maxX - minX); }
        public double depth() { return Math.max(1, maxZ - minZ); }
        public List<Location> spawnLocations() {
            List<Location> result = new ArrayList<>();
            for (List<Location> team : teamSpawns.values()) result.addAll(team);
            return result;
        }

        /** Resolve a source spawn to its source team, preserving WorldConfig group ordering. */
        public int teamIndexForSpawn(int spawnIndex, int teamCount) {
            int teams = Math.max(1, teamCount);
            List<Map.Entry<String, List<Location>>> groups = new ArrayList<>();
            int total = 0;
            for (Map.Entry<String, List<Location>> entry : teamSpawns.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                groups.add(entry);
                total += entry.getValue().size();
            }
            if (groups.size() <= 1 || total == 0) return Math.floorMod(spawnIndex, teams);
            int position = Math.floorMod(spawnIndex, total);
            for (int i = 0; i < groups.size(); i++) {
                Map.Entry<String, List<Location>> group = groups.get(i);
                if (position < group.getValue().size()) {
                    String label = group.getKey().toLowerCase(java.util.Locale.ROOT);
                    if (label.contains("red")) return 0;
                    if (label.contains("blue")) return Math.floorMod(1, teams);
                    if (label.contains("green")) return Math.floorMod(2, teams);
                    if (label.contains("yellow")) return Math.floorMod(3, teams);
                    if (label.contains("purple")) return Math.floorMod(4, teams);
                    return Math.floorMod(i, teams);
                }
                position -= group.getValue().size();
            }
            return Math.floorMod(spawnIndex, teams);
        }

        public List<Location> dataLocations(String name) {
            return copyLocations(dataLocations.get(name == null ? "" : name.toUpperCase(java.util.Locale.ROOT)));
        }
        public List<Location> customLocations(String name) {
            return copyLocations(customLocations.get(name == null ? "" : name.toUpperCase(java.util.Locale.ROOT)));
        }
        private List<Location> copyLocations(List<Location> values) {
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
        public Map<String, List<Location>> teamSpawns() { return Map.copyOf(teamSpawns); }
    }
}
