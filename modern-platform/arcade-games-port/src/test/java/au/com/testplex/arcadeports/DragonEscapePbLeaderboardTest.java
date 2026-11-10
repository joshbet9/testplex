package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DragonEscapePbLeaderboardTest {
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test void sortsFastestFirstAndPreservesCompetitionTies() {
        var rows = DragonEscapePbLeaderboard.rank(List.of(
                new DragonEscapePbLeaderboard.Run(a, 10_000L, "jumper"),
                new DragonEscapePbLeaderboard.Run(b, 8_000L, "builder"),
                new DragonEscapePbLeaderboard.Run(c, 8_000L, "warper")), a, Set.of(), false);
        assertEquals(List.of(1, 1, 3), rows.stream().map(DragonEscapePbLeaderboard.RankedRun::rank).toList());
        assertEquals(a, rows.get(2).playerId());
    }

    @Test void friendsBoardIncludesTheViewerAndFollowedPlayersOnly() {
        var runs = List.of(new DragonEscapePbLeaderboard.Run(a, 12_000L, "jumper"),
                new DragonEscapePbLeaderboard.Run(b, 8_000L, "jumper"),
                new DragonEscapePbLeaderboard.Run(c, 9_000L, "jumper"));
        var rows = DragonEscapePbLeaderboard.rank(runs, a, Set.of(c), true);
        assertEquals(List.of(c, a), rows.stream().map(DragonEscapePbLeaderboard.RankedRun::playerId).toList());
    }

    @Test void ignoresInvalidTimesAndDeduplicatesTheSamePlayer() {
        var rows = DragonEscapePbLeaderboard.rank(List.of(
                new DragonEscapePbLeaderboard.Run(a, 0L, "jumper"),
                new DragonEscapePbLeaderboard.Run(a, 11_000L, "jumper"),
                new DragonEscapePbLeaderboard.Run(a, 9_000L, "warper"),
                new DragonEscapePbLeaderboard.Run(b, -4L, "jumper")), a, Set.of(), false);
        assertEquals(1, rows.size());
        assertEquals(9_000L, rows.getFirst().elapsedMillis());
    }
}
