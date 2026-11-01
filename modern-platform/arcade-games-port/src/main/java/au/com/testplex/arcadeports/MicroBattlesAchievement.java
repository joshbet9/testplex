package au.com.testplex.arcadeports;

import java.util.UUID;

/** Local Micro Battles achievement, isolated from production Mineplex achievement storage. */
public enum MicroBattlesAchievement {
    ANNIHILATION("annihilation", "Annihilation", "Get 8 kills in one Micro Battles game."),
    LAST_TWO("last-two", "Last Two", "Be alive when exactly two teams remain in a Micro Battles game."),
    PHEW("phew", "PHEW!", "Build up to the border without dying to it."),
    MICRO_BATTLES_KING("micro-battles-king", "Micro Battles King",
            "Use a placed block to knock a jumper into lava or water and get 3 kills."),
    BUILT_FOR_THIS("built-for-this", "Built for This",
            "As Fighter, win the final 1v1 where another kit would have lost."),
    SNIPER_DAWG("sniper-dawg", "Sniper Dawg",
            "As Archer, knock a player from every opposing team off a skybase in one game.");

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