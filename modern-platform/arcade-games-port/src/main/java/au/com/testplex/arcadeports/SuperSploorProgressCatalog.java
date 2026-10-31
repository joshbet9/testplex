package au.com.testplex.arcadeports;

import java.util.List;
import java.util.UUID;

/** Super Sploor achievements and lifetime milestones recorded by the local Arcade stats store. */
public final class SuperSploorProgressCatalog {
    public record Achievement(String id, String name, String requirement) { }
    public record Milestone(String id, String name, String statKey, long maximum,
                            String unit, List<Long> explicitStages) {
        public Milestone {
            explicitStages = List.copyOf(explicitStages);
            if (explicitStages.size() != 4) {
                throw new IllegalArgumentException("Super Sploor milestones require exactly four stages: " + id);
            }
        }
        public long[] stages() { return explicitStages.stream().mapToLong(Long::longValue).toArray(); }
    }
    private static final List<Achievement> ACHIEVEMENTS = List.of(
            a("first-splash", "First Splash", "Complete your first Super Sploor match."),
            a("fresh-canvas", "Fresh Canvas", "Claim 500 previously neutral territory units in one match."),
            a("reclaimer", "Reclaimer", "Reclaim 200 enemy-owned territory units in one match."),
            a("squid-express", "Squid Express", "Travel 500 blocks in Squid Mode during one match."),
            a("wall-to-wall", "Wall to Wall", "Climb 25 vertical blocks on friendly-painted walls in one match."),
            a("team-palette", "Team Palette", "Earn 5 assists in one match."),
            a("close-shave", "Close Shave", "Win a match by 2 percentage points or less."),
            a("clean-sweep", "Clean Sweep", "Win a match without being splatted."),
            a("colour-outside-the-lines", "Colour Outside the Lines",
                    "As Sprayer, claim 350 neutral territory units in one match."),
            a("road-roller", "Road Roller",
                    "As Roller, paint 500 territory units using the rolling path in one match."),
            a("needle-threader", "Needle Threader",
                    "As Charger, land 5 fully charged primary hits on opponents in one match."),
            a("splash-zone", "Splash Zone", "As Lobber, reclaim 150 enemy-owned territory units in one match."),
            a("blink-and-youll-miss-it", "Blink and You'll Miss It",
                    "As Dualies, splat 3 different opponents within 5 seconds of a Dodge Roll in one match."),
            a("special-delivery", "Special Delivery", "Mark 3 different opponents with one special activation."),
            a("master-of-the-palette", "Master of the Palette",
                    "Win at least one match using each of the five kits.")
    );
    private static final List<Milestone> MILESTONES = List.of(
            m("wins", "Turf War Wins", "wins", 250, "wins", 10, 25, 100, 250),
            m("territory-painted", "Territory Painted", "territory-blocks-painted",
                    250_000, "blocks", 1_000, 10_000, 50_000, 250_000),
            m("enemy-territory", "Enemy Turf Reclaimed", "enemy-blocks-reclaimed",
                    50_000, "blocks", 250, 2_000, 10_000, 50_000),
            m("splats", "Opponents Splatted", "splats", 1_000, "splats", 10, 50, 250, 1_000),
            m("assists", "Team Assists", "assists", 500, "assists", 5, 25, 100, 500),
            m("squid-travel", "Squid Travel", "squid-distance-blocks",
                    100_000, "blocks", 250, 2_500, 25_000, 100_000),
            m("specials", "Specials Activated", "specials-used", 250, "specials", 5, 25, 100, 250)
    );
    private SuperSploorProgressCatalog() { }
    public static List<Achievement> achievements() { return ACHIEVEMENTS; }
    public static List<Milestone> milestones() { return MILESTONES; }
    public static Achievement achievement(String id) {
        if (id == null) return null;
        return ACHIEVEMENTS.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }
    public static String achievementConfigKey(UUID playerId, String achievementId) {
        if (playerId == null || achievement(achievementId) == null) return "";
        return "local-achievements." + playerId + ".super-sploor." + achievementId;
    }
    private static Achievement a(String id, String name, String requirement) {
        return new Achievement(id, name, requirement);
    }
    private static Milestone m(String id, String name, String statKey, long maximum,
                               String unit, long... stages) {
        return new Milestone(id, name, statKey, maximum, unit,
                java.util.Arrays.stream(stages).boxed().toList());
    }
}