package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** The selectable public leaderboard metrics, including newly recorded stats. */
final class ArcadeLobbyLeaderboardMetricCatalog {
    record Metric(String id, String label, boolean lowerIsBetter) { }

    private static final Set<String> BASE = Set.of("wins", "games-played", "losses", "test-gems-earned");

    private ArcadeLobbyLeaderboardMetricCatalog() { }

    static List<Metric> metrics(String gameId, Collection<String> trackedStatIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(List.of("wins", "games-played", "losses", "test-gems-earned"));
        if ("dragon-escape".equals(gameId)) ids.add("pb");
        if (trackedStatIds != null) {
            for (String id : new TreeSet<>(trackedStatIds)) {
                if (isSelectableStatId(id)) ids.add(id);
            }
        }
        return ids.stream().map(id -> new Metric(id, label(id), lowerIsBetter(id))).toList();
    }

    static boolean isSelectableStatId(String id) {
        if (id == null || id.isBlank() || id.length() > 120 || "pb".equals(id)) return false;
        if (BASE.contains(id)) return true;
        String[] segments = id.split("\\.", -1);
        for (String segment : segments) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) return false;
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) return false;
            }
        }
        return true;
    }

    static String label(String id) {
        if (id == null || id.isBlank()) return "Unknown Stat";
        String[] parts = id.split("\\.");
        if (parts.length > 1) {
            String suffix = humanize(String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)));
            return humanize(parts[0]) + ": " + suffix;
        }
        return switch (id) {
            case "games-played" -> "Games Played";
            case "test-gems-earned" -> "Test Gems Earned";
            case "pb" -> "Personal Best";
            default -> humanize(id);
        };
    }

    private static boolean lowerIsBetter(String id) {
        String normalized = id.toLowerCase(Locale.ROOT).replace('_', '-');
        return normalized.equals("pb") || normalized.contains("personal-best")
                || normalized.contains("best-time") || normalized.contains("completion-time")
                || normalized.contains("finish-time") || normalized.contains("fastest")
                || normalized.contains("time-to-");
    }

    private static String humanize(String value) {
        String normalized = value.replace('-', ' ').replace('_', ' ').trim();
        if (normalized.isEmpty()) return "Unknown Stat";
        StringBuilder label = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (char c : normalized.toCharArray()) {
            if (Character.isWhitespace(c)) {
                label.append(c);
                capitalize = true;
            } else {
                label.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return label.toString();
    }
}
