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
import java.util.Locale;
import java.util.Set;

/** Simple modern inventory UI. Game-specific renderers are registered alongside each port. */
public final class MenuService implements Listener {
    private static final String PANEL_TITLE = "Game Panel";
    private static final String SET_GAME_TITLE = "Set Game";
    private static final String GAME_OPTIONS_TITLE = "Game Options";
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

    public MenuService(JavaPlugin plugin, ArcadeCatalog catalog, PlayerPreferenceStore preferences) {
        this.plugin = plugin;
        this.catalog = catalog;
        this.preferences = preferences;
        actionKey = new NamespacedKey(plugin, "menu-action");
        gameKey = new NamespacedKey(plugin, "game-id");
        lineKey = new NamespacedKey(plugin, "line-id");
        optionKey = new NamespacedKey(plugin, "option-id");
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
        panelButton(inventory, 10, Material.EMERALD_BLOCK, "Start Game", "panel-start");
        panelButton(inventory, 19, Material.REDSTONE_BLOCK, "Stop Game", "panel-stop");
        panelButton(inventory, 16, Material.LAVA_BUCKET, "Remove Player", "panel-remove");
        panelButton(inventory, 12, Material.WRITABLE_BOOK, "Set Game", "panel-set-game");
        panelButton(inventory, 21, Material.BOOK, "Edit Game Rotation", "panel-rotation");
        panelButton(inventory, 14, Material.COMPARATOR, "Game Options", "panel-options");
        panelButton(inventory, 25, Material.WATER_BUCKET, "Un-Remove Player", "panel-unremove");
        player.openInventory(inventory);
    }

    private void panelButton(Inventory inventory, int slot, Material material, String label, String action) {
        ItemStack item = makeItem(material, label, List.of(ChatColor.GRAY + "Click to open."));
        mark(item, action, null, null, null);
        inventory.setItem(slot, item);
    }

    public void openSetGamePage(Player player) {
        openGameSelection(player, false);
    }

    private void openGameSelection(Player player, boolean rotation) {
        Inventory inventory = Bukkit.createInventory(null, 54, rotation ? "Edit Game Rotation" : SET_GAME_TITLE);
        int slot = 9;
        for (GameDefinition game : catalog.getGames()) {
            if (slot >= 45) break;
            boolean ready = "bacon-brawl".equals(game.getId())
                    && Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern");
            ItemStack item = makeItem(icon(game.getIcon()),
                    (ready ? ChatColor.GREEN : ChatColor.WHITE) + game.getName(),
                    List.of(ChatColor.RESET + ChatColor.GRAY.toString() + "Select this as the next game.",
                            "§1",
                            ChatColor.YELLOW + "Left-Click " + ChatColor.GRAY + (ready
                                    ? "to select this game."
                                    : "to view the parity gate; this game is not enabled yet."),
                            ChatColor.YELLOW + "Right-Click " + ChatColor.GRAY + "to choose an installed map.",
                            ChatColor.DARK_GRAY + "Port status: " + game.getPortStatus().getDisplay()));
            mark(item, rotation ? "panel-rotation-select" : "panel-game-select", game, null, null);
            inventory.setItem(slot++, item);
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
        if (title == null || !(title.equals(PANEL_TITLE) || title.equals(SET_GAME_TITLE)
                || title.equals("Edit Game Rotation") || title.equals(GAME_OPTIONS_TITLE)
                || title.equals("Remove Player") || title.equals("Un-Remove Player")
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
            } else {
                player.closeInventory();
                player.performCommand("arcadeport maps " + game.getId());
            }
            return;
        }
        if (PANEL_TITLE.equals(title)) {
            if ("panel-set-game".equals(action)) openGameSelection(player, false);
            else if ("panel-rotation".equals(action)) openGameSelection(player, true);
            else if ("panel-options".equals(action)) {
                player.sendMessage(ChatColor.YELLOW + "Modern game options are handled by the isolated test runtime.");
            } else if ("panel-remove".equals(action) || "panel-unremove".equals(action)) {
                player.sendMessage(ChatColor.YELLOW + "Player moderation is managed by the isolated server operator.");
            } else if ("panel-start".equals(action)) {
                if (!player.isOp() && !player.hasPermission("testplex.modern.admin")) {
                    player.sendMessage(ChatColor.RED + "Operator permission required to start a game.");
                } else {
                    player.closeInventory();
                    player.performCommand("arcadeport start "
                            + plugin.getConfig().getString("game-panel.selected-game", "bacon-brawl"));
                }
            } else if ("panel-stop".equals(action)) {
                if (!player.isOp() && !player.hasPermission("testplex.arcadeports.admin")) {
                    player.sendMessage(ChatColor.RED + "Operator permission required to stop a game.");
                } else {
                    player.closeInventory();
                    player.performCommand("arcadeport stop");
                }
            }
        } else if ((SET_GAME_TITLE.equals(title) || "Edit Game Rotation".equals(title))
                && ("panel-game-select".equals(action) || "panel-rotation-select".equals(action))) {
            if (game == null) return;
            if (!"bacon-brawl".equals(game.getId())
                    || !Bukkit.getPluginManager().isPluginEnabled("ArcadeMechanicsModern")) {
                player.sendMessage(ChatColor.RED + game.getName() + " is not enabled for modern acceptance yet.");
            } else if ("panel-rotation-select".equals(action)) {
                plugin.getConfig().set("game-panel.selected-game", game.getId());
                List<String> rotation = new ArrayList<>(plugin.getConfig().getStringList("game-panel.rotation"));
                if (!rotation.contains(game.getId())) rotation.add(game.getId());
                plugin.getConfig().set("game-panel.rotation", rotation);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + game.getName() + " added to the local test rotation.");
                openGameSelection(player, true);
            } else {
                plugin.getConfig().set("game-panel.selected-game", game.getId());
                plugin.saveConfig();
                player.closeInventory();
                player.performCommand("arcadeport lobby " + game.getId());
                player.sendMessage(ChatColor.GREEN + "Selected " + game.getName()
                        + ". Right-click the game to choose a map, then use Start Game in the Game Panel.");
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
