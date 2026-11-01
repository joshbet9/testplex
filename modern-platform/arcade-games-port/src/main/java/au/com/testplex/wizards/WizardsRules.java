package au.com.testplex.wizards;

import org.bukkit.Material;

import java.util.List;

public final class WizardsRules {
    public static final float BASE_MANA_PER_SECOND = 2.5F;
    public static final float SOUL_STAR_MANA_PER_SECOND = 0.2F;
    public static final int WAND_SLOT_COUNT = 5;
    public static final int MAGE_RANDOM_STARTER_SPELL_COUNT = 2;
    public static final int MAGE_STARTER_SPELL_SELECTION_ATTEMPTS = 100;
    public static final int BASE_MANA_BOOK_CONVERSION = 50;
    public static final int FULL_WAND_CONVERSION = 100;
    public static final int MIN_PLAYERS = 8;
    public static final int MAX_PLAYERS = 12;
    public static final int[] GAME_PACE_MINUTES = {4, 5, 6, 7, 8, 9};
    public static final int ENDGAME_START_MINUTES = 10;
    public static final int ENDGAME_INITIAL_STRIKE_MILLIS = 6_000;
    public static final int ENDGAME_MIN_STRIKE_MILLIS = 750;
    public static final int ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS = 2;

    private WizardsRules() {}

    public static float manaPerSecond(WizardsKit kit, int soulStars) {
        if (kit == null) throw new IllegalArgumentException("kit is required");
        return kit.manaPerSecond() + Math.max(0, soulStars) * SOUL_STAR_MANA_PER_SECOND;
    }

    public static float initialMana() { return 0.0F; }

    public static double capMeleeDamage(double incomingDamage) {
        if (!Double.isFinite(incomingDamage) || incomingDamage < 0.0D) return 0.0D;
        return Math.min(incomingDamage, 0.5D);
    }

    /** Original Mineplex protection for a single equipped armour piece. */
    public static double armourPieceProtection(Material piece) {
        if (piece == null) return 0.0D;
        String name = piece.name();
        double protection;
        if (name.contains("LEATHER")) protection = 0.10D;
        else if (name.contains("GOLD") || name.contains("CHAINMAIL")) protection = 0.15D;
        else if (name.contains("IRON")) protection = 0.19D;
        else if (name.contains("DIAMOND")) protection = 0.25D;
        else return 0.0D;

        if (name.endsWith("_BOOTS")) protection /= 3.0D;
        else if (name.endsWith("_LEGGINGS")) protection /= 1.5D;
        else if (name.endsWith("_CHESTPLATE")) { /* full piece value */ }
        else if (name.endsWith("_HELMET")) protection /= 2.0D;
        else return 0.0D;
        return protection;
    }

    /** Adds the four original piece-specific percentages, without vanilla armour points. */
    public static double armorProtection(Material... pieces) {
        if (pieces == null) return 0.0D;
        double protection = 0.0D;
        for (Material piece : pieces) protection += armourPieceProtection(piece);
        return Math.max(0.0D, Math.min(1.0D, protection));
    }

    public static List<WizardsSpell> initialKnownSpells() {
        return List.of(WizardsSpell.MANA_BOLT, WizardsSpell.WIZARDS_COMPASS);
    }

    /** Original Mage starter selection: random SpellType, then roll 0-9 against its loot weight. */
    public static boolean mageStarterSpellRollSucceeds(WizardsKit kit, int rarityRoll, int lootWeight) {
        return kit == WizardsKit.MAGE && rarityRoll >= 0 && rarityRoll < 10 && rarityRoll < lootWeight;
    }

    public static int summonedWolfCount(int spellLevel) {
        return Math.max(0, spellLevel) + 2;
    }

    public static int webShotCount(int spellLevel) {
        return Math.max(0, spellLevel) * 2;
    }

    public static int trapRuneRange(int spellLevel) {
        return Math.max(0, spellLevel) * 4 + 12;
    }

    public static double trapRuneSize(int spellLevel) {
        return Math.max(0, spellLevel);
    }

    public static double trapRuneDamage(int spellLevel) {
        return Math.max(0, spellLevel) * 2.0D + 3.0D;
    }

    public static double spectralArrowDamage(int spellLevel, double travelledBlocks) {
        if (!Double.isFinite(travelledBlocks) || travelledBlocks < 0) return 0.0D;
        int level = Math.max(1, Math.min(3, spellLevel));
        return 3.0D + travelledBlocks / (7.0D - level);
    }

    public static double manaBoltDamage(int spellLevel) {
        return Math.max(0, spellLevel) + 2.0D;
    }

    public static double fireballDamage(int spellLevel) {
        return Math.max(0, spellLevel) + 3.0D;
    }

    public static double lightningStrikeDamage(int spellLevel) {
        return Math.max(0, spellLevel) * 2.0D + 2.0D;
    }

    public static double healAmount(int spellLevel) {
        return Math.max(0, spellLevel) / 2.0D + 1.5D;
    }

    public static double rainbowBeamDamage(int spellLevel, double travelledBlocks) {
        if (!Double.isFinite(travelledBlocks) || travelledBlocks < 0) return 0.0D;
        return Math.max(0.0D, Math.max(0, spellLevel) + 2.0D
                - Math.max(0.0D, travelledBlocks - 30.0D) * 0.2D);
    }

    public static double rumbleDamage(int spellLevel) {
        return Math.max(0, spellLevel) + 2.0D;
    }

    public static double rumbleExplosionDamage(int spellLevel) {
        return Math.max(0, spellLevel) / 4.0D;
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
        return List.of(4, 5, 6, 7, 8, 9);
    }
}
