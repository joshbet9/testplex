package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Match-scoped detectors for multi-ability Heroes of GWEN achievements. */
final class HeroesOfGwenAchievementTracker {
    private record TargetKey(UUID player, UUID target) { }
    private final Map<TargetKey, Map<String, Long>> abilityHits = new HashMap<>();
    private final Map<UUID, Long> larissaAquaHitAt = new HashMap<>();
    private final Map<UUID, RallyProgress> rallyProgress = new HashMap<>();

    private static final class RallyProgress {
        final Set<UUID> healed = new HashSet<>();
        final Set<UUID> damaged = new HashSet<>();
    }

    String recordAbilityHit(UUID player, UUID target, String ability, long now) {
        if (player == null || target == null || ability == null) return null;
        TargetKey key = new TargetKey(player, target);
        Map<String, Long> hits = abilityHits.computeIfAbsent(key, ignored -> new HashMap<>());
        hits.entrySet().removeIf(entry -> now - entry.getValue() > 15_000L);
        hits.put(ability, now);
        if (containsWithin(hits, now, 15_000L, "hattori.shuriken", "hattori.ninja_dash", "hattori.ender_blade")) {
            return "hattori-smoke-and-mirrors";
        }
        // Flame Wand applies three seconds of fire, so every follow-up must land inside that window.
        if (containsWithin(hits, now, 3_000L, "anath.flame_wand", "anath.burn_beam", "anath.meteor_shower")) {
            return "anath-from-ember-to-inferno";
        }
        if (containsWithin(hits, now, 5_000L, "biff.tether", "biff.cavalry_charge")) {
            return "biff-the-long-ride";
        }
        Long vinePullAt = hits.get("ivy.vine_pull");
        Long thornWallAt = hits.get("ivy.thorn_wall");
        if ("ivy.floral_leap".equals(ability) && vinePullAt != null && thornWallAt != null
                && vinePullAt <= thornWallAt && now >= thornWallAt && now - thornWallAt <= 5_000L) {
            return "ivy-garden-variety-trouble";
        }
        return null;
    }

    void recordLarissaAquaCannonHit(UUID player, long now) {
        if (player != null) larissaAquaHitAt.put(player, now);
    }

    boolean recordLarissaStormBlessingHeal(UUID player, long now) {
        if (player == null) return false;
        Long hitAt = larissaAquaHitAt.get(player);
        if (hitAt == null || now - hitAt > 10_000L) return false;
        larissaAquaHitAt.remove(player);
        return true;
    }

    boolean recordBardolfPackKill(UUID owner, int livingWolves, boolean fullMoonActive) {
        return owner != null && livingWolves >= 5 && fullMoonActive;
    }

    void startDanaRally(UUID player) {
        if (player != null) rallyProgress.put(player, new RallyProgress());
    }

    String recordDanaRallyHeal(UUID player, UUID teammate) {
        RallyProgress progress = rallyProgress.get(player);
        if (progress == null || teammate == null) return null;
        progress.healed.add(teammate);
        return progress.healed.size() >= 3 && progress.damaged.size() >= 2
                ? "dana-rally-the-vanguard" : null;
    }

    String recordDanaRallyDamage(UUID player, UUID enemy) {
        RallyProgress progress = rallyProgress.get(player);
        if (progress == null || enemy == null) return null;
        progress.damaged.add(enemy);
        return progress.healed.size() >= 3 && progress.damaged.size() >= 2
                ? "dana-rally-the-vanguard" : null;
    }

    void endDanaRally(UUID player) {
        if (player != null) rallyProgress.remove(player);
    }

    void removePlayer(UUID player) {
        if (player == null) return;
        abilityHits.keySet().removeIf(key -> key.player().equals(player) || key.target().equals(player));
        larissaAquaHitAt.remove(player);
        rallyProgress.remove(player);
    }

    void clear() {
        abilityHits.clear();
        larissaAquaHitAt.clear();
        rallyProgress.clear();
    }

    private static boolean containsWithin(Map<String, Long> hits, long now, long windowMillis, String... required) {
        for (String ability : required) {
            Long at = hits.get(ability);
            if (at == null || now - at > windowMillis) return false;
        }
        return true;
    }
}
