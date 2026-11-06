package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Round-local, independently testable Bacon Brawl achievement progress. */
final class BaconBrawlAchievementTracker {
    static final int IRON_HOG_BLOCKS = 8;
    static final int EXTRA_CRISPY_UNIQUE_IGNITIONS = 5;
    static final int QUADRUPLE_SERVING_KNOCKOUTS = 4;
    static final long QUADRUPLE_SERVING_GAP_MILLIS = 10_000L;
    static final long GUARD_HIT_REPEAT_DELAY_MILLIS = 400L;

    static final List<String> ALL_KITS = List.of("el-muchacho-pigo", "mama-piggles",
            "pig", "brutus-bacon", "chris-p-bacon");

    private record HitPair(UUID defender, UUID attacker) { }
    private record KnockoutStreak(int count, long lastKnockoutAt) { }

    private final Map<UUID, Integer> guardedHits = new HashMap<>();
    private final Map<HitPair, Long> lastGuardHitAt = new HashMap<>();
    private final Map<UUID, Set<UUID>> ignitedOpponents = new HashMap<>();
    private final Map<UUID, KnockoutStreak> knockoutStreaks = new HashMap<>();

    int recordBlockedHit(UUID brutusId, UUID attackerId, long now) {
        if (brutusId == null || attackerId == null || brutusId.equals(attackerId)) return 0;
        HitPair pair = new HitPair(brutusId, attackerId);
        Long previous = lastGuardHitAt.get(pair);
        if (previous != null && now >= previous && now - previous <= GUARD_HIT_REPEAT_DELAY_MILLIS) {
            return guardedHits.getOrDefault(brutusId, 0);
        }
        lastGuardHitAt.put(pair, now);
        return guardedHits.merge(brutusId, 1, Integer::sum);
    }

    int recordCrispyIgnition(UUID chrisId, UUID victimId) {
        if (chrisId == null || victimId == null || chrisId.equals(victimId)) return 0;
        Set<UUID> opponents = ignitedOpponents.computeIfAbsent(chrisId, ignored -> new HashSet<>());
        if (!opponents.add(victimId)) return 0;
        return opponents.size();
    }

    /** Every qualifying knockout refreshes the ten-second deadline for the next one. */
    int recordElimination(UUID killerId, boolean knockOff, long now) {
        if (killerId == null) return 0;
        if (!knockOff) {
            knockoutStreaks.remove(killerId);
            return 0;
        }
        KnockoutStreak previous = knockoutStreaks.get(killerId);
        int consecutive = previous != null && now >= previous.lastKnockoutAt()
                && now - previous.lastKnockoutAt() <= QUADRUPLE_SERVING_GAP_MILLIS
                ? previous.count() + 1 : 1;
        knockoutStreaks.put(killerId, new KnockoutStreak(consecutive, now));
        return consecutive;
    }

    static boolean hasWonEveryKit(Set<String> wonKitIds) {
        return wonKitIds != null && wonKitIds.containsAll(ALL_KITS);
    }

    void clearAll() {
        guardedHits.clear();
        lastGuardHitAt.clear();
        ignitedOpponents.clear();
        knockoutStreaks.clear();
    }
}
