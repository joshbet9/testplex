package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GladiatorsRoundAndCombatHudTest {
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    @Test
    void waitsForEveryFeederInARoundThenReleasesAllReadyDuels() {
        var first = match(1, GladiatorsBracket.Bracket.WINNERS, 2, GladiatorsBracket.Status.READY);
        var second = match(2, GladiatorsBracket.Bracket.WINNERS, 2, GladiatorsBracket.Status.WAITING);
        assertTrue(GladiatorsRoundStartGate.readyMatches(List.of(first, second), List.of(first)).isEmpty());

        var resolvedSecond = match(2, GladiatorsBracket.Bracket.WINNERS, 2, GladiatorsBracket.Status.READY);
        assertEquals(List.of(first, resolvedSecond),
                GladiatorsRoundStartGate.readyMatches(List.of(first, resolvedSecond), List.of(first, resolvedSecond)));
    }

    @Test
    void independentBracketRoundsDoNotBlockOneAnother() {
        var winners = match(1, GladiatorsBracket.Bracket.WINNERS, 2, GladiatorsBracket.Status.READY);
        var losers = match(2, GladiatorsBracket.Bracket.LOSERS, 3, GladiatorsBracket.Status.WAITING);
        assertEquals(List.of(winners),
                GladiatorsRoundStartGate.readyMatches(List.of(winners, losers), List.of(winners)));
    }

    @Test
    void countdownRoundsUpAndStopsAtFight() {
        assertEquals(5, GladiatorsRoundCountdown.secondsRemaining(5_000L, 1L));
        assertEquals(1, GladiatorsRoundCountdown.secondsRemaining(1_000L, 1L));
        assertEquals(0, GladiatorsRoundCountdown.secondsRemaining(1_000L, 1_000L));
        assertEquals(0, GladiatorsRoundCountdown.secondsRemaining(999L, 1_000L));
    }

    @Test
    void modernCombatDisplayShowsShieldAndCooldownState() {
        assertEquals(new GladiatorsModernCombatHud.View("Raised", "0.2s"),
                GladiatorsModernCombatHud.view(true, 0.5F, 4.0D));
        assertEquals(new GladiatorsModernCombatHud.View("Lowered", "Ready"),
                GladiatorsModernCombatHud.view(false, 1.0F, 4.0D));
        assertEquals("0.3s",
                GladiatorsModernCombatHud.view(false, Float.NaN, Double.NaN).cooldownStatus());
    }

    private static GladiatorsBracket.MatchView match(int id, GladiatorsBracket.Bracket bracket,
            int round, GladiatorsBracket.Status status) {
        return new GladiatorsBracket.MatchView(id, bracket, round, A, B, null, null, status,
                false, -1, false, -1, false);
    }
}
