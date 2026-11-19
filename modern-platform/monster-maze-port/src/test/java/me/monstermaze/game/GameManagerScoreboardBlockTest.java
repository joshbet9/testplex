package me.monstermaze.game;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerScoreboardBlockTest {
    @Test
    void movementWithinTheSameBlockDoesNotRebuildDistanceStandings() {
        Location before = new Location(null, 10.01D, 64.01D, -4.99D, 0.0F, 0.0F);
        Location after = new Location(null, 10.99D, 64.99D, -4.01D, 180.0F, 30.0F);
        assertTrue(GameManager.sameScoreboardBlock(before, after));
    }

    @Test
    void crossingAnyBlockBoundaryRefreshesStandings() {
        Location before = new Location(null, 10.99D, 64.0D, -4.01D);
        assertFalse(GameManager.sameScoreboardBlock(before,
                new Location(null, 11.01D, 64.0D, -4.01D)));
        assertFalse(GameManager.sameScoreboardBlock(before,
                new Location(null, 10.99D, 65.0D, -4.01D)));
        assertFalse(GameManager.sameScoreboardBlock(before,
                new Location(null, 10.99D, 64.0D, -5.01D)));
        assertFalse(GameManager.sameScoreboardBlock(null, before));
    }
}
