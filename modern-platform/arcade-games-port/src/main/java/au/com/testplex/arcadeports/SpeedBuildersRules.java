package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic source-parity rules for the Paper 26.3 Speed Builders port. */
public final class SpeedBuildersRules {
    public static final int BUILD_WIDTH = 7;
    public static final int BUILD_HEIGHT = 7;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 8;
    public static final int DEFAULT_VIEW_SECONDS = 10;
    public static final int DEFAULT_BUILD_SECONDS = 40;
    public static final int MIN_BUILD_SECONDS = 5;
    public static final int MAX_BUILD_SECONDS = 60;
    public static final int REVIEW_SECONDS = 10;
    public static final int ELIMINATION_INTERVAL_SECONDS = 2;
    public static final int INTERMISSION_SECONDS = 3;

    private SpeedBuildersRules() { }

    public record Coordinate(int x, int y, int z) { }

    /** A source template cell stores both its modern material and full BlockData state. */
    public record Cell(int x, int y, int z, Material material, String blockData) {
        public Cell {
            if (x < 0 || x >= BUILD_WIDTH || y < 0 || y >= BUILD_HEIGHT
                    || z < 0 || z >= BUILD_WIDTH) {
                throw new IllegalArgumentException("Template cell is outside the 7x7x7 build volume.");
            }
            Objects.requireNonNull(material, "material");
            blockData = blockData == null ? "" : blockData;
        }

        /** Compatibility constructor for material-only fixtures. */
        public Cell(int x, int y, int z, Material material) {
            this(x, y, z, material, "");
        }

        public Coordinate coordinate() {
            return new Coordinate(x, y, z);
        }
    }

    public record BlockState(Material material, String blockData) {
        public BlockState {
            Objects.requireNonNull(material, "material");
            blockData = blockData == null ? "" : blockData;
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

    public record TemplateTiming(String displayName, int addSeconds, int subtractSeconds,
                                 double multiplier, Integer exactSeconds) {
        public TemplateTiming {
            displayName = displayName == null ? "" : displayName.trim();
            if (!Double.isFinite(multiplier) || multiplier < 0.0D
                    || addSeconds < 0 || subtractSeconds < 0
                    || (exactSeconds != null && exactSeconds < 0)) {
                throw new IllegalArgumentException("Invalid Speed Builders template timing.");
            }
        }

        public int buildSeconds(int baseSeconds) {
            return SpeedBuildersRules.buildSeconds(baseSeconds, addSeconds, subtractSeconds,
                    multiplier, exactSeconds);
        }
    }

    /** Parses Reborn's time+N, time-N, time=N and time*X custom-location modifiers. */
    public static TemplateTiming parseTemplateName(String sourceName) {
        StringBuilder display = new StringBuilder();
        int add = 0;
        int subtract = 0;
        Integer exact = null;
        double multiplier = 1.0D;
        if (sourceName != null) {
            for (String token : sourceName.trim().split("\\s+")) {
                if (token.matches("(?i)^time[+-=][0-9]+$")) {
                    int value = Integer.parseInt(token.substring(5));
                    switch (token.charAt(4)) {
                        case '+' -> add = value;
                        case '-' -> subtract = value;
                        case '=' -> exact = value;
                        default -> { }
                    }
                } else if (token.matches("(?i)^time\\*[0-9]*\\.?[0-9]+$")) {
                    multiplier = Double.parseDouble(token.substring(5));
                } else {
                    if (!display.isEmpty()) display.append(' ');
                    display.append(token);
                }
            }
        }
        return new TemplateTiming(display.toString(), add, subtract, multiplier, exact);
    }

    public static int buildSeconds(int baseSeconds, int addSeconds, int subtractSeconds,
                                   double multiplier, Integer exactSeconds) {
        if (baseSeconds < 0 || addSeconds < 0 || subtractSeconds < 0
                || !Double.isFinite(multiplier) || multiplier < 0.0D
                || (exactSeconds != null && exactSeconds < 0)) {
            throw new IllegalArgumentException("Invalid template timing modifier.");
        }
        int adjusted = (int) (multiplier * (baseSeconds + addSeconds - subtractSeconds));
        if (exactSeconds != null) adjusted = exactSeconds;
        return Math.max(MIN_BUILD_SECONDS, Math.min(MAX_BUILD_SECONDS, adjusted));
    }

    /** Material-only compatibility scorer used by older simple fixtures. */
    public static int score(List<Cell> expected, Map<Coordinate, Material> actual) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        int score = 0;
        for (Cell cell : expected) {
            if (cell.material() == actual.get(cell.coordinate())) score++;
        }
        return score;
    }

