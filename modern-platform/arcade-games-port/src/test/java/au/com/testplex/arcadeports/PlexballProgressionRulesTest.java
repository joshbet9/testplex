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
    void difficultDistanceAchievementsUseRaisedThresholds() {
        assertFalse(PlexballProgressionRules.shouldAwardSweetSpot(74.99D));
        assertTrue(PlexballProgressionRules.shouldAwardSweetSpot(75.0D));
        assertFalse(PlexballProgressionRules.shouldAwardWarningTrack(94.99D));
        assertTrue(PlexballProgressionRules.shouldAwardWarningTrack(95.0D));
        assertFalse(PlexballProgressionRules.shouldAwardSweetSpot(Double.NaN));
    }

    @Test
    void perfectContactAndFieldingAchievementsRequireRepeatedSkill() {
        assertTrue(PlexballProgressionRules.isPerfectContact(1.0D));
        assertTrue(PlexballProgressionRules.isPerfectContact(0.99D));
        assertFalse(PlexballProgressionRules.isPerfectContact(0.98D));
        assertFalse(PlexballProgressionRules.isPerfectContact(Double.NaN));
        assertFalse(PlexballProgressionRules.shouldAwardPerfectContact(2));
        assertTrue(PlexballProgressionRules.shouldAwardPerfectContact(3));

        assertFalse(PlexballProgressionRules.shouldAwardStickyGloves(2));
        assertTrue(PlexballProgressionRules.shouldAwardStickyGloves(3));
        assertFalse(PlexballProgressionRules.shouldAwardGoldenGlove(4));
        assertTrue(PlexballProgressionRules.shouldAwardGoldenGlove(5));
        assertTrue(PlexballProgressionRules.shouldAwardAllRounder(75.0D, 1));
        assertFalse(PlexballProgressionRules.shouldAwardAllRounder(74.99D, 3));
        assertFalse(PlexballProgressionRules.shouldAwardAllRounder(75.0D, 0));
    }

    @Test
    void clutchAndChampionAchievementsNeedBigPerformances() {
        assertTrue(PlexballProgressionRules.shouldAwardClutchPerformer(true, 2));
        assertFalse(PlexballProgressionRules.shouldAwardClutchPerformer(true, 1));
        assertFalse(PlexballProgressionRules.shouldAwardClutchPerformer(false, 3));
        assertFalse(PlexballProgressionRules.shouldAwardChampion(4));
        assertTrue(PlexballProgressionRules.shouldAwardChampion(5));
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
    void milestoneCatalogueContainsOnlyTheFourRequestedCareerGoals() {
        assertEquals(4, PlexballMilestone.values().length);
        assertEquals(250L, milestone(PlexballMilestone.Metric.WINS).target());
        assertEquals(300L, milestone(PlexballMilestone.Metric.CATCHES).target());
        assertEquals(100L, milestone(PlexballMilestone.Metric.HOME_RUNS).target());
        PlexballMilestone distance = milestone(PlexballMilestone.Metric.DISTANCE_TENTHS);
        assertEquals(100_000L, distance.target());
        assertEquals("10km", distance.targetLabel());
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
    }

    private static PlexballMilestone milestone(PlexballMilestone.Metric metric) {
        return java.util.Arrays.stream(PlexballMilestone.values())
                .filter(value -> value.metric() == metric)
                .findFirst().orElseThrow();
    }
}
