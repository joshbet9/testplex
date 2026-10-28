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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Simple modern inventory UI. Game-specific renderers are registered alongside each port. */
public final class MenuService implements Listener {
    private static final String PANEL_TITLE = "Modern Arcade | Game Panel";
    private static final String COSMETICS_TITLE = "Modern Arcade | Cosmetics";
    private static final String GAME_TITLE = "Modern Arcade | Game | ";
    private static final String LINES_TITLE = "Modern Arcade | Lines | ";
    private static final String OPTIONS_TITLE = "Modern Arcade | Options | ";
    private static final String WINS_TITLE = "Modern Arcade | Win Effects | ";

    private final JavaPlugin plugin;
    private final ArcadeCatalog catalog;
    private final PlayerPreferenceStore preferences;
    private final NamespacedKey actionKey;
    private final NamespacedKey gameKey;
    private final NamespacedKey lineKey;
    private final NamespacedKey optionKey;

    public MenuService(JavaPlugin plugin, ArcadeCatalog catalog, PlayerPreferenceStore preferences) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.preferences = preferences;
        actionKey = new NamespacedKey(plugin, "menu-action");
        gameKey = new NamespacedKey(plugin, "game-id");
        lineKey = new NamespacedKey(plugin, "line-id");
        optionKey = new NamespacedKey(plugin, "option-id");
    }

    public void openGamePanel(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, PANEL_TITLE);
        int slot = 0;
        for (GameDefinition game : catalog.getGames()) {
            if (slot >= inventory.getSize()) break;
            ItemStack item = makeItem(game.getIcon(), ChatColor.AQUA + game.getName(), List.of(
                    ChatColor.GRAY + game.getDescription(),
                    "",
                    ChatColor.YELLOW + "Port status: " + game.getPortStatus().getDisplay(),
                    ChatColor.DARK_GRAY + (game.isReadyForTest() ? "Ready for testing" : "Not yet playable")));
            mark(item, "game", game, null, null);
            inventory.setItem(slot++, item);
        }
        player.openInventory(inventory);
    }

    public void openCosmeticsHome(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, COSMETICS_TITLE);
        int slot = 0;
        for (GameDefinition game : catalog.getGames()) {
            if (slot >= inventory.getSize()) break;
            ItemStack item = makeItem(game.getIcon(), ChatColor.LIGHT_PURPLE + game.getName(), List.of(
                    ChatColor.GRAY + String.valueOf(game.getCosmeticLines().size()) + " dedicated cosmetic lines",
                    ChatColor.GRAY + String.valueOf(game.getWinEffects().size()) + " distinct win-scene choices",
                    ChatColor.DARK_GRAY + "Preferences are scoped to this game."));
            mark(item, "cosmetics-game", game, null, null);
            inventory.setItem(slot++, item);
        }
        player.openInventory(inventory);
    }

    private void openGameDetails(Player player, GameDefinition game) {
        Inventory inventory = Bukkit.createInventory(null, 27, GAME_TITLE + game.getId());
        inventory.setItem(11, makeItem(icon(game.getIcon()), ChatColor.AQUA + game.getName(), List.of(
                ChatColor.GRAY + game.getDescription(),
                ChatColor.YELLOW + "Port status: " + game.getPortStatus().getDisplay())));
        if ("monster-maze".equals(game.getId())) {
            boolean engineInstalled = Bukkit.getPluginManager().isPluginEnabled("MonsterMazeModern");
            ItemStack start = makeItem(engineInstalled ? Material.LIME_CONCRETE : Material.BARRIER,
                    (engineInstalled ? ChatColor.GREEN : ChatColor.RED) + "Start / Test Monster Maze",
                    List.of(ChatColor.GRAY + (engineInstalled
                            ? "Launch a mechanics test run."
                            : "Install MonsterMazeModern.jar alongside this plugin."),
                            ChatColor.YELLOW + (engineInstalled ? "Click to run /mm start." : "Engine module not detected.")));
            mark(start, "launch-game", game, null, null);
            inventory.setItem(4, start);
        }
        ItemStack cosmetics = makeItem(Material.CHEST, ChatColor.LIGHT_PURPLE + "Game-specific cosmetics", List.of(
                ChatColor.GRAY + "Browse this game's own visual identity."));
        mark(cosmetics, "open-lines", game, null, null);
        inventory.setItem(13, cosmetics);
        ItemStack wins = makeItem(Material.FIREWORK_ROCKET, ChatColor.GOLD + "Win Effects", List.of(
                ChatColor.GRAY + "Distinct celebrations for " + game.getName() + "."));
        mark(wins, "win-effects", game, null, null);
        inventory.setItem(15, wins);
        inventory.setItem(22, backItem("Back to Game Panel", "back-panel", null));
        player.openInventory(inventory);
    }

    private void openLines(Player player, GameDefinition game) {
        Inventory inventory = Bukkit.createInventory(null, 54, LINES_TITLE + game.getId());
        int slot = 0;
        for (CosmeticLineDefinition line : game.getCosmeticLines()) {
            if (slot >= 45) break;
            ItemStack item = makeItem(line.getIcon(), ChatColor.LIGHT_PURPLE + line.getTitle(), List.of(
                    ChatColor.GRAY + line.getDescription(),
                    ChatColor.YELLOW + "Selection: " + line.getSelectionMode().name(),
                    ChatColor.GRAY + String.valueOf(line.getOptions().size()) + " options"));
            mark(item, "cosmetic-line", game, line, null);
            inventory.setItem(slot++, item);
        }
        ItemStack wins = makeItem(Material.FIREWORK_ROCKET, ChatColor.GOLD + "Win Effects", List.of(
                ChatColor.GRAY + "Win effects are configured separately from cosmetic lines."));
        mark(wins, "win-effects", game, null, null);
        inventory.setItem(49, wins);
        inventory.setItem(53, backItem("Back to Game", "back-game", game));
        player.openInventory(inventory);
    }

    private void openOptions(Player player, GameDefinition game, CosmeticLineDefinition line) {
        Inventory inventory = Bukkit.createInventory(null, 27, OPTIONS_TITLE + game.getId() + " | " + line.getId());
        Set<String> selected = preferences.getSelected(player.getUniqueId(), game, line);
        int slot = 0;
        for (CosmeticOption option : line.getOptions()) {
            ItemStack item = makeItem(line.getIcon(),
                    (selected.contains(option.getId()) ? ChatColor.GREEN + "✓ " : ChatColor.WHITE) + option.getName(),
                    List.of(ChatColor.GRAY + option.getDescription(),
                            selected.contains(option.getId()) ? ChatColor.GREEN + "Selected" : ChatColor.GRAY + "Not selected",
                            line.getSelectionMode() == CosmeticLineDefinition.SelectionMode.MULTI
                                    ? ChatColor.YELLOW + "Click to toggle" : ChatColor.YELLOW + "Click to equip"));
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
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null || !title.startsWith("Modern Arcade |")) return;
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

        if ("launch-game".equals(action) && game != null && "monster-maze".equals(game.getId())) {
            if (!Bukkit.getPluginManager().isPluginEnabled("MonsterMazeModern")) {
                player.sendMessage(ChatColor.RED + "MonsterMazeModern.jar is not installed on this server yet.");
                return;
            }
            player.closeInventory();
            player.performCommand("mm start");
        } else if ("game".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("cosmetics-game".equals(action) && game != null) {
            openLines(player, game);
        } else if ("open-lines".equals(action) && game != null) {
            openLines(player, game);
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
        } else if ("back-panel".equals(action)) {
            openGamePanel(player);
        } else if ("back-game".equals(action) && game != null) {
            openGameDetails(player, game);
        } else if ("back-lines".equals(action) && game != null) {
            openLines(player, game);
        }
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
