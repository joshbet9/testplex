package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reviewed achievement-kit prerequisites. Entries are stable IDs rather than a
 * moving count of catalogue achievements, so new challenges never revoke access.
 *
 * Runner's three older runtime awards retain their historical config keys while
 * appearing in the shared catalogue, so earned progress survives the migration.
 */
final class AchievementKitUnlockPolicy {
    record Rule(String gameId, String kitId, String displayName, List<String> requirements,
                boolean originalAllAchievements) {
        Rule {
            requirements = List.copyOf(requirements);
            if (requirements.isEmpty()) throw new IllegalArgumentException("Missing prerequisites for " + kitId);
        }

        boolean unlocked(Predicate<String> hasAchievement) {
            return requirements.stream().allMatch(hasAchievement);
        }

        List<String> missing(Predicate<String> hasAchievement) {
            return requirements.stream().filter(id -> !hasAchievement.test(id)).toList();
        }
    }

    private static final Map<String, Rule> RULES = createRules();

    private AchievementKitUnlockPolicy() { }

    static Rule find(ArcadePortGame game, String kitId) {
        if (game == null || kitId == null) return null;
        return RULES.get(gameId(game.id()) + ":" + kitId);
    }

    static String gameId(String gameId) {
        return gameId != null && gameId.startsWith("monster-maze") ? "monster-maze" : gameId;
    }

    static List<Rule> unlockedBy(String gameId, String achievementId) {
        if (gameId == null || achievementId == null) return List.of();
        List<Rule> found = new ArrayList<>();
        for (Rule rule : RULES.values()) {
            if (rule.gameId().equals(gameId(gameId)) && rule.requirements().contains(achievementId)) {
                found.add(rule);
            }
        }
        return List.copyOf(found);
    }

    static List<Rule> all() {
        return List.copyOf(RULES.values());
    }

    /**
     * Source-style achievement hover: display the reward before the description.
     * Multi-achievement gates still show that more than one feat is required.
     */
    static List<String> achievementHoverUnlocks(String gameId, String achievementId) {
        List<String> lines = new ArrayList<>();
        for (Rule rule : unlockedBy(gameId, achievementId)) {
            lines.add(ChatColor.GOLD + "Unlocks: " + ChatColor.WHITE + rule.displayName());
            if (rule.requirements().size() > 1) {
                lines.add(ChatColor.DARK_GRAY + "Requires " + rule.requirements().size()
                        + (rule.originalAllAchievements() ? " original achievements" : " achievements in total"));
            }
        }
        return List.copyOf(lines);
    }

    private static Map<String, Rule> createRules() {
        Map<String, Rule> rules = new LinkedHashMap<>();
        add(rules, "bacon-brawl", "brutus-bacon", "Brutus Bacon", "king-of-bacon", "pigs-in-blankets");
        // Both prerequisites are obtainable without first owning Chris P. Bacon.
        add(rules, "bacon-brawl", "chris-p-bacon", "Chris P. Bacon", "flash-fried", "raw-pork");
        // Reviewed four-achievement Constructor gate; all four can be earned without the kit.
        add(rules, "bomb-lobbers", "constructor", "Constructor", "two-birds-one-stone",
                "whats-a-double-jump", "blast-proof", "jelly-skin");
        add(rules, "death-tag", "runner-turtle", "Runner Turtle", "lots-of-subject-matter");
        add(rules, "death-tag", "runner-trickster", "Runner Trickster", "smoke-and-mirrors");
        add(rules, "dragon-escape", "digger", "Digger", "to-hell-and-back", "paralympics");
        add(rules, "micro-battles", "dasher", "Dasher", "speedster");
        add(rules, "sheep-quest", "shepherd", "Shepherd", "cheeky");
        add(rules, "monster-maze", "repulsor", "Repulsor", "blizzard", "stampede");
        add(rules, "monster-maze", "maverick", "Maverick", "pilot");
        add(rules, "one-in-the-quiver", "quiver-ninja", "Ninja", "sharpshooter", "the-perfect-game");
        add(rules, "paintball", "sniper", "Sniper", "share-your-screen");
        add(rules, "wither-assault", "warden", "Warden", "savior");
        add(rules, "sneaky-assassins", "briber", "Briber", "prosecutor", "idiot");
        add(rules, "spleef", "spleef-gust", "Gust", "praying-to-the-gods");
        add(rules, "turf-wars", "turf-wars-fortifier", "Fortifier", "genius-mathematician");
        add(rules, "super-sploor", "dualies", "Dualies", "reclaimer", "team-palette");
        add(rules, "plexball", "skyguard", "Skyguard", "sticky-gloves-three", "sweet-spot-75m");
        add(rules, "camera-shy", "snap-understudy", "Understudy", "camera-shy", "double-take");

        add(rules, "runner", "runnerkit-switcher", "Switcher",
                "long-leaper", "cold-front", "deadeye");
        return Map.copyOf(rules);
    }

    private static void add(Map<String, Rule> rules, String gameId, String kitId,
                            String displayName, String... achievements) {
        rules.put(gameId + ":" + kitId,
                new Rule(gameId, kitId, displayName, List.of(achievements), false));
    }
}
