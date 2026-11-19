package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Set;

/** Pure end-of-match predicates for Sheep Quest challenge awards. */
final class SheepQuestAchievementRules {
    private SheepQuestAchievementRules() { }

    static boolean wageWar(int playerTeam, Set<Integer> capturedPenTeams) {
        if (playerTeam < 0 || playerTeam > 3 || capturedPenTeams == null) return false;
        for (int team = 0; team < 4; team++) {
            if (team != playerTeam && !capturedPenTeams.contains(team)) return false;
        }
        return true;
    }

    static boolean cheeky(boolean won, List<Long> captureTimes, long completedAtMillis) {
        if (!won || captureTimes == null || completedAtMillis <= 0L) return false;
        long windowStart = completedAtMillis - 10_000L;
        return captureTimes.stream().filter(at -> at != null && at >= windowStart && at <= completedAtMillis)
                .count() >= 5L;
    }

    static boolean whatsASheep(boolean won, int kills, int captures) {
        return won && kills >= 10 && captures == 0;
    }

    static boolean yeet(boolean won, String kit, int airborneLeapCaptures, int otherCaptures) {
        return won && "beserker".equals(kit) && airborneLeapCaptures > 0 && otherCaptures == 0;
    }

    static boolean unconventional(boolean won, String kit, boolean enteredOwnPen) {
        return won && "archer".equals(kit) && !enteredOwnPen;
    }

    static boolean soloCarry(int unassistedKillsFromPenTeam, int stolenSheepAfterKills) {
        return unassistedKillsFromPenTeam >= 3 && stolenSheepAfterKills >= 2;
    }

    static boolean shepherdsFlock(boolean won, String kit, int maxCarried) {
        return won && "shepherd".equals(kit) && maxCarried >= 10;
    }

    static boolean cleanGetaway(boolean won, int enemySheepPickedUp, boolean wasEliminated) {
        return won && enemySheepPickedUp >= 5 && !wasEliminated;
    }

    static boolean biggestLoser(boolean won, int captures) {
        return !won && captures >= 8;
    }
}
