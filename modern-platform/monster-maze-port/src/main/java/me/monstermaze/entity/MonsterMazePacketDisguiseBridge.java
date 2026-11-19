package me.monstermaze.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.util.MobThemeManager;
import me.monstermaze.util.UtilEnt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses the already installed PacketEvents 2.14 bridge for viewer-specific Monster Maze mob appearances.
 * Each server entity remains the real, managed Snow Golem for collision and damage; only its outgoing
 * spawn type is rewritten. This class is loaded reflectively when PacketEvents is enabled.
 */
public final class MonsterMazePacketDisguiseBridge implements PacketListener {
    private final MonsterMazePlugin plugin;
    private final Map<Integer, UUID> skinnedEntityIds = new ConcurrentHashMap<>();
    private PacketListenerCommon registered;

    public MonsterMazePacketDisguiseBridge(MonsterMazePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean register() {
        if (registered != null) return true;
        try {
            registered = PacketEvents.getAPI().getEventManager()
                    .registerListener(this, PacketListenerPriority.NORMAL);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().warning("Monster Maze PacketEvents disguise bridge could not start: "
                    + failure.getClass().getSimpleName());
            return false;
        }
    }

    public void shutdown() {
        PacketListenerCommon previous = registered;
        registered = null;
        skinnedEntityIds.clear();
        if (previous != null) PacketEvents.getAPI().getEventManager().unregisterListener(previous);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled() || registered == null) return;
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(event);
            UUID entityId = spawn.getUUID().orElse(null);
            if (entityId == null) return;
            // The first SPAWN_ENTITY may precede the registration of the actual Bukkit entity.
            String baseSkin = MonsterEntityController.getSkin(entityId);
            if (baseSkin == null) baseSkin = UtilEnt.getPendingSpawnSkin();
            if (baseSkin == null) return;

            Player viewer = Bukkit.getPlayer(event.getUser().getUUID());
            if (viewer == null || !viewer.isOnline()) return;
            String requested = themeForViewer(viewer, entityId, baseSkin);
            EntityType targetType = resolveEntityType(requested);
            if (targetType == null) return;
            skinnedEntityIds.put(spawn.getEntityId(), entityId);
            spawn.setEntityType(targetType);
            event.setLastUsedWrapper(spawn);
            event.markForReEncode(true);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(event);
            UUID uuid = skinnedEntityIds.get(metadata.getEntityId());
            if (uuid == null) return;
            if (MonsterEntityController.getSkin(uuid) == null) {
                skinnedEntityIds.remove(metadata.getEntityId(), uuid);
                return;
            }
            // A Snow Golem's mob-specific metadata is invalid for other client mob types.
            // Only common Entity indices are safe regardless of the viewer's selected form.
            List<EntityData<?>> compatible = new ArrayList<>();
            for (EntityData<?> item : metadata.getEntityMetadata()) {
                if (isCrossMobMetadataIndex(item.getIndex())) compatible.add(item);
            }
            if (compatible.size() == metadata.getEntityMetadata().size()) return;
            if (compatible.isEmpty()) {
                event.setCancelled(true);
            } else {
                event.setLastUsedWrapper(new WrapperPlayServerEntityMetadata(
                        metadata.getEntityId(), compatible));
                event.markForReEncode(true);
            }
        }
    }

    public void refreshPlayer(Player viewer) {
        if (registered == null || viewer == null || !viewer.isOnline()) return;
        for (LivingEntity entity : MonsterEntityController.getSkinnedEntities(viewer.getWorld())) {
            if (entity == null || !entity.isValid()) continue;
            String baseSkin = MonsterEntityController.getSkin(entity.getUniqueId());
            if (baseSkin == null) continue;
            EntityType target = resolveEntityType(themeForViewer(viewer, entity.getUniqueId(), baseSkin));
            if (target == null) continue;
            Location at = entity.getLocation();
            skinnedEntityIds.put(entity.getEntityId(), entity.getUniqueId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerDestroyEntities(entity.getEntityId()));
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerSpawnEntity(entity.getEntityId(),
                            Optional.of(entity.getUniqueId()), target,
                            new Vector3d(at.getX(), at.getY(), at.getZ()),
                            at.getPitch(), at.getYaw(), at.getYaw(), 0, Optional.empty()));
        }
    }

    private String themeForViewer(Player viewer, UUID entityId, String fallback) {
        MobThemeManager themes = plugin.getMobThemeManager();
        return themes == null ? fallback : themes.forViewer(viewer, entityId);
    }

    static boolean isCrossMobMetadataIndex(int index) {
        return index >= 0 && index <= 7;
    }

    static String clientTypeName(String rawTheme) {
        if (rawTheme == null) return "snow_golem";
        String theme = rawTheme.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (theme) {
            case "snowman", "snow_golem" -> "snow_golem";
            case "zombie_pigman", "pig_zombie", "zombified_piglin" -> "zombified_piglin";
            case "eyeofender", "eye_of_ender" -> "enderman";
            case "mushroom_cow" -> "mooshroom";
            default -> theme;
        };
    }

    private static EntityType resolveEntityType(String theme) {
        String name = clientTypeName(theme);
        EntityType type = EntityTypes.getByName("minecraft:" + name);
        if (type == null) type = EntityTypes.getByName(name);
        return type == null ? EntityTypes.SNOW_GOLEM : type;
    }
}
