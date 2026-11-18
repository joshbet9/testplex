package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Admin-only, isolated Dragon Escape map drafts with validated export and reversible publishing. */
final class DragonEscapeMapEditor implements Listener {
    private static final String DRAFTS_DIRECTORY = "map-editor/drafts";
    private static final String EXPORTS_DIRECTORY = "map-editor/exports";
    private static final String PUBLISHED_DIRECTORY = "map-editor/published";
    private static final String WORLD_PREFIX = "arcadeport_source_mapeditor_";

    private final JavaPlugin plugin;
    private final ArcadeMechanicsModernPlugin arcadePlugin;
    private final Map<UUID, DraftSession> sessions = new HashMap<>();
    private final Map<String, UUID> worldOwners = new HashMap<>();

    DragonEscapeMapEditor(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.arcadePlugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    boolean handle(CommandSender sender, String[] args) {
        if (!arcadePlugin.hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.RED + "Only authorised Arcade testers can use the Dragon Escape map editor.");
            return true;
        }
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> listMaps(sender);
            case "open" -> open(sender, args);
            case "current" -> openCurrentMap(sender, args);
            case "close" -> close(sender);
            case "save" -> save(sender);
            case "markers" -> markers(sender);
            case "point" -> editPoint(sender, args);
            case "bounds" -> setBounds(sender, args);
            case "validate" -> validate(sender);
            case "export" -> export(sender);
            case "publish" -> publish(sender, args);
            case "rollback" -> rollback(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Dragon Escape map drafts");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit current" + ChatColor.GRAY + " — open the map currently selected for Dragon Escape");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit list" + ChatColor.GRAY + " — list maps and their IDs");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit open <map-id> <draft>" + ChatColor.GRAY + " — create/open an isolated creative copy");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit markers" + ChatColor.GRAY + " — list authored points in this draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit point <add|remove> <DATA|TEAM|CUSTOM> <group> [index]" + ChatColor.GRAY + " — edit a point at your position");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit bounds set <minX> <maxX> <minY> <maxY> <minZ> <maxZ>" + ChatColor.GRAY + " — set map bounds");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit <save|validate|export|close>" + ChatColor.GRAY + " — save, check, export or leave the draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit publish <map-id> <draft> confirm" + ChatColor.GRAY + " — publish after validation");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit rollback <map-id> confirm" + ChatColor.GRAY + " — switch to the previous published version");
    }

    private void listMaps(CommandSender sender) {
        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE);
        if (maps.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No Dragon Escape source maps are available.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "Dragon Escape maps:");
        for (SourceMapCatalog.MapArchive map : maps) {
            sender.sendMessage(ChatColor.YELLOW + mapId(map.resource()) + ChatColor.GRAY + " — "
                    + map.displayName() + (map.inferredMetadata() ? ChatColor.RED + " (inferred metadata)" : ""));
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Map drafts must be opened by an in-game authorised tester.");
            return;
        }
        if (args.length != 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit open <map-id> <draft-name>");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            player.sendMessage(ChatColor.RED + "Close the active Arcade round before opening a map draft.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Close your current draft before opening another.");
            return;
        }
        String draftSlug = DragonEscapeMapDraftRules.draftSlug(args[2]);
        SourceMapCatalog.MapArchive archive = findMap(args[1]);
        if (draftSlug == null || archive == null) {
            player.sendMessage(ChatColor.RED + "Unknown map or invalid draft name. Use /arcadeport mapedit list; draft names use letters, numbers, _ and -.");
            return;
        }
        String worldName = DragonEscapeMapDraftRules.worldName(archive.resource(), draftSlug);
        if (worldName == null || !SourceMapCatalog.isReservedSourceWorldName(worldName)) {
            player.sendMessage(ChatColor.RED + "The draft could not be assigned a safe isolated world name.");
            return;
        }
        UUID owner = worldOwners.get(worldName);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Another tester is editing this named draft. Choose another draft name.");
            return;
        }
        Path manifestPath;
        try {
            manifestPath = draftManifestPath(draftSlug);
        } catch (IOException exception) {
            player.sendMessage(ChatColor.RED + "The draft storage path is not safe to use.");
            return;
        }
        YamlConfiguration manifest = YamlConfiguration.loadConfiguration(manifestPath.toFile());
        boolean existing = Files.isRegularFile(manifestPath, LinkOption.NOFOLLOW_LINKS);
        if (existing && (!archive.resource().equals(manifest.getString("source-resource"))
                || !worldName.equals(manifest.getString("world-name")))) {
            player.sendMessage(ChatColor.RED + "That draft name is already tied to another map.");
            return;
        }
        if (!existing) {
            Path worldFolder = Bukkit.getWorldContainer().toPath().resolve(worldName).normalize();
            if (Files.exists(worldFolder, LinkOption.NOFOLLOW_LINKS)) {
                player.sendMessage(ChatColor.RED + "An untracked map-editor world already exists for this draft; refusing to overwrite it.");
                return;
            }
            if (!SourceMapCatalog.installResource(plugin, ArcadePortGame.DRAGON_ESCAPE,
                    archive.resource(), worldName)) {
                player.sendMessage(ChatColor.RED + "Could not extract that map into an isolated draft world. Check the server log.");
                return;
            }
            manifest.set("source-resource", archive.resource());
            manifest.set("map-name", archive.displayName());
            manifest.set("draft-name", draftSlug);
            manifest.set("world-name", worldName);
            manifest.set("created-at", System.currentTimeMillis());
            try {
                saveYamlAtomically(manifest, manifestPath);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not save Dragon Escape draft manifest: " + exception.getMessage());
                player.sendMessage(ChatColor.RED + "The draft metadata could not be saved; the extracted world was left untouched.");
                return;
            }
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            try {
                world = new WorldCreator(worldName).environment(World.Environment.NORMAL).createWorld();
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Could not load Dragon Escape draft world " + worldName + ": " + exception.getMessage());
            }
        }
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Could not load the isolated draft world. The draft files were preserved.");
            return;
        }
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setTime(6000L);
        ensureLocalWorldConfig(world);
        Location previous = player.getLocation().clone();
        GameMode previousMode = player.getGameMode();
        boolean previousAllowFlight = player.getAllowFlight();
        player.teleport(new Location(world, world.getSpawnLocation().getX(),
                world.getHighestBlockYAt(world.getSpawnLocation()) + 2.0D,
                world.getSpawnLocation().getZ()));
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        DraftSession session = new DraftSession(draftSlug, archive.resource(), archive.displayName(), world,
                previous, previousMode, previousAllowFlight);
        sessions.put(player.getUniqueId(), session);
        worldOwners.put(worldName, player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Opened isolated draft " + ChatColor.YELLOW + draftSlug
                + ChatColor.GREEN + " for " + archive.displayName() + ". Source files are unchanged.");
        player.sendMessage(ChatColor.GRAY + "Build in creative, then use /arcadeport mapedit markers and validate before export or publish.");
        markers(player);
    }

    private void openCurrentMap(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Map drafts must be opened in-game.");
            return;
        }
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit current");
            return;
        }
        ArcadePortGame selectedGame = ArcadePortGame.fromId(
                plugin.getConfig().getString("lobby-selected-game", ""));
        if (selectedGame != ArcadePortGame.DRAGON_ESCAPE) {
            player.sendMessage(ChatColor.YELLOW + "Select Dragon Escape in the lobby Game Panel, then use the Map Editor in hotbar slot 1.");
            return;
        }
        String selectedResource = SourceMapCatalog.selectedResource(plugin, ArcadePortGame.DRAGON_ESCAPE);
        String mapId = DragonEscapeMapDraftRules.mapSlug(selectedResource);
        if (mapId == null || findMap(mapId) == null) {
            player.sendMessage(ChatColor.RED + "The currently selected Dragon Escape map could not be identified.");
            return;
        }
        // Reuse the per-map draft on repeated hotbar clicks while keeping every map
        // in its own isolated world and draft folder.
        open(sender, new String[] {"open", mapId, mapId});
    }

    private void close(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        DraftSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.YELLOW + "You are not editing a Dragon Escape map draft.");
            return;
        }
        session.world.save();
        worldOwners.remove(session.world.getName(), player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);
        player.teleport(session.returnLocation);
        player.setGameMode(session.returnGameMode);
        player.setAllowFlight(session.returnAllowFlight);
        if (session.returnAllowFlight && session.returnLocation.getWorld() != null) player.setFlying(false);
        player.sendMessage(ChatColor.GREEN + "Saved and closed draft " + session.draftName + ".");
    }

    private void save(CommandSender sender) {
        DraftSession session = current(sender);
        if (session == null) return;
        ensureLocalWorldConfig(session.world);
        session.world.save();
        sender.sendMessage(ChatColor.GREEN + "Saved draft " + session.draftName + " to its isolated world.");
    }

    private void markers(CommandSender sender) {
        DraftSession session = current(sender);
        if (session == null) return;
        SourceMapCatalog.MapData data = SourceMapCatalog.read(plugin, ArcadePortGame.DRAGON_ESCAPE, session.world);
        sender.sendMessage(ChatColor.GOLD + "Draft markers for " + session.mapName + ":");
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "No WorldConfig.dat exists yet. Add RED, BLACK and GREEN DATA points.");
            return;
        }
        sendGroups(sender, "DATA", data.dataLocationGroups());
        sendGroups(sender, "TEAM", data.teamSpawns());
        sendGroups(sender, "CUSTOM", data.customLocationGroups());
        sender.sendMessage(ChatColor.GRAY + "Dragon Escape requires DATA RED (start), DATA BLACK (route, at least two), and DATA GREEN (speed marker).");
    }

    private void sendGroups(CommandSender sender, String type, Map<String, List<Location>> groups) {
        groups.forEach((name, points) -> sender.sendMessage(ChatColor.YELLOW + type + " " + name
                + ChatColor.GRAY + " — " + points.size() + " point(s)"));
    }

    private void editPoint(CommandSender sender, String[] args) {
        DraftSession session = current(sender);
        if (session == null) return;
        if (args.length < 4 || args.length > 5) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit point <add|remove> <DATA|TEAM|CUSTOM> <group> [index]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        String type = args[2].toUpperCase(Locale.ROOT);
        String group = args[3];
        try {
            Path config = ensureLocalWorldConfig(session.world);
            List<String> lines = Files.exists(config) ? Files.readAllLines(config, StandardCharsets.UTF_8) : new ArrayList<>();
            if ("add".equals(action) && args.length == 4) {
                Location location = sender instanceof Player player ? player.getLocation() : null;
                if (location == null || location.getWorld() != session.world) {
                    sender.sendMessage(ChatColor.RED + "Stand at the desired point in your active draft before adding it.");
                    return;
                }
                Vector v = location.toVector();
                lines = DragonEscapeMapDraftRules.addPoint(lines, type, group, v.getX(), v.getY(), v.getZ());
            } else if ("remove".equals(action) && args.length == 5) {
                int index = Integer.parseInt(args[4]);
                lines = DragonEscapeMapDraftRules.removePoint(lines, type, group, index);
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Use point add with no index or point remove with a one-based index.");
                return;
            }
            writeLinesAtomically(lines, config);
            session.world.save();
            sender.sendMessage(ChatColor.GREEN + ("add".equals(action) ? "Added" : "Removed")
                    + " point " + type + " " + group + ".");
        } catch (IOException | IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + "Could not edit that point: " + exception.getMessage());
        }
    }

    private void setBounds(CommandSender sender, String[] args) {
        DraftSession session = current(sender);
        if (session == null) return;
        if (args.length != 8 || !"set".equalsIgnoreCase(args[1])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit bounds set <minX> <maxX> <minY> <maxY> <minZ> <maxZ>");
            return;
        }
        try {
            List<Integer> values = new ArrayList<>(6);
            for (int i = 2; i < 8; i++) values.add(Integer.parseInt(args[i]));
            Path config = ensureLocalWorldConfig(session.world);
            List<String> lines = Files.exists(config) ? Files.readAllLines(config, StandardCharsets.UTF_8) : new ArrayList<>();
            writeLinesAtomically(DragonEscapeMapDraftRules.setBounds(lines,
                    values.get(0), values.get(1), values.get(2), values.get(3), values.get(4), values.get(5)), config);
            sender.sendMessage(ChatColor.GREEN + "Updated draft bounds. Run /arcadeport mapedit validate before exporting.");
        } catch (IOException | IllegalArgumentException exception) {
            sender.sendMessage(ChatColor.RED + "Invalid bounds: " + exception.getMessage());
        }
    }

    private void validate(CommandSender sender) {
        DraftSession session = current(sender);
        if (session == null) return;
        List<String> errors = validateDraft(session);
        if (errors.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "Draft validation passed: world files, bounds and Dragon Escape route markers are usable.");
        } else {
            sender.sendMessage(ChatColor.RED + "Draft needs " + errors.size() + " correction(s):");
            errors.forEach(error -> sender.sendMessage(ChatColor.RED + " • " + error));
        }
    }

    private List<String> validateDraft(DraftSession session) {
        List<String> errors = new ArrayList<>();
        Path worldFolder = session.world.getWorldFolder().toPath();
        if (!Files.isRegularFile(worldFolder.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(worldFolder.resolve("region"), LinkOption.NOFOLLOW_LINKS)) {
            errors.add("The draft world is missing level.dat or region files.");
        }
        SourceMapCatalog.MapData data = SourceMapCatalog.read(plugin, ArcadePortGame.DRAGON_ESCAPE, session.world);
        if (data == null) {
            errors.add("WorldConfig.dat is missing or unreadable.");
            return errors;
        }
        if (!data.hasBounds() || data.maxX() <= data.minX() || data.maxZ() <= data.minZ()) {
            errors.add("Valid MIN/MAX X and Z map bounds are required.");
        }
        List<Location> starts = data.dataLocations("RED");
        List<Location> route = data.dataLocations("BLACK");
        List<Location> speed = data.dataLocations("GREEN");
        if (starts.isEmpty()) errors.add("Add a DATA RED start point.");
        if (route.size() < 2) errors.add("Add at least two DATA BLACK route points, including the finish.");
        if (speed.isEmpty()) errors.add("Add a DATA GREEN speed marker.");
        List<Location> routePoints = new ArrayList<>(starts);
        routePoints.addAll(route);
        routePoints.addAll(speed);
        for (Location point : routePoints) {
            if (point.getWorld() != session.world || point.getY() < session.world.getMinHeight()
                    || point.getY() >= session.world.getMaxHeight()) {
                errors.add("A route marker falls outside the draft world's vertical range.");
                break;
            }
            if (data.hasBounds() && (point.getBlockX() < data.minX() || point.getBlockX() > data.maxX()
                    || point.getBlockZ() < data.minZ() || point.getBlockZ() > data.maxZ())) {
                errors.add("A RED, BLACK or GREEN route marker falls outside the authored X/Z bounds.");
                break;
            }
        }
        return errors;
    }

    private void export(CommandSender sender) {
        DraftSession session = current(sender);
        if (session == null) return;
        List<String> errors = validateDraft(session);
        if (!errors.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Export blocked: fix validation errors first.");
            errors.forEach(error -> sender.sendMessage(ChatColor.RED + " • " + error));
            return;
        }
        try {
            Path target = exportDraft(session);
            sender.sendMessage(ChatColor.GREEN + "Exported review package for " + session.mapName + " to "
                    + plugin.getDataFolder().toPath().relativize(target.toAbsolutePath().normalize()).toString().replace('\\', '/') + ".");
            sender.sendMessage(ChatColor.GRAY + "The source map is unchanged. Publishing is a separate explicit step.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not export Dragon Escape draft " + session.draftName + ": " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Could not create the reviewable package; source map remains unchanged.");
        }
    }

    private void publish(CommandSender sender, String[] args) {
        if (args.length != 4 || !"confirm".equalsIgnoreCase(args[3])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit publish <map-id> <draft> confirm");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            sender.sendMessage(ChatColor.RED + "Publishing is locked while an Arcade match is active.");
            return;
        }
        SourceMapCatalog.MapArchive map = findMap(args[1]);
        String draft = DragonEscapeMapDraftRules.draftSlug(args[2]);
        if (map == null || draft == null) {
            sender.sendMessage(ChatColor.RED + "Unknown map or invalid draft name. Use /arcadeport mapedit list.");
            return;
        }
        DraftSession session = sessionForDraft(map.resource(), draft);
        if (session == null) {
            sender.sendMessage(ChatColor.RED + "Open the named draft in the editor and save it before publishing.");
            return;
        }
        List<String> errors = validateDraft(session);
        if (!errors.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Publishing blocked: fix validation errors first.");
            errors.forEach(error -> sender.sendMessage(ChatColor.RED + " • " + error));
            return;
        }
        try {
            Path export = exportDraft(session);
            String currentKey = SourceMapCatalog.publishedOverrideConfigKey(map.resource());
            String previousKey = currentKey.replace(".current.", ".previous.");
            String current = plugin.getConfig().getString(currentKey);
            String version = Long.toString(System.currentTimeMillis());
            String publishedRelative = PUBLISHED_DIRECTORY + "/" + mapId(map.resource()) + "/" + draft + "-" + version + ".zip";
            Path published = safePluginPath(publishedRelative);
            if (published == null) throw new IOException("Unsafe published archive path.");
            Files.createDirectories(published.getParent());
            Path staging = published.resolveSibling(published.getFileName() + ".installing-" + UUID.randomUUID());
            Files.copy(export, staging, StandardCopyOption.COPY_ATTRIBUTES);
            moveAtomically(staging, published);
            String oldPrevious = plugin.getConfig().getString(previousKey);
            plugin.getConfig().set(previousKey, current);
            plugin.getConfig().set(currentKey, publishedRelative);
            try {
                plugin.saveConfig();
            } catch (RuntimeException saveFailure) {
                plugin.getConfig().set(previousKey, oldPrevious);
                plugin.getConfig().set(currentKey, current);
                throw new IOException("Could not persist the new map version pointer.", saveFailure);
            }
            sender.sendMessage(ChatColor.GREEN + "Published " + session.mapName + " as a new reversible map version.");
            sender.sendMessage(ChatColor.GRAY + "It will be used by the next Dragon Escape match. Use mapedit rollback "
                    + mapId(map.resource()) + " confirm to switch back.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not publish Dragon Escape map draft " + draft + ": " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Publish failed before switching the active map version; the previous version is unchanged.");
        }
    }

    private void rollback(CommandSender sender, String[] args) {
        if (args.length != 3 || !"confirm".equalsIgnoreCase(args[2])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit rollback <map-id> confirm");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            sender.sendMessage(ChatColor.RED + "Rollback is locked while an Arcade match is active.");
            return;
        }
        SourceMapCatalog.MapArchive map = findMap(args[1]);
        if (map == null) {
            sender.sendMessage(ChatColor.RED + "Unknown map ID. Use /arcadeport mapedit list.");
            return;
        }
        String currentKey = SourceMapCatalog.publishedOverrideConfigKey(map.resource());
        String previousKey = currentKey.replace(".current.", ".previous.");
        String current = plugin.getConfig().getString(currentKey);
        String previous = plugin.getConfig().getString(previousKey);
        if (current == null && previous == null) {
            sender.sendMessage(ChatColor.YELLOW + "That map has no published editor version to roll back.");
            return;
        }
        plugin.getConfig().set(currentKey, previous);
        plugin.getConfig().set(previousKey, current);
        try {
            plugin.saveConfig();
        } catch (RuntimeException saveFailure) {
            plugin.getConfig().set(currentKey, current);
            plugin.getConfig().set(previousKey, previous);
            sender.sendMessage(ChatColor.RED + "Rollback could not be saved; the active version pointer was restored.");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Rolled " + map.displayName() + " back to its previous immutable version.");
        sender.sendMessage(ChatColor.GRAY + "The next Dragon Escape match will load the restored version; no source ZIP was modified.");
    }

    private Path exportDraft(DraftSession session) throws IOException {
        ensureLocalWorldConfig(session.world);
        session.world.save();
        String version = Long.toString(System.currentTimeMillis());
        Path export = safePluginPath(EXPORTS_DIRECTORY + "/" + mapId(session.resource) + "/"
                + session.draftName + "-" + version + ".zip");
        if (export == null) throw new IOException("Unsafe export path.");
        Files.createDirectories(export.getParent());
        Path staging = export.resolveSibling(export.getFileName() + ".building-" + UUID.randomUUID());
        Path root = session.world.getWorldFolder().toPath().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Draft world folder is not a safe directory.");
        }
        try (OutputStream raw = Files.newOutputStream(staging);
             ZipOutputStream zip = new ZipOutputStream(raw, StandardCharsets.UTF_8);
             var files = Files.walk(root)) {
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(p -> !Files.isSymbolicLink(p))
                    .sorted(Comparator.comparing(p -> root.relativize(p).toString())).toList()) {
                Path relative = root.relativize(path);
                if (relative.startsWith("playerdata") || relative.startsWith("stats")
                        || relative.startsWith("advancements") || relative.toString().equals("session.lock")) continue;
                if (!path.toRealPath().startsWith(root.toRealPath())) throw new IOException("Draft file escaped its world folder.");
                ZipEntry entry = new ZipEntry(relative.toString().replace('\\', '/'));
                entry.setTime(0L);
                zip.putNextEntry(entry);
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
        if (!Files.isRegularFile(staging) || Files.size(staging) == 0) {
            Files.deleteIfExists(staging);
            throw new IOException("Export archive is empty.");
        }
        moveAtomically(staging, export);
        return export;
    }

    private DraftSession sessionForDraft(String resource, String draft) {
        String expectedWorld = DragonEscapeMapDraftRules.worldName(resource, draft);
        World world = expectedWorld == null ? null : Bukkit.getWorld(expectedWorld);
        if (world == null) return null;
        for (Map.Entry<UUID, DraftSession> entry : sessions.entrySet()) {
            DraftSession session = entry.getValue();
            if (session.world == world && session.resource.equals(resource) && session.draftName.equals(draft)) return session;
        }
        return null;
    }

    private Path ensureLocalWorldConfig(World world) {
        Path local = world.getWorldFolder().toPath().resolve("WorldConfig.dat");
        try {
            if (Files.isRegularFile(local, LinkOption.NOFOLLOW_LINKS)) return local;
            Path source = SourceMapCatalog.resolveWorldConfigPath(world.getWorldFolder().toPath(),
                    Bukkit.getWorldContainer().toPath(), world.getName());
            if (source != null && Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
                    && !source.toAbsolutePath().normalize().equals(local.toAbsolutePath().normalize())) {
                Files.copy(source, local);
            }
            return local;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not prepare local Dragon Escape draft metadata: " + exception.getMessage());
            return local;
        }
    }

    private DraftSession current(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires an in-game authorised tester.");
            return null;
        }
        DraftSession session = sessions.get(player.getUniqueId());
        if (session == null || session.world != player.getWorld()) {
            player.sendMessage(ChatColor.YELLOW + "Open a Dragon Escape draft first: /arcadeport mapedit open <map-id> <draft>");
            return null;
        }
        return session;
    }

    private SourceMapCatalog.MapArchive findMap(String id) {
        if (id == null) return null;
        return SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE).stream()
                .filter(map -> mapId(map.resource()).equalsIgnoreCase(id)
                        || map.displayName().equalsIgnoreCase(id)
                        || map.resource().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }

    private static String mapId(String resource) {
        String slug = DragonEscapeMapDraftRules.mapSlug(resource);
        return slug == null ? "default" : slug;
    }

    private Path draftManifestPath(String draft) throws IOException {
        Path candidate = safePluginPath(DRAFTS_DIRECTORY + "/" + draft + "/draft.yml");
        if (candidate == null) throw new IOException("Unsafe draft manifest path.");
        return candidate;
    }

    private Path safePluginPath(String relative) throws IOException {
        if (relative == null || relative.startsWith("/") || relative.indexOf('\\') >= 0 || relative.contains("..")) return null;
        Path root = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root)) return null;
        Path current = root;
        for (Path segment : root.relativize(path)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) throw new IOException("Refusing to follow a symbolic link in map-editor storage.");
        }
        return path;
    }

    private static void saveYamlAtomically(YamlConfiguration yaml, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp-" + UUID.randomUUID());
        Files.writeString(temp, yaml.saveToString(), StandardCharsets.UTF_8);
        moveAtomically(temp, destination);
    }

    private static void writeLinesAtomically(List<String> lines, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp-" + UUID.randomUUID());
        Files.write(temp, lines, StandardCharsets.UTF_8);
        moveAtomically(temp, destination);
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void saveOpenDrafts() {
        for (DraftSession session : sessions.values()) {
            try {
                ensureLocalWorldConfig(session.world);
                session.world.save();
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Could not save Dragon Escape map draft " + session.draftName
                        + " during shutdown: " + exception.getMessage());
            }
        }
        sessions.clear();
        worldOwners.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDraftBlockBreak(BlockBreakEvent event) {
        if (isEditorWorld(event.getBlock().getWorld()) && !isDraftOwner(event.getPlayer(), event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDraftBlockPlace(BlockPlaceEvent event) {
        if (isEditorWorld(event.getBlock().getWorld()) && !isDraftOwner(event.getPlayer(), event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDraftInteract(PlayerInteractEvent event) {
        if (isEditorWorld(event.getPlayer().getWorld()) && !isDraftOwner(event.getPlayer(), event.getPlayer().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDraftDamage(EntityDamageEvent event) {
        if (isEditorWorld(event.getEntity().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onEditorQuit(PlayerQuitEvent event) {
        DraftSession session = sessions.remove(event.getPlayer().getUniqueId());
        if (session != null) {
            session.world.save();
            worldOwners.remove(session.world.getName(), event.getPlayer().getUniqueId());
        }
    }

    private boolean isDraftOwner(Player player, World world) {
        DraftSession session = sessions.get(player.getUniqueId());
        return session != null && session.world == world && arcadePlugin.hasAdminAccess(player);
    }

    private static boolean isEditorWorld(World world) {
        return world != null && world.getName().startsWith(WORLD_PREFIX);
    }

    private record DraftSession(String draftName, String resource, String mapName, World world,
            Location returnLocation, GameMode returnGameMode, boolean returnAllowFlight) {}
}
