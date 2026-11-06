package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AchievementKitUnlockPolicyTest {
    @Test void everyReviewedAchievementKitHasStableEarnableRequirements() {
        assertEquals(19, AchievementKitUnlockPolicy.all().size());
        for (AchievementKitUnlockPolicy.Rule rule : AchievementKitUnlockPolicy.all()) {
            assertFalse(rule.requirements().isEmpty(), rule.kitId());
            assertEquals(rule.requirements().size(), Set.copyOf(rule.requirements()).size(), rule.kitId());
            assertTrue(rule.unlocked(id -> true));
            assertFalse(rule.unlocked(id -> false));
            if (rule.originalAllAchievements()) assertEquals("runner", rule.gameId());
            for (String id : rule.requirements()) {
                if (id.startsWith("legacy-runner:")) {
                    assertTrue(rule.originalAllAchievements());
                } else if ("plexball".equals(rule.gameId())) {
                    assertTrue(java.util.Arrays.stream(PlexballAchievement.values())
                            .anyMatch(value -> value.slug().equals(id)), rule.kitId() + ":" + id);
                } else if ("camera-shy".equals(rule.gameId())) {
                    assertNotNull(CameraShyProgressCatalog.achievement(id));
                } else if ("super-sploor".equals(rule.gameId())) {
                    assertNotNull(SuperSploorProgressCatalog.achievement(id));
                } else {
                    assertTrue(ArcadeProgressCatalog.achievements(rule.gameId()).stream()
                            .anyMatch(value -> value.id().equals(id)), rule.kitId() + ":" + id);
                }
            }
        }
    }

    @Test void allReviewRulesCoverPurpleCatalogueExceptRemovedChrisBacon() {
        int purple = 0;
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (ArcadeLobbyKitCatalogue.KitChoice choice : ArcadeLobbyKitCatalogue.forGame(game, java.util.List.of())) {
                if (choice.unlock != ArcadeLobbyKitCatalogue.Unlock.ACHIEVEMENT) continue;
                if (game.isMonsterMaze() && game != ArcadePortGame.MONSTER_MAZE) continue;
                purple++;
                if ("chris-p-bacon".equals(choice.id)) continue;
                assertNotNull(AchievementKitUnlockPolicy.find(game, choice.id), game.id() + ":" + choice.id);
            }
        }
        assertEquals(20, purple);
    }

    @Test void combinationsRequireEverySpecificAchievement() {
        AchievementKitUnlockPolicy.Rule digger = AchievementKitUnlockPolicy.find(ArcadePortGame.DRAGON_ESCAPE, "digger");
        Set<String> earned = new HashSet<>(Set.of("to-hell-and-back"));
        assertEquals(java.util.List.of("paralympics"), digger.missing(earned::contains));
        assertFalse(digger.unlocked(earned::contains));
        earned.add("paralympics");
        assertTrue(digger.unlocked(earned::contains));
        assertFalse(digger.requirements().contains("learn-to-code"));
    }

    @Test void runnerMasteryIsFrozenAndKitExclusiveRequirementsAreNotCircular() {
        var runner = AchievementKitUnlockPolicy.find(ArcadePortGame.RUNNER, "runnerkit-switcher");
        assertTrue(runner.originalAllAchievements());
        assertEquals(10, runner.requirements().size());
        assertFalse(AchievementKitUnlockPolicy.find(ArcadePortGame.CAMERA_SHY, "snap-understudy")
                .requirements().contains("method-actor"));
        assertFalse(AchievementKitUnlockPolicy.find(ArcadePortGame.SUPER_SPLOOR, "dualies")
                .requirements().contains("master-of-the-palette"));
        assertEquals(1, AchievementKitUnlockPolicy.unlockedBy("monster-maze-speed", "perfect-run").size());
    }
}
