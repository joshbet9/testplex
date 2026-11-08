package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Spleef floor eligibility, slow-damage colours, and projectile impact geometry. */
final class SpleefManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.SPLEEF; }

    boolean isProtectedBlock(Material material) {
        return ArcadePortRules.isSpleefProtectedMaterial(material);
    }

    boolean isWoolOrTerracotta(Material material) {
        String name = material.name();
        return name.endsWith("_WOOL") || name.endsWith("_TERRACOTTA") || material == Material.TERRACOTTA;
    }

    Material nextColour(Material material) {
        return ArcadePortRules.nextSpleefSlowDamageMaterial(material);
    }

    /** Reborn's 3×3×3 search for a glancing snowball hit on the floor. */
    Block projectileImpactBlock(Location impact) {
        if (impact == null || impact.getWorld() == null) return null;
        Block direct = impact.getBlock();
        if (!direct.getType().isAir()) return direct;
        Block nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block candidate = direct.getRelative(dx, dy, dz);
                    if (candidate.getType().isAir()) continue;
                    double distance = candidate.getLocation().add(0.5D, 0.5D, 0.5D).distanceSquared(impact);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }

    boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    boolean isFletchedArrow(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).contains("Fletched Arrow");
    }

    Player nearestSpectatorTarget(SessionState session, Player spectator) {
        if (session == null || spectator == null) return null;
        return alivePlayers(session).stream()
                .min(java.util.Comparator.comparingDouble(target ->
                        target.getLocation().distanceSquared(spectator.getLocation())))
                .orElse(null);
    }

    void updateSpectatorCompasses(SessionState session, long now) {
        if (session == null || now < session.spleefNextCompassUpdateAt) return;
        session.spleefNextCompassUpdateAt = now + 250L;
        for (Player spectator : session.players.values()) {
            if (spectator == null || !spectator.isOnline()
                    || session.alive.contains(spectator.getUniqueId())) continue;
            Player target = nearestSpectatorTarget(session, spectator);
            if (target == null) continue;
            boolean hasCompass = false;
            for (ItemStack item : spectator.getInventory().getContents()) {
                if (isTrackingCompass(item)) { hasCompass = true; break; }
            }
            if (!hasCompass) spectator.getInventory().addItem(trackingCompass());
            spectator.setCompassTarget(target.getLocation());
        }
    }

    private ItemStack trackingCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + "Tracks the nearest living Spleef player."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<Player> alivePlayers(SessionState session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }
}
