package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HeroesOfGwenManagerTest {
    private final HeroesOfGwenManager manager = new HeroesOfGwenManager();

    @Test
    void heroPreferenceHasSafeDefaultAndRejectsUnknownHeroes() {
        UUID playerId = UUID.randomUUID();
        assertEquals("hattori", manager.preferredHero(playerId));

        manager.setPreferredHero(playerId, "rowena");
        assertEquals("rowena", manager.preferredHero(playerId));
        manager.setPreferredHero(playerId, "not-a-hero");
        assertEquals("rowena", manager.preferredHero(playerId));
        manager.setPreferredHero(null, "hattori");
        assertEquals("hattori", manager.preferredHero(null));
    }

    @Test
    void opponentScalingUsesOnlyTheOtherTeamAndNeverZeroAttackers() {
        Map<UUID, Integer> teams = new HashMap<>();
        teams.put(UUID.randomUUID(), 0);
        assertEquals(ArcadePortRules.heroesOfGwenTowerMaximumHealth(true, 1),
                manager.towerMaximumHealth(teams, 0, true));
        teams.put(UUID.randomUUID(), 1);
        teams.put(UUID.randomUUID(), 1);
        assertEquals(ArcadePortRules.heroesOfGwenTowerMaximumHealth(false, 2),
                manager.towerMaximumHealth(teams, 0, false));
    }

    @Test
    void refreshPreservesHealthFractionAndRoundsUp() {
        assertEquals(51.0D, manager.rescaleTowerHealth(25.1D, 100.0D, 200.0D));
        assertEquals(1.0D, manager.rescaleTowerHealth(0.0D, 100.0D, 200.0D));
        assertEquals(200.0D, manager.rescaleTowerHealth(50.0D, 0.0D, 200.0D));
    }
}
