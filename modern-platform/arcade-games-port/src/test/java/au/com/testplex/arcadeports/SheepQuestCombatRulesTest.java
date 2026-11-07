package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheepQuestCombatRulesTest {
    @Test void swordsAndAxesUsePreCombatUpdateDamageWithoutAttackCooldownScaling() {
        assertDamage("WOODEN_SWORD", 4.0D);
        assertDamage("GOLDEN_SWORD", 4.0D);
        assertDamage("STONE_SWORD", 5.0D);
        assertDamage("IRON_SWORD", 6.0D);
        assertDamage("DIAMOND_SWORD", 7.0D);
        assertDamage("WOODEN_AXE", 3.0D);
        assertDamage("GOLDEN_AXE", 3.0D);
        assertDamage("STONE_AXE", 4.0D);
        assertDamage("IRON_AXE", 5.0D);
        assertDamage("DIAMOND_AXE", 6.0D);
        assertDamage("SHEARS", 1.0D);
        assertDamage("SADDLE", 1.0D);
        assertDamage(null, 1.0D);
    }

    @Test void legacyFallingCriticalsIncreaseMeleeDamageByHalf() {
        assertEquals(6.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SWORD", true, Double.NaN));
        assertEquals(7.5D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "IRON_AXE", true, Double.NaN));
        assertEquals(9.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "IRON_SWORD", true, Double.NaN));
    }

    @Test void shepherdSpearJabsAreWeakAndChargesCannotExceedLegacyIronSword() {
        assertEquals(3.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SPEAR", false, 1.0D));
        assertEquals(4.5D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SPEAR", false, 4.5D));
        assertEquals(5.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SPEAR", false, 30.0D));
        assertEquals(5.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SPEAR", true, 500.0D));
        assertEquals(3.0D, ArcadePortRules.sheepQuestLegacyMeleeDamage(
                "WOODEN_SPEAR", true, Double.NaN));
    }

    @Test void arrowsUseImpactVelocityAndBoundedCriticalBonus() {
        assertEquals(2.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(1.0D, 0));
        assertEquals(3.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(1.5D, 0));
        assertEquals(6.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(3.0D, 0));
        assertEquals(10.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(3.0D, 4));
        assertEquals(10.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(3.0D, 100));
        assertEquals(6.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(3.0D, -10));
        assertEquals(0.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(Double.NaN, 0));
        assertEquals(0.0D, ArcadePortRules.sheepQuestLegacyArrowDamage(0.0D, 0));
    }

    @Test void armourUsesLegacyFourPercentPerDefencePointAndHandlesBadInputs() {
        assertEquals(6.0D, ArcadePortRules.sheepQuestLegacyDamageAfterArmour(6.0D, 0.0D));
        assertEquals(4.32D, ArcadePortRules.sheepQuestLegacyDamageAfterArmour(6.0D, 7.0D), 0.00001D);
        assertEquals(1.2D, ArcadePortRules.sheepQuestLegacyDamageAfterArmour(6.0D, 20.0D), 0.00001D);
        assertEquals(0.0D, ArcadePortRules.sheepQuestLegacyDamageAfterArmour(-1.0D, 7.0D));
        assertEquals(0.0D, ArcadePortRules.sheepQuestLegacyDamageAfterArmour(Double.NaN, 7.0D));
        assertTrue(ArcadePortRules.sheepQuestLegacyDamageAfterArmour(6.0D, Double.NaN) > 0.0D);
    }

    private static void assertDamage(String weapon, double expected) {
        assertEquals(expected, ArcadePortRules.sheepQuestLegacyMeleeDamage(weapon, false, Double.NaN));
    }
}
