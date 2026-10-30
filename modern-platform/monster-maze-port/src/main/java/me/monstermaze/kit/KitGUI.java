package me.monstermaze.kit;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.game.GameManager;
import me.monstermaze.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * In-round kit selection only. This is not a lobby menu and cannot launch games,
 * choose maps, change modes, or bypass Testplex Arcade's session lifecycle.
 */
public final class KitGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "Monster Maze Kits";
    private static final int[] SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};

    private final MonsterMazePlugin plugin;
    private final GameManager game;
    private final KitManager kits;

    public KitGUI(MonsterMazePlugin plugin, GameManager game, KitManager kits) {
        this.plugin = plugin;
        this.game = game;
        this.kits = kits;
        Bukkit.getPluginManager().registerEvents(this, plugin.getHostPlugin());
    }

    public void open(Player player) {
        if (player == null || !player.isOnline()) return;
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        ItemStack header = item(Material.NETHER_STAR, ChatColor.GOLD + "Choose your Monster Maze kit",
                List.of(ChatColor.GRAY + "Select a kit for this Testplex game session.",
                        ChatColor.GRAY + "The round is started and stopped by Testplex Arcade."));
        inventory.setItem(4, header);

        List<KitType> available = KitType.available(game.qolEnabled());
        KitType selected = kits.getKit(player);
        for (int i = 0; i < available.size() && i < SLOTS.length; i++) {
            KitType kit = available.get(i);
            List<String> lore = new ArrayList<>(java.util.Arrays.asList(kit.description));
            lore.add("");
            lore.add(kit == selected ? ChatColor.GREEN + "SELECTED" : ChatColor.YELLOW + "Click to select");
            inventory.setItem(SLOTS[i], item(kit.icon,
                    (kit == selected ? ChatColor.GREEN + "▶ " : ChatColor.WHITE) + kit.display, lore));
        }
        inventory.setItem(49, item(Material.BARRIER, ChatColor.RED + "Close", List.of()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (game.getState() == GameState.LIVE) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "The game is already running; kits cannot be changed now.");
            return;
        }

        List<KitType> available = KitType.available(game.qolEnabled());
        for (int i = 0; i < SLOTS.length && i < available.size(); i++) {
            if (slot != SLOTS[i]) continue;
            KitType selected = available.get(i);
            kits.setKit(player, selected);
            player.sendMessage(ChatColor.GREEN + "Selected " + selected.display + " for this Monster Maze session.");
            open(player);
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.CHEST : material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
