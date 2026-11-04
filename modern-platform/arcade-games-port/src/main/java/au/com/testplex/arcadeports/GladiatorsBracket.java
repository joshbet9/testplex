package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Fixed, dependency-driven double-elimination bracket for Gladiators tournaments. */
public final class GladiatorsBracket {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 16;

    public enum Bracket { WINNERS, LOSERS, GRAND_FINAL, GRAND_FINAL_RESET }
    public enum Status { WAITING, READY, FIGHTING, COMPLETED, BYE }

    private final Map<Integer, Match> matches = new LinkedHashMap<>();
    private final Map<UUID, Integer> losses = new HashMap<>();
    private final Set<UUID> withdrawn = new HashSet<>();
    private final List<UUID> seededPlayers;
    private final Match winnersFinal;
    private final Match grandFinal;
    private Match resetFinal;
    private UUID champion;
    private int nextId;

    /** The supplied order is the one-time random seed. It is copied and never reshuffled. */
    public GladiatorsBracket(List<UUID> seededPlayers) {
        Objects.requireNonNull(seededPlayers, "seededPlayers");
        if (seededPlayers.size() < MIN_PLAYERS || seededPlayers.size() > MAX_PLAYERS
                || seededPlayers.stream().anyMatch(Objects::isNull)
                || seededPlayers.stream().distinct().count() != seededPlayers.size()) {
            throw new IllegalArgumentException("Gladiators requires 2-16 unique seeded participants.");
        }
        this.seededPlayers = List.copyOf(seededPlayers);
        seededPlayers.forEach(player -> losses.put(player, 0));

        int bracketSize = 1;
        while (bracketSize < seededPlayers.size()) bracketSize <<= 1;
        List<Slot> entrants = seedSlots(seededPlayers, bracketSize);
        List<List<Match>> winnersRounds = new ArrayList<>();
        int round = 1;
        while (entrants.size() > 1) {
            List<Match> matchesThisRound = new ArrayList<>();
            List<Slot> winners = new ArrayList<>();
            for (int index = 0; index < entrants.size(); index += 2) {
                Match match = create(Bracket.WINNERS, round, entrants.get(index), entrants.get(index + 1), false);
                matchesThisRound.add(match);
                winners.add(winnerSlot(match));
            }
            winnersRounds.add(matchesThisRound);
            entrants = winners;
            round++;
        }
        winnersFinal = entrants.get(0).match;

        List<Slot> lowerSurvivors = List.of();
        int lowerRound = 1;
        for (int winnersRound = 0; winnersRound < winnersRounds.size(); winnersRound++) {
            List<Slot> drops = winnersRounds.get(winnersRound).stream().map(this::loserSlot).toList();
            if (drops.isEmpty()) continue;
            if (lowerSurvivors.isEmpty()) {
                lowerSurvivors = pairAll(drops, lowerRound++, Bracket.LOSERS);
            } else {
                lowerSurvivors = pairGroups(lowerSurvivors, drops, lowerRound++, Bracket.LOSERS);
            }
        }
        if (lowerSurvivors.size() > 1) {
            lowerSurvivors = pairAll(lowerSurvivors, lowerRound, Bracket.LOSERS);
        }
        if (lowerSurvivors.size() != 1) {
            throw new IllegalStateException("Could not construct the lower bracket champion slot.");
        }
        grandFinal = create(Bracket.GRAND_FINAL, 1, winnerSlot(winnersFinal), lowerSurvivors.get(0), false);
        // Reserve the reset match and both feeder paths up front. Its lower
        // champion winner path is only populated if they take the first final;
        // the upper champion is supplied through the first-final loser path.
        resetFinal = create(Bracket.GRAND_FINAL_RESET, 2,
                winnerSlot(grandFinal), loserSlot(grandFinal), true);
    }

    private List<Slot> seedSlots(List<UUID> players, int bracketSize) {
        List<UUID> seeded = new ArrayList<>(players);
        while (seeded.size() < bracketSize) seeded.add(null);
        // A fixed seeded order is used verbatim. Byes are distributed through the opening round.
        List<Slot> slots = new ArrayList<>(bracketSize);
        seeded.forEach(player -> slots.add(directSlot(player)));
        return slots;
    }

    private List<Slot> pairAll(List<Slot> entrants, int round, Bracket bracket) {
        List<Slot> remaining = new ArrayList<>(entrants);
        List<Slot> output = new ArrayList<>((remaining.size() + 1) / 2);
        for (int index = 0; index < remaining.size(); index += 2) {
            Slot first = remaining.get(index);
            if (index + 1 == remaining.size()) output.add(first);
            else output.add(winnerSlot(create(bracket, round, first, remaining.get(index + 1), false)));
        }
        return output;
    }

