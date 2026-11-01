package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Regression coverage for the complete Dragons live-playtest achievement roster. */
class DragonsPlaytestAchievementsTest {
    @Test
    void hasAllElevenDragonAchievementsWithUniqueIds() {
        var achievements = ArcadeProgressCatalog.achievements("dragons");
        var ids = achievements.stream().map(ArcadeProgressCatalog.Achievement::id)
                .collect(Collectors.toSet());
        assertEquals(11, achievements.size());
        assertEquals(11, ids.size());
        assertEquals(Set.of(
                "win-three-in-a-row", "survive-five-minutes", "no-damage-win",
                "adept", "sharpshooter", "ultra", "survivors-guilt",
                "freefallin", "dodgems", "sticks-and-stones", "mlg-pro"), ids);
    }

    @Test
    void eachDragonAchievementHasVisibleNameAndRequirement() {
        for (var achievement : ArcadeProgressCatalog.achievements("dragons")) {
            assertTrue(achievement.name() != null && !achievement.name().isBlank());
            assertTrue(achievement.requirement() != null && !achievement.requirement().isBlank());
        }
    }
}
