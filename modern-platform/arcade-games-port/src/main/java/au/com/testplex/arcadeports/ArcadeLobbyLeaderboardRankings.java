package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure leaderboard ranking and around-you windows, shared by every Arcade game. */
final class ArcadeLobbyLeaderboardRankings {
    record Entry(UUID player, long value, int rank) { }

    private ArcadeLobbyLeaderboardRankings() { }

    static List<Entry> rank(Map<UUID, Long> scores, boolean ascending) {
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(scores.entrySet());
        Comparator<Map.Entry<UUID, Long>> byScore = Comparator.comparingLong(Map.Entry::getValue);
        if (!ascending) byScore = byScore.reversed();
        sorted.sort(byScore.thenComparing(e -> e.getKey().toString()));
        List<Entry> result = new ArrayList<>(sorted.size());
        int rank = 0;
        long previous = 0;
        for (int index = 0; index < sorted.size(); index++) {
            Map.Entry<UUID, Long> row = sorted.get(index);
            if (index == 0 || row.getValue() != previous) rank = index + 1;
            result.add(new Entry(row.getKey(), row.getValue(), rank));
            previous = row.getValue();
        }
        return List.copyOf(result);
    }

    static List<Entry> visible(List<Entry> ranked, UUID viewer, boolean aroundYou, int maximum) {
        if (ranked.isEmpty() || maximum <= 0) return List.of();
        int limit = Math.min(maximum, ranked.size());
        if (!aroundYou) return ranked.subList(0, limit);
        int index = 0;
        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).player().equals(viewer)) {
                index = i;
                break;
            }
        }
        int start = Math.max(0, Math.min(index - limit / 2, ranked.size() - limit));
        return ranked.subList(start, start + limit);
    }
}
