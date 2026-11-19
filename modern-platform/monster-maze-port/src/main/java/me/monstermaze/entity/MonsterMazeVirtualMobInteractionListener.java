package me.monstermaze.entity;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import me.monstermaze.MonsterMazePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles right clicks on the viewer-local, client-only Monster Maze mob entity
 * IDs. Bukkit does not own these entities and therefore cannot dispatch its
 * normal PlayerInteractEntityEvent for them.
 *
 * Only reads/cancels an inbound INTERACT_ENTITY targeting the same viewer's
 * known virtual ID; it does not rewrite any outbound packets, which was the
 * cause suspected in the original game-start disconnect regression.
 */
public final class MonsterMazeVirtualMobInteractionListener implements PacketListener {
    private final MonsterMazePlugin plugin;
    // Concurrent collection owned by the per-viewer renderer; read on Netty threads.
    private final Map<UUID, Set<Integer>> fakeIdsByViewer;

    public MonsterMazeVirtualMobInteractionListener(MonsterMazePlugin plugin,
            Map<UUID, Set<Integer>> fakeIdsByViewer) {
        this.plugin = plugin;
        this.fakeIdsByViewer = fakeIdsByViewer;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled() || event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY)
            return;

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        UUID viewerId = event.getUser().getUUID();
        if (!belongsToViewer(fakeIdsByViewer, viewerId, packet.getEntityId())) return;

        // The entity ID is synthetic and must never be processed by vanilla
        // interaction logic, regardless of whether the player is holding the selector.
        event.setCancelled(true);
        if (packet.getHand() != InteractionHand.MAIN_HAND) return;

        // Never access Bukkit inventories/player state from the network callback.
        Bukkit.getScheduler().runTask(plugin.getHostPlugin(), () -> {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()
                    && belongsToViewer(fakeIdsByViewer, viewerId, packet.getEntityId())
                    && plugin.getMobThemeManager() != null) {
                plugin.getMobThemeManager().cycleThemeFromVirtualMob(viewer);
            }
        });
    }

    static boolean belongsToViewer(Map<UUID, Set<Integer>> fakeIdsByViewer, UUID viewerId,
            int entityId) {
        Set<Integer> ids = fakeIdsByViewer.get(viewerId);
        return ids != null && ids.contains(entityId);
    }
}
