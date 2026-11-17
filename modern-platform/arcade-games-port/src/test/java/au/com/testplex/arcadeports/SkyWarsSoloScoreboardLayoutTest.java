package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyWarsSoloScoreboardLayoutTest {
    @Test
    void sourceListsAlivePlayersThroughTenThenShowsOnlyTheirCount() {
        List<String> ten = IntStream.rangeClosed(1, 10).mapToObj(i -> "Player" + i).toList();
        List<String> eleven = IntStream.rangeClosed(1, 11).mapToObj(i -> "Player" + i).toList();

        List<String> tenLines = SkyWarsSoloScoreboardLayout.playersSection(ten);
        List<String> elevenLines = SkyWarsSoloScoreboardLayout.playersSection(eleven);

        assertTrue(tenLines.get(0).contains(ChatColor.BOLD.toString()));
        assertEquals(ChatColor.WHITE + "Player10", tenLines.get(10));
        assertEquals(List.of(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Players",
                ChatColor.WHITE + "11"), elevenLines);
    }
}
