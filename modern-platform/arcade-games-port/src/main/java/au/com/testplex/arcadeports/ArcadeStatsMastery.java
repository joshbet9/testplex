package au.com.testplex.arcadeports;

/** Both achievement and milestone catalogues must be non-empty and fully earned. */
final class ArcadeStatsMastery {
    static boolean mastered(long earnedAchievements, long totalAchievements,
                            long earnedMilestoneTiers, long totalMilestoneTiers) {
        return totalAchievements > 0 && totalMilestoneTiers > 0
                && earnedAchievements >= totalAchievements && earnedMilestoneTiers >= totalMilestoneTiers;
    }

    private ArcadeStatsMastery() { }
}
