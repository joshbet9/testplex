package au.com.testplex.arcadeports;

import java.util.List;
import java.util.ArrayList;

/** Maps opening winners' bracket duels to distinct arenas before the rules vote completes. */
final class GladiatorsOpeningSeatPlan {
    private GladiatorsOpeningSeatPlan() { }

    static List<Seat> create(List<GladiatorsBracket.MatchView> readyMatches, int arenaCapacity) {
        if (readyMatches == null || arenaCapacity <= 0) return List.of();
        List<Seat> seats = new ArrayList<>();
        for (GladiatorsBracket.MatchView match : readyMatches) {
            if (match == null || match.status() != GladiatorsBracket.Status.READY
                    || match.bracket() != GladiatorsBracket.Bracket.WINNERS || match.round() != 1
                    || match.first() == null || match.second() == null || match.first().equals(match.second())) continue;
            if (seats.size() >= arenaCapacity) break;
            seats.add(new Seat(match, seats.size()));
        }
        return List.copyOf(seats);
    }

    record Seat(GladiatorsBracket.MatchView match, int arenaIndex) { }
}
