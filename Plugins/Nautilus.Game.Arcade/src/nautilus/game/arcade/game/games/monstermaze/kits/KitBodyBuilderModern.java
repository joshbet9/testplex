package nautilus.game.arcade.game.games.monstermaze.kits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mineplex.core.common.util.C;
import mineplex.core.common.util.F;
import mineplex.core.common.util.UtilInv;
import mineplex.core.itemstack.ItemStackFactory;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

import nautilus.game.arcade.ArcadeManager;
import mineplex.core.game.kit.GameKit;
import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.Game.GameState;
import nautilus.game.arcade.game.games.monstermaze.events.AbilityUseEvent;

/**
 * Body Builder for Monster Maze Modern. Two apple activations each run.
 * While active, each monster hit is deflected and removes two seconds from the
 * remaining ten-second duration.
 */
public class KitBodyBuilderModern extends KitBodyBuilder
{
    private static final long BODY_RUSH_DURATION_MS = 10000L;
    private static final long HIT_PENALTY_MS = 2000L;
    private static final long LAUNCH_TIMEOUT_MS = 1500L;

    private final Map<UUID, Long> _bodyRushUntil = new HashMap<UUID, Long>();
    private final Map<UUID, Long> _bodyRushFeedbackUntil = new HashMap<UUID, Long>();
    private final Map<LivingEntity, Long> _launched = new HashMap<LivingEntity, Long>();

    public KitBodyBuilderModern(ArcadeManager manager)
    {
        this(manager, GameKit.MONSTER_MAZE_MODERN_BODY_BUILDER);
    }

    protected KitBodyBuilderModern(ArcadeManager manager, GameKit gameKit)
    {
        super(manager, gameKit);
    }

    @Override
    public void GiveItems(Player player)
    {
        super.GiveItems(player);
        ItemStack item = ItemStackFactory.Instance.CreateStack(
                Material.APPLE, (byte) 0, 2, C.cRed + C.Bold + "Body Rush");
        ItemMeta meta = item.getItemMeta();
        meta.setLore(java.util.Arrays.asList(
                C.cGray + "Right-click to activate.",
                C.cGray + "10 seconds of monster-hit immunity.",
                C.cGray + "Each monster hit removes 2 seconds.",
                C.cRed + "Activations: 2"));
        item.setItemMeta(meta);
        player.getInventory().setItem(0, item);
    }

