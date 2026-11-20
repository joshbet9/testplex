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
        assertEquals(java.util.List.of("builder"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("dragon-escape", "paralympics"));
        var digger = ArcadeAchievementKitRequirements.find("dragon-escape", "digger");
        assertNotNull(digger);
        assertEquals(java.util.List.of("skylands-master", "to-hell-and-back", "plundered"),
                digger.achievementIds());
        for (String kit : java.util.List.of("flutterfly", "acrobat")) {
            var requirement = ArcadeAchievementKitRequirements.find("dragon-escape", kit);
            assertNotNull(requirement);
            assertFalse(requirement.allAchievements());
            assertEquals(digger.achievementIds(), requirement.achievementIds());
        }
        for (String achievement : digger.achievementIds()) {
            assertEquals(java.util.List.of("acrobat", "digger", "flutterfly"),
                    ArcadeAchievementKitRequirements.kitIdsForAchievement("dragon-escape", achievement));
        }
        assertEquals(java.util.List.of("dasher"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("micro-battles", "speedster"));
        assertEquals(java.util.List.of("runnerkit-switcher"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("runner", "nsfm"));
        assertEquals(java.util.List.of("pitcher"),
                ArcadeAchievementKitRequirements.kitIdsForAchievement("bomb-lobbers", "two-birds-one-stone"));
        assertEquals(java.util.List.of("two-birds-one-stone"),
                ArcadeAchievementKitRequirements.find("bomb-lobbers", "pitcher").achievementIds());
    }
}