    /** Pair the previous lower-bracket survivors against new upper-bracket drops first. */
    private List<Slot> pairGroups(List<Slot> survivors, List<Slot> drops, int round, Bracket bracket) {
        List<Slot> output = new ArrayList<>((survivors.size() + drops.size() + 1) / 2);
        int crossPairs = Math.min(survivors.size(), drops.size());
        for (int index = 0; index < crossPairs; index++) {
            output.add(winnerSlot(create(bracket, round, survivors.get(index), drops.get(index), false)));
        }
        List<Slot> remaining = new ArrayList<>();
        remaining.addAll(survivors.subList(crossPairs, survivors.size()));
        remaining.addAll(drops.subList(crossPairs, drops.size()));
        output.addAll(pairAll(remaining, round, bracket));
        return output;
    }

    private Match create(Bracket bracket, int round, Slot first, Slot second, boolean reset) {
        Match match = new Match(nextId++, bracket, round, first, second, reset);
        matches.put(match.id, match);
        return match;
    }

    private Slot directSlot(UUID player) { return new Slot(player, null, false); }
    private Slot winnerSlot(Match match) { return new Slot(null, match, false); }
    private Slot loserSlot(Match match) { return new Slot(null, match, true); }

    /** Returns all newly playable predetermined matches. Independent matches may start together. */
    public List<MatchView> readyMatches() {
        propagateByes();
        return matches.values().stream()
                .filter(match -> !match.complete && !match.fighting && match.first.resolved && match.second.resolved
                        && match.first.player != null && match.second.player != null)
            .map(Match::view).toList();
    }

    public boolean begin(int matchId) {
        Match match = matches.get(matchId);
        if (match == null || match.complete || match.fighting) return false;
        propagateByes();
        if (!match.first.resolved || !match.second.resolved
                || match.first.player == null || match.second.player == null
                || match.first.player.equals(match.second.player)) return false;
        if (hasFighting(match.first.player) || hasFighting(match.second.player)) return false;
        match.fighting = true;
        return true;
    }

    public void cancelBegin(int matchId) {
        Match match = matches.get(matchId);
        if (match == null || match.complete || !match.fighting) return;
        match.fighting = false;
    }

    public void complete(int matchId, UUID winner) {
        Match match = matches.get(matchId);
        if (match == null || match.complete || !match.fighting) throw new IllegalStateException("Match is not fighting.");
        if (!Objects.equals(winner, match.first.player) && !Objects.equals(winner, match.second.player)) {
            throw new IllegalArgumentException("Winner must be a participant in this match.");
        }
        match.fighting = false;
        match.complete = true;
        match.winner = winner;
        UUID loser = match.first.player.equals(winner) ? match.second.player : match.first.player;
        int lossCount = losses.merge(loser, 1, Integer::sum);
        match.loser = lossCount >= 2 ? null : loser;
        if (match.bracket == Bracket.GRAND_FINAL) {
            UUID winnersChampion = winnersFinal.winner;
            if (Objects.equals(winner, winnersChampion)) champion = winner;
        } else if (match.bracket == Bracket.GRAND_FINAL_RESET) {
            champion = winner;
        }
        propagateByes();
    }

    /** Resolve a disconnect or forfeit as a loss for the unavailable participant. */
    public void forfeit(int matchId, UUID unavailable) {
        Match match = matches.get(matchId);
        if (match == null || match.complete || !match.fighting) throw new IllegalStateException("Match is not fighting.");
        if (!Objects.equals(unavailable, match.first.player) && !Objects.equals(unavailable, match.second.player)) {
            throw new IllegalArgumentException("Forfeiting player is not in this match.");
        }
        UUID winner = Objects.equals(unavailable, match.first.player) ? match.second.player : match.first.player;
        complete(matchId, winner);
    }

    /** Resolve a not-yet-started slot without assigning a loss, for a bye or unavailable entrant. */
    public void resolveWithoutFight(int matchId, UUID availableWinner) {
        Match match = matches.get(matchId);
        if (match == null || match.complete || match.fighting) throw new IllegalStateException("Match cannot be resolved as a bye.");
        if (availableWinner != null && !Objects.equals(availableWinner, match.first.player)
                && !Objects.equals(availableWinner, match.second.player)) {
            throw new IllegalArgumentException("Available winner is not in this match.");
        }
        match.complete = true;
        match.winner = availableWinner;
        match.loser = null;
        if (match.bracket == Bracket.GRAND_FINAL && availableWinner != null) {
            champion = availableWinner;
        } else if (match.bracket == Bracket.GRAND_FINAL_RESET && availableWinner != null) {
            champion = availableWinner;
        }
        propagateByes();
    }

