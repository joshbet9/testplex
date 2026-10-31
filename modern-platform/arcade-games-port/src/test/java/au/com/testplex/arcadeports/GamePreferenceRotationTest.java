package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GamePreferenceRotationTest {
    @Test
    void selectsFromTheCorrectWeightedBand() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("first", 0.75D);
        probabilities.put("second", 0.25D);

        assertEquals("first", GamePreferenceRotation.chooseWeightedGameId(
                probabilities, null, false, 0.0D));
        assertEquals("first", GamePreferenceRotation.chooseWeightedGameId(
                probabilities, null, false, 0.749D));
        assertEquals("second", GamePreferenceRotation.chooseWeightedGameId(
                probabilities, null, false, 0.75D));
    }

    @Test
    void avoidsThePreviouslyPlayedGameWhenAnAlternativeExists() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("last-game", 0.99D);
        probabilities.put("alternative", 0.01D);

        assertEquals("alternative", GamePreferenceRotation.chooseWeightedGameId(
                probabilities, "last-game", true, 0.0D));
    }

    @Test
    void preferenceDisplayMatchesTheNoRepeatSelectorOdds() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("previous", 0.75D);
        probabilities.put("next-a", 0.20D);
        probabilities.put("next-b", 0.05D);

        Map<String, Double> effective = GamePreferenceRotation.effectiveProbabilities(
                probabilities, "previous", true);
        assertEquals(0.0D, effective.get("previous"), 0.0000001D);
        assertEquals(0.8D, effective.get("next-a"), 0.0000001D);
        assertEquals(0.2D, effective.get("next-b"), 0.0000001D);
        assertEquals(1.0D, effective.values().stream().mapToDouble(Double::doubleValue).sum(), 0.0000001D);
    }

    @Test
    void keepsPreviousGameOddsWhenNoAlternativeHasPositiveWeight() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("previous", 1.0D);
        probabilities.put("zero", 0.0D);
        Map<String, Double> effective = GamePreferenceRotation.effectiveProbabilities(
                probabilities, "previous", true);
        assertEquals(1.0D, effective.get("previous"), 0.0000001D);
        assertEquals(0.0D, effective.get("zero"), 0.0000001D);
    }

    @Test
    void permitsThePreviousGameWhenItIsTheOnlyAvailableCandidate() {
        assertEquals("only-game", GamePreferenceRotation.chooseWeightedGameId(
                Map.of("only-game", 1.0D), "only-game", true, 0.5D));
    }

    @Test
    void ignoresZeroAndInvalidWeights() {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        probabilities.put("zero", 0.0D);
        probabilities.put("nan", Double.NaN);
        probabilities.put("valid", 1.0D);

        assertEquals("valid", GamePreferenceRotation.chooseWeightedGameId(
                probabilities, null, false, 0.9D));
        assertNull(GamePreferenceRotation.chooseWeightedGameId(
                Map.of("zero", 0.0D), null, false, 0.5D));
    }

    @Test
    void clampsOutOfRangeRandomDraws() {
        assertEquals("first", GamePreferenceRotation.chooseWeightedGameId(
                Map.of("first", 1.0D), null, false, -3.0D));
        assertEquals("first", GamePreferenceRotation.chooseWeightedGameId(
                Map.of("first", 1.0D), null, false, 4.0D));
        assertEquals("first", GamePreferenceRotation.chooseWeightedGameId(
                Map.of("first", 1.0D), null, false, Double.NaN));
    }
}
