package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BaconBrawlDebugRulesTest {
    @AfterEach void resetOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void meterGainThresholdAndAbilityPowerUseOverridesAndRestoreDefaults() {
        assertEquals(5, ArcadePortRules.baconMeterGainForHit(false, 16));
        assertEquals(0.75D, ArcadePortRules.baconBodySlamMeterMultiplier(50), 0.00001D);
        assertEquals(1.25D, ArcadePortRules.baconBlastPowerMultiplier(50), 0.00001D);

        GameDebugRuntimeOverrides.activate(Map.of(
                "BACON_METER_GAIN_NORMAL_HIT", 8,
                "BACON_METER_GAIN_CRITICAL_HIT", 16,
                "BACON_METER_POPULATION_SCALE", 7.0D,
                "BACON_METER_MAX", 120,
                "BACON_METER_MIN_ABILITY", 40,
                "BACON_BODY_SLAM_METER_BASE_MULTIPLIER", 0.25D,
                "BACON_BODY_SLAM_METER_CHARGE_SCALE", 100.0D,
                "BACON_BLAST_METER_BASE_MULTIPLIER", 1.0D,
                "BACON_BLAST_METER_CHARGE_SCALE", 2.0D));

        assertEquals(8, ArcadePortRules.baconMeterGainForHit(false, 16));
        assertEquals(48, ArcadePortRules.baconMeterGainForHit(true, 2));
        assertEquals(44, ArcadePortRules.nextBaconMeter(20, false, 2));
        assertEquals(120, ArcadePortRules.nextBaconMeter(115, true, 16));
        assertTrue(ArcadePortRules.canUseBaconMeterAbility(40));
        assertEquals(0.75D, ArcadePortRules.baconBodySlamMeterMultiplier(50), 0.00001D);
        assertEquals(1.833333D, ArcadePortRules.baconBlastPowerMultiplier(50), 0.00001D);

        GameDebugRuntimeOverrides.clear();
        assertEquals(5, ArcadePortRules.baconMeterGainForHit(false, 16));
        assertFalse(ArcadePortRules.canUseBaconMeterAbility(40));
        assertEquals(0.75D, ArcadePortRules.baconBodySlamMeterMultiplier(50), 0.00001D);
        assertEquals(1.25D, ArcadePortRules.baconBlastPowerMultiplier(50), 0.00001D);
    }

    @Test void hungerFoodChangesUseSourceSaturationAndAllowTuningThenReset() {
        assertEquals(5.0F, ArcadePortRules.baconSaturationOnFoodChange());
        GameDebugRuntimeOverrides.activate(Map.of("BACON_SATURATION_ON_FOOD_CHANGE", 12.0F));
        assertEquals(12.0F, ArcadePortRules.baconSaturationOnFoodChange());
        GameDebugRuntimeOverrides.activate(Map.of("BACON_SATURATION_ON_FOOD_CHANGE", 25.0F));
        assertEquals(20.0F, ArcadePortRules.baconSaturationOnFoodChange());
        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(5.0F, ArcadePortRules.baconSaturationOnFoodChange());
    }

    @Test void crispyFireDurationAndImpactFootprintAreTunableAndRestoreDefaults() {
        assertEquals(5_000L, ArcadePortRules.baconCrispyBurnDurationMillis());
        assertEquals(2.25D, ArcadePortRules.baconCrispyDashHitRadiusSquared());
        assertEquals(2.25D, ArcadePortRules.baconCrispyFireContactRadiusSquared());
        assertEquals(3, ArcadePortRules.baconCrispyImpactFireBlocks());

        GameDebugRuntimeOverrides.activate(Map.of(
                "BACON_CRISPY_BURN_DURATION_MILLIS", 8_000L,
                "BACON_CRISPY_DASH_HIT_RADIUS_SQUARED", 9.0D,
                "BACON_CRISPY_FIRE_CONTACT_RADIUS_SQUARED", 4.0D,
                "BACON_CRISPY_IMPACT_FIRE_BLOCKS", 5));
        assertEquals(8_000L, ArcadePortRules.baconCrispyBurnDurationMillis());
        assertEquals(9.0D, ArcadePortRules.baconCrispyDashHitRadiusSquared());
        assertEquals(4.0D, ArcadePortRules.baconCrispyFireContactRadiusSquared());
        assertEquals(5, ArcadePortRules.baconCrispyImpactFireBlocks());

        GameDebugRuntimeOverrides.clear();
        assertEquals(5_000L, ArcadePortRules.baconCrispyBurnDurationMillis());
        assertEquals(2.25D, ArcadePortRules.baconCrispyDashHitRadiusSquared());
        assertEquals(2.25D, ArcadePortRules.baconCrispyFireContactRadiusSquared());
        assertEquals(3, ArcadePortRules.baconCrispyImpactFireBlocks());
    }

    @Test void combatTimingReachAndBlastCollisionUseLiveRuleValues() {
        assertEquals(0.65D, ArcadePortRules.baconTimedHitKnockbackMultiplier(200L), 0.00001D);
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 13.0D, true));
        assertFalse(ArcadePortRules.isBaconBlastNearPlayer(0.8D));

        GameDebugRuntimeOverrides.activate(Map.of(
                "BACON_TIMED_HIT_SPAM_WINDOW_MILLIS", 100L,
                "BACON_TIMED_HIT_GOOD_MIN_MILLIS", 200L,
                "BACON_TIMED_HIT_GOOD_MAX_MILLIS", 300L,
                "BACON_AVATAR_MAX_ATTACK_DISTANCE_SQUARED", 16.0D,
                "BACON_BLAST_PLAYER_COLLISION_RADIUS_SQUARED", 1.0D,
                "BACON_BLAST_MAX_FLIGHT_MILLIS", 2_000L));
        assertEquals(1.6D, ArcadePortRules.baconTimedHitKnockbackMultiplier(200L), 0.00001D);
        assertTrue(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 13.0D, true));
        assertTrue(ArcadePortRules.isBaconBlastNearPlayer(0.8D));
        assertTrue(ArcadePortRules.shouldExpireBaconBlast(3_000L, 1_000L, false));

        GameDebugRuntimeOverrides.clear();
        assertFalse(ArcadePortRules.shouldAcceptBaconAvatarPacketHit(true, true, true, false, 13.0D, true));
        assertFalse(ArcadePortRules.isBaconBlastNearPlayer(0.8D));
        assertFalse(ArcadePortRules.shouldExpireBaconBlast(3_000L, 1_000L, false));
    }

    @Test void progressionAndKnockbackCadenceUseOverridesAndRestoreDefaults() {
        assertFalse(ArcadePortRules.shouldAwardBaconPigsInBlankets(5));
        assertEquals(50_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 8));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(40_000L, 1L, true));

        GameDebugRuntimeOverrides.activate(Map.of(
                "BACON_BRAWL_PIGS_IN_BLANKETS_KILLS", 5,
                "BACON_THREE_FOR_ONE_BODY_SLAM_KILLS", 2,
                "BACON_FLASH_FRIED_MAX_WIN_MILLIS", 90_000L,
                "BACON_FALL_KILL_CREDIT_WINDOW_MILLIS", 10_000L,
                "BACON_INITIAL_KNOCKBACK_INTERVAL_MILLIS", 20_000L,
                "BACON_KNOCKBACK_INTERVAL_STEP_MILLIS", 2_000L,
                "BACON_MIN_KNOCKBACK_INTERVAL_MILLIS", 4_000L,
                "BACON_BRAWL_ENDGAME_ESCALATION_DELAY_MILLIS", 6_000L));
        assertTrue(ArcadePortRules.shouldAwardBaconPigsInBlankets(5));
        assertTrue(ArcadePortRules.shouldAwardBaconThreeForOne(2));
        assertTrue(ArcadePortRules.shouldAwardBaconFlashFried(90_000L));
        assertTrue(ArcadePortRules.shouldCreditBaconFallKill(10_000L, 1L, true));
        var killer = java.util.UUID.randomUUID();
        GameDebugRuntimeOverrides.setValue("BACON_BLAST_KILL_COMBO_WINDOW_MILLIS", 8_000L);
        assertTrue(ArcadePortRules.shouldCountMamaCameraBlastKill(true, false, killer, killer, 16_000L, 9_000L));
        assertEquals(20_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 8));
        assertEquals(6_000L, ArcadePortRules.nextBaconKnockbackInterval(1, 2));

        GameDebugRuntimeOverrides.clear();
        assertFalse(ArcadePortRules.shouldAwardBaconPigsInBlankets(5));
        assertEquals(50_000L, ArcadePortRules.nextBaconKnockbackInterval(0, 8));
        assertFalse(ArcadePortRules.shouldCreditBaconFallKill(40_000L, 1L, true));
        assertFalse(ArcadePortRules.shouldCountMamaCameraBlastKill(true, false, killer, killer, 16_000L, 9_000L));
    }
}
