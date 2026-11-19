package me.monstermaze.entity;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.util.MobThemeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Viewer-local Monster Maze appearances without rewriting any outbound vanilla packets.
 *
 * Paper continues to own and simulate real Snow Golems (hitboxes, movement, mob contact
 * damage). Each viewer hides those Bukkit entities and receives a different, client-only
 * entity ID for their selected cosmetic model. No Snow Golem metadata, equipment, health,
 * or entity-status packets are ever sent to the client-only IDs. This avoids the unsafe
 * original design which changed the vanilla spawn type while keeping server metadata.
 *
 * All Bukkit access and packet sends run on the primary thread; this class is not a
 * PacketEvents network-thread listener. Its activation is kept reflective and optional.
 */
public final class MonsterMazePacketDisguiseBridge {
    private static final AtomicInteger NEXT_CLIENT_ENTITY_ID = new AtomicInteger(1_900_000_000);
    private static final double VIEW_RANGE_SQUARED = 128.0D * 128.0D;
    private static final double MOVE_THRESHOLD_SQUARED = 0.0004D;

    private final MonsterMazePlugin plugin;
    private final Map<UUID, Map<UUID, ClientMob>> displaysByViewer = new HashMap<>();
    private final Set<UUID> failedViewers = new HashSet<>();
    // Read by Netty's inbound listener; all mutations on Bukkit's main thread.
    private final Map<UUID, Set<Integer>> fakeIdsByViewer = new ConcurrentHashMap<>();
    private PacketListenerCommon mobInteractListener;
    private BukkitTask updateTask;
    private int tick;

