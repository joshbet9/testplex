package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaintballScoreboardLayoutTest {
    private record RosterPlayer(String name, int team) {
    }

    @Test
    void listsTeamPlayersInTheirSourceRosterOrder() {
        List<RosterPlayer> joined = List.of(
                new RosterPlayer("Zed", 0), new RosterPlayer("Amy", 1),
                new RosterPlayer("Bo", 0), new RosterPlayer("Cal", 1));
        List<String> names = new ArrayList<>();

        PaintballScoreboardLayout.forEachInRosterOrder(joined, RosterPlayer::team, 0,
                player -> names.add(player.name()));

        assertEquals(List.of("Zed", "Bo"), names);
    }
}
