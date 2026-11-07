package me.monstermaze.game;

import java.util.Set;
import java.util.UUID;

/** Pure predicates for Monster Maze achievements; runtime code supplies match event facts. */
public final class MonsterMazeAchievementRules {
    private MonsterMazeAchievementRules() { }

    public static int nextPilotStreak(int currentStreak, int previousStage, int pilotedStage) {
        if (pilotedStage <= 0 || pilotedStage == previousStage) return Math.max(0, currentStreak);
        return pilotedStage == previousStage + 1 ? Math.max(0, currentStreak) + 1 : 1;
    }

    public static boolean pilotedEveryStage(Set<Integer> pilotedStages, int finalStage) {
        if (pilotedStages == null || finalStage < 1) return false;
        for (int stage = 1; stage <= finalStage; stage++) {
            if (!pilotedStages.contains(stage)) return false;
        }
        return true;
    }

    public static boolean shouldAwardPhotoFinish(boolean playerSecuredPad,
                                                  Set<UUID> nearbyOpponents,
                                                  Set<UUID> playersWhoSecuredPad) {
        if (!playerSecuredPad || nearbyOpponents == null || nearbyOpponents.isEmpty()) return false;
        for (UUID opponent : nearbyOpponents) {
            if (opponent != null && (playersWhoSecuredPad == null || !playersWhoSecuredPad.contains(opponent))) {
                return true;
            }
        }
        return false;
    }

    public static boolean winnerEarnedPhotoFinish(boolean isWinner, UUID playerId, Set<UUID> candidates) {
        return isWinner && playerId != null && candidates != null && candidates.contains(playerId);
    }

    public static boolean shouldAwardOutOfTheCup(int firstToPadStage, int slowballStage, boolean targetMissedPad) {
        return firstToPadStage > 0 && firstToPadStage == slowballStage && targetMissedPad;
    }

    public static boolean reachedThreshold(Set<?> uniqueEntities, int threshold) {
        return uniqueEntities != null && threshold > 0 && uniqueEntities.size() >= threshold;
    }

    public static boolean shouldAwardRepulsor(Set<UUID> uniqueMonsters) {
        return reachedThreshold(uniqueMonsters, 20);
    }

    public static boolean shouldAwardPilot(int consecutivePilotedStages) {
        return consecutivePilotedStages >= 3;
    }

    public static boolean shouldAwardNinja(boolean won, boolean hitByMonster) {
        return won && !hitByMonster;
    }

    public static boolean shouldAwardHardMode(boolean won, boolean usedAbility) {
        return won && !usedAbility;
    }

    public static boolean shouldAwardTailwind(boolean won, boolean usedMaverick,
                                               Set<Integer> pilotedStages, int finalStage) {
        return won && usedMaverick && pilotedEveryStage(pilotedStages, finalStage);
    }
}
