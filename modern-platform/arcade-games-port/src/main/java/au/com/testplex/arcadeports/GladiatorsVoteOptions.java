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

    /** Place every available mode in a balanced, centered row of a 27-slot menu. */
    static int[] centeredVoteSlots(int count) {
        return switch (count) {
            case 0 -> new int[0];
            case 1 -> new int[]{13};
            case 2 -> new int[]{12, 14};
            case 3 -> new int[]{11, 13, 15};
            case 4 -> new int[]{11, 12, 14, 15};
            case 5 -> new int[]{9, 11, 13, 15, 17};
            case 6 -> new int[]{10, 11, 12, 14, 15, 16};
            case 7 -> new int[]{10, 11, 12, 13, 14, 15, 16};
            case 8 -> new int[]{9, 10, 11, 12, 14, 15, 16, 17};
            case 9 -> new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17};
            default -> throw new IllegalArgumentException("A 27-slot vote menu supports at most 9 modes.");
        };
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
