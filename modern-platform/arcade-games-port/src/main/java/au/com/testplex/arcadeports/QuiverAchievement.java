package au.com.testplex.arcadeports;

import java.util.UUID;

/** One in the Quiver achievements exposed by the isolated modern test server. */
public enum QuiverAchievement {
    THE_PERFECT_GAME("the-perfect-game", "The Perfect Game",
            "Achieve a Perfect Run killstreak in a game."),
    SHARPSHOOTER("sharpshooter", "Sharpshooter",
            "Land 8 arrow hits without missing."),
    WHATS_A_BOW("whats-a-bow", "What's A bow?",
            "Win a game by getting 20 kills without using archery."),
    SPEEDRUNNER("speedrunner", "Speedrunner",
            "Win a game in under 1:30."),
    THATS_NOT_A_SWORD("thats-not-a-sword", "That's not a sword…",
            "Get 3 kills with fist/bow punch."),
    HEADS("heads", "HEADS!", "Kill a player with a high-arced bow shot."),
    RHYTHM_HEAVEN("rhythm-heaven", "Rhythm Heaven", "Win with 10 separate two-kill streaks."),
    DOUBLE_TAKE("double-take", "Double Take", "As Jumper, kill a player with an arrow while your double jump is still carrying you through the air."),
    IRON_RESOLVE("iron-resolve", "Iron Resolve", "As Brawler, get 10 melee kills in one game."),
    SLAM_DUNK("slam-dunk", "Slam Dunk", "As Slam Shot, eliminate two players with one Ground Pound."),
    VANISHING_ACT("vanishing-act", "Vanishing Act", "As Ninja, get 3 kills while Vanish is active in one game."),
    REBOUND_REMARKABLE("rebound-remarkable", "Rebound Remarkable", "As Enchanter, get 3 kills with rebound arrows in one game."),
    QUIVER_JUGGLER("quiver-juggler", "Quiver Juggler", "Win a game with 10 bow kills and 10 melee kills."),
    PHOENIX_RISING("phoenix-rising", "Phoenix Rising", "After dying, win the game with a 15-kill streak.");

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
