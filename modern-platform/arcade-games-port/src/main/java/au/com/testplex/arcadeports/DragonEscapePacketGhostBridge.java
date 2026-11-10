package au.com.testplex.arcadeports;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * Sends a genuine client-side player entity for the owner's personal-best run.
 *
 * The entity is packet-only: it has no server hitbox, AI, collision, inventory,
 * or gameplay interaction. An invisible flag plus a viewer-local scoreboard
 * team with seeFriendlyInvisibles enabled gives the normal player model its
 * vanilla translucent rendering.
 */
public final class DragonEscapePacketGhostBridge implements DragonEscapeGhostBridge {
    private static final byte INVISIBLE_FLAG = 0x20;
    private static final byte GLOWING_FLAG = 0x40;
    private static final byte SNEAKING_FLAG = 0x02;
    private static final byte SPRINTING_FLAG = 0x08;

    private final Map<UUID, Map<String, GhostState>> ghosts = new HashMap<>();

    public DragonEscapePacketGhostBridge(JavaPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin");
    }

    @Override
    public void update(Player viewer, Location location, boolean sneaking, boolean sprinting, boolean onGround) {
        update(viewer, viewer == null ? null : viewer.getUniqueId(),
                location, sneaking, sprinting, onGround);
    }

    @Override
    public void update(Player viewer, UUID sourcePlayerId, Location location,
                       boolean sneaking, boolean sprinting, boolean onGround) {
        update(viewer, "personal", sourcePlayerId, location, sneaking, sprinting, onGround);
    }

