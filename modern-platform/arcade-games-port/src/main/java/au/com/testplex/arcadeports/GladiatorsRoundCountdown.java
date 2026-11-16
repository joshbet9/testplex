package au.com.testplex.arcadeports;

/** Small deterministic helpers for source-style Gladiators duel countdowns. */
final class GladiatorsRoundCountdown {
    private GladiatorsRoundCountdown() { }

    static int secondsRemaining(long startsAtMillis, long nowMillis) {
        if (startsAtMillis <= nowMillis) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (startsAtMillis - nowMillis + 999L) / 1_000L);
    }
}
