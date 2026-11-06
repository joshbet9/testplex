package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/** A game's cohort is human accounts with at least one recorded completed match in that game. */
final class ArcadeStatsRarity {
    private ArcadeStatsRarity() { }

    static String percent(int unlocked, int eligible) {
        if (eligible <= 0) return "N/A (no recorded players)";
        return String.format(Locale.ROOT, "%.1f%% (%d/%d players)",
                Math.min(eligible, Math.max(0, unlocked)) * 100.0D / eligible,
                Math.min(eligible, Math.max(0, unlocked)), eligible);
    }

    /** Lowest unlock count first (rarest/hardest), with original catalogue order for ties. */
    static <T> List<T> hardestFirst(List<T> original, ToIntFunction<T> unlockedCount) {
        List<T> ordered = new ArrayList<>(original);
        ordered.sort(Comparator.comparingInt(unlockedCount));
        return List.copyOf(ordered);
    }
}
