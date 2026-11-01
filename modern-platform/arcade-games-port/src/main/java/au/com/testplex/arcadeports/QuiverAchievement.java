package au.com.testplex.arcadeports;

import java.util.UUID;

/** One in the Quiver achievements exposed by the isolated modern test server. */
public enum QuiverAchievement {
    THE_PERFECT_GAME("the-perfect-game", "The Perfect Game",
            "Achieve a Perfect Run killstreak in a game."),
    WHATS_A_BOW("whats-a-bow", "What's A bow?",
            "Win a game by getting 20 kills without using archery."),
    SPEEDRUNNER("speedrunner", "Speedrunner",
            "Win a game in under 1:30."),
    THATS_NOT_A_SWORD("thats-not-a-sword", "That's not a sword…",
            "Get 3 kills with fist/bow punch."),
    HEADS("heads", "HEADS!", "Kill a player with a high-arced bow shot."),
    RHYTHM_HEAVEN("rhythm-heaven", "Rhythm Heaven", "Win with 10 separate two-kill streaks.");

    private final String slug;
    private final String displayName;
    private final String requirement;

    QuiverAchievement(String slug, String displayName, String requirement) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }

    public String configKey(UUID playerId) {
        return playerId == null ? "" : "local-achievements." + playerId + ".one-in-the-quiver." + slug;
    }
}
