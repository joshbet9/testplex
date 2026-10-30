package au.com.testplex.snake;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Loads the original Reborn Snake maps into a dedicated Paper 26.3 world.
 * Only the reserved snake_arena extraction and its Paper-migrated dimension
 * are ever removed; the server's primary world is never a deletion target.
 */
public final class SnakeMapCatalog {
    public static final String WORLD_NAME = "snake_arena";
    private static final String RESOURCE_PREFIX = "source-maps/Snake/";
    private static final long MAX_EXTRACTED_BYTES = 2_500_000_000L;

    private SnakeMapCatalog() {}

    public static List<String> maps(JavaPlugin plugin) {
        if (plugin == null) return List.of();
        List<String> resources = new ArrayList<>();
        try {
            URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            Path location = Path.of(codeSource.toURI());
            if (Files.isDirectory(location)) {
                Path root = location.resolve(RESOURCE_PREFIX);
                if (Files.isDirectory(root)) {
                    try (Stream<Path> paths = Files.list(root)) {
                        paths.filter(Files::isRegularFile)
                                .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                                .forEach(path -> resources.add(RESOURCE_PREFIX + path.getFileName()));
                    }
                }
            } else if (Files.isRegularFile(location)) {
                try (JarFile jar = new JarFile(location.toFile())) {
                    jar.stream().map(java.util.jar.JarEntry::getName)
                            .filter(name -> name.startsWith(RESOURCE_PREFIX))
                            .filter(name -> name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                            .forEach(resources::add);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not enumerate original Snake map archives: " + exception.getMessage());
        }
        return resources.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public static String selectedResource(JavaPlugin plugin) {
        List<String> resources = maps(plugin);
        if (resources.isEmpty()) return null;
        String configured = plugin.getConfig().getString("selected-map", "Snake_Pit.zip");
        if (configured != null && !configured.isBlank()) {
            for (String resource : resources) {
                String file = resource.substring(resource.lastIndexOf('/') + 1);
                String display = displayName(resource);
                if (configured.equalsIgnoreCase(resource)
                        || configured.equalsIgnoreCase(file)
                        || configured.equalsIgnoreCase(display)
                        || configured.equalsIgnoreCase(display.replace(' ', '_'))) {
                    return resource;
                }
            }
        }
        return resources.get(0);
    }

    public static String resolveResource(JavaPlugin plugin, String requested) {
        if (plugin == null || requested == null || requested.isBlank()) return null;
        String normalized = requested.trim();
        for (String resource : maps(plugin)) {
            String file = resource.substring(resource.lastIndexOf('/') + 1);
            if (normalized.equalsIgnoreCase(resource)
                    || normalized.equalsIgnoreCase(file)
                    || normalized.equalsIgnoreCase(displayName(resource))
                    || normalized.equalsIgnoreCase(displayName(resource).replace(' ', '_'))) {
                return resource;
            }
        }
        return null;
    }

    public static String displayName(String resource) {
        if (resource == null) return "Unknown";
        String file = resource.substring(resource.lastIndexOf('/') + 1);
        if (file.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            file = file.substring(0, file.length() - 4);
        }
        return file.replace('_', ' ');
    }

    public static boolean install(JavaPlugin plugin, String resource) {
        if (plugin == null || !isSnakeResource(resource) || plugin.getResource(resource) == null) {
            plugin.getLogger().severe("Refusing to install an unbundled or non-Snake map resource: " + resource);
            return false;
        }
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path root = container.resolve(WORLD_NAME).normalize();
        Path staging = container.resolve(WORLD_NAME + ".installing").normalize();
        if (!safeChild(container, root) || !safeChild(container, staging)) return false;

        World loaded = Bukkit.getWorld(WORLD_NAME);
        if (loaded != null && !Bukkit.unloadWorld(loaded, false)) {
            plugin.getLogger().severe("Could not unload the previous Snake map; refusing to replace it.");
            return false;
        }

        try {
            resetReservedWorld(container, root, staging);
            Files.createDirectories(staging);
            try (InputStream input = plugin.getResource(resource);
                 ZipInputStream zip = new ZipInputStream(input)) {
                if (input == null) throw new IOException("Missing embedded map archive " + resource);
                ZipEntry entry;
                long extracted = 0L;
                while ((entry = zip.getNextEntry()) != null) {
                    Path output = staging.resolve(entry.getName()).normalize();
                    if (!output.startsWith(staging) || output.equals(staging)) {
                        throw new IOException("Unsafe path in Snake map archive: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(output);
                    } else {
                        Path parent = output.getParent();
                        if (parent != null) Files.createDirectories(parent);
                        try (OutputStream stream = Files.newOutputStream(output)) {
                            byte[] buffer = new byte[64 * 1024];
                            int count;
                            while ((count = zip.read(buffer)) != -1) {
                                extracted += count;
                                if (extracted > MAX_EXTRACTED_BYTES) {
                                    throw new IOException("Snake map archive exceeds the extraction safety limit.");
                                }
                                stream.write(buffer, 0, count);
                            }
                        }
                    }
                    zip.closeEntry();
                }
            }

            Path archiveRoot = locateMapRoot(staging);
            if (archiveRoot == null) {
                throw new IOException("Archive does not contain a single world root with level.dat and WorldConfig.dat.");
            }
            if (Files.exists(root)) deleteTree(root, container, WORLD_NAME, false);
            if (archiveRoot.equals(staging)) Files.move(staging, root);
            else Files.move(archiveRoot, root);
            preparePaper26WorldStorage(root);
            if (!Files.isRegularFile(root.resolve("level.dat"))
                    || !Files.isRegularFile(root.resolve("WorldConfig.dat"))) {
                throw new IOException("Installed Snake map is missing required world metadata.");
            }
            plugin.getLogger().info("Installed original Snake map " + displayName(resource) + " for Paper 26.3.");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Could not install original Snake map " + resource + ": " + exception.getMessage(), exception);
            return false;
        } finally {
            try {
                if (Files.exists(staging) && !Files.isSymbolicLink(staging)) {
                    deleteTree(staging, container, WORLD_NAME + ".installing", false);
                }
            } catch (IOException cleanup) {
                plugin.getLogger().warning("Could not clean Snake map staging directory: " + cleanup.getMessage());
            }
        }
    }

    private static boolean isSnakeResource(String resource) {
        return resource != null && resource.startsWith(RESOURCE_PREFIX)
                && !resource.contains("..") && !resource.contains("\\")
                && resource.toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
    }

    private static boolean safeChild(Path container, Path candidate) {
        return candidate.startsWith(container) && !candidate.equals(container);
    }

    private static void resetReservedWorld(Path container, Path root, Path staging) throws IOException {
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(staging)) {
            throw new IOException("Refusing to follow a symbolic link for the reserved Snake world.");
        }
        if (Files.exists(root)) deleteTree(root, container, WORLD_NAME, false);
        if (Files.exists(staging)) deleteTree(staging, container, WORLD_NAME + ".installing", false);

        java.util.Set<Path> primaryWorlds = new java.util.LinkedHashSet<>();
        primaryWorlds.add(container.resolve("world").normalize());
        for (World world : Bukkit.getWorlds()) {
            if (world == null || WORLD_NAME.equals(world.getName())) continue;
            Path folder = world.getWorldFolder().toPath().toAbsolutePath().normalize();
            if (folder.startsWith(container) && !Files.isSymbolicLink(folder)) primaryWorlds.add(folder);
        }
        if (Files.isDirectory(container)) {
            try (Stream<Path> children = Files.list(container)) {
                children.filter(Files::isDirectory)
                        .filter(path -> !Files.isSymbolicLink(path))
                        .map(path -> path.toAbsolutePath().normalize())
                        .filter(path -> path.startsWith(container) && !path.equals(container))
                        .filter(path -> !WORLD_NAME.equals(path.getFileName().toString()))
                        .forEach(primaryWorlds::add);
            }
        }
        for (Path primary : primaryWorlds) {
            if (primary == null || !primary.startsWith(container) || primary.equals(container)
                    || Files.isSymbolicLink(primary) || WORLD_NAME.equals(primary.getFileName().toString())) continue;
            Path migrated = primary.resolve("dimensions").resolve("minecraft").resolve(WORLD_NAME).normalize();
            if (migrated.startsWith(primary) && !migrated.equals(primary)
                    && Files.exists(migrated) && !Files.isSymbolicLink(migrated)) {
                deleteTree(migrated, primary, WORLD_NAME, true);
            }
        }
    }

    private static void deleteTree(Path target, Path allowedRoot, String expectedName,
                                   boolean migratedDimension) throws IOException {
        Path safeTarget = target.toAbsolutePath().normalize();
        Path safeRoot = allowedRoot.toAbsolutePath().normalize();
        if (!safeTarget.startsWith(safeRoot) || safeTarget.equals(safeRoot)
                || safeTarget.getFileName() == null
                || !expectedName.equals(safeTarget.getFileName().toString())
                || Files.isSymbolicLink(safeTarget)) {
            throw new IOException("Refusing to remove an unsafe Snake map path: " + safeTarget);
        }
        if (migratedDimension) {
            Path minecraft = safeTarget.getParent();
            Path dimensions = minecraft == null ? null : minecraft.getParent();
            if (minecraft == null || dimensions == null
                    || !"minecraft".equals(minecraft.getFileName().toString())
                    || !"dimensions".equals(dimensions.getFileName().toString())) {
                throw new IOException("Refusing to remove a path that is not the exact Paper Snake dimension.");
            }
        } else if (!safeTarget.getParent().equals(safeRoot)) {
            throw new IOException("Refusing to remove a non-root Snake map extraction.");
        }
        if (!Files.exists(safeTarget)) return;
        try (Stream<Path> paths = Files.walk(safeTarget)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isSymbolicLink(item)) throw new IOException("Symbolic link found in Snake map path: " + item);
                Files.deleteIfExists(item);
            }
        }
    }

    private static Path locateMapRoot(Path extractedRoot) throws IOException {
        if (hasWorldMetadata(extractedRoot)) return extractedRoot;
        List<Path> candidates;
        try (Stream<Path> paths = Files.walk(extractedRoot, 6)) {
            candidates = paths.filter(Files::isDirectory)
                    .filter(SnakeMapCatalog::hasWorldMetadata)
                    .sorted(Comparator.comparingInt(Path::getNameCount))
                    .toList();
        }
        if (candidates.isEmpty()) return null;
        int depth = candidates.get(0).getNameCount();
        List<Path> shallowest = candidates.stream().filter(path -> path.getNameCount() == depth).toList();
        return shallowest.size() == 1 ? shallowest.get(0) : null;
    }

    private static boolean hasWorldMetadata(Path path) {
        return path != null && Files.isRegularFile(path.resolve("level.dat"))
                && Files.isRegularFile(path.resolve("WorldConfig.dat"));
    }

    /** Paper 26.1+ stores imported custom-world chunks in dimensions/minecraft/overworld. */
    private static void preparePaper26WorldStorage(Path worldRoot) throws IOException {
        Path root = worldRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root)) {
            throw new IOException("Snake source-world root is missing or a symbolic link.");
        }
        Path overworld = root.resolve("dimensions").resolve("minecraft").resolve("overworld").normalize();
        if (!overworld.startsWith(root)) throw new IOException("Unsafe Paper Overworld storage path.");
        for (String directory : List.of("region", "entities", "poi")) {
            Path legacy = root.resolve(directory).normalize();
            Path modern = overworld.resolve(directory).normalize();
            if (!legacy.startsWith(root) || !modern.startsWith(overworld)
                    || Files.isSymbolicLink(legacy) || Files.isSymbolicLink(modern)) {
                throw new IOException("Unsafe Snake map data path: " + directory);
            }
            boolean oldExists = Files.exists(legacy);
            boolean newExists = Files.exists(modern);
            if (oldExists && !Files.isDirectory(legacy)) throw new IOException("Invalid legacy " + directory + " path.");
            if (newExists && !Files.isDirectory(modern)) throw new IOException("Invalid Paper " + directory + " path.");
            if (oldExists && newExists) throw new IOException("Both legacy and Paper " + directory + " folders exist.");
            if (oldExists) {
                Files.createDirectories(modern.getParent());
                Files.move(legacy, modern);
            }
        }
        Path region = overworld.resolve("region");
        if (!Files.isDirectory(region)) throw new IOException("Snake map has no Paper Overworld region directory.");
        try (Stream<Path> files = Files.list(region)) {
            if (files.noneMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mca"))) {
                throw new IOException("Snake map has no terrain region files.");
            }
        }
    }

