package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HoopsRemasteredRulesTest {
    @Test void supportsTwoTeamLobbiesUpToSixteenPlayers() {
        assertFalse(HoopsRemasteredRules.supportsPlayerCount(1));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(2));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(10));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(11));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(16));
        assertFalse(HoopsRemasteredRules.supportsPlayerCount(17));
    }

    @Test void twoAndThreePointShotBoundaryIsExplicit() {
        assertEquals(2, HoopsRemasteredRules.scoreValue(12.49D));
        assertEquals(3, HoopsRemasteredRules.scoreValue(12.5D));
        assertEquals(2, HoopsRemasteredRules.scoreValue(Double.NaN));
        assertEquals(2, HoopsRemasteredRules.scoreValue(Double.POSITIVE_INFINITY));
    }

    @Test void shootingRequiresAimingAtTheHoopAndRejectsInvalidAngles() {
        assertTrue(HoopsRemasteredRules.isAimedAtHoop(true, 2.25D));
        assertFalse(HoopsRemasteredRules.isAimedAtHoop(true, 2.26D));
        assertFalse(HoopsRemasteredRules.isAimedAtHoop(false, 0.0D));
        assertFalse(HoopsRemasteredRules.isAimedAtHoop(true, Double.NaN));
    }

    @Test void layupsAndDunksHaveSeparatePositionRequirements() {
        assertTrue(HoopsRemasteredRules.isLayupEligible(true, 6.0D, true));
        assertFalse(HoopsRemasteredRules.isLayupEligible(false, 4.0D, true));
        assertFalse(HoopsRemasteredRules.isLayupEligible(true, 6.01D, true));
        assertTrue(HoopsRemasteredRules.isDunkEligible(true, true, 4.5D, true));
        assertFalse(HoopsRemasteredRules.isDunkEligible(true, false, 4.0D, true));
        assertFalse(HoopsRemasteredRules.isDunkEligible(false, true, 4.0D, true));
        assertFalse(HoopsRemasteredRules.isDunkEligible(true, true, 4.51D, true));
        assertFalse(HoopsRemasteredRules.isDunkEligible(true, true, 4.0D, false));
    }

    @Test void timedBlocksHaveAnExplicitCooldown() {
        assertEquals(700L, HoopsRemasteredRules.BLOCK_COOLDOWN_MILLIS);
        assertTrue(HoopsRemasteredRules.BLOCK_RADIUS > 2.0D);
    }

    @Test void rimIntersectionUsesInterpolatedCrossingPoint() {
        // The ball crosses at x=0 halfway through this tick; the end position
        // is outside the rim radius, so endpoint-only checks would miss it.
        assertTrue(HoopsRemasteredRules.crossesHoopPlane(
                -1.5D, 67.0D, 0.0D, 1.5D, 65.0D, 0.0D,
                0.0D, 66.0D, HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(
                1.5D, 67.0D, 0.0D, 2.5D, 65.0D, 0.0D,
                0.0D, 66.0D, HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(
                0.0D, 65.0D, 0.0D, 0.0D, 67.0D, 0.0D,
                0.0D, 66.0D, HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(
                0.0D, 67.0D, 0.0D, 0.0D, 66.5D, 0.0D,
                0.0D, 66.0D, HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(
                Double.NaN, 67.0D, 0.0D, 0.0D, 65.0D, 0.0D,
                0.0D, 66.0D, HoopsRemasteredRules.RIM_SCORING_RADIUS));
    }

    @Test void firstTeamToReachTwentyOneWinsImmediatelyWithoutWinByTwo() {
        assertFalse(HoopsRemasteredRules.hasReachedTargetScore(20, 19));
        assertTrue(HoopsRemasteredRules.hasReachedTargetScore(21, 20));
        assertTrue(HoopsRemasteredRules.hasReachedTargetScore(20, 21));
        assertTrue(HoopsRemasteredRules.hasReachedTargetScore(23, 18));
    }

    @Test void aTiedFiveMinuteScoreEntersNextBasketWinsOvertime() {
        assertTrue(HoopsRemasteredRules.isTiedAtTimeLimit(10, 10));
        assertTrue(HoopsRemasteredRules.isTiedAtTimeLimit(20, 20));
        assertFalse(HoopsRemasteredRules.isTiedAtTimeLimit(11, 10));
        assertFalse(HoopsRemasteredRules.isTiedAtTimeLimit(10, 11));
    }

    @Test void anyBasketWinsInOvertimeButRegulationStillUsesFirstToTwentyOne() {
        assertFalse(HoopsRemasteredRules.isWinningBasket(20, 19, false));
        assertTrue(HoopsRemasteredRules.isWinningBasket(21, 20, false));
        assertTrue(HoopsRemasteredRules.isWinningBasket(20, 21, false));
        assertTrue(HoopsRemasteredRules.isWinningBasket(10, 10, true));
        assertTrue(HoopsRemasteredRules.isWinningBasket(10, 12, true));
    }

    @Test void offensiveReboundsHaveAShorterShotClockThanDefensiveRebounds() {
        assertEquals(14_000L, HoopsRemasteredRules.OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS);
        assertEquals(24_000L, HoopsRemasteredRules.SHOT_CLOCK_MILLIS);
        assertTrue(HoopsRemasteredRules.OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS
                < HoopsRemasteredRules.SHOT_CLOCK_MILLIS);
    }

    @Test void leadingTeamReturnsNoWinnerForTies() {
        assertEquals(-1, HoopsRemasteredRules.leadingTeam(10, 10));
        assertEquals(0, HoopsRemasteredRules.leadingTeam(11, 10));
        assertEquals(1, HoopsRemasteredRules.leadingTeam(9, 12));
    }

    @Test void clutchAndBuzzerWindowsAreInclusiveAndBounded() {
        assertTrue(HoopsRemasteredRules.isBuzzerBeater(0));
        assertTrue(HoopsRemasteredRules.isBuzzerBeater(3_000));
        assertFalse(HoopsRemasteredRules.isBuzzerBeater(3_001));
        assertTrue(HoopsRemasteredRules.isClutchTime(60_000));
        assertFalse(HoopsRemasteredRules.isClutchTime(60_001));
    }

    @Test void HopsUsesEightSecondRechargeAndCannotFireEarly() {
        assertEquals(8_000L, HoopsRemasteredRules.HOPS_COOLDOWN_MILLIS);
        assertTrue(HoopsRemasteredRules.isHopsReady(10_000L, 10_000L));
        assertFalse(HoopsRemasteredRules.isHopsReady(9_999L, 10_000L));
        assertTrue(HoopsRemasteredRules.isHopsReady(18_000L, 10_000L));
    }

    @Test void assistWindowRejectsStaleOrFuturePasses() {
        assertTrue(HoopsRemasteredRules.isAssistEligible(10_000, 2_000));
        assertFalse(HoopsRemasteredRules.isAssistEligible(10_001, 2_000));
        assertFalse(HoopsRemasteredRules.isAssistEligible(1_999, 2_000));
        assertFalse(HoopsRemasteredRules.isAssistEligible(10_000, 0));
    }

    @Test void doubleDoubleAchievementsUseFiveBoxScoreCategories() {
        assertTrue(HoopsRemasteredRules.isDoubleDouble(3, 3, 0, 0, 0));
        assertFalse(HoopsRemasteredRules.isTripleDouble(3, 3, 0, 0, 0));
        assertTrue(HoopsRemasteredRules.isTripleDouble(3, 3, 3, 0, 0));
        assertTrue(HoopsRemasteredRules.isQuadrupleDouble(3, 3, 3, 3, 0));
        assertTrue(HoopsRemasteredRules.isQuintupleDouble(3, 3, 3, 3, 3));
        assertFalse(HoopsRemasteredRules.isQuintupleDouble(3, 3, 3, 2, 3));
    }

    @Test void comebackRequiresAFullTenPointDeficit() {
        assertFalse(HoopsRemasteredRules.isComebackEligible(10, 19));
        assertTrue(HoopsRemasteredRules.isComebackEligible(10, 20));
    }

    @Test void spawnAssignmentAlternatesTeamsAndUsesSeparateCourtHalves() {
        assertEquals(0, HoopsRemasteredRules.teamForSpawnIndex(0));
        assertEquals(1, HoopsRemasteredRules.teamForSpawnIndex(1));
        assertEquals(0, HoopsRemasteredRules.teamForSpawnIndex(2));
        assertEquals(-10.0D, HoopsRemasteredRules.spawnX(0));
        assertEquals(10.0D, HoopsRemasteredRules.spawnX(1));
        assertEquals(-8.0D, HoopsRemasteredRules.spawnZ(0));
        assertEquals(8.0D, HoopsRemasteredRules.spawnZ(7));
        Set<Double> lanes = new HashSet<>();
        for (int player = 0; player < 8; player++) lanes.add(HoopsRemasteredRules.spawnZ(player));
        assertEquals(8, lanes.size(), "Each team must have eight distinct spawn lanes for 8v8.");
    }

    @Test void clockFormatsMinutesAndRoundsRemainingSecondsUp() {
        assertEquals("5:00", HoopsRemasteredRules.formatClock(300_000));
        assertEquals("1:01", HoopsRemasteredRules.formatClock(60_001));
        assertEquals("0:01", HoopsRemasteredRules.formatClock(1));
        assertEquals("0:00", HoopsRemasteredRules.formatClock(0));
    }
}
