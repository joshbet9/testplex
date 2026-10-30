package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WizardsRulesTest {
    @Test
    void sourceKitManaAndWandProfilesArePreserved() {
        assertEquals(0.0F, WizardsRules.initialMana());
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
    void originalStarterSpellSetIsIdenticalForEveryKit() {
        assertEquals(java.util.List.of(WizardsSpell.MANA_BOLT, WizardsSpell.WIZARDS_COMPASS),
                WizardsRules.initialKnownSpells());
    }

    @Test
    void originalSummonWebTrapAndProjectileFormulasArePreserved() {
        assertEquals(3, WizardsRules.summonedWolfCount(1));
        assertEquals(4, WizardsRules.summonedWolfCount(2));
        assertEquals(5, WizardsRules.summonedWolfCount(3));
        assertEquals(2, WizardsRules.webShotCount(1));
        assertEquals(4, WizardsRules.webShotCount(2));
        assertEquals(6, WizardsRules.webShotCount(3));
        assertEquals(16, WizardsRules.trapRuneRange(1));
        assertEquals(20, WizardsRules.trapRuneRange(2));
        assertEquals(24, WizardsRules.trapRuneRange(3));
        assertEquals(1.0D, WizardsRules.trapRuneSize(1));
        assertEquals(3.0D, WizardsRules.trapRuneDamage(1));
        assertEquals(9.0D, WizardsRules.trapRuneDamage(3));
        assertEquals(6.0D, WizardsRules.spectralArrowDamage(1, 0.0D));
        assertEquals(7.0D, WizardsRules.spectralArrowDamage(1, 1.0D));
        assertEquals(0.3D, WizardsRules.manaBoltVelocity());
        assertEquals(0.14D, WizardsRules.fireballVelocity());
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

    @Test
    void endgameUsesOriginalTenAndTwentyMinuteMilestones() {
        assertEquals(10, WizardsRules.ENDGAME_START_MINUTES);
        assertEquals(20, WizardsRules.SUDDEN_DEATH_MINUTES);
        assertEquals(6_000, WizardsRules.ENDGAME_INITIAL_STRIKE_MILLIS);
        assertEquals(750, WizardsRules.ENDGAME_MIN_STRIKE_MILLIS);
    }

    @Test
    void endgameStrikeIntervalTightensDownTo750Milliseconds() {
        assertEquals(5_998, WizardsRules.nextEndgameStrikeInterval(6_000));
        assertEquals(5_000, WizardsRules.nextEndgameStrikeInterval(5_002));
        assertEquals(750, WizardsRules.nextEndgameStrikeInterval(751));
        assertEquals(750, WizardsRules.nextEndgameStrikeInterval(750));
        assertEquals(750, WizardsRules.nextEndgameStrikeInterval(100));
    }

    @Test
    void endgameDamageFallsLinearlyToZeroAtFourTimesItsSize() {
        assertEquals(9.0D, WizardsRules.endgameDamage(1.5D, 0.0D), 0.0001D);
        assertEquals(4.5D, WizardsRules.endgameDamage(1.5D, 3.0D), 0.0001D);
        assertEquals(0.0D, WizardsRules.endgameDamage(1.5D, 6.0D), 0.0001D);
        assertEquals(0.0D, WizardsRules.endgameDamage(1.5D, -1.0D), 0.0001D);
    }
}
