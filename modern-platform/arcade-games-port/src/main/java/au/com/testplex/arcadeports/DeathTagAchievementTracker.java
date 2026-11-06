package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Match-local, player-attributed Death Tag achievement progress. */
final class DeathTagAchievementTracker {
    private static final long TRAITOR_ASSIST_WINDOW_MILLIS = 8_000L;
    private static final long TURTLE_STACK_WINDOW_MILLIS = 5_000L;

    private final Map<UUID, Set<UUID>> chasersHitByRunner = new HashMap<>();
    private final Map<UUID, Set<UUID>> chasersWhoDamagedRunner = new HashMap<>();
    private final Map<UUID, Integer> archerHits = new HashMap<>();
    private final Map<UUID, List<Long>> chaserKillTimes = new HashMap<>();
    private final Map<UUID, Map<UUID, HitChain>> turtleHitChains = new HashMap<>();
    private final Map<UUID, Set<UUID>> turtleFullStackTargets = new HashMap<>();
    private final Map<UUID, UUID> lastTraitorByRunner = new HashMap<>();
    private final Map<UUID, Long> lastTraitorHitAt = new HashMap<>();
    private final Map<UUID, Integer> traitorAssists = new HashMap<>();

    private UUID originalAlpha;
    private UUID originalAlphaFirstVictim;
    private final Map<UUID, Integer> infectionGenerations = new HashMap<>();
    private boolean patientZeroAwarded;

    boolean recordRunnerMeleeHit(UUID runner, UUID chaser) {
        return chasersHitByRunner.computeIfAbsent(runner, ignored -> new HashSet<>()).add(chaser)
                && chasersHitByRunner.get(runner).size() >= 5;
    }

    boolean recordArcherHit(UUID archer) {
        return archerHits.merge(archer, 1, Integer::sum) == 10;
    }

    boolean recordChaserKillForHastyHunter(UUID chaser, long nowMillis) {
        List<Long> kills = chaserKillTimes.computeIfAbsent(chaser, ignored -> new ArrayList<>());
        kills.removeIf(killAt -> nowMillis - killAt > 30_000L);
        kills.add(nowMillis);
        return kills.size() == 3;
    }

    void recordChaserDamageToRunner(UUID chaser, UUID runner) {
        chasersWhoDamagedRunner.computeIfAbsent(runner, ignored -> new HashSet<>()).add(chaser);
    }

    boolean survivedThreeChasers(UUID runner) {
        return chasersWhoDamagedRunner.getOrDefault(runner, Set.of()).size() >= 3;
    }

    boolean recordTurtleHit(UUID turtle, UUID chaser, long nowMillis) {
        Map<UUID, HitChain> targets = turtleHitChains.computeIfAbsent(turtle, ignored -> new HashMap<>());
        HitChain chain = targets.get(chaser);
        if (chain == null || nowMillis - chain.startedAt > TURTLE_STACK_WINDOW_MILLIS) {
            chain = new HitChain(nowMillis);
            targets.put(chaser, chain);
        }
        chain.hits++;
        if (chain.hits != 3) return false;
        Set<UUID> completed = turtleFullStackTargets.computeIfAbsent(turtle, ignored -> new HashSet<>());
        return completed.add(chaser) && completed.size() >= 2;
    }

    void recordTraitorKnockback(UUID traitor, UUID fellowRunner, long nowMillis) {
        lastTraitorByRunner.put(fellowRunner, traitor);
        lastTraitorHitAt.put(fellowRunner, nowMillis);
    }

    UUID consumeTraitorAssist(UUID convertedRunner, long nowMillis) {
        UUID traitor = lastTraitorByRunner.remove(convertedRunner);
        Long hitAt = lastTraitorHitAt.remove(convertedRunner);
        return traitor != null && hitAt != null && nowMillis - hitAt <= TRAITOR_ASSIST_WINDOW_MILLIS
                ? traitor : null;
    }

    boolean creditTraitorAssist(UUID traitor) {
        return traitorAssists.merge(traitor, 1, Integer::sum) == 3;
    }

    /**
     * Only the original Alpha's FIRST victim can seed the four-generation chain:
     * Alpha -> A -> B -> C -> D. Forced promotions never extend the chain.
     * Returns the Alpha's UUID once when a valid chain completes.
     */
    UUID recordConversion(UUID convertedRunner, UUID killer, boolean forcedAlpha) {
        if (forcedAlpha) {
            if (originalAlpha == null) originalAlpha = convertedRunner;
            return null;
        }
        if (killer == null || originalAlpha == null) return null;
        if (killer.equals(originalAlpha)) {
            if (originalAlphaFirstVictim == null) {
                originalAlphaFirstVictim = convertedRunner;
                infectionGenerations.put(convertedRunner, 1);
            }
            return null;
        }
        Integer parentGeneration = infectionGenerations.get(killer);
        if (parentGeneration == null) return null;
        int generation = parentGeneration + 1;
        infectionGenerations.put(convertedRunner, generation);
        if (generation >= 4 && !patientZeroAwarded) {
            patientZeroAwarded = true;
            return originalAlpha;
        }
        return null;
    }

    private static final class HitChain {
        final long startedAt;
        int hits;

        HitChain(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
