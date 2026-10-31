package au.com.testplex.arcadeports;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Selects the next regular Arcade game from the live lobby's persistent preferences. */
public final class GamePreferenceRotation {
    private GamePreferenceRotation() {
    }

    public static ArcadePortGame chooseNext(
            JavaPlugin plugin,
            List<ArcadePortGame> candidates,
            List<Player> eligiblePlayers,
            String previousGameId,
            boolean avoidConsecutiveGames,
            double randomUnit) {
        if (plugin == null || candidates == null || candidates.isEmpty()
                || eligiblePlayers == null || eligiblePlayers.isEmpty()) return null;

        GamePreferenceStore store = new GamePreferenceStore(plugin);
        Map<String, Double> baseWeights = new LinkedHashMap<>();
        Map<String, GamePreferenceWeights.Counts> counts = new LinkedHashMap<>();
        int eligibleCount = 0;
        for (Player player : eligiblePlayers) {
            if (player != null && player.isOnline()) eligibleCount++;
        }
        if (eligibleCount == 0) return null;

        for (ArcadePortGame game : candidates) {
            if (game == null) continue;
            double baseWeight = plugin.getConfig().getDouble(
                    "game-preferences.base-weights." + game.id(), 1.0D);
            if (!Double.isFinite(baseWeight) || baseWeight <= 0.0D) baseWeight = 0.0D;
            baseWeights.put(game.id(), baseWeight);

            int likes = 0;
            int dislikes = 0;
            for (Player player : eligiblePlayers) {
                if (player == null || !player.isOnline()) continue;
                GamePreferenceStore.Preference preference =
                        store.getPreference(player.getUniqueId(), game.id());
                if (preference == GamePreferenceStore.Preference.LIKE) likes++;
                else if (preference == GamePreferenceStore.Preference.DISLIKE) dislikes++;
            }
            counts.put(game.id(), new GamePreferenceWeights.Counts(likes, dislikes));
        }

        Map<String, Double> probabilities =
                GamePreferenceWeights.selectionProbabilities(baseWeights, counts, eligibleCount);
        String selectedId = chooseWeightedGameId(
                probabilities, previousGameId, avoidConsecutiveGames, randomUnit);
        return selectedId == null ? null : ArcadePortGame.fromId(selectedId);
    }

    static String chooseWeightedGameId(
            Map<String, Double> probabilities,
            String previousGameId,
            boolean avoidConsecutiveGames,
            double randomUnit) {
        if (probabilities == null || probabilities.isEmpty()) return null;

        List<Map.Entry<String, Double>> available = new ArrayList<>();
        for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
            String id = entry.getKey();
            Double weight = entry.getValue();
            if (id != null && !id.isBlank() && weight != null
                    && Double.isFinite(weight) && weight > 0.0D) {
                available.add(Map.entry(id, weight));
            }
        }
        if (available.isEmpty()) return null;

        if (avoidConsecutiveGames && previousGameId != null && available.size() > 1) {
            List<Map.Entry<String, Double>> alternatives = available.stream()
                    .filter(entry -> !previousGameId.equals(entry.getKey()))
                    .toList();
            if (!alternatives.isEmpty()) available = alternatives;
        }

        double totalWeight = available.stream().mapToDouble(Map.Entry::getValue).sum();
        if (!Double.isFinite(totalWeight) || totalWeight <= 0.0D) return null;

        double draw = Double.isFinite(randomUnit)
                ? Math.max(0.0D, Math.min(Math.nextDown(1.0D), randomUnit))
                : 0.0D;
        double target = draw * totalWeight;
        double cumulative = 0.0D;
        String lastId = null;
        for (Map.Entry<String, Double> entry : available) {
            lastId = entry.getKey();
            cumulative += entry.getValue();
            if (target < cumulative) return entry.getKey();
        }
        return lastId;
    }
}
