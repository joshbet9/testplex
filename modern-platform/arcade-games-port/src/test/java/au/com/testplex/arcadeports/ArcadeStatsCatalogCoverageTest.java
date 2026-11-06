package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** All 30 game entries must have a real progression source; no cosmetic-only placeholder pages. */
class ArcadeStatsCatalogCoverageTest {
    @Test
    void everyGameHasAchievementsAndMilestoneTracks() {
        assertEquals(30, ArcadeGameDirectory.games().size());
        for (ArcadeGameDirectory.Game game : ArcadeGameDirectory.games()) {
            String id = game.id();
            int achievements;
            int milestones;
            switch (id) {
                case "plexball" -> {
                    achievements = PlexballAchievement.values().length;
                    milestones = PlexballMilestone.values().length;
                }
                case "camera-shy" -> {
                    achievements = CameraShyProgressCatalog.achievements().size();
                    milestones = CameraShyProgressCatalog.milestones().size();
                }
                case "super-sploor" -> {
                    achievements = SuperSploorProgressCatalog.achievements().size();
                    milestones = SuperSploorProgressCatalog.milestones().size();
                }
                default -> {
                    String catalogId = "speed-builders".equals(id) ? "speed-builders-arcade" : id;
                    List<ArcadeProgressCatalog.Achievement> entries = ArcadeProgressCatalog.achievements(catalogId);
                    achievements = entries.size();
                    milestones = ArcadeProgressCatalog.milestones(catalogId).size();
                    assertEquals(entries.size(), entries.stream().map(ArcadeProgressCatalog.Achievement::id)
                            .collect(java.util.stream.Collectors.toCollection(HashSet::new)).size(), id);
                    for (ArcadeProgressCatalog.Milestone milestone : ArcadeProgressCatalog.milestones(catalogId)) {
                        long previous = 0L;
                        for (long threshold : milestone.stages()) {
                            assertTrue(threshold >= previous, id + " / " + milestone.id());
                            previous = threshold;
                        }
                        assertEquals(milestone.maximum(), previous, id + " / " + milestone.id());
                    }
                }
            }
            assertTrue(achievements > 0, id + " must have achievement entries");
            assertTrue(milestones > 0, id + " must have milestone entries");
        }
    }
}
