package au.com.testplex.wizards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WizardsRulesTest {
    @Test
    void capsEveryDirectMeleeHitAtTheOriginalHalfHeartValue() {
        assertEquals(0.5D, WizardsRules.capMeleeDamage(4.0D), 0.000001D);
        assertEquals(0.5D, WizardsRules.capMeleeDamage(0.5D), 0.000001D);
        assertEquals(0.25D, WizardsRules.capMeleeDamage(0.25D), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(0.0D), 0.000001D);
    }

    @Test
    void reproducesOriginalMineplexArmourPiecePercentages() {
        assertEquals(0.10D, WizardsRules.armourPieceProtection(org.bukkit.Material.LEATHER_CHESTPLATE), 0.000001D);
        assertEquals(0.075D, WizardsRules.armourPieceProtection(org.bukkit.Material.GOLDEN_HELMET), 0.000001D);
        assertEquals(0.05D, WizardsRules.armourPieceProtection(org.bukkit.Material.CHAINMAIL_BOOTS), 0.000001D);
        assertEquals(0.19D, WizardsRules.armourPieceProtection(org.bukkit.Material.IRON_CHESTPLATE), 0.000001D);
        assertEquals(0.25D, WizardsRules.armourPieceProtection(org.bukkit.Material.DIAMOND_CHESTPLATE), 0.000001D);
        assertEquals(0.25D, WizardsRules.armorProtection(
                org.bukkit.Material.LEATHER_HELMET,
                org.bukkit.Material.LEATHER_CHESTPLATE,
                org.bukkit.Material.LEATHER_LEGGINGS,
                org.bukkit.Material.LEATHER_BOOTS), 0.000001D);
        assertEquals(0.625D, WizardsRules.armorProtection(
                org.bukkit.Material.DIAMOND_HELMET,
                org.bukkit.Material.DIAMOND_CHESTPLATE,
                org.bukkit.Material.DIAMOND_LEGGINGS,
                org.bukkit.Material.DIAMOND_BOOTS), 0.000001D);
        assertEquals(0.0D, WizardsRules.armourPieceProtection(org.bukkit.Material.NETHERITE_CHESTPLATE), 0.000001D);
        assertEquals(0.0D, WizardsRules.armourPieceProtection(null), 0.000001D);
    }

    @Test
    void appliesTheGamePaceDiscountToManaAndCooldowns() {
        assertEquals(40, WizardsSpell.ANVIL_DROP.manaCost(1, 1.0D));
        assertEquals(20, WizardsSpell.ANVIL_DROP.manaCost(1, 0.5D));
        assertEquals(8, WizardsSpell.ANVIL_DROP.cooldownSeconds(1, 0.5D));

        assertEquals(25, WizardsSpell.HEAL.manaCost(1, 0.5D));
        assertEquals(15, WizardsSpell.HEAL.cooldownSeconds(1, 0.5D));
        assertEquals(10, WizardsSpell.RAINBOW_ROAD.cooldownSeconds(1, 0.5D));
    }

    @Test
    void everyHistoricalSpellHasAnImplementedModernMechanic() {
        assertEquals(21, WizardsSpell.values().length);
        assertEquals(21, WizardsSpell.implementedSpells().size());
        assertTrue(java.util.Arrays.stream(WizardsSpell.values()).allMatch(WizardsSpell::implemented));
    }

    @Test
    void usesTheOriginalMineplexKitPerksAndWizardDefault() {
        assertEquals(WizardsKit.WIZARD, WizardsKit.parse("wizard"));
        assertEquals(100.0F, WizardsRules.maximumMana(WizardsKit.WIZARD));
        assertEquals(2.5F, WizardsRules.manaPerSecond(WizardsKit.WIZARD, 0));
        assertEquals(2, WizardsRules.startingWands(WizardsKit.WIZARD));
        assertEquals(2, WizardsRules.startingWands(WizardsKit.MAGE));
        assertEquals(3, WizardsRules.startingWands(WizardsKit.SORCERER));
        assertEquals(2.75F, WizardsRules.manaPerSecond(WizardsKit.MYSTIC, 0));
        assertEquals(150.0F, WizardsRules.maximumMana(WizardsKit.WITCH_DOCTOR));
    }

    @Test
    void followsTheOriginalPowerSurgeScheduleAndSpellFormulas() {
        assertEquals(8, WizardsRules.MIN_PLAYERS);
        assertEquals(12, WizardsRules.MAX_PLAYERS);
        assertEquals(java.util.List.of(4, 5, 6, 7, 8, 9), WizardsRules.gamePaceMinutes());
        assertEquals(5, WizardsSpell.ICE_SHARDS.maximumLevel());
        assertEquals(5, WizardsSpell.RUMBLE.maximumLevel());
        assertEquals(3.0D, WizardsRules.manaBoltDamage(1), 0.000001D);
        assertEquals(4.0D, WizardsRules.fireballDamage(1), 0.000001D);
        assertEquals(4.0D, WizardsRules.lightningStrikeDamage(1), 0.000001D);
        assertEquals(2.0D, WizardsRules.healAmount(1), 0.000001D);
        assertEquals(4.0D, WizardsRules.healAmount(5), 0.000001D);
        assertEquals(5.0D, WizardsRules.rainbowBeamDamage(3, 30), 0.000001D);
        assertEquals(3.0D, WizardsRules.rainbowBeamDamage(3, 40), 0.000001D);
        assertEquals(3, WizardsRules.summonedWolfCount(1));
        assertEquals(16, WizardsRules.trapRuneRange(1));
        assertEquals(1.0D, WizardsRules.trapRuneSize(1), 0.000001D);
        assertEquals(5.0D, WizardsRules.trapRuneDamage(1), 0.000001D);
        assertEquals(3.0D, WizardsRules.spectralArrowDamage(1, 0), 0.000001D);
        assertEquals(3.0D, WizardsRules.rumbleDamage(1), 0.000001D);
        assertEquals(0.25D, WizardsRules.rumbleExplosionDamage(1), 0.000001D);
    }

    @Test
    void rejectsNegativeOrNonFiniteDamageInputs() {
        assertEquals(0.0D, WizardsRules.capMeleeDamage(-1.0D), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.NaN), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.POSITIVE_INFINITY), 0.000001D);
    }
}
