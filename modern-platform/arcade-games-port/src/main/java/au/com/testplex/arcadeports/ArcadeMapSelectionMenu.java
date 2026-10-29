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

/** 1.8 Choose Map inventory adapted to source archives bundled with the modern test module. */
public final class ArcadeMapSelectionMenu implements Listener {
    private static final String TITLE = "Choose Map";
    private final JavaPlugin plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey gameKey;
    private final NamespacedKey resourceKey;

    public ArcadeMapSelectionMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        actionKey = new NamespacedKey(plugin, "map-menu-action");
        gameKey = new NamespacedKey(plugin, "map-menu-game");
        resourceKey = new NamespacedKey(plugin, "map-menu-resource");
    }

    public void openMapSelection(Player player, ArcadePortGame game) {
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Unknown game id.");
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        ItemStack back = createItem(Material.RED_BED, ChatColor.GRAY + "↩ Go Back",
                ChatColor.GRAY + "Return to the Set Game page.");
        mark(back, "back", game, null);
        inventory.setItem(4, back);

        String resource = SourceMapCatalog.resourceFor(game);
        if (resource == null) {
            inventory.setItem(22, createItem(Material.BARRIER, ChatColor.RED + "No Maps Installed",
                    ChatColor.GRAY + "No source map archive is bundled for this game on modern 26.3."));
        } else {
            String mapName = SourceMapCatalog.displayName(plugin, game);
            ItemStack map = createItem(Material.PAPER, ChatColor.YELLOW + mapName,
                    ChatColor.GRAY + "Source: " + game.displayName(),
                    ChatColor.YELLOW + "Left-click to select this map.");
            mark(map, "select-map", game, resource);
            inventory.setItem(9, map);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(ChatColor.stripColor(event.getView().getTitle()))) return;
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
        } else if ("select-map".equals(action) && game != null && resource != null
                && resource.equals(SourceMapCatalog.resourceFor(game))) {
            plugin.getConfig().set("selected-maps." + game.id(), resource);
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Selected map " + SourceMapCatalog.displayName(plugin, game)
                    + " for " + game.displayName() + ".");
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
}
