package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombLobbersDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void powerupToggleIsProfileBackedAndConsumedByTheRuntimeRule() {
        assertTrue(ArcadePortRules.bombLobbersPowerupsEnabled());
        assertTrue(GameDebugVariableCatalog.variables(ArcadePortGame.BOMB_LOBBERS).stream()
                .anyMatch(variable -> variable.key().equals("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED")
                        && variable.kind() == GameDebugVariableCatalog.Kind.BOOLEAN));

        GameDebugRuntimeOverrides.activate(Map.of("BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED", false));
        assertFalse(ArcadePortRules.bombLobbersPowerupsEnabled());

        GameDebugRuntimeOverrides.clear();
        assertTrue(ArcadePortRules.bombLobbersPowerupsEnabled());
    }
}
