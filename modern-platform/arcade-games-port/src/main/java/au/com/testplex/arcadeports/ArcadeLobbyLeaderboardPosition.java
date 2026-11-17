package au.com.testplex.arcadeports;

/** Per-game vertical placement for the viewer-local lobby leaderboard panels. */
final class ArcadeLobbyLeaderboardPosition {
    private static final double DEFAULT_FIRST_ROW_Y = 106.8D;
    private static final double DEFAULT_HITBOX_Y = 104.2D;
    private static final double DRAGON_ESCAPE_FIRST_ROW_Y = 104.9D;
    private static final double DRAGON_ESCAPE_HITBOX_Y = 102.7D;
    private static final double ROW_SPACING = 0.32D;

    private ArcadeLobbyLeaderboardPosition() { }

    static double rowY(String game, int line) {
        return firstRowY(game) - Math.max(0, line) * ROW_SPACING;
    }

    static double hitboxY(String game) {
        return "dragon-escape".equals(game) ? DRAGON_ESCAPE_HITBOX_Y : DEFAULT_HITBOX_Y;
    }

    private static double firstRowY(String game) {
        return "dragon-escape".equals(game) ? DRAGON_ESCAPE_FIRST_ROW_Y : DEFAULT_FIRST_ROW_Y;
    }
}
