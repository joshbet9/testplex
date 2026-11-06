package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WitherAssaultAchievementRulesTest {
    @Test void requiresEachInstalledMapAndAtLeastOneMap() {
        Set<String> won = Set.of("map-a", "map-c");
        assertFalse(WitherAssaultAchievementRules.allMapsWon(List.of(), won::contains));
        assertFalse(WitherAssaultAchievementRules.allMapsWon(List.of("map-a", "map-b"), won::contains));
        assertTrue(WitherAssaultAchievementRules.allMapsWon(List.of("map-a", "map-c"), won::contains));
        assertFalse(WitherAssaultAchievementRules.allMapsWon(List.of(""), won::contains));
    }
}