    @EventHandler
    public void onBodyRushInteract(PlayerInteractEvent event)
    {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK)
            return;
        if (!isBodyRushItem(event.getItem()))
            return;
        event.setCancelled(true);
        activateBodyRush(event.getPlayer());
    }

    /** Entity-interact replaces PlayerInteractEvent when a mob is under the crosshair. */
    @EventHandler
    public void onBodyRushEntityInteract(PlayerInteractEntityEvent event)
    {
        if (!isBodyRushItem(event.getPlayer().getItemInHand()))
            return;
        event.setCancelled(true);
        activateBodyRush(event.getPlayer());
    }

    private boolean isBodyRushItem(ItemStack item)
    {
        return item != null && item.getType() == Material.APPLE && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && ChatColor.stripColor(item.getItemMeta().getDisplayName()).contains("Body Rush");
    }

    private void activateBodyRush(Player player)
    {
        if (!HasKit(player) || !Manager.GetGame().IsLive())
            return;
        long now = System.currentTimeMillis();
        Long activeUntil = _bodyRushUntil.get(player.getUniqueId());
        if (activeUntil != null && activeUntil.longValue() > now)
        {
            sendActionBar(player, C.cRed + C.Bold + "BODY RUSH " + String.format(java.util.Locale.US,
                    "%.1fs", (activeUntil.longValue() - now) / 1000.0D) + " remaining");
            return;
        }

        ItemStack hand = player.getItemInHand();
        if (!isBodyRushItem(hand))
            return;
        if (hand.getAmount() <= 1)
            player.setItemInHand(null);
        else
        {
            hand.setAmount(hand.getAmount() - 1);
            player.setItemInHand(hand);
        }
        player.updateInventory();
        _bodyRushUntil.put(player.getUniqueId(), now + BODY_RUSH_DURATION_MS);
        _bodyRushFeedbackUntil.remove(player.getUniqueId());
        player.sendMessage(C.cRed + C.Bold + "Body Rush activated!" + C.cGray
                + " Monster hits will be deflected for 10 seconds.");
        player.playSound(player.getLocation(), org.bukkit.Sound.SUCCESSFUL_HIT, 1.0f, 1.25f);
        sendActionBar(player, C.cRed + C.Bold + "BODY RUSH 10.0s");
        Bukkit.getPluginManager().callEvent(new AbilityUseEvent(player));
    }

    private void sendActionBar(Player player, String message)
    {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    public boolean isBodyRushActive(Player player)
    {
        Long until = _bodyRushUntil.get(player.getUniqueId());
        if (until == null)
            return false;
        if (until.longValue() <= System.currentTimeMillis())
        {
            _bodyRushUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * Return true if this contact was deflected. The hit is deflected even when
     * its two-second penalty consumes the final part of the duration.
     */
    public boolean consumeMonsterHit(Player player)
    {
        long now = System.currentTimeMillis();
        Long until = _bodyRushUntil.get(player.getUniqueId());
        if (until == null || until.longValue() <= now)
        {
            _bodyRushUntil.remove(player.getUniqueId());
            return false;
        }

        long remaining = until.longValue() - HIT_PENALTY_MS;
        _bodyRushFeedbackUntil.put(player.getUniqueId(), now + 750L);
        if (remaining <= now)
            _bodyRushUntil.remove(player.getUniqueId());
        else
            _bodyRushUntil.put(player.getUniqueId(), remaining);

        return true;
    }

    /** Track a monster after a Body Rush deflection so it despawns when launch resolves. */
    public void trackLaunched(LivingEntity entity)
    {
        if (entity != null && entity.isValid())
            _launched.put(entity, System.currentTimeMillis());
    }

    @EventHandler
    public void onUpdate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK)
            return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<LivingEntity, Long>> iterator = _launched.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<LivingEntity, Long> entry = iterator.next();
            LivingEntity entity = entry.getKey();
            long elapsed = now - entry.getValue().longValue();
            if (entity == null || !entity.isValid()
                    || (entity.isOnGround() && elapsed > 500L)
                    || elapsed > LAUNCH_TIMEOUT_MS)
            {
                if (entity != null && entity.isValid())
                    entity.remove();
                iterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, Long>> active = _bodyRushUntil.entrySet().iterator();
        while (active.hasNext())
        {
            Map.Entry<UUID, Long> entry = active.next();
            UUID playerId = entry.getKey();
            long until = entry.getValue().longValue();
            Player player = Bukkit.getPlayer(playerId);
            if (until <= now)
            {
                active.remove();
                _bodyRushFeedbackUntil.remove(playerId);
                continue;
            }
            if (player != null && player.isOnline() && HasKit(player) && Manager.GetGame().IsLive())
            {
                Long feedbackUntil = _bodyRushFeedbackUntil.get(playerId);
                String feedback = feedbackUntil != null && feedbackUntil.longValue() > now
                        ? " " + ChatColor.YELLOW + "-2.0s" : "";
                sendActionBar(player, C.cRed + C.Bold + "BODY RUSH "
                        + String.format(java.util.Locale.US, "%.1fs", (until - now) / 1000.0D) + feedback);
            }
        }
        Iterator<Map.Entry<UUID, Long>> feedbackIterator = _bodyRushFeedbackUntil.entrySet().iterator();
        while (feedbackIterator.hasNext())
            if (feedbackIterator.next().getValue().longValue() <= now)
                feedbackIterator.remove();
    }

    @EventHandler
    public void onGameEnd(GameStateChangeEvent event)
    {
        if (event.GetState() != GameState.End && event.GetState() != GameState.Dead)
            return;

        _bodyRushUntil.clear();
        _bodyRushFeedbackUntil.clear();
        for (LivingEntity entity : new HashSet<LivingEntity>(_launched.keySet()))
        {
            if (entity != null && entity.isValid())
                entity.remove();
        }
        _launched.clear();
    }
}
