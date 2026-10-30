package au.com.testplex.arcadeports;

import java.util.UUID;

/** Local Micro Battles achievement, isolated from production Mineplex achievement storage. */
public enum MicroBattlesAchievement {
    ANNIHILATION("annihilation", "Annihilation", "Get 8 kills in one Micro Battles game."),
    LAST_TWO("last-two", "Last Two", "Be alive when exactly two teams remain in a Micro Battles game.");

    private final String slug;
    private final String displayName;
    private final String requirement;

    MicroBattlesAchievement(String slug, String displayName, String requirement) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }

    public String configKey(UUID playerId) {
        return ArcadePortRules.microBattlesAchievementConfigKey(playerId, slug);
    }
}
