package au.com.testplex.arcadeports;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Turf Wars' terrain-material and Shredder ammunition handling. */
final class TurfWarsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.TURF_WARS; }

    boolean isWool(Material material) {
        return material != null && material.name().endsWith("_WOOL");
    }

    boolean isTerracotta(Material material) {
        return material != null && material.name().endsWith("_TERRACOTTA");
    }

    int countFletchedArrows(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.ARROW || !stack.hasItemMeta()) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName()
                    && ChatColor.stripColor(meta.getDisplayName()).contains("Fletched Arrow")) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    boolean isFletchedArrow(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ARROW || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).contains("Fletched Arrow");
    }

    double barrageSpread(double spread) {
        return spread <= 0.0D ? 0.0D : ThreadLocalRandom.current().nextDouble(-spread, spread);
    }
}