    public MonsterMazePacketDisguiseBridge(MonsterMazePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean register() {
        if (updateTask != null) return true;
        try {
            if (PacketEvents.getAPI() == null || PacketEvents.getAPI().getPlayerManager() == null) {
                return false;
            }
            mobInteractListener = PacketEvents.getAPI().getEventManager().registerListener(
                    new MonsterMazeVirtualMobInteractionListener(plugin, fakeIdsByViewer),
                    PacketListenerPriority.NORMAL);
            updateTask = Bukkit.getScheduler().runTaskTimer(
                    plugin.getHostPlugin(), this::tick, 1L, 2L);
            plugin.getLogger().info("Monster Maze per-viewer virtual mob renderers enabled (no vanilla packet rewriting).");
            return true;
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.WARNING, "Could not enable Monster Maze virtual mob appearances.", failure);
            return false;
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        PacketListenerCommon previousListener = mobInteractListener;
        mobInteractListener = null;
        if (previousListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(previousListener);
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) clearViewer(viewer);
        displaysByViewer.clear();
        fakeIdsByViewer.clear();
        failedViewers.clear();
    }

    /** Called by the in-game mob-head selector; this changes only that viewer's renderers. */
    public void refreshPlayer(Player viewer) {
        if (updateTask == null || viewer == null || !viewer.isOnline()) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin.getHostPlugin(), () -> refreshPlayer(viewer));
            return;
        }
        syncViewer(viewer, MonsterEntityController.getSkinnedEntities(viewer.getWorld()));
    }

    private void tick() {
        if (updateTask == null) return;
        tick++;
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID viewerId = viewer.getUniqueId();
            online.add(viewerId);
            if (failedViewers.contains(viewerId)) continue;
            // Spawn newly registered mobs, remove departed mobs and track choice changes.
            // Doing this every five runs avoids repeatedly querying cosmetic preferences.
            if (tick % 5 == 1) {
                syncViewer(viewer, MonsterEntityController.getSkinnedEntities(viewer.getWorld()));
            }
            moveViewerDisplays(viewer);
        }
        displaysByViewer.keySet().removeIf(id -> !online.contains(id));
        fakeIdsByViewer.keySet().removeIf(id -> !online.contains(id));
        failedViewers.retainAll(online);
    }

    private void syncViewer(Player viewer, List<LivingEntity> entities) {
        UUID viewerId = viewer.getUniqueId();
        if (failedViewers.contains(viewerId)) return;
        Map<UUID, ClientMob> existing = displaysByViewer.computeIfAbsent(viewerId, id -> new HashMap<>());
        Set<UUID> present = new HashSet<>();

        for (LivingEntity real : entities) {
            if (real == null || !real.isValid() || real.getWorld() != viewer.getWorld()) continue;
            UUID realId = real.getUniqueId();
            present.add(realId);
            EntityType target = resolveEntityType(themeForViewer(viewer, realId));
            ClientMob current = existing.get(realId);
            Location at = real.getLocation();
            boolean inRange = viewer.getLocation().distanceSquared(at) <= VIEW_RANGE_SQUARED;
            if (target == EntityTypes.SNOW_GOLEM || !inRange) {
                if (current != null) {
                    destroyViewerDisplay(viewer, current, real);
                    existing.remove(realId);
                }
                continue;
            }

            if (current != null && current.type == target) {
                current.realEntity = real;
                continue;
            }
            if (current != null) {
                destroyViewerDisplay(viewer, current, null);
                existing.remove(realId);
            }

            try {
                // Different fake ID and UUID per viewer prevents cross-player or vanilla
                // metadata collisions. Bukkit's hidden real mob still owns all mechanics.
                int fakeId = NEXT_CLIENT_ENTITY_ID.getAndDecrement();
                UUID fakeUuid = virtualUuid(viewerId, realId);
                viewer.hideEntity(plugin.getHostPlugin(), real);
                fakeIdsByViewer.computeIfAbsent(viewerId, id -> ConcurrentHashMap.newKeySet())
                        .add(fakeId);
                sendSpawn(viewer, fakeId, fakeUuid, target, at);
                existing.put(realId, new ClientMob(real, fakeId, fakeUuid, target, at.clone()));
            } catch (RuntimeException | LinkageError failure) {
                // Never keep the actual mob hidden if a virtual-renderer call fails.
                safeShow(viewer, real);
                failViewerOpen(viewer, failure);
                return;
            }
        }

        Iterator<Map.Entry<UUID, ClientMob>> it = existing.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ClientMob> entry = it.next();
            if (present.contains(entry.getKey())) continue;
            // Even if the server mob has been removed, clear Bukkit's per-player
            // hide state so it cannot accumulate across repeated rounds.
            destroyViewerDisplay(viewer, entry.getValue(), entry.getValue().realEntity);
            it.remove();
        }
    }

    private void moveViewerDisplays(Player viewer) {
        Map<UUID, ClientMob> displays = displaysByViewer.get(viewer.getUniqueId());
        if (displays == null || displays.isEmpty()) return;
        for (ClientMob fake : displays.values()) {
            LivingEntity real = fake.realEntity;
            if (real == null || !real.isValid() || real.getWorld() != viewer.getWorld()) continue;
            Location at = real.getLocation();
            if (!moved(fake.lastPosition, at)) continue;
            try {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                        new WrapperPlayServerEntityTeleport(
                                fake.entityId,
                                new Vector3d(at.getX(), at.getY(), at.getZ()),
                                at.getYaw(), at.getPitch(), real.isOnGround()));
                fake.lastPosition = at.clone();
            } catch (RuntimeException | LinkageError failure) {
                failViewerOpen(viewer, failure);
                return;
            }
        }
    }

    private void destroyViewerDisplay(Player viewer, ClientMob mob, LivingEntity showReal) {
        Set<Integer> fakeIds = fakeIdsByViewer.get(viewer.getUniqueId());
        if (fakeIds != null) fakeIds.remove(mob.entityId);
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    viewer, new WrapperPlayServerDestroyEntities(mob.entityId));
        } catch (RuntimeException | LinkageError failure) {
            plugin.getLogger().log(Level.WARNING, "Could not remove a viewer-only Monster Maze mob.", failure);
        }
        if (showReal != null) safeShow(viewer, showReal);
    }

    private void clearViewer(Player viewer) {
        Map<UUID, ClientMob> previous = displaysByViewer.remove(viewer.getUniqueId());
        if (previous == null) return;
        for (ClientMob fake : previous.values()) {
            destroyViewerDisplay(viewer, fake, fake.realEntity);
        }
    }

    private void failViewerOpen(Player viewer, Throwable failure) {
        if (!failedViewers.add(viewer.getUniqueId())) return;
        plugin.getLogger().log(Level.WARNING,
                "Monster Maze virtual appearance delivery failed for " + viewer.getName()
                        + "; restoring real mobs without affecting the game.", failure);
        clearViewer(viewer);
    }

    private void safeShow(Player viewer, LivingEntity real) {
        try {
            if (real != null && viewer.isOnline()) {
                viewer.showEntity(plugin.getHostPlugin(), real);
            }
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "Could not restore native Monster Maze mob renderer.", failure);
        }
    }

    private void sendSpawn(Player viewer, int fakeId, UUID fakeUuid, EntityType target, Location at) {
        // Client-only mobs are not backed by a Bukkit entity or Snow Golem metadata;
        // they receive only a complete, type-correct SPAWN_ENTITY packet and teleports.
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerSpawnEntity(
                        fakeId, Optional.of(fakeUuid), target,
                        new Vector3d(at.getX(), at.getY(), at.getZ()),
                        at.getPitch(), at.getYaw(), at.getYaw(), 0, Optional.empty()));
    }

    private String themeForViewer(Player viewer, UUID realId) {
        MobThemeManager themes = plugin.getMobThemeManager();
        if (themes == null) return "snow_golem";
        return themes.forViewer(viewer, realId);
    }

    static boolean moved(Location previous, Location current) {
        if (previous == null || current == null || previous.getWorld() != current.getWorld()) return true;
        if (previous.distanceSquared(current) > MOVE_THRESHOLD_SQUARED) return true;
        return Math.abs(previous.getYaw() - current.getYaw()) > 3.0F
                || Math.abs(previous.getPitch() - current.getPitch()) > 3.0F;
    }

    static UUID virtualUuid(UUID viewerId, UUID mobId) {
        return UUID.nameUUIDFromBytes(("monster-maze:" + viewerId + ":" + mobId)
                .getBytes(StandardCharsets.UTF_8));
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

    private static final class ClientMob {
        final int entityId;
        final UUID uuid;
        final EntityType type;
        LivingEntity realEntity;
        Location lastPosition;

        ClientMob(LivingEntity realEntity, int entityId, UUID uuid, EntityType type, Location lastPosition) {
            this.realEntity = realEntity;
            this.entityId = entityId;
            this.uuid = uuid;
            this.type = type;
            this.lastPosition = lastPosition;
        }
    }
}
