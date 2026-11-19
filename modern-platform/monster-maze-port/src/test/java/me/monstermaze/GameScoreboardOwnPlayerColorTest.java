package me.monstermaze;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.monstermaze.util.GameScoreboard;
import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

class GameScoreboardOwnPlayerColorTest {
    @Test
    void ownNameAndSafePlayersAreGreenWhileOtherUnsafePlayersKeepYellow() {
        assertEquals(ChatColor.GREEN, GameScoreboard.playerNameColor(false, true));
        assertEquals(ChatColor.GREEN, GameScoreboard.playerNameColor(true, false));
        assertEquals(ChatColor.YELLOW, GameScoreboard.playerNameColor(false, false));
    }
}
