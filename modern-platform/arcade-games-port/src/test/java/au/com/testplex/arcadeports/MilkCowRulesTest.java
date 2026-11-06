package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MilkCowRulesTest {
    @AfterEach void clearOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test void chargeAndHerdTuningChangesLiveRuleOutputsAndResetsToDefaults() {
        assertEquals(250L, MilkCowRules.cowChargeHungerIntervalMillis());
        assertEquals(2.0D, MilkCowRules.cowChargeTargetRadiusBlocks());
        assertEquals(1.6D, MilkCowRules.cowChargeTargetHeightBlocks());
        assertEquals(500L, MilkCowRules.cowChargeTargetCooldownMillis());
        assertEquals(3.0D, MilkCowRules.cowChargeDamage());
        assertEquals(1.0D, MilkCowRules.cowChargeKnockbackBase());
        assertEquals(0.8D, MilkCowRules.cowChargeKnockbackBonus());
        assertEquals(0.8D, MilkCowRules.cowChargeVerticalBase());
        assertEquals(0.4D, MilkCowRules.cowChargeVerticalBonus());
        MilkCowRules.ChargeKnockback defaultCenter = MilkCowRules.cowChargeKnockbackAtDistance(0.0D);
        assertEquals(1.8D, defaultCenter.horizontal(), 1.0e-9);
        assertEquals(1.2D, defaultCenter.vertical(), 1.0e-9);
        MilkCowRules.ChargeKnockback defaultEdge = MilkCowRules.cowChargeKnockbackAtDistance(2.0D);
        assertEquals(1.0D, defaultEdge.horizontal(), 1.0e-9);
        assertEquals(0.8D, defaultEdge.vertical(), 1.0e-9);
        assertEquals(1_000L, MilkCowRules.herdUpdateIntervalMillis());
        assertEquals(2.25D, MilkCowRules.herdFollowMinDistanceSquared());
        assertEquals(2.5D, MilkCowRules.herdSpawnRadiusBlocks());
        assertEquals(2.0D, MilkCowRules.farmerHealPerMilkHp());
        assertEquals(1, MilkCowRules.scorePerCowKill());
        assertEquals(4, MilkCowRules.cowSprintSpeedDurationTicks());
        assertEquals(1, MilkCowRules.cowSprintSpeedAmplifier());

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("MILK_THE_COW_COW_CHARGE_HUNGER_INTERVAL_MILLIS", 500L),
                Map.entry("MILK_THE_COW_COW_CHARGE_TARGET_RADIUS_BLOCKS", 4.0D),
                Map.entry("MILK_THE_COW_COW_CHARGE_TARGET_HEIGHT_BLOCKS", 3.0D),
                Map.entry("MILK_THE_COW_COW_CHARGE_TARGET_COOLDOWN_MILLIS", 1_000L),
                Map.entry("MILK_THE_COW_COW_CHARGE_DAMAGE", 7.0D),
                Map.entry("MILK_THE_COW_COW_CHARGE_KNOCKBACK_BASE", 2.0D),
                Map.entry("MILK_THE_COW_COW_CHARGE_KNOCKBACK_BONUS", 1.5D),
                Map.entry("MILK_THE_COW_COW_CHARGE_VERTICAL_BASE", 1.2D),
                Map.entry("MILK_THE_COW_COW_CHARGE_VERTICAL_BONUS", 0.8D),
                Map.entry("MILK_THE_COW_HERD_UPDATE_INTERVAL_MILLIS", 2_000L),
                Map.entry("MILK_THE_COW_HERD_FOLLOW_MIN_DISTANCE_SQUARED", 9.0D),
                Map.entry("MILK_THE_COW_HERD_SPAWN_RADIUS_BLOCKS", 5.0D),
                Map.entry("MILK_THE_COW_FARMER_HEAL_PER_MILK_HP", 4.0D),
                Map.entry("MILK_THE_COW_SCORE_PER_COW_KILL", 3),
                Map.entry("MILK_THE_COW_COW_SPRINT_SPEED_DURATION_TICKS", 12),
                Map.entry("MILK_THE_COW_COW_SPRINT_SPEED_AMPLIFIER", 2)));
        assertEquals(500L, MilkCowRules.cowChargeHungerIntervalMillis());
        assertEquals(4.0D, MilkCowRules.cowChargeTargetRadiusBlocks());
        assertEquals(3.0D, MilkCowRules.cowChargeTargetHeightBlocks());
        assertEquals(1_000L, MilkCowRules.cowChargeTargetCooldownMillis());
        assertEquals(7.0D, MilkCowRules.cowChargeDamage());
        assertEquals(2.0D, MilkCowRules.cowChargeKnockbackBase());
        assertEquals(1.5D, MilkCowRules.cowChargeKnockbackBonus());
        assertEquals(1.2D, MilkCowRules.cowChargeVerticalBase());
        assertEquals(0.8D, MilkCowRules.cowChargeVerticalBonus());
        MilkCowRules.ChargeKnockback changedCenter = MilkCowRules.cowChargeKnockbackAtDistance(0.0D);
        assertEquals(3.5D, changedCenter.horizontal(), 1.0e-9);
        assertEquals(2.0D, changedCenter.vertical(), 1.0e-9);
        MilkCowRules.ChargeKnockback changedEdge = MilkCowRules.cowChargeKnockbackAtDistance(4.0D);
        assertEquals(2.0D, changedEdge.horizontal(), 1.0e-9);
        assertEquals(1.2D, changedEdge.vertical(), 1.0e-9);
        assertEquals(2_000L, MilkCowRules.herdUpdateIntervalMillis());
        assertEquals(9.0D, MilkCowRules.herdFollowMinDistanceSquared());
        assertEquals(5.0D, MilkCowRules.herdSpawnRadiusBlocks());
        assertEquals(4.0D, MilkCowRules.farmerHealPerMilkHp());
        assertEquals(3, MilkCowRules.scorePerCowKill());
        assertEquals(12, MilkCowRules.cowSprintSpeedDurationTicks());
        assertEquals(2, MilkCowRules.cowSprintSpeedAmplifier());

        GameDebugRuntimeOverrides.clear();
        assertEquals(250L, MilkCowRules.cowChargeHungerIntervalMillis());
        assertEquals(2.0D, MilkCowRules.cowChargeTargetRadiusBlocks());
        assertEquals(3.0D, MilkCowRules.cowChargeDamage());
        assertEquals(1_000L, MilkCowRules.herdUpdateIntervalMillis());
        assertEquals(2.0D, MilkCowRules.farmerHealPerMilkHp());
        assertEquals(1, MilkCowRules.scorePerCowKill());
        assertEquals(4, MilkCowRules.cowSprintSpeedDurationTicks());
        assertEquals(1, MilkCowRules.cowSprintSpeedAmplifier());
    }

    @Test void cowTeamStartsAtTwentyPercentAndLeavesAFarmerInTheGame() {
        assertEquals(0, MilkCowRules.cowCountForPlayers(0));
        assertEquals(0, MilkCowRules.cowCountForPlayers(1));
        assertEquals(1, MilkCowRules.cowCountForPlayers(2));
        assertEquals(1, MilkCowRules.cowCountForPlayers(5));
        assertEquals(2, MilkCowRules.cowCountForPlayers(10));
        assertEquals(4, MilkCowRules.cowCountForPlayers(20));
    }

    @Test void milkScoreStopsAtZeroAndWinsAtFifteen() {
        assertEquals(0, MilkCowRules.scoreAfterChange(1, -2));
        assertEquals(6, MilkCowRules.scoreAfterChange(5, 1));
        assertFalse(MilkCowRules.hasWon(14));
        assertTrue(MilkCowRules.hasWon(15));
    }

    @Test void roleAbilityTuningChangesProductionImpactRulesAndResets() {
        assertEquals(3_000L, MilkCowRules.cowBombCooldownMillis());
        assertEquals(1.35D, MilkCowRules.cowBombLaunchSpeed());
        assertEquals(2_500L, MilkCowRules.cowBombLifetimeMillis());
        assertEquals(1.1D, MilkCowRules.cowBombContactRadiusBlocks());
        assertEquals(4.0D, MilkCowRules.cowBombDamage());
        assertEquals(2.5D, MilkCowRules.cowBombKnockback());
        assertEquals(6_000L, MilkCowRules.bodySlamCooldownMillis());
        assertEquals(1_000L, MilkCowRules.bodySlamLandingDelayMillis());
        assertEquals(4.5D, MilkCowRules.bodySlamDamageAtDistance(0.0D));
        assertEquals(0.5D, MilkCowRules.bodySlamDamageAtDistance(6.0D));
        assertEquals(1.8D, MilkCowRules.bodySlamKnockbackHorizontalAtDistance(0.0D));
        assertEquals(0.4D, MilkCowRules.bodySlamKnockbackVerticalAtDistance(6.0D));
        assertEquals(0.8D, MilkCowRules.farmerDoubleJumpHorizontalSpeed());
        assertEquals(0.45D, MilkCowRules.farmerDoubleJumpVerticalBonus());
        assertEquals(0.45D, MilkCowRules.cowChargeImpactVertical());

        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("MILK_THE_COW_COW_BOMB_COOLDOWN_MILLIS", 1_500L),
                Map.entry("MILK_THE_COW_COW_BOMB_LAUNCH_SPEED", 2.0D),
                Map.entry("MILK_THE_COW_COW_BOMB_LIFETIME_MILLIS", 4_000L),
                Map.entry("MILK_THE_COW_COW_BOMB_CONTACT_RADIUS_BLOCKS", 2.0D),
                Map.entry("MILK_THE_COW_COW_BOMB_DAMAGE", 8.0D),
                Map.entry("MILK_THE_COW_COW_BOMB_KNOCKBACK", 3.5D),
                Map.entry("MILK_THE_COW_BODY_SLAM_RADIUS_BLOCKS", 3.0D),
                Map.entry("MILK_THE_COW_BODY_SLAM_DAMAGE_AT_CENTER", 8.0D),
                Map.entry("MILK_THE_COW_BODY_SLAM_KNOCKBACK_HORIZONTAL", 2.5D),
                Map.entry("MILK_THE_COW_ASSIST_CREDIT_WINDOW_MILLIS", 20_000L),
                Map.entry("MILK_THE_COW_PERFECT_GAME_MAX_FARMER_DEATHS", 2),
                Map.entry("MILK_THE_COW_COMEBACK_LEAD_THRESHOLD", 2),
                Map.entry("MILK_THE_COW_GOT_THE_CHOPS_AIRBORNE_KILLS", 4),
                Map.entry("MILK_THE_COW_SLAUGHTERHOUSE_ASSISTS", 5),
                Map.entry("MILK_THE_COW_FARMER_DOUBLE_JUMP_HORIZONTAL_SPEED", 1.4D),
                Map.entry("MILK_THE_COW_FARMER_DOUBLE_JUMP_VERTICAL_BONUS", 0.8D),
                Map.entry("MILK_THE_COW_COW_CHARGE_IMPACT_VERTICAL", 1.2D)));

        assertEquals(1_500L, MilkCowRules.cowBombCooldownMillis());
        assertEquals(2.0D, MilkCowRules.cowBombLaunchSpeed());
        assertEquals(4_000L, MilkCowRules.cowBombLifetimeMillis());
        assertEquals(2.0D, MilkCowRules.cowBombContactRadiusBlocks());
        assertEquals(8.0D, MilkCowRules.cowBombDamage());
        assertEquals(3.5D, MilkCowRules.cowBombKnockback());
        assertEquals(1.4D, MilkCowRules.farmerDoubleJumpHorizontalSpeed());
        assertEquals(0.8D, MilkCowRules.farmerDoubleJumpVerticalBonus());
        assertEquals(1.2D, MilkCowRules.cowChargeImpactVertical());
        assertEquals(8.5D, MilkCowRules.bodySlamDamageAtDistance(0.0D));
        assertEquals(0.5D, MilkCowRules.bodySlamDamageAtDistance(3.0D));
        assertEquals(2.5D, MilkCowRules.bodySlamKnockbackHorizontalAtDistance(0.0D));
        assertEquals(20_000L, MilkCowRules.assistCreditWindowMillis());
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowPerfectGame(2));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowStopStopHeIsAlreadyDead(2));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowGotTheChops(4));
        assertTrue(ArcadeProgressRules.shouldAwardMilkCowSlaughterhouse(5));

        GameDebugRuntimeOverrides.clear();
        assertEquals(3_000L, MilkCowRules.cowBombCooldownMillis());
        assertEquals(1.35D, MilkCowRules.cowBombLaunchSpeed());
        assertEquals(0.8D, MilkCowRules.farmerDoubleJumpHorizontalSpeed());
        assertEquals(0.45D, MilkCowRules.cowChargeImpactVertical());
        assertEquals(4.5D, MilkCowRules.bodySlamDamageAtDistance(0.0D));
        assertEquals(10_000L, MilkCowRules.assistCreditWindowMillis());
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowPerfectGame(2));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowStopStopHeIsAlreadyDead(2));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowGotTheChops(4));
        assertFalse(ArcadeProgressRules.shouldAwardMilkCowSlaughterhouse(5));
    }
}
