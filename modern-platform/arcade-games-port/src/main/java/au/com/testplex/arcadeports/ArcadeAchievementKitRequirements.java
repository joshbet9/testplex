package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;

/**
 * Read-only kit requirement descriptions for menus. The achievement-kit audit
 * owns enforcement and can extend these game-specific requirements later.
 */
final class ArcadeAchievementKitRequirements {
    record Requirement(boolean allAchievements, List<String> achievementIds) {
        Requirement { achievementIds = List.copyOf(achievementIds); }
    }

    private static final Map<String, Requirement> KNOWN = Map.ofEntries(
            one("death-tag", "runner-turtle", "turtle-shell"),
            one("death-tag", "runner-trickster", "smoke-and-mirrors"),
            one("dragons", "tamer", "dragon-tamer"),
            one("evolution", "health-harvester", "second-wind"),
            one("evolution", "neanderthal", "pity-party"),
            one("sheep-quest", "shepherd", "cheeky"),
            one("wither-assault", "warden", "savior"),
            one("micro-battles", "dasher", "speedster"),
            one("spleef", "spleef-gust", "praying-to-the-gods"),
            one("turf-wars", "turf-wars-fortifier", "genius-mathematician"),
            Map.entry("runner/runnerkit-switcher", new Requirement(true, List.of()))
    );

    private static Map.Entry<String, Requirement> one(String game, String kit, String achievement) {
        return Map.entry(game + "/" + kit, new Requirement(false, List.of(achievement)));
    }

    static Requirement find(String game, String kit) {
        return KNOWN.get(game + "/" + kit);
    }

    /** Only confirmed relationships are advertised as achievement rewards. */
    static List<String> kitIdsForAchievement(String game, String achievement) {
        return KNOWN.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(game + "/"))
                .filter(entry -> entry.getValue().allAchievements()
                        || entry.getValue().achievementIds().contains(achievement))
                .map(entry -> entry.getKey().substring(game.length() + 1))
                .sorted()
                .toList();
    }

    private ArcadeAchievementKitRequirements() { }
}
