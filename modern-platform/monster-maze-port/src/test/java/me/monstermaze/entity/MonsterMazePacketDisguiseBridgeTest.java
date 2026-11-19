package me.monstermaze.entity;

import com.github.retrooper.packetevents.event.PacketListener;
import me.monstermaze.util.MobThemeManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for viewer-local models that do not touch vanilla outbound entity packets. */
class MonsterMazePacketDisguiseBridgeTest {
    @Test
    void selectableModernMobIdsMapToStableClientEntityNames() {
        // PacketEvents' registry is initialized by the server plugin, not in a
        // pure JUnit unit test. Validate all name mappings offline and verify
        // actual model support in the dedicated multi-client Paper acceptance.
        for (String choice : MobThemeManager.allowedThemes(null)) {
            if ("map_default".equals(choice) || "random".equals(choice)) continue;
            String id = MonsterMazePacketDisguiseBridge.clientTypeName(choice);
            assertNotNull(id, choice);
            assertTrue(id.matches("[a-z_]+"), "Invalid entity identifier: " + id);
        }
        assertEquals("wither_skeleton",
                MonsterMazePacketDisguiseBridge.clientTypeName("wither_skeleton"));
        assertEquals("sulfur_cube",
                MonsterMazePacketDisguiseBridge.clientTypeName("sulfur_cube"));
    }

    @Test
    void savedMobThemeAliasesResolveToModernMobNames() {
        assertEquals("snow_golem", MonsterMazePacketDisguiseBridge.clientTypeName("snowman"));
        assertEquals("snow_golem", MonsterMazePacketDisguiseBridge.clientTypeName(null));
        assertEquals("zombified_piglin", MonsterMazePacketDisguiseBridge.clientTypeName("zombie_pigman"));
        assertEquals("enderman", MonsterMazePacketDisguiseBridge.clientTypeName("eyeofender"));
        assertEquals("mooshroom", MonsterMazePacketDisguiseBridge.clientTypeName("mushroom_cow"));
        assertEquals("cave_spider", MonsterMazePacketDisguiseBridge.clientTypeName("Cave Spider"));
        assertEquals("axolotl", MonsterMazePacketDisguiseBridge.clientTypeName("axolotl"));
    }

    @Test
    void sameMobGetsDifferentStableClientOnlyUuidsForDifferentViewers() {
        UUID mob = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID forAlice = MonsterMazePacketDisguiseBridge.virtualUuid(alice, mob);
        UUID forBob = MonsterMazePacketDisguiseBridge.virtualUuid(bob, mob);
        assertNotEquals(mob, forAlice);
        assertNotEquals(forAlice, forBob);
        assertEquals(forAlice, MonsterMazePacketDisguiseBridge.virtualUuid(alice, mob));
        assertNotEquals(forAlice,
                MonsterMazePacketDisguiseBridge.virtualUuid(alice, UUID.randomUUID()));
    }

    @Test
    void noPacketListenerCanRewriteVanillaSpawnOrSnowGolemMetadata() {
        assertFalse(Arrays.asList(MonsterMazePacketDisguiseBridge.class.getInterfaces())
                .contains(PacketListener.class));
        assertTrue(Arrays.stream(MonsterMazePacketDisguiseBridge.class.getDeclaredMethods())
                .noneMatch(method -> "onPacketSend".equals(method.getName())));
    }
}
