package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WizardsRulesTest {
    @Test
    void sourceKitManaAndWandProfilesArePreserved() {
        assertEquals(100.0F, WizardsRules.maximumMana(WizardsKit.MAGE));
        assertEquals(100.0F, WizardsRules.maximumMana(WizardsKit.SORCERER));
        assertEquals(100.0F, WizardsRules.maximumMana(WizardsKit.MYSTIC));
        assertEquals(150.0F, WizardsRules.maximumMana(WizardsKit.WITCH_DOCTOR));
        assertEquals(2, WizardsRules.startingWands(WizardsKit.MAGE));
        assertEquals(3, WizardsRules.startingWands(WizardsKit.SORCERER));
        assertEquals(2, WizardsRules.startingWands(WizardsKit.MYSTIC));
        assertEquals(2, WizardsRules.startingWands(WizardsKit.WITCH_DOCTOR));
    }

    @Test
    void mysticAndSoulStarsScaleManaRegeneration() {
        assertEquals(2.5F, WizardsRules.manaPerSecond(WizardsKit.MAGE, 0), 0.0001F);
        assertEquals(2.75F, WizardsRules.manaPerSecond(WizardsKit.MYSTIC, 0), 0.0001F);
        assertEquals(2.9F, WizardsRules.manaPerSecond(WizardsKit.MAGE, 2), 0.0001F);
        assertEquals(3.15F, WizardsRules.manaPerSecond(WizardsKit.MYSTIC, 2), 0.0001F);
    }

    @Test
    void spellLevelsStopAtTheSourceMaximumAndDuplicatesConvert() {
        assertEquals(1, WizardsRules.nextSpellLevel(0, 3));
        assertEquals(2, WizardsRules.nextSpellLevel(1, 3));
        assertEquals(3, WizardsRules.nextSpellLevel(3, 3));
        assertFalse(WizardsRules.spellNeedsManaConversion(2, 3));
        assertTrue(WizardsRules.spellNeedsManaConversion(3, 3));
        assertEquals(50, WizardsRules.BASE_MANA_BOOK_CONVERSION);
    }

    @Test
    void wandPickupIsAllowedUntilAllFiveWandSlotsAreFilled() {
        assertTrue(WizardsRules.canGainWand(2));
        assertTrue(WizardsRules.canGainWand(4));
        assertFalse(WizardsRules.canGainWand(5));
        assertFalse(WizardsRules.canGainWand(-1));
        assertEquals(100, WizardsRules.FULL_WAND_CONVERSION);
    }

    @Test
    void manaIsClampedAndProgressAlwaysStaysInsideBossBarRange() {
        assertEquals(100.0F, WizardsRules.addMana(90.0F, 50.0F, 100.0F));
        assertEquals(0.0F, WizardsRules.addMana(10.0F, -50.0F, 100.0F));
        assertEquals(1.0D, WizardsRules.manaProgress(150.0F, 100.0F));
        assertEquals(0.0D, WizardsRules.manaProgress(-5.0F, 100.0F));
        assertEquals(0.0D, WizardsRules.manaProgress(10.0F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> WizardsRules.addMana(Float.NaN, 1, 100));
    }

    @Test
    void gamePaceSurgesFollowTheHistoricalMinuteMilestones() {
        assertEquals(java.util.List.of(2, 4, 6, 8, 9), WizardsRules.gamePaceMinutes());
    }
}