    /** Source scorer: score each expected non-air cell once; extra blocks do not deduct points. */
    public static int scoreBlockStates(List<Cell> expected, Map<Coordinate, BlockState> actual) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        int score = 0;
        for (Cell cell : expected) {
            BlockState placed = actual.get(cell.coordinate());
            if (placed != null && blockMatches(cell.material(), cell.blockData(),
                    placed.material(), placed.blockData())) score++;
        }
        return score;
    }

    public static boolean isPerfect(List<Cell> expected, Map<Coordinate, Material> actual) {
        return !expected.isEmpty() && score(expected, actual) == expected.size();
    }

    public static boolean isPerfectBlockStateBuild(List<Cell> expected, Map<Coordinate, BlockState> actual) {
        return !expected.isEmpty() && scoreBlockStates(expected, actual) == expected.size();
    }

    /**
     * Source 1.8 comparison is exact for normal blocks, with explicit compatibility
     * exceptions for state that naturally changes while a player is recreating a model.
     */
    public static boolean blockMatches(Material expectedMaterial, String expectedData,
                                       Material actualMaterial, String actualData) {
        if (expectedMaterial == null || actualMaterial == null || expectedMaterial == Material.AIR) return false;
        if (expectedMaterial != actualMaterial && !(isAnvil(expectedMaterial) && isAnvil(actualMaterial))) {
            return false;
        }
        Map<String, String> expected = properties(expectedData);
        Map<String, String> actual = properties(actualData);
        if (expected.equals(actual)) return true;

        if (isAnvil(expectedMaterial)) {
            return samePropertiesExcept(expected, actual, Set.of("damage"));
        }
        if (isEndPortalFrame(expectedMaterial)) {
            // Reborn's 1.8 rule ignores frame facing but still requires the same eye state.
            return Objects.equals(expected.get("eye"), actual.get("eye"));
        }
        if (isSapling(expectedMaterial)) {
            return samePropertiesExcept(expected, actual, Set.of("stage"));
        }
        if (isLeaves(expectedMaterial)) {
            return samePropertiesExcept(expected, actual, Set.of("distance", "persistent"));
        }
        if (isRedstoneOre(expectedMaterial)) {
            return samePropertiesExcept(expected, actual, Set.of("lit"));
        }
        if (isStairs(expectedMaterial)) {
            String expectedShape = expected.get("shape");
            String actualShape = actual.get("shape");
            if (!stairShapeCompatible(expectedShape, actualShape)) return false;
            return samePropertiesExcept(expected, actual, Set.of("shape"));
        }
        return false;
    }

    private static Map<String, String> properties(String blockData) {
        if (blockData == null || blockData.isBlank()) return Map.of();
        int open = blockData.indexOf('[');
        int close = blockData.lastIndexOf(']');
        if (open < 0 || close <= open) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : blockData.substring(open + 1, close).split(",")) {
            int equals = token.indexOf('=');
            if (equals <= 0) continue;
            result.put(token.substring(0, equals).trim().toLowerCase(java.util.Locale.ROOT),
                    token.substring(equals + 1).trim().toLowerCase(java.util.Locale.ROOT));
        }
        return result;
    }

    private static boolean samePropertiesExcept(Map<String, String> expected, Map<String, String> actual,
                                                Set<String> ignored) {
        Map<String, String> left = new LinkedHashMap<>(expected);
        Map<String, String> right = new LinkedHashMap<>(actual);
        ignored.forEach(key -> { left.remove(key); right.remove(key); });
        return left.equals(right);
    }

    private static boolean stairShapeCompatible(String expected, String actual) {
        if (expected == null || actual == null) return false;
        if (expected.equals(actual)) return true;
        return (expected.startsWith("inner_") && actual.startsWith("inner_"))
                || (expected.startsWith("outer_") && actual.startsWith("outer_"));
    }

    private static boolean isAnvil(Material material) {
        String name = material.name();
        return name.equals("ANVIL") || name.equals("CHIPPED_ANVIL") || name.equals("DAMAGED_ANVIL");
    }

    private static boolean isEndPortalFrame(Material material) {
        return material.name().equals("END_PORTAL_FRAME");
    }

    private static boolean isSapling(Material material) {
        return material.name().endsWith("_SAPLING") || material.name().equals("SAPLING");
    }

    private static boolean isLeaves(Material material) {
        return material.name().endsWith("_LEAVES") || material.name().equals("LEAVES") || material.name().equals("LEAVES_2");
    }

    private static boolean isRedstoneOre(Material material) {
        return material.name().equals("REDSTONE_ORE") || material.name().equals("DEEPSLATE_REDSTONE_ORE");
    }

    private static boolean isStairs(Material material) {
        return material.name().endsWith("_STAIRS");
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
