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
        openMapSelection(player, game, 0);
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
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String gameId = meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
        String resource = meta.getPersistentDataContainer().get(resourceKey, PersistentDataType.STRING);
        ArcadePortGame game = ArcadePortGame.fromId(gameId);

        if ("back".equals(action)) {
            player.performCommand("setgame");
        } else if ("previous-page".equals(action) || "next-page".equals(action)) {
            Integer page = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (game != null && page != null) openMapSelection(player, game, page);
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
}
