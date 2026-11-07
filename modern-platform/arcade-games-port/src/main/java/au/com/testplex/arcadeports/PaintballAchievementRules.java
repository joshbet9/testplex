package au.com.testplex.arcadeports;

import java.util.Set;
import java.util.UUID;

/** Narrow achievement predicates used by Paintball's event handlers and match completion. */
final class PaintballAchievementRules {
    private static final Set<String> ALL_KITS = Set.of("rifle", "shotgun", "machine-gun", "sniper");

    private PaintballAchievementRules() { }

    static boolean killingSpree(int creditedKillsInFiveSeconds) {
        return creditedKillsInFiveSeconds >= 4;
    }

    static boolean lastStand(int creditedKillsWhileSolo) {
        return creditedKillsWhileSolo >= 3;
    }

    static boolean sniperLongRange(Set<UUID> distinctTargets) {
        return distinctTargets != null && distinctTargets.size() >= 3;
    }

    static boolean medic(int successfulRevives) {
        return successfulRevives >= 1;
    }

    static boolean rescueRelay(int successfulRevives) {
        return successfulRevives >= 5;
    }

    static boolean secondWind(boolean won, Set<UUID> distinctRevivedTeammates) {
        return won && distinctRevivedTeammates != null && distinctRevivedTeammates.size() >= 2;
    }

    static boolean flawlessVictory(boolean won, int teammatesLost) {
        return won && teammatesLost == 0;
    }

    static boolean speedrunner(boolean won, long matchDurationMillis) {
        return won && matchDurationMillis >= 0L && matchDurationMillis < 30_000L;
    }

    static boolean fourColourArsenal(Set<String> winningKits) {
        return winningKits != null && winningKits.containsAll(ALL_KITS);
    }

    static boolean soloBeforeRevive(long livingTeammates) {
        return livingTeammates == 1L;
    }
}
