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

    @Test void kitAndTeamAchievementsUseTheirApprovedDistinctEventThresholds() {
        assertTrue(WitherAssaultAchievementRules.smokeAndMirrors(3));
        assertFalse(WitherAssaultAchievementRules.smokeAndMirrors(2));
        assertTrue(WitherAssaultAchievementRules.buildersRefuge("editor", true, 20));
        assertFalse(WitherAssaultAchievementRules.buildersRefuge("archer", true, 20));
        assertFalse(WitherAssaultAchievementRules.buildersRefuge("editor", false, 20));
        assertTrue(WitherAssaultAchievementRules.holdTheLine("warden", true, 5));
        assertFalse(WitherAssaultAchievementRules.holdTheLine("warden", true, 4));
        assertFalse(WitherAssaultAchievementRules.holdTheLine("medic", true, 5));
        assertTrue(WitherAssaultAchievementRules.boneBrigade(true, 3));
        assertFalse(WitherAssaultAchievementRules.boneBrigade(false, 3));
        assertTrue(WitherAssaultAchievementRules.keepTheLight(true, 3, 3));
        assertFalse(WitherAssaultAchievementRules.keepTheLight(true, 3, 2));
        assertTrue(WitherAssaultAchievementRules.oneBreathLeft(true, 1));
        assertFalse(WitherAssaultAchievementRules.oneBreathLeft(true, 2));
        assertTrue(WitherAssaultAchievementRules.splitUp(5));
        assertFalse(WitherAssaultAchievementRules.splitUp(4));
    }
}
