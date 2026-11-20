package au.com.testplex.arcadeports;

import org.bukkit.configuration.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-map, per-kit longest survival records for Dragons. */
final class DragonsSurvivalRecords {
    record Run(UUID playerId, long elapsedMillis) { }
    record RankedRun(UUID playerId, long elapsedMillis, int rank) { }

    private DragonsSurvivalRecords() { }

    static String path(UUID playerId, String mapResource, String kitId) {
        String mapKey = ArcadePortRules.dragonEscapeMapStorageKey(mapResource);
        if (playerId == null || mapKey.isBlank() || kitId == null || kitId.isBlank()) return "";
        return "dragons-survival-pb." + playerId + "." + mapKey + "." + kitId + ".time-ms";
    }

    static long get(Configuration config, UUID playerId, String mapResource, String kitId) {
        String path = path(playerId, mapResource, kitId);
        return path.isEmpty() ? -1L : config.getLong(path, -1L);
    }

    /** True only when a positive, longer survival record replaces the previous best. */
    static boolean update(Configuration config, UUID playerId, String mapResource,
            String kitId, long elapsedMillis) {
        String path = path(playerId, mapResource, kitId);
        long existing = path.isEmpty() ? -1L : config.getLong(path, -1L);
        if (path.isEmpty() || elapsedMillis <= 0L || elapsedMillis <= existing) return false;
        config.set(path, elapsedMillis);
        return true;
    }

    /** Longest time ranks first; equal times share competition rank. */
    static List<RankedRun> rank(Collection<Run> recorded) {
        if (recorded == null || recorded.isEmpty()) return List.of();
        Map<UUID, Long> bestByPlayer = new HashMap<>();
        for (Run run : recorded) {
            if (run == null || run.playerId() == null || run.elapsedMillis() <= 0L) continue;
            bestByPlayer.merge(run.playerId(), run.elapsedMillis(), Math::max);
        }
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(bestByPlayer.entrySet());
        sorted.sort(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(entry -> entry.getKey().toString()));
        List<RankedRun> result = new ArrayList<>(sorted.size());
        long previousTime = -1L;
        int previousRank = 0;
        for (int index = 0; index < sorted.size(); index++) {
            Map.Entry<UUID, Long> entry = sorted.get(index);
            int rank = entry.getValue() == previousTime ? previousRank : index + 1;
            result.add(new RankedRun(entry.getKey(), entry.getValue(), rank));
            previousTime = entry.getValue();
            previousRank = rank;
        }
        return List.copyOf(result);
    }
}
