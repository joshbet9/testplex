package au.com.testplex.arcadeports;

/** Chaser Swap is reusable only after a successful grounded swap. */
final class DeathTagTricksterSwapRules {
    static final long COOLDOWN_MILLIS = 30_000L;
    private DeathTagTricksterSwapRules() { }
    static boolean isReady(long nowMillis, long readyAtMillis) { return nowMillis >= readyAtMillis; }
}
