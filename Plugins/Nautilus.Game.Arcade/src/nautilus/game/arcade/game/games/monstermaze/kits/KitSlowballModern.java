package nautilus.game.arcade.game.games.monstermaze.kits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
import mineplex.core.game.kit.GameKit;
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
        this(manager, GameKit.MONSTER_MAZE_MODERN_SLOW_BALL);
    }

    protected KitSlowballModern(nautilus.game.arcade.ArcadeManager manager, GameKit gameKit)
    {
        super(manager, gameKit);
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
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                C.cAqua + C.Bold + "CRYO BLITZ " + C.cWhite + frozen + " frozen - 3.0s"));
        org.bukkit.Bukkit.getPluginManager().callEvent(new AbilityUseEvent(player));
    }

    @EventHandler
    public void onUpdate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || !Manager.GetGame().IsLive())
            return;

        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (!player.isOnline() || !HasKit(player))
                continue;
            Long next = _nextUse.get(player.getUniqueId());
            if (next == null)
            {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        C.cAqua + C.Bold + "CRYO BLITZ READY"));
                continue;
            }
            long remaining = next.longValue() - now;
            if (remaining <= 0)
            {
                _nextUse.remove(player.getUniqueId());
                player.sendMessage(C.cAqua + "Cryo Blitz is ready.");
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        C.cAqua + C.Bold + "CRYO BLITZ READY"));
            }
            else
            {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
                        C.cAqua + C.Bold + "CRYO BLITZ " + C.cWhite
                                + String.format(java.util.Locale.US, "%.1fs", remaining / 1000.0D)));
            }
        }
    }

    @EventHandler
    public void onGameEnd(GameStateChangeEvent event)
    {
        if (event.GetState() == GameState.End || event.GetState() == GameState.Dead)
            _nextUse.clear();
    }
}
