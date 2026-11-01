package au.com.testplex.arcadeports;

import java.util.UUID;

/** Evolution achievements in the isolated modern test server. */
public enum EvolutionAchievement {
    OVERKILL("overkill", "Overkill", "Get 2 kills before each evolution."),
    WRONG_KIT_CHAMP("wrong-kit-champ", "Wrong Kit Champ",
            "Lose while your final standard evolution is still transforming and Quick Evolver would have finished it in time.");

    private final String slug;
    private final String displayName;
    private final String requirement;

    EvolutionAchievement(String slug, String displayName, String requirement) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }

    public String configKey(UUID playerId) {
        return "local-achievements." + playerId + ".evolution." + slug;
    }
}
