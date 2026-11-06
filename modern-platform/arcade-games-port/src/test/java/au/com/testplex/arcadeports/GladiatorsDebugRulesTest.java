package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
