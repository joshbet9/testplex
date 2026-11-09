package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.bukkit.entity.Player;
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
    void winAchievementsAreAwardedThroughTheInjectedGameCallback() {
        EvolutionManager evolution = new EvolutionManager();
        SessionState session = new SessionState(ArcadePortGame.EVOLUTION, null, List.of(), false);
        UUID winnerId = UUID.randomUUID();
        Player winner = playerStub(winnerId);
        List<String> awarded = new ArrayList<>();

        evolution.recordWinAchievements(session, winner, (player, achievementId) -> {
            assertSame(winner, player);
            awarded.add(achievementId);
        });

        assertTrue(awarded.contains("perfect-game"));
        session.evolutionDeaths.add(winnerId);
        awarded.clear();
        evolution.recordWinAchievements(session, winner, (player, achievementId) -> awarded.add(achievementId));
        assertFalse(awarded.contains("perfect-game"));
    }

    private static Player playerStub(UUID id) {
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[] {Player.class},
                (proxy, method, args) -> "getUniqueId".equals(method.getName()) ? id : null);
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
