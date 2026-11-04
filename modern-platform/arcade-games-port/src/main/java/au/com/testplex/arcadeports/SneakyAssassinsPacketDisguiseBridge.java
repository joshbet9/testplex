package au.com.testplex.arcadeports;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces the outgoing player spawn with a Villager spawn using the same entity ID.
 * Client attacks therefore still target the real server-side Player; no second
 * entity, hitbox, or collision body is created.
 */
public final class SneakyAssassinsPacketDisguiseBridge implements PacketListener,
        SneakyAssassinsDisguiseBridge {
    private final Set<UUID> disguisedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Integer, UUID> disguisedEntityOwners = new ConcurrentHashMap<>();
    private PacketListenerCommon registeredListener;

    public boolean register() {
        if (registeredListener != null) return true;
        try {
            registeredListener = PacketEvents.getAPI().getEventManager()
                    .registerListener(this, PacketListenerPriority.NORMAL);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            registeredListener = null;
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return registeredListener != null;
    }

    @Override
    public void setDisguised(UUID playerId, boolean disguised) {
        if (playerId == null || !isAvailable()) return;
        Player target = Bukkit.getPlayer(playerId);
        if (disguised && (target == null || !target.isOnline())) return;

        boolean changed = disguised ? disguisedPlayers.add(playerId) : disguisedPlayers.remove(playerId);
        if (!changed) return;

        if (disguised) disguisedEntityOwners.put(target.getEntityId(), playerId);
        else disguisedEntityOwners.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
        if (target == null || !target.isOnline()) return;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target) || viewer.getWorld() != target.getWorld()) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerDestroyEntities(target.getEntityId()));
            if (disguised) sendVillagerSpawn(viewer, target.getEntityId(), playerId,
                    target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(),
                    target.getLocation().getYaw(), target.getLocation().getPitch());
            else sendPlayerSpawn(viewer, target);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer spawn = new WrapperPlayServerSpawnPlayer(event);
            if (!disguisedPlayers.contains(spawn.getUUID())) return;
            event.setCancelled(true);
            Player viewer = Bukkit.getPlayer(event.getUser().getUUID());
            if (viewer != null && viewer.isOnline()) {
                sendVillagerSpawn(viewer, spawn.getEntityId(), spawn.getUUID(),
                        spawn.getPosition().getX(), spawn.getPosition().getY(), spawn.getPosition().getZ(),
                        spawn.getYaw(), spawn.getPitch());
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event);
            if (!disguisedEntityOwners.containsKey(metadata.getEntityId())) return;

            // Player-only metadata indices have a different type/layout on Villagers.
            List<EntityData<?>> compatible = new ArrayList<>();
            for (EntityData<?> value : metadata.getEntityMetadata()) {
                if (value.getIndex() <= 14) compatible.add(value);
            }
            if (compatible.isEmpty()) {
                event.setCancelled(true);
            } else {
                event.setLastUsedWrapper(new WrapperPlayServerEntityMetadata(
                        metadata.getEntityId(), compatible));
                event.markForReEncode(true);
            }
        }
    }

    private void sendVillagerSpawn(Player viewer, int entityId, UUID playerId,
            double x, double y, double z, float yaw, float pitch) {
        Player target = Bukkit.getPlayer(playerId);
        String name = target == null ? "" : target.getName();
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerSpawnEntity(entityId, Optional.of(playerId), EntityTypes.VILLAGER,
                        new Vector3d(x, y, z), pitch, yaw, yaw, 0, Optional.empty()));
        if (!name.isBlank()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerEntityMetadata(entityId, List.of(
                            new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                                    Optional.of(Component.text(name))),
                            new EntityData<>(3, EntityDataTypes.BOOLEAN, true))));
        }
    }

    private void sendPlayerSpawn(Player viewer, Player target) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerSpawnPlayer(target.getEntityId(), target.getUniqueId(),
                        new Vector3d(target.getLocation().getX(), target.getLocation().getY(),
                                target.getLocation().getZ()),
                        target.getLocation().getYaw(), target.getLocation().getPitch(), List.of()));
    }

    @Override
    public void unregister() {
        PacketListenerCommon listener = registeredListener;
        registeredListener = null;
        disguisedPlayers.clear();
        disguisedEntityOwners.clear();
        if (listener != null) PacketEvents.getAPI().getEventManager().unregisterListener(listener);
    }
}
