package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GladiatorsDebugRulesTest {
    @AfterEach
    void resetOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void tierArrowSuppliesCanBeAdjustedAndResetWithoutChangingDefaultLoadouts() {
        assertEquals(3, ArcadePortRules.gladiatorsArrowCount(0));
        assertEquals(5, ArcadePortRules.gladiatorsArrowCount(1));
        assertEquals(7, ArcadePortRules.gladiatorsArrowCount(2));
        assertEquals(10, ArcadePortRules.gladiatorsArrowCount(3));

        GameDebugRuntimeOverrides.activate(Map.of(
                "GLADIATOR_TIER_0_ARROWS", 1,
                "GLADIATOR_TIER_1_ARROWS", 2,
                "GLADIATOR_TIER_2_ARROWS", 3,
                "GLADIATOR_TIER_3_ARROWS", 4));
        assertEquals(1, ArcadePortRules.gladiatorsArrowCount(0));
        assertEquals(2, ArcadePortRules.gladiatorsArrowCount(1));
        assertEquals(3, ArcadePortRules.gladiatorsArrowCount(2));
        assertEquals(4, ArcadePortRules.gladiatorsArrowCount(3));

        GameDebugRuntimeOverrides.clear();
        assertEquals(3, ArcadePortRules.gladiatorsArrowCount(0));
        assertEquals(5, ArcadePortRules.gladiatorsArrowCount(1));
        assertEquals(7, ArcadePortRules.gladiatorsArrowCount(2));
        assertEquals(10, ArcadePortRules.gladiatorsArrowCount(3));
    }

    @Test
    void meleeAndProjectileHitWindowsAndKnockbackAreIndependentlyAdjustable() {
        assertEquals(10, ArcadePortRules.gladiatorsNoDamageTicks(false));
        assertEquals(8, ArcadePortRules.gladiatorsNoDamageTicks(true));
        assertEquals(0.32D, ArcadePortRules.gladiatorsKnockbackHorizontal(false), 1.0e-9);
        assertEquals(0.12D, ArcadePortRules.gladiatorsKnockbackVertical(false), 1.0e-9);
        assertEquals(0.18D, ArcadePortRules.gladiatorsKnockbackHorizontal(true), 1.0e-9);
        assertEquals(0.06D, ArcadePortRules.gladiatorsKnockbackVertical(true), 1.0e-9);

        GameDebugRuntimeOverrides.activate(Map.of(
                "GLADIATOR_MELEE_NO_DAMAGE_TICKS", 4,
                "GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS", 2,
                "GLADIATOR_MELEE_KNOCKBACK_HORIZONTAL", 0.6D,
                "GLADIATOR_MELEE_KNOCKBACK_VERTICAL", 0.3D,
                "GLADIATOR_PROJECTILE_KNOCKBACK_HORIZONTAL", 0.25D,
                "GLADIATOR_PROJECTILE_KNOCKBACK_VERTICAL", 0.1D));
        assertEquals(4, ArcadePortRules.gladiatorsNoDamageTicks(false));
        assertEquals(2, ArcadePortRules.gladiatorsNoDamageTicks(true));
        assertEquals(0.6D, ArcadePortRules.gladiatorsKnockbackHorizontal(false), 1.0e-9);
        assertEquals(0.3D, ArcadePortRules.gladiatorsKnockbackVertical(false), 1.0e-9);
        assertEquals(0.25D, ArcadePortRules.gladiatorsKnockbackHorizontal(true), 1.0e-9);
        assertEquals(0.1D, ArcadePortRules.gladiatorsKnockbackVertical(true), 1.0e-9);

        GameDebugRuntimeOverrides.clear();
        assertEquals(10, ArcadePortRules.gladiatorsNoDamageTicks(false));
        assertEquals(8, ArcadePortRules.gladiatorsNoDamageTicks(true));
        assertEquals(0.32D, ArcadePortRules.gladiatorsKnockbackHorizontal(false), 1.0e-9);
        assertEquals(0.18D, ArcadePortRules.gladiatorsKnockbackHorizontal(true), 1.0e-9);
    }

    @Test
    void potSplashHealingAndRadiusAreTunableAndRestoreSourceDefaults() {
        assertEquals(4.0D, ArcadePortRules.gladiatorsPotionHealing(), 1.0e-9);
        assertEquals(4.0D, ArcadePortRules.gladiatorsPotionSplashRadius(), 1.0e-9);

        GameDebugRuntimeOverrides.activate(Map.of(
                "GLADIATOR_POTION_HEALING", 7.0D,
                "GLADIATOR_POTION_SPLASH_RADIUS", 6.0D));
        assertEquals(7.0D, ArcadePortRules.gladiatorsPotionHealing(), 1.0e-9);
        assertEquals(6.0D, ArcadePortRules.gladiatorsPotionSplashRadius(), 1.0e-9);

        GameDebugRuntimeOverrides.clear();
        assertEquals(4.0D, ArcadePortRules.gladiatorsPotionHealing(), 1.0e-9);
        assertEquals(4.0D, ArcadePortRules.gladiatorsPotionSplashRadius(), 1.0e-9);
    }

    @Test
    void tournamentSetupControlsKeepVoteDefaultsAndCanForceModeTierAndPacing() {
        assertFalse(ArcadePortRules.gladiatorsForceModeEnabled());
        assertEquals(0, ArcadePortRules.gladiatorsModeOverrideIndex());
        assertEquals(15_000L, ArcadePortRules.gladiatorsVoteMillis());
        assertEquals(15_000L, ArcadePortRules.gladiatorsArenaTransitionMillis());
        assertEquals(5, ArcadePortRules.gladiatorsPotionCount());
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(2));
        assertEquals(2, ArcadePortRules.gladiatorsStartingTierForPlayers(4));
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(8));
        assertEquals(0, ArcadePortRules.gladiatorsStartingTierForPlayers(16));

        GameDebugRuntimeOverrides.activate(Map.of(
                "GLADIATOR_FORCE_MODE_ENABLED", true,
                "GLADIATOR_MODE_OVERRIDE", 3,
                "GLADIATOR_STARTING_TIER_OVERRIDE", 1,
                "GLADIATOR_VOTE_MILLIS", 3_000L,
                "GLADIATOR_ARENA_TRANSITION_MILLIS", 2_000L,
                "GLADIATOR_POTION_COUNT", 9));
        assertTrue(ArcadePortRules.gladiatorsForceModeEnabled());
        assertEquals(GladiatorsMode.MODERN, GladiatorsMode.values()[ArcadePortRules.gladiatorsModeOverrideIndex()]);
        assertEquals(1, ArcadePortRules.gladiatorsStartingTierForPlayers(16));
        assertEquals(3_000L, ArcadePortRules.gladiatorsVoteMillis());
        assertEquals(2_000L, ArcadePortRules.gladiatorsArenaTransitionMillis());
        assertEquals(9, ArcadePortRules.gladiatorsPotionCount());

        GameDebugRuntimeOverrides.clear();
        assertFalse(ArcadePortRules.gladiatorsForceModeEnabled());
        assertEquals(3, ArcadePortRules.gladiatorsStartingTierForPlayers(2));
        assertEquals(5, ArcadePortRules.gladiatorsPotionCount());
    }

    @Test
    void pvpAndEnvironmentalDamagePacingAreAdjustableAndRestoreDefaults() {
        assertEquals(20.0D, ArcadePortRules.gladiatorsPvpDamage(20.0D), 1.0e-9);
        assertEquals(10, ArcadePortRules.gladiatorsEnvironmentalNoDamageTicks());
        GameDebugRuntimeOverrides.activate(Map.of(
                "GLADIATOR_PVP_DAMAGE_MULTIPLIER", 1.5D,
                "GLADIATOR_ENVIRONMENTAL_NO_DAMAGE_TICKS", 5));
        assertEquals(30.0D, ArcadePortRules.gladiatorsPvpDamage(20.0D), 1.0e-9);
        assertEquals(5, ArcadePortRules.gladiatorsEnvironmentalNoDamageTicks());
        GameDebugRuntimeOverrides.clear();
        assertEquals(20.0D, ArcadePortRules.gladiatorsPvpDamage(20.0D), 1.0e-9);
        assertEquals(10, ArcadePortRules.gladiatorsEnvironmentalNoDamageTicks());
    }

    @Test
    void setupVariablesAreExposedWithBoundedRangesAndModeDescriptions() {
        assertVariable("GLADIATOR_MODE_OVERRIDE", 0.0D, 4.0D);
        assertVariable("GLADIATOR_STARTING_TIER_OVERRIDE", -1.0D, 3.0D);
        assertVariable("GLADIATOR_POTION_COUNT", 0.0D, 64.0D);
        assertVariable("GLADIATOR_ARENA_TRANSITION_MILLIS", 0.0D, 60_000.0D);
        assertVariable("GLADIATOR_VOTE_MILLIS", 0.0D, 60_000.0D);
        assertVariable("GLADIATOR_PVP_DAMAGE_MULTIPLIER", 0.0D, 5.0D);
        assertVariable("GLADIATOR_ENVIRONMENTAL_NO_DAMAGE_TICKS", 0.0D, 40.0D);
        assertTrue(GameDebugVariableCatalog.humanize("GLADIATOR_MODE_OVERRIDE").contains("0 Normal"));
        assertNotNull(GameDebugVariableCatalog.variable(ArcadePortGame.GLADIATORS,
                "GLADIATOR_FORCE_MODE_ENABLED"));
    }

    private static void assertVariable(String key, double minimum, double maximum) {
        GameDebugVariableCatalog.Variable variable = GameDebugVariableCatalog.variable(ArcadePortGame.GLADIATORS, key);
        assertNotNull(variable, key);
        assertEquals(minimum, variable.minimum(), key);
        assertEquals(maximum, variable.maximum(), key);
    }
}
