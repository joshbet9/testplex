package au.com.testplex.arcadeports;

import org.bukkit.Material;

import java.util.UUID;

/** One-time Plexball challenges, saved only in the isolated Testplex test profile. */
public enum PlexballAchievement {
    SWEET_SPOT("sweet-spot", "Sweet Spot", "Land an uncaught hit of at least 50 metres.", Material.IRON_INGOT),
    WARNING_TRACK("warning-track", "Warning Track", "Land an uncaught hit of at least 90 metres.", Material.GOLD_INGOT),
    OUT_OF_THE_PARK("out-of-the-park", "Out of the Park", "Hit a fair 100-metre home run.", Material.FIREWORK_STAR),
    PERFECT_CONTACT("perfect-contact", "Perfect Contact", "Make contact with the ball using the top timing grade.", Material.DIAMOND),
    STICKY_GLOVES("sticky-gloves", "Sticky Gloves", "Make your first successful catch.", Material.LEATHER),
    GOLDEN_GLOVE("golden-glove", "Golden Glove", "Make at least three catches in one match.", Material.GOLDEN_CHESTPLATE),
    ALL_ROUNDER("all-rounder", "All-Rounder", "Make a catch and land an uncaught hit of at least 50 metres in one match.", Material.COMPASS),
    CLUTCH_PERFORMER("clutch-performer", "Clutch Performer", "Win a match after hitting a home run in sudden death.", Material.NETHER_STAR),
    CHAMPION("champion", "Champion", "Win a Plexball match.", Material.NETHER_STAR);

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
