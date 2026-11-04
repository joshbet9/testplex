package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GladiatorsBracketTest {
    @Test
    void completesTournamentsForEverySupportedPlayerCount() {
        for (int count = 2; count <= 16; count++) {
            GladiatorsBracket bracket = new GladiatorsBracket(players(count));
            play(bracket, new Random(1000L + count), false);
            assertTrue(bracket.complete(), "tournament stalled with " + count + " players");
            assertTrue(bracket.losses(bracket.champion()) <= 1,
                    "champion must have zero or one loss for " + count + " players");
            for (UUID player : bracket.seededPlayers()) {
                if (!player.equals(bracket.champion())) assertEquals(2, bracket.losses(player));
            }
            assertEquals(count, bracket.seededPlayers().size());
        }
    }

    @Test
    void sixteenPlayerBracketHasRequiredRoundCountsAndResettableFinal() {
        GladiatorsBracket bracket = new GladiatorsBracket(players(16));
        var initial = bracket.matches();
        assertEquals(8, count(initial, GladiatorsBracket.Bracket.WINNERS, 1));
        assertEquals(4, count(initial, GladiatorsBracket.Bracket.WINNERS, 2));
        assertEquals(2, count(initial, GladiatorsBracket.Bracket.WINNERS, 3));
        assertEquals(1, count(initial, GladiatorsBracket.Bracket.WINNERS, 4));
        assertEquals(List.of(4, 4, 3, 2, 1), java.util.stream.IntStream.rangeClosed(1, 5)
                .map(round -> count(initial, GladiatorsBracket.Bracket.LOSERS, round)).boxed().toList());
        assertEquals(1, count(initial, GladiatorsBracket.Bracket.GRAND_FINAL, 1));
        assertEquals(1, count(initial, GladiatorsBracket.Bracket.GRAND_FINAL_RESET, 2));

        playUntilGrandFinal(bracket, new Random(4L));
        GladiatorsBracket.MatchView grandFinal = bracket.matches().stream()
                .filter(match -> match.bracket() == GladiatorsBracket.Bracket.GRAND_FINAL).findFirst().orElseThrow();
        UUID winnersChampion = grandFinal.first();
        UUID losersChampion = grandFinal.second();
        assertNotNull(winnersChampion);
        assertNotNull(losersChampion);
        assertTrue(bracket.begin(grandFinal.id()));
        bracket.complete(grandFinal.id(), losersChampion);
        assertFalse(bracket.complete());
        var reset = bracket.readyMatches().stream()
                .filter(match -> match.bracket() == GladiatorsBracket.Bracket.GRAND_FINAL_RESET).findFirst().orElseThrow();
        assertTrue(bracket.begin(reset.id()));
        bracket.complete(reset.id(), winnersChampion);
        assertEquals(winnersChampion, bracket.champion());
        assertEquals(1, bracket.losses(winnersChampion));
        assertEquals(2, bracket.losses(losersChampion));
    }

    @Test
    void sixteenPlayerLowerBracketFollowsRequiredEightEightSixFourTwoProgression() {
        GladiatorsBracket bracket = new GladiatorsBracket(players(16));
        completeReadyRound(bracket, GladiatorsBracket.Bracket.WINNERS, 1);
        assertReadyParticipants(bracket, GladiatorsBracket.Bracket.LOSERS, 1, 8);

        completeReadyRound(bracket, GladiatorsBracket.Bracket.LOSERS, 1);
        completeReadyRound(bracket, GladiatorsBracket.Bracket.WINNERS, 2);
        assertReadyParticipants(bracket, GladiatorsBracket.Bracket.LOSERS, 2, 8);

        completeReadyRound(bracket, GladiatorsBracket.Bracket.LOSERS, 2);
        completeReadyRound(bracket, GladiatorsBracket.Bracket.WINNERS, 3);
        assertReadyParticipants(bracket, GladiatorsBracket.Bracket.LOSERS, 3, 6);

        completeReadyRound(bracket, GladiatorsBracket.Bracket.LOSERS, 3);
        completeReadyRound(bracket, GladiatorsBracket.Bracket.WINNERS, 4);
        assertReadyParticipants(bracket, GladiatorsBracket.Bracket.LOSERS, 4, 4);

        completeReadyRound(bracket, GladiatorsBracket.Bracket.LOSERS, 4);
        assertReadyParticipants(bracket, GladiatorsBracket.Bracket.LOSERS, 5, 2);
    }

    @Test
    void aWinnersBracketChampionWinEndsWithoutReset() {
        GladiatorsBracket bracket = new GladiatorsBracket(players(5));
        playUntilGrandFinal(bracket, new Random(55L));
        GladiatorsBracket.MatchView grandFinal = bracket.matches().stream()
                .filter(match -> match.bracket() == GladiatorsBracket.Bracket.GRAND_FINAL).findFirst().orElseThrow();
        assertTrue(bracket.begin(grandFinal.id()));
        bracket.complete(grandFinal.id(), grandFinal.first());
        assertEquals(grandFinal.first(), bracket.champion());
        assertTrue(bracket.readyMatches().stream()
                .noneMatch(match -> match.bracket() == GladiatorsBracket.Bracket.GRAND_FINAL_RESET));
    }

    @Test
    void matchSlotDependenciesAreInvariantToCompletionOrder() {
        GladiatorsBracket first = new GladiatorsBracket(players(8));
        GladiatorsBracket second = new GladiatorsBracket(players(8));
        List<String> structure = structure(first);
        assertEquals(structure, structure(second));
        play(first, new Random(1L), false);
        play(second, new Random(2L), true);
        assertEquals(structure, structure(first));
        assertEquals(structure, structure(second));
    }

    @Test
    void forfeitAndConcurrentMatchesResolveExactlyOnce() {
        GladiatorsBracket bracket = new GladiatorsBracket(players(8));
        List<GladiatorsBracket.MatchView> ready = bracket.readyMatches();
        assertEquals(4, ready.size());
        List<UUID> readyParticipants = ready.stream()
                .flatMap(match -> java.util.stream.Stream.of(match.first(), match.second())).toList();
        assertEquals(readyParticipants.size(), readyParticipants.stream().distinct().count());
        for (var match : ready) assertTrue(bracket.begin(match.id()));
        var forfeited = ready.get(0);
        bracket.forfeit(forfeited.id(), forfeited.first());
        for (int i = 1; i < ready.size(); i++) bracket.complete(ready.get(i).id(), ready.get(i).first());
        assertEquals(1, bracket.losses(forfeited.first()));
        assertEquals(0, bracket.losses(forfeited.second()));
        assertFalse(bracket.begin(forfeited.id()));
    }

    @Test
    void withdrawingAPlayerWaitingForTheLowerBracketDoesNotStall() {
        GladiatorsBracket bracket = new GladiatorsBracket(players(8));
        List<GladiatorsBracket.MatchView> firstRound = bracket.readyMatches();
        for (var match : firstRound) assertTrue(bracket.begin(match.id()));
        for (var match : firstRound) bracket.complete(match.id(), match.first());
        UUID withdrawn = firstRound.get(0).second();
        bracket.withdraw(withdrawn);
        assertEquals(2, bracket.losses(withdrawn));

        play(bracket, new Random(812L), false);
        assertTrue(bracket.complete());
        assertTrue(bracket.matches().stream()
                .filter(match -> match.status() != GladiatorsBracket.Status.COMPLETED)
                .noneMatch(match -> withdrawn.equals(match.first()) || withdrawn.equals(match.second())));
    }

    private static void playUntilGrandFinal(GladiatorsBracket bracket, Random random) {
        for (int guard = 0; guard < 300; guard++) {
            var active = bracket.matches().stream().filter(match -> match.status() == GladiatorsBracket.Status.FIGHTING)
                    .toList();
            for (var match : active) bracket.complete(match.id(), random.nextBoolean() ? match.first() : match.second());
            List<GladiatorsBracket.MatchView> ready = bracket.readyMatches();
            var finalReady = ready.stream().filter(match -> match.bracket() == GladiatorsBracket.Bracket.GRAND_FINAL)
                    .findFirst();
            if (finalReady.isPresent()) return;
            if (!ready.isEmpty()) {
                for (var match : ready) bracket.begin(match.id());
            }
        }
        throw new AssertionError("Grand final did not become ready.");
    }

    private static void play(GladiatorsBracket bracket, Random random, boolean completeReverse) {
        for (int guard = 0; guard < 1000 && !bracket.complete(); guard++) {
            List<GladiatorsBracket.MatchView> ready = new ArrayList<>(bracket.readyMatches());
            if (completeReverse) java.util.Collections.reverse(ready);
            for (var match : ready) bracket.begin(match.id());
            if (completeReverse) java.util.Collections.reverse(ready);
            for (var match : ready) {
                UUID winner = random.nextBoolean() ? match.first() : match.second();
                bracket.complete(match.id(), winner);
            }
        }
    }

    private static void completeReadyRound(GladiatorsBracket bracket,
            GladiatorsBracket.Bracket bracketType, int round) {
        List<GladiatorsBracket.MatchView> ready = bracket.readyMatches().stream()
                .filter(match -> match.bracket() == bracketType && match.round() == round).toList();
        for (var match : ready) assertTrue(bracket.begin(match.id()), "could not begin " + match);
        for (var match : ready) bracket.complete(match.id(), match.first());
    }

    private static void assertReadyParticipants(GladiatorsBracket bracket,
            GladiatorsBracket.Bracket bracketType, int round, int expected) {
        List<GladiatorsBracket.MatchView> ready = bracket.readyMatches().stream()
                .filter(match -> match.bracket() == bracketType && match.round() == round).toList();
        long participants = ready.stream()
                .flatMap(match -> java.util.stream.Stream.of(match.first(), match.second()))
                .filter(java.util.Objects::nonNull).count();
        assertEquals(expected, participants, bracketType + " round " + round);
    }

    private static int count(List<GladiatorsBracket.MatchView> matches, GladiatorsBracket.Bracket bracket, int round) {
        return (int) matches.stream().filter(match -> match.bracket() == bracket && match.round() == round).count();
    }

    private static List<String> structure(GladiatorsBracket bracket) {
        return bracket.matches().stream().map(match -> match.id() + ":" + match.bracket() + ":" + match.round()
                + ":" + match.firstFeederId() + ":" + match.firstFeederLoser() + ":"
                + match.secondFeederId() + ":" + match.secondFeederLoser()).toList();
    }

    private static List<UUID> players(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) ids.add(new UUID(0L, index + 1L));
        return ids;
    }
}
