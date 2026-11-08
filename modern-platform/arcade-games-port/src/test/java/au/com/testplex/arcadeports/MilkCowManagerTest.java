package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class MilkCowManagerTest {
    @Test
    void controllerClaimsOnlyMilkTheCow() {
        MilkCowManager milk = new MilkCowManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(milk);

        assertSame(milk, registry.get(ArcadePortGame.MILK_THE_COW));
        assertNull(registry.get(ArcadePortGame.WITHER_ASSAULT));
    }

    @Test
    void uniqueLeaderRejectsTiesAndEmptyMatches() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        assertSame(first, MilkCowManager.uniqueLeader(Map.of(first, 12, second, 10)));
        assertNull(MilkCowManager.uniqueLeader(Map.of(first, 12, second, 12)));
        assertNull(MilkCowManager.uniqueLeader(Map.of()));
    }

    @Test
    void herdAndMapAnimalsDoNotSpawnForMissingMatchData() {
        MilkCowManager controller = new MilkCowManager();
        SessionState session = new SessionState(ArcadePortGame.MILK_THE_COW, null, List.of(), false);

        controller.spawnMapAnimals(session, List.of(), EntityType.CHICKEN);
        controller.updateCowHunger(session, null, 100L);
        controller.updateHerd(session, null, 100L, true);
        controller.updateHerd(null, null, 100L, false);

        assertTrue(session.entities.isEmpty());
        assertTrue(session.milkCowHerd.isEmpty());
        assertTrue(session.milkChargeHungerAt.isEmpty());
    }
}
