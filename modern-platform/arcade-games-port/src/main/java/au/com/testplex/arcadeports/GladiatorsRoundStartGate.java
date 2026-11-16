package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Releases every duel in a bracket round together once all feeder matches resolve. */
final class GladiatorsRoundStartGate {
    private GladiatorsRoundStartGate() { }

    static List<GladiatorsBracket.MatchView> readyMatches(
            List<GladiatorsBracket.MatchView> allMatches,
            List<GladiatorsBracket.MatchView> readyMatches) {
        if (allMatches == null || readyMatches == null || readyMatches.isEmpty()) return List.of();
        Set<RoundKey> unresolvedRounds = allMatches.stream()
                .filter(match -> match.status() == GladiatorsBracket.Status.WAITING)
                .map(match -> new RoundKey(match.bracket(), match.round()))
                .collect(Collectors.toSet());
        return readyMatches.stream()
                .filter(match -> !unresolvedRounds.contains(new RoundKey(match.bracket(), match.round())))
                .toList();
    }

    private record RoundKey(GladiatorsBracket.Bracket bracket, int round) { }
}
