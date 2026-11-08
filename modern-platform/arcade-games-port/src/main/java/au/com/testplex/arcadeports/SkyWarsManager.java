package au.com.testplex.arcadeports;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** SkyWars' kit-item identity, ability cooldown bar and spectator target selection. */
final class SkyWarsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.SKY_WARS; }

    boolean isSkill(ItemStack item, String name) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return name.equalsIgnoreCase(ChatColor.stripColor(item.getItemMeta().getDisplayName()));
    }

    boolean isAbilitySkill(ItemStack item) {
        return isSkill(item, "Ice Bridge") || isSkill(item, "Fire Burst")
                || isSkill(item, "Eye of Ender") || isSkill(item, "Magnet")
                || isSkill(item, "Throwable Dirt");
    }

    void updateAbilityBar(Player player, SessionState session, long now) {
        long readyAt = session.skyWarsKitReadyAt.getOrDefault(player.getUniqueId(), 0L);
        long duration = cooldownDuration(session.skyWarsKits.getOrDefault(player.getUniqueId(), "ice"));
        if (readyAt <= now) {
            player.setExp(1.0F);
            return;
        }
        player.setExp((float) Math.max(0.0D, Math.min(1.0D, 1.0D - (readyAt - now) / (double) duration)));
    }

    static long cooldownDuration(String kit) {
        return switch (kit) {
            case "fire", "ice" -> 30_000L;
            case "metal" -> 15_000L;
            case "air" -> 25_000L;
            default -> 500L;
        };
    }

    ItemStack kitItem(Material material, String title, String lore, boolean glowing) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + title);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            if (glowing) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    Player nearestTarget(SessionState session, Player player) {
        return session.alive.stream()
                .map(session.players::get)
                .filter(target -> target != null && target.isOnline())
                .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                .min(Comparator.comparingDouble(target ->
                        target.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
    }

    void openCompassMenu(SessionState session, Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GREEN + "SkyWars Targets");
        int slot = 0;
        for (UUID playerId : session.alive) {
            Player target = session.players.get(playerId);
            if (target == null || !target.isOnline() || target.equals(player) || slot >= menu.getSize()) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.YELLOW + target.getName());
                meta.setLore(List.of(ChatColor.GRAY + "Click to spectate this player."));
                head.setItemMeta(meta);
            }
            menu.setItem(slot++, head);
        }
        if (slot == 0) player.sendMessage(ChatColor.YELLOW + "There are no living players to track.");
        else player.openInventory(menu);
    }

    void updateCompass(SessionState session) {
        if (session.ticks % 5 != 0) return;
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline() || player.getWorld() != session.world) continue;
            Player target = nearestTarget(session, player);
            if (target == null) continue;
            if (!player.getInventory().contains(Material.COMPASS)) {
                player.getInventory().addItem(trackingCompassItem());
            }
            player.setCompassTarget(target.getLocation());
            if (isTrackingCompass(player.getInventory().getItemInMainHand())) {
                double distance = Math.sqrt(player.getLocation().distanceSquared(target.getLocation()));
                double height = target.getLocation().getY() - player.getLocation().getY();
                player.sendActionBar(ChatColor.WHITE + "Nearest Target: " + ChatColor.YELLOW + target.getName()
                        + ChatColor.WHITE + " | Distance: " + ChatColor.YELLOW
                        + String.format(Locale.ROOT, "%.1f", distance)
                        + ChatColor.WHITE + " | Height: " + ChatColor.YELLOW
                        + String.format(Locale.ROOT, "%.1f", height));
            }
        }
    }

    private ItemStack trackingCompassItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY
                    + "Tracks the nearest living player. Left-click to spectate when eliminated."));
            item.setItemMeta(meta);
        }
        return item;
    }
}
