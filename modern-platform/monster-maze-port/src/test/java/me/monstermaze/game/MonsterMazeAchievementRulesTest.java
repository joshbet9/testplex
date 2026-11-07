package me.monstermaze.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MonsterMazeAchievementRulesTest {
    @Test
    void pilotStreakRequiresThreeDifferentConsecutiveStages() {
        assertEquals(1, MonsterMazeAchievementRules.nextPilotStreak(0, 0, 1));
        assertEquals(2, MonsterMazeAchievementRules.nextPilotStreak(1, 1, 2));
        assertEquals(3, MonsterMazeAchievementRules.nextPilotStreak(2, 2, 3));
        assertEquals(2, MonsterMazeAchievementRules.nextPilotStreak(2, 2, 2));
        assertEquals(1, MonsterMazeAchievementRules.nextPilotStreak(2, 1, 3));
    }

    @Test
    void tailwindRequiresPilotingEveryStageThroughTheWin() {
        assertTrue(MonsterMazeAchievementRules.pilotedEveryStage(Set.of(1, 2, 3), 3));
        assertFalse(MonsterMazeAchievementRules.pilotedEveryStage(Set.of(1, 3), 3));
        assertFalse(MonsterMazeAchievementRules.pilotedEveryStage(Set.of(1, 2, 3), 4));
    }

    @Test
    void photoFinishRequiresSecuringPadAndNearbyOpponentMissingIt() {
        UUID rival = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        assertTrue(MonsterMazeAchievementRules.shouldAwardPhotoFinish(true, Set.of(rival), Set.of()));
        assertFalse(MonsterMazeAchievementRules.shouldAwardPhotoFinish(false, Set.of(rival), Set.of()));
        assertFalse(MonsterMazeAchievementRules.shouldAwardPhotoFinish(true, Set.of(rival), Set.of(rival)));
        assertTrue(MonsterMazeAchievementRules.winnerEarnedPhotoFinish(true, player, Set.of(player)));
        assertFalse(MonsterMazeAchievementRules.winnerEarnedPhotoFinish(false, player, Set.of(player)));
    }

    @Test
    void outOfTheCupRequiresAFirstPadSlowballAndMissInTheSameStage() {
        assertTrue(MonsterMazeAchievementRules.shouldAwardOutOfTheCup(4, 4, true));
        assertFalse(MonsterMazeAchievementRules.shouldAwardOutOfTheCup(3, 4, true));
        assertFalse(MonsterMazeAchievementRules.shouldAwardOutOfTheCup(4, 4, false));
    }

    @Test
    void launchAchievementsUseDistinctMonsterIds() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertFalse(MonsterMazeAchievementRules.reachedThreshold(Set.of(first), 2));
        assertTrue(MonsterMazeAchievementRules.reachedThreshold(Set.of(first, second), 2));
        assertFalse(MonsterMazeAchievementRules.reachedThreshold(Set.of(first, second), 0));
        assertFalse(MonsterMazeAchievementRules.shouldAwardRepulsor(Set.of(first, second)));
        assertTrue(MonsterMazeAchievementRules.shouldAwardRepulsor(java.util.stream.IntStream.range(0, 20)
                .mapToObj(ignored -> UUID.randomUUID()).collect(java.util.stream.Collectors.toSet())));
    }

    @Test
    void ninjaAndHardModeRequireWinningWithoutTheDisqualifyingEvent() {
        assertTrue(MonsterMazeAchievementRules.shouldAwardNinja(true, false));
        assertFalse(MonsterMazeAchievementRules.shouldAwardNinja(true, true));
        assertFalse(MonsterMazeAchievementRules.shouldAwardNinja(false, false));
        assertTrue(MonsterMazeAchievementRules.shouldAwardHardMode(true, false));
        assertFalse(MonsterMazeAchievementRules.shouldAwardHardMode(true, true));
        assertFalse(MonsterMazeAchievementRules.shouldAwardHardMode(false, false));
    }

    @Test
    void tailwindRequiresMaverickWinAndEveryStagePiloted() {
        assertTrue(MonsterMazeAchievementRules.shouldAwardTailwind(true, true, Set.of(1, 2), 2));
        assertFalse(MonsterMazeAchievementRules.shouldAwardTailwind(true, false, Set.of(1, 2), 2));
        assertFalse(MonsterMazeAchievementRules.shouldAwardTailwind(true, true, Set.of(1), 2));
        assertFalse(MonsterMazeAchievementRules.shouldAwardTailwind(false, true, Set.of(1, 2), 2));
    }
}
