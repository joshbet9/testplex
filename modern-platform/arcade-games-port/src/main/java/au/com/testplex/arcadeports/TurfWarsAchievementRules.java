package au.com.testplex.arcadeports;

/** Pure requirement checks shared by Turf Wars achievement detectors and tests. */
public final class TurfWarsAchievementRules {
    private TurfWarsAchievementRules() { }

    public static final int TURF_MASTER_WINS = 50;
    public static final int SHREDDINATOR_BLOCKS = 2_000;
    public static final int DEAD_CENTRE_BOW_KILLS = 20;
    public static final int KEEP_YOUR_FORT_BLOCKS = 30;

    public static boolean deadCentre(String kit, boolean won, int creditedBowEliminations) {
        return "marksman".equals(kit) && won && creditedBowEliminations >= DEAD_CENTRE_BOW_KILLS;
    }

    public static boolean keepYourFort(String kit, boolean won, int intactPlacedBlocksOnOwnTurf) {
        return "fortifier".equals(kit) && won
                && intactPlacedBlocksOnOwnTurf >= KEEP_YOUR_FORT_BLOCKS;
    }

    public static boolean behindEnemyLines(boolean oneLife, int consecutiveSeconds) {
        return oneLife && consecutiveSeconds >= 15;
    }

    public static boolean comeback(boolean won, boolean teamPreviouslyReachedFiveLinesOrFewer) {
        return won && teamPreviouslyReachedFiveLinesOrFewer;
    }
}
