package au.com.testplex.arcadeports;

/** Shared source constants and score/role calculations for Milk the Cow. */
public final class MilkCowRules {
    public static final int SCORE_TO_WIN = 15;
    public static final int SCORE_LOST_ON_DEATH = 2;
    public static final int SCORE_PER_MILK = 1;
    public static final double COW_TEAM_RATIO = 0.20D;
    public static final int HERD_SIZE = 5;
    public static final double HERD_MOVE_SPEED = 1.4D;

    private MilkCowRules() { }

    /** Keep at least one player on each side when the lobby has two or more players. */
    public static int cowCountForPlayers(int playerCount) {
        if (playerCount <= 1) return 0;
        return Math.max(1, Math.min(playerCount - 1,
                (int) Math.round(playerCount * COW_TEAM_RATIO)));
    }

    public static int scoreAfterChange(int currentScore, int change) {
        return Math.max(0, currentScore + change);
    }

    public static boolean hasWon(int score) {
        return score >= SCORE_TO_WIN;
    }
}
