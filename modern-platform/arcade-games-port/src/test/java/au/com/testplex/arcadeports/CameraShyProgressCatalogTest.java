package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CameraShyProgressCatalogTest {
    @Test
    void definesExactlyTheTwentyAgreedAchievements() {
        var achievements = CameraShyProgressCatalog.achievements();
        assertEquals(20, achievements.size());
        assertEquals(20, new HashSet<>(achievements.stream().map(CameraShyProgressCatalog.Achievement::id).toList()).size());
        assertEquals(20, new HashSet<>(achievements.stream().map(CameraShyProgressCatalog.Achievement::name).toList()).size());
        assertEquals(20, new HashSet<>(achievements.stream().map(CameraShyProgressCatalog.Achievement::requirement).toList()).size());
        assertEquals("Revenge Is Sweet", CameraShyProgressCatalog.achievement("revenge-is-sweet").name());
        assertEquals("Group Portrait", CameraShyProgressCatalog.achievement("group-portrait").name());
    }

    @Test
    void achievementPagesCoverTheCatalogueExactlyOnceAndClampInvalidPages() {
        assertEquals(4, CameraShyProgressCatalog.pageCount());
        HashSet<String> ids = new HashSet<>();
        for (int page = 1; page <= CameraShyProgressCatalog.pageCount(); page++) {
            var entries = CameraShyProgressCatalog.page(page);
            assertEquals(5, entries.size());
            for (var entry : entries) assertTrue(ids.add(entry.id()), "duplicate page entry: " + entry.id());
        }
        assertEquals(20, ids.size());
        assertEquals(CameraShyProgressCatalog.page(1), CameraShyProgressCatalog.page(0));
        assertEquals(CameraShyProgressCatalog.page(4), CameraShyProgressCatalog.page(99));
    }

    @Test
    void definesTheThreeLifetimeMilestonesAtTheAgreedThresholds() {
        var milestones = CameraShyProgressCatalog.milestones();
        assertEquals(3, milestones.size());
        assertEquals(250, milestones.get(0).target());
        assertEquals("wins", milestones.get(0).statKey());
        assertEquals(500, milestones.get(1).target());
        assertEquals("eliminations", milestones.get(1).statKey());
        assertEquals(1_000, milestones.get(2).target());
        assertEquals("photos", milestones.get(2).statKey());
    }
}
