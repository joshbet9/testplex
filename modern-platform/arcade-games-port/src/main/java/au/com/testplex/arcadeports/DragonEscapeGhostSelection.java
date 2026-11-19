package au.com.testplex.arcadeports;

import java.util.Locale;
import java.util.List;
import java.util.UUID;

/** Selection and privacy rules shared by the hotbar menu, race rendering and finish summary. */
final class DragonEscapeGhostSelection {
    enum Mode { PERSONAL_BEST, CURRENT_KIT, TARGET_PLAYER }
    static final int MAX_CONCURRENT_SOURCES = 4;
    private static final List<String> SOURCE_KEYS = List.of(
            "personal-pb", "kit-pb", "world-record", "kit-record", "target");

    record Source(String key, UUID playerId, long timeMillis, String kitId, String label) { }

    private DragonEscapeGhostSelection() { }

    static Mode mode(String requested) {
        if (requested == null) return Mode.PERSONAL_BEST;
        return switch (requested.toLowerCase(Locale.ROOT)) {
            case "kit", "current-kit" -> Mode.CURRENT_KIT;
            case "target", "target-player" -> Mode.TARGET_PLAYER;
            default -> Mode.PERSONAL_BEST;
        };
    }

    static List<String> sourceKeys() { return SOURCE_KEYS; }

    static String kitDisplayName(String kitId) {
        if (kitId == null || kitId.isBlank()) return "Unknown Kit";
        String normalized = kitId.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jumper" -> "Jumper";
            case "disruptor" -> "Disruptor";
            case "builder" -> "Builder";
            case "warper" -> "Warper";
            case "digger" -> "Digger";
            default -> {
                StringBuilder name = new StringBuilder();
                for (String word : normalized.replace('-', ' ').replace('_', ' ').split("\\s+")) {
                    if (word.isBlank()) continue;
                    if (!name.isEmpty()) name.append(' ');
                    name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
                yield name.isEmpty() ? "Unknown Kit" : name.toString();
            }
        };
    }

    static List<Source> limitConcurrent(List<Source> sources) {
        if (sources == null || sources.isEmpty()) return List.of();
        return List.copyOf(sources.subList(0, Math.min(sources.size(), MAX_CONCURRENT_SOURCES)));
    }

    static boolean canShare(UUID owner, UUID viewer, boolean publiclyShared) {
        return owner != null && (owner.equals(viewer) || publiclyShared);
    }

    static String relativeFinish(long actualMillis, long comparisonMillis) {
        if (actualMillis <= 0L || comparisonMillis <= 0L) return "No comparison available";
        long delta = actualMillis - comparisonMillis;
        if (delta == 0L) return "exactly matched";
        return ArcadePortRules.formatDragonEscapeTime(Math.abs(delta))
                + (delta < 0L ? " faster" : " slower");
    }
}
