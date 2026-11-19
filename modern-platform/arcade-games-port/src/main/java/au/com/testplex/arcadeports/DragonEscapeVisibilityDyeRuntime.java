package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Renders Dragon Escape's existing visibility control as state-coloured dyes. */
final class DragonEscapeVisibilityDyeRuntime implements Listener {
    private final ArcadeMechanicsModernPlugin plugin;
    private final NamespacedKey visibilityToggleKey;

    DragonEscapeVisibilityDyeRuntime(ArcadeMechanicsModernPlugin plugin) {
        this.plugin = plugin;
        this.visibilityToggleKey = new NamespacedKey(plugin, "dragon-escape-player-visibility-toggle");
    }

    static Material dyeForVisibility(boolean visible) {
        return visible ? Material.LIME_DYE : Material.GRAY_DYE;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaceWorldEntered(PlayerChangedWorldEvent event) {
        refreshNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        refreshNextTick(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHotbarSlotSelected(PlayerItemHeldEvent event) {
        if (event.getNewSlot() == DragonEscapeMovementKits.DRAGON_ESCAPE_PLAYER_VISIBILITY_SLOT) {
            refreshNextTick(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onVisibilityToggle(PlayerInteractEvent event) {
        if (isVisibilityToggle(event.getItem())) refreshNextTick(event.getPlayer());
    }

    private boolean isVisibilityToggle(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                        .has(visibilityToggleKey, PersistentDataType.BYTE);
    }

    private void refreshNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player == null || !player.isOnline()) return;
            int slot = DragonEscapeMovementKits.DRAGON_ESCAPE_PLAYER_VISIBILITY_SLOT;
            ItemStack item = player.getInventory().getItem(slot);
            if (!isVisibilityToggle(item)) return;

            boolean visible = plugin.getConfig().getBoolean(
                    "dragon-escape-player-visible." + player.getUniqueId(),
                    DragonEscapeMovementKits.dragonEscapePlayerVisibleByDefault());
            Material desired = dyeForVisibility(visible);
            if (item.getType() == desired) return;

            ItemStack dye = item.clone();
            dye.setType(desired);
            player.getInventory().setItem(slot, dye);
        });
    }
}
