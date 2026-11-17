package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GladiatorsOpeningSeatPlanTest {
    @Test
    void seatsEveryReadyOpeningDuelInItsOwnArenaBeforeTheVoteResolves() {
        List<UUID> players = IntStream.range(0, 16).mapToObj(index -> UUID.randomUUID()).toList();
        GladiatorsBracket bracket = new GladiatorsBracket(players);

        List<GladiatorsOpeningSeatPlan.Seat> seats = GladiatorsOpeningSeatPlan.create(bracket.readyMatches(), 8);

        assertEquals(8, seats.size());
        assertEquals(8, seats.stream().map(GladiatorsOpeningSeatPlan.Seat::arenaIndex).distinct().count());
        List<UUID> seatedPlayers = seats.stream()
                .flatMap(seat -> List.of(seat.match().first(), seat.match().second()).stream()).toList();
        assertEquals(16, seatedPlayers.size());
        assertEquals(16, seatedPlayers.stream().distinct().count());
    }

    @Test
    void onlyReadyFirstRoundWinnersMatchesCanBePreseated() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        GladiatorsBracket.MatchView readyOpening = new GladiatorsBracket.MatchView(1,
                GladiatorsBracket.Bracket.WINNERS, 1, first, second, null, null,
                GladiatorsBracket.Status.READY, false, -1, false, -1, false);
        GladiatorsBracket.MatchView unresolved = new GladiatorsBracket.MatchView(2,
                GladiatorsBracket.Bracket.WINNERS, 1, third, fourth, null, null,
                GladiatorsBracket.Status.WAITING, false, -1, false, -1, false);
        GladiatorsBracket.MatchView lower = new GladiatorsBracket.MatchView(3,
                GladiatorsBracket.Bracket.LOSERS, 1, third, fourth, null, null,
                GladiatorsBracket.Status.READY, false, -1, false, -1, false);
        GladiatorsBracket.MatchView laterRound = new GladiatorsBracket.MatchView(4,
                GladiatorsBracket.Bracket.WINNERS, 2, third, fourth, null, null,
                GladiatorsBracket.Status.READY, false, 1, false, 2, false);

        List<GladiatorsOpeningSeatPlan.Seat> seats = GladiatorsOpeningSeatPlan.create(
                List.of(readyOpening, unresolved, lower, laterRound), 4);

        assertEquals(1, seats.size());
        assertEquals(1, seats.get(0).match().id());
        assertEquals(0, seats.get(0).arenaIndex());
    }

    @Test
    void arenaCapacityNeverCreatesDuplicateOrUnassignedArenaIndices() {
        List<UUID> players = IntStream.range(0, 16).mapToObj(index -> UUID.randomUUID()).toList();
        GladiatorsBracket bracket = new GladiatorsBracket(players);

        List<GladiatorsOpeningSeatPlan.Seat> seats = GladiatorsOpeningSeatPlan.create(bracket.readyMatches(), 3);

        assertEquals(3, seats.size());
        assertEquals(List.of(0, 1, 2), seats.stream().map(GladiatorsOpeningSeatPlan.Seat::arenaIndex).toList());
        assertFalse(seats.get(0).match().first().equals(seats.get(0).match().second()));
        assertTrue(seats.stream().map(seat -> seat.match().id()).distinct().count() == seats.size());
    }
}
