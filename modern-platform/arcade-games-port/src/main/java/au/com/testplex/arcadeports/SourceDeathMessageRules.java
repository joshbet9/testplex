package au.com.testplex.arcadeports;

import java.util.Locale;

/** Source-style cause wording shared by Arcade death-message paths. */
final class SourceDeathMessageRules {
    private SourceDeathMessageRules() { }

    static String dragonEscapeCause(String reason) {
        String normalized = normalize(reason);
        if (normalized.contains("void")) return "fell into the void!";
        if (normalized.contains("dragon")) return "was caught by Douglas the Dragon!";
        if (normalized.contains("fell") || normalized.contains("fall")) {
            return "fell from the Skylands course!";
        }
        return "couldn't escape the dragon!";
    }

    static String dragonEscapeDisruptorMessage(String victim, String owner, String reason) {
        return victim + " was disrupted by " + owner + " and " + dragonEscapeDisruptorCause(reason);
    }

    static String dragonEscapeDisruptorCause(String reason) {
        String normalized = normalize(reason);
        if (normalized.contains("water")) return "was lost to the water!";
        if (normalized.contains("lava")) return "was burned by lava!";
        return dragonEscapeCause(reason);
    }

    static String genericCause(String reason) {
        if (reason == null || reason.isBlank()) return "has died";
        String normalized = normalize(reason);
        // Check void before fall: void reasons often contain both words.
        if (normalized.contains("void")) return "fell into the void";
        if (normalized.contains("fell") || normalized.contains("fall")) return "fell to their death";
        if (normalized.contains("starv")) return "starved to death";
        if (normalized.contains("burn")) return "burned to death";
        if (normalized.startsWith("was ")) return normalized;
        return "was killed by " + normalized;
    }

    private static String normalize(String reason) {
        return reason == null ? "" : reason.toLowerCase(Locale.ROOT).trim();
    }
}
