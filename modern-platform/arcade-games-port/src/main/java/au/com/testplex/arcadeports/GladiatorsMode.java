package au.com.testplex.arcadeports;

import java.util.Locale;
import org.bukkit.Material;

/** Lobby-voted Gladiators combat rules. */
public enum GladiatorsMode {
    NORMAL("normal", "Classic", true, true),
    RODLESS("rodless", "Rodless", false, true),
    BOWLESS("bowless", "Bowless", true, false),
    MODERN("modern", "Axe", false, false),
    POT("pot", "Pot", false, false),
    MODERN_SWORD("modern-sword", "Modern Sword", false, false);

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
    public boolean usesModernCombat() { return this == MODERN || this == MODERN_SWORD; }
    public boolean usesAxeLoadout() { return this == MODERN; }
    public boolean hasCrossbow() { return this == MODERN; }
    public boolean hasShield() { return this == MODERN; }

    /** Weapon tiers follow the game's round progression; vanilla attributes give axes 7/9 damage. */
    public Material meleeWeaponForTier(int tier) {
        if (usesAxeLoadout()) {
            return switch (tier) {
                case 1 -> Material.STONE_AXE;
                case 2 -> Material.IRON_AXE;
                case 3 -> Material.DIAMOND_AXE;
                default -> Material.WOODEN_AXE;
            };
        }
        return switch (tier) {
            case 1 -> Material.STONE_SWORD;
            case 2 -> Material.IRON_SWORD;
            case 3 -> Material.DIAMOND_SWORD;
            default -> Material.WOODEN_SWORD;
        };
    }

    /** Modern uses the vanilla attack-speed base so sword/axe item modifiers drive cooldowns. */
    public double attackSpeedBaseValue() {
        return usesModernCombat() ? 4.0D : ArcadePortRules.LEGACY_ATTACK_SPEED;
    }

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
