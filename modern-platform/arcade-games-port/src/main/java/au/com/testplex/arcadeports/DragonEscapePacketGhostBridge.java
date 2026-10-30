package au.com.testplex.arcadeports;

import com.github.retrooper.packetevents.PacketEvents;
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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final byte SNEAKING_FLAG = 0x02;
    private static final byte SPRINTING_FLAG = 0x08;
    private static final int PLAYER_SKIN_PARTS_METADATA_INDEX = 17;

    private final Map<UUID, GhostState> ghosts = new HashMap<>();

    public DragonEscapePacketGhostBridge(JavaPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin");
    }

    @Override
    public void update(Player viewer, Location location, boolean sneaking, boolean sprinting, boolean onGround) {
        if (viewer == null || !viewer.isOnline() || location == null || location.getWorld() != viewer.getWorld()) {
            if (viewer != null) hide(viewer);
            return;
        }

        GhostState state = ghosts.get(viewer.getUniqueId());
        if (state == null) {
            state = spawn(viewer, location);
            if (state == null) return;
            ghosts.put(viewer.getUniqueId(), state);
        }

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityTeleport(state.entityId,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getYaw(), location.getPitch(), onGround));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityHeadLook(state.entityId, location.getYaw()));

        byte flags = (byte) (INVISIBLE_FLAG
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

    private GhostState spawn(Player viewer, Location location) {
        User viewerUser = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (viewerUser == null || viewerUser.getProfile() == null) return null;

        UUID ghostUuid = UUID.nameUUIDFromBytes(
                ("testplex:dragon-escape-pb-ghost:" + viewer.getUniqueId())
                        .getBytes(StandardCharsets.UTF_8));
        String suffix = String.format("%08x", ghostUuid.getLeastSignificantBits()).substring(0, 8);
        String profileName = "DEGhost" + suffix;
        String teamName = "deg" + suffix;
        int entityId = -1 - (ghostUuid.hashCode() & 0x3fffffff);

        UserProfile sourceProfile = viewerUser.getProfile();
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
                        WrapperPlayServerTeams.NameTagVisibility.NEVER,
                        WrapperPlayServerTeams.CollisionRule.NEVER,
                        NamedTextColor.WHITE,
                        WrapperPlayServerTeams.OptionData.FRIENDLY_CAN_SEE_INVISIBLE);
        // Team membership must include the viewer as well as the fake player:
        // vanilla only renders invisible team-mates translucently to team members.
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerTeams(teamName, WrapperPlayServerTeams.TeamMode.CREATE,
                        teamInfo, List.of(viewer.getName(), profileName)));

        // Player entities use the dedicated Add Player packet, not Add Entity.
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerSpawnPlayer(entityId, ghostUuid,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getYaw(), location.getPitch(), List.of()));

        byte flags = INVISIBLE_FLAG;
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerEntityMetadata(entityId, List.of(
                        new EntityData<>(0, EntityDataTypes.BYTE, flags),
                        new EntityData<>(6, EntityDataTypes.ENTITY_POSE, EntityPose.STANDING),
                        new EntityData<>(PLAYER_SKIN_PARTS_METADATA_INDEX, EntityDataTypes.BYTE, (byte) 0x7f))));

        return new GhostState(entityId, ghostUuid, teamName, flags, EntityPose.STANDING);
    }

    @Override
    public void hide(Player viewer) {
        if (viewer == null) return;
        GhostState state = ghosts.remove(viewer.getUniqueId());
        if (state == null || !viewer.isOnline()) return;

        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerDestroyEntities(state.entityId));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerTeams(state.teamName, WrapperPlayServerTeams.TeamMode.REMOVE,
                        (WrapperPlayServerTeams.ScoreBoardTeamInfo) null));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                new WrapperPlayServerPlayerInfoRemove(state.profileUuid));

        // The viewer was temporarily added to the private ghost team. Put them
        // back into whichever real team the server scoreboard currently assigns.
        Team currentTeam = viewer.getScoreboard().getEntryTeam(viewer.getName());
        if (currentTeam != null) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer,
                    new WrapperPlayServerTeams(currentTeam.getName(),
                            WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
                            (WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
                            viewer.getName()));
        }
    }

    private static final class GhostState {
        private final int entityId;
        private final UUID profileUuid;
        private final String teamName;
        private byte entityFlags;
        private EntityPose entityPose;

        private GhostState(int entityId, UUID profileUuid, String teamName,
                           byte entityFlags, EntityPose entityPose) {
            this.entityId = entityId;
            this.profileUuid = profileUuid;
            this.teamName = teamName;
            this.entityFlags = entityFlags;
            this.entityPose = entityPose;
        }
    }
}
