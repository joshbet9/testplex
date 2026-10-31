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
        assertEquals(30, WizardsSpell.HEAL.cooldownSeconds(1, 0.5D));
        assertEquals(20, WizardsSpell.RAINBOW_ROAD.cooldownSeconds(1, 0.5D));
    }

    @Test
    void everyHistoricalSpellHasAnImplementedModernMechanic() {
        assertEquals(21, WizardsSpell.values().length);
        assertEquals(21, WizardsSpell.implementedSpells().size());
        assertTrue(java.util.Arrays.stream(WizardsSpell.values()).allMatch(WizardsSpell::implemented));
    }

    @Test
    void rejectsNegativeOrNonFiniteDamageInputs() {
        assertEquals(0.0D, WizardsRules.capMeleeDamage(-1.0D), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.NaN), 0.000001D);
        assertEquals(0.0D, WizardsRules.capMeleeDamage(Double.POSITIVE_INFINITY), 0.000001D);
    }
}
