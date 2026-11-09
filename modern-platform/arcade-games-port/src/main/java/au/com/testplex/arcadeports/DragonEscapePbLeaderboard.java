package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Testplex-only per-map race rankings. There is no Mineplex production leaderboard dependency. */
public final class DragonEscapePbLeaderboard {
    public record Run(UUID playerId, long elapsedMillis, String kitId) { }
    public record RankedRun(UUID playerId, long elapsedMillis, String kitId, int rank) { }

    private DragonEscapePbLeaderboard() { }

    /** Lowest valid completion time wins; identical times share the same competition rank. */
    public static List<RankedRun> rank(Collection<Run> recorded, UUID viewer,
                                       Set<UUID> followed, boolean friendsOnly) {
        if (recorded == null || recorded.isEmpty()) return List.of();
        Map<UUID, Run> bestByPlayer = new HashMap<>();
        for (Run run : recorded) {
            if (run == null || run.playerId() == null || run.elapsedMillis() <= 0L) continue;
            if (friendsOnly && !run.playerId().equals(viewer)
                    && (followed == null || !followed.contains(run.playerId()))) continue;
            bestByPlayer.merge(run.playerId(), run,
                    (existing, candidate) -> candidate.elapsedMillis() < existing.elapsedMillis()
                            ? candidate : existing);
        }
        List<Run> sorted = new ArrayList<>(bestByPlayer.values());
        sorted.sort(Comparator.comparingLong(Run::elapsedMillis)
                .thenComparing(run -> run.playerId().toString()));
        List<RankedRun> result = new ArrayList<>(sorted.size());
        long previousTime = -1L;
        int previousRank = 0;
        for (int index = 0; index < sorted.size(); index++) {
            Run run = sorted.get(index);
            int rank = run.elapsedMillis() == previousTime ? previousRank : index + 1;
            result.add(new RankedRun(run.playerId(), run.elapsedMillis(), run.kitId(), rank));
            previousTime = run.elapsedMillis();
            previousRank = rank;
        }
        return List.copyOf(result);
    }
}
