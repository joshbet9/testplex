package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void combatAndAbilitySlidersChangeProductionRuleResultsAndReset() {
        assertEquals(8.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(4_000L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertEquals(50.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "BOMB_LOBBERS_EXPLOSION_RADIUS", 4.0D,
                "BOMB_LOBBERS_MAX_EXPLOSION_DAMAGE", 30.0D,
                "BOMB_LOBBERS_DIRECT_HIT_DAMAGE", 6.0D,
                "BOMB_LOBBERS_WALL_CONTACT_DELAY_MILLIS", 500L,
                "BOMB_LOBBERS_DOUBLE_JUMP_RECHARGE_MILLIS", 200L,
                "BOMB_LOBBERS_ASSIST_WINDOW_MILLIS", 2_000L,
                "BOMB_LOBBERS_ULTIMATE_KILLER_KILLS", 3));
        assertEquals(4.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(30.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));
        assertEquals(6.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertFalse(ArcadePortRules.shouldPrimeBombLobbersBomb(499L, true));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(500L, true));
        assertEquals(4_150L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertTrue(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(3));

        GameDebugRuntimeOverrides.clear();
        assertEquals(8.0D, ArcadePortRules.bombLobbersExplosionRadius("normal"));
        assertEquals(50.0D, ArcadePortRules.bombLobbersBlastDamage(0.0D, 1.0D));
        assertEquals(10.0D, ArcadePortRules.bombLobbersDirectHitDamage("normal"));
        assertTrue(ArcadePortRules.shouldPrimeBombLobbersBomb(3_000L, true));
        assertEquals(4_000L, ArcadePortRules.nextBombLobbersDoubleJumpReadyAt(3_950L));
        assertFalse(ArcadePortRules.shouldAwardBombLobbersUltimateKiller(3));
    }
}
