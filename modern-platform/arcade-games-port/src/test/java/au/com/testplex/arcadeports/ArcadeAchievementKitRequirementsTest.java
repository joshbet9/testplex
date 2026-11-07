package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeAchievementKitRequirementsTest {
    @Test void knownKitsUseTheirActualGameSpecificAchievement() {
        var dasher = ArcadeAchievementKitRequirements.find("micro-battles", "dasher");
        assertNotNull(dasher);
        assertFalse(dasher.allAchievements());
        assertEquals(java.util.List.of("speedster"), dasher.achievementIds());
        assertEquals(java.util.List.of("savior"),
                ArcadeAchievementKitRequirements.find("wither-assault", "warden").achievementIds());
    }

    @Test void runnerSwitcherNeedsEveryCurrentAchievement() {
        assertTrue(ArcadeAchievementKitRequirements.find("runner", "runnerkit-switcher").allAchievements());
    }

    @Test void tamerUsesItsReviewedOriginalAchievementUnlock() {
        var tamer = ArcadeAchievementKitRequirements.find("dragons", "tamer");
        assertNotNull(tamer);
        assertEquals(java.util.List.of("dragon-tamer"), tamer.achievementIds());
        assertTrue(ArcadeAchievementKitRequirements.kitIdsForAchievement("dragons", "adept").isEmpty());
        assertEquals(java.util.List.of("tamer"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("dragons", "dragon-tamer"));
    }

    @Test void knownRewardsCanBeShownInAchievementTooltips() {
        assertEquals(java.util.List.of("dasher"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("micro-battles", "speedster"));
        assertEquals(java.util.List.of("runnerkit-switcher"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("runner", "nsfm"));
    }
}
