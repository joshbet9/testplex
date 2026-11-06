package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SuperSploorRulesTest {
    @AfterEach
    void clearRuntimeOverrides() { GameDebugRuntimeOverrides.clear(); }

    @Test
    void inkIsBoundedAndCannotBeSpentBelowZero() {
        assertEquals(0, SuperSploorRules.clampInk(-10));
        assertEquals(100, SuperSploorRules.clampInk(150));
        assertEquals(76, SuperSploorRules.spendInk(80, 4));
        assertEquals(3, SuperSploorRules.spendInk(3, 4));
    }

    @Test
    void primaryFireHonoursInkAndCadence() {
        assertFalse(SuperSploorRules.canFire(3, 1_000, 0));
        assertFalse(SuperSploorRules.canFire(4, 199, 0));
        assertTrue(SuperSploorRules.canFire(4, 200, 0));
    }

    @Test
    void territoryTransitionsOnlyCountNeutralClaimsAndEnemyReclaims() {
        assertEquals(1, SuperSploorRules.territoryPaintTransition(-1, 0));
        assertEquals(2, SuperSploorRules.territoryPaintTransition(1, 0));
        assertEquals(0, SuperSploorRules.territoryPaintTransition(0, 0));
        assertEquals(1, SuperSploorRules.territoryPaintTransition(-1, 1));
        assertEquals(2, SuperSploorRules.territoryPaintTransition(0, 1));
        assertEquals(0, SuperSploorRules.territoryPaintTransition(1, 1));
        assertEquals(0, SuperSploorRules.territoryPaintTransition(-2, 0));
        assertEquals(0, SuperSploorRules.territoryPaintTransition(-1, 2));
    }

    @Test
    void fourMarksAreRequiredForASplat() {
        assertEquals(1, SuperSploorRules.markCountAfterHit(0));
        assertEquals(3, SuperSploorRules.markCountAfterHit(2));
        assertFalse(SuperSploorRules.isSplatted(3));
        assertTrue(SuperSploorRules.isSplatted(4));
        assertEquals(4, SuperSploorRules.markCountAfterHit(99));
    }

    @Test
    void markRecoveryRequiresFriendlyTerritoryAndBothDeadlines() {
        long hit = 10_000;
        assertFalse(SuperSploorRules.canRecoverMark(18_000, hit, 20_000, true, 2));
        assertFalse(SuperSploorRules.canRecoverMark(20_000, hit, 20_000, false, 2));
        assertTrue(SuperSploorRules.canRecoverMark(20_000, hit, 20_000, true, 2));
    }

    @Test
    void squidAndNormalInkRegenerationHaveDifferentRates() {
        assertEquals(56, SuperSploorRules.nextInkAfterRegeneration(50, 1, false));
        assertEquals(80, SuperSploorRules.nextInkAfterRegeneration(50, 1, true));
        assertEquals(100, SuperSploorRules.nextInkAfterRegeneration(98, 5, true));
    }

    @Test
    void chargerChargeControlsInkCostAndProjectileSpeed() {
        assertEquals(0.2D, SuperSploorRules.chargerChargeFraction(0), 0.0001D);
        assertEquals(0.5D, SuperSploorRules.chargerChargeFraction(450), 0.0001D);
        assertEquals(1.0D, SuperSploorRules.chargerChargeFraction(2_000), 0.0001D);
        assertEquals(10, SuperSploorRules.chargerInkCost(0.2D));
        assertEquals(13, SuperSploorRules.chargerInkCost(0.5D));
        assertEquals(18, SuperSploorRules.chargerInkCost(1.0D));
        assertEquals(3.0D, SuperSploorRules.chargerProjectileSpeed(1.0D), 0.0001D);
    }

    @Test
    void specialMeterCapsAndDoesNotChargeFromItsOwnAbility() {
        assertEquals(12.5D, SuperSploorRules.nextSpecialCharge(10.0D, 25, false), 0.0001D);
        assertEquals(72.0D, SuperSploorRules.nextSpecialCharge(72.0D, 50, true), 0.0001D);
        assertEquals(100.0D, SuperSploorRules.nextSpecialCharge(99.0D, 50, false), 0.0001D);
        assertEquals(100.0D, SuperSploorRules.specialChargeAfterSplat(98.0D), 0.0001D);
        assertTrue(SuperSploorRules.canUseSpecial(100.0D));
        assertFalse(SuperSploorRules.canUseSpecial(99.99D));
    }

    @Test
    void dodgeRollChargesAreBoundedAndSpendOneAtATime() {
        assertEquals(1, SuperSploorRules.dodgeRollChargesAfterUse(2));
        assertEquals(0, SuperSploorRules.dodgeRollChargesAfterUse(0));
        assertEquals(2, SuperSploorRules.nextDodgeRollCharge(1));
        assertEquals(2, SuperSploorRules.nextDodgeRollCharge(8));
    }

    @Test
    void assistsOnlyCountRecentContributionWithinEightSeconds() {
        assertTrue(SuperSploorRules.isAssistEligible(20_000L, 12_000L));
        assertTrue(SuperSploorRules.isAssistEligible(20_000L, 19_999L));
        assertFalse(SuperSploorRules.isAssistEligible(20_001L, 12_000L));
        assertFalse(SuperSploorRules.isAssistEligible(20_000L, 0L));
        assertFalse(SuperSploorRules.isAssistEligible(10_000L, 11_000L));
    }

    @Test
    void spawnRoomFootprintIsExcludedFromTerritory() {
        assertTrue(SuperSploorRules.isSpawnSafeTerritoryBlock(10, 64, 10, 10, 64, 10));
        assertTrue(SuperSploorRules.isSpawnSafeTerritoryBlock(13, 67, 10, 10, 64, 10));
        assertFalse(SuperSploorRules.isSpawnSafeTerritoryBlock(14, 64, 10, 10, 64, 10));
        assertFalse(SuperSploorRules.isSpawnSafeTerritoryBlock(10, 68, 10, 10, 64, 10));
        assertFalse(SuperSploorRules.isSpawnSafeTerritoryBlock(10, 61, 10, 10, 64, 10));
    }

    @Test
    void specialMarksRequireAnAliveUnprotectedOpponent() {
        assertTrue(SuperSploorRules.canMarkTarget(0, 1, false, 10_000L, 8_000L));
        assertFalse(SuperSploorRules.canMarkTarget(0, 0, false, 10_000L, 8_000L));
        assertFalse(SuperSploorRules.canMarkTarget(0, 1, true, 10_000L, 8_000L));
        assertFalse(SuperSploorRules.canMarkTarget(0, 1, false, 7_999L, 8_000L));
        assertFalse(SuperSploorRules.canMarkTarget(-1, 1, false, 10_000L, 8_000L));
    }

    @Test
    void inkRegenerationWaitsAfterPrimaryOrSecondaryUseUnlessSwimmingOnFriendlyPaint() {
        assertFalse(SuperSploorRules.canRegenerateInk(10_999L, 10_000L, false, false));
        assertTrue(SuperSploorRules.canRegenerateInk(11_000L, 10_000L, false, false));
        assertTrue(SuperSploorRules.canRegenerateInk(10_100L, 10_000L, true, true));
        assertFalse(SuperSploorRules.canRegenerateInk(10_100L, 10_000L, true, false));
        assertFalse(SuperSploorRules.canRegenerateInk(9_000L, 10_000L, false, false));
    }

    @Test
    void profileOverridesChangeProductionInkChargerSpecialAndMarkRulesThenRestoreDefaults() {
        assertEquals(4, SuperSploorRules.primaryInkCost("roller"));
        assertEquals(550L, SuperSploorRules.primaryIntervalMillis("roller"));
        assertEquals(30, SuperSploorRules.secondaryInkCost("roller"));
        assertEquals(4_000L, SuperSploorRules.secondaryCooldownMillis());
        GameDebugRuntimeOverrides.activate(Map.ofEntries(
                Map.entry("SUPER_SPLOOR_MAX_INK", 120),
                Map.entry("SUPER_SPLOOR_INK_PER_PRIMARY_SHOT", 5),
                Map.entry("SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS", 100L),
                Map.entry("SUPER_SPLOOR_NORMAL_INK_REGEN_PER_SECOND", 9),
                Map.entry("SUPER_SPLOOR_INK_REGEN_INTERVAL_MILLIS", 500L),
                Map.entry("SUPER_SPLOOR_MAX_PAINT_MARKS", 2),
                Map.entry("SUPER_SPLOOR_MARK_RECOVERY_DELAY_MILLIS", 3_000L),
                Map.entry("SUPER_SPLOOR_CHARGER_FULL_CHARGE_MILLIS", 450L),
                Map.entry("SUPER_SPLOOR_CHARGER_MIN_INK_COST", 2),
                Map.entry("SUPER_SPLOOR_CHARGER_MAX_INK_COST", 12),
                Map.entry("SUPER_SPLOOR_CHARGER_MIN_SPEED", 0.5D),
                Map.entry("SUPER_SPLOOR_CHARGER_MAX_SPEED", 1.5D),
                Map.entry("SUPER_SPLOOR_SPECIAL_CHARGE_PER_TERRITORY_UNIT", 2.0D),
                Map.entry("SUPER_SPLOOR_SPECIAL_CHARGE_PER_SPLAT", 10.0D),
                Map.entry("SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED", 50.0D),
                Map.entry("SUPER_SPLOOR_DODGE_ROLL_MAX_CHARGES", 3),
                Map.entry("SUPER_SPLOOR_ROLLER_PRIMARY_INK_COST", 8),
                Map.entry("SUPER_SPLOOR_ROLLER_PRIMARY_INTERVAL_MILLIS", 300L),
                Map.entry("SUPER_SPLOOR_ROLLER_SECONDARY_INK_COST", 40),
                Map.entry("SUPER_SPLOOR_SECONDARY_COOLDOWN_MILLIS", 2_000L)));

        assertEquals(120, SuperSploorRules.clampInk(150));
        assertTrue(SuperSploorRules.canFire(5, 100L, 0L));
        assertEquals(59, SuperSploorRules.nextInkAfterRegeneration(50, 1, false));
        assertTrue(SuperSploorRules.canRegenerateInk(10_500L, 10_000L, false, false));
        assertEquals(2, SuperSploorRules.markCountAfterHit(1));
        assertTrue(SuperSploorRules.isSplatted(2));
        assertFalse(SuperSploorRules.canRecoverMark(12_999L, 10_000L, 12_999L, true, 2));
        assertEquals(0.5D, SuperSploorRules.chargerChargeFraction(225L), 0.0001D);
        assertEquals(12, SuperSploorRules.chargerInkCost(1.0D));
        assertEquals(1.0D, SuperSploorRules.chargerProjectileSpeed(0.5D), 0.0001D);
        assertEquals(30.0D, SuperSploorRules.nextSpecialCharge(10.0D, 10, false), 0.0001D);
        assertTrue(SuperSploorRules.canUseSpecial(50.0D));
        assertEquals(50.0D, SuperSploorRules.specialChargeAfterSplat(40.0D), 0.0001D);
        assertEquals(3, SuperSploorRules.nextDodgeRollCharge(2));
        assertEquals(8, SuperSploorRules.primaryInkCost("roller"));
        assertEquals(300L, SuperSploorRules.primaryIntervalMillis("roller"));
        assertEquals(40, SuperSploorRules.secondaryInkCost("roller"));
        assertEquals(2_000L, SuperSploorRules.secondaryCooldownMillis());

        GameDebugRuntimeOverrides.clear();
        assertEquals(100, SuperSploorRules.clampInk(150));
        assertEquals(56, SuperSploorRules.nextInkAfterRegeneration(50, 1, false));
        assertEquals(4, SuperSploorRules.markCountAfterHit(3));
        assertEquals(10, SuperSploorRules.chargerInkCost(0.2D));
        assertEquals(100.0D, SuperSploorRules.nextSpecialCharge(99.0D, 50, false), 0.0001D);
        assertEquals(4, SuperSploorRules.primaryInkCost("roller"));
        assertEquals(550L, SuperSploorRules.primaryIntervalMillis("roller"));
        assertEquals(30, SuperSploorRules.secondaryInkCost("roller"));
        assertEquals(4_000L, SuperSploorRules.secondaryCooldownMillis());
    }

    @Test
    void overtimeRequiresAOnePercentLeadAndSquidTravelRejectsTeleports() {
        assertEquals(10, SuperSploorRules.overtimeLeadThreshold(1_000));
        assertEquals(2, SuperSploorRules.overtimeLeadThreshold(150));
        assertEquals(1, SuperSploorRules.overtimeLeadThreshold(100));
        assertFalse(SuperSploorRules.hasDecisiveOvertimeLead(500, 509, 1_000));
        assertTrue(SuperSploorRules.hasDecisiveOvertimeLead(500, 510, 1_000));
        assertFalse(SuperSploorRules.hasDecisiveOvertimeLead(50, 51, 150));
        assertTrue(SuperSploorRules.hasDecisiveOvertimeLead(50, 52, 150));
        assertTrue(SuperSploorRules.shouldCountSquidTravel(1.5, false));
        assertFalse(SuperSploorRules.shouldCountSquidTravel(200, false));
        assertFalse(SuperSploorRules.shouldCountSquidTravel(1.5, true));
    }
}
