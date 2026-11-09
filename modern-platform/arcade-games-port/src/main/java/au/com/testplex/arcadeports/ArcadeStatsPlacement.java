package au.com.testplex.arcadeports;

import java.util.Collection;
import java.util.Locale;

/**
 * Local Testplex career and personal-best rankings. Ties share the same competition rank.
 * The display bands match Mineplex's placement colours, not Mineplex's player population.
 */
public final class ArcadeStatsPlacement {
    public enum Band { PINK, BLUE, GREEN, YELLOW, WHITE, GRAY }

    public record Result(int rank, int participants, Band band, String label) { }

    private ArcadeStatsPlacement() { }

    public static Result higherIsBetter(long value, Collection<Long> competitors) {
        return rank(value, competitors, false);
    }

    public static Result lowerIsBetter(long value, Collection<Long> competitors) {
        return rank(value, competitors, true);
    }

    private static Result rank(long value, Collection<Long> competitors, boolean ascending) {
        if (competitors == null || competitors.isEmpty()) return null;
        int better = 0;
        int total = 0;
        for (Long competitor : competitors) {
            if (competitor == null) continue;
            total++;
            if (ascending ? competitor < value : competitor > value) better++;
        }
        if (total == 0) return null;
        int position = better + 1;
        if (position <= 10) return new Result(position, total, Band.PINK, "#" + position);
        if (position <= 100) return new Result(position, total, Band.BLUE, "#" + position);
        if (position <= 1_000) return new Result(position, total, Band.GREEN, "#" + position);
        // Round the top-percentile up to the nearest tenth so no rank is overstated.
        double percentile = Math.ceil(position * 1_000.0D / total) / 10.0D;
        Band band = percentile <= 5.0D ? Band.YELLOW : percentile <= 25.0D ? Band.WHITE : Band.GRAY;
        return new Result(position, total, band,
                String.format(Locale.ROOT, "top %.1f%%", percentile));
    }
}
