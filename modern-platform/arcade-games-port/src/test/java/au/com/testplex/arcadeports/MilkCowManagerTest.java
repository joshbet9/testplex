package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.UUID;
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
}
