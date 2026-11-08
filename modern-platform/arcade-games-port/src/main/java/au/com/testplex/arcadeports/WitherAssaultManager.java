package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Wither Assault's editor block safety, tunnel recognition and revival item validation. */
final class WitherAssaultManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.WITHER_ASSAULT; }

    boolean isRevivalPotion(ItemStack item) {
        if (item == null || item.getType() != Material.SPLASH_POTION || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("Revival Potion");
    }

    boolean isBlockUnderAnotherPlayer(SessionState session, Block block, Player editor) {
        for (UUID id : session.alive) {
            if (id.equals(editor.getUniqueId())) continue;
            Player other = session.players.get(id);
            if (other == null || !other.isOnline() || other.getWorld() != session.world) continue;
            Block feet = other.getLocation().getBlock();
            if (feet.equals(block) || feet.getRelative(0, -1, 0).equals(block)) return true;
        }
        return false;
    }

    void restoreEditorBlocks(SessionState session) {
        if (session == null) return;
        for (Map.Entry<Block, BlockData> entry : new HashMap<>(session.witherEditorReplacedBlocks).entrySet()) {
            Block block = entry.getKey();
            if (block != null && block.getWorld() == session.world && entry.getValue() != null) {
                block.setBlockData(entry.getValue(), false);
            }
        }
        session.witherEditorBlocks.clear();
        session.witherEditorReplacedBlocks.clear();
        session.witherEditorBlockOwners.clear();
    }

    boolean isEditorTunnelPosition(SessionState session, Location location, UUID editorId) {
        if (location == null || location.getWorld() != session.world) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        int nearbyBlocks = 0;
        boolean overhead = false;
        for (Map.Entry<Block, UUID> entry : session.witherEditorBlockOwners.entrySet()) {
            if (!editorId.equals(entry.getValue())) continue;
            Block placed = entry.getKey();
            if (!session.witherEditorBlocks.contains(placed) || placed.getType().isAir()) continue;
            if (Math.abs(placed.getX() - x) > 2 || Math.abs(placed.getZ() - z) > 2
                    || placed.getY() < y || placed.getY() > y + 3) continue;
            nearbyBlocks++;
            if (placed.getY() == y + 2 && Math.abs(placed.getX() - x) <= 1
                    && Math.abs(placed.getZ() - z) <= 1) overhead = true;
        }
        return nearbyBlocks >= 5 && overhead;
    }
}
