package au.com.testplex.arcadeports;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlexballRulesTest {
    @AfterEach
    void clearRuntimeOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test
    void sharedArcadeLifecycleAcceptsOnlySupportedPlexballPlayerCounts() {
        assertFalse(PlexballRules.hasValidPlayerCount(0));
        assertFalse(PlexballRules.hasValidPlayerCount(1));
        assertTrue(PlexballRules.hasValidPlayerCount(2));
        assertTrue(PlexballRules.hasValidPlayerCount(8));
        assertTrue(PlexballRules.hasValidPlayerCount(16));
        assertFalse(PlexballRules.hasValidPlayerCount(17));
    }

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
    void catchEvaluationNeverConsumesTicksWithoutARecordedBallSample() {
        assertEquals(49L, PlexballRules.catchEvaluationThroughTick(50L, 49L));
        assertEquals(50L, PlexballRules.catchEvaluationThroughTick(50L, 50L));
        assertEquals(50L, PlexballRules.catchEvaluationThroughTick(50L, 51L));
    }

    @Test
    void catchCheckUsesSweptBallSegmentRatherThanOnlyEndPosition() {
        Vector player = new Vector(0.0D, 0.0D, 0.0D);
        Vector from = new Vector(-2.0D, 0.0D, 0.0D);
        Vector to = new Vector(2.0D, 0.0D, 0.0D);
        assertEquals(0.0D, PlexballRules.distanceSquaredToSegment(player, from, to), 1.0E-9D);
    }

    @Test
    void debugPhysicsAndKitControlsChangeProductionRuleOutputsAndResetToSourceDefaults() {
        assertEquals(85, PlexballRules.ballMaxFlightTicks());
        assertEquals(0.24D, PlexballRules.ballRadius());
        assertEquals(1.07D, PlexballRules.battingPower(1.07D), 1.0e-9D);
        assertEquals(0.0035D, PlexballRules.curveAcceleration());
        assertEquals(0.25D, PlexballRules.curveArtistSideSpeed());
        assertEquals(0.62D, PlexballRules.sluggerDiveSpeed());
        assertEquals(0.78D, PlexballRules.sprinterBurstSpeed());
        assertEquals(0.48D, PlexballRules.skyguardLeapSpeed());

        GameDebugRuntimeOverrides.activate(Map.of(
                "PLEXBALL_BALL_MAX_FLIGHT_TICKS", 120,
                "PLEXBALL_BALL_RADIUS", 0.4D,
                "PLEXBALL_BATTING_POWER_MULTIPLIER", 1.5D,
                "PLEXBALL_CURVE_ACCELERATION", 0.008D,
                "PLEXBALL_CURVE_ARTIST_SIDE_SPEED", 0.5D,
                "PLEXBALL_SLUGGER_DIVE_SPEED", 1.0D,
                "PLEXBALL_SPRINTER_BURST_SPEED", 1.2D,
                "PLEXBALL_SKYGUARD_LEAP_SPEED", 0.8D), UUID.randomUUID());

        assertEquals(120, PlexballRules.ballMaxFlightTicks());
        assertEquals(0.4D, PlexballRules.ballRadius());
        assertEquals(1.605D, PlexballRules.battingPower(1.07D), 1.0e-9D);
        assertEquals(0.008D, PlexballRules.curveAcceleration());
        assertEquals(0.5D, PlexballRules.curveArtistSideSpeed());
        assertEquals(1.0D, PlexballRules.sluggerDiveSpeed());
        assertEquals(1.2D, PlexballRules.sprinterBurstSpeed());
        assertEquals(0.8D, PlexballRules.skyguardLeapSpeed());

        GameDebugRuntimeOverrides.clear();
        assertEquals(85, PlexballRules.ballMaxFlightTicks());
        assertEquals(0.24D, PlexballRules.ballRadius());
        assertEquals(1.07D, PlexballRules.battingPower(1.07D), 1.0e-9D);
        assertEquals(0.0035D, PlexballRules.curveAcceleration());
    }
}
