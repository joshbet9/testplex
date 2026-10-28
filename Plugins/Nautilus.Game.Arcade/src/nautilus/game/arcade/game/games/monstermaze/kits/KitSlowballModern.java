package nautilus.game.arcade.game.games.monstermaze.kits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
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
    private final Map<UUID, Long> _frozenUntil = new HashMap<UUID, Long>();

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

        org.bukkit.Location origin = player.getLocation().clone();
        int frozen = game.getMaze().freezeMonsters(origin, RADIUS, FREEZE_MS);
        _nextUse.put(player.getUniqueId(), now + COOLDOWN_MS);
        if (frozen > 0)
            _frozenUntil.put(player.getUniqueId(), now + FREEZE_MS);
        player.sendMessage(C.cAqua + C.Bold + "CRYO BLITZ! " + C.cWhite
                + frozen + C.cAqua + C.Bold + " monsters frozen " + C.cGray
                + "for " + C.cYellow + C.Bold + "3 seconds" + C.cGray + ".");
        player.playSound(origin, org.bukkit.Sound.DIG_SNOW, 1.0f, 0.6f);
        player.playSound(origin, org.bukkit.Sound.SUCCESSFUL_HIT, 1.0f, 1.5f);
        playCryoRadiusVisual(origin);
        sendActionBar(player, C.cAqua + C.Bold + "CRYO BLITZ! " + C.cWhite + frozen
                + C.cAqua + C.Bold + " FROZEN " + C.cYellow + C.Bold + "RADIUS " + C.cGold + "6"
                + C.cGray + " | " + C.cWhite + "3.0s");
        org.bukkit.Bukkit.getPluginManager().callEvent(new AbilityUseEvent(player));
    }

    /**
     * A visible snow-burst ring marks the exact Cryo Blitz radius for everyone nearby.
     * Use legacy 1.8 effects rather than client/version-specific particle APIs.
     */
    private void playCryoRadiusVisual(org.bukkit.Location origin)
    {
        if (origin == null || origin.getWorld() == null)
            return;
        org.bukkit.World world = origin.getWorld();
        for (int i = 0; i < 56; i++)
        {
            double angle = (Math.PI * 2.0D * i) / 56.0D;
            org.bukkit.Location point = origin.clone().add(
                    Math.cos(angle) * RADIUS, 0.15D, Math.sin(angle) * RADIUS);
            world.playEffect(point, Effect.SNOWBALL_BREAK, 0);
        }
        for (int i = 0; i < 28; i++)
        {
            double angle = (Math.PI * 2.0D * i) / 28.0D;
            org.bukkit.Location point = origin.clone().add(
                    Math.cos(angle) * (RADIUS - 0.7D), 0.15D, Math.sin(angle) * (RADIUS - 0.7D));
            world.playEffect(point, Effect.SNOWBALL_BREAK, 0);
        }
    }

    private void sendActionBar(Player player, String message)
    {
        BaseComponent[] components = TextComponent.fromLegacyText(message);
        IChatBaseComponent packetText = IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(components));
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(packetText, (byte) 2));
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
            Long frozenUntil = _frozenUntil.get(player.getUniqueId());
            if (frozenUntil != null && frozenUntil.longValue() > now)
            {
                sendActionBar(player, C.cAqua + C.Bold + "FROZEN " + C.cWhite
                        + String.format(java.util.Locale.US, "%.1fs", (frozenUntil.longValue() - now) / 1000.0D));
                continue;
            }
            _frozenUntil.remove(player.getUniqueId());
            Long next = _nextUse.get(player.getUniqueId());
            if (next == null)
            {
                sendActionBar(player, C.cGreen + C.Bold + "CRYO BLITZ READY!");
                continue;
            }
            long remaining = next.longValue() - now;
            if (remaining <= 0)
            {
                _nextUse.remove(player.getUniqueId());
                player.sendMessage(C.cAqua + "Cryo Blitz is ready.");
                sendActionBar(player, C.cAqua + C.Bold + "CRYO BLITZ READY");
            }
            else
            {
                sendActionBar(player, C.cAqua + C.Bold + "CRYO BLITZ " + C.cWhite
                        + String.format(java.util.Locale.US, "%.1fs", remaining / 1000.0D));
            }
        }
    }

    @EventHandler
    public void onGameEnd(GameStateChangeEvent event)
    {
        if (event.GetState() == GameState.End || event.GetState() == GameState.Dead)
        {
            _nextUse.clear();
            _frozenUntil.clear();
        }
    }
}
