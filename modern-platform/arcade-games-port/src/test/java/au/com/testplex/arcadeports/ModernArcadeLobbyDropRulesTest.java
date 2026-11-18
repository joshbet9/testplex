package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernArcadeLobbyDropRulesTest {
    @Test
    void onlyLobbyDropsAreCancelled() {
        assertTrue(ModernArcadeLobby.shouldCancelLobbyItemDrop(ModernArcadeLobby.WORLD_NAME));
        assertFalse(ModernArcadeLobby.shouldCancelLobbyItemDrop("world"));
        assertFalse(ModernArcadeLobby.shouldCancelLobbyItemDrop("arcadeport_source_dragon_escape"));
    }
}
