package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class DrawMyThingAchievementRulesTest {
    @Test
    void minimalistMasterpieceRequiresTwoHumanSolvesAndAtMostTwelvePaintedBlocks() {
        assertTrue(DrawMyThingAchievementRules.minimalistMasterpiece(12, 2));
        assertFalse(DrawMyThingAchievementRules.minimalistMasterpiece(13, 2));
        assertFalse(DrawMyThingAchievementRules.minimalistMasterpiece(12, 1));
        assertFalse(DrawMyThingAchievementRules.minimalistMasterpiece(-1, 2));
    }

    @Test
    void blankWhiteWoolIsNotCountedAsPaintAndToolChangesAreDistinctCanvasCells() {
        assertFalse(DrawMyThingAchievementRules.isPaintedBlock(Material.WHITE_WOOL));
        assertFalse(DrawMyThingAchievementRules.isPaintedBlock(Material.AIR));
        assertTrue(DrawMyThingAchievementRules.isPaintedBlock(Material.BLACK_WOOL));
        Map<Integer, Material> before = Map.of(0, Material.WHITE_WOOL, 1, Material.BLACK_WOOL,
                2, Material.RED_WOOL);
        Map<Integer, Material> after = Map.of(0, Material.BLUE_WOOL, 1, Material.BLACK_WOOL,
                2, Material.WHITE_WOOL);
        assertEquals(2, DrawMyThingAchievementRules.countChangedBlocks(before, after));
    }

    @Test
    void toolAchievementsRequireTheSpecifiedDistinctBlockChanges() {
        assertTrue(DrawMyThingAchievementRules.qualifiesToolUse("Spray Can", 20));
        assertFalse(DrawMyThingAchievementRules.qualifiesToolUse("Spray Can", 19));
        assertTrue(DrawMyThingAchievementRules.qualifiesToolUse("Line Tool", 12));
        assertFalse(DrawMyThingAchievementRules.qualifiesToolUse("Line Tool", 11));
        assertTrue(DrawMyThingAchievementRules.qualifiesToolUse("Circle Tool", 16));
        assertFalse(DrawMyThingAchievementRules.qualifiesToolUse("Circle Tool", 15));
        assertFalse(DrawMyThingAchievementRules.qualifiesToolUse("Paint Bucket", 100));
    }

    @Test
    void cleanSlateRequiresARealDrawingBeforeAndAfterTheClear() {
        assertTrue(DrawMyThingAchievementRules.cleanSlate(8, 8));
        assertFalse(DrawMyThingAchievementRules.cleanSlate(7, 20));
        assertFalse(DrawMyThingAchievementRules.cleanSlate(20, 7));
    }

    @Test
    void lastSecondAndTwoHatsUseInclusiveBoundaries() {
        assertTrue(DrawMyThingAchievementRules.lastSecondSketch(3_000));
        assertTrue(DrawMyThingAchievementRules.lastSecondSketch(0));
        assertFalse(DrawMyThingAchievementRules.lastSecondSketch(3_001));
        assertFalse(DrawMyThingAchievementRules.lastSecondSketch(-1));
        assertTrue(DrawMyThingAchievementRules.twoHats(2, 6));
        assertFalse(DrawMyThingAchievementRules.twoHats(1, 20));
        assertFalse(DrawMyThingAchievementRules.twoHats(10, 5));
    }

    @Test
    void guessingAchievementsRequireTheirExactRoundAndHintConditions() {
        assertTrue(DrawMyThingAchievementRules.readBetweenTheLines(true, 1));
        assertFalse(DrawMyThingAchievementRules.readBetweenTheLines(false, 1));
        assertFalse(DrawMyThingAchievementRules.readBetweenTheLines(true, 2));
        assertTrue(DrawMyThingAchievementRules.thirdTimesTheCharm(3, 2, 8));
        assertTrue(DrawMyThingAchievementRules.thirdTimesTheCharm(3, 0, 3));
        assertFalse(DrawMyThingAchievementRules.thirdTimesTheCharm(2, 0, 8));
        assertFalse(DrawMyThingAchievementRules.thirdTimesTheCharm(3, 3, 8));
        assertFalse(DrawMyThingAchievementRules.thirdTimesTheCharm(3, 0, 0));
    }
}
