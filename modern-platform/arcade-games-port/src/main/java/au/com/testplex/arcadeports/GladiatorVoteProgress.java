package au.com.testplex.arcadeports;

/** Small deterministic model shared by the Gladiators vote bar and scoreboard countdown. */
final class GladiatorVoteProgress {
    static final int SEGMENTS = 24;

    private GladiatorVoteProgress() { }

    static View at(long now, long startsAt, long endsAt) {
        long total = Math.max(1L, endsAt - startsAt);
        long remaining = Math.max(0L, endsAt - now);
        long elapsed = Math.max(0L, Math.min(total, now - startsAt));
        int filled = (int) Math.min(SEGMENTS, elapsed * SEGMENTS / total);
        long secondsLeft = (remaining + 999L) / 1_000L;
        return new View(filled, SEGMENTS - filled, secondsLeft, remaining / 100L);
    }

    record View(int greenSegments, int redSegments, long secondsLeft, long remainingTenths) { }
}
