package me.monstermaze.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional packet-level invisibility adapter.
 *
 * <p>This class is loaded reflectively only when ProtocolLib is enabled. The core
 * Monster Maze scoreboard intentionally has no hard class dependency on ProtocolLib,
 * so the vanilla Snow Golem fallback continues to work without the plugin.</p>
 */
public final class ProtocolLibVisibility {
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public void sendInvisibleMetadata(Player observer, Player target, boolean invisible) {
        if (observer == null || target == null || !observer.isOnline() || !target.isOnline()) return;
        try {
            WrappedDataWatcher source = WrappedDataWatcher.getEntityWatcher(target);
            Byte current = source.getByte(0);
            byte flags = current == null ? 0 : current.byteValue();
            flags = invisible ? (byte) (flags | 0x20) : (byte) (flags & ~0x20);
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, target.getEntityId());
            List<WrappedDataValue> values = new ArrayList<WrappedDataValue>();
            values.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), flags));
            packet.getDataValueCollectionModifier().write(0, values);
            protocolManager.sendServerPacket(observer, packet);
        } catch (Throwable ignored) {
            // Packet cosmetics must never break core game visibility or gameplay.
        }
    }
}
