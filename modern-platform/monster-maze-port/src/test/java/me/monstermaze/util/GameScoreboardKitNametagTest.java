package me.monstermaze.util;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameScoreboardKitNametagTest {
    @Test
    void usesShortStablePerPlayerTeamNamesForViewerSpecificSuffixes() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String a = GameScoreboard.kitNametagTeamName(first);
        String b = GameScoreboard.kitNametagTeamName(second);
        assertTrue(a.startsWith("mmk_"));
        assertTrue(a.length() <= 16);
        assertNotEquals(a, b);
        assertEquals(a, GameScoreboard.kitNametagTeamName(first));
    }
}
