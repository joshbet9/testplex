package au.com.testplex.arcadeports;

import java.util.Comparator;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
}
