package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HoopsRemasteredRulesTest {
    @Test void supportsTwoTeamLobbiesUpToSixteenPlayers() {
        assertFalse(HoopsRemasteredRules.supportsPlayerCount(1));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(2));
        assertTrue(HoopsRemasteredRules.supportsPlayerCount(10));
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
    @Test void rimIntersectionUsesInterpolatedCrossingPoint() {
        assertTrue(HoopsRemasteredRules.crossesHoopPlane(-1.5D,67D,0D,1.5D,65D,0D,0D,66D,HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(1.5D,67D,0D,2.5D,65D,0D,0D,66D,HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(0D,65D,0D,0D,67D,0D,0D,66D,HoopsRemasteredRules.RIM_SCORING_RADIUS));
        assertFalse(HoopsRemasteredRules.crossesHoopPlane(Double.NaN,67D,0D,0D,65D,0D,0D,66D,HoopsRemasteredRules.RIM_SCORING_RADIUS));
    }
    @Test void firstTeamToReachTwentyOneWinsWithoutWinByTwo() {
        assertFalse(HoopsRemasteredRules.hasReachedTargetScore(20,19));
        assertTrue(HoopsRemasteredRules.hasReachedTargetScore(21,20));
        assertTrue(HoopsRemasteredRules.hasReachedTargetScore(20,21));
    }
    @Test void aTiedFiveMinuteScoreEntersSuddenDeath() {
        assertTrue(HoopsRemasteredRules.isTiedAtTimeLimit(10,10));
        assertFalse(HoopsRemasteredRules.isTiedAtTimeLimit(11,10));
        assertTrue(HoopsRemasteredRules.isWinningBasket(10,10,true));
    }
    @Test void offensiveReboundsHaveAShorterShotClock() {
        assertEquals(14_000L,HoopsRemasteredRules.OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS);
        assertEquals(24_000L,HoopsRemasteredRules.SHOT_CLOCK_MILLIS);
        assertTrue(HoopsRemasteredRules.OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS<HoopsRemasteredRules.SHOT_CLOCK_MILLIS);
    }
    @Test void leadingTeamReturnsNoWinnerForTies() {
        assertEquals(-1,HoopsRemasteredRules.leadingTeam(10,10));
        assertEquals(0,HoopsRemasteredRules.leadingTeam(11,10));
        assertEquals(1,HoopsRemasteredRules.leadingTeam(9,12));
    }
    @Test void clutchAndBuzzerWindowsAreInclusiveAndBounded() {
        assertTrue(HoopsRemasteredRules.isBuzzerBeater(0));
        assertTrue(HoopsRemasteredRules.isBuzzerBeater(3_000));
        assertFalse(HoopsRemasteredRules.isBuzzerBeater(3_001));
        assertTrue(HoopsRemasteredRules.isClutchTime(60_000));
        assertFalse(HoopsRemasteredRules.isClutchTime(60_001));
    }
    @Test void HopsUsesEightSecondRechargeAndCannotFireEarly() {
        assertEquals(8_000L,HoopsRemasteredRules.HOPS_COOLDOWN_MILLIS);
        assertTrue(HoopsRemasteredRules.isHopsReady(10_000L,10_000L));
        assertFalse(HoopsRemasteredRules.isHopsReady(9_999L,10_000L));
    }
    @Test void assistWindowRejectsStaleOrFuturePasses() {
        assertTrue(HoopsRemasteredRules.isAssistEligible(10_000L,2_000L));
        assertFalse(HoopsRemasteredRules.isAssistEligible(10_001L,2_000L));
        assertFalse(HoopsRemasteredRules.isAssistEligible(1_999L,2_000L));
        assertFalse(HoopsRemasteredRules.isAssistEligible(10_000L,0L));
    }
    @Test void boxScoreAwardsUseFiveCategories() {
        assertTrue(HoopsRemasteredRules.isDoubleDouble(3,3,0,0,0));
        assertFalse(HoopsRemasteredRules.isTripleDouble(3,3,0,0,0));
        assertTrue(HoopsRemasteredRules.isTripleDouble(3,3,3,0,0));
        assertTrue(HoopsRemasteredRules.isQuadrupleDouble(3,3,3,3,0));
        assertTrue(HoopsRemasteredRules.isQuintupleDouble(3,3,3,3,3));
        assertFalse(HoopsRemasteredRules.isQuintupleDouble(3,3,3,2,3));
    }
    @Test void comebackRequiresTenPointDeficit() {
        assertFalse(HoopsRemasteredRules.isComebackEligible(10,19));
        assertTrue(HoopsRemasteredRules.isComebackEligible(10,20));
    }
    @Test void spawnAssignmentAlternatesAndUsesEightDistinctLanes() {
        assertEquals(0,HoopsRemasteredRules.teamForSpawnIndex(0));
        assertEquals(1,HoopsRemasteredRules.teamForSpawnIndex(1));
        assertEquals(0,HoopsRemasteredRules.teamForSpawnIndex(2));
        assertEquals(-10D,HoopsRemasteredRules.spawnX(0));
        assertEquals(10D,HoopsRemasteredRules.spawnX(1));
        java.util.Set<Double> lanes=new java.util.HashSet<>();
        for(int i=0;i<8;i++)lanes.add(HoopsRemasteredRules.spawnZ(i));
        assertEquals(8,lanes.size());
    }
    @Test void clockRoundsUpWithoutGoingNegative() {
        assertEquals("5:00",HoopsRemasteredRules.formatClock(300_000L));
        assertEquals("0:01",HoopsRemasteredRules.formatClock(1L));
        assertEquals("0:00",HoopsRemasteredRules.formatClock(-1L));
    }
}
