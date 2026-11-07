package au.com.testplex.arcadeports;

import java.util.Collection;
import java.util.function.Predicate;

/** Career challenges must be earned on all currently available real maps. */
final class WitherAssaultAchievementRules {
    private WitherAssaultAchievementRules() { }

    static boolean allMapsWon(Collection<String> availableResources, Predicate<String> wonOnMap) {
        return availableResources != null && !availableResources.isEmpty()
                && availableResources.stream().allMatch(resource -> resource != null
                        && !resource.isBlank() && wonOnMap.test(resource));
    }

    static boolean smokeAndMirrors(int distinctWithersHitByArrow) {
        return distinctWithersHitByArrow >= 3;
    }

    static boolean buildersRefuge(String kit, boolean humanWon, int blocksPlaced) {
        return "editor".equals(kit) && humanWon && blocksPlaced >= 20;
    }

    static boolean holdTheLine(String kit, boolean humanWon, int skullHitsBlocked) {
        return "warden".equals(kit) && humanWon && skullHitsBlocked >= 5;
    }

    static boolean boneBrigade(boolean witherWon, int minionEliminations) {
        return witherWon && minionEliminations >= 3;
    }

    static boolean keepTheLight(boolean humanWon, int distinctRevivedAllies, int revivedAlliesAliveAtFinish) {
        return humanWon && distinctRevivedAllies >= 3 && revivedAlliesAliveAtFinish >= 3;
    }

    static boolean oneBreathLeft(boolean humanWon, int livingHumansAtFinish) {
        return humanWon && livingHumansAtFinish == 1;
    }

    static boolean splitUp(int distinctTeammatesInEditorTunnel) {
        return distinctTeammatesInEditorTunnel >= 5;
    }
}
