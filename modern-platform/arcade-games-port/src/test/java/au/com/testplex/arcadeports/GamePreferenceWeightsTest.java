package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePreferenceWeightsTest {
    @Test
    void unanimousLikeRaisesTheBaseWeightToOnePointNineFive() {
        assertEquals(1.95D, GamePreferenceWeights.preferenceMultiplier(12, 0, 12), 0.000001D);
        assertEquals(1.95D, GamePreferenceWeights.finalWeight(1.0D, 12, 0, 12), 0.000001D);
    }

    @Test
    void unanimousDislikeReducesTheBaseWeightToZeroPointZeroFive() {
        assertEquals(0.05D, GamePreferenceWeights.preferenceMultiplier(0, 12, 12), 0.000001D);
    }

    @Test
    void equalLikeAndDislikeCountsCancelOut() {
        assertEquals(1.0D, GamePreferenceWeights.preferenceMultiplier(5, 5, 12), 0.000001D);
    }

    @Test
    void neutralPlayersDoNotDirectlyChangeTheMultiplier() {
        assertEquals(1.0D, GamePreferenceWeights.preferenceMultiplier(0, 0, 12), 0.000001D);
    }

    @Test
    void curveIsSmoothAndHasExpectedMidpoint() {
        assertEquals(0.0D, GamePreferenceWeights.preferenceCurve(0.0D), 0.000001D);
        assertEquals(0.5D, GamePreferenceWeights.preferenceCurve(0.5D), 0.000001D);
        assertEquals(1.0D, GamePreferenceWeights.preferenceCurve(1.0D), 0.000001D);
        assertTrue(GamePreferenceWeights.preferenceCurve(0.75D)
                > GamePreferenceWeights.preferenceCurve(0.5D));
    }

    @Test
    void usesTheActualEligibleLobbySizeAsTheDenominator() {
        double multiplier = GamePreferenceWeights.preferenceMultiplier(8, 1, 12);
        assertEquals(1.746D, multiplier, 0.001D);
    }

    @Test
    void probabilitiesNormaliseFinalWeightsAndRespectBaseWeights() {
        Map<String, Double> baseWeights = new LinkedHashMap<>();
        baseWeights.put("liked", 1.0D);
        baseWeights.put("disliked", 1.0D);
        Map<String, GamePreferenceWeights.Counts> counts = new LinkedHashMap<>();
        counts.put("liked", new GamePreferenceWeights.Counts(12, 0));
        counts.put("disliked", new GamePreferenceWeights.Counts(0, 12));

        Map<String, Double> probabilities =
                GamePreferenceWeights.selectionProbabilities(baseWeights, counts, 12);
        assertEquals(0.975D, probabilities.get("liked"), 0.000001D);
        assertEquals(0.025D, probabilities.get("disliked"), 0.000001D);
        assertEquals(1.0D, probabilities.values().stream().mapToDouble(Double::doubleValue).sum(),
                0.000001D);
    }

    @Test
    void extremeFiniteBaseWeightsStillProduceFiniteNormalizedProbabilities() {
        Map<String, Double> baseWeights = new LinkedHashMap<>();
        baseWeights.put("liked", Double.MAX_VALUE);
        baseWeights.put("neutral", Double.MAX_VALUE / 2.0D);
        Map<String, GamePreferenceWeights.Counts> counts = new LinkedHashMap<>();
        counts.put("liked", new GamePreferenceWeights.Counts(12, 0));
        counts.put("neutral", new GamePreferenceWeights.Counts(0, 0));

        Map<String, Double> probabilities =
                GamePreferenceWeights.selectionProbabilities(baseWeights, counts, 12);
        assertTrue(probabilities.values().stream().allMatch(Double::isFinite));
        assertEquals(1.0D, probabilities.values().stream().mapToDouble(Double::doubleValue).sum(), 0.000001D);
        assertEquals(1.95D / (1.95D + 0.5D), probabilities.get("liked"), 0.000001D);
        assertEquals(0.5D / (1.95D + 0.5D), probabilities.get("neutral"), 0.000001D);
        assertEquals(Double.MAX_VALUE,
                GamePreferenceWeights.finalWeight(Double.MAX_VALUE, 12, 0, 12));
    }

    @Test
    void noEligiblePlayersLeavesTheBaseWeightsUnmodified() {
        Map<String, Double> baseWeights = Map.of("first", 2.0D, "second", 1.0D);
        Map<String, Double> probabilities =
                GamePreferenceWeights.selectionProbabilities(baseWeights, Map.of(), 0);
        assertEquals(2.0D / 3.0D, probabilities.get("first"), 0.000001D);
        assertEquals(1.0D / 3.0D, probabilities.get("second"), 0.000001D);
    }
}
