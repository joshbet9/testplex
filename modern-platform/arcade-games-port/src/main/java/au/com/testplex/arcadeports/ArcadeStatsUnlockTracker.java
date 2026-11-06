package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tracks new unlocks since a human player joined, without replaying saved history. */
final class ArcadeStatsUnlockTracker {
    record Unlock(String key, String game, String name, int tier, String requirement) {
        boolean milestone() { return tier > 0; }
    }

    private final Map<UUID, Set<String>> seenByPlayer = new HashMap<>();

    List<Unlock> observe(UUID playerId, Map<String, Unlock> earnedNow) {
        Set<String> seen = seenByPlayer.get(playerId);
        if (seen == null) {
            seenByPlayer.put(playerId, new HashSet<>(earnedNow.keySet()));
            return List.of();
        }
        List<Unlock> newlyEarned = new ArrayList<>();
        for (Map.Entry<String, Unlock> entry : earnedNow.entrySet()) {
            if (seen.add(entry.getKey())) newlyEarned.add(entry.getValue());
        }
        return List.copyOf(newlyEarned);
    }

    void forget(UUID playerId) { seenByPlayer.remove(playerId); }

    void clear() { seenByPlayer.clear(); }
}
