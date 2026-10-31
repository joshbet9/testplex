package au.com.testplex.arcadeports;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The shared preference-weight calculation for the Mixed Arcade rotation.
 *
 * Probabilities are normalised from each game's existing base selection weight
 * after applying the lobby's Like/Dislike preference multiplier.
 */
public final class GamePreferenceWeights {
    public static final double MAX_PREFERENCE_ADJUSTMENT = 0.95D;

    private GamePreferenceWeights() {
    }

    public record Counts(int likes, int dislikes) {
    }

    /**
     * Quintic smoothstep curve used by the original Mixed Arcade proposal.
     * Inputs are clamped so malformed counts cannot produce out-of-range weights.
     */
    public static double preferenceCurve(double proportion) {
        double x = Math.max(0.0D, Math.min(1.0D, proportion));
        double x2 = x * x;
        double x3 = x2 * x;
        return 6.0D * x3 * x2 - 15.0D * x2 * x2 + 10.0D * x3;
    }

    /**
     * Returns 0.05 for unanimous dislike, 1.95 for unanimous Like and 1.0
     * when everyone is Neutral or Like and Dislike counts are equal.
     */
    public static double preferenceMultiplier(int likes, int dislikes, int eligiblePlayers) {
        if (eligiblePlayers <= 0) return 1.0D;

        int safeLikes = Math.max(0, Math.min(eligiblePlayers, likes));
        int safeDislikes = Math.max(0, Math.min(eligiblePlayers, dislikes));
        double likedShare = (double) safeLikes / eligiblePlayers;
        double dislikedShare = (double) safeDislikes / eligiblePlayers;

        return 1.0D + MAX_PREFERENCE_ADJUSTMENT
                * (preferenceCurve(likedShare) - preferenceCurve(dislikedShare));
    }

    public static double finalWeight(double baseWeight, int likes, int dislikes, int eligiblePlayers) {
        if (!Double.isFinite(baseWeight) || baseWeight <= 0.0D) return 0.0D;
        return baseWeight * preferenceMultiplier(likes, dislikes, eligiblePlayers);
    }

    /**
     * Calculates each game's chance of being selected from its final weight.
     * Games with a zero/invalid base weight remain visible with a 0% chance.
     */
    public static Map<String, Double> selectionProbabilities(
            Map<String, Double> baseWeights,
            Map<String, Counts> preferenceCounts,
            int eligiblePlayers) {
        Map<String, Double> weighted = new LinkedHashMap<>();
        double totalWeight = 0.0D;

        for (Map.Entry<String, Double> entry : baseWeights.entrySet()) {
            String gameId = entry.getKey();
            double baseWeight = entry.getValue() == null ? 0.0D : entry.getValue();
            Counts counts = preferenceCounts.getOrDefault(gameId, new Counts(0, 0));
            double weight = finalWeight(baseWeight, counts.likes(), counts.dislikes(), eligiblePlayers);
            weighted.put(gameId, weight);
            totalWeight += weight;
        }

        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : weighted.entrySet()) {
            double probability = totalWeight > 0.0D ? entry.getValue() / totalWeight : 0.0D;
            probabilities.put(entry.getKey(), probability);
        }
        return Collections.unmodifiableMap(probabilities);
    }
}
