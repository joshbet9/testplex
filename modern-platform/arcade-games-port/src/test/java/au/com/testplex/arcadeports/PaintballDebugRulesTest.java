package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PaintballDebugRulesTest {
    @AfterEach void resetOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void profileDefaultsPreserveCurrentWeaponAndMovementRules() {
        assertEquals(500L, ArcadePortRules.paintballReloadMillis("rifle"));
        assertEquals(1_000L, ArcadePortRules.paintballReloadMillis("sniper"));
        assertEquals(1_400L, ArcadePortRules.paintballReloadMillis("shotgun"));
        assertEquals(8, ArcadePortRules.paintballShotCount("shotgun"));
        assertEquals(0.2D, ArcadePortRules.paintballShotSpread("shotgun"));
        assertEquals(0.125D, ArcadePortRules.paintballShotSpread("machine-gun"));
        assertEquals(2.0D, ArcadePortRules.paintballProjectileSpeedMultiplier("rifle"));
        assertEquals(1.6D, ArcadePortRules.paintballProjectileSpeedMultiplier("machine-gun"));
        assertEquals(15.0D, ArcadePortRules.paintballSniperArrowSpeed());
        assertEquals(0.03332D, ArcadePortRules.paintballSniperChargePerTick());
        assertEquals(8, ArcadePortRules.paintballSniperSlownessAmplifier());
        assertEquals(40, ArcadePortRules.paintballSniperSlownessTicks());
        assertEquals(80L, ArcadePortRules.paintballKitSpeedRefreshTicks());
        assertEquals(160, ArcadePortRules.paintballKitSpeedDurationTicks());
        assertEquals(0, ArcadePortRules.paintballKitSpeedAmplifier("rifle"));
        assertEquals(1, ArcadePortRules.paintballKitSpeedAmplifier("shotgun"));
        assertEquals(Math.log10(2.0D) * 2.0D, ArcadePortRules.paintballKnockbackStrength());
        assertEquals(3.0D, ArcadePortRules.paintballSaveMeRadius());
    }

    @Test void profileOverridesChangeActualWeaponRulesAndResetToSourceDefaults() {
        GameDebugRuntimeOverrides.activate(java.util.Map.of(
                "PAINTBALL_RIFLE_RELOAD_MILLIS", 250L,
                "PAINTBALL_SHOTGUN_PELLETS", 12,
                "PAINTBALL_SHOTGUN_SPREAD", 0.4D,
                "PAINTBALL_RIFLE_PROJECTILE_SPEED", 3.0D,
                "PAINTBALL_SNIPER_SCOPE_CHARGE_PER_TICK", 0.05D,
                "PAINTBALL_RIFLE_PAINT_PER_HIT", 2,
                "PAINTBALL_SNIPER_SCOPE_CHARGE_PER_EXTRA_PAINT", 0.5D,
                "PAINTBALL_SNIPER_MAX_EXTRA_PAINT", 1,
                "PAINTBALL_KNOCKBACK_MULTIPLIER", 0.0D,
                "PAINTBALL_SAVE_ME_RADIUS", 5.0D));

        assertEquals(250L, ArcadePortRules.paintballReloadMillis("rifle"));
        assertEquals(12, ArcadePortRules.paintballShotCount("shotgun"));
        assertEquals(0.4D, ArcadePortRules.paintballShotSpread("shotgun"));
        assertEquals(3.0D, ArcadePortRules.paintballProjectileSpeedMultiplier("rifle"));
        assertEquals(0.05D, ArcadePortRules.paintballSniperChargePerTick());
        assertEquals(2, ArcadePortRules.paintballPaintAmount("rifle", false, 0.0D));
        assertEquals(2, ArcadePortRules.paintballSniperPaintDamage(0.75D));
        assertEquals(5.0D, ArcadePortRules.paintballSaveMeRadius());
        assertTrue(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(24.0D, true));
        assertFalse(ArcadePortRules.shouldPaintballWaterBombAffectPlayer(24.0D, false));
        assertEquals(0.0D, ArcadePortRules.paintballKnockbackStrength());

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(500L, ArcadePortRules.paintballReloadMillis("rifle"));
        assertEquals(8, ArcadePortRules.paintballShotCount("shotgun"));
        assertEquals(3, ArcadePortRules.paintballPaintAmount("rifle", false, 0.0D));
        assertEquals(Math.log10(2.0D) * 2.0D, ArcadePortRules.paintballKnockbackStrength());
        assertEquals(3.0D, ArcadePortRules.paintballSaveMeRadius());
        assertTrue(GameDebugRuntimeOverrides.isDebugSession());
    }

    @Test void meaningfulPaintballTuningIsExposedWithBoundedProfileRanges() {
        GameDebugVariableCatalog.Variable reload = GameDebugVariableCatalog.variable(
                ArcadePortGame.PAINTBALL, "PAINTBALL_RIFLE_RELOAD_MILLIS");
        GameDebugVariableCatalog.Variable pellets = GameDebugVariableCatalog.variable(
                ArcadePortGame.PAINTBALL, "PAINTBALL_SHOTGUN_PELLETS");
        GameDebugVariableCatalog.Variable knockback = GameDebugVariableCatalog.variable(
                ArcadePortGame.PAINTBALL, "PAINTBALL_KNOCKBACK_MULTIPLIER");
        assertNotNull(reload);
        assertNotNull(pellets);
        assertNotNull(knockback);
        assertEquals(1.0D, reload.minimum());
        assertEquals(10_000.0D, reload.maximum());
        assertEquals(1.0D, pellets.minimum());
        assertEquals(32.0D, pellets.maximum());
        assertEquals(0.0D, knockback.minimum());
        assertEquals(3.0D, knockback.maximum());
        assertNotEquals(reload.defaultValue(), GameDebugVariableCatalog.clamp(reload, 250.0D));
    }
}
