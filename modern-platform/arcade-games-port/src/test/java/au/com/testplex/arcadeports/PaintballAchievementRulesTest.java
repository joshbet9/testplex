package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaintballAchievementRulesTest {
    @Test
    void sourceTrackersKeepTheirExactEventThresholds() {
        assertFalse(PaintballAchievementRules.killingSpree(3));
        assertTrue(PaintballAchievementRules.killingSpree(4));
        assertFalse(PaintballAchievementRules.lastStand(2));
        assertTrue(PaintballAchievementRules.lastStand(3));
        assertFalse(PaintballAchievementRules.medic(0));
        assertTrue(PaintballAchievementRules.medic(1));
        assertFalse(PaintballAchievementRules.rescueRelay(4));
        assertTrue(PaintballAchievementRules.rescueRelay(5));
    }

    @Test
    void teamAndKitAchievementsRequireDistinctQualifyingProgress() {
        Set<UUID> threeTargets = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertFalse(PaintballAchievementRules.sniperLongRange(Set.copyOf(threeTargets.stream().limit(2).toList())));
        assertTrue(PaintballAchievementRules.sniperLongRange(threeTargets));
        assertFalse(PaintballAchievementRules.secondWind(true, Set.of(UUID.randomUUID())));
        assertFalse(PaintballAchievementRules.secondWind(false, threeTargets));
        assertTrue(PaintballAchievementRules.secondWind(true, threeTargets));
        assertTrue(PaintballAchievementRules.soloBeforeRevive(1));
        assertFalse(PaintballAchievementRules.soloBeforeRevive(2));
        assertTrue(PaintballAchievementRules.flawlessVictory(true, 0));
        assertFalse(PaintballAchievementRules.flawlessVictory(true, 1));
        assertFalse(PaintballAchievementRules.speedrunner(true, 30_000));
        assertTrue(PaintballAchievementRules.speedrunner(true, 29_999));
        assertFalse(PaintballAchievementRules.speedrunner(false, 1));
        assertFalse(PaintballAchievementRules.fourColourArsenal(Set.of("rifle", "shotgun", "machine-gun")));
        assertTrue(PaintballAchievementRules.fourColourArsenal(Set.of("rifle", "shotgun", "machine-gun", "sniper")));
    }
}
