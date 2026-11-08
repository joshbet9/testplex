package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/** Milk the Cow's score, comeback, near-win and undisputed-leader bookkeeping. */
final class MilkCowManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.MILK_THE_COW; }

    /** Returns true when the shared match owner must announce and finish the match. */
    boolean incrementScore(SessionState session, Player player, int change, String reason) {
        UUID id = player.getUniqueId();
        int score = MilkCowRules.scoreAfterChange(session.scores.getOrDefault(id, 0), change);
        session.scores.put(id, score);
        int scoreToWin = GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_SCORE_TO_WIN", MilkCowRules.SCORE_TO_WIN);
        if (score != scoreToWin - 1 && !session.milkSpiltMilkCandidates.contains(id)) {
            session.milkNearWinBucketEmptyings.remove(id);
        }
        int bestOpponentScore = session.scores.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(id))
                .mapToInt(Map.Entry::getValue).max().orElse(0);
        if (bestOpponentScore - score >= 5) session.milkComebackCandidates.add(id);
        UUID nextLeader = uniqueLeader(session.scores);
        if (nextLeader != null && !nextLeader.equals(session.milkCurrentLeader)) {
            session.milkTimesLed.merge(nextLeader, 1, Integer::sum);
        }
        session.milkCurrentLeader = nextLeader;
        player.sendMessage((change >= 0 ? ChatColor.GREEN : ChatColor.RED)
                + reason + ": " + score + "/" + GameDebugRuntimeOverrides.intValue(
                        "MILK_THE_COW_SCORE_TO_WIN", MilkCowRules.SCORE_TO_WIN));
        return MilkCowRules.hasWon(score);
    }

    static UUID uniqueLeader(Map<UUID, Integer> scores) {
        int currentLeaderScore = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<UUID> currentLeaders = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == currentLeaderScore)
                .map(Map.Entry::getKey).toList();
        return currentLeaders.size() == 1 ? currentLeaders.get(0) : null;
    }
}
