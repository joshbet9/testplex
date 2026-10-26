package nautilus.game.arcade.game.games.monstermaze.kits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;

import mineplex.core.common.util.C;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

import nautilus.game.arcade.events.GameStateChangeEvent;
import nautilus.game.arcade.game.Game.GameState;
import nautilus.game.arcade.game.games.monstermaze.MonsterMaze;
import nautilus.game.arcade.game.games.monstermaze.events.AbilityUseEvent;

/** Slowballer variant with a Q-drop Cryo Blitz secondary ability. */
public class KitSlowballModern extends KitSlowball
{
    private static final long COOLDOWN_MS = 30000L;
    private static final long FREEZE_MS = 3000L;
    private static final double RADIUS = 6.0D;

    private final Map<UUID, Long> _nextUse = new HashMap<UUID, Long>();

    public KitSlowballModern(nautilus.game.arcade.ArcadeManager manager)
    {
        super(manager);
    }

    @EventHandler
    public void onCryoBlitz(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();
        if (!HasKit(player) || !Manager.GetGame().IsLive())
            return;

        org.bukkit.inventory.ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped == null || dropped.getType() != Material.SNOW_BALL)
            return;

        MonsterMaze game = Manager.GetGame() instanceof MonsterMaze
                ? (MonsterMaze) Manager.GetGame() : null;
        if (game == null || !game.isModernVariant() || game.getMaze() == null)
            return;

        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long next = _nextUse.get(player.getUniqueId());
        if (next != null && next.longValue() > now)
        {
            player.sendMessage(C.cAqua + "Cryo Blitz ready in "
                    + Math.max(1L, (next.longValue() - now + 999L) / 1000L) + "s.");
            return;
        }

        int frozen = game.getMaze().freezeMonsters(player.getLocation(), RADIUS, FREEZE_MS);
        _nextUse.put(player.getUniqueId(), now + COOLDOWN_MS);
        player.sendMessage(C.cAqua + C.Bold + "Cryo Blitz! " + C.cGray
                + frozen + " monster(s) frozen for 3 seconds.");
        player.playSound(player.getLocation(), org.bukkit.Sound.SUCCESSFUL_HIT, 1.0f, 1.5f);
        org.bukkit.Bukkit.getPluginManager().callEvent(new AbilityUseEvent(player));
    }

    @EventHandler
    public void onUpdate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.SEC || !Manager.GetGame().IsLive())
            return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = _nextUse.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue().longValue() > now)
                continue;

            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && HasKit(player))
                player.sendMessage(C.cAqua + "Cryo Blitz is ready.");
            iterator.remove();
        }
    }

    @EventHandler
    public void onGameEnd(GameStateChangeEvent event)
    {
        if (event.GetState() == GameState.End || event.GetState() == GameState.Dead)
            _nextUse.clear();
    }
}
