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
                    case "MIN_Y" -> { data.minY = parseInt(value, data.minY); data.hasMinY = true; }
                    case "MAX_Y" -> { data.maxY = parseInt(value, data.maxY); data.hasMaxY = true; }
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
        private boolean hasMinX, hasMaxX, hasMinZ, hasMaxZ, hasMinY, hasMaxY;
        private int minX = -40, maxX = 40, minZ = -40, maxZ = 40, minY = 0, maxY = 128;
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
        public boolean hasVerticalBounds() { return hasMinY && hasMaxY && maxY >= minY; }
        public int minX() { return minX; }
        public int maxX() { return maxX; }
        public int minZ() { return minZ; }
        public int maxZ() { return maxZ; }
        public int minY() { return minY; }
        public int maxY() { return maxY; }
        public double centerX() { return (minX + maxX) / 2.0D; }
        public double centerZ() { return (minZ + maxZ) / 2.0D; }
        public double width() { return Math.max(1, maxX - minX); }
        public double depth() { return Math.max(1, maxZ - minZ); }
        /**
         * Return source spawns interleaved across configured teams, so small lobbies
         * use each team's first spawn instead of filling only the first team's roster.
         */
        public List<Location> spawnLocations() {
            List<Map.Entry<String, List<Location>>> groups = new ArrayList<>();
            List<Integer> sizes = new ArrayList<>();
            for (Map.Entry<String, List<Location>> entry : teamSpawns.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                groups.add(entry);
                sizes.add(entry.getValue().size());
            }
            List<Location> result = new ArrayList<>();
            int[] offsets = new int[groups.size()];
            for (int groupIndex : roundRobinGroupOrder(sizes)) {
                result.add(groups.get(groupIndex).getValue().get(offsets[groupIndex]++).clone());
            }
            return result;
        }

        /**
         * List the configured group index for each spawn in round-robin order.
         * For group sizes [3, 2, 1], this returns [0, 1, 2, 0, 1, 0].
         */
        public static List<Integer> roundRobinGroupOrder(List<Integer> groupSizes) {
            List<Integer> result = new ArrayList<>();
            if (groupSizes == null || groupSizes.isEmpty()) return result;
            int maxSize = 0;
            for (Integer size : groupSizes) if (size != null) maxSize = Math.max(maxSize, Math.max(0, size));
            for (int slot = 0; slot < maxSize; slot++) {
                for (int group = 0; group < groupSizes.size(); group++) {
                    Integer size = groupSizes.get(group);
                    if (size != null && size > slot) result.add(group);
                }
            }
            return result;
        }

        /** Resolve an interleaved source spawn to its source team. */
        public int teamIndexForSpawn(int spawnIndex, int teamCount) {
            int teams = Math.max(1, teamCount);
            List<Map.Entry<String, List<Location>>> groups = nonEmptyTeamGroups();
            List<Integer> sizes = new ArrayList<>();
            int total = 0;
            for (Map.Entry<String, List<Location>> group : groups) {
                sizes.add(group.getValue().size());
                total += group.getValue().size();
            }
            if (groups.size() <= 1 || total == 0) return Math.floorMod(spawnIndex, teams);
            List<Integer> order = roundRobinGroupOrder(sizes);
            int groupIndex = order.get(Math.floorMod(spawnIndex, order.size()));
            return teamIndexForGroup(groups.get(groupIndex).getKey(), groupIndex, teams);
        }

        /** Get the source map spawn markers belonging to one indexed team. */
        public List<Location> teamSpawnLocationsForTeam(int teamIndex, int teamCount) {
            int teams = Math.max(1, teamCount);
            int targetTeam = Math.floorMod(teamIndex, teams);
            List<Map.Entry<String, List<Location>>> groups = nonEmptyTeamGroups();
            List<Location> result = new ArrayList<>();
            if (groups.size() == 1) {
                List<Location> locations = groups.get(0).getValue();
                for (int i = 0; i < locations.size(); i++) {
                    if (Math.floorMod(i, teams) == targetTeam) result.add(locations.get(i).clone());
                }
                return result;
            }
            for (int i = 0; i < groups.size(); i++) {
                if (teamIndexForGroup(groups.get(i).getKey(), i, teams) == targetTeam) {
                    for (Location location : groups.get(i).getValue()) result.add(location.clone());
                }
            }
            return result;
        }

        /** Resolve familiar source group names while preserving source order as a fallback. */
        public static int teamIndexForGroup(String teamName, int groupIndex, int teamCount) {
            int teams = Math.max(1, teamCount);
            String label = teamName == null ? "" : teamName.toLowerCase(java.util.Locale.ROOT);
            if (label.contains("red")) return Math.floorMod(0, teams);
            if (label.contains("blue") || label.contains("aqua")) return Math.floorMod(1, teams);
            if (label.contains("green") || label.contains("lime")) return Math.floorMod(2, teams);
            if (label.contains("yellow")) return Math.floorMod(3, teams);
            if (label.contains("purple")) return Math.floorMod(4, teams);
            return Math.floorMod(groupIndex, teams);
        }

        private List<Map.Entry<String, List<Location>>> nonEmptyTeamGroups() {
            List<Map.Entry<String, List<Location>>> groups = new ArrayList<>();
            for (Map.Entry<String, List<Location>> entry : teamSpawns.entrySet()) {
                if (!entry.getValue().isEmpty()) groups.add(entry);
            }
            return groups;
        }

        /**
         * Preserve WorldConfig's authored route order and remove only consecutive duplicate markers.
         * Route points can double back near earlier parts of a course, so nearest-neighbour sorting
         * is not a safe way to reconstruct Dragon Escape switchbacks.
         */
        public static List<Location> orderedRouteMarkers(Location start, List<Location> markers,
                double duplicateDistance) {
            List<Location> result = new ArrayList<>();
            if (start == null && (markers == null || markers.isEmpty())) return result;
            Location last = start;
            if (start != null) result.add(start.clone());
            if (markers == null || markers.isEmpty()) return result;
            for (Location marker : markers) {
                if (marker == null) continue;
                if (last != null) {
                    double distance = marker.distance(last);
                    if (ArcadePortRules.isDuplicateDragonEscapeWaypoint(distance, duplicateDistance)) continue;
                }
                result.add(marker.clone());
                last = marker;
            }
            return result;
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
