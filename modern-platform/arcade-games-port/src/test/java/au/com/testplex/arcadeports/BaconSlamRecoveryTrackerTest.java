package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaconSlamRecoveryTrackerTest {
    @Test
    void attackerEarnsGotEmWhenTheyKnockAnAirborneRecoveryOffBeforeLanding() {
        BaconSlamRecoveryTracker tracker = new BaconSlamRecoveryTracker();
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, attacker, true);
        assertEquals(attacker, tracker.knockout(victim, true, true));
        assertNull(tracker.knockout(victim, true, true));
    }

    @Test
    void touchingGroundEvenMomentarilyCancelsTheAttempt() {
        BaconSlamRecoveryTracker tracker = new BaconSlamRecoveryTracker();
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, attacker, true);
        tracker.landed(victim);
        assertNull(tracker.knockout(victim, true, true));
    }

    @Test
    void hitAfterLandingAndOrdinaryGroundedBodySlamDoNotCount() {
        BaconSlamRecoveryTracker tracker = new BaconSlamRecoveryTracker();
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        tracker.bodySlam(victim, false);
        tracker.successfulHit(victim, attacker, true);
        assertNull(tracker.knockout(victim, true, true));
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, attacker, false);
        assertNull(tracker.knockout(victim, true, true));
    }

    @Test
    void recoveryMustBeInterruptedByAnOpponentAndEndOffArena() {
        BaconSlamRecoveryTracker tracker = new BaconSlamRecoveryTracker();
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        tracker.bodySlam(victim, true);
        assertNull(tracker.knockout(victim, true, true));
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, victim, true);
        assertNull(tracker.knockout(victim, true, true));
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, attacker, true);
        assertNull(tracker.knockout(victim, false, true));
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, attacker, true);
        assertNull(tracker.knockout(victim, true, false));
    }

    @Test
    void theLastOpponentToHitTheRecoveryGetsCredit() {
        BaconSlamRecoveryTracker tracker = new BaconSlamRecoveryTracker();
        UUID victim = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        tracker.bodySlam(victim, true);
        tracker.successfulHit(victim, first, true);
        tracker.successfulHit(victim, last, true);
        assertEquals(last, tracker.knockout(victim, true, true));
    }
}
