package au.com.testplex.arcadeports;

/** Chooses the one tournament status that is most useful across concurrent duels. */
final class GladiatorsScoreboardStatus {
    private GladiatorsScoreboardStatus() { }

    static String display(boolean voting, long voteSeconds, int activeMatches,
            long now, long nextFightAt, long nextPoisonAt) {
        if (voting) {
            long seconds = Math.max(0L, voteSeconds);
            return "Voting " + seconds + (seconds == 1L ? " second" : " seconds") + " left";
        }
        if (activeMatches == 0) return "Waiting";
        if (now >= nextPoisonAt) return "Active";
        if (now < nextFightAt) {
            return "Starting in " + Math.max(1L, (nextFightAt - now + 999L) / 1_000L) + "s";
        }
        return "Poison in " + Math.max(0L, (nextPoisonAt - now + 500L) / 1_000L) + "s";
    }
}
