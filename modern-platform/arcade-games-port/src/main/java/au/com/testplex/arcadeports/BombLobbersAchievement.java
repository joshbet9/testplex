package au.com.testplex.arcadeports;

import java.util.UUID;

/** Bomb Lobbers achievements, stored only in this isolated modern test server. */
public enum BombLobbersAchievement {
    ULTIMATE_KILLER("ultimate-killer", "Ultimate Killer", "Get 5 kills in one game."),
    TWO_BIRDS_ONE_STONE("two-birds-one-stone", "Two birds, one stone", "Kill 2 players with one TNT."),
    TEAM_PLAYER("team-player", "Team player!", "Get 5 assists in one game."),
    WHATS_A_DOUBLE_JUMP("whats-a-double-jump", "What's a double jump?", "Win without using double jump while alive."),
    THE_MVP("the-mvp", "The MVP", "Win against 3 opponents while the last living member of your team."),
    BLAST_PROOF("blast-proof", "Blast Proof", "Win a game as the Armorer."),
    JELLY_SKIN("jelly-skin", "Jelly Skin", "Win a game without taking damage."),
    MASTER_BOMBER("master-bomber", "Master Bomber", "Win 30 Bomb Lobbers games."),
    GRENADE_FROM_THE_GRAVE("grenade-from-the-grave", "Grenade From the Grave",
            "Get 2 kills after you die in one Bomb Lobbers game.");

    private final String slug;
    private final String displayName;
    private final String requirement;

    BombLobbersAchievement(String slug, String displayName, String requirement) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }
    public String configKey(UUID playerId) {
        return ArcadePortRules.bombLobbersAchievementConfigKey(playerId, slug);
    }

    public static boolean shouldAwardGrenadeFromTheGrave(int postDeathKills) {
        return postDeathKills >= 2;
    }
}
