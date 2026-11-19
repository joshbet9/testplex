package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeAchievementProgressTest {
    @Test void supportedProgressIsBoundToThePersistedMetricAndActualThreshold() {
        assertNull(ArcadeAchievementProgress.counter("bomb-lobbers", "master-bomber"));
        assertNull(ArcadeAchievementProgress.counter("dragon-escape", "douglas-defeater"));
        assertNull(ArcadeAchievementProgress.counter("sheep-quest", "hungry-hungry-hippo"));
        assertEquals("map-wins.through-hell",
                ArcadeAchievementProgress.counter("dragon-escape", "to-hell-and-back").statKey());
        assertEquals(100, ArcadeAchievementProgress.counter("sheep-quest", "thief").target());
        assertEquals(100, ArcadeAchievementProgress.counter("sheep-quest", "animal-rescue").target());
    }

    @Test void OneMatchChallengesCannotBeFakedFromLifetimeTotals() {
        assertNull(ArcadeAchievementProgress.counter("bomb-lobbers", "ultimate-killer"));
        assertNull(ArcadeAchievementProgress.counter("camera-shy", "double-elimination"));
        assertNull(ArcadeAchievementProgress.counter("bacon-brawl", "king-of-bacon"));
    }

    @Test void removedEvolutionAchievementsDoNotExposeObsoleteLifetimeProgress() {
        assertNull(ArcadeAchievementProgress.counter("evolution", "expert-evolver"));
        assertNull(ArcadeAchievementProgress.counter("evolution", "no-evolve-5-u"));
    }
}
