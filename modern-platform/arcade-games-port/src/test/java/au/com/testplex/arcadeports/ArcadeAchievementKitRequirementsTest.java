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

    @Test void unauditedKitsDoNotReceiveInventedUnlockRequirements() {
        assertNull(ArcadeAchievementKitRequirements.find("dragons", "tamer"));
    }
}
