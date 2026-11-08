package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvolutionManagerTest {
    @Test
    void registeredAsEvolutionOnly() {
        EvolutionManager evolution = new EvolutionManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(evolution);
        assertSame(evolution, registry.get(ArcadePortGame.EVOLUTION));
        assertNull(registry.get(ArcadePortGame.RUNNER));
    }

    @Test
    void darwinistCooldownAndNeanderthalPityRemainKitSpecific() {
        EvolutionManager evolution = new EvolutionManager();
        SessionState session = new SessionState(ArcadePortGame.EVOLUTION, null, List.of(), false);
        UUID playerId = UUID.randomUUID();
        assertEquals(EvolutionAchievementRules.darwinistCooldown(9_000L),
                evolution.kitCooldown(session, playerId, 9_000L));
        session.evolutionKits.put(playerId, "neanderthal");
        assertEquals(9_000L, evolution.kitCooldown(session, playerId, 9_000L));
        evolution.reducePity(session, playerId, 1_000L, 10_000L, true);
        assertEquals(1, session.evolutionPityKillReductionsThisCycle.get(playerId));
    }
}
