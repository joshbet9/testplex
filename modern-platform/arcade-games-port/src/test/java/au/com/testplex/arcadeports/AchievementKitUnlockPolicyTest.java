package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AchievementKitUnlockPolicyTest {
    @Test void everyReviewedAchievementKitHasStableEarnableRequirements() {
        assertEquals(20, AchievementKitUnlockPolicy.all().size());
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

    @Test void allReviewedRulesCoverEveryPurpleCatalogueKitIncludingChrisBacon() {
        int purple = 0;
        for (ArcadePortGame game : ArcadePortGame.values()) {
            for (ArcadeLobbyKitCatalogue.KitChoice choice : ArcadeLobbyKitCatalogue.forGame(game, java.util.List.of())) {
                if (choice.unlock != ArcadeLobbyKitCatalogue.Unlock.ACHIEVEMENT) continue;
                if (game.isMonsterMaze() && game != ArcadePortGame.MONSTER_MAZE) continue;
                purple++;
                assertNotNull(AchievementKitUnlockPolicy.find(game, choice.id), game.id() + ":" + choice.id);
            }
        }
        assertEquals(20, purple);
    }

    @Test void combinationsRequireEverySpecificAchievement() {
        var constructor = AchievementKitUnlockPolicy.find(ArcadePortGame.BOMB_LOBBERS, "constructor");
        assertEquals(java.util.List.of("two-birds-one-stone", "whats-a-double-jump", "blast-proof", "jelly-skin"),
                constructor.requirements());
        assertFalse(constructor.requirements().contains("not-on-my-watch"));
        assertFalse(constructor.unlocked(Set.of("blast-proof", "jelly-skin")::contains));
        assertTrue(constructor.unlocked(Set.copyOf(constructor.requirements())::contains));
        AchievementKitUnlockPolicy.Rule digger = AchievementKitUnlockPolicy.find(ArcadePortGame.DRAGON_ESCAPE, "digger");
        Set<String> earned = new HashSet<>(Set.of("to-hell-and-back"));
        assertEquals(java.util.List.of("paralympics"), digger.missing(earned::contains));
        assertFalse(digger.unlocked(earned::contains));
        earned.add("paralympics");
        assertTrue(digger.unlocked(earned::contains));
        assertFalse(digger.requirements().contains("learn-to-code"));
        var chris = AchievementKitUnlockPolicy.find(ArcadePortGame.BACON_BRAWL, "chris-p-bacon");
        assertEquals(java.util.List.of("flash-fried", "raw-pork"), chris.requirements());
        assertFalse(chris.unlocked(Set.of("flash-fried")::contains));
        assertTrue(chris.unlocked(Set.of("flash-fried", "raw-pork")::contains));
        assertFalse(chris.requirements().contains("extra-crispy"));
    }

    @Test void runnerSwitcherRequiresJumperFrostyAndArcherMasteryWithoutCircularUnlocks() {
        var runner = AchievementKitUnlockPolicy.find(ArcadePortGame.RUNNER, "runnerkit-switcher");
        assertFalse(runner.originalAllAchievements());
        assertEquals(java.util.List.of("long-leaper", "cold-front", "deadeye"), runner.requirements());
        assertFalse(runner.unlocked(Set.of("long-leaper", "cold-front")::contains));
        assertTrue(runner.unlocked(Set.of("long-leaper", "cold-front", "deadeye")::contains));
        assertFalse(AchievementKitUnlockPolicy.find(ArcadePortGame.CAMERA_SHY, "snap-understudy")
                .requirements().contains("method-actor"));
        assertFalse(AchievementKitUnlockPolicy.find(ArcadePortGame.SUPER_SPLOOR, "dualies")
                .requirements().contains("master-of-the-palette"));
        assertEquals(0, AchievementKitUnlockPolicy.unlockedBy("monster-maze-speed", "perfect-run").size());
        assertEquals(1, AchievementKitUnlockPolicy.unlockedBy("monster-maze-speed", "pilot").size());
        assertEquals(java.util.List.of("pilot"),
                AchievementKitUnlockPolicy.find(ArcadePortGame.MONSTER_MAZE, "maverick").requirements());
        var turtle = AchievementKitUnlockPolicy.find(ArcadePortGame.DEATH_TAG, "runner-turtle");
        var trickster = AchievementKitUnlockPolicy.find(ArcadePortGame.DEATH_TAG, "runner-trickster");
        assertEquals(java.util.List.of("lots-of-subject-matter"), turtle.requirements());
        assertEquals(java.util.List.of("smoke-and-mirrors"), trickster.requirements());
        assertFalse(turtle.requirements().contains("shell-shock"));
        assertFalse(trickster.requirements().contains("sleight-of-hand"));
    }
}
