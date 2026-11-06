package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeAchievementProgressTest {
    @Test void supportedProgressIsBoundToThePersistedMetricAndActualThreshold() {
        var masterBomber = ArcadeAchievementProgress.counter("bomb-lobbers", "master-bomber");
        assertNotNull(masterBomber);
        assertEquals("wins", masterBomber.statKey());
        assertEquals(30, masterBomber.target());
        assertEquals("map-wins.through-hell",
                ArcadeAchievementProgress.counter("dragon-escape", "to-hell-and-back").statKey());
    }

    @Test void OneMatchChallengesCannotBeFakedFromLifetimeTotals() {
        assertNull(ArcadeAchievementProgress.counter("bomb-lobbers", "ultimate-killer"));
        assertNull(ArcadeAchievementProgress.counter("camera-shy", "double-elimination"));
    }

    @Test void removedEvolutionAchievementsDoNotExposeObsoleteLifetimeProgress() {
        assertNull(ArcadeAchievementProgress.counter("evolution", "expert-evolver"));
        assertNull(ArcadeAchievementProgress.counter("evolution", "no-evolve-5-u"));
    }
}
