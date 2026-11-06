package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaconBrawlAchievementTrackerTest {
    @Test
    void ironHogRequiresEightRealBlockedHitsAndRejectsSpam() {
        BaconBrawlAchievementTracker tracker = new BaconBrawlAchievementTracker();
        UUID brutus = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        assertEquals(1, tracker.recordBlockedHit(brutus, attacker, 1_000L));
        assertEquals(1, tracker.recordBlockedHit(brutus, attacker, 1_100L));
        assertEquals(1, tracker.recordBlockedHit(brutus, attacker, 1_400L));
        for (int hit = 2; hit <= 8; hit++) {
            assertEquals(hit, tracker.recordBlockedHit(brutus, attacker, 1_000L + (hit - 1) * 401L));
        }
        assertEquals(8, BaconBrawlAchievementTracker.IRON_HOG_BLOCKS);
    }

    @Test
    void extraCrispyRequiresFiveDistinctOpponentsNotRepeatIgnitions() {
        BaconBrawlAchievementTracker tracker = new BaconBrawlAchievementTracker();
        UUID chris = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            UUID target = UUID.randomUUID();
            assertEquals(i, tracker.recordCrispyIgnition(chris, target));
            assertEquals(0, tracker.recordCrispyIgnition(chris, target));
        }
        assertEquals(5, BaconBrawlAchievementTracker.EXTRA_CRISPY_UNIQUE_IGNITIONS);
    }

    @Test
    void quadrupleServingResetsTenSecondDeadlineAfterEveryKnockoff() {
        BaconBrawlAchievementTracker tracker = new BaconBrawlAchievementTracker();
        UUID attacker = UUID.randomUUID();
        assertEquals(1, tracker.recordElimination(attacker, true, 10_000L));
        assertEquals(2, tracker.recordElimination(attacker, true, 19_000L));
        assertEquals(3, tracker.recordElimination(attacker, true, 28_000L));
        assertEquals(4, tracker.recordElimination(attacker, true, 37_000L));
        // The first and fourth kills are 27 seconds apart, still a valid four-kill chain.
        assertEquals(5, tracker.recordElimination(attacker, true, 47_000L));
        assertEquals(1, tracker.recordElimination(attacker, true, 57_001L));
        assertEquals(0, tracker.recordElimination(attacker, false, 58_000L));
        assertEquals(1, tracker.recordElimination(attacker, true, 58_500L));
    }

    @Test
    void varietyPackRequiresWinsAcrossAllFiveKits() {
        Set<String> wins = new HashSet<>(BaconBrawlAchievementTracker.ALL_KITS);
        assertEquals(5, wins.size());
        assertTrue(BaconBrawlAchievementTracker.hasWonEveryKit(wins));
        wins.remove("chris-p-bacon");
        assertFalse(BaconBrawlAchievementTracker.hasWonEveryKit(wins));
        assertFalse(BaconBrawlAchievementTracker.hasWonEveryKit(null));
    }
}