    @Override
    public void update(Player viewer, String ghostKey, UUID sourcePlayerId, Location location,
                       boolean sneaking, boolean sprinting, boolean onGround) {
        if (viewer == null || !viewer.isOnline() || location == null || location.getWorld() != viewer.getWorld()) {
            if (viewer != null) hide(viewer, ghostKey);
            return;
        }

        String key = ghostKey == null || ghostKey.isBlank() ? "personal" : ghostKey;
        Map<String, GhostState> viewerGhosts = ghosts.computeIfAbsent(
                viewer.getUniqueId(), ignored -> new HashMap<>());
        GhostState state = viewerGhosts.get(key);
        if (state != null && !Objects.equals(state.sourcePlayerId, sourcePlayerId)) {
            hide(viewer, key);
            viewerGhosts = ghosts.computeIfAbsent(viewer.getUniqueId(), ignored -> new HashMap<>());
            state = null;
        }
        if (state == null) {
            state = spawn(viewer, key, sourcePlayerId, location);
            if (state == null) return;
            viewerGhosts.put(key, state);
        }

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityTeleport(state.entityId,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getYaw(), location.getPitch(), onGround));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityHeadLook(state.entityId, location.getYaw()));

        byte flags = (byte) (INVISIBLE_FLAG | GLOWING_FLAG
                | (sneaking ? SNEAKING_FLAG : 0)
                | (sprinting ? SPRINTING_FLAG : 0));
        EntityPose pose = sneaking ? EntityPose.CROUCHING : EntityPose.STANDING;
        if (flags != state.entityFlags || pose != state.entityPose) {
            state.entityFlags = flags;
            state.entityPose = pose;
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerEntityMetadata(state.entityId, List.of(
                            new EntityData<>(0, EntityDataTypes.BYTE, flags),
                            new EntityData<>(6, EntityDataTypes.ENTITY_POSE, pose))));
        }
    }

    private GhostState spawn(Player viewer, String ghostKey, UUID sourcePlayerId, Location location) {
        User viewerUser = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (viewerUser == null || viewerUser.getProfile() == null) return null;

        UUID ghostUuid = UUID.nameUUIDFromBytes(
                ("testplex:dragon-escape-ghost:" + viewer.getUniqueId() + ":" + ghostKey)
                        .getBytes(StandardCharsets.UTF_8));
        // Use the racer’s name for the overhead label so the replay is identifiable.
        String sourceName = sourcePlayerId == null ? null : Bukkit.getOfflinePlayer(sourcePlayerId).getName();
        String displayName = sourceName == null || sourceName.isBlank() ? viewer.getName() : sourceName;
        // The profile and team entry must be unique per ghost. The visible custom name
        // remains the real racer's name, even when one racer owns multiple selected records.
        String profileName = profileName(ghostUuid);
        String suffix = String.format("%08x", ghostUuid.getLeastSignificantBits()).substring(0, 8);
        String teamName = "deg" + suffix;
        // Keep packet-only NPC IDs positive and far outside normal server entity IDs.
        int entityId = 1_800_000_000 - (ghostUuid.hashCode() & 0x1fffffff);

        Player sourceOnline = sourcePlayerId == null ? null : Bukkit.getPlayer(sourcePlayerId);
        User sourceUser = sourceOnline == null ? null : PacketEvents.getAPI().getPlayerManager().getUser(sourceOnline);
        UserProfile sourceProfile = sourceUser != null && sourceUser.getProfile() != null
                ? sourceUser.getProfile() : viewerUser.getProfile();
        UserProfile ghostProfile = new UserProfile(ghostUuid, profileName);
        for (TextureProperty property : sourceProfile.getTextureProperties()) {
            ghostProfile.getTextureProperties().add(new TextureProperty(
                    property.getName(), property.getValue(), property.getSignature()));
        }

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        ghostProfile, false, 0, GameMode.SURVIVAL, null, null, 0, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerPlayerInfoUpdate(
                        EnumSet.of(
                                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_HAT),
                        info));

        WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo =
                new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                        Component.text("Dragon Escape PB Ghost"),
                        Component.empty(),
                        Component.empty(),
                        WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
                        WrapperPlayServerTeams.CollisionRule.NEVER,
                        NamedTextColor.WHITE,
                        WrapperPlayServerTeams.OptionData.FRIENDLY_CAN_SEE_INVISIBLE);
        // The ghost profile name is the source racer's name, so the overhead nametag
        // identifies whose run this ghost represents.
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerTeams(teamName, WrapperPlayServerTeams.TeamMode.CREATE,
                        teamInfo, List.of(profileName)));

        // Since 1.20.2, player-shaped NPCs use Add Entity (EntityTypes.PLAYER).
        // The old Add Player packet does not create a player entity on 26.3 clients.
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerSpawnEntity(entityId, Optional.of(ghostUuid), EntityTypes.PLAYER,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getPitch(), location.getYaw(), location.getYaw(), 0, Optional.empty()));

        byte flags = (byte) (INVISIBLE_FLAG | GLOWING_FLAG);
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityMetadata(entityId, List.of(
                        new EntityData<>(0, EntityDataTypes.BYTE, flags),
                        new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                                Optional.of(Component.text(displayName))),
                        new EntityData<>(3, EntityDataTypes.BOOLEAN, true),
                        new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.STANDING))));

        return new GhostState(entityId, ghostUuid, teamName, profileName, sourcePlayerId,
                flags, EntityPose.STANDING);
    }

    static String profileName(UUID ghostUuid) {
        String suffix = String.format("%08x", ghostUuid.getLeastSignificantBits()).substring(0, 8);
        return "DEGhost" + suffix;
    }

    @Override
    public void hide(Player viewer) {
        if (viewer == null) return;
        Map<String, GhostState> removed = ghosts.remove(viewer.getUniqueId());
        if (removed == null || !viewer.isOnline()) return;
        for (GhostState state : removed.values()) hideState(viewer, state);
    }

    @Override
    public void hide(Player viewer, String ghostKey) {
        if (viewer == null || ghostKey == null) return;
        Map<String, GhostState> viewerGhosts = ghosts.get(viewer.getUniqueId());
        if (viewerGhosts == null) return;
        GhostState state = viewerGhosts.remove(ghostKey);
        if (viewerGhosts.isEmpty()) ghosts.remove(viewer.getUniqueId());
        if (state != null && viewer.isOnline()) hideState(viewer, state);
    }

    private void hideState(Player viewer, GhostState state) {

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerDestroyEntities(state.entityId));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerTeams(state.teamName, WrapperPlayServerTeams.TeamMode.REMOVE,
                        (WrapperPlayServerTeams.ScoreBoardTeamInfo) null));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerPlayerInfoRemove(state.profileUuid));

    }

    private static final class GhostState {
        private final int entityId;
        private final UUID profileUuid;
        private final String teamName;
        private final String profileName;
        private final UUID sourcePlayerId;
        private byte entityFlags;
        private EntityPose entityPose;

        private GhostState(int entityId, UUID profileUuid, String teamName, String profileName, UUID sourcePlayerId,
                           byte entityFlags, EntityPose entityPose) {
            this.entityId = entityId;
            this.profileUuid = profileUuid;
            this.teamName = teamName;
            this.profileName = profileName;
            this.sourcePlayerId = sourcePlayerId;
            this.entityFlags = entityFlags;
            this.entityPose = entityPose;
        }
    }
}
