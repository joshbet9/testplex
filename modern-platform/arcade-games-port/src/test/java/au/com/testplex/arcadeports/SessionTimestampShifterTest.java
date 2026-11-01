package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionTimestampShifterTest {
    @Test
    void shiftsAbsoluteDeadlinesCooldownsAndNestedArenaTimers() {
        Fixture fixture = new Fixture();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        fixture.cooldownUntil.put(player, 300L);
        fixture.pairedArenaStartsAtByPair.put(2, new HashMap<>(Map.of(player, 400L)));
        fixture.infiltration.put(player, new TimerCarrier(600L, 700L));
        fixture.activeFlareZones.add(new Expiry("flare", 800L));

        List<String> failures = SessionTimestampShifter.shift(fixture, 10_000L);

        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(10_250L, fixture.spawnAt);
        assertEquals(10_300L, fixture.cooldownUntil.get(player));
        assertEquals(10_400L, fixture.pairedArenaStartsAtByPair.get(2).get(player));
        assertEquals(10_600L, fixture.infiltration.get(player).lastSecondAt);
        assertEquals(10_700L, fixture.infiltration.get(player).lastSlowAt);
        assertEquals(10_800L, fixture.activeFlareZones.getFirst().expiresAtMillis());
    }

    @Test
    void shiftsTimersOnTopLevelRuntimeObjectsAndTheirNestedState() {
        TopLevelTimerFixture fixture = new TopLevelTimerFixture();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000002");
        fixture.nextAbilityAt.put(player, 500L);
        fixture.playerStates.put(player, new TopLevelTimerFixture.PlayerTimer(700L));

        List<String> failures = SessionTimestampShifter.shift(fixture, 2_000L);

        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(2_050L, fixture.nextSpawnAt);
        assertEquals(2_500L, fixture.nextAbilityAt.get(player));
        assertEquals(2_700L, fixture.playerStates.get(player).nextUpdateAt);
    }

    @Test
    void preservesLifecycleAnchorsAndNonPositiveSentinels() {
        Fixture fixture = new Fixture();

        SessionTimestampShifter.shift(fixture, 10_000L);

        assertEquals(100L, fixture.startedAt);
        assertEquals(500L, fixture.endsAt);
        assertEquals(200L, fixture.prepareEndsAt);
        assertEquals(0L, fixture.noDeadlineAt);
        assertEquals(-1L, fixture.disabledUntil);
    }

    @Test
    void noOpsForNullTargetsOrNonPositivePauseDurations() {
        Fixture fixture = new Fixture();

        assertTrue(SessionTimestampShifter.shift(null, 10_000L).isEmpty());
        assertTrue(SessionTimestampShifter.shift(fixture, 0L).isEmpty());
        assertTrue(SessionTimestampShifter.shift(fixture, -1L).isEmpty());
        assertEquals(250L, fixture.spawnAt);
    }

    private static final class Fixture {
        long startedAt = 100L;
        long endsAt = 500L;
        long prepareEndsAt = 200L;
        long spawnAt = 250L;
        long noDeadlineAt;
        long disabledUntil = -1L;
        final Map<UUID, Long> cooldownUntil = new HashMap<>();
        final Map<Integer, Map<UUID, Long>> pairedArenaStartsAtByPair = new HashMap<>();
        final Map<UUID, TimerCarrier> infiltration = new HashMap<>();
        final List<Expiry> activeFlareZones = new java.util.ArrayList<>();
    }

    private static final class TimerCarrier {
        long lastSecondAt;
        long lastSlowAt;

        TimerCarrier(long lastSecondAt, long lastSlowAt) {
            this.lastSecondAt = lastSecondAt;
            this.lastSlowAt = lastSlowAt;
        }
    }

    private record Expiry(String id, long expiresAtMillis) {
    }
}


final class TopLevelTimerFixture {
    long nextSpawnAt = 50L;
    final Map<UUID, Long> nextAbilityAt = new HashMap<>();
    final Map<UUID, PlayerTimer> playerStates = new HashMap<>();

    static final class PlayerTimer {
        long nextUpdateAt;

        private PlayerTimer(long nextUpdateAt) {
            this.nextUpdateAt = nextUpdateAt;
        }
    }
}
