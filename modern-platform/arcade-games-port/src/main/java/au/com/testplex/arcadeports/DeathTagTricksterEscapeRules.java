package au.com.testplex.arcadeports;

/** High-ping-safe Trickster water clutch and bounded temporary web rules. */
final class DeathTagTricksterEscapeRules {
    static final long WATER_ARM_WINDOW_MILLIS = 5_000L;
    static final long WATER_COOLDOWN_MILLIS = 30_000L;
    static final long WEB_COOLDOWN_MILLIS = 25_000L;
    static final long WEB_DURATION_TICKS = 60L;

    private DeathTagTricksterEscapeRules() { }

    static boolean shouldClutch(long now, long readyAt, long armedUntil, boolean holdingBucket) {
        return now >= readyAt && (holdingBucket || now < armedUntil);
    }

    static boolean canPlaceWeb(boolean air, boolean solidSupport) {
        return air && solidSupport;
    }
}