    public static MapData readMapData(JavaPlugin plugin, World world, String resource) {
        if (plugin == null || world == null || !isSnakeResource(resource)) return MapData.empty(world);
        try (InputStream raw = plugin.getResource(resource)) {
            if (raw == null) return MapData.empty(world);
            try (ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().replace('\\', '/').endsWith("/WorldConfig.dat")
                            || "WorldConfig.dat".equals(entry.getName())) {
                        return parseMapConfig(world, new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    zip.closeEntry();
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read Snake WorldConfig.dat from " + resource + ": " + exception.getMessage());
        }
        return MapData.empty(world);
    }

    static MapData parseMapConfig(World world, String content) {
        MapData data = new MapData(world);
        if (content == null) return data;
        String currentTeam = "Unassigned";
        String currentDataName = "Unassigned";
        String currentCustomName = "Unassigned";
        for (String line : content.split("\\R")) {
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
                case "MIN_Y" -> { data.minY = parseInt(value, data.minY); data.hasMinY = true; }
                case "MAX_Y" -> { data.maxY = parseInt(value, data.maxY); data.hasMaxY = true; }
                case "TEAM_NAME" -> currentTeam = value.isEmpty() ? "Unassigned" : value;
                case "TEAM_SPAWNS" -> data.addLocations(data.teamSpawns, currentTeam, value, true);
                case "DATA_NAME" -> currentDataName = value.isEmpty() ? "Unassigned" : value;
                case "DATA_LOCS" -> data.addLocations(data.dataLocations, currentDataName, value, false);
                case "CUSTOM_NAME" -> currentCustomName = value.isEmpty() ? "Unassigned" : value;
                case "CUSTOM_LOCS" -> data.addLocations(data.customLocations, currentCustomName, value, false);
                default -> { }
            }
        }
        return data;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static final class MapData {
        private final World world;
        private String mapName = "Unknown";
        private boolean hasMinX, hasMaxX, hasMinZ, hasMaxZ, hasMinY, hasMaxY;
        private int minX = -48, maxX = 48, minZ = -48, maxZ = 48, minY = 0, maxY = 128;
        private final Map<String, List<Location>> teamSpawns = new LinkedHashMap<>();
        private final Map<String, List<Location>> dataLocations = new LinkedHashMap<>();
        private final Map<String, List<Location>> customLocations = new LinkedHashMap<>();

        private MapData(World world) { this.world = world; }
        static MapData empty(World world) { return new MapData(world); }

        private void addLocations(Map<String, List<Location>> target, String name,
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
                } catch (NumberFormatException ignored) { }
            }
        }

        public String mapName() { return mapName; }
        public List<Location> spawnLocations() {
            List<Location> result = new ArrayList<>();
            teamSpawns.values().forEach(locations -> locations.forEach(location -> result.add(location.clone())));
            return result;
        }
        public Location foodSpawnOrigin() {
            List<Location> spawns = spawnLocations();
            return spawns.isEmpty() ? center() : spawns.get(0);
        }
        public Location spectatorLocation() {
            for (Map.Entry<String, List<Location>> entry : customLocations.entrySet()) {
                String key = entry.getKey();
                if ((key.contains("SPECTATOR") || "SPEC".equals(key) || key.contains("LOBBY"))
                        && !entry.getValue().isEmpty()) return entry.getValue().get(0).clone();
            }
            for (Map.Entry<String, List<Location>> entry : dataLocations.entrySet()) {
                if ((entry.getKey().contains("SPECTATOR") || "SPEC".equals(entry.getKey()))
                        && !entry.getValue().isEmpty()) return entry.getValue().get(0).clone();
            }
            return center();
        }
        public Location center() {
            if (hasMinX && hasMaxX && hasMinZ && hasMaxZ) {
                return new Location(world, (minX + maxX) / 2.0D, world == null ? 64 : world.getSpawnLocation().getY(),
                        (minZ + maxZ) / 2.0D);
            }
            List<Location> spawns = spawnLocations();
            if (!spawns.isEmpty()) {
                double x = 0.0D, z = 0.0D;
                for (Location location : spawns) { x += location.getX(); z += location.getZ(); }
                return new Location(world, x / spawns.size(), spawns.get(0).getY(), z / spawns.size());
            }
            return world == null ? null : world.getSpawnLocation();
        }
        public double clampX(double value, double margin) {
            if (hasMinX && hasMaxX) return Math.max(minX + margin, Math.min(maxX - margin, value));
            Location center = center();
            return center == null ? value : Math.max(center.getX() - 42.0D, Math.min(center.getX() + 42.0D, value));
        }
        public double clampZ(double value, double margin) {
            if (hasMinZ && hasMaxZ) return Math.max(minZ + margin, Math.min(maxZ - margin, value));
            Location center = center();
            return center == null ? value : Math.max(center.getZ() - 42.0D, Math.min(center.getZ() + 42.0D, value));
        }
        public int minY() { return hasMinY ? minY : 1; }
        public int maxY() { return hasMaxY ? maxY : (world == null ? 128 : world.getMaxHeight() - 5); }
        public boolean hasSpawnLocations() { return !teamSpawns.values().stream().allMatch(List::isEmpty); }
    }
}
