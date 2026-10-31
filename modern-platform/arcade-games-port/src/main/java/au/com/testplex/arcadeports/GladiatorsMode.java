package au.com.testplex.arcadeports;

import java.util.Locale;

/** Lobby-voted Gladiators combat rules. */
public enum GladiatorsMode {
    NORMAL("normal", "Normal", true, true),
    RODLESS("rodless", "Rodless", false, true),
    BOWLESS("bowless", "Bowless", true, false),
    MODERN("modern", "Modern", false, false),
    POT("pot", "Pot", false, false);

    private final String id;
    private final String displayName;
    private final boolean rodAvailable;
    private final boolean bowAvailable;

    GladiatorsMode(String id, String displayName, boolean rodAvailable, boolean bowAvailable) {
        this.id = id;
        this.displayName = displayName;
        this.rodAvailable = rodAvailable;
        this.bowAvailable = bowAvailable;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public boolean rodAvailable() { return rodAvailable; }
    public boolean bowAvailable() { return bowAvailable; }
    public boolean hasClassicRangedTools() { return rodAvailable || bowAvailable; }

    /** The untouched Reborn-style loadout is the default when nobody votes or in solo practice. */
    public static GladiatorsMode defaultMode() { return NORMAL; }

    public static GladiatorsMode fromId(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(Locale.ROOT).trim();
        if ("classic".equals(normalized)) return NORMAL;
        for (GladiatorsMode mode : values()) if (mode.id.equals(normalized)) return mode;
        return null;
    }
}
