package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Match-scoped validation for Rowena's Light Arrows tower achievement. */
final class RowenaTowerAchievementTracker {
    private final Map<Integer, UUID> owners = new HashMap<>();
    private final Map<Integer, Set<String>> lightArrowHits = new HashMap<>();
    private final Set<Integer> voidedTeams = new HashSet<>();

    void recordDamage(int attackingTeam, String towerMarker, UUID attacker,
            boolean rowenaLightArrow, boolean minionDamage) {
        if (minionDamage) return;
        if (attacker != null && rowenaLightArrow) {
            UUID owner = owners.putIfAbsent(attackingTeam, attacker);
            if (owner != null && !owner.equals(attacker)) voidedTeams.add(attackingTeam);
            lightArrowHits.computeIfAbsent(attackingTeam, ignored -> new HashSet<>()).add(towerMarker);
            return;
        }
        voidedTeams.add(attackingTeam);
    }

    UUID achievementOwner(int attackingTeam, Set<String> destroyedEnemyTowerMarkers,
            Set<String> allEnemyTowerMarkers) {
        UUID owner = owners.get(attackingTeam);
        if (owner == null || voidedTeams.contains(attackingTeam)
                || allEnemyTowerMarkers.isEmpty()
                || !destroyedEnemyTowerMarkers.containsAll(allEnemyTowerMarkers)
                || !lightArrowHits.getOrDefault(attackingTeam, Set.of()).containsAll(allEnemyTowerMarkers)) {
            return null;
        }
        return owner;
    }

    void clear() {
        owners.clear();
        lightArrowHits.clear();
        voidedTeams.clear();
    }
}
