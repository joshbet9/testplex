package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.UUID;

/** One-time Plexball challenges, saved only in the isolated Testplex test profile. */
public enum PlexballAchievement {
    SWEET_SPOT("sweet-spot-75m", "Sweet Spot",
            "Land an uncaught hit of at least 75 metres.", Material.IRON_INGOT),
    WARNING_TRACK("warning-track-95m", "Warning Track",
            "Land an uncaught hit of at least 95 metres.", Material.GOLD_INGOT),
    OUT_OF_THE_PARK("out-of-the-park", "Out of the Park",
            "Hit a fair 100-metre home run.", Material.FIREWORK_STAR),
    PERFECT_CONTACT("perfect-contact-three", "Perfect Contact",
            "Record three top-grade contacts across your Plexball career.", Material.DIAMOND),
    STICKY_GLOVES("sticky-gloves-three", "Sticky Gloves",
            "Make three successful catches in a row in one match.", Material.LEATHER),
    GOLDEN_GLOVE("golden-glove-five", "Golden Glove",
            "Make at least five successful catches in one match.", Material.GOLDEN_CHESTPLATE),
    ALL_ROUNDER("all-rounder-75m", "All-Rounder",
            "Make a catch and land an uncaught hit of at least 75 metres in one match.", Material.COMPASS),
    CLUTCH_PERFORMER("clutch-double-homer", "Clutch Performer",
            "Win a match after hitting at least two home runs in sudden death.", Material.NETHER_STAR),
    CHAMPION("champion-five-win-streak", "Champion",
            "Win five Plexball matches in a row.", Material.NETHER_STAR),
    WALK_OFF_POWER_DIVE("walk-off-power-dive", "Walk-Off Power Dive",
            "As Slugger, hit the fair home run that wins the match while Power Dive is active.", Material.DIAMOND_SWORD),
    SURE_HANDS("sure-hands", "Sure Hands",
            "As Technician, catch three consecutive balls with Sure Hands and win the match.", Material.SHIELD),
    BEND_IT_IN("bend-it-in", "Bend It In",
            "As Curve Artist, land three fair curved hits in one match and win.", Material.BOW),
    FIRST_TO_HOME("first-to-home", "First to Home",
            "As Sprinter, activate Burst and hit two home runs in one match, then win.", Material.RABBIT_FOOT),
    HIGH_POINT("high-point", "High Point",
            "As Skyguard, catch three balls at least two blocks above their release height and win.", Material.ELYTRA);

    private final String slug;
    private final String displayName;
    private final String requirement;
    private final Material icon;

    PlexballAchievement(String slug, String displayName, String requirement, Material icon) {
        this.slug = slug;
        this.displayName = displayName;
        this.requirement = requirement;
        this.icon = icon;
    }

    public String slug() { return slug; }
    public String displayName() { return displayName; }
    public String requirement() { return requirement; }
    public Material icon() { return icon; }

    public String configPath(UUID playerId) {
        return playerId == null ? "" : "players." + playerId + ".achievements." + slug;
    }
}
