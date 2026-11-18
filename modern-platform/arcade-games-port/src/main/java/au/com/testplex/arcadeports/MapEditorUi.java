package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** In-world controls for the shared map editor. No game-specific marker names are assumed. */
final class MapEditorUi implements Listener {
    private final ArcadeMechanicsModernPlugin plugin;
    private final DragonEscapeMapEditor editor;
    private final NamespacedKey toolKey;
    private final Map<UUID, Menu> menus = new HashMap<>();
    private final Map<UUID, Location> firstBounds = new HashMap<>();
    private final Set<UUID> shiftClicking = new HashSet<>();

    private record Menu(Inventory inventory, Map<Integer, Consumer<Player>> actions) {}
    private record MarkerGroup(String type, String name, List<Location> points) {}

    MapEditorUi(ArcadeMechanicsModernPlugin plugin, DragonEscapeMapEditor editor) {
        this.plugin = plugin;
        this.editor = editor;
        this.toolKey = new NamespacedKey(plugin, "map-editor-tool");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void equip(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, item(Material.COMPASS, "Datapoints", "markers",
                "Browse, visit, add and remove authored map points."));
        player.getInventory().setItem(1, item(Material.EMERALD, "Add Datapoint", "add",
                "Choose a data, team or custom group; add at your position."));
        player.getInventory().setItem(2, item(Material.WOODEN_AXE, "Map Bounds", "bounds",
                "Left-click one corner, right-click the opposite corner."));
        player.getInventory().setItem(4, item(Material.CHEST, "Save Draft", "save",
                "Save edited terrain and markers in your isolated copy."));
        player.getInventory().setItem(5, item(Material.BOOK, "Validate / Export", "review",
                "Check required metadata and export a reviewable ZIP."));
        player.getInventory().setItem(8, item(Material.BARRIER, "Editor / Exit", "session",
                "Save, publish, exit or permanently delete this draft."));
        player.sendMessage(ChatColor.AQUA + "Map Editor tools equipped. Slots 4, 7 and 8 are free for building blocks.");
        player.sendMessage(ChatColor.GRAY + "Use the creative inventory to pick blocks. /arcadeport mapedit tools restores the toolbar.");
    }

