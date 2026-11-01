package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Player-facing Like/Dislike/Neutral menu and live selection-likelihood display. */
public final class GamePreferenceMenu implements Listener {
    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "Game Preferences";
    private static final int PAGE_SIZE = 36;
    private static final int FIRST_GAME_SLOT = 9;

    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final GamePreferenceStore store;
    private final NamespacedKey gameKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey pageKey;

    public GamePreferenceMenu(JavaPlugin plugin, ArcadePortManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.store = new GamePreferenceStore(plugin);
        this.gameKey = new NamespacedKey(plugin, "game-preference-game");
        this.actionKey = new NamespacedKey(plugin, "game-preference-action");
        this.pageKey = new NamespacedKey(plugin, "game-preference-page");
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int requestedPage) {
        List<ArcadeGameDirectory.Game> games = ArcadeGameDirectory.games();
        List<Player> eligiblePlayers = eligiblePlayers();
        int eligibleCount = eligiblePlayers.size();

        Set<String> clearedGames = new LinkedHashSet<>(plugin.getConfig().getStringList("enabled-games"));
        String poolPath = "game-preferences.automatic-rotation.enabled-games";
        List<String> configuredPool = plugin.getConfig().contains(poolPath)
                ? plugin.getConfig().getStringList(poolPath)
                : plugin.getConfig().getStringList("enabled-games");
        Set<String> rotationPool = new LinkedHashSet<>(configuredPool);

        Map<String, Double> baseWeights = new LinkedHashMap<>();
        Map<String, GamePreferenceWeights.Counts> counts = new LinkedHashMap<>();
        for (ArcadeGameDirectory.Game game : games) {
            int likes = 0;
            int dislikes = 0;
            for (Player eligible : eligiblePlayers) {
                GamePreferenceStore.Preference preference =
                        store.getPreference(eligible.getUniqueId(), game.id());
                if (preference == GamePreferenceStore.Preference.LIKE) likes++;
                if (preference == GamePreferenceStore.Preference.DISLIKE) dislikes++;
            }
            counts.put(game.id(), new GamePreferenceWeights.Counts(likes, dislikes));

            if (clearedGames.contains(game.id()) && rotationPool.contains(game.id())
                    && rotationCandidateAvailable(game.id())) {
                baseWeights.put(game.id(), configuredBaseWeight(game.id()));
            }
        }

        Map<String, Double> probabilities = GamePreferenceWeights.selectionProbabilities(
                baseWeights, counts, eligibleCount);
        boolean avoidConsecutive = plugin.getConfig().getBoolean(
                "game-preferences.automatic-rotation.avoid-consecutive-games", true);
        if (!probabilities.isEmpty()) {
            probabilities = GamePreferenceRotation.effectiveProbabilities(
                    probabilities, manager.previousRotationGameId(), avoidConsecutive);
        }

        int pageCount = Math.max(1, (games.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        Inventory inventory = Bukkit.createInventory(null, 54, MENU_TITLE);

        inventory.setItem(4, createItem(Material.WRITABLE_BOOK,
                ChatColor.GREEN + "Rate the games",
                ChatColor.GRAY + "Your preferences adjust each game's weighted selection likelihood.",
                ChatColor.GREEN + "Left-click: Like",
                ChatColor.RED + "Right-click: Dislike",
                ChatColor.YELLOW + "Shift-click: Neutral",
                ChatColor.DARK_GRAY + "Neutral is the default for unrated games.",
                ChatColor.DARK_GRAY + "Only players with at least one saved Like/Dislike are counted.",
                ChatColor.DARK_GRAY + "Games outside the approved rotation pool remain visible but are not weighted.",
                ChatColor.GRAY + "Eligible lobby voters: " + ChatColor.WHITE + eligibleCount,
                ChatColor.GRAY + "Selection likelihoods update after every rating."));

        if (games.isEmpty()) {
            inventory.setItem(22, createItem(Material.BARRIER,
                    ChatColor.RED + "No Games in the Directory",
                    ChatColor.GRAY + "The shared Arcade game directory is empty."));
        } else {
            int start = page * PAGE_SIZE;
            int end = Math.min(games.size(), start + PAGE_SIZE);
            for (int index = start; index < end; index++) {
                ArcadeGameDirectory.Game game = games.get(index);
                GamePreferenceWeights.Counts gameCounts = counts.get(game.id());
                GamePreferenceStore.Preference ownPreference =
                        store.getPreference(player.getUniqueId(), game.id());
                boolean inRotation = baseWeights.containsKey(game.id());
                double chance = probabilities.getOrDefault(game.id(), 0.0D);
                int neutralCount = Math.max(0,
                        eligibleCount - gameCounts.likes() - gameCounts.dislikes());
                inventory.setItem(FIRST_GAME_SLOT + index - start,
                        gameItem(game, ownPreference, gameCounts, neutralCount,
                                eligibleCount, configuredBaseWeight(game.id()), chance,
                                inRotation, clearedGames.contains(game.id()), rotationPool.contains(game.id())));
            }
        }

        ItemStack pageInfo = createItem(Material.MAP,
                ChatColor.AQUA + "Page " + (page + 1) + "/" + pageCount,
                ChatColor.GRAY.toString() + games.size() + " Arcade games in alphabetical order.",
                ChatColor.GRAY + "Monster Maze's Original, Speed and Modern modes share one preference.");
        markAction(pageInfo, "page-info", page);
        inventory.setItem(49, pageInfo);

        if (page > 0) {
            ItemStack previous = createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page");
            markAction(previous, "previous-page", page - 1);
            inventory.setItem(45, previous);
        }
        if (page + 1 < pageCount) {
            ItemStack next = createItem(Material.ARROW, ChatColor.YELLOW + "Next Page");
            markAction(next, "next-page", page + 1);
            inventory.setItem(53, next);
        }
        player.openInventory(inventory);
    }

    private ItemStack gameItem(
            ArcadeGameDirectory.Game game,
            GamePreferenceStore.Preference ownPreference,
            GamePreferenceWeights.Counts counts,
            int neutralCount,
            int eligiblePlayers,
            double baseWeight,
            double probability,
            boolean inRotation,
            boolean cleared,
            boolean selectedForRotation) {
        String chance = inRotation
                ? String.format(Locale.ROOT, "%.1f%%", probability * 100.0D)
                : "Not in rotation";
        String base = String.format(Locale.ROOT, "%.2f", baseWeight);
        ChatColor nameColour = switch (ownPreference) {
            case LIKE -> ChatColor.GREEN;
            case DISLIKE -> ChatColor.RED;
            case NEUTRAL -> ChatColor.YELLOW;
        };
        ItemStack item = new ItemStack(game.icon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(nameColour + game.name());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Selection likelihood: " + ChatColor.GREEN + chance);
        lore.add(ChatColor.GRAY + "Your preference: " + preferenceLabel(ownPreference));
        lore.add(ChatColor.DARK_GRAY + "Likes: " + ChatColor.GREEN + counts.likes()
                + ChatColor.DARK_GRAY + " | Dislikes: " + ChatColor.RED + counts.dislikes()
                + ChatColor.DARK_GRAY + " | Neutral: " + ChatColor.GRAY + neutralCount);
        lore.add(ChatColor.GRAY + "Eligible lobby voters: " + ChatColor.WHITE + eligiblePlayers);
        lore.add(ChatColor.GRAY + "Acceptance: "
                + (cleared ? ChatColor.GREEN + "Cleared" : ChatColor.YELLOW + "Not yet cleared"));
        lore.add(ChatColor.GRAY + "Rotation pool: "
                + (selectedForRotation ? ChatColor.GREEN + "Included" : ChatColor.RED + "Excluded"));
        if (inRotation) lore.add(ChatColor.GRAY + "Base selection weight: " + ChatColor.WHITE + base);
        lore.add("");
        lore.add(ChatColor.GREEN + "Left-click to Like");
        lore.add(ChatColor.RED + "Right-click to Dislike");
        lore.add(ChatColor.YELLOW + "Shift-click to set Neutral");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(gameKey, PersistentDataType.STRING, game.id());
        item.setItemMeta(meta);
        return item;
    }

    private boolean rotationCandidateAvailable(String gameId) {
        ArcadePortGame game = ArcadePortGame.fromId(gameId);
        return game != null && manager.isRotationCandidateAvailable(game);
    }

    private double configuredBaseWeight(String gameId) {
        double weight = plugin.getConfig().getDouble(
                "game-preferences.base-weights." + gameId, 1.0D);
        return Double.isFinite(weight) && weight > 0.0D ? weight : 0.0D;
    }

    /** Only human lobby players with a saved non-neutral rating are voters. */
    private List<Player> eligiblePlayers() {
        List<Player> eligible = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()
                    || !ModernArcadeLobby.WORLD_NAME.equals(player.getWorld().getName())) continue;
            if (manager.isTestPlayer(player) || !store.hasConfiguredPreferences(player.getUniqueId())) continue;
            eligible.add(player);
        }
        return eligible;
    }

    private String preferenceLabel(GamePreferenceStore.Preference preference) {
        return switch (preference) {
            case LIKE -> ChatColor.GREEN + "Like";
            case DISLIKE -> ChatColor.RED + "Dislike";
            case NEUTRAL -> ChatColor.GRAY + "Neutral";
        };
    }

    private ItemStack createItem(Material material, String title, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(List.of(loreLines));
        item.setItemMeta(meta);
        return item;
    }

    private void markAction(ItemStack item, String action, int page) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!MENU_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir() || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if ("previous-page".equals(action) || "next-page".equals(action)) {
            Integer targetPage = meta.getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER);
            if (targetPage != null) open(player, targetPage);
            return;
        }

        String gameId = meta.getPersistentDataContainer().get(gameKey, PersistentDataType.STRING);
        if (ArcadeGameDirectory.find(gameId) == null) return;

        GamePreferenceStore.Preference preference;
        ClickType click = event.getClick();
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT) {
            preference = GamePreferenceStore.Preference.NEUTRAL;
        } else if (click == ClickType.LEFT) {
            preference = GamePreferenceStore.Preference.LIKE;
        } else if (click == ClickType.RIGHT) {
            preference = GamePreferenceStore.Preference.DISLIKE;
        } else {
            return;
        }
        store.setPreference(player.getUniqueId(), gameId, preference);

        ItemStack pageInfo = event.getView().getTopInventory().getItem(49);
        int page = 0;
        if (pageInfo != null && pageInfo.hasItemMeta()) {
            Integer currentPage = pageInfo.getItemMeta().getPersistentDataContainer()
                    .get(pageKey, PersistentDataType.INTEGER);
            if (currentPage != null) page = currentPage;
        }
        open(player, page);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (MENU_TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }
}
