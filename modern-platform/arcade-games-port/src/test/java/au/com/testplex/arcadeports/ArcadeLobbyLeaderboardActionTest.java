package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeLobbyLeaderboardActionTest {
    @Test
    void legacyAndUnknownSettingsKeepThePreviousClickCyclingBehavior() {
        assertEquals(ArcadeLobbyLeaderboardAction.DEFAULT_CYCLE,
                ArcadeLobbyLeaderboardAction.byId(null, ArcadeLobbyLeaderboardAction.DEFAULT_CYCLE));
        assertEquals(ArcadeLobbyLeaderboardAction.DEFAULT_CYCLE,
                ArcadeLobbyLeaderboardAction.byId("unknown", ArcadeLobbyLeaderboardAction.DEFAULT_CYCLE));
        assertEquals(ArcadeLobbyLeaderboardAction.CYCLE_STAT,
                ArcadeLobbyLeaderboardAction.byId("cycle-stat", ArcadeLobbyLeaderboardAction.NONE));
    }

    @Test
    void dragonEscapePbOffersMapAndKitActionsButOtherBoardsDoNot() {
        assertTrue(ArcadeLobbyLeaderboardAction.choices(true).contains(ArcadeLobbyLeaderboardAction.CYCLE_MAP));
        assertTrue(ArcadeLobbyLeaderboardAction.choices(true).contains(ArcadeLobbyLeaderboardAction.CYCLE_KIT));
        assertFalse(ArcadeLobbyLeaderboardAction.choices(false).contains(ArcadeLobbyLeaderboardAction.CYCLE_MAP));
        assertFalse(ArcadeLobbyLeaderboardAction.choices(false).contains(ArcadeLobbyLeaderboardAction.CYCLE_KIT));
        assertTrue(ArcadeLobbyLeaderboardAction.choices(false).contains(ArcadeLobbyLeaderboardAction.CYCLE_STAT));
        assertTrue(ArcadeLobbyLeaderboardAction.choices(false).contains(ArcadeLobbyLeaderboardAction.NONE));
    }
}
