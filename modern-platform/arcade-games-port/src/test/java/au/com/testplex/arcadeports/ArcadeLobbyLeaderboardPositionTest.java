package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcadeLobbyLeaderboardPositionTest {
    @Test void dragonEscapePanelSitsNearTheLobbyFloor() {
        double first = ArcadeLobbyLeaderboardPosition.rowY("dragon-escape", 0);
        double last = ArcadeLobbyLeaderboardPosition.rowY("dragon-escape", 14);

        assertEquals(104.9D, first);
        assertTrue(last > 100.0D, "The last display row must stay above the lobby floor at Y=100");
        assertEquals(102.7D, ArcadeLobbyLeaderboardPosition.hitboxY("dragon-escape"));
    }

    @Test void otherGamesKeepTheirExistingLeaderboardHeight() {
        assertEquals(106.8D, ArcadeLobbyLeaderboardPosition.rowY("bacon-brawl", 0));
        assertEquals(102.32D, ArcadeLobbyLeaderboardPosition.rowY("bacon-brawl", 14));
        assertEquals(104.2D, ArcadeLobbyLeaderboardPosition.hitboxY("bacon-brawl"));
    }
}
