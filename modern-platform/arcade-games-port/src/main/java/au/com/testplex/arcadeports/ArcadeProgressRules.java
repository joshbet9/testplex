package au.com.testplex.arcadeports;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
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

    public static boolean shouldRecordDrawMyThingKeenEye(boolean debugSession, boolean failedAnyRound) {
        return !debugSession && !failedAnyRound;
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

    public static long masterBuildersBuildMillis(long totalMillis, int judgedBuildCount, long reviewMillis) {
        long reviewBudget = judgedBuildCount <= 1 ? 0L : judgedBuildCount * Math.max(0L, reviewMillis);
        return Math.max(30_000L, Math.max(0L, totalMillis) - reviewBudget);
    }

    public static boolean shouldAwardMasterBuildersGreaterGood(int ownScore, int hypotheticalWinnerScore,
            int highestOtherScore) {
        return ownScore > hypotheticalWinnerScore && ownScore > highestOtherScore;
    }

    public static boolean shouldAwardMasterBuildersBlockParty(Set<Material> placedTypes) {
        return placedTypes != null && placedTypes.size() >= 12;
    }

    public static boolean shouldAwardMasterBuildersMinimalist(int remainingPlacedBlocks) {
        return remainingPlacedBlocks >= 0 && remainingPlacedBlocks <= 32;
    }

    public static boolean shouldAwardMasterBuildersSecondDraft(int ownBlocksBroken) {
        return ownBlocksBroken >= 30;
    }

    public static boolean shouldAwardMasterBuildersTouchingTheSky(boolean placedAtHighestBuildY) {
        return placedAtHighestBuildY;
    }

    public static boolean shouldAwardMasterBuildersCriticsDarling(double averageRating) {
        return Double.isFinite(averageRating) && averageRating >= 5.0D;
    }

    public static boolean shouldAwardMasterBuildersUnanimousOvation(Map<UUID, Integer> votes,
            Set<UUID> eligibleVoters) {
        return votes != null && eligibleVoters != null && !eligibleVoters.isEmpty()
                && eligibleVoters.stream().allMatch(voter -> votes.getOrDefault(voter, 0) == 6);
    }

    public static boolean shouldAwardMasterBuildersMixedReviews(Map<UUID, Integer> votes) {
        return votes != null && votes.containsValue(1) && votes.containsValue(6);
    }

    public static boolean shouldAwardMasterBuildersKingmaker(int actualWinnerScore,
            int hypotheticalWinnerScore, int highestOtherScore, int ratingForWinner) {
        return ratingForWinner == 6 && actualWinnerScore > highestOtherScore
                && hypotheticalWinnerScore <= highestOtherScore;
    }

    public static boolean shouldAwardMasterBuildersFairCritic(UUID voterId, Set<UUID> ratedTargets,
            Map<UUID, Map<UUID, Integer>> votesByTarget, Map<UUID, Double> finalAverages) {
        if (voterId == null || ratedTargets == null || ratedTargets.isEmpty()) return false;
        if (ratedTargets.stream().filter(target -> !voterId.equals(target)).count() == 0L) return false;
        for (UUID target : ratedTargets) {
            if (target == null || target.equals(voterId)) continue;
            Integer vote = votesByTarget.getOrDefault(target, Map.of()).get(voterId);
            Double average = finalAverages.get(target);
            if (vote == null || average == null || !Double.isFinite(average)
                    || Math.abs(vote - average) > 1.0D) return false;
        }
        return true;
    }

    /** Checks every pair whose rounded final score establishes a strict standings order. */
    public static boolean shouldAwardMasterBuildersGoldenEye(UUID voterId, Set<UUID> ratedTargets,
            Map<UUID, Map<UUID, Integer>> votesByTarget, Map<UUID, Integer> roundedFinalScores) {
        if (voterId == null || ratedTargets == null || ratedTargets.size() < 2) return false;
        for (UUID target : ratedTargets) {
            if (target == null || target.equals(voterId)
                    || votesByTarget.getOrDefault(target, Map.of()).get(voterId) == null
                    || roundedFinalScores.get(target) == null) return false;
        }
        for (UUID first : ratedTargets) {
            if (first.equals(voterId)) continue;
            for (UUID second : ratedTargets) {
                if (second.equals(voterId) || first.equals(second)) continue;
                int finalOrder = Integer.compare(roundedFinalScores.get(first), roundedFinalScores.get(second));
                if (finalOrder == 0) continue;
                int voteOrder = Integer.compare(votesByTarget.get(first).get(voterId),
                        votesByTarget.get(second).get(voterId));
                if (Integer.signum(finalOrder) != Integer.signum(voteOrder)) return false;
            }
        }
        return true;
    }

    /** Source Build grants floor(sum of ratings / 3) gems with the "Voting Fairly" label. */
    public static boolean shouldRecordMasterBuildersFairVote(int ratingTotal) {
        return ratingTotal >= 3;
    }

    public static boolean shouldAwardMilkCowPerfectGame(int farmerDeaths) {
        return farmerDeaths <= MilkCowRules.perfectGameMaxFarmerDeaths();
    }

    public static boolean shouldAwardMilkCowHighClub(boolean drankWhileAirborne, boolean drankWhileGrounded) {
        return drankWhileAirborne && !drankWhileGrounded;
    }

    public static boolean shouldAwardMilkCowStopStopHeIsAlreadyDead(int opponentLeadCount) {
        return opponentLeadCount >= MilkCowRules.comebackLeadThreshold();
    }

    public static boolean shouldAwardMilkCowGotTheChops(int airborneKills) {
        return airborneKills >= MilkCowRules.gotTheChopsAirborneKills();
    }

    public static boolean shouldAwardMilkCowSlaughterhouse(int assists) {
        return assists >= MilkCowRules.slaughterhouseAssists();
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