    /** Close a match whose two competitors are both unavailable, preserving both bracket paths as empty. */
    public void abandon(int matchId) {
        Match match = matches.get(matchId);
        if (match == null || match.complete) throw new IllegalStateException("Match cannot be abandoned.");
        match.fighting = false;
        match.complete = true;
        match.winner = null;
        match.loser = null;
        propagateByes();
    }

    /** Remove a participant who disconnects while waiting for a later fixed slot. */
    public void withdraw(UUID player) {
        if (player == null || champion != null || hasFighting(player)) return;
        losses.put(player, 2);
        withdrawn.add(player);
        for (Match match : matches.values()) {
            if (match.first.match == null && Objects.equals(match.first.direct, player)) match.first.direct = null;
            if (match.second.match == null && Objects.equals(match.second.direct, player)) match.second.direct = null;
            if (match.complete && Objects.equals(match.winner, player)) match.winner = null;
        }
        propagateByes();
    }

    private boolean hasFighting(UUID player) {
        return matches.values().stream().anyMatch(match -> match.fighting
                && (Objects.equals(match.first.player, player) || Objects.equals(match.second.player, player)));
    }

    private void propagateByes() {
        boolean changed;
        do {
            changed = false;
            for (Match match : matches.values()) {
                match.first.refresh();
                match.second.refresh();
                if (match.complete || match.fighting || !match.first.resolved || !match.second.resolved) continue;
                if (match.first.player != null && match.second.player != null) continue;
                match.complete = true;
                match.winner = match.first.player == null ? match.second.player : match.first.player;
                match.loser = null;
                if ((match.bracket == Bracket.GRAND_FINAL || match.bracket == Bracket.GRAND_FINAL_RESET)
                        && match.winner != null) champion = match.winner;
                changed = true;
            }
        } while (changed);
    }

    public List<MatchView> matches() { return matches.values().stream().map(Match::view).toList(); }
    public List<UUID> seededPlayers() { return seededPlayers; }
    public int losses(UUID player) { return losses.getOrDefault(player, 0); }
    public UUID champion() { return champion; }
    public boolean complete() { return champion != null; }
    public MatchView match(int matchId) { Match match = matches.get(matchId); return match == null ? null : match.view(); }

    private final class Slot {
        private UUID direct;
        private final Match match;
        private final boolean loser;
        private boolean resolved;
        private UUID player;
        private Slot(UUID direct, Match match, boolean loser) {
            this.direct = direct;
            this.match = match;
            this.loser = loser;
            this.resolved = match == null;
            this.player = direct;
        }
        void refresh() {
            if (match == null || !match.complete) return;
            resolved = true;
            UUID resolvedPlayer = loser ? match.loser : match.winner;
            player = resolvedPlayer != null && !withdrawn.contains(resolvedPlayer) ? resolvedPlayer : null;
        }
    }

    private final class Match {
        private final int id;
        private final Bracket bracket;
        private final int round;
        private final Slot first;
        private final Slot second;
        private final boolean reset;
        private boolean fighting;
        private boolean complete;
        private UUID winner;
        private UUID loser;
        private Match(int id, Bracket bracket, int round, Slot first, Slot second, boolean reset) {
            this.id = id; this.bracket = bracket; this.round = round;
            this.first = first; this.second = second; this.reset = reset;
        }
        private MatchView view() {
            first.refresh(); second.refresh();
            Status status = complete ? Status.COMPLETED : fighting ? Status.FIGHTING
                    : first.resolved && second.resolved && first.player != null && second.player != null
                    ? Status.READY : Status.WAITING;
            return new MatchView(id, bracket, round, first.player, second.player, winner, loser, status, reset,
                    first.match == null ? -1 : first.match.id, first.loser,
                    second.match == null ? -1 : second.match.id, second.loser);
        }
    }

    public record MatchView(int id, Bracket bracket, int round, UUID first, UUID second,
            UUID winner, UUID loser, Status status, boolean reset,
            int firstFeederId, boolean firstFeederLoser, int secondFeederId, boolean secondFeederLoser) { }
}
