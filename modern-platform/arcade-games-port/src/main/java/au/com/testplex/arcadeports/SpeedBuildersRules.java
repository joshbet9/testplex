package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic, source-mapped rules for the Paper 26.3 Speed Builders prototype. */
public final class SpeedBuildersRules {
    public static final int BUILD_WIDTH = 7;
    public static final int BUILD_HEIGHT = 7;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 16;
    public static final int DEFAULT_VIEW_SECONDS = 10;
    public static final int DEFAULT_BUILD_SECONDS = 40;
    public static final int MIN_BUILD_SECONDS = 5;
    public static final int MAX_BUILD_SECONDS = 60;
    public static final int REVIEW_SECONDS = 5;
    public static final int INTERMISSION_SECONDS = 3;

    private SpeedBuildersRules() { }

    public record Coordinate(int x, int y, int z) { }

    public record Cell(int x, int y, int z, Material material) {
        public Cell {
            if (x < 0 || x >= BUILD_WIDTH || y < 0 || y >= BUILD_HEIGHT
                    || z < 0 || z >= BUILD_WIDTH) {
                throw new IllegalArgumentException("Template cell is outside the 7x7x7 build volume.");
            }
            Objects.requireNonNull(material, "material");
            if (material.isAir() || !material.isBlock()) {
                throw new IllegalArgumentException("Template cells must contain placeable blocks.");
            }
        }

        public Coordinate coordinate() {
            return new Coordinate(x, y, z);
        }
    }

    public record PlayerScore(String playerId, int matchedBlocks, int expectedBlocks, boolean empty) {
        public PlayerScore {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId is required.");
            if (matchedBlocks < 0 || expectedBlocks < 0 || matchedBlocks > expectedBlocks) {
                throw new IllegalArgumentException("Invalid Speed Builders score.");
            }
        }

        public boolean perfect() {
            return expectedBlocks > 0 && matchedBlocks == expectedBlocks;
        }

        public int percent() {
            return expectedBlocks == 0 ? 0 : (int) Math.round(matchedBlocks * 100.0D / expectedBlocks);
        }
    }

    /** Counts correctly matched expected cells, matching Reborn's score model. */
    public static int score(List<Cell> expected, Map<Coordinate, Material> actual) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        int score = 0;
        for (Cell cell : expected) {
            if (cell.material() == actual.get(cell.coordinate())) score++;
        }
        return score;
    }

    public static boolean isPerfect(List<Cell> expected, Map<Coordinate, Material> actual) {
        return !expected.isEmpty() && score(expected, actual) == expected.size();
    }

    public static int buildSeconds(int baseSeconds, int addSeconds, int subtractSeconds,
                                   double multiplier, Integer exactSeconds) {
        if (baseSeconds < 0 || addSeconds < 0 || subtractSeconds < 0
                || !Double.isFinite(multiplier) || multiplier <= 0.0D
                || (exactSeconds != null && exactSeconds < 0)) {
            throw new IllegalArgumentException("Invalid template timing modifier.");
        }
        int adjusted = (int) (multiplier * (baseSeconds + addSeconds - subtractSeconds));
        if (exactSeconds != null) adjusted = exactSeconds;
        return Math.max(MIN_BUILD_SECONDS, Math.min(MAX_BUILD_SECONDS, adjusted));
    }

    /**
     * Reborn eliminates empty plots first. Otherwise it removes one lowest scorer
     * only when the round was neither a complete-perfect round nor a full tie.
     */
    public static Set<String> eliminationCandidates(List<PlayerScore> scores) {
        Objects.requireNonNull(scores, "scores");
        if (scores.isEmpty()) return Set.of();

        Set<String> eliminated = new LinkedHashSet<>();
        for (PlayerScore score : scores) {
            if (score.empty()) eliminated.add(score.playerId());
        }

        boolean allPerfect = scores.stream().allMatch(PlayerScore::perfect);
        boolean allEqual = scores.stream().map(PlayerScore::matchedBlocks).distinct().count() <= 1L;
        if (!allPerfect && !allEqual) {
            PlayerScore lowest = scores.stream()
                    .min((left, right) -> Integer.compare(left.matchedBlocks(), right.matchedBlocks()))
                    .orElse(null);
            if (lowest != null) eliminated.add(lowest.playerId());
        }
        return Set.copyOf(eliminated);
    }
}
