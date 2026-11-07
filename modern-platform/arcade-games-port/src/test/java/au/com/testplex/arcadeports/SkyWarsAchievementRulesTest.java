package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkyWarsAchievementRulesTest {
    @Test void sourceAchievementThresholdsMatchTheMineplexCatalogue() {
        assertEquals(20, SkyWarsAchievementRules.SKY_KING_WINS);
        assertEquals(100, SkyWarsAchievementRules.TNT_HOARDER_PICKUPS);
        assertEquals(120, SkyWarsAchievementRules.LEFT_FOR_DEAD_ZOMBIES);
        assertEquals(80, SkyWarsAchievementRules.COLD_BLOODED_KILLS);
        assertFalse(SkyWarsAchievementRules.reached(99, 100));
        assertTrue(SkyWarsAchievementRules.reached(100, 100));
    }

    @Test void abilityChallengesRequireTheApprovedMatchConditions() {
        assertFalse(SkyWarsAchievementRules.secondChance(100, 80, false));
        assertFalse(SkyWarsAchievementRules.secondChance(100, 81, true));
        assertTrue(SkyWarsAchievementRules.secondChance(100, 80, true));
        assertFalse(SkyWarsAchievementRules.crossedBridge(16, false));
        assertTrue(SkyWarsAchievementRules.crossedBridge(16, true));
        assertFalse(SkyWarsAchievementRules.hasFullMetalArmor(3));
        assertTrue(SkyWarsAchievementRules.hasFullMetalArmor(4));
        assertFalse(SkyWarsAchievementRules.highGroundElimination(119.9, 100));
        assertTrue(SkyWarsAchievementRules.highGroundElimination(120, 100));
    }

    @Test void fireSaleRequiresTheSameOwnersFireBurstToReceiveTheKillCredit() {
        UUID owner = UUID.randomUUID();
        assertTrue(SkyWarsAchievementRules.fireBurstElimination(owner, owner, "Fire Burst", true));
        assertFalse(SkyWarsAchievementRules.fireBurstElimination(owner, UUID.randomUUID(), "Fire Burst", true));
        assertFalse(SkyWarsAchievementRules.fireBurstElimination(owner, owner, "Sword", true));
        assertFalse(SkyWarsAchievementRules.fireBurstElimination(owner, owner, "Fire Burst", false));
    }

    @Test void distinctVictimsCannotBeDuplicatedToCompleteMatchChallenges() {
        SkyWarsAchievementRules.MatchTracker fireBurst = new SkyWarsAchievementRules.MatchTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertFalse(fireBurst.recordFireBurstElimination(first));
        assertFalse(fireBurst.recordFireBurstElimination(first));
        assertTrue(fireBurst.recordFireBurstElimination(second));

        SkyWarsAchievementRules.MatchTracker oneTnt = new SkyWarsAchievementRules.MatchTracker();
        assertFalse(oneTnt.recordThrowableTntElimination(first));
        assertTrue(oneTnt.recordThrowableTntElimination(second));
        assertEquals(2, oneTnt.tntVictims());
    }

    @Test void earthAndHighGroundChallengesCountDistinctVictims() {
        SkyWarsAchievementRules.MatchTracker tracker = new SkyWarsAchievementRules.MatchTracker();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        assertFalse(tracker.recordDirtVoidElimination(first));
        assertTrue(tracker.recordDirtVoidElimination(second));
        assertFalse(tracker.recordHighGroundElimination(first, 120, 100));
        assertFalse(tracker.recordHighGroundElimination(first, 120, 100));
        assertFalse(tracker.recordHighGroundElimination(second, 120, 100));
        assertTrue(tracker.recordHighGroundElimination(third, 120, 100));
        tracker.recordSecondChance(true);
        tracker.recordBridgeCrossing(true);
        tracker.recordMagnetVoidElimination(true);
        assertTrue(tracker.secondChance());
        assertTrue(tracker.bridgeCrossed());
        assertTrue(tracker.magnetVoidElimination());
    }
}
