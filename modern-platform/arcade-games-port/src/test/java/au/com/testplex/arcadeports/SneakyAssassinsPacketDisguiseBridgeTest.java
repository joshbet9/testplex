package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SneakyAssassinsPacketDisguiseBridgeTest {
    @Test
    void forwardsOnlyEntityMetadataSharedByPlayersAndVillagers() {
        for (int index = 0; index <= 7; index++) {
            assertTrue(SneakyAssassinsPacketDisguiseBridge.isVillagerCompatibleMetadataIndex(index),
                    "common entity metadata index should be forwarded: " + index);
        }

        for (int playerSpecificIndex = 8; playerSpecificIndex <= 17; playerSpecificIndex++) {
            assertFalse(SneakyAssassinsPacketDisguiseBridge.isVillagerCompatibleMetadataIndex(playerSpecificIndex),
                    "player-specific metadata must not be sent to a Villager: " + playerSpecificIndex);
        }
    }

    @Test
    void rejectsInvalidMetadataIndices() {
        assertFalse(SneakyAssassinsPacketDisguiseBridge.isVillagerCompatibleMetadataIndex(-1));
        assertFalse(SneakyAssassinsPacketDisguiseBridge.isVillagerCompatibleMetadataIndex(Integer.MAX_VALUE));
    }

    @Test
    void keepsAlreadyCompatibleVillagerMetadataOnItsOriginalPacket() {
        assertFalse(SneakyAssassinsPacketDisguiseBridge.containsVillagerIncompatibleMetadata(
                IntStream.of(0, 2, 3, 7)));
    }

    @Test
    void filtersPlayerOnlyMetadataBeforeForwardingToVillager() {
        assertTrue(SneakyAssassinsPacketDisguiseBridge.containsVillagerIncompatibleMetadata(
                IntStream.of(0, 8)));
    }
}
