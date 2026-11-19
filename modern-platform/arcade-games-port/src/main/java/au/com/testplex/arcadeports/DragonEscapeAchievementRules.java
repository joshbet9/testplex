package au.com.testplex.arcadeports;

/** Match-specific predicates for Dragon Escape achievements. */
final class DragonEscapeAchievementRules {
    static final long PHOTO_FINISH_MAX_GAP_MILLIS = 250L;

    static boolean photoFinish(long firstMillis, long secondMillis) {
        return firstMillis >= 0L && secondMillis >= firstMillis
                && secondMillis - firstMillis <= PHOTO_FINISH_MAX_GAP_MILLIS;
    }

    private DragonEscapeAchievementRules() { }
}
