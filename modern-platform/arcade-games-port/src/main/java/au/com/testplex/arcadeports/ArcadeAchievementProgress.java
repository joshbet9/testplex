package au.com.testplex.arcadeports;

import java.util.Map;

/**
 * Only persistent, achievement-specific counters belong here. One-round feats
 * deliberately have no progress bar: lifetime kills cannot stand in for kills
 * in a single match, and a previously earned unlock must never be recalculated.
 */
final class ArcadeAchievementProgress {
    record Counter(String statKey, long target, String unit) {
        Counter {
            if (statKey == null || statKey.isBlank() || target <= 0) throw new IllegalArgumentException();
        }
    }

    private static final Map<String, Counter> COUNTERS = Map.ofEntries(
            entry("bacon-brawl", "king-of-bacon", "wins", 15, "wins"),
            entry("bomb-lobbers", "master-bomber", "wins", 30, "wins"),
            entry("death-tag", "death-proof", "wins", 50, "wins"),
            entry("death-tag", "catch-me-if-you-can", "run-distance", 10_000, "blocks"),
            entry("death-tag", "resilient-runner", "basher-hearts-regenerated", 250, "hearts"),
            entry("dragon-escape", "douglas-defeater", "wins", 50, "wins"),
            entry("dragon-escape", "skylands-master", "map-wins.skylands", 5, "finishes"),
            entry("dragon-escape", "to-hell-and-back", "map-wins.through-hell", 5, "finishes"),
            entry("dragon-escape", "plundered", "map-wins.pirate-bay", 5, "finishes"),
            entry("dragons", "win-three-in-a-row", "win-streak", 3, "consecutive wins"),
            entry("sheep-quest", "hungry-hungry-hippo", "wins", 50, "wins"),
            entry("sheep-quest", "thief", "thief", 300, "sheep"),
            entry("sheep-quest", "animal-rescue", "animal-rescue", 300, "rescues")
    );

    private static Map.Entry<String, Counter> entry(String game, String achievement,
                                                    String stat, long target, String unit) {
        return Map.entry(game + "/" + achievement, new Counter(stat, target, unit));
    }

    static Counter counter(String game, String achievement) {
        return COUNTERS.get(game + "/" + achievement);
    }

    private ArcadeAchievementProgress() { }
}
