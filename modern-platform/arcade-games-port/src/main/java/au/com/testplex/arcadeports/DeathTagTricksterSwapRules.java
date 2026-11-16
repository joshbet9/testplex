package au.com.testplex.arcadeports;

/** Cooldown rules for Runner Trickster's Chaser Swap. */
final class DeathTagTricksterSwapRules {
    static final long COOLDOWN_MILLIS = 30_000L;

    private DeathTagTricksterSwapRules() { }

    static boolean isReady(long nowMillis, long readyAtMillis) {
        return nowMillis >= readyAtMillis;
    }
}
