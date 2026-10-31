package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SuperSploorProgressCatalogTest {
    @Test void exposesTheFifteenDesignedAchievements() {
        assertEquals(15, SuperSploorProgressCatalog.achievements().size());
        assertNotNull(SuperSploorProgressCatalog.achievement("first-splash"));
        assertNotNull(SuperSploorProgressCatalog.achievement("master-of-the-palette"));
        assertNull(SuperSploorProgressCatalog.achievement("not-a-super-sploor-achievement"));
    }
    @Test void exposesSevenFourStageLifetimeMilestones() {
        assertEquals(7, SuperSploorProgressCatalog.milestones().size());
        for (SuperSploorProgressCatalog.Milestone milestone : SuperSploorProgressCatalog.milestones()) {
            assertEquals(4, milestone.stages().length, milestone.id());
            assertEquals(milestone.maximum(), milestone.stages()[3], milestone.id());
            assertTrue(milestone.stages()[0] > 0L, milestone.id());
            assertTrue(milestone.stages()[0] < milestone.stages()[1], milestone.id());
            assertTrue(milestone.stages()[1] < milestone.stages()[2], milestone.id());
            assertTrue(milestone.stages()[2] < milestone.stages()[3], milestone.id());
        }
    }
    @Test void achievementsUseStablePerPlayerConfigKeys() {
        UUID player = UUID.fromString("ed2f6b6b-f2d4-4720-9a6c-c3d52f86b9b7");
        assertEquals("local-achievements." + player + ".super-sploor.first-splash",
                SuperSploorProgressCatalog.achievementConfigKey(player, "first-splash"));
        assertEquals("", SuperSploorProgressCatalog.achievementConfigKey(player, "unknown"));
        assertEquals("", SuperSploorProgressCatalog.achievementConfigKey(null, "first-splash"));
    }
}
