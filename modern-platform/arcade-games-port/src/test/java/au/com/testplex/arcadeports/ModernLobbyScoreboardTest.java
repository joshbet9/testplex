package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernLobbyScoreboardTest {
    @Test
    void rendersOnlyTheOriginalFourArcadeScoreboardSections() {
        List<String> lines = ModernLobbyScoreboard.sidebarLines(
                "2/16", "Runner Basher", "0", "Testplex Modern");

        assertEquals(12, lines.size());
        assertEquals(12, new HashSet<>(lines).size());

        assertEquals(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Players", lines.get(1));
        assertEquals(ChatColor.WHITE + "2/16", lines.get(2));
        assertEquals(ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit", lines.get(4));
        assertEquals(ChatColor.WHITE + "Runner Basher", lines.get(5));
        assertEquals(ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems", lines.get(7));
        assertEquals(ChatColor.WHITE + "0", lines.get(8));
        assertEquals(ChatColor.AQUA.toString() + ChatColor.BOLD + "Server", lines.get(10));
        assertEquals(ChatColor.WHITE + "Testplex Modern", lines.get(11));

        String visible = String.join("|", lines.stream().map(ChatColor::stripColor).toList());
        assertTrue(visible.contains("Players"));
        assertTrue(visible.contains("Kit"));
        assertTrue(visible.contains("Gems"));
        assertTrue(visible.contains("Server"));
        assertTrue(!visible.contains("Shards"));
        assertTrue(!visible.contains("Rank"));
        assertTrue(!visible.contains("Website"));
        assertTrue(!visible.contains("Game"));
    }

    @Test
    void titleMatchesTheOriginalArcadeLifecycleStates() {
        assertEquals(ChatColor.GREEN.toString() + ChatColor.BOLD + "Waiting for players",
                ModernLobbyScoreboard.scoreboardTitleForStatus("Waiting for players"));
        assertEquals(ChatColor.BOLD + "Starting in "
                        + ChatColor.GREEN + ChatColor.BOLD + "10 Seconds",
                ModernLobbyScoreboard.scoreboardTitleForStatus("Starting in 10 Seconds"));
        assertEquals(ChatColor.BOLD + "Vote ends in "
                        + ChatColor.GREEN + ChatColor.BOLD + "15",
                ModernLobbyScoreboard.scoreboardTitleForStatus("Vote ends in 15"));
        assertEquals(ChatColor.GREEN.toString() + ChatColor.BOLD + "Loading...",
                ModernLobbyScoreboard.scoreboardTitleForStatus("Loading..."));
        assertEquals(ChatColor.GREEN.toString() + ChatColor.BOLD + "In Progress...",
                ModernLobbyScoreboard.scoreboardTitleForStatus("In Progress..."));
    }

    @Test
    void tabListRankOrderMatchesLegacyPriority() {
        assertEquals("r0_owner", ModernLobbyScoreboard.tabSortKey("owner"));
        assertEquals("r1_admin", ModernLobbyScoreboard.tabSortKey("admin"));
        assertEquals("r2_mod", ModernLobbyScoreboard.tabSortKey("mod"));
        assertEquals("r3_helper", ModernLobbyScoreboard.tabSortKey("helper"));
        assertEquals("r4_ultra", ModernLobbyScoreboard.tabSortKey("ultra"));
        assertEquals("r5_player", ModernLobbyScoreboard.tabSortKey("player"));
    }

    @Test
    void playerCountIsFormattedLikeTheOriginalPlayersValue() {
        assertEquals("0/16", ModernLobbyScoreboard.formatPlayerCount(0, 16));
        assertEquals("3/16", ModernLobbyScoreboard.formatPlayerCount(3, 16));
        assertEquals("20/16", ModernLobbyScoreboard.formatPlayerCount(20, 16));
        assertEquals("0/1", ModernLobbyScoreboard.formatPlayerCount(-2, 0));
    }

    @Test
    void selectedKitUsesReadableNamesAndTheLegacyNoneFallback() {
        assertEquals("El Muchacho Pigo", ModernLobbyScoreboard.formatKitName("el-muchacho-pigo"));
        assertEquals("Runner Basher", ModernLobbyScoreboard.formatKitName("runner-basher"));
        assertEquals("None", ModernLobbyScoreboard.formatKitName("None"));
        assertEquals("None", ModernLobbyScoreboard.formatKitName(""));
    }
}
