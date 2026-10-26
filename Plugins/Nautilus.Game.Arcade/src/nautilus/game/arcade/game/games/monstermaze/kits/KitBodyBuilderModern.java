package nautilus.game.arcade.game.games.monstermaze.kits;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;

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
        player.getInventory().setItem(0, ItemStackFactory.Instance.CreateStack(
                Material.APPLE, (byte) 0, 2, C.cRed + C.Bold + "Body Rush"
                        + C.cGray + " - Drop to activate"));
    }

    @EventHandler
    public void onBodyRushDrop(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();
        if (!HasKit(player) || !Manager.GetGame().IsLive())
            return;

        org.bukkit.inventory.ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped == null || dropped.getType() != Material.APPLE
                || !dropped.hasItemMeta() || !dropped.getItemMeta().hasDisplayName()
                || !dropped.getItemMeta().getDisplayName().contains("Body Rush"))
            return;

        event.setCancelled(true);
        UtilInv.remove(player, Material.APPLE, (byte) 0, 1);
        _bodyRushUntil.put(player.getUniqueId(), System.currentTimeMillis() + BODY_RUSH_DURATION_MS);
        player.sendMessage(C.cRed + C.Bold + "Body Rush activated!" + C.cGray + " Monster hits will be deflected.");
        player.playSound(player.getLocation(), org.bukkit.Sound.SUCCESSFUL_HIT, 1.0f, 1.25f);
        Bukkit.getPluginManager().callEvent(new AbilityUseEvent(player));
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
            if (active.next().getValue().longValue() <= now)
                active.remove();
        }
    }

    @EventHandler
    public void onGameEnd(GameStateChangeEvent event)
    {
        if (event.GetState() != GameState.End && event.GetState() != GameState.Dead)
            return;

        _bodyRushUntil.clear();
        for (LivingEntity entity : new HashSet<LivingEntity>(_launched.keySet()))
        {
            if (entity != null && entity.isValid())
                entity.remove();
        }
        _launched.clear();
    }
}
