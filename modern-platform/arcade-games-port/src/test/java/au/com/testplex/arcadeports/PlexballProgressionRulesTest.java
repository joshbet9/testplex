package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlexballProgressionRulesTest {
    @Test
    void distanceProgressUsesTenthsOfAMetreAndRejectsInvalidValues() {
        assertEquals(0L, PlexballProgressionRules.distanceToTenths(-1.0D));
        assertEquals(0L, PlexballProgressionRules.distanceToTenths(Double.NaN));
        assertEquals(0L, PlexballProgressionRules.distanceToTenths(Double.POSITIVE_INFINITY));
        assertEquals(503L, PlexballProgressionRules.distanceToTenths(50.25D));
        assertEquals(1_000L, PlexballProgressionRules.distanceToTenths(100.0D));
    }

    @Test
    void distanceAchievementsUseUncaughtDistanceThresholds() {
        assertFalse(PlexballProgressionRules.shouldAwardSweetSpot(49.99D));
        assertTrue(PlexballProgressionRules.shouldAwardSweetSpot(50.0D));
        assertFalse(PlexballProgressionRules.shouldAwardWarningTrack(89.99D));
        assertTrue(PlexballProgressionRules.shouldAwardWarningTrack(90.0D));
        assertFalse(PlexballProgressionRules.shouldAwardSweetSpot(Double.NaN));
    }

    @Test
    void skillAndFieldingAchievementsHaveMatchScopedRequirements() {
        assertTrue(PlexballProgressionRules.shouldAwardPerfectContact(1.0D));
        assertTrue(PlexballProgressionRules.shouldAwardPerfectContact(0.99D));
        assertFalse(PlexballProgressionRules.shouldAwardPerfectContact(0.98D));
        assertFalse(PlexballProgressionRules.shouldAwardPerfectContact(Double.NaN));

        assertFalse(PlexballProgressionRules.shouldAwardGoldenGlove(2));
        assertTrue(PlexballProgressionRules.shouldAwardGoldenGlove(3));
        assertTrue(PlexballProgressionRules.shouldAwardAllRounder(50.0D, 1));
        assertFalse(PlexballProgressionRules.shouldAwardAllRounder(50.0D, 0));
        assertFalse(PlexballProgressionRules.shouldAwardAllRounder(49.9D, 3));
        assertTrue(PlexballProgressionRules.shouldAwardClutchPerformer(true, 1));
        assertFalse(PlexballProgressionRules.shouldAwardClutchPerformer(false, 2));
        assertFalse(PlexballProgressionRules.shouldAwardClutchPerformer(true, 0));
    }

    @Test
    void milestoneProgressIsClampedAndHandlesZeroTargets() {
        assertEquals(0, PlexballProgressionRules.milestoneProgressPercent(0, 10));
        assertEquals(50, PlexballProgressionRules.milestoneProgressPercent(5, 10));
        assertEquals(99, PlexballProgressionRules.milestoneProgressPercent(99, 100));
        assertEquals(100, PlexballProgressionRules.milestoneProgressPercent(120, 100));
        assertEquals(100, PlexballProgressionRules.milestoneProgressPercent(0, 0));
        assertEquals(100, PlexballProgressionRules.milestoneProgressPercent(0, -1));
    }

    @Test
    void achievementAndMilestoneCataloguesHaveStableUniqueSlugs() {
        Set<String> achievementSlugs = new HashSet<>();
        for (PlexballAchievement achievement : PlexballAchievement.values()) {
            assertTrue(achievementSlugs.add(achievement.slug()), achievement.slug());
            assertFalse(achievement.configPath(UUID.randomUUID()).isBlank());
            assertNotNull(achievement.icon());
        }
        assertEquals(9, achievementSlugs.size());

        Set<String> milestoneSlugs = new HashSet<>();
        for (PlexballMilestone milestone : PlexballMilestone.values()) {
            assertTrue(milestoneSlugs.add(milestone.slug()), milestone.slug());
            assertTrue(milestone.target() > 0L);
            assertFalse(milestone.configPath(UUID.randomUUID()).isBlank());
            assertFalse(milestone.targetLabel().isBlank());
        }
        assertEquals(24, milestoneSlugs.size());
    }
}
