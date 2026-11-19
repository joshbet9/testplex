package au.com.testplex.arcadeports;

import java.util.UUID;

/**
 * Local Bacon Brawl achievements shown as bottles in the modern /stats game page.
 * Keys are deliberately namespaced to the isolated Testplex config, not production Mineplex data.
 */
public enum BaconBrawlAchievement {
    WELL_DONE("king-of-bacon", "Well Done", "Win Bacon Brawl with at least 3 credited kills, and have every credited kill be a critical hit."),
    PIGS_IN_BLANKETS("pigs-in-blankets", "Pigs In Blankets", "Kill 6 players in one game."),
    FLASH_FRIED("flash-fried", "Flash Fried", "Win Bacon Brawl in 60 seconds or less."),
    RAW_PORK("raw-pork", "Raw Pork", "Win Bacon Brawl without using an ability."),
    THREE_FOR_ONE("three-for-one", "Three for One!", "Get 3 Body Slam kills in one game.");

    private final String slug;
    private final String displayName;
    private final String requirement;

    BaconBrawlAchievement(String slug, String displayName, String requirement) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }

    public String configKey(UUID playerId) {
        return ArcadePortRules.baconBrawlAchievementConfigKey(playerId, slug);
    }
}
