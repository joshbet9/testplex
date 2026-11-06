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
}
