package au.com.testplex.wizards;

import java.util.List;

public final class WizardsRules {
    public static final float BASE_MANA_PER_SECOND = 2.5F;
    public static final float SOUL_STAR_MANA_PER_SECOND = 0.2F;
    public static final int WAND_SLOT_COUNT = 5;
    public static final int BASE_MANA_BOOK_CONVERSION = 50;
    public static final int FULL_WAND_CONVERSION = 100;
    public static final int GAME_PACE_MINUTES[] = {2, 4, 6, 8, 9};

    private WizardsRules() {}

    public static float manaPerSecond(WizardsKit kit, int soulStars) {
        if (kit == null) throw new IllegalArgumentException("kit is required");
        return kit.manaPerSecond() + Math.max(0, soulStars) * SOUL_STAR_MANA_PER_SECOND;
    }

    public static float initialMana() { return 0.0F; }

    public static float maximumMana(WizardsKit kit) {
        if (kit == null) throw new IllegalArgumentException("kit is required");
        return kit.maximumMana();
    }

    public static int startingWands(WizardsKit kit) {
        if (kit == null) throw new IllegalArgumentException("kit is required");
        return kit.startingWands();
    }

    public static int nextSpellLevel(int currentLevel, int maximumLevel) {
        if (maximumLevel < 1) throw new IllegalArgumentException("maximumLevel must be positive");
        return Math.min(maximumLevel, Math.max(0, currentLevel) + 1);
    }

    public static boolean spellNeedsManaConversion(int currentLevel, int maximumLevel) {
        return currentLevel >= maximumLevel;
    }

    public static boolean canGainWand(int currentWands) {
        return currentWands >= 0 && currentWands < WAND_SLOT_COUNT;
    }

    public static float addMana(float current, float amount, float maximum) {
        if (!Float.isFinite(current) || !Float.isFinite(amount) || !Float.isFinite(maximum) || maximum < 0) {
            throw new IllegalArgumentException("Mana values must be finite and maximum must be non-negative");
        }
        return Math.max(0.0F, Math.min(maximum, current + amount));
    }

    public static double manaProgress(float mana, float maximum) {
        if (maximum <= 0 || !Float.isFinite(mana) || !Float.isFinite(maximum)) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, mana / maximum));
    }

    public static List<Integer> gamePaceMinutes() {
        return List.of(2, 4, 6, 8, 9);
    }
}
