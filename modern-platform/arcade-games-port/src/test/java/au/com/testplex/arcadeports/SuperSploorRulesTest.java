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
    void overtimeRequiresAOnePercentLeadAndSquidTravelRejectsTeleports() {
        assertEquals(10, SuperSploorRules.overtimeLeadThreshold(1_000));
        assertFalse(SuperSploorRules.hasDecisiveOvertimeLead(500, 509, 1_000));
        assertTrue(SuperSploorRules.hasDecisiveOvertimeLead(500, 510, 1_000));
        assertTrue(SuperSploorRules.shouldCountSquidTravel(1.5, false));
        assertFalse(SuperSploorRules.shouldCountSquidTravel(200, false));
        assertFalse(SuperSploorRules.shouldCountSquidTravel(1.5, true));
    }
}
