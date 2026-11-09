package au.com.testplex.arcadeports;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-viewer, Mineplex-style floating lobby leaderboards and their hotbar editor. */
final class ArcadeLobbyLeaderboardService implements Listener {
    private static final int MAX_BOARDS = 10;
    private static final int MAX_ROWS = 10;
    private static final String ROOT = "lobby-leaderboards.";
    private static final String[] METRICS = {"wins", "games-played", "losses", "test-gems-earned", "pb"};
    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final ModernArcadeLobby lobby;
    /** Each viewer has a different layout for *each selected lobby game*. */
    private final Map<LayoutKey, List<Board>> preferences = new HashMap<>();
    private final Map<UUID, List<Panel>> visible = new HashMap<>();
    private BukkitTask renderTask;
    private BukkitTask historyTask;
    private boolean historyDirty;
    private List<SourceMapCatalog.MapArchive> cachedDragonMaps;
    private long dragonMapsCacheUntil;

    enum Period {
        ALL_TIME("All-Time"), WEEKLY("Weekly"), DAILY("Daily"), AROUND_YOU("All-Time (Around You)");
        final String title;
        Period(String title) { this.title = title; }
    }

    private record LayoutKey(UUID viewer, String game) { }

    private static final class Board {
        String metric = "wins";
        Period period = Period.ALL_TIME;
        EnumSet<Period> periods = EnumSet.allOf(Period.class);
        String map = "";
        String kit = DragonEscapePersonalBestViews.BEST_TIME;
        boolean cycleMaps = true;
        boolean cycleKits = true;

        boolean isPb(String effectiveGame) {
            return "dragon-escape".equals(effectiveGame) && "pb".equals(metric);
        }
    }

    private static final class Panel {
        final List<TextDisplay> rows = new ArrayList<>();
        Interaction hitbox;
        int index;
        String lastContent = "";

        void remove() {
            for (TextDisplay display : rows) if (display.isValid()) display.remove();
            if (hitbox != null && hitbox.isValid()) hitbox.remove();
        }
    }

    private enum MenuType { ROOT, EDIT, MAP }

    private static final class Menu implements InventoryHolder {
        final MenuType type;
        final int board;
        final int page;
        final String game;
        Inventory inventory;
        Menu(MenuType type, int board, int page, String game) {
            this.type = type;
            this.board = board;
            this.page = page;
            this.game = game;
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    ArcadeLobbyLeaderboardService(JavaPlugin plugin, ArcadePortManager manager, ModernArcadeLobby lobby) {
        this.plugin = plugin;
        this.manager = manager;
        this.lobby = lobby;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void start() {
        if (renderTask != null) return;
        // Capture the baseline before the next match; older lifetime results must not inflate daily wins.
        captureHistory();
        renderTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 40L);
        historyTask = Bukkit.getScheduler().runTaskTimer(plugin, this::captureHistory, 200L, 200L);
    }

    void shutdown() {
        if (renderTask != null) renderTask.cancel();
        if (historyTask != null) historyTask.cancel();
        for (UUID id : List.copyOf(visible.keySet())) clear(id);
        preferences.clear();
        if (historyDirty) plugin.saveConfig();
    }

    void open(Player player) {
        if (!lobby.isLobbyWorld(player.getWorld())) {
            player.sendMessage(ChatColor.RED + "Leaderboard configuration is available in the Arcade lobby.");
            return;
        }
        openRoot(player);
    }

    private String selectedGame() {
        String selected = plugin.getConfig().getString("lobby-selected-game", "bacon-brawl");
        if (selected.startsWith("monster-maze")) return "monster-maze";
        if (selected.startsWith("skywars")) return "skywars";
        return ArcadeGameDirectory.find(selected) == null ? "bacon-brawl" : selected;
    }

    private String effectiveGame(Board board) {
        return selectedGame();
    }

    private List<Board> boards(UUID viewer) {
        return preferences.computeIfAbsent(new LayoutKey(viewer, selectedGame()), this::loadBoards);
    }

    private List<Board> loadBoards(LayoutKey key) {
        List<Board> results = new ArrayList<>();
        String path = ROOT + "players." + key.viewer() + ".games." + key.game() + ".boards";
        for (Map<?, ?> saved : plugin.getConfig().getMapList(path)) {
            if (results.size() >= MAX_BOARDS) break;
            Map<String, Object> data = new HashMap<>();
            saved.forEach((field, value) -> data.put(String.valueOf(field), value));
            Board board = new Board();
            String metric = String.valueOf(data.getOrDefault("metric", "wins"));
            board.metric = List.of(METRICS).contains(metric) ? metric : "wins";
            board.map = String.valueOf(data.getOrDefault("map", ""));
            board.kit = String.valueOf(data.getOrDefault("kit", DragonEscapePersonalBestViews.BEST_TIME));
            if (!DragonEscapePersonalBestViews.all().contains(board.kit))
                board.kit = DragonEscapePersonalBestViews.BEST_TIME;
            board.cycleMaps = Boolean.parseBoolean(String.valueOf(data.getOrDefault("cycleMaps", true)));
            board.cycleKits = Boolean.parseBoolean(String.valueOf(data.getOrDefault("cycleKits", true)));
            board.periods.clear();
            for (String name : String.valueOf(data.getOrDefault("periods", "ALL_TIME,WEEKLY,DAILY,AROUND_YOU")).split(",")) {
                try { board.periods.add(Period.valueOf(name)); }
                catch (IllegalArgumentException ignored) { }
            }
            if (board.periods.isEmpty()) board.periods.add(Period.ALL_TIME);
            try { board.period = Period.valueOf(String.valueOf(data.getOrDefault("period", "ALL_TIME"))); }
            catch (IllegalArgumentException ignored) { }
            if (!board.periods.contains(board.period)) board.period = board.periods.iterator().next();
            results.add(board);
        }
        if (results.isEmpty() && !plugin.getConfig().contains(path))
            results.add(new Board());
        return results;
    }

    private void save(UUID viewer) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Board board : boards(viewer)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("metric", board.metric);
            row.put("period", board.period.name());
            row.put("periods", String.join(",", board.periods.stream().map(Enum::name).toList()));
            row.put("map", board.map);
            row.put("kit", board.kit);
            row.put("cycleMaps", board.cycleMaps);
            row.put("cycleKits", board.cycleKits);
            serialized.add(row);
        }
        plugin.getConfig().set(ROOT + "players." + viewer + ".games." + selectedGame() + ".boards", serialized);
        plugin.saveConfig();
        clear(viewer);
    }

    private void clear(UUID viewer) {
        List<Panel> old = visible.remove(viewer);
        if (old != null) for (Panel panel : old) panel.remove();
    }

    private void refresh() {
        World world = Bukkit.getWorld(ModernArcadeLobby.WORLD_NAME);
        if (world == null) return;
        for (UUID id : List.copyOf(visible.keySet())) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer == null || viewer.getWorld() != world) clear(id);
        }
        for (Player viewer : world.getPlayers()) render(viewer, world);
    }

    private void render(Player viewer, World world) {
        List<Board> layouts = boards(viewer.getUniqueId());
        List<Panel> panels = visible.computeIfAbsent(viewer.getUniqueId(), ignored -> new ArrayList<>());
        if (panels.size() != layouts.size()) {
            clear(viewer.getUniqueId());
            panels = new ArrayList<>();
            visible.put(viewer.getUniqueId(), panels);
            for (int i = 0; i < layouts.size(); i++) panels.add(spawnPanel(viewer, world, i, layouts.size()));
        }
        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            String content = format(viewer, layouts.get(i));
            if (content.equals(panel.lastContent)) continue;
            panel.lastContent = content;
            String[] lines = content.split("\n", -1);
            for (int line = 0; line < panel.rows.size(); line++) {
                panel.rows.get(line).setText(line < lines.length ? lines[line] : "");
            }
        }
    }

    private Panel spawnPanel(Player viewer, World world, int index, int count) {
        Panel panel = new Panel();
        panel.index = index;
        double x = (index - (count - 1) / 2.0D) * 9.2D;
        double z = -28.0D;
        for (int line = 0; line < 15; line++) {
            Location at = new Location(world, x, 109.0D - line * 0.55D, z);
            TextDisplay display = world.spawn(at, TextDisplay.class, entity -> {
                entity.setVisibleByDefault(false);
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setGravity(false);
                entity.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                entity.setDefaultBackground(true);
                entity.setSeeThrough(true);
                entity.setShadowed(true);
                entity.setLineWidth(450);
                entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                        new Vector3f(0.65F, 0.65F, 0.65F), new Quaternionf()));
            });
            viewer.showEntity(plugin, display);
            panel.rows.add(display);
        }
        panel.hitbox = world.spawn(new Location(world, x, 104.0D, z), Interaction.class, entity -> {
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
            entity.setInteractionWidth(7.8F);
            entity.setInteractionHeight(10.5F);
            entity.setResponsive(true);
        });
        viewer.showEntity(plugin, panel.hitbox);
        return panel;
    }

    private List<SourceMapCatalog.MapArchive> dragonMaps() {
        long now = System.currentTimeMillis();
        if (cachedDragonMaps == null || now >= dragonMapsCacheUntil) {
            cachedDragonMaps = SourceMapCatalog.mapsFor(plugin, ArcadePortGame.DRAGON_ESCAPE);
            dragonMapsCacheUntil = now + 60_000L;
        }
        return cachedDragonMaps;
    }

    private String mapLabel(Board board) {
        return dragonMaps().stream().filter(m -> m.resource().equals(board.map))
                .map(SourceMapCatalog.MapArchive::displayName).findFirst().orElse("No maps available");
    }

    private void normalizeMap(Board board) {
        if (dragonMaps().stream().noneMatch(m -> m.resource().equals(board.map)) && !dragonMaps().isEmpty())
            board.map = dragonMaps().getFirst().resource();
    }

    private String format(Player viewer, Board board) {
        String game = effectiveGame(board);
        boolean pb = board.isPb(game);
        if (pb) normalizeMap(board);
        String name = ArcadeGameDirectory.find(game).name();
        String subtitle = pb ? "Best Time: " + mapLabel(board) + " / "
                + DragonEscapePersonalBestViews.name(board.kit)
                : switch (board.metric) {
                    case "games-played" -> "Most Games Played";
                    case "losses" -> "Most Losses";
                    case "test-gems-earned" -> "Most Gems Earned";
                    default -> "Most Wins";
                };
        if (pb) subtitle += board.period == Period.AROUND_YOU ? " (Around You)" : "";
        Map<UUID, Long> data = pb ? manager.lobbyDragonEscapeTimes(board.map, board.kit)
                : (board.period == Period.ALL_TIME || board.period == Period.AROUND_YOU)
                ? manager.lobbyLeaderboardScores(game, board.metric)
                : historicalScores(game, board.metric, board.period);
        List<ArcadeLobbyLeaderboardRankings.Entry> ranked = ArcadeLobbyLeaderboardRankings.rank(data, pb);
        List<ArcadeLobbyLeaderboardRankings.Entry> rows = ArcadeLobbyLeaderboardRankings.visible(
                ranked, viewer.getUniqueId(), board.period == Period.AROUND_YOU, MAX_ROWS);
        StringBuilder out = new StringBuilder(ChatColor.AQUA + (pb ? "All-Time " : board.period.title + " ")
                + name + "\n" + ChatColor.GRAY + subtitle);
        for (ArcadeLobbyLeaderboardRankings.Entry entry : rows) {
            Player online = Bukkit.getPlayer(entry.player());
            String playerName = online != null ? online.getName() : Bukkit.getOfflinePlayer(entry.player()).getName();
            if (playerName == null) playerName = entry.player().toString().substring(0, 8);
            ChatColor color = entry.player().equals(viewer.getUniqueId()) ? ChatColor.GREEN : ChatColor.AQUA;
            out.append('\n').append(ChatColor.AQUA).append('#').append(entry.rank())
                    .append(ChatColor.GRAY).append(" - ").append(color).append(playerName)
                    .append(ChatColor.GRAY).append(" - ").append(ChatColor.YELLOW)
                    .append(pb ? ArcadePortRules.formatDragonEscapeTime(entry.value()) : entry.value());
        }
        // Stable footer position, including when fewer than ten records exist.
        for (int missing = rows.size(); missing < MAX_ROWS; missing++) out.append('\n');
        ArcadeLobbyLeaderboardRankings.Entry own = ranked.stream()
                .filter(e -> e.player().equals(viewer.getUniqueId())).findFirst().orElse(null);
        out.append('\n').append(ChatColor.GREEN).append("You: ")
                .append(own == null ? ChatColor.GRAY + "Unranked" : ChatColor.WHITE + "#" + own.rank()
                        + ChatColor.GRAY + " - " + ChatColor.YELLOW
                        + (pb ? ArcadePortRules.formatDragonEscapeTime(own.value()) : own.value()));
        out.append('\n').append(ChatColor.YELLOW).append("Punch to view the")
                .append('\n').append(ChatColor.YELLOW).append("next category");
        return out.toString();
    }

    private String day() { return LocalDate.now(java.time.ZoneOffset.UTC).toString(); }
    private String week() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        return today.get(WeekFields.ISO.weekBasedYear()) + "-W"
                + String.format(Locale.ROOT, "%02d", today.get(WeekFields.ISO.weekOfWeekBasedYear()));
    }

    private String historicPath(Period period, String game, String metric, UUID id) {
        return ROOT + (period == Period.DAILY ? "daily." + day() : "weekly." + week())
                + "." + game + "." + metric + "." + id;
    }

    private Map<UUID, Long> historicalScores(String game, String metric, Period period) {
        Map<UUID, Long> data = new HashMap<>();
        for (UUID id : manager.lobbyLeaderboardScores(game, metric).keySet()) {
            long value = plugin.getConfig().getLong(historicPath(period, game, metric, id));
            if (value > 0L) data.put(id, value);
        }
        return data;
    }

    private void captureHistory() {
        FileConfiguration config = plugin.getConfig();
        for (ArcadeGameDirectory.Game game : ArcadeGameDirectory.games()) {
            for (String metric : List.of("wins", "games-played", "losses", "test-gems-earned")) {
                for (var entry : manager.lobbyLeaderboardScores(game.id(), metric).entrySet()) {
                    String observed = ROOT + "observed." + game.id() + "." + metric + "." + entry.getKey();
                    long total = entry.getValue();
                    if (config.contains(observed)) {
                        long delta = Math.max(0L, total - config.getLong(observed));
                        if (delta > 0L) {
                            for (Period period : List.of(Period.DAILY, Period.WEEKLY)) {
                                String path = historicPath(period, game.id(), metric, entry.getKey());
                                config.set(path, config.getLong(path) + delta);
                            }
                        }
                    }
                    if (!config.contains(observed) || config.getLong(observed) != total) {
                        config.set(observed, total);
                        historyDirty = true;
                    }
                }
            }
        }
        if (historyDirty) {
            plugin.saveConfig();
            historyDirty = false;
        }
    }

    private void advance(Player viewer, int index) {
        List<Board> all = boards(viewer.getUniqueId());
        if (index < 0 || index >= all.size()) return;
        Board board = all.get(index);
        if (board.isPb(effectiveGame(board))) {
            normalizeMap(board);
            List<String> kits = DragonEscapePersonalBestViews.all();
            int kitIndex = Math.max(0, kits.indexOf(board.kit));
            if (board.cycleKits) board.kit = kits.get((kitIndex + 1) % kits.size());
            if (board.cycleMaps && (!board.cycleKits || kitIndex + 1 >= kits.size())) {
                List<SourceMapCatalog.MapArchive> maps = dragonMaps();
                for (int i = 0; i < maps.size(); i++) {
                    if (maps.get(i).resource().equals(board.map)) {
                        board.map = maps.get((i + 1) % maps.size()).resource();
                        break;
                    }
                }
            }
            if (!board.cycleKits && !board.cycleMaps) board.period = board.period == Period.AROUND_YOU
                    ? Period.ALL_TIME : Period.AROUND_YOU;
        } else {
            List<Period> periods = List.copyOf(board.periods);
            board.period = periods.get((periods.indexOf(board.period) + 1) % periods.size());
        }
        save(viewer.getUniqueId());
        render(viewer, viewer.getWorld());
    }

    private int findPanel(Player viewer, Entity entity) {
        List<Panel> panels = visible.get(viewer.getUniqueId());
        if (panels == null) return -1;
        for (Panel panel : panels) if (panel.hitbox == entity) return panel.index;
        return -1;
    }

    @EventHandler public void onPunch(PrePlayerAttackEntityEvent event) {
        int index = findPanel(event.getPlayer(), event.getAttacked());
        if (index < 0) return;
        event.setCancelled(true);
        advance(event.getPlayer(), index);
    }

    @EventHandler public void onRightClick(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        int index = findPanel(event.getPlayer(), event.getRightClicked());
        if (index < 0) return;
        event.setCancelled(true);
        advance(event.getPlayer(), index);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
        preferences.keySet().removeIf(key -> key.viewer().equals(event.getPlayer().getUniqueId()));
    }

    private static ItemStack item(Material material, String title, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(List.of(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private Inventory inventory(MenuType type, int board, int page, String title) {
        Menu holder = new Menu(type, board, page, selectedGame());
        holder.inventory = Bukkit.createInventory(holder, 54, title);
        return holder.inventory;
    }

    private void openRoot(Player player) {
        String selected = selectedGame();
        Inventory inv = inventory(MenuType.ROOT, -1, 0,
                ArcadeGameDirectory.find(selected).name() + " Leaderboards");
        inv.setItem(0, item(Material.RED_BED, ChatColor.RED + "Close",
                ChatColor.GRAY + "Return to the lobby."));
        List<Board> all = boards(player.getUniqueId());
        for (int i = 0; i < all.size(); i++) {
            Board board = all.get(i);
            String game = effectiveGame(board);
            inv.setItem(10 + i, item(board.isPb(game) ? Material.CLOCK : ArcadeGameDirectory.find(game).icon(),
                    ChatColor.AQUA + "Board " + (i + 1) + ": " + ArcadeGameDirectory.find(game).name(),
                    ChatColor.GRAY + "Only affects the currently selected lobby game.",
                    ChatColor.YELLOW + "Metric: " + board.metric,
                    ChatColor.YELLOW + "Current: " + (board.isPb(game) ? mapLabel(board) : board.period.title),
                    ChatColor.GREEN + "Click to configure this board."));
        }
        inv.setItem(31, item(all.size() < MAX_BOARDS ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.GREEN + "Add Leaderboard (" + all.size() + "/10)",
                ChatColor.GRAY + "Adds a Mineplex-format floating board beside the others.",
                all.size() < MAX_BOARDS ? ChatColor.YELLOW + "Click to add" : ChatColor.RED + "Maximum 10 boards"));
        inv.setItem(49, item(Material.BOOK, ChatColor.YELLOW + "Punch a floating board to cycle",
                ChatColor.GRAY + "Rankings and layouts are private to each viewer.",
                ChatColor.GRAY + "Left-click or right-click the board to advance."));
        player.openInventory(inv);
    }

    private void openEdit(Player player, int index) {
        List<Board> all = boards(player.getUniqueId());
        if (index < 0 || index >= all.size()) return;
        Board board = all.get(index);
        String game = effectiveGame(board);
        boolean pb = board.isPb(game);
        Inventory inv = inventory(MenuType.EDIT, index, 0, "Leaderboard " + (index + 1) + " Settings");
        inv.setItem(0, item(Material.RED_BED, ChatColor.GRAY + "Back to Leaderboards"));
        inv.setItem(10, item(ArcadeGameDirectory.find(game).icon(), ChatColor.AQUA + "Game: "
                + ArcadeGameDirectory.find(game).name(),
                ChatColor.GRAY + "Follows the currently selected lobby game.",
                ChatColor.YELLOW + "This game's settings are independent."));
        inv.setItem(12, item(pb ? Material.CLOCK : Material.PAPER,
                ChatColor.AQUA + "Metric: " + board.metric,
                ChatColor.GRAY + "Wins, Games Played, Losses, Gems",
                ChatColor.GRAY + "or Dragon Escape personal bests.",
                ChatColor.YELLOW + "Click to change metric."));
        inv.setItem(14, item(Material.FILLED_MAP,
                ChatColor.AQUA + "Dragon Escape map: " + (pb ? mapLabel(board) : "N/A"),
                ChatColor.YELLOW + (pb ? "Click to pick a map." : "Select the Dragon Escape PB metric.")));
        inv.setItem(16, item(DragonEscapePersonalBestViews.icon(board.kit),
                ChatColor.AQUA + "Kit: " + DragonEscapePersonalBestViews.name(board.kit),
                ChatColor.YELLOW + (pb ? "Click to select a kit (or overall best)." : "Dragon Escape PB only.")));
        inv.setItem(20, item(board.cycleMaps ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.AQUA + "Cycle PB maps: " + (board.cycleMaps ? "ON" : "OFF"),
                ChatColor.GRAY + "Punch to advance to the next map."));
        inv.setItem(22, item(board.cycleKits ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.AQUA + "Cycle PB kits: " + (board.cycleKits ? "ON" : "OFF"),
                ChatColor.GRAY + "Punch to advance to the next kit."));
        int[] periodSlots = {28, 29, 30, 31};
        int p = 0;
        for (Period period : Period.values()) {
            boolean enabled = board.periods.contains(period);
            inv.setItem(periodSlots[p++], item(enabled ? Material.LIME_DYE : Material.RED_DYE,
                    (enabled ? ChatColor.GREEN : ChatColor.RED) + period.title,
                    ChatColor.GRAY + "Punch cycles only through enabled categories.",
                    ChatColor.YELLOW + "Click to " + (enabled ? "disable" : "enable") + ".",
                    board.period == period ? ChatColor.GOLD + "Currently displaying" : ""));
        }
        inv.setItem(38, item(Material.ARROW, ChatColor.YELLOW + "Move board left"));
        inv.setItem(40, item(Material.ARROW, ChatColor.YELLOW + "Move board right"));
        inv.setItem(42, item(Material.BARRIER, ChatColor.RED + "Remove this board"));
        inv.setItem(49, item(Material.RED_BED, ChatColor.GRAY + "Back to Leaderboards"));
        player.openInventory(inv);
    }

    private void openMapPicker(Player player, int index, int requestedPage) {
        List<SourceMapCatalog.MapArchive> maps = dragonMaps();
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (maps.size() - 1) / 45)));
        Inventory inv = inventory(MenuType.MAP, index, page, "Choose Dragon Escape Map");
        for (int i = page * 45; i < Math.min(maps.size(), (page + 1) * 45); i++)
            inv.setItem(i - page * 45, item(Material.PAPER,
                    ChatColor.AQUA + maps.get(i).displayName(),
                    ChatColor.GRAY + "Click to use this map for PB rankings."));
        if (page > 0) inv.setItem(45, item(Material.ARROW, ChatColor.YELLOW + "Previous"));
        if ((page + 1) * 45 < maps.size()) inv.setItem(53, item(Material.ARROW, ChatColor.YELLOW + "Next"));
        inv.setItem(49, item(Material.RED_BED, ChatColor.GRAY + "Back"));
        player.openInventory(inv);
    }

    @EventHandler public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        if (!menu.game.equals(selectedGame())) { openRoot(player); return; }
        UUID id = player.getUniqueId();
        List<Board> all = boards(id);
        if (menu.type == MenuType.ROOT) {
            if (slot == 0) player.closeInventory();
            else if (slot >= 10 && slot < 10 + all.size()) openEdit(player, slot - 10);
            else if (slot == 31 && all.size() < MAX_BOARDS) {
                all.add(new Board());
                save(id);
                openEdit(player, all.size() - 1);
            }
            return;
        }
        if (menu.board < 0 || menu.board >= all.size()) { openRoot(player); return; }
        Board board = all.get(menu.board);
        if (menu.type == MenuType.MAP) {
            if (slot == 49) { openEdit(player, menu.board); return; }
            if (slot == 45 && menu.page > 0) { openMapPicker(player, menu.board, menu.page - 1); return; }
            if (slot == 53) { openMapPicker(player, menu.board, menu.page + 1); return; }
            int index = menu.page * 45 + slot;
            List<SourceMapCatalog.MapArchive> maps = dragonMaps();
            if (slot < 45 && index < maps.size()) {
                board.map = maps.get(index).resource();
                save(id);
                openEdit(player, menu.board);
            }
            return;
        }
        if (slot == 0 || slot == 49) { openRoot(player); return; }
        boolean pb = board.isPb(effectiveGame(board));
        if (slot == 10) return;
        if (slot == 12) {
            int index = List.of(METRICS).indexOf(board.metric);
            do { board.metric = METRICS[(++index) % METRICS.length]; }
            while ("pb".equals(board.metric) && !"dragon-escape".equals(effectiveGame(board)));
        } else if (slot == 14 && pb) { openMapPicker(player, menu.board, 0); return; }
        else if (slot == 16 && pb) {
            var kits = DragonEscapePersonalBestViews.all();
            board.kit = kits.get((Math.max(0, kits.indexOf(board.kit)) + 1) % kits.size());
        } else if (slot == 20 && pb) board.cycleMaps = !board.cycleMaps;
        else if (slot == 22 && pb) board.cycleKits = !board.cycleKits;
        else if (slot >= 28 && slot <= 31) {
            Period period = Period.values()[slot - 28];
            if (board.periods.contains(period) && board.periods.size() > 1) board.periods.remove(period);
            else board.periods.add(period);
            if (!board.periods.contains(board.period)) board.period = board.periods.iterator().next();
        } else if (slot == 38 && menu.board > 0) {
            java.util.Collections.swap(all, menu.board, menu.board - 1);
            save(id);
            openEdit(player, menu.board - 1);
            return;
        } else if (slot == 40 && menu.board + 1 < all.size()) {
            java.util.Collections.swap(all, menu.board, menu.board + 1);
            save(id);
            openEdit(player, menu.board + 1);
            return;
        } else if (slot == 42) {
            all.remove(menu.board);
            // Allow zero configured boards: a player can choose to hide all of them.
            save(id);
            openRoot(player);
            return;
        } else return;
        save(id);
        openEdit(player, menu.board);
    }

    @EventHandler public void onEditorDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
}
