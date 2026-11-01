package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/** 1.8 Choose Map inventory backed by every legacy archive bundled with the modern plugin. */
public final class ArcadeMapSelectionMenu implements Listener {
    private static final String TITLE = "Choose Map";
    private static final int PAGE_SIZE = 36;
    private static final int FIRST_MAP_SLOT = 9;
    private final JavaPlugin plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey gameKey;
    private final NamespacedKey resourceKey;
    private final NamespacedKey pageKey;

    public ArcadeMapSelectionMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        actionKey = new NamespacedKey(plugin, "map-menu-action");
        gameKey = new NamespacedKey(plugin, "map-menu-game");
        resourceKey = new NamespacedKey(plugin, "map-menu-resource");
        pageKey = new NamespacedKey(plugin, "map-menu-page");
    }

    public void openMapSelection(Player player, ArcadePortGame game) {
        if (!(plugin instanceof ArcadeMechanicsModernPlugin arcade) || !arcade.hasAdminAccess(player)) {
            player.sendMessage(ChatColor.RED + "Operator permission required to change the selected map.");
            return;
        }
        if (game == ArcadePortGame.MONSTER_MAZE) {
            openMonsterMazeMapSelection(player, 0);
            return;
        }
        openMapSelection(player, game, 0);
    }

    /** Browse both original Wizards and Nano Wizards server map pools. */
    public void openWizardsMapSelection(Player player) {
        openWizardsMapSelection(player, 0);
    }

    private void openWizardsMapSelection(Player player, int requestedPage) {
        List<String> folders = List.of("Wizards", "Nano Wizards");
        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsForFolders(plugin, "wizards", folders);
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        ItemStack back = createItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back",
                ChatColor.GRAY + "Return to the Testplex Set Game page.");
        markCustom(back, "back", "wizards", null);
        inventory.setItem(4, back);

        if (maps.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "No Wizards Source Maps Found",
                    ChatColor.GRAY + "No Wizards ZIPs were found in the plugin or configured server map directory.",
                    ChatColor.GRAY + "Set source-maps.directory in config.yml to the existing update/maps folder.",
                    ChatColor.GRAY + "Both Wizards and Nano Wizards folders are scanned."));
            player.openInventory(inventory);
            return;
        }

        int pageCount = Math.max(1, (maps.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        String selected = SourceMapCatalog.selectedResourceForFolders(plugin, "wizards", folders);
        int start = page * PAGE_SIZE;
        int end = Math.min(maps.size(), start + PAGE_SIZE);
        int slot = FIRST_MAP_SLOT;
        for (int index = start; index < end; index++) {
            SourceMapCatalog.MapArchive map = maps.get(index);
            boolean isSelected = map.resource().equals(selected);
            ItemStack item = createItem(Material.PAPER,
                    (isSelected ? ChatColor.GREEN + "✓ " : ChatColor.YELLOW.toString()) + map.displayName(),
                    ChatColor.GRAY + "Source: Mineplex Wizards/Nano Wizards",
                    ChatColor.YELLOW + "Left-click to select this source map.",
                    isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.DARK_GRAY + map.resource());
            markCustom(item, "select-map", "wizards", map.resource());
            inventory.setItem(slot++, item);
        }
        inventory.setItem(49, createItem(Material.MAP,
                ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                ChatColor.GRAY.toString() + maps.size() + " Wizards source maps available."));
        if (page > 0) {
            ItemStack previous = createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            markPageCustom(previous, "previous-page", "wizards", page - 1);
            inventory.setItem(45, previous);
        }
        if (page + 1 < pageCount) {
            ItemStack next = createItem(Material.ARROW, ChatColor.YELLOW + "Next Page");
            markPageCustom(next, "next-page", "wizards", page + 1);
            inventory.setItem(53, next);
        }
        player.openInventory(inventory);
    }

    /** Monster Maze maps are physical source worlds owned by its in-process game module. */
    private void openMonsterMazeMapSelection(Player player, int requestedPage) {
        if (!(plugin instanceof ArcadeMechanicsModernPlugin arcade)
                || arcade.getMonsterMazeModule() == null
                || arcade.getMonsterMazeModule().getMapManager() == null) {
            player.sendMessage(ChatColor.RED + "Monster Maze map data is not loaded by Testplex Arcade.");
            return;
        }
        me.monstermaze.world.MapManager mapManager = arcade.getMonsterMazeModule().getMapManager();
        List<String> maps = mapManager.knownMaps();
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        ItemStack back = createItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back",
                ChatColor.GRAY + "Return to the Testplex Set Game page.");
        mark(back, "back", ArcadePortGame.MONSTER_MAZE, null);
        inventory.setItem(4, back);

        if (maps.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "No Maps Defined",
                    ChatColor.GRAY + "Monster Maze has no maps in its bundled maps.yml."));
            player.openInventory(inventory);
            return;
        }

        int pageCount = Math.max(1, (maps.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        String selected = mapManager.getActiveMap();
        int start = page * PAGE_SIZE;
        int end = Math.min(maps.size(), start + PAGE_SIZE);
        int slot = FIRST_MAP_SLOT;
        for (int index = start; index < end; index++) {
            String map = maps.get(index);
            boolean isSelected = map.equalsIgnoreCase(selected);
            boolean available = mapManager.isAvailable(map);
            ItemStack item = createItem(available ? Material.PAPER : Material.BARRIER,
                    (isSelected ? ChatColor.GREEN + "✓ " : ChatColor.YELLOW.toString())
                            + map.replace('-', ' '),
                    ChatColor.GRAY + "Source: Monster Maze",
                    available ? ChatColor.YELLOW + "Left-click to select this installed map."
                            : ChatColor.RED + "Map world is not installed.",
                    isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.DARK_GRAY + map);
            mark(item, "select-map", ArcadePortGame.MONSTER_MAZE, map);
            inventory.setItem(slot++, item);
        }

        inventory.setItem(49, createItem(Material.MAP,
                ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                ChatColor.GRAY.toString() + maps.size() + " Monster Maze maps defined."));
        if (page > 0) {
            ItemStack previous = createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            markPage(previous, "previous-page", ArcadePortGame.MONSTER_MAZE, page - 1);
            inventory.setItem(45, previous);
        }
        if (page + 1 < pageCount) {
            ItemStack next = createItem(Material.ARROW, ChatColor.YELLOW + "Next Page");
            markPage(next, "next-page", ArcadePortGame.MONSTER_MAZE, page + 1);
            inventory.setItem(53, next);
        }
        player.openInventory(inventory);
    }

    private void openMapSelection(Player player, ArcadePortGame game, int requestedPage) {
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Unknown game id.");
            return;
        }

        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, game);
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        ItemStack back = createItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back",
                ChatColor.GRAY + "Return to the Set Game page.");
        mark(back, "back", game, null);
        inventory.setItem(4, back);

        if (maps.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "No Maps Installed",
                    ChatColor.GRAY + "There are no bundled 1.8 map archives for this game."));
            player.openInventory(inventory);
            return;
        }

        int pageCount = Math.max(1, (maps.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        String selected = SourceMapCatalog.selectedResource(plugin, game);
        int start = page * PAGE_SIZE;
        int end = Math.min(maps.size(), start + PAGE_SIZE);
        int slot = FIRST_MAP_SLOT;

        for (int index = start; index < end; index++) {
            SourceMapCatalog.MapArchive map = maps.get(index);
            boolean isSelected = map.resource().equals(selected);
            ItemStack item = createItem(Material.PAPER,
                    (isSelected ? ChatColor.GREEN + "✓ " : ChatColor.YELLOW.toString()) + map.displayName(),
                    ChatColor.GRAY + "Source: " + game.displayName(),
                    ChatColor.YELLOW + "Left-click to select this map.",
                    isSelected ? ChatColor.GREEN + "Currently selected" : ChatColor.DARK_GRAY + "Authentic 1.8 map archive");
            mark(item, "select-map", game, map.resource());
            inventory.setItem(slot++, item);
        }

        ItemStack pageInfo = createItem(Material.MAP,
                ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                ChatColor.GRAY.toString() + maps.size() + " authentic maps available for " + game.displayName() + ".");
        inventory.setItem(49, pageInfo);

        if (page > 0) {
            ItemStack previous = createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            markPage(previous, "previous-page", game, page - 1);
            inventory.setItem(45, previous);
        }
        if (page + 1 < pageCount) {
            ItemStack next = createItem(Material.ARROW, ChatColor.YELLOW + "Next Page");
            markPage(next, "next-page", game, page + 1);
            inventory.setItem(53, next);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).startsWith(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (!(plugin instanceof ArcadeMechanicsModernPlugin arcade) || !arcade.hasAdminAccess(player)) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Operator permission required to change the selected map.");
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String gameId = meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
        String resource = meta.getPersistentDataContainer().get(resourceKey, PersistentDataType.STRING);
        ArcadePortGame game = ArcadePortGame.fromId(gameId);

        if ("wizards".equalsIgnoreCase(gameId)
                && ("previous-page".equals(action) || "next-page".equals(action))) {
            Integer page = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (page != null) openWizardsMapSelection(player, page);
        } else if ("select-map".equals(action) && "wizards".equalsIgnoreCase(gameId) && resource != null
                && SourceMapCatalog.mapsForFolders(plugin, "wizards", List.of("Wizards", "Nano Wizards")).stream()
                        .anyMatch(map -> map.resource().equals(resource))) {
            plugin.getConfig().set("selected-maps.wizards", resource);
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Selected Wizards map " + SourceMapCatalog.displayName(plugin, resource)
                    + ". It will load for the next Wizards match.");
            player.closeInventory();
        } else if ("back".equals(action)) {
            player.performCommand("setgame");
        } else if ("previous-page".equals(action) || "next-page".equals(action)) {
            Integer page = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (game == ArcadePortGame.MONSTER_MAZE && page != null) {
                openMonsterMazeMapSelection(player, page);
            } else if (game != null && page != null) {
                openMapSelection(player, game, page);
            }
        } else if ("select-map".equals(action) && game == ArcadePortGame.MONSTER_MAZE && resource != null
                && arcade.getMonsterMazeModule() != null) {
            me.monstermaze.MonsterMazePlugin module = arcade.getMonsterMazeModule();
            if (!module.getMapManager().setActiveMap(resource)) {
                player.sendMessage(ChatColor.RED + "That Monster Maze map is unavailable on this test server.");
                return;
            }
            org.bukkit.Location center = module.getMapManager().defaultCenter();
            if (center == null) {
                player.sendMessage(ChatColor.RED + "The selected Monster Maze map could not be loaded.");
                return;
            }
            module.getGameManager().setCenter(center);
            module.getGameManager().getMonsterManager().setMobType(module.getMapManager().activeMob());
            player.performCommand("arcadeport lobby monster-maze");
            player.sendMessage(ChatColor.GREEN + "Selected Monster Maze map "
                    + resource.replace('-', ' ') + " for Testplex Arcade.");
            player.performCommand("setgame");
        } else if ("select-map".equals(action) && game != null && resource != null
                && SourceMapCatalog.mapsFor(plugin, game).stream()
                        .anyMatch(map -> map.resource().equals(resource))) {
            plugin.getConfig().set("selected-maps." + game.id(), resource);
            plugin.saveConfig();
            player.performCommand("arcadeport lobby " + game.id());
            player.sendMessage(ChatColor.GREEN + "Selected authentic map "
                    + SourceMapCatalog.displayName(plugin, resource) + " for " + game.displayName() + ".");
            player.performCommand("setgame");
        }
    }

    private ItemStack createItem(Material material, String title, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(java.util.Arrays.asList(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    private void mark(ItemStack item, String action, ArcadePortGame game, String resource) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (game != null) meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, game.id());
        if (resource != null) meta.getPersistentDataContainer().set(resourceKey, PersistentDataType.STRING, resource);
        item.setItemMeta(meta);
    }

    private void markPage(ItemStack item, String action, ArcadePortGame game, int page) {
        mark(item, action, game, null);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
    }

    private void markCustom(ItemStack item, String action, String gameId, String resource) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, gameId);
        if (resource != null) meta.getPersistentDataContainer().set(resourceKey, PersistentDataType.STRING, resource);
        item.setItemMeta(meta);
    }

    private void markPageCustom(ItemStack item, String action, String gameId, int page) {
        markCustom(item, action, gameId, null);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
    }
}
