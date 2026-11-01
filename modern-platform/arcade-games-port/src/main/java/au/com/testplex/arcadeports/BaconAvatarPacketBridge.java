package au.com.testplex.arcadeports;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Optional PacketEvents bridge for Bacon Brawl. The visible Pig/Sheep entity keeps its source
 * rendering, but attacks are resolved against the hidden player after server-side reach/LOS checks.
 * This class is loaded reflectively only when PacketEvents is installed; bare Paper still works.
 */
public final class BaconAvatarPacketBridge implements PacketListener {
    private final ArcadeMechanicsModernPlugin plugin;
    private final ArcadePortManager manager;
    private PacketListenerCommon registeredListener;

    public BaconAvatarPacketBridge(ArcadeMechanicsModernPlugin plugin, ArcadePortManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        if (registeredListener != null) return;
        registeredListener = PacketEvents.getAPI().getEventManager()
                .registerListener(this, PacketListenerPriority.NORMAL);
    }

    public void unregister() {
        PacketListenerCommon listener = registeredListener;
        registeredListener = null;
        if (listener != null) PacketEvents.getAPI().getEventManager().unregisterListener(listener);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled()) return;

        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            int entityId = new WrapperPlayClientAttack(event).getEntityId();
            if (!manager.isBaconAvatarEntityId(entityId)) return;
            UUID attackerId = event.getUser().getUUID();
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player attacker = Bukkit.getPlayer(attackerId);
                if (attacker != null && attacker.isOnline()) {
                    manager.handleBaconAvatarPacketAttack(attacker, entityId);
                }
            });
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            int entityId = new WrapperPlayClientInteractEntity(event).getEntityId();
            // The original Bacon disguise is a visual avatar, not a mountable/pickup-able animal.
            if (manager.isBaconAvatarEntityId(entityId)) event.setCancelled(true);
        }
    }
}
