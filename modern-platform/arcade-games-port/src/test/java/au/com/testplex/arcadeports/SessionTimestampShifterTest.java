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

        List<String> failures = SessionTimestampShifter.shift(fixture, 10_000L);

        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals(10_250L, fixture.spawnAt);
        assertEquals(10_300L, fixture.cooldownUntil.get(player));
        assertEquals(10_400L, fixture.pairedArenaStartsAtByPair.get(2).get(player));
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
    }
}
