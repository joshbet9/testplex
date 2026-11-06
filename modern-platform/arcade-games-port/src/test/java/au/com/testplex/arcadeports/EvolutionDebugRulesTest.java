package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EvolutionDebugRulesTest {
    @AfterEach void clearProfileOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void allFiveFormCombatAndMovementDefaultsRemainUnchanged() {
        assertEquals(40.0D, ArcadePortRules.evolutionMaxHealth(0));
        assertEquals(36.0D, ArcadePortRules.evolutionMaxHealth(1));
        assertEquals(28.0D, ArcadePortRules.evolutionMaxHealth(2));
        assertEquals(22.0D, ArcadePortRules.evolutionMaxHealth(3));
        assertEquals(16.0D, ArcadePortRules.evolutionMaxHealth(4));
        assertEquals(6.0D, ArcadePortRules.evolutionMeleeDamage(0));
        assertEquals(5.0D, ArcadePortRules.evolutionMeleeDamage(1));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(2));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(3));
        assertEquals(3.0D, ArcadePortRules.evolutionMeleeDamage(4));
        assertEquals(1.0D, ArcadePortRules.evolutionFallDamageMultiplier(0));
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(1));
        assertEquals(1.0D, ArcadePortRules.evolutionFallDamageMultiplier(2));
        assertEquals(0.5D, ArcadePortRules.evolutionFallDamageMultiplier(3));
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(4));
        assertEquals(0.012D, ArcadePortRules.evolutionChargePerTick(1));
        assertEquals(0.018D, ArcadePortRules.evolutionChargePerTick(3), 0.0000001D);
        assertEquals(2, ArcadePortRules.evolutionOverkillKillsRequired());
        assertEquals(2.0D, ArcadePortRules.evolutionEggDamage());
    }

    @Test void profileOverridesChangeFormCombatChargeAndOverkillProgressionThenReset() {
        GameDebugRuntimeOverrides.activate(Map.of(
                "EVOLUTION_GOLEM_MAX_HEALTH", 50.0D,
                "EVOLUTION_BLAZE_MELEE_DAMAGE", 9.0D,
                "EVOLUTION_BLAZE_FALL_DAMAGE_MULTIPLIER", 0.75D,
                "EVOLUTION_CHARGE_BASE_PER_TICK", 0.02D,
                "EVOLUTION_CHARGE_PER_EXTRA_KILL", 0.005D,
                "EVOLUTION_CHARGE_MAX_PER_TICK", 0.04D,
                "EVOLUTION_OVERKILL_KILLS_PER_EVOLUTION", 3,
                "EVOLUTION_EGG_DAMAGE", 5.0D));

        assertEquals(50.0D, ArcadePortRules.evolutionMaxHealth(0));
        assertEquals(9.0D, ArcadePortRules.evolutionMeleeDamage(1));
        assertEquals(0.75D, ArcadePortRules.evolutionFallDamageMultiplier(1));
        assertEquals(0.03D, ArcadePortRules.evolutionChargePerTick(3));
        assertEquals(3, ArcadePortRules.evolutionOverkillKillsRequired());
        assertEquals(5.0D, ArcadePortRules.evolutionEggDamage());
        assertEquals(3, ArcadePortRules.nextEvolutionOverkillKillCount(2));
        assertTrue(ArcadePortRules.evolutionOverkillRequirementMet(3));
        assertFalse(ArcadePortRules.evolutionOverkillRequirementMet(2));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(36.0D, ArcadePortRules.evolutionMaxHealth(1));
        assertEquals(4.0D, ArcadePortRules.evolutionMeleeDamage(2));
        assertEquals(0.0D, ArcadePortRules.evolutionFallDamageMultiplier(1));
        assertEquals(0.018D, ArcadePortRules.evolutionChargePerTick(3), 0.0000001D);
        assertEquals(2, ArcadePortRules.nextEvolutionOverkillKillCount(9));
        assertEquals(2, ArcadePortRules.evolutionOverkillKillsRequired());
        assertEquals(2.0D, ArcadePortRules.evolutionEggDamage());
        assertTrue(GameDebugRuntimeOverrides.isDebugSession());
    }

    @Test void practicalFormAndProgressionVariablesAppearWithUsefulRanges() {
        GameDebugVariableCatalog.Variable health = GameDebugVariableCatalog.variable(
                ArcadePortGame.EVOLUTION, "EVOLUTION_GOLEM_MAX_HEALTH");
        GameDebugVariableCatalog.Variable fall = GameDebugVariableCatalog.variable(
                ArcadePortGame.EVOLUTION, "EVOLUTION_BLAZE_FALL_DAMAGE_MULTIPLIER");
        GameDebugVariableCatalog.Variable charge = GameDebugVariableCatalog.variable(
                ArcadePortGame.EVOLUTION, "EVOLUTION_CHARGE_BASE_PER_TICK");
        GameDebugVariableCatalog.Variable eggDamage = GameDebugVariableCatalog.variable(
                ArcadePortGame.EVOLUTION, "EVOLUTION_EGG_DAMAGE");
        assertNotNull(health);
        assertNotNull(fall);
        assertNotNull(charge);
        assertNotNull(eggDamage);
        assertEquals(1.0D, health.minimum());
        assertEquals(80.0D, health.maximum());
        assertEquals(0.0D, fall.minimum());
        assertEquals(1.0D, fall.maximum());
        assertEquals(0.001D, charge.step());
    }
}
