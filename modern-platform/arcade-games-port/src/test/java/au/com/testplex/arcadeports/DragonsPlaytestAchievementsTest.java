package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Regression coverage for the complete Dragons live-playtest achievement roster. */
class DragonsPlaytestAchievementsTest {
    @Test
    void hasAllFourteenDragonAchievementsWithUniqueIds() {
        var achievements = ArcadeProgressCatalog.achievements("dragons");
        var ids = achievements.stream().map(ArcadeProgressCatalog.Achievement::id)
                .collect(Collectors.toSet());
        assertEquals(14, achievements.size());
        assertEquals(14, ids.size());
        assertEquals(Set.of(
                "dragon-tamer", "bridge-over-troubled-void", "wing-it-to-win-it", "dragon-whisperer",
                "win-three-in-a-row", "survive-five-minutes", "no-damage-win",
                "sharpshooter", "ultra", "survivors-guilt",
                "freefallin", "dodgems", "sticks-and-stones", "mlg-pro"), ids);
        assertFalse(ids.contains("sparklez"));
    }

    @Test
    void eachDragonAchievementHasVisibleNameAndRequirement() {
        for (var achievement : ArcadeProgressCatalog.achievements("dragons")) {
            assertTrue(achievement.name() != null && !achievement.name().isBlank());
            assertTrue(achievement.requirement() != null && !achievement.requirement().isBlank());
        }
    }

    @Test
    void tamerIsAchievementLockedByAnAchievementEarnableWithoutTamer() {
        var tamer = AchievementKitUnlockPolicy.find(ArcadePortGame.DRAGONS, "tamer");
        assertNotNull(tamer);
        assertEquals(java.util.List.of("dragon-tamer"), tamer.requirements());
        assertTrue(ArcadeProgressCatalog.achievements("dragons").stream()
                .anyMatch(achievement -> achievement.id().equals("dragon-tamer")));
    }
}
