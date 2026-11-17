package au.com.testplex.snake;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnakeScoreboardLayoutTest {
    @Test
    void sourceListsAliveAndEliminatedPlayersInSmallLobbies() {
        assertEquals(List.of(ChatColor.GREEN + "A", ChatColor.GRAY + "B"),
                SnakeScoreboardLayout.lines(List.of(
                        new SnakeScoreboardLayout.Participant("A", true),
                        new SnakeScoreboardLayout.Participant("B", false))));
    }

    @Test
    void sourceListsLivingPlayersForMediumLobbies() {
        List<SnakeScoreboardLayout.Participant> roster = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> new SnakeScoreboardLayout.Participant("P" + i, i < 14))
                .toList();

        List<String> lines = SnakeScoreboardLayout.lines(roster);

        assertEquals(14, lines.size());
        assertEquals(ChatColor.GREEN + "P13", lines.get(13));
    }

    @Test
    void sourceShowsAliveAndDeadTotalsForSixteenLivingPlayers() {
        List<SnakeScoreboardLayout.Participant> roster = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> new SnakeScoreboardLayout.Participant("P" + i, true))
                .toList();

        assertEquals(List.of(ChatColor.GREEN + "Players Alive", ChatColor.WHITE + "16", " ",
                        ChatColor.RED + "Players Dead", ChatColor.WHITE + "0"),
                SnakeScoreboardLayout.lines(roster));
    }
}
