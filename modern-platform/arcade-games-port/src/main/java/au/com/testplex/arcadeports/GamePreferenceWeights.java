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
        double multiplier = preferenceMultiplier(likes, dislikes, eligiblePlayers);
        // Saturate the standalone helper when the mathematical product exceeds
        // the largest representable double. Selection normalization below scales
        // base weights first so relative odds are still preserved exactly enough.
        return baseWeight > Double.MAX_VALUE / multiplier
                ? Double.MAX_VALUE : baseWeight * multiplier;
    }

    /**
     * Calculates each game's chance of being selected from its final weight.
     * Games with a zero/invalid base weight remain visible with a 0% chance.
     */
    public static Map<String, Double> selectionProbabilities(
            Map<String, Double> baseWeights,
            Map<String, Counts> preferenceCounts,
            int eligiblePlayers) {
        Map<String, Double> weights = new LinkedHashMap<>();
        Map<String, Counts> countsByGame = preferenceCounts == null ? Map.of() : preferenceCounts;
        double maximumBaseWeight = 0.0D;

        if (baseWeights == null || baseWeights.isEmpty()) return Map.of();
        for (Map.Entry<String, Double> entry : baseWeights.entrySet()) {
            double baseWeight = entry.getValue() == null ? 0.0D : entry.getValue();
            if (Double.isFinite(baseWeight) && baseWeight > maximumBaseWeight) {
                maximumBaseWeight = baseWeight;
            }
        }

        // Scale by the largest base weight before applying the preference curve.
        // Each adjusted value is therefore in [0, 1.95], so neither multiplication
        // nor the total can overflow even for Double.MAX_VALUE config values.
        double totalWeight = 0.0D;
        for (Map.Entry<String, Double> entry : baseWeights.entrySet()) {
            String gameId = entry.getKey();
            double baseWeight = entry.getValue() == null ? 0.0D : entry.getValue();
            double scaledWeight = 0.0D;
            if (maximumBaseWeight > 0.0D && Double.isFinite(baseWeight) && baseWeight > 0.0D) {
                Counts counts = countsByGame.getOrDefault(gameId, new Counts(0, 0));
                scaledWeight = (baseWeight / maximumBaseWeight)
                        * preferenceMultiplier(counts.likes(), counts.dislikes(), eligiblePlayers);
            }
            weights.put(gameId, scaledWeight);
            totalWeight += scaledWeight;
        }

        Map<String, Double> probabilities = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            double probability = totalWeight > 0.0D ? entry.getValue() / totalWeight : 0.0D;
            probabilities.put(entry.getKey(), probability);
        }
        return Collections.unmodifiableMap(probabilities);
    }
}
