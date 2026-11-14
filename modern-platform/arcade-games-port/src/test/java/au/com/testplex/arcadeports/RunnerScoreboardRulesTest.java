package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunnerScoreboardRulesTest {
    @Test
    void listsRunnersGreenWhenAliveAndGrayWhenEliminatedWithoutTeamOrTimerLines() {
        assertEquals(List.of("§aAlex", "§7Blake"), RunnerScoreboardRules.lines(List.of(
                new RunnerScoreboardRules.PlayerEntry("Alex", true),
                new RunnerScoreboardRules.PlayerEntry("Blake", false))));
    }

    @Test
    void listsOnlyAliveRunnersWhenAtLeastOneOfSixteenIsEliminated() {
        List<RunnerScoreboardRules.PlayerEntry> players = java.util.stream.IntStream.range(0, 16)
                .mapToObj(index -> new RunnerScoreboardRules.PlayerEntry("Runner" + index, index < 15)).toList();

        assertEquals(15, RunnerScoreboardRules.lines(players).size());
        assertEquals("§aRunner14", RunnerScoreboardRules.lines(players).get(14));
    }

    @Test
    void showsSourceAliveAndDeadCountsWhenAllSixteenAreAlive() {
        List<RunnerScoreboardRules.PlayerEntry> players = java.util.stream.IntStream.range(0, 16)
                .mapToObj(index -> new RunnerScoreboardRules.PlayerEntry("Runner" + index, true)).toList();

        assertEquals(List.of("§aPlayers Alive", "16", "§cPlayers Dead", "0"),
                RunnerScoreboardRules.lines(players));
    }
}
