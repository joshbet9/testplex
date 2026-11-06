package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SheepQuestAchievementRulesTest {
    @Test void cheekyRequiresFiveCapturesInsideFinalTenSecondsAndWinning() {
        long endedAt = 100_000L;
        assertTrue(SheepQuestAchievementRules.cheeky(true,
                List.of(90_000L, 92_000L, 95_000L, 99_999L, 100_000L), endedAt));
        assertFalse(SheepQuestAchievementRules.cheeky(true,
                List.of(89_999L, 92_000L, 95_000L, 99_999L, 100_000L), endedAt));
        assertFalse(SheepQuestAchievementRules.cheeky(false,
                List.of(90_000L, 92_000L, 95_000L, 99_999L, 100_000L), endedAt));
        assertFalse(SheepQuestAchievementRules.cheeky(true, List.of(), endedAt));
        assertFalse(SheepQuestAchievementRules.cheeky(true,
                List.of(90_000L, 92_000L, 95_000L, 99_999L, 100_001L), endedAt));
    }

    @Test void sheepQuestAchievementsRequireTheRightKitAndEventCounts() {
        assertTrue(SheepQuestAchievementRules.whatsASheep(true, 10, 0));
        assertFalse(SheepQuestAchievementRules.whatsASheep(true, 10, 1));
        assertTrue(SheepQuestAchievementRules.yeet(true, "beserker", 2, 0));
        assertFalse(SheepQuestAchievementRules.yeet(true, "beserker", 1, 1));
        assertTrue(SheepQuestAchievementRules.unconventional(true, "archer", false));
        assertFalse(SheepQuestAchievementRules.unconventional(true, "archer", true));
        assertTrue(SheepQuestAchievementRules.soloCarry(true, 3, 2));
        assertFalse(SheepQuestAchievementRules.soloCarry(true, 2, 2));
        assertTrue(SheepQuestAchievementRules.biggestLoser(false, 8));
        assertFalse(SheepQuestAchievementRules.biggestLoser(true, 8));
    }
}
