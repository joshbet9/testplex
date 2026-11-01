package au.com.testplex.arcadeports;

import java.util.Set;
import org.bukkit.Material;

/** Deterministic threshold helpers for achievements used by the modern Arcade ports. */
public final class ArcadeProgressRules {
    private ArcadeProgressRules() { }

    public static boolean isDrawMyThingPixelLinguist(int perfectRounds, int expectedRounds, boolean failedAnyRound) {
        return expectedRounds > 0 && !failedAnyRound && perfectRounds >= expectedRounds;
    }

    public static boolean shouldAwardDrawMyThingTripleThreat(int consecutiveFirstGuesses) {
        return consecutiveFirstGuesses >= 3;
    }

    public static boolean shouldAwardDrawMyThingFastGuesser(long elapsedMillis) {
        return elapsedMillis >= 0L && elapsedMillis <= 5_000L;
    }

    public static boolean isMasterBuildersPurist(Set<Material> placedTypes) {
        return placedTypes != null && placedTypes.size() == 1;
    }

    public static boolean shouldAwardMasterBuildersArchitect(int winnerScore, int runnerUpScore) {
        return runnerUpScore != Integer.MIN_VALUE && winnerScore - runnerUpScore >= 20;
    }

    public static int masterBuildersScoreIfVoteChangedToOne(int voteSum, int voteCount, int originalVote) {
        if (voteCount <= 0 || originalVote < 1 || originalVote > 6 || originalVote == 1) return Integer.MIN_VALUE;
        return (int) Math.round((voteSum - originalVote + 1.0D) / voteCount * 100.0D);
    }

    public static boolean shouldAwardMasterBuildersGreaterGood(int ownScore, int hypotheticalWinnerScore,
            int highestOtherScore) {
        return ownScore > hypotheticalWinnerScore && ownScore > highestOtherScore;
    }

    public static boolean shouldAwardMilkCowPerfectGame(int farmerDeaths) {
        return farmerDeaths == 0;
    }

    public static boolean shouldAwardMilkCowHighClub(boolean drankWhileAirborne, boolean drankWhileGrounded) {
        return drankWhileAirborne && !drankWhileGrounded;
    }

    public static boolean shouldAwardMilkCowStopStopHeIsAlreadyDead(int opponentLeadCount) {
        return opponentLeadCount >= 3;
    }

    public static boolean shouldAwardMilkCowGotTheChops(int airborneKills) {
        return airborneKills >= 10;
    }

    public static boolean shouldAwardMilkCowSlaughterhouse(int assists) {
        return assists >= 15;
    }


    public static boolean isRecentWitherHumanActivity(long nowMillis, long lastActivityAtMillis, long windowMillis) {
        return windowMillis >= 0L && lastActivityAtMillis > 0L
                && nowMillis >= lastActivityAtMillis && nowMillis - lastActivityAtMillis <= windowMillis;
    }

    public static boolean shouldAwardWitherSavior(int distinctHumansRevived) {
        return distinctHumansRevived >= 5;
    }

    public static boolean shouldAwardWitherTormentor(int humanKillsFromOneSkull) {
        return humanKillsFromOneSkull >= 3;
    }

    public static boolean shouldAwardWitherWorstWitherEver(int killsOfSameHuman, boolean humanRecentlyActive) {
        return humanRecentlyActive && killsOfSameHuman >= 5;
    }

    public static boolean shouldAwardWitherLucidDreams(boolean humanTeamWon, boolean tookDamageThisRound) {
        return humanTeamWon && !tookDamageThisRound;
    }

}
