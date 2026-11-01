package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaconBlastComboAchievementRulesTest {
    @Test
    void mamaCameraCountsDirectBlastKillsAndSameOwnerFollowUpComboKills() {
        UUID mama = UUID.randomUUID();
        assertTrue(ArcadePortRules.shouldCountMamaCameraBlastKill(
                true, true, mama, null, 10_000L, 0L));
        assertTrue(ArcadePortRules.shouldCountMamaCameraBlastKill(
                true, false, mama, mama, 14_999L, 10_000L));
        assertFalse(ArcadePortRules.shouldCountMamaCameraBlastKill(
                true, false, mama, UUID.randomUUID(), 14_999L, 10_000L));
        assertFalse(ArcadePortRules.shouldCountMamaCameraBlastKill(
                true, false, mama, mama, 15_001L, 10_000L));
        assertFalse(ArcadePortRules.shouldCountMamaCameraBlastKill(
                false, true, mama, mama, 10_000L, 10_000L));
        assertFalse(ArcadePortRules.shouldCountMamaCameraBlastKill(
                true, false, null, mama, 10_000L, 10_000L));
    }
}
