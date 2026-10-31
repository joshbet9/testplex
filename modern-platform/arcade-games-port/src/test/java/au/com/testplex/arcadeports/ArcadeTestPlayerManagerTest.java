package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcadeTestPlayerManagerTest {
    @Test
    void fillsEachGameToSixteenTotalPlayersRatherThanAddingSixteenExtraBots() {
        assertEquals(15, ArcadeTestPlayerManager.additionalPlayersNeeded(1));
        assertEquals(14, ArcadeTestPlayerManager.additionalPlayersNeeded(2));
        assertEquals(1, ArcadeTestPlayerManager.additionalPlayersNeeded(15));
        assertEquals(0, ArcadeTestPlayerManager.additionalPlayersNeeded(16));
        assertEquals(0, ArcadeTestPlayerManager.additionalPlayersNeeded(20));
    }

    @Test
    void emptyRosterNeedsSixteenBotsAndNegativeCountsAreTreatedAsEmpty() {
        assertEquals(16, ArcadeTestPlayerManager.additionalPlayersNeeded(0));
        assertEquals(16, ArcadeTestPlayerManager.additionalPlayersNeeded(-3));
    }
}
