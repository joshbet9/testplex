package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlexballRulesTest {
    @Test
    void spectatorsCanOnlyJoinActiveMatchesOutsideThePlayerRoster() {
        assertTrue(PlexballRules.canJoinAsSpectator(true, false, 0));
        assertTrue(PlexballRules.canJoinAsSpectator(true, false, PlexballRules.MAX_SPECTATORS - 1));
        assertFalse(PlexballRules.canJoinAsSpectator(false, false, 0));
        assertFalse(PlexballRules.canJoinAsSpectator(true, true, 0));
        assertFalse(PlexballRules.canJoinAsSpectator(true, false, PlexballRules.MAX_SPECTATORS));
        assertFalse(PlexballRules.canJoinAsSpectator(true, false, -1));
    }

    @Test
    void paperPingEstimateProvidesOnlyBoundedCompensation() {
        assertEquals(0, PlexballRules.latencyRewindTicks(-1));
        assertEquals(0, PlexballRules.latencyRewindTicks(0));
        assertEquals(0, PlexballRules.latencyRewindTicks(49));
        assertEquals(1, PlexballRules.latencyRewindTicks(50));
        assertEquals(1, PlexballRules.latencyRewindTicks(149));
        assertEquals(2, PlexballRules.latencyRewindTicks(150));
        assertEquals(2, PlexballRules.latencyRewindTicks(200));
        assertEquals(4, PlexballRules.latencyRewindTicks(400));
        assertEquals(4, PlexballRules.latencyRewindTicks(449));
        assertEquals(PlexballRules.MAX_REWIND_TICKS, PlexballRules.latencyRewindTicks(450));
        assertEquals(PlexballRules.MAX_REWIND_TICKS, PlexballRules.latencyRewindTicks(1200));
    }

    @Test
    void timingRewardsTheCentreAndGradesNearMisses() {
        assertEquals(1.0D, PlexballRules.timingMultiplier(0));
        assertEquals(1.0D, PlexballRules.timingMultiplier(2));
        assertTrue(PlexballRules.timingMultiplier(4) > PlexballRules.timingMultiplier(8));
        assertTrue(PlexballRules.timingMultiplier(8) > PlexballRules.timingMultiplier(14));
    }

    @Test
    void timingBeatCanBeRandomlyPhasedPerAtBat() {
        assertEquals(0, PlexballRules.timingDistanceTicks(34, 10, 34, 7));
        assertEquals(1, PlexballRules.timingDistanceTicks(33, 10, 34, 7));
        assertEquals(1, PlexballRules.timingDistanceTicks(35, 10, 34, 7));
    }

    @Test
    void fairConeIsCentredDownTheTriangle() {
        assertTrue(PlexballRules.isFairYaw(180.0D));
        assertTrue(PlexballRules.isFairYaw(150.0D));
        assertTrue(PlexballRules.isFairYaw(210.0D));
        assertFalse(PlexballRules.isFairYaw(140.0D));
        assertFalse(PlexballRules.isFairYaw(220.0D));
    }

    @Test
    void triangularFieldNarrowsAtHomePlateAndWidensToOneHundredMetres() {
        assertTrue(PlexballRules.isInsideField(0.0D, 0.0D));
        assertTrue(PlexballRules.isInsideField(64.0D, -100.0D));
        assertFalse(PlexballRules.isInsideField(68.0D, -100.0D));
        assertFalse(PlexballRules.isInsideField(0.0D, -101.0D));
    }

    @Test
    void suddenDeathTimeoutDoesNotTreatAnIncompleteRoundAsComplete() {
        Set<String> finalists = Set.of("alpha", "bravo", "charlie");
        Set<String> completedTurns = Set.of("alpha", "bravo");
        assertFalse(PlexballRules.hasCompletedRound(finalists, completedTurns));
        assertFalse(PlexballRules.hasCompletedRound(finalists, Set.of()));
        assertTrue(PlexballRules.hasCompletedRound(finalists, finalists));
        assertTrue(PlexballRules.hasCompletedRound(Set.of("alpha"), Set.of("alpha")));
        assertFalse(PlexballRules.hasCompletedRound(Set.of(), Set.of()));
    }

    @Test
    void catchCheckUsesSweptBallSegmentRatherThanOnlyEndPosition() {
        Vector player = new Vector(0.0D, 0.0D, 0.0D);
        Vector from = new Vector(-2.0D, 0.0D, 0.0D);
        Vector to = new Vector(2.0D, 0.0D, 0.0D);
        assertEquals(0.0D, PlexballRules.distanceSquaredToSegment(player, from, to), 1.0E-9D);
    }
}
