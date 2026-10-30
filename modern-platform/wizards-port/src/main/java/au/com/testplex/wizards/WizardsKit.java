package au.com.testplex.wizards;

import java.util.Locale;

public enum WizardsKit {
    MAGE(100.0F, 2.5F, 2),
    SORCERER(100.0F, 2.5F, 3),
    MYSTIC(100.0F, 2.75F, 2),
    WITCH_DOCTOR(150.0F, 2.5F, 2);

    private final float maximumMana;
    private final float manaPerSecond;
    private final int startingWands;

    WizardsKit(float maximumMana, float manaPerSecond, int startingWands) {
        this.maximumMana = maximumMana;
        this.manaPerSecond = manaPerSecond;
        this.startingWands = startingWands;
    }

    public float maximumMana() { return maximumMana; }
    public float manaPerSecond() { return manaPerSecond; }
    public int startingWands() { return startingWands; }

    public static WizardsKit parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("WITCHDOCTOR".equals(normalized)) normalized = "WITCH_DOCTOR";
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
