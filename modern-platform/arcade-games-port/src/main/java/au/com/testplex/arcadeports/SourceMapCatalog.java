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
import java.util.jar.JarFile;

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
            case MILK_THE_COW -> "maps/milk-the-cow.zip";
            case DRAW_MY_THING -> "source-maps/Draw My Thing/Draw_Stadium.zip";
            default -> null;
        };
    }

    /** Supported map directories in the legacy 1.8 archive collection. */
    static List<String> mapFoldersFor(ArcadePortGame game) {
        if (game == null) return List.of();
        return switch (game) {
            case BACON_BRAWL -> List.of("Bacon Brawl");
            case CASTLE_ASSAULT -> List.of("Castle Assault");
            case BOMB_LOBBERS -> List.of("Bomb Lobbers");
            case DEATH_TAG -> List.of("Death Tag");
            case DRAGON_ESCAPE -> List.of("Dragon Escape");
            case DRAGONS -> List.of("Dragons");
            case EVOLUTION -> List.of();
            case GLADIATORS -> List.of("Gladiators");
            case HEROES_OF_GWEN -> List.of("Heroes of GWEN");
            case MICRO_BATTLES -> List.of("Micro Battle");
            case ONE_IN_THE_QUIVER -> List.of("One in the Quiver");
            case PAINTBALL -> List.of("Paintball", "Super Paintball");
            case RUNNER -> List.of("Runner");
            case SHEEP_QUEST -> List.of("Sheep Quest");
            case SPLEEF -> List.of("Speef", "Super Spleef");
            case TURF_WARS -> List.of("Turf Wars");
            case DRAW_MY_THING -> List.of("Draw My Thing");
            case MASTER_BUILDERS -> List.of();
            case SKY_WARS -> List.of("Skywars");
            case MILK_THE_COW -> List.of("Milk the Cow");
            case WITHER_ASSAULT -> List.of();
            default -> List.of();
        };
    }

    /**
     * Locate every legacy map ZIP bundled inside the plugin JAR. Maven packages the
     * corresponding update/maps directories under source-maps/, so adding a map to
     * the repository makes it available without another hand-written Java entry.
     */
    private static List<String> bundledMapResources(JavaPlugin plugin, ArcadePortGame game) {
        return bundledMapResources(plugin, mapFoldersFor(game), game == null ? "Arcade" : game.displayName());
    }

    private static List<String> bundledMapResources(JavaPlugin plugin, List<String> mapFolders, String label) {
        List<String> prefixes = mapFolders == null ? List.of() : mapFolders.stream()
                .filter(folder -> folder != null && !folder.isBlank())
                .map(folder -> "source-maps/" + folder + "/")
                .toList();
        if (prefixes.isEmpty()) return List.of();
        List<String> resources = new ArrayList<>();
        try {
            java.net.URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            Path location = Path.of(codeSource.toURI());
            if (Files.isDirectory(location)) {
                Path root = location.resolve("source-maps");
                if (Files.isDirectory(root)) {
                    try (Stream<Path> paths = Files.walk(root)) {
                        paths.filter(Files::isRegularFile)
                                .map(root::relativize)
                                .map(Path::toString)
                                .map(value -> "source-maps/" + value.replace('\\', '/'))
                                .filter(value -> prefixes.stream().anyMatch(value::startsWith))
                                .filter(value -> value.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                                .forEach(resources::add);
                    }
                }
            } else if (Files.isRegularFile(location)) {
                try (JarFile jar = new JarFile(location.toFile())) {
                    jar.stream().map(java.util.jar.JarEntry::getName)
                            .filter(value -> prefixes.stream().anyMatch(value::startsWith))
                            .filter(value -> value.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                            .forEach(resources::add);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not enumerate bundled " + label
                    + " maps: " + exception.getMessage());
        }
        return resources.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    /**
     * Also discover map archives left beside the server checkout. This is useful
     * for the Wizards/Nano Wizards source pool, whose original ZIPs may already
     * exist on a Reborn server even when they are intentionally not committed to
     * the application repository or copied into the modern plugin JAR.
     */
    private static List<String> availableMapResources(JavaPlugin plugin, List<String> mapFolders, String label) {
        List<String> resources = new ArrayList<>(bundledMapResources(plugin, mapFolders, label));
        for (Path root : externalMapRoots(plugin)) {
            resources.addAll(externalMapResources(root, mapFolders));
        }
        return resources.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static List<Path> externalMapRoots(JavaPlugin plugin) {
        if (plugin == null) return List.of();
        java.util.LinkedHashSet<Path> roots = new java.util.LinkedHashSet<>();
        Path workingDirectory = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        String configured = plugin.getConfig().getString("source-maps.directory", "update/maps");
        if (configured != null && !configured.isBlank()) {
            try {
                Path configuredPath = Path.of(configured);
                addMapRoot(roots, configuredPath.isAbsolute() ? configuredPath : workingDirectory.resolve(configuredPath));
            } catch (java.nio.file.InvalidPathException exception) {
                plugin.getLogger().warning("Ignoring invalid source-maps.directory setting: " + configured);
            }
        }

        Path worldContainer = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path worldParent = worldContainer.getParent();
        if (worldParent != null) {
            addMapRoot(roots, worldParent.resolve("update/maps"));
            addMapRoot(roots, worldParent.resolve("../update/maps"));
        }

        Path pluginData = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path pluginsDirectory = pluginData.getParent();
        if (pluginsDirectory != null && pluginsDirectory.getParent() != null) {
            addMapRoot(roots, pluginsDirectory.getParent().resolve("update/maps"));
        }
        return List.copyOf(roots);
    }

    private static void addMapRoot(java.util.Set<Path> roots, Path candidate) {
        if (candidate == null) return;
        Path normalized = candidate.toAbsolutePath().normalize();
        if (!Files.isSymbolicLink(normalized)) roots.add(normalized);
    }

    /** List safe ZIP files in just the requested legacy map folders. */
    static List<String> externalMapResources(Path mapRoot, List<String> mapFolders) {
        if (mapRoot == null || mapFolders == null || mapFolders.isEmpty()) return List.of();
        Path root = mapRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root)) return List.of();
        List<String> resources = new ArrayList<>();
        for (String folder : mapFolders) {
            if (folder == null || folder.isBlank() || folder.contains("..") || folder.indexOf('\\') >= 0) continue;
            Path directory = root.resolve(folder).normalize();
            if (!directory.startsWith(root) || directory.equals(root)
                    || Files.isSymbolicLink(directory) || !Files.isDirectory(directory)) continue;
            try (Stream<Path> paths = Files.walk(directory, 8)) {
                paths.filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> !hasSymbolicLinkBetween(root, path))
                        .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                        .map(path -> "source-maps/" + root.relativize(path).toString().replace('\\', '/'))
                        .filter(resource -> resourcePathBelongsToFolders(mapFolders, resource))
                        .forEach(resources::add);
            } catch (IOException exception) {
                // An inaccessible optional source directory must not prevent bundled maps from working.
            }
        }
        return resources.stream().distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static boolean hasSymbolicLinkBetween(Path root, Path candidate) {
        if (root == null || candidate == null) return true;
        Path safeRoot = root.toAbsolutePath().normalize();
        Path safeCandidate = candidate.toAbsolutePath().normalize();
        if (!safeCandidate.startsWith(safeRoot) || safeCandidate.equals(safeRoot)) return true;
        Path current = safeRoot;
        for (Path segment : safeRoot.relativize(safeCandidate)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) return true;
        }
        return false;
    }

    /** Resolve one external ZIP while refusing traversal and symbolic-link escapes. */
    static Path resolveExternalArchivePath(Path mapRoot, String resource) {
        if (mapRoot == null || resource == null || resource.isBlank()
                || !resource.startsWith("source-maps/") || resource.contains("..")
                || resource.indexOf('\\') >= 0
                || !resource.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) return null;
        Path root = mapRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root)) return null;
        Path relative;
        try {
            relative = Path.of(resource.substring("source-maps/".length()));
        } catch (java.nio.file.InvalidPathException exception) {
            return null;
        }
        if (relative.isAbsolute()) return null;
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root) || candidate.equals(root)
                || hasSymbolicLinkBetween(root, candidate)
                || !Files.isRegularFile(candidate, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return null;
        return candidate;
    }

    private static InputStream openMapArchive(JavaPlugin plugin, String resource) throws IOException {
        if (plugin == null || resource == null || resource.isBlank()) return null;
        InputStream bundled = plugin.getResource(resource);
        if (bundled != null) return bundled;
        for (Path root : externalMapRoots(plugin)) {
            Path external = resolveExternalArchivePath(root, resource);
            if (external != null) return Files.newInputStream(external);
        }
        return null;
    }

    private static String mapNameFromResource(String resource) {
        if (resource == null) return "Unknown Map";
        String name = resource.substring(resource.lastIndexOf('/') + 1);
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        String[] prefixes = {
                "BaconBrawl_", "CastleAssault_", "HOG_", "Lobbers_", "DeathTag_", "DragonEscape_", "Dragons_",
                "DR_", "Gladiators_", "Micro_", "OITQ_", "Quiver_", "Paintball_",
                "Runner_", "Spleef_", "SP_", "TF_", "Draw_", "Wizards_", "NanoWizards_", "NW_"
        };
        for (String prefix : prefixes) {
            if (name.regionMatches(true, 0, prefix, 0, prefix.length())) {
                name = name.substring(prefix.length());
                break;
            }
        }
        return name.replace('_', ' ').trim();
    }

    public record MapArchive(String resource, String displayName) {}

    /** Discover all bundled source archives from a named set of legacy map folders. */
    public static List<MapArchive> mapsForFolders(JavaPlugin plugin, String selectionKey, List<String> mapFolders) {
        if (plugin == null || selectionKey == null || selectionKey.isBlank()) return List.of();
        String selected = selectedResourceForFolders(plugin, selectionKey, mapFolders);
        List<MapArchive> maps = new ArrayList<>();
        for (String resource : availableMapResources(plugin, mapFolders, selectionKey)) {
            maps.add(new MapArchive(resource, displayName(plugin, resource)));
        }
        maps.sort((left, right) -> {
            if (left.resource().equals(selected)) return right.resource().equals(selected) ? 0 : -1;
            if (right.resource().equals(selected)) return 1;
            return left.displayName().compareToIgnoreCase(right.displayName());
        });
        return List.copyOf(maps);
    }

    /** Pure path validation for a named embedded source-map folder pool. */
    static boolean resourcePathBelongsToFolders(List<String> mapFolders, String resource) {
        if (resource == null || resource.isBlank() || resource.startsWith("/")
                || resource.contains("..") || resource.indexOf('\\') >= 0
                || !resource.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) return false;
        if (mapFolders == null || mapFolders.isEmpty()) return false;
        return mapFolders.stream().filter(folder -> folder != null && !folder.isBlank())
                .map(folder -> "source-maps/" + folder + "/")
                .anyMatch(resource::startsWith);
    }

    /** Resolve the configured selection against bundled or server-local source folders. */
    public static String selectedResourceForFolders(JavaPlugin plugin, String selectionKey, List<String> mapFolders) {
        if (plugin == null || selectionKey == null || selectionKey.isBlank()) return null;
        List<String> resources = availableMapResources(plugin, mapFolders, selectionKey);
        String configured = plugin.getConfig().getString("selected-maps." + selectionKey);
        if (configured != null && resourcePathBelongsToFolders(mapFolders, configured)
                && resources.contains(configured) && archiveAvailable(plugin, configured)) return configured;
        return resources.isEmpty() ? null : resources.get(0);
    }

    /** Return the selected map resource, preserving an existing legacy selection on upgrade. */
    public static String selectedResource(JavaPlugin plugin, ArcadePortGame game) {
        if (game == null) return null;
        String configured = plugin.getConfig().getString("selected-maps." + game.id());
        String legacy = resourceFor(game);
        String legacyName = displayName(plugin, legacy);
        List<String> resources = bundledMapResources(plugin, game);

        // Older builds persisted the single hand-bundled ZIP as the selected map.
        // Translate that setting to the matching archive from update/maps when it
        // exists, so upgrades expose and actually run the full legacy map asset.
        if (configured != null && configured.equals(legacy)) {
            for (String resource : resources) {
                if (mapNameFromResource(resource).equalsIgnoreCase(legacyName)) return resource;
            }
            if (plugin.getResource(configured) != null) return configured;
        }
        if (isResourceForGame(plugin, game, configured)) return configured;
        for (String resource : resources) {
            if (mapNameFromResource(resource).equalsIgnoreCase(legacyName)) return resource;
        }
        if (legacy != null && plugin.getResource(legacy) != null) return legacy;
        return resources.isEmpty() ? null : resources.get(0);
    }

    /**
     * True only when the configured resource exists in this plugin and belongs to
     * the selected game's supported archive folders. The old maps/<game>.zip
     * resources remain valid for backwards-compatible configurations.
     */
    public static boolean isInstalledResource(JavaPlugin plugin, ArcadePortGame game, String resource) {
        return plugin != null && resourcePathBelongsToGame(game, resource)
                && plugin.getResource(resource) != null;
    }

    /** Pure path validation, kept separate so the game's resource mapping is unit-testable. */
    static boolean resourcePathBelongsToGame(ArcadePortGame game, String resource) {
        if (game == null || resource == null || resource.isBlank()
                || resource.startsWith("/") || resource.contains("..") || resource.indexOf('\\') >= 0) {
            return false;
        }
        if (resource.equals(resourceFor(game))) return true;
        return mapFoldersFor(game).stream()
                .map(folder -> "source-maps/" + folder + "/")
                .anyMatch(resource::startsWith)
                && resource.toLowerCase(java.util.Locale.ROOT).endsWith(".zip");
    }

    private static boolean isResourceForGame(JavaPlugin plugin, ArcadePortGame game, String resource) {
        return isInstalledResource(plugin, game, resource);
    }

    /** All usable maps for a game, ordered with the currently selected map first. */
    public static List<MapArchive> mapsFor(JavaPlugin plugin, ArcadePortGame game) {
        if (game == null) return List.of();
        List<MapArchive> maps = new ArrayList<>();
        for (String resource : bundledMapResources(plugin, game)) {
            maps.add(new MapArchive(resource, mapNameFromResource(resource)));
        }

        String legacy = resourceFor(game);
        if (legacy != null && plugin.getResource(legacy) != null) {
            String legacyName = displayName(plugin, legacy);
            boolean duplicate = maps.stream().anyMatch(map -> map.displayName().equalsIgnoreCase(legacyName));
            if (!duplicate) maps.add(new MapArchive(legacy, legacyName));
        }

        String selected = selectedResource(plugin, game);
        maps.sort((left, right) -> {
            if (left.resource().equals(selected)) return right.resource().equals(selected) ? 0 : -1;
            if (right.resource().equals(selected)) return 1;
            return left.displayName().compareToIgnoreCase(right.displayName());
        });
        return List.copyOf(maps);
    }

    /** Read map metadata directly from an embedded archive, without loading a world. */
    public static String displayName(JavaPlugin plugin, ArcadePortGame game) {
        return displayName(plugin, selectedResource(plugin, game));
    }

    public static String displayName(JavaPlugin plugin, String resource) {
        if (resource == null) return null;
        try (InputStream raw = openMapArchive(plugin, resource)) {
            if (raw == null) return mapNameFromResource(resource);
            try (ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith("WorldConfig.dat")) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    zip.transferTo(bytes);
                    for (String line : bytes.toString(StandardCharsets.UTF_8).split("\\R")) {
                        if (line.trim().toUpperCase(java.util.Locale.ROOT).startsWith("MAP_NAME:")) {
                            String name = line.substring(line.indexOf(':') + 1).trim();
                            if (!name.isEmpty()) return name;
                        }
                    }
                    }
                    zip.closeEntry();
                }
            }
        } catch (IOException exception) {
            if (plugin != null) plugin.getLogger().warning("Could not read map name from " + resource + ": " + exception.getMessage());
        }
        return mapNameFromResource(resource);
    }

    public static boolean hasMap(ArcadePortGame game) {
        return game != null && !mapFoldersFor(game).isEmpty();
    }

    public static boolean isReservedSourceWorldName(String worldName) {
        return worldName != null && worldName.startsWith("arcadeport_source_")
                && worldName.indexOf('/') < 0 && worldName.indexOf('\\') < 0 && !worldName.contains("..");
    }

    /**
     * Paper 26.3 migrates legacy CraftBukkit world regions from
     * <world-container>/<name>/region into
     * <primary-world>/dimensions/minecraft/<name>/region. Reset both the
     * legacy extraction and that migrated destination before reinstalling an
     * archived 1.8 map, otherwise Paper refuses to overwrite the old region
     * files on the next createWorld() call.
     *
     * This method intentionally deletes only the exact reserved source-map
     * directory and its exact Paper migration counterpart; it never clears the
     * primary world or the shared dimensions directory.
     */
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

        try {
            // Paper's legacy-world migration target is relative to the primary
            // server world (the live error explicitly reports ./world/...). Do
            // not assume the first normal Bukkit World is that folder: plugins
            // may create their own NORMAL environments before the next reset.
            List<Path> migratedRoots = findMigratedDimensionFolders(container, worldName);
            deleteTreeIfPresent(root, container, worldName);
            for (Path migratedRoot : migratedRoots) {
                deleteTreeIfPresent(migratedRoot, container, worldName);
                plugin.getLogger().info("Removed Paper-migrated source-map folder " + migratedRoot);
            }
            plugin.getLogger().info("Reset source test map " + worldName
                    + " including " + migratedRoots.size() + " discovered Paper 26.3 migrated dimension folder(s).");
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Could not fully reset source test world " + worldName + ": " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Find the exact migrated folder under each plausible server-world root.
     * Includes Paper's observed default ./world path, loaded world's actual
     * folders, and direct child world directories. Candidate matching remains
     * confined to dimensions/minecraft/<reserved source-world name>; unrelated
     * world data is never a deletion target.
     */
    private static List<Path> findMigratedDimensionFolders(Path container, String worldName) throws IOException {
        if (container == null || !isReservedSourceWorldName(worldName)) return List.of();
        Path safeContainer = container.toAbsolutePath().normalize();
        java.util.Set<Path> worldRoots = new java.util.LinkedHashSet<>();
        worldRoots.add(safeContainer.resolve("world").normalize());

        for (World world : Bukkit.getWorlds()) {
            if (world == null || isReservedSourceWorldName(world.getName())) continue;
            Path candidate = world.getWorldFolder().toPath().toAbsolutePath().normalize();
            if (candidate.startsWith(safeContainer) && !candidate.equals(safeContainer)
                    && !Files.isSymbolicLink(candidate)) worldRoots.add(candidate);
        }

        if (Files.isDirectory(safeContainer)) {
            try (Stream<Path> children = Files.list(safeContainer)) {
                children.filter(Files::isDirectory)
                        .filter(candidate -> !Files.isSymbolicLink(candidate))
                        .map(candidate -> candidate.toAbsolutePath().normalize())
                        .filter(candidate -> candidate.startsWith(safeContainer) && !candidate.equals(safeContainer))
                        .filter(candidate -> !isReservedSourceWorldName(candidate.getFileName().toString()))
                        .forEach(worldRoots::add);
            }
        }

        List<Path> migratedRoots = new ArrayList<>();
        for (Path worldRoot : worldRoots) {
            if (worldRoot == null || !worldRoot.startsWith(safeContainer) || worldRoot.equals(safeContainer)
                    || Files.isSymbolicLink(worldRoot)
                    || isReservedSourceWorldName(worldRoot.getFileName().toString())) continue;
            Path migrated = migratedDimensionFolder(worldRoot, worldName);
            if (migrated != null && migrated.startsWith(safeContainer)
                    && !migrated.equals(safeContainer) && Files.exists(migrated)
                    && !Files.isSymbolicLink(migrated)) migratedRoots.add(migrated);
        }
        return migratedRoots.stream().distinct().toList();
    }

    /** Returns the isolated Paper-migrated folder for a valid source-map name. */
    static Path migratedDimensionFolder(Path primaryWorldFolder, String worldName) {
        if (primaryWorldFolder == null || !isReservedSourceWorldName(worldName)) return null;
        Path primaryRoot = primaryWorldFolder.toAbsolutePath().normalize();
        if (isReservedSourceWorldName(primaryRoot.getFileName().toString())) return null;
        Path migrated = primaryRoot.resolve("dimensions").resolve("minecraft").resolve(worldName).normalize();
        if (!migrated.startsWith(primaryRoot) || migrated.equals(primaryRoot)) return null;
        return migrated;
    }

    private static void deleteTreeIfPresent(Path root, Path container, String worldName) throws IOException {
        if (root == null || container == null || !isReservedSourceWorldName(worldName)) {
            throw new IOException("Refusing to delete an unvalidated source-map path.");
        }
        Path safeContainer = container.toAbsolutePath().normalize();
        Path safeRoot = root.toAbsolutePath().normalize();
        Path legacyRoot = safeContainer.resolve(worldName).normalize();
        boolean legacySourceRoot = safeRoot.equals(legacyRoot);

        boolean migratedDimensionRoot = false;
        Path minecraft = safeRoot.getParent();
        Path dimensions = minecraft == null ? null : minecraft.getParent();
        Path primaryRoot = dimensions == null ? null : dimensions.getParent();
        if (safeRoot.getFileName() != null
                && worldName.equals(safeRoot.getFileName().toString())
                && minecraft != null && "minecraft".equals(minecraft.getFileName().toString())
                && dimensions != null && "dimensions".equals(dimensions.getFileName().toString())
                && primaryRoot != null && primaryRoot.startsWith(safeContainer)
                && !primaryRoot.equals(safeContainer) && primaryRoot.getFileName() != null
                && !isReservedSourceWorldName(primaryRoot.getFileName().toString())) {
            migratedDimensionRoot = true;
        }
        if ((!legacySourceRoot && !migratedDimensionRoot)
                || !safeRoot.startsWith(safeContainer) || safeRoot.equals(safeContainer)) {
            throw new IOException("Refusing to delete non-reserved world path: " + safeRoot);
        }

        if (!Files.exists(safeRoot)) return;
        try (Stream<Path> paths = Files.walk(safeRoot)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    /**
     * Resolve an archive's actual world root. Some archived maps are zipped
     * with a single enclosing folder; Bukkit needs level.dat and WorldConfig.dat
     * directly inside the world directory, not one directory deeper.
     *
     * Returns null when there is no unambiguous directory containing both files.
     */
    static Path locateMapRoot(Path extractedRoot) throws IOException {
        if (extractedRoot == null || !Files.isDirectory(extractedRoot)) return null;
        Path root = extractedRoot.toAbsolutePath().normalize();
        if (hasWorldMetadata(root)) return root;

        List<Path> candidates;
        try (Stream<Path> paths = Files.walk(root, 6)) {
            candidates = paths.filter(Files::isDirectory)
                    .filter(path -> !path.equals(root))
                    .filter(SourceMapCatalog::hasWorldMetadata)
                    .sorted(Comparator.comparingInt(Path::getNameCount))
                    .toList();
        }
        if (candidates.isEmpty()) return null;
        int shallowest = candidates.get(0).getNameCount();
        List<Path> nearest = candidates.stream()
                .filter(path -> path.getNameCount() == shallowest)
                .toList();
        return nearest.size() == 1 ? nearest.get(0) : null;
    }

    /**
     * Convert a legacy Reborn/CraftBukkit map archive to the directory layout Paper
     * 26.1+ expects when importing a custom world. In this layout Bukkit imports
     * the source world's Overworld from dimensions/minecraft/overworld and moves it
     * under the primary world's dimensions/minecraft/<world-name> directory.
     *
     * This is a structural move only: the authored level.dat, region files and
     * WorldConfig.dat contents are not rewritten. Never merge or overwrite existing
     * region data; an ambiguous/partial installation must fail closed for a clean retry.
     */
    static void preparePaper26WorldStorage(Path worldRoot) throws IOException {
        if (worldRoot == null) throw new IOException("Source world root is null.");
        Path root = worldRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root)) {
            throw new IOException("Source world root is missing or is a symbolic link: " + root);
        }

        Path dimensions = root.resolve("dimensions").resolve("minecraft").resolve("overworld").normalize();
        if (!dimensions.startsWith(root) || dimensions.equals(root)) {
            throw new IOException("Unsafe Paper 26.3 Overworld path for source map: " + dimensions);
        }

        for (String directory : List.of("region", "entities", "poi")) {
            Path legacy = root.resolve(directory).normalize();
            Path modern = dimensions.resolve(directory).normalize();
            if (!legacy.startsWith(root) || !modern.startsWith(dimensions)) {
                throw new IOException("Unsafe source map data path: " + directory);
            }
            if (Files.isSymbolicLink(legacy) || Files.isSymbolicLink(modern)) {
                throw new IOException("Refusing symbolic links in source-map data: " + directory);
            }
            boolean legacyExists = Files.exists(legacy);
            boolean modernExists = Files.exists(modern);
            if (legacyExists && !Files.isDirectory(legacy)) {
                throw new IOException("Legacy " + directory + " path is not a directory: " + legacy);
            }
            if (modernExists && !Files.isDirectory(modern)) {
                throw new IOException("Paper 26.3 " + directory + " path is not a directory: " + modern);
            }
            if (legacyExists && modernExists) {
                throw new IOException("Both legacy and Paper 26.3 " + directory
                        + " directories exist; refusing to merge or overwrite: " + legacy + " and " + modern);
            }
            if (legacyExists) {
                Files.createDirectories(modern.getParent());
                Files.move(legacy, modern);
            }
        }

        Path region = dimensions.resolve("region");
        if (!Files.isDirectory(region)) {
            throw new IOException("Archived map has no region directory at " + region
                    + " (expected legacy region/ to be moved into Paper 26.3 dimension layout).");
        }
        try (Stream<Path> files = Files.list(region)) {
            if (files.noneMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mca"))) {
                throw new IOException("No .mca terrain region files found in " + region);
            }
        }
    }

    private static boolean hasWorldMetadata(Path folder) {
        return folder != null
                && Files.isRegularFile(folder.resolve("level.dat"))
                && Files.isRegularFile(folder.resolve("WorldConfig.dat"));
    }

    private static void deleteExtractionTree(Path target, Path container) throws IOException {
        Path safeContainer = container.toAbsolutePath().normalize();
        Path safeTarget = target.toAbsolutePath().normalize();
        if (!safeTarget.startsWith(safeContainer) || safeTarget.equals(safeContainer)
                || Files.isSymbolicLink(safeTarget)) {
            throw new IOException("Refusing to remove an unsafe map extraction path: " + safeTarget);
        }
        if (!Files.exists(safeTarget)) return;
        try (Stream<Path> paths = Files.walk(safeTarget)) {
            for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }

    /** Extract a game's selected source archive into its dedicated, isolated test world. */
    public static boolean install(JavaPlugin plugin, ArcadePortGame game, String worldName) {
        return installResource(plugin, selectedResource(plugin, game), worldName, mapFoldersFor(game));
    }

    private static boolean archiveAvailable(JavaPlugin plugin, String resource) {
        try (InputStream input = openMapArchive(plugin, resource)) {
            return input != null;
        } catch (IOException exception) {
            return false;
        }
    }

    /** Extract a source archive only when it belongs to the caller's expected folder pool. */
    public static boolean installResource(JavaPlugin plugin, String resource, String worldName, List<String> mapFolders) {
        if (!resourcePathBelongsToFolders(mapFolders, resource)) {
            if (plugin != null) plugin.getLogger().severe("Refusing a source map outside the selected folder pool: " + resource);
            return false;
        }
        return installResource(plugin, resource, worldName);
    }

    /** Extract a selected archive discovered from any validated source-map folder pool. */
    public static boolean installResource(JavaPlugin plugin, String resource, String worldName) {
        if (plugin == null || resource == null || resource.isBlank()) {
            if (plugin != null) plugin.getLogger().warning("Selected source map resource is missing: " + resource);
            return false;
        }
        if (!resource.startsWith("source-maps/") || !resource.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                || resource.contains("..") || resource.indexOf('\\') >= 0) {
            plugin.getLogger().severe("Refusing to install an invalid embedded source map resource: " + resource);
            return false;
        }
        if (!isReservedSourceWorldName(worldName)) {
            plugin.getLogger().severe("Refusing to install a source map into an unsafe world name: " + worldName);
            return false;
        }
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path root = container.resolve(worldName).normalize();
        Path staging = container.resolve(worldName + ".installing").normalize();
        if (!root.startsWith(container) || root.equals(container)
                || !staging.startsWith(container) || staging.equals(container)) {
            throw new IllegalArgumentException("Map installation path escapes world container.");
        }
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(staging)) {
            plugin.getLogger().severe("Refusing to follow a symbolic link while installing source map " + worldName);
            return false;
        }
        if (hasWorldMetadata(root)) {
            try {
                preparePaper26WorldStorage(root);
                return true;
            } catch (IOException exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Existing source map " + worldName + " has incompatible/incomplete Paper 26.3 "
                                + "dimension storage: " + exception.getMessage(), exception);
                return false;
            }
        }

        try {
            deleteExtractionTree(staging, container);
            Files.createDirectories(staging);
            try (InputStream input = openMapArchive(plugin, resource)) {
                if (input == null) {
                    plugin.getLogger().warning("Missing bundled or server-local original map archive: " + resource);
                    return false;
                }
                try (ZipInputStream zip = new ZipInputStream(input)) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        Path output = staging.resolve(entry.getName()).normalize();
                        if (!output.startsWith(staging)) {
                            throw new IOException("Unsafe archive entry: " + entry.getName());
                        }
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
            }

            Path archiveRoot = locateMapRoot(staging);
            if (archiveRoot == null) {
                String entries;
                try (Stream<Path> paths = Files.list(staging)) {
                    entries = paths.map(path -> path.getFileName().toString()).sorted().limit(20)
                            .collect(java.util.stream.Collectors.joining(", "));
                }
                plugin.getLogger().warning("Selected map archive " + resource
                        + " did not contain an unambiguous level.dat and WorldConfig.dat world root. "
                        + "Extraction root entries: [" + entries + "].");
                return false;
            }

            // Replace only this reserved test-map directory. The caller unloads
            // the world and clears Paper's migrated counterpart before reinstalling.
            deleteExtractionTree(root, container);
            if (archiveRoot.equals(staging)) {
                Files.move(staging, root);
            } else {
                Files.move(archiveRoot, root);
            }

            // Minecraft/Paper 26.1+ expects a custom world's Overworld chunks in
            // <world>/dimensions/minecraft/overworld/region before Bukkit imports
            // that archived CraftBukkit world into the server world's dimensions.
            // The 1.8 Reborn ZIPs have the pre-26.1 layout (<world>/region). If we
            // leave that old layout in place, Paper's LegacyCraftBukkitWorldMigration
            // fails while creating the world; retrying the same ZIP cannot help.
            preparePaper26WorldStorage(root);

            if (!hasWorldMetadata(root)) {
                plugin.getLogger().warning("Original map archive " + resource
                        + " was extracted but its normalized world root is missing required metadata at " + root);
                return false;
            }
            plugin.getLogger().info("Installed authentic map archive " + resource
                    + " into " + root + " (metadata root normalized from " + archiveRoot + ").");
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Could not install source map archive " + resource
                            + " into " + root + ": " + exception.getMessage(), exception);
            return false;
        } finally {
            if (Files.exists(staging) && !Files.isSymbolicLink(staging)) {
                try {
                    deleteExtractionTree(staging, container);
                } catch (IOException cleanupFailure) {
                    plugin.getLogger().warning("Could not clean temporary map extraction " + staging
                            + ": " + cleanupFailure.getMessage());
                }
            }
        }
    }

    /**
     * Paper 26.3 can expose a migrated custom world's dimension folder from
     * World#getWorldFolder(), while map-specific legacy metadata such as
     * WorldConfig.dat remains alongside the original archive extraction.
     * Prefer the world folder, then check only the reserved source world's
     * original extraction directory inside the server world container.
     */
    static Path resolveWorldConfigPath(Path worldFolder, Path worldContainer, String worldName) {
        Path local = worldFolder == null ? null : worldFolder.toAbsolutePath().normalize().resolve("WorldConfig.dat");
        if (local != null && Files.isRegularFile(local)) return local;
        if (worldContainer == null || !isReservedSourceWorldName(worldName)) return local;
        Path container = worldContainer.toAbsolutePath().normalize();
        Path legacyRoot = container.resolve(worldName).normalize();
        if (!legacyRoot.startsWith(container) || legacyRoot.equals(container)) return local;
        Path legacyConfig = legacyRoot.resolve("WorldConfig.dat");
        return Files.isRegularFile(legacyConfig) ? legacyConfig : local;
    }

    /**
     * Reads the map-specific Reborn metadata. Paper 26.1+ imports an API-created
     * CraftBukkit world into the main world's dimensions folder and deletes the
     * old standalone world directory. Paper migrates region/entities/poi, but it
     * does not know about the extra Reborn WorldConfig.dat file. Therefore, after
     * a successful terrain import, this metadata must be read from the exact
     * selected archive instead of assuming the old file survived on disk.
     */
    public static MapData read(JavaPlugin plugin, ArcadePortGame game, World world) {
        if (plugin == null || game == null || world == null) return null;
        Path config = resolveWorldConfigPath(world.getWorldFolder().toPath(),
                Bukkit.getWorldContainer().toPath(), world.getName());
        if (config != null && Files.isRegularFile(config)) {
            try {
                return parseMapConfig(world, Files.readAllLines(config, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Could not read on-disk WorldConfig.dat for " + game.displayName()
                                + "; retrying from the selected embedded source archive " + selectedResource(plugin, game),
                        exception);
            }
        }

        String resource = selectedResource(plugin, game);
        if (resource == null) {
            plugin.getLogger().warning("Cannot read source map metadata for " + game.displayName()
                    + ": no selected archive is configured.");
            return null;
        }
        try (InputStream input = openMapArchive(plugin, resource)) {
            if (input == null) {
                plugin.getLogger().warning("Cannot read source map metadata for " + game.displayName()
                        + ": the selected bundled or server-local archive is missing: " + resource);
                return null;
            }
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().replace('\\', '/').endsWith("/WorldConfig.dat")
                            || entry.getName().equals("WorldConfig.dat")) {
                        String contents = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                        MapData parsed = parseMapConfig(world, java.util.Arrays.asList(contents.split("\\R", -1)));
                        if (parsed != null) {
                            plugin.getLogger().info("Loaded " + game.displayName()
                                    + " WorldConfig.dat from the selected embedded archive " + resource
                                    + " (Paper's custom-world migration does not retain this Reborn-only file).");
                        }
                        return parsed;
                    }
                    zip.closeEntry();
                }
            }
            plugin.getLogger().warning("Selected map archive " + resource + " has no WorldConfig.dat entry.");
            return null;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Could not read WorldConfig.dat from selected map archive " + resource, exception);
            return null;
        }
    }

    /**
     * Read a selected source archive's WorldConfig.dat. Paper's custom-world
     * migration does not preserve this Reborn-only metadata beside the imported
     * terrain, so archive lookup is the authoritative fallback after load.
     */
    public static MapData readResource(JavaPlugin plugin, String resource, World world) {
        if (plugin == null || resource == null || world == null) return null;
        Path config = resolveWorldConfigPath(world.getWorldFolder().toPath(),
                Bukkit.getWorldContainer().toPath(), world.getName());
        if (config != null && Files.isRegularFile(config)) {
            try {
                return parseMapConfig(world, Files.readAllLines(config, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Could not read on-disk WorldConfig.dat for " + world.getName()
                                + "; retrying from selected embedded source archive " + resource,
                        exception);
            }
        }
        try (InputStream input = openMapArchive(plugin, resource)) {
            if (input == null) {
                plugin.getLogger().warning("Cannot read source map metadata: selected bundled or server-local archive is missing: " + resource);
                return null;
            }
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (name.equals("WorldConfig.dat") || name.endsWith("/WorldConfig.dat")) {
                        String contents = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                        MapData parsed = parseMapConfig(world, java.util.Arrays.asList(contents.split("\\R", -1)));
                        if (parsed != null) {
                            plugin.getLogger().info("Loaded " + world.getName()
                                    + " WorldConfig.dat from selected source archive " + resource + ".");
                        }
                        return parsed;
                    }
                    zip.closeEntry();
                }
            }
            plugin.getLogger().warning("Selected map archive " + resource + " has no WorldConfig.dat entry.");
            return null;
        } catch (IOException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Could not read WorldConfig.dat from source archive " + resource, exception);
            return null;
        }
    }

    /** Read an already-extracted WorldConfig.dat or its UTF-8 contents into source map data. */
    public static MapData read(World world) {
        if (world == null) return null;
        Path config = resolveWorldConfigPath(world.getWorldFolder().toPath(),
                Bukkit.getWorldContainer().toPath(), world.getName());
        if (config == null || !Files.isRegularFile(config)) return null;
        try {
            return parseMapConfig(world, Files.readAllLines(config, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            return null;
        }
    }

    static MapData parseMapConfig(World world, List<String> lines) {
        if (lines == null) return null;
        MapData data = new MapData();
        String currentTeam = "Unassigned";
        String currentDataName = "Unassigned";
        String currentCustomName = "Unassigned";
        for (String line : lines) {
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

        /** Get source TEAM_SPAWNS locations for a named WorldConfig team, such as Bacon Brawl's Green starts. */
        public List<Location> teamSpawnLocationsForName(String teamName) {
            if (teamName == null) return new ArrayList<>();
            List<Location> matches = new ArrayList<>();
            for (Map.Entry<String, List<Location>> entry : teamSpawns.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(teamName)) {
                    for (Location location : entry.getValue()) matches.add(location.clone());
                }
            }
            return matches;
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
                    double dx = marker.getX() - last.getX();
                    double dy = marker.getY() - last.getY();
                    double dz = marker.getZ() - last.getZ();
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
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
        /** All authored custom marker groups, copied so match runtimes cannot mutate map metadata. */
        public Map<String, List<Location>> customLocationGroups() {
            Map<String, List<Location>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, List<Location>> entry : customLocations.entrySet()) copy.put(entry.getKey(), copyLocations(entry.getValue()));
            return Map.copyOf(copy);
        }
        private List<Location> copyLocations(List<Location> values) {
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
        public Map<String, List<Location>> teamSpawns() { return Map.copyOf(teamSpawns); }
    }
}
