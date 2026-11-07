package au.com.testplex.arcadeports;

import java.util.UUID;

/** Local Micro Battles achievement, isolated from production Mineplex achievement storage. */
public enum MicroBattlesAchievement {
    ANNIHILATION("annihilation", "Annihilation", "Get 8 kills in one Micro Battles game."),
    LAST_TWO("last-two", "Last Two", "Be alive when two teams remain, eliminate every player on the other team, and win."),
    PHEW("phew", "PHEW!", "Place 3 blocks within 2 blocks of the border in one game without dying to it."),
    MICRO_BATTLES_KING("micro-battles-king", "Micro Battles King",
            "Use a placed block to knock a jumper into lava or water and get 3 kills."),
    BUILT_FOR_THIS("built-for-this", "Built for This",
            "As Fighter, win the final 1v1 after Iron Skin prevents a lethal hit."),
    SPEEDSTER("speedster", "Speedster", "Win a Micro Battles game in under 1:30. Unlocks the Dasher kit."),
    SNIPER_DAWG("sniper-dawg", "Sniper Dawg",
            "As Archer, knock a player from every opposing team off a skybase in one game."),
    MOSH_PIT_PLAYROOM("mosh-pit-playroom", "Mosh-Pit Playroom", "Get 8 assists in one game."),
    GRAVEDIGGER("gravedigger", "Gravedigger", "Get a melee kill with a shovel."),
    AIR_MAIL("air-mail", "Air Mail",
            "As Dasher, double-jump across a gap, land on an enemy island, and eliminate an enemy there."),
    NEEDLE_THREADER("needle-threader", "Needle Threader",
            "As Archer, eliminate an opponent with an arrow from at least 25 blocks away."),
    SHIFT_SUPERVISOR("shift-supervisor", "Shift Supervisor",
            "As Worker, break 30 blocks, place 30 blocks, and win the game."),
    NO_ROOM_TO_BUILD("no-room-to-build", "No Room to Build", "Win a game without placing any blocks."),
    LAST_BREATH("last-breath", "Last Breath", "Win the final 1v1 with 1 heart or less.");

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
