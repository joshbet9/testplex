package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class MicroBattlesManagerTest {
    @Test
    void controllerClaimsOnlyMicroBattles() {
        MicroBattlesManager micro = new MicroBattlesManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(micro);

        assertSame(micro, registry.get(ArcadePortGame.MICRO_BATTLES));
        assertNull(registry.get(ArcadePortGame.TURF_WARS));
    }

    @Test
    void noWorldOrPlayerListDoesNotSpawnParticles() {
        MicroBattlesManager micro = new MicroBattlesManager();
        SessionState session = new SessionState(ArcadePortGame.MICRO_BATTLES, null, List.of(), false);

        micro.updateBorderParticles(session, List.of(), ignored -> true);
        micro.updateBorderParticles(null, List.of(), ignored -> true);
        micro.updateBorderParticles(session, null, ignored -> true);
    }
}
