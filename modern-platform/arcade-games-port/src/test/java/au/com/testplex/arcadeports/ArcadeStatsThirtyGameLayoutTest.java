package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Regression matrix for every actual game catalogue, not just arbitrary geometry samples. */
class ArcadeStatsThirtyGameLayoutTest {
    @Test void allGamesHaveCollisionFreeAchievementPagesAndCompleteMilestoneTierCoverage() {
        assertEquals(30, ArcadeGameDirectory.games().size());
        for (ArcadeGameDirectory.Game game : ArcadeGameDirectory.games()) {
            String id = game.id();
            int achievementCount;
            List<long[]> milestones;
            switch (id) {
                case "plexball" -> {
                    achievementCount = PlexballAchievement.values().length;
                    milestones = java.util.Arrays.stream(PlexballMilestone.values())
                            .map(value -> ArcadeStatsLayout.fourStages(value.target())).toList();
                }
                case "camera-shy" -> {
                    achievementCount = CameraShyProgressCatalog.achievements().size();
                    milestones = CameraShyProgressCatalog.milestones().stream()
                            .map(value -> ArcadeStatsLayout.fourStages(value.target())).toList();
                }
                case "super-sploor" -> {
                    achievementCount = SuperSploorProgressCatalog.achievements().size();
                    milestones = SuperSploorProgressCatalog.milestones().stream()
                            .map(SuperSploorProgressCatalog.Milestone::stages).toList();
                }
                default -> {
                    String catalogId = "speed-builders".equals(id) ? "speed-builders-arcade" : id;
                    achievementCount = ArcadeProgressCatalog.achievements(catalogId).size();
                    milestones = ArcadeProgressCatalog.milestones(catalogId).stream()
                            .map(value -> java.util.Arrays.stream(value.stages()).distinct().toArray()).toList();
                }
            }

            Set<Integer> reserved = Set.of(0, 8, 13, 45, 53);
            int seenAchievements = 0;
            int achievementPages = ArcadeStatsLayout.pages(achievementCount, 14);
            for (int page = 0; page < achievementPages; page++) {
                int count = Math.min(14, achievementCount - page * 14);
                List<Integer> slots = ArcadeStatsLayout.achievementSlots(count);
                assertEquals(count, new HashSet<>(slots).size(), id + " page " + page);
                for (int slot : slots) {
                    assertTrue(slot >= 18 && slot <= 35, id + " achievement outside centred rows");
                    assertFalse(reserved.contains(slot), id + " achievement overlaps control");
                }
                seenAchievements += slots.size();
            }
            assertEquals(achievementCount, seenAchievements, id + " achievement pagination lost entries");

            int expectedTiers = milestones.stream().mapToInt(stages -> stages.length).sum();
            int visitedTiers = 0;
            for (int trackStart = 0; trackStart < milestones.size(); trackStart += 4) {
                List<long[]> tracks = milestones.subList(trackStart, Math.min(trackStart + 4, milestones.size()));
                int maxStages = tracks.stream().mapToInt(stages -> stages.length).max().orElse(0);
                for (int stageStart = 0; stageStart < maxStages; stageStart += 4) {
                    int offset = stageStart;
                    List<long[]> visible = tracks.stream().filter(stages -> stages.length > offset).toList();
                    List<Integer> columns = ArcadeStatsLayout.milestoneColumns(visible.size());
                    Set<Integer> occupied = new HashSet<>();
                    for (int index = 0; index < visible.size(); index++) {
                        int column = columns.get(index);
                        assertTrue(occupied.add(column), id + " colliding milestone headers");
                        for (int stage = stageStart; stage < Math.min(visible.get(index).length, stageStart + 4); stage++) {
                            int slot = 9 * (stage - stageStart + 1) + column;
                            assertTrue(occupied.add(slot), id + " milestone tier collision at " + slot);
                            assertTrue(slot >= 9 && slot <= 44, id + " milestone outside middle rows");
                            visitedTiers++;
                        }
                    }
                }
            }
            assertEquals(expectedTiers, visitedTiers, id + " milestone pagination lost tiers");
        }
    }
}
