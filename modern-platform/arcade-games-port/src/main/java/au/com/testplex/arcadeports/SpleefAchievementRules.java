package au.com.testplex.arcadeports;

import java.util.List;

/** Pure requirement checks shared by Spleef achievement detectors and tests. */
public final class SpleefAchievementRules {
    private SpleefAchievementRules() { }

    public static final int KING_OR_QUEEN_WINS = 15;
    public static final int DEMOLITIONIST_BLOCKS = 20_000;

    /** Each boolean represents one successive elimination, newest last. */
    public static boolean finalEliminationsMatch(List<Boolean> qualifyingEliminations, int required) {
        if (qualifyingEliminations == null || required <= 0
                || qualifyingEliminations.size() < required) return false;
        int start = qualifyingEliminations.size() - required;
        for (int index = start; index < qualifyingEliminations.size(); index++) {
            if (!Boolean.TRUE.equals(qualifyingEliminations.get(index))) return false;
        }
        return true;
    }

    public static boolean brawlersGambit(String kit, boolean won, int sameSmashBlocks,
            boolean finalOpponentFellFromSameSmash) {
        return "brawler".equals(kit) && won && sameSmashBlocks >= 5
                && finalOpponentFellFromSameSmash;
    }

    public static boolean thinIce(boolean won, int remainingBreakableFloorBlocks) {
        return won && remainingBreakableFloorBlocks >= 0 && remainingBreakableFloorBlocks <= 5;
    }
}
