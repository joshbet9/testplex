package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Creates one fixed three-choice Gladiators vote for a tournament. */
final class GladiatorsVoteOptions {
    static final int OPTION_COUNT = 3;

    private GladiatorsVoteOptions() { }

    static List<GladiatorsMode> randomOptions() {
        return randomOptions(new Random());
    }

    static List<GladiatorsMode> randomOptions(Random random) {
        if (random == null) throw new IllegalArgumentException("random is required");
        List<GladiatorsMode> pool = new ArrayList<>(List.of(GladiatorsMode.values()));
        Collections.shuffle(pool, random);
        return List.copyOf(pool.subList(0, Math.min(OPTION_COUNT, pool.size())));
    }

    static GladiatorsMode winner(List<GladiatorsMode> options, Collection<String> votes) {
        if (options == null || options.isEmpty()) return GladiatorsMode.defaultMode();
        Map<GladiatorsMode, Integer> counts = new EnumMap<>(GladiatorsMode.class);
        if (votes != null) {
            for (String vote : votes) {
                GladiatorsMode mode = GladiatorsMode.fromId(vote);
                if (mode != null && options.contains(mode)) counts.merge(mode, 1, Integer::sum);
            }
        }
        if (counts.values().stream().mapToInt(Integer::intValue).sum() == 0) {
            return GladiatorsMode.defaultMode();
        }
        GladiatorsMode selected = options.get(0);
        int highest = counts.getOrDefault(selected, 0);
        for (int i = 1; i < options.size(); i++) {
            GladiatorsMode candidate = options.get(i);
            int count = counts.getOrDefault(candidate, 0);
            if (count > highest) {
                selected = candidate;
                highest = count;
            }
        }
        return selected;
    }
}
