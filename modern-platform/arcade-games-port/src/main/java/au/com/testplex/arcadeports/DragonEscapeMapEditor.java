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
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
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
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
    private final MapEditorUi ui;
    private final Map<String, UUID> worldOwners = new HashMap<>();

    DragonEscapeMapEditor(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.arcadePlugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.ui = new MapEditorUi(plugin, this);
    }

    boolean handle(CommandSender sender, String[] args) {
        if (!arcadePlugin.hasAdminAccess(sender)) {
            sender.sendMessage(ChatColor.RED + "Only authorised Arcade testers can use the Map Editor.");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) ui.chooseGame(player);
            else sendHelp(sender);
            return true;
        }
        if ("help".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> listMaps(sender, args);
            case "open" -> open(sender, args);
            case "current" -> openCurrentMap(sender, args);
            case "menu" -> { if (sender instanceof Player p) ui.sessionMenu(p); }
            case "tools" -> { if (sender instanceof Player p && isEditing(p)) ui.equip(p); }
            case "delete" -> delete(sender, args);
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
        sender.sendMessage(ChatColor.GOLD + "Universal Map Editor — /arcadeport mapedit opens the in-game game/map picker");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit current" + ChatColor.GRAY + " — edit the selected map for the current lobby game");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit list [game-id]" + ChatColor.GRAY + " — list maps and their IDs");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit open <game-id> <map-id> <draft>" + ChatColor.GRAY + " — create/open an isolated creative copy");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit tools" + ChatColor.GRAY + " — restore the in-game editing hotbar");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit menu" + ChatColor.GRAY + " — save, exit, publish or delete the draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit markers" + ChatColor.GRAY + " — list authored points in this draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit point <add|remove> <DATA|TEAM|CUSTOM> <group> [index]" + ChatColor.GRAY + " — edit a point at your position");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit bounds set <minX> <maxX> <minY> <maxY> <minZ> <maxZ>" + ChatColor.GRAY + " — set map bounds");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit <save|validate|export|close>" + ChatColor.GRAY + " — save, check, export or leave the draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit delete confirm" + ChatColor.GRAY + " — permanently delete the active isolated draft");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit publish <game-id> <map-id> <draft> confirm" + ChatColor.GRAY + " — explicit publish");
        sender.sendMessage(ChatColor.YELLOW + "/arcadeport mapedit rollback <game-id> <map-id> confirm" + ChatColor.GRAY + " — previous published version");
    }

    private void listMaps(CommandSender sender, String[] args) {
        ArcadePortGame game = args.length > 1 ? ArcadePortGame.fromId(args[1]) : selectedGame();
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Unknown game. Open the map picker to browse every game.");
            return;
        }
        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, game);
        if (maps.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No " + game.displayName() + " source maps are available.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + game.displayName() + " maps:");
        for (SourceMapCatalog.MapArchive map : maps) {
            sender.sendMessage(ChatColor.YELLOW + mapId(map.resource(), game) + ChatColor.GRAY + " — "
                    + map.displayName() + (map.inferredMetadata() ? ChatColor.RED + " (inferred metadata)" : ""));
        }
    }

    private void open(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Map drafts must be opened by an in-game authorised tester.");
            return;
        }
        if (args.length != 3 && args.length != 4) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit open <game-id> <map-id> <draft-name>");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            player.sendMessage(ChatColor.RED + "Close the active Arcade round before opening a map draft.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "A draft is already open. Use the Editor / Exit hotbar item to save, delete or close it.");
            ui.sessionMenu(player);
            return;
        }
        ArcadePortGame game = args.length == 4 ? ArcadePortGame.fromId(args[1]) : ArcadePortGame.DRAGON_ESCAPE;
        String resourceId = args.length == 4 ? args[2] : args[1];
        String draftSlug = DragonEscapeMapDraftRules.draftSlug(args[args.length - 1]);
        SourceMapCatalog.MapArchive archive = findMap(game, resourceId);
        if (draftSlug == null || archive == null) {
            player.sendMessage(ChatColor.RED + "Unknown map or invalid draft name. Use /arcadeport mapedit list; draft names use letters, numbers, _ and -.");
            return;
        }
        String worldName = DragonEscapeMapDraftRules.worldName(archive.resource(), draftSlug, game);
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
            manifestPath = draftManifestPath(game, archive.resource(), draftSlug);
            // Keep already-created Dragon Escape drafts editable after this upgrade.
            if (game == ArcadePortGame.DRAGON_ESCAPE) {
                Path legacy = draftManifestPath(draftSlug);
                if (Files.isRegularFile(legacy, LinkOption.NOFOLLOW_LINKS)) manifestPath = legacy;
            }
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
            if (!SourceMapCatalog.installEditableResource(plugin, game,
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
                plugin.getLogger().warning("Could not save map editor draft manifest: " + exception.getMessage());
                player.sendMessage(ChatColor.RED + "The draft metadata could not be saved; the extracted world was left untouched.");
                return;
            }
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            try {
                world = new WorldCreator(worldName).environment(World.Environment.NORMAL).createWorld();
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Could not load map editor draft world " + worldName + ": " + exception.getMessage());
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
        ItemStack[] previousInventory = player.getInventory().getContents().clone();
        if (!player.teleport(new Location(world, world.getSpawnLocation().getX(),
                world.getHighestBlockYAt(world.getSpawnLocation()) + 2.0D,
                world.getSpawnLocation().getZ()))) {
            player.sendMessage(ChatColor.RED + "Could not teleport to the isolated draft. No session was started.");
            return;
        }
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        DraftSession session = new DraftSession(game, draftSlug, archive.resource(), archive.displayName(), world,
                previous, previousMode, previousAllowFlight, previousInventory, manifestPath);
        sessions.put(player.getUniqueId(), session);
        worldOwners.put(worldName, player.getUniqueId());
        ui.equip(player);
        player.sendMessage(ChatColor.GREEN + "Opened isolated " + game.displayName() + " draft "
                + ChatColor.YELLOW + draftSlug + ChatColor.GREEN + " for " + archive.displayName()
                + ". Source files are unchanged.");
        player.sendMessage(ChatColor.GRAY + "Creative mode: place and break blocks normally; use the hotbar for datapoints, saving and exit.");
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
        if (selectedGame == null) {
            player.sendMessage(ChatColor.RED + "Choose a game in the Game Panel before using mapedit current.");
            return;
        }
        String selectedResource = SourceMapCatalog.selectedResource(plugin, selectedGame);
        String mapId = DragonEscapeMapDraftRules.mapSlug(selectedResource, selectedGame);
        if (mapId == null || findMap(selectedGame, selectedResource) == null) {
            player.sendMessage(ChatColor.RED + "The selected " + selectedGame.displayName() + " map is not available.");
            return;
        }
        open(sender, new String[] {"open", selectedGame.id(), selectedResource, mapId});
    }

    private void close(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        DraftSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(ChatColor.YELLOW + "No map draft is currently open.");
            return;
        }
        leaveEditor(player, session, true);
        player.sendMessage(ChatColor.GREEN + "Saved and closed draft " + session.draftName + ".");
    }

    private void leaveEditor(Player player, DraftSession session, boolean save) {
        if (save) {
            ensureLocalWorldConfig(session.world);
            session.world.save();
        }
        sessions.remove(player.getUniqueId());
        worldOwners.remove(session.world.getName(), player.getUniqueId());
        ui.endSession(player);
        player.setFlying(false);
        player.setAllowFlight(false);
        Location destination = session.returnLocation.getWorld() == null
                ? plugin.getServer().getWorlds().get(0).getSpawnLocation() : session.returnLocation;
        player.teleport(destination);
        player.setGameMode(session.returnGameMode);
        player.setAllowFlight(session.returnAllowFlight);
        player.getInventory().setContents(session.returnInventory);
        player.updateInventory();
    }

    private void delete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;
        DraftSession session = current(sender);
        if (session == null) return;
        if (args.length != 2 || !"confirm".equalsIgnoreCase(args[1])) {
            sender.sendMessage(ChatColor.YELLOW + "Delete permanently? Use the Delete Draft confirmation in the editor menu.");
            return;
        }
        if (session.world.getPlayers().size() != 1) {
            sender.sendMessage(ChatColor.RED + "Other players are present in the draft world. Ask them to leave before deletion.");
            return;
        }
        Path root = session.world.getWorldFolder().toPath().toAbsolutePath().normalize();
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        if (!root.equals(container.resolve(session.world.getName()).normalize())
                || !session.world.getName().startsWith(WORLD_PREFIX)
                || Files.isSymbolicLink(root)) {
            sender.sendMessage(ChatColor.RED + "Draft world folder location is unsafe; refusing deletion.");
            return;
        }
        leaveEditor(player, session, false);
        if (!Bukkit.unloadWorld(session.world, false)) {
            sender.sendMessage(ChatColor.RED + "Draft was closed, but the world is still loaded. No draft files were deleted.");
            return;
        }
        try {
            deleteTree(root);
            Path manifestDirectory = session.manifestPath.getParent();
            if (manifestDirectory == null || !manifestDirectory.startsWith(plugin.getDataFolder().toPath().toAbsolutePath().normalize())
                    || Files.isSymbolicLink(manifestDirectory)) throw new IOException("Unsafe draft manifest directory.");
            deleteTree(manifestDirectory);
            sender.sendMessage(ChatColor.GREEN + "Permanently deleted isolated draft " + session.draftName
                    + ". Published and original maps were not changed.");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not fully delete map draft: " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Draft was closed but deletion was incomplete. Review the server log.");
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(root)) throw new IOException("Refusing to delete a symbolic-link root.");
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
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
        SourceMapCatalog.MapData data = SourceMapCatalog.read(plugin, session.game, session.world);
        sender.sendMessage(ChatColor.GOLD + "Draft markers for " + session.mapName + ":");
        if (data == null) {
            sender.sendMessage(ChatColor.RED + "No WorldConfig.dat exists yet. Add the required data, team or custom points.");
            return;
        }
        sendGroups(sender, "DATA", data.dataLocationGroups());
        sendGroups(sender, "TEAM", data.teamSpawns());
        sendGroups(sender, "CUSTOM", data.customLocationGroups());
        if (session.game == ArcadePortGame.DRAGON_ESCAPE)
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
            sender.sendMessage(ChatColor.GREEN + "Draft validation passed: terrain, bounds and authored markers passed basic checks.");
            if (session.game != ArcadePortGame.DRAGON_ESCAPE)
                sender.sendMessage(ChatColor.YELLOW + "Game-specific objective/spawn behaviour still requires an in-game staging test.");
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
        SourceMapCatalog.MapData data = SourceMapCatalog.read(plugin, session.game, session.world);
        if (data == null) {
            errors.add("WorldConfig.dat is missing or unreadable.");
            return errors;
        }
        if (!data.hasBounds() || data.maxX() <= data.minX() || data.maxZ() <= data.minZ()) {
            errors.add("Valid MIN/MAX X and Z map bounds are required.");
        }
        List<Location> points = new ArrayList<>();
        data.dataLocationGroups().values().forEach(points::addAll);
        data.teamSpawns().values().forEach(points::addAll);
        data.customLocationGroups().values().forEach(points::addAll);
        if (points.isEmpty()) errors.add("This map needs authored spawn, team or objective datapoints.");
        if (session.game == ArcadePortGame.DRAGON_ESCAPE) {
            if (data.dataLocations("RED").isEmpty()) errors.add("Add a DATA RED start point.");
            if (data.dataLocations("BLACK").size() < 2)
                errors.add("Add at least two DATA BLACK route points, including the finish.");
            if (data.dataLocations("GREEN").isEmpty()) errors.add("Add a DATA GREEN speed marker.");
        }
        for (Location point : points) {
            if (point.getWorld() != session.world || point.getY() < session.world.getMinHeight()
                    || point.getY() >= session.world.getMaxHeight()) {
                errors.add("An authored marker falls outside the draft world's vertical range.");
                break;
            }
            if (data.hasBounds() && (point.getBlockX() < data.minX() || point.getBlockX() > data.maxX()
                    || point.getBlockZ() < data.minZ() || point.getBlockZ() > data.maxZ())) {
                errors.add("An authored marker falls outside the map's X/Z bounds.");
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
            plugin.getLogger().warning("Could not export map draft " + session.draftName + ": " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Could not create the reviewable package; source map remains unchanged.");
        }
    }

    private void publish(CommandSender sender, String[] args) {
        if ((args.length != 4 && args.length != 5)
                || !"confirm".equalsIgnoreCase(args[args.length - 1])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit publish <game-id> <map-id> <draft> confirm");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            sender.sendMessage(ChatColor.RED + "Publishing is locked while an Arcade match is active.");
            return;
        }
        ArcadePortGame game = args.length == 5 ? ArcadePortGame.fromId(args[1]) : ArcadePortGame.DRAGON_ESCAPE;
        int offset = args.length == 5 ? 1 : 0;
        SourceMapCatalog.MapArchive map = findMap(game, args[1 + offset]);
        String draft = DragonEscapeMapDraftRules.draftSlug(args[2 + offset]);
        if (map == null || draft == null) {
            sender.sendMessage(ChatColor.RED + "Unknown map or invalid draft name. Use /arcadeport mapedit list.");
            return;
        }
        DraftSession session = sessionForDraft(game, map.resource(), draft);
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
            String publishedRelative = PUBLISHED_DIRECTORY + "/" + game.id() + "/" + mapId(map.resource(), game)
                    + "/" + draft + "-" + version + ".zip";
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
            sender.sendMessage(ChatColor.GRAY + "The next " + game.displayName() + " match will use this version. Rollback: /arcadeport mapedit rollback "
                    + game.id() + " " + mapId(map.resource(), game) + " confirm");
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not publish map draft " + draft + ": " + exception.getMessage());
            sender.sendMessage(ChatColor.RED + "Publish failed before switching the active map version; the previous version is unchanged.");
        }
    }

    private void rollback(CommandSender sender, String[] args) {
        if ((args.length != 3 && args.length != 4)
                || !"confirm".equalsIgnoreCase(args[args.length - 1])) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /arcadeport mapedit rollback <game-id> <map-id> confirm");
            return;
        }
        if (!arcadePlugin.getArcadePortManager().canStartExternalGame()) {
            sender.sendMessage(ChatColor.RED + "Rollback is locked while an Arcade match is active.");
            return;
        }
        ArcadePortGame game = args.length == 4 ? ArcadePortGame.fromId(args[1]) : ArcadePortGame.DRAGON_ESCAPE;
        SourceMapCatalog.MapArchive map = findMap(game, args[args.length == 4 ? 2 : 1]);
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
        sender.sendMessage(ChatColor.GRAY + "The next " + game.displayName() + " match will load this version; no source ZIP was modified.");
    }

    private Path exportDraft(DraftSession session) throws IOException {
        ensureLocalWorldConfig(session.world);
        session.world.save();
        String version = Long.toString(System.currentTimeMillis());
        Path export = safePluginPath(EXPORTS_DIRECTORY + "/" + session.game.id() + "/"
                + mapId(session.resource, session.game) + "/" + session.draftName + "-" + version + ".zip");
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

    private DraftSession sessionForDraft(ArcadePortGame game, String resource, String draft) {
        String expectedWorld = DragonEscapeMapDraftRules.worldName(resource, draft, game);
        World world = expectedWorld == null ? null : Bukkit.getWorld(expectedWorld);
        if (world == null) return null;
        for (Map.Entry<UUID, DraftSession> entry : sessions.entrySet()) {
            DraftSession session = entry.getValue();
            if (session.world == world && session.game == game && session.resource.equals(resource)
                    && session.draftName.equals(draft)) return session;
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
            plugin.getLogger().warning("Could not prepare local map draft metadata: " + exception.getMessage());
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
            player.sendMessage(ChatColor.YELLOW + "Open a map from the Map Editor in the lobby first.");
            return null;
        }
        return session;
    }

    private ArcadePortGame selectedGame() {
        return ArcadePortGame.fromId(plugin.getConfig().getString("lobby-selected-game", ""));
    }

    boolean isEditing(Player player) {
        if (player == null) return false;
        DraftSession session = sessions.get(player.getUniqueId());
        return session != null && session.world == player.getWorld();
    }

    ArcadePortGame activeGame(Player player) {
        DraftSession session = player == null ? null : sessions.get(player.getUniqueId());
        return session != null && session.world == player.getWorld() ? session.game : null;
    }

    String activeResource(Player player) {
        DraftSession session = player == null ? null : sessions.get(player.getUniqueId());
        return session != null && session.world == player.getWorld() ? session.resource : null;
    }

    String activeDraft(Player player) {
        DraftSession session = player == null ? null : sessions.get(player.getUniqueId());
        return session != null && session.world == player.getWorld() ? session.draftName : null;
    }

    private SourceMapCatalog.MapArchive findMap(ArcadePortGame game, String id) {
        if (game == null || id == null) return null;
        return SourceMapCatalog.mapsFor(plugin, game).stream()
                .filter(map -> mapId(map.resource(), game).equalsIgnoreCase(id)
                        || map.displayName().equalsIgnoreCase(id)
                        || map.resource().equalsIgnoreCase(id))
                .findFirst().orElse(null);
    }

    private static String mapId(String resource, ArcadePortGame game) {
        String slug = DragonEscapeMapDraftRules.mapSlug(resource, game);
        return slug == null ? "default" : slug;
    }

    private Path draftManifestPath(ArcadePortGame game, String resource, String draft) throws IOException {
        String worldName = DragonEscapeMapDraftRules.worldName(resource, draft, game);
        if (worldName == null) throw new IOException("Unsafe map or draft identifier.");
        Path candidate = safePluginPath(DRAFTS_DIRECTORY + "/" + game.id() + "/"
                + worldName.substring(WORLD_PREFIX.length()) + "/" + draft + "/draft.yml");
        if (candidate == null) throw new IOException("Unsafe draft manifest path.");
        return candidate;
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
                plugin.getLogger().warning("Could not save map draft " + session.draftName
                        + " during shutdown: " + exception.getMessage());
            }
        }
        sessions.clear();
        worldOwners.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDraftBlockBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (!isEditorWorld(world)) return;
        // Gameplay/lobby protections must not prevent authorised edits in isolated draft worlds.
        event.setCancelled(!isDraftOwner(event.getPlayer(), world));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDraftBlockPlace(BlockPlaceEvent event) {
        World world = event.getBlock().getWorld();
        if (!isEditorWorld(world)) return;
        event.setCancelled(!isDraftOwner(event.getPlayer(), world));
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
            ensureLocalWorldConfig(session.world);
            session.world.save();
            worldOwners.remove(session.world.getName(), event.getPlayer().getUniqueId());
            ui.endSession(event.getPlayer());
        }
    }

    private boolean isDraftOwner(Player player, World world) {
        DraftSession session = sessions.get(player.getUniqueId());
        return session != null && session.world == world && arcadePlugin.hasAdminAccess(player);
    }

    private static boolean isEditorWorld(World world) {
        return world != null && world.getName().startsWith(WORLD_PREFIX);
    }

    private record DraftSession(ArcadePortGame game, String draftName, String resource, String mapName, World world,
            Location returnLocation, GameMode returnGameMode, boolean returnAllowFlight,
            ItemStack[] returnInventory, Path manifestPath) {}
}
