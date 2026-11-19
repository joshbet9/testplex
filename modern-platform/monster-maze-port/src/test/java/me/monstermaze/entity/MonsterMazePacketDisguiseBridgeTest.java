package me.monstermaze.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonsterMazePacketDisguiseBridgeTest {
    @Test
    void convertsSavedMobThemeAliasesToModernClientEntityNames() {
        assertEquals("snow_golem", MonsterMazePacketDisguiseBridge.clientTypeName("snowman"));
        assertEquals("snow_golem", MonsterMazePacketDisguiseBridge.clientTypeName(null));
        assertEquals("zombified_piglin", MonsterMazePacketDisguiseBridge.clientTypeName("zombie_pigman"));
        assertEquals("enderman", MonsterMazePacketDisguiseBridge.clientTypeName("eyeofender"));
        assertEquals("mooshroom", MonsterMazePacketDisguiseBridge.clientTypeName("mushroom_cow"));
        assertEquals("cave_spider", MonsterMazePacketDisguiseBridge.clientTypeName("Cave Spider"));
        assertEquals("axolotl", MonsterMazePacketDisguiseBridge.clientTypeName("axolotl"));
    }

    @Test
    void doNotSendSnowGolemSpecificMetadataToOtherMobClientRenderers() {
        assertTrue(MonsterMazePacketDisguiseBridge.isCrossMobMetadataIndex(0));
        assertTrue(MonsterMazePacketDisguiseBridge.isCrossMobMetadataIndex(7));
        assertFalse(MonsterMazePacketDisguiseBridge.isCrossMobMetadataIndex(8));
        assertFalse(MonsterMazePacketDisguiseBridge.isCrossMobMetadataIndex(16));
        assertFalse(MonsterMazePacketDisguiseBridge.isCrossMobMetadataIndex(-1));
    }
}