    private ItemStack item(Material material, String name, String tool, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        List<String> lines = new ArrayList<>();
        for (String line : lore) lines.add(ChatColor.GRAY + line);
        meta.setLore(lines);
        if (tool != null) meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, tool);
        item.setItemMeta(meta);
        return item;
    }

    private static Inventory menu(String title) {
        return Bukkit.createInventory(null, 54, ChatColor.DARK_AQUA + "Map Editor | " + title);
    }

    private void show(Player player, Inventory inventory, Map<Integer, Consumer<Player>> actions) {
        menus.put(player.getUniqueId(), new Menu(inventory, actions));
        player.openInventory(inventory);
    }

    void chooseGame(Player player) {
        if (!plugin.hasAdminAccess(player)) return;
        if (editor.isEditing(player)) {
            sessionMenu(player);
            return;
        }
        Inventory inv = menu("Choose Game");
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        List<ArcadePortGame> games = new ArrayList<>(List.of(ArcadePortGame.values()));
        games.sort(Comparator.comparing(ArcadePortGame::displayName));
        int slot = 0;
        for (ArcadePortGame game : games) {
            List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, game);
            inv.setItem(slot, item(maps.isEmpty() ? Material.BARRIER : game.floor(), game.displayName(), null,
                    maps.size() + " available map(s)", maps.isEmpty() ? "No source maps available" : "Click to choose a map"));
            if (!maps.isEmpty()) actions.put(slot, p -> chooseMap(p, game, 0));
            slot++;
        }
        show(player, inv, actions);
    }

    private void chooseMap(Player player, ArcadePortGame game, int page) {
        List<SourceMapCatalog.MapArchive> maps = SourceMapCatalog.mapsFor(plugin, game);
        int start = page * 45;
        if (start >= maps.size() && page > 0) {
            chooseMap(player, game, page - 1);
            return;
        }
        Inventory inv = menu(game.displayName());
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        for (int i = start; i < Math.min(start + 45, maps.size()); i++) {
            SourceMapCatalog.MapArchive map = maps.get(i);
            int slot = i - start;
            inv.setItem(slot, item(Material.FILLED_MAP, map.displayName(), null,
                    "Creates or resumes an isolated draft", map.inferredMetadata() ? "Source metadata may need repairs" : "Authored map"));
            actions.put(slot, p -> {
                p.closeInventory();
                String id = DragonEscapeMapDraftRules.mapSlug(map.resource(), game);
                if (id == null) {
                    p.sendMessage(ChatColor.RED + "Could not generate a safe draft ID for this map.");
                    return;
                }
                editor.handle(p, new String[] {"open", game.id(), map.resource(), id});
            });
        }
        inv.setItem(45, item(Material.ARROW, "Previous Page", null));
        inv.setItem(49, item(Material.OAK_DOOR, "Choose Game", null));
        inv.setItem(53, item(Material.ARROW, "Next Page", null));
        if (page > 0) actions.put(45, p -> chooseMap(p, game, page - 1));
        actions.put(49, this::chooseGame);
        if (start + 45 < maps.size()) actions.put(53, p -> chooseMap(p, game, page + 1));
        show(player, inv, actions);
    }

    private List<MarkerGroup> groups(Player player) {
        ArcadePortGame game = editor.activeGame(player);
        if (game == null) return List.of();
        SourceMapCatalog.MapData data = SourceMapCatalog.readEditorDraft(plugin, game,
                editor.activeResource(player), player.getWorld());
        Map<String, MarkerGroup> all = new LinkedHashMap<>();
        if (data != null) {
            collect(all, "DATA", data.dataLocationGroups());
            collect(all, "TEAM", data.teamSpawns());
            collect(all, "CUSTOM", data.customLocationGroups());
        }
        // Defaults are shortcuts only. All authored names are discovered dynamically above.
        for (String name : List.of("RED", "BLACK", "GREEN", "BLUE")) {
            all.putIfAbsent("DATA:" + name, new MarkerGroup("DATA", name, List.of()));
        }
        for (String name : List.of("RED", "BLUE", "GREEN", "YELLOW")) {
            all.putIfAbsent("TEAM:" + name, new MarkerGroup("TEAM", name, List.of()));
        }
        all.putIfAbsent("CUSTOM:SPAWN", new MarkerGroup("CUSTOM", "SPAWN", List.of()));
        return new ArrayList<>(all.values());
    }

    private static void collect(Map<String, MarkerGroup> all, String type, Map<String, List<Location>> input) {
        input.forEach((name, locations) -> all.put(type + ":" + name,
                new MarkerGroup(type, name, List.copyOf(locations))));
    }

    void markerGroups(Player player, boolean addMode, int page) {
        if (!editor.isEditing(player)) return;
        List<MarkerGroup> groups = groups(player);
        int start = page * 45;
        Inventory inv = menu(addMode ? "Add Point" : "Datapoints");
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        for (int i = start; i < Math.min(start + 45, groups.size()); i++) {
            MarkerGroup group = groups.get(i);
            int slot = i - start;
            inv.setItem(slot, item(Material.LODESTONE, group.type + " " + group.name, null,
                    group.points.size() + " point(s)", addMode ? "Click to add at your feet" : "Click to view and edit points"));
            actions.put(slot, p -> {
                if (addMode) {
                    editor.handle(p, new String[] {"point", "add", group.type, group.name});
                    markerGroups(p, true, page);
                } else {
                    markerPoints(p, group.type, group.name, 0);
                }
            });
        }
        inv.setItem(45, item(Material.ARROW, "Previous", null));
        inv.setItem(49, item(Material.OAK_DOOR, "Editor Menu", null));
        inv.setItem(53, item(Material.ARROW, "Next", null));
        if (page > 0) actions.put(45, p -> markerGroups(p, addMode, page - 1));
        actions.put(49, this::sessionMenu);
        if (start + 45 < groups.size()) actions.put(53, p -> markerGroups(p, addMode, page + 1));
        show(player, inv, actions);
    }

    private void markerPoints(Player player, String type, String group, int page) {
        if (!editor.isEditing(player)) return;
        List<Location> points = groups(player).stream()
                .filter(g -> g.type.equals(type) && g.name.equals(group))
                .findFirst().map(MarkerGroup::points).orElse(List.of());
        int start = page * 45;
        Inventory inv = menu(type + " " + group);
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        for (int i = start; i < Math.min(start + 45, points.size()); i++) {
            Location point = points.get(i);
            int index = i + 1;
            int slot = i - start;
            inv.setItem(slot, item(Material.ENDER_PEARL, "Point #" + index, null,
                    String.format("X %.1f  Y %.1f  Z %.1f", point.getX(), point.getY(), point.getZ()),
                    "Click: teleport and highlight", "Shift-click: confirm deletion"));
            actions.put(slot, p -> {
                if (p.isSneaking() || shiftClicking.contains(p.getUniqueId())) {
                    confirm(p, "Remove point #" + index + "?", () -> {
                        editor.handle(p, new String[] {"point", "remove", type, group, Integer.toString(index)});
                        markerPoints(p, type, group, Math.max(0, page - (index > points.size() - 1 ? 1 : 0)));
                    }, () -> markerPoints(p, type, group, page));
                } else {
                    p.closeInventory();
                    p.teleport(point.clone().add(0, 0.15, 0));
                    p.spawnParticle(Particle.END_ROD, point, 25, 0.4, 0.6, 0.4);
                }
            });
        }
        inv.setItem(45, item(Material.ARROW, "Previous", null));
        inv.setItem(48, item(Material.EMERALD, "Add Point Here", null, "Adds at your current position"));
        inv.setItem(49, item(Material.OAK_DOOR, "All Datapoints", null));
        inv.setItem(53, item(Material.ARROW, "Next", null));
        if (page > 0) actions.put(45, p -> markerPoints(p, type, group, page - 1));
        actions.put(48, p -> {
            editor.handle(p, new String[] {"point", "add", type, group});
            markerPoints(p, type, group, 0);
        });
        actions.put(49, p -> markerGroups(p, false, 0));
        if (start + 45 < points.size()) actions.put(53, p -> markerPoints(p, type, group, page + 1));
        show(player, inv, actions);
    }

    void reviewMenu(Player player) {
        if (!editor.isEditing(player)) return;
        Inventory inv = menu("Review / Export");
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        inv.setItem(20, item(Material.WRITABLE_BOOK, "Validate Draft", null, "Report errors in chat"));
        inv.setItem(24, item(Material.CHEST, "Export ZIP", null, "Requires successful validation"));
        inv.setItem(49, item(Material.OAK_DOOR, "Editor Menu", null));
        actions.put(20, p -> { editor.handle(p, new String[] {"validate"}); p.closeInventory(); });
        actions.put(24, p -> { editor.handle(p, new String[] {"export"}); p.closeInventory(); });
        actions.put(49, this::sessionMenu);
        show(player, inv, actions);
    }

    void sessionMenu(Player player) {
        if (!editor.isEditing(player)) { chooseGame(player); return; }
        Inventory inv = menu("Draft Actions");
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        inv.setItem(10, item(Material.CHEST, "Save Draft", null, "Persist your terrain and datapoints"));
        inv.setItem(12, item(Material.COMPASS, "View Datapoints", null));
        inv.setItem(14, item(Material.BOOK, "Validate / Export", null));
        inv.setItem(16, item(Material.NETHER_STAR, "Publish Draft", null, "Requires validation and separate confirmation"));
        inv.setItem(29, item(Material.OAK_DOOR, "Save and Exit", null, "Return to where you opened the editor"));
        inv.setItem(31, item(Material.BARRIER, "Delete Draft", null, "Permanently delete this isolated working copy"));
        inv.setItem(33, item(Material.LIME_DYE, "Resume Editing", null));
        actions.put(10, p -> { editor.handle(p, new String[] {"save"}); p.closeInventory(); });
        actions.put(12, p -> markerGroups(p, false, 0));
        actions.put(14, this::reviewMenu);
        actions.put(16, p -> confirm(p, "Publish this map?", () -> {
            ArcadePortGame game = editor.activeGame(p);
            String resource = editor.activeResource(p);
            String draft = editor.activeDraft(p);
            if (game != null && resource != null && draft != null) {
                editor.handle(p, new String[] {"publish", game.id(), resource, draft, "confirm"});
            }
        }, () -> sessionMenu(p)));
        actions.put(29, p -> { p.closeInventory(); editor.handle(p, new String[] {"close"}); });
        actions.put(31, p -> confirm(p, "Delete this draft?", () -> {
            p.closeInventory();
            editor.handle(p, new String[] {"delete", "confirm"});
        }, () -> sessionMenu(p)));
        actions.put(33, Player::closeInventory);
        show(player, inv, actions);
    }

    private void confirm(Player player, String title, Runnable yes, Runnable no) {
        Inventory inv = menu(title);
        Map<Integer, Consumer<Player>> actions = new HashMap<>();
        inv.setItem(20, item(Material.LIME_CONCRETE, "Confirm", null));
        inv.setItem(24, item(Material.RED_CONCRETE, "Cancel", null));
        actions.put(20, p -> { p.closeInventory(); yes.run(); });
        actions.put(24, p -> no.run());
        show(player, inv, actions);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTool(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!editor.isEditing(player)) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (action == null) return;
        Action click = event.getAction();
        if (!click.isRightClick() && !click.isLeftClick()) return;
        event.setCancelled(true);
        switch (action) {
            case "markers" -> { if (click.isRightClick()) markerGroups(player, false, 0); }
            case "add" -> { if (click.isRightClick()) markerGroups(player, true, 0); }
            case "save" -> { if (click.isRightClick()) editor.handle(player, new String[] {"save"}); }
            case "review" -> { if (click.isRightClick()) reviewMenu(player); }
            case "session" -> { if (click.isRightClick()) sessionMenu(player); }
            case "bounds" -> {
                if (event.getClickedBlock() == null) {
                    player.sendMessage(ChatColor.YELLOW + "Click a block to choose a bounds corner.");
                    return;
                }
                Location loc = event.getClickedBlock().getLocation();
                if (click.isLeftClick()) {
                    firstBounds.put(player.getUniqueId(), loc);
                    player.sendMessage(ChatColor.GREEN + "First bounds corner selected. Right-click the opposite corner.");
                } else {
                    Location first = firstBounds.remove(player.getUniqueId());
                    if (first == null || first.getWorld() != loc.getWorld()) {
                        player.sendMessage(ChatColor.YELLOW + "Left-click the first corner before right-clicking the second.");
                        return;
                    }
                    editor.handle(player, new String[] {"bounds", "set",
                            Integer.toString(Math.min(first.getBlockX(), loc.getBlockX())),
                            Integer.toString(Math.max(first.getBlockX(), loc.getBlockX()) + 1),
                            Integer.toString(Math.min(first.getBlockY(), loc.getBlockY())),
                            Integer.toString(Math.max(first.getBlockY(), loc.getBlockY()) + 1),
                            Integer.toString(Math.min(first.getBlockZ(), loc.getBlockZ())),
                            Integer.toString(Math.max(first.getBlockZ(), loc.getBlockZ()) + 1)});
                }
            }
            default -> {}
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = menus.get(player.getUniqueId());
        if (menu == null || event.getView().getTopInventory() != menu.inventory()) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != menu.inventory()) return;
        Consumer<Player> action = menu.actions().get(event.getRawSlot());
        if (action != null) {
            if (event.isShiftClick()) shiftClicking.add(player.getUniqueId());
            try { action.accept(player); } finally { shiftClicking.remove(player.getUniqueId()); }
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Menu menu = menus.get(player.getUniqueId());
        if (menu != null && event.getView().getTopInventory() == menu.inventory()) event.setCancelled(true);
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        Menu menu = menus.get(event.getPlayer().getUniqueId());
        if (menu != null && event.getInventory() == menu.inventory()) {
            menus.remove(event.getPlayer().getUniqueId());
        }
    }

    void endSession(Player player) {
        firstBounds.remove(player.getUniqueId());
        menus.remove(player.getUniqueId());
        shiftClicking.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endSession(event.getPlayer());
    }
}
