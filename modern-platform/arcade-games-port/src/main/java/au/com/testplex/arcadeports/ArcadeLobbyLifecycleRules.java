package au.com.testplex.arcadeports;

final class ArcadeLobbyLifecycleRules {
    static final int MINIMUM_TEST_PLAYERS = 1;
    static final int DEFAULT_PREPARE_SECONDS = 10;
    static final int DEFAULT_END_PRESENTATION_SECONDS = 10;
    private ArcadeLobbyLifecycleRules() { }
    static int recruitCountdownSeconds(int players, int minimumPlayers, int capacity) {
        if (players < Math.max(1, minimumPlayers)) return -1;
        int safeCapacity = Math.max(1, capacity);
        if (players >= safeCapacity) return 10;
        if (players >= Math.ceil(safeCapacity * 0.75D)) return 30;
        return 60;
    }
    /** Gameplay input is not accepted until the session's Prepare phase has ended. */
    static boolean gameplayInputAllowed(boolean liveStarted) { return liveStarted; }

    static int prepareSeconds(int seconds) { return Math.max(3, Math.min(30, seconds)); }
    static int endPresentationSeconds(int seconds) { return Math.max(0, Math.min(30, seconds)); }
    static String countdownStatus(int seconds) { return "Starting in " + Math.max(0, seconds) + " Seconds"; }
}