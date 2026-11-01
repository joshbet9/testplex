package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyWarsMixedArcadeRulesTest {
    @Test void mixedSkyWarsCapsLobbiesAtSixteen() {
        assertEquals(16, SkyWarsMixedArcadeRules.acceptedPlayers(16));
        assertEquals(16, SkyWarsMixedArcadeRules.acceptedPlayers(20));
        assertEquals(1, SkyWarsMixedArcadeRules.acceptedPlayers(1));
        assertEquals(0, SkyWarsMixedArcadeRules.acceptedPlayers(-1));
        assertTrue(SkyWarsMixedArcadeRules.hasRoom(15));
        assertFalse(SkyWarsMixedArcadeRules.hasRoom(16));
    }

    @Test void mixedSkyWarsUsesShorterRoundCadence() {
        long start = 1_000_000L;
        assertEquals(start + 5_000L, SkyWarsMixedArcadeRules.liveStartsAt(start));
        assertEquals(start + 65_000L, SkyWarsMixedArcadeRules.crumbleStartsAt(start));
        assertEquals(start + 20_000L, SkyWarsMixedArcadeRules.firstTntAt(start));
        assertEquals(start + 485_000L, SkyWarsMixedArcadeRules.roundEndsAt(start));
        assertEquals(45_000L, SkyWarsMixedArcadeRules.BORDER_SHRINK_MILLIS);
        assertEquals(32.0D, SkyWarsMixedArcadeRules.FINAL_BORDER_SIZE);
        assertEquals(60L, SkyWarsMixedArcadeRules.TNT_THROW_FUSE_TICKS);
        assertEquals(500L, SkyWarsMixedArcadeRules.TNT_THROW_COOLDOWN_MILLIS);
        assertEquals(1.6D, SkyWarsMixedArcadeRules.TNT_THROW_STRENGTH);
        assertEquals(2, SkyWarsMixedArcadeRules.TNT_PER_DROP);
    }
}
