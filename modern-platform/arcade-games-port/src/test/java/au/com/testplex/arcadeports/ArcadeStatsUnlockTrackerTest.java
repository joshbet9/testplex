package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArcadeStatsUnlockTrackerTest {
    private static final ArcadeStatsUnlockTracker.Unlock ACHIEVEMENT =
            new ArcadeStatsUnlockTracker.Unlock("a:bacon-brawl:goat-sty", "Bacon Brawl", "GOAT Sty", 0, "");
    private static final ArcadeStatsUnlockTracker.Unlock TIER_ONE =
            new ArcadeStatsUnlockTracker.Unlock("m:bacon-brawl:kills:1", "Bacon Brawl", "Kills", 1, "10 kills");
    private static final ArcadeStatsUnlockTracker.Unlock TIER_TWO =
            new ArcadeStatsUnlockTracker.Unlock("m:bacon-brawl:kills:2", "Bacon Brawl", "Kills", 2, "50 kills");

    @Test
    void savedUnlocksNeverReplayOnJoinOrOnRepeatedPolls() {
        ArcadeStatsUnlockTracker tracker = new ArcadeStatsUnlockTracker();
        UUID playerId = UUID.randomUUID();
        Map<String, ArcadeStatsUnlockTracker.Unlock> existing = Map.of(ACHIEVEMENT.key(), ACHIEVEMENT);
        assertTrue(tracker.observe(playerId, existing).isEmpty());
        assertTrue(tracker.observe(playerId, existing).isEmpty());
        tracker.forget(playerId);
        assertTrue(tracker.observe(playerId, existing).isEmpty());
    }

    @Test
    void midgameAchievementAndEachCrossedTierAnnounceExactlyOnce() {
        ArcadeStatsUnlockTracker tracker = new ArcadeStatsUnlockTracker();
        UUID playerId = UUID.randomUUID();
        assertTrue(tracker.observe(playerId, Map.of()).isEmpty());
        Map<String, ArcadeStatsUnlockTracker.Unlock> progress = new LinkedHashMap<>();
        progress.put(ACHIEVEMENT.key(), ACHIEVEMENT);
        progress.put(TIER_ONE.key(), TIER_ONE);
        progress.put(TIER_TWO.key(), TIER_TWO);
        assertEquals(java.util.List.of(ACHIEVEMENT, TIER_ONE, TIER_TWO), tracker.observe(playerId, progress));
        assertTrue(tracker.observe(playerId, progress).isEmpty());
        assertTrue(tracker.observe(playerId, Map.of()).isEmpty());
        assertTrue(tracker.observe(playerId, progress).isEmpty());
    }

    @Test
    void playersAreIndependent() {
        ArcadeStatsUnlockTracker tracker = new ArcadeStatsUnlockTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.observe(first, Map.of());
        tracker.observe(second, Map.of(ACHIEVEMENT.key(), ACHIEVEMENT));
        assertEquals(java.util.List.of(ACHIEVEMENT), tracker.observe(first, Map.of(ACHIEVEMENT.key(), ACHIEVEMENT)));
        assertTrue(tracker.observe(second, Map.of(ACHIEVEMENT.key(), ACHIEVEMENT)).isEmpty());
    }
}
