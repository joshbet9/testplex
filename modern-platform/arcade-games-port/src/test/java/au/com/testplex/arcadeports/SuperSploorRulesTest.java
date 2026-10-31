package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SuperSploorRulesTest {
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