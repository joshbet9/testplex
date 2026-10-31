package au.com.testplex.wizards;

import java.util.List;

public final class WizardsRules {
    public static final float BASE_MANA_PER_SECOND = 2.5F;
    public static final float SOUL_STAR_MANA_PER_SECOND = 0.2F;
    public static final int WAND_SLOT_COUNT = 5;
    public static final int MAGE_RANDOM_STARTER_SPELL_COUNT = 2;
    public static final int MAGE_STARTER_SPELL_SELECTION_ATTEMPTS = 100;
    public static final int BASE_MANA_BOOK_CONVERSION = 50;
    public static final int FULL_WAND_CONVERSION = 100;
    public static final int GAME_PACE_MINUTES[] = {2, 4, 6, 8, 9};
    public static final int ENDGAME_START_MINUTES = 10;
    public static final int SUDDEN_DEATH_MINUTES = 20;
    public static final int ENDGAME_INITIAL_STRIKE_MILLIS = 6_000;
    public static final int ENDGAME_MIN_STRIKE_MILLIS = 750;
    public static final int ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS = 2;

    private WizardsRules() {}

    public static float manaPerSecond(WizardsKit kit, int soulStars) {
        if (kit == null) throw new IllegalArgumentException("kit is required");
        return kit.manaPerSecond() + Math.max(0, soulStars) * SOUL_STAR_MANA_PER_SECOND;
    }

    public static float initialMana() { return 0.0F; }

    public static List<WizardsSpell> initialKnownSpells() {
        return List.of(WizardsSpell.MANA_BOLT, WizardsSpell.WIZARDS_COMPASS);
    }

    /** Original Mage starter selection: random SpellType, then roll 0-9 against its loot weight. */
    public static boolean mageStarterSpellRollSucceeds(WizardsKit kit, int rarityRoll, int lootWeight) {
        return kit == WizardsKit.MAGE && rarityRoll >= 0 && rarityRoll < 10 && rarityRoll < lootWeight;
    }

    public static int summonedWolfCount(int spellLevel) {
        return Math.max(0, spellLevel);
    }

    public static int webShotCount(int spellLevel) {
        return Math.max(0, spellLevel) * 2;
    }

    public static int trapRuneRange(int spellLevel) {
        return Math.max(0, spellLevel) * 4 + 4;
    }

    public static double trapRuneSize(int spellLevel) {
        return Math.max(1.0D, Math.max(0, spellLevel) * 0.8D);
    }

    public static double trapRuneDamage(int spellLevel) {
        return Math.max(0, spellLevel) * 4.0D + 6.0D;
    }

    public static double spectralArrowDamage(int spellLevel, double travelledBlocks) {
        if (!Double.isFinite(travelledBlocks) || travelledBlocks < 0) return 0.0D;
        return 6.0D + travelledBlocks / Math.max(1.0D, 7.0D - Math.max(1, spellLevel));
    }

    public static double manaBoltVelocity() { return 0.3D; }
    public static double fireballVelocity() { return 0.14D; }

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

    public static int nextEndgameStrikeInterval(int currentMillis) {
        return Math.max(ENDGAME_MIN_STRIKE_MILLIS,
                currentMillis - ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS);
    }

    public static double endgameDamage(double size, double distance) {
        if (!Double.isFinite(size) || !Double.isFinite(distance) || size <= 0 || distance < 0) return 0.0D;
        double radius = 4.0D * size;
        if (distance >= radius) return 0.0D;
        return 6.0D * size * (1.0D - distance / radius);
    }

    public static List<Integer> gamePaceMinutes() {
        return List.of(2, 4, 6, 8, 9);
    }
}
