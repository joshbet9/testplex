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

        // Fake/test players may have identical UUID prefixes but different tails.
        UUID botA = UUID.fromString("00000000-0000-4000-8000-000000000101");
        UUID botB = UUID.fromString("00000000-0000-4000-8000-000000000102");
        assertNotEquals(GameScoreboard.kitNametagTeamName(botA),
                GameScoreboard.kitNametagTeamName(botB));
    }
}
