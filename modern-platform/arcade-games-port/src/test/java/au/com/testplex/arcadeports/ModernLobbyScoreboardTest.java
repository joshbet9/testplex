package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernLobbyScoreboardTest {
    @Test
    void everyGameUsesTheSameFifteenLineSidebarStructure() {
        List<String> deathTag = ModernLobbyScoreboard.sidebarLines(
                "Testplex Modern", "Death Tag", "Runner Basher", "0", "0", "Owner", "www.mineplex.com");
        List<String> baconBrawl = ModernLobbyScoreboard.sidebarLines(
                "Testplex Modern", "Bacon Brawl", "El Muchacho Pigo", "0", "0", "Owner", "www.mineplex.com");

        assertEquals(15, deathTag.size());
        assertEquals(15, baconBrawl.size());
        assertEquals(15, new HashSet<>(deathTag).size());
        assertEquals(15, new HashSet<>(baconBrawl).size());
        assertEquals(ChatColor.AQUA + "Server: " + ChatColor.WHITE + "Testplex Modern", deathTag.get(0));
        assertEquals(ChatColor.GREEN.toString() + ChatColor.BOLD + "Game", deathTag.get(2));
        assertEquals(ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit", deathTag.get(4));
        assertEquals(ChatColor.RED + "Website: " + ChatColor.DARK_AQUA + "www.mineplex.com", deathTag.get(14));
    }

    @Test
    void scoreboardHeaderDoesNotChangeForBaconBrawlOrOtherGames() {
        String defaultTitle = "Testplex";
        String expected = ChatColor.WHITE.toString() + ChatColor.BOLD + "TESTPLEX";

        assertEquals(expected, ModernLobbyScoreboard.scoreboardTitleForGame("bacon-brawl", defaultTitle));
        assertEquals(expected, ModernLobbyScoreboard.scoreboardTitleForGame("death-tag", defaultTitle));
        assertEquals(expected, ModernLobbyScoreboard.scoreboardTitleForGame("bomb-lobbers", defaultTitle));
    }

    @Test
    void rankFormattingKeepsOwnerHigherPriorityThanAdmin() {
        assertEquals("Owner", ModernLobbyScoreboard.rankName(true, true));
        assertEquals("Admin", ModernLobbyScoreboard.rankName(false, true));
        assertEquals("No Rank", ModernLobbyScoreboard.rankName(false, false));
    }

    @Test
    void baconBrawlAndOtherGamesShareTheSameLineCount() {
        assertEquals(
                ModernLobbyScoreboard.sidebarLines("Testplex Modern", "Bacon Brawl", "Default", "0", "0", "Admin", "www.mineplex.com").size(),
                ModernLobbyScoreboard.sidebarLines("Testplex Modern", "Death Tag", "Default", "0", "0", "Admin", "www.mineplex.com").size());
    }
}
