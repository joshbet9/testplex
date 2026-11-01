package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpeedBuildersRulesTest {
    @Test
    void onlyMatchingTemplateCellsScore() {
        List<SpeedBuildersRules.Cell> expected = List.of(
                new SpeedBuildersRules.Cell(3, 0, 3, Material.STONE),
                new SpeedBuildersRules.Cell(3, 1, 3, Material.OAK_PLANKS),
                new SpeedBuildersRules.Cell(4, 1, 3, Material.GLASS));
        Map<SpeedBuildersRules.Coordinate, Material> actual = Map.of(
                new SpeedBuildersRules.Coordinate(3, 0, 3), Material.STONE,
                new SpeedBuildersRules.Coordinate(3, 1, 3), Material.OAK_PLANKS,
                new SpeedBuildersRules.Coordinate(4, 1, 3), Material.WHITE_WOOL);
        assertEquals(2, SpeedBuildersRules.score(expected, actual));
        assertFalse(SpeedBuildersRules.isPerfect(expected, actual));
    }

    @Test
    void perfectBuildRequiresEveryExpectedCell() {
        List<SpeedBuildersRules.Cell> expected = List.of(
                new SpeedBuildersRules.Cell(0, 0, 0, Material.STONE),
                new SpeedBuildersRules.Cell(1, 0, 0, Material.STONE));
        Map<SpeedBuildersRules.Coordinate, Material> actual = Map.of(
                new SpeedBuildersRules.Coordinate(0, 0, 0), Material.STONE,
                new SpeedBuildersRules.Coordinate(1, 0, 0), Material.STONE);
        assertTrue(SpeedBuildersRules.isPerfect(expected, actual));
    }

    @Test
    void templateCellsCannotEscapeTheSevenBlockBuildVolume() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpeedBuildersRules.Cell(7, 0, 0, Material.STONE));
        assertThrows(IllegalArgumentException.class,
                () -> new SpeedBuildersRules.Cell(0, -1, 0, Material.STONE));
        assertThrows(IllegalArgumentException.class,
                () -> new SpeedBuildersRules.Cell(0, 0, 7, Material.STONE));
    }

    @Test
    void templateTimingModifiersRespectSourceClamp() {
        assertEquals(40, SpeedBuildersRules.buildSeconds(40, 0, 0, 1.0D, null));
        assertEquals(45, SpeedBuildersRules.buildSeconds(40, 5, 0, 1.0D, null));
        assertEquals(20, SpeedBuildersRules.buildSeconds(40, 0, 0, 0.5D, null));
        assertEquals(12, SpeedBuildersRules.buildSeconds(40, 0, 0, 2.0D, 12));
        assertEquals(5, SpeedBuildersRules.buildSeconds(10, 0, 20, 1.0D, null));
        assertEquals(60, SpeedBuildersRules.buildSeconds(40, 40, 0, 2.0D, null));
    }

 
    @Test
    void sourceBlockStateScoringChecksOrientationAndState() {
        assertTrue(SpeedBuildersRules.blockMatches(Material.OAK_STAIRS,
                "minecraft:oak_stairs[facing=north,half=bottom,shape=inner_left,waterlogged=false]",
                Material.OAK_STAIRS,
                "minecraft:oak_stairs[facing=north,half=bottom,shape=inner_right,waterlogged=false]"));
        assertFalse(SpeedBuildersRules.blockMatches(Material.OAK_STAIRS,
                "minecraft:oak_stairs[facing=north,half=bottom,shape=inner_left,waterlogged=false]",
                Material.OAK_STAIRS,
                "minecraft:oak_stairs[facing=south,half=bottom,shape=inner_right,waterlogged=false]"));
        assertFalse(SpeedBuildersRules.blockMatches(Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=east,eye=true]",
                Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=east,eye=false]"));
        assertTrue(SpeedBuildersRules.blockMatches(Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=east,eye=true]",
                Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=west,eye=true]"));
        assertFalse(SpeedBuildersRules.blockMatches(Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=east,eye=true]",
                Material.END_PORTAL_FRAME,
                "minecraft:end_portal_frame[facing=west,eye=false]"));
        assertTrue(SpeedBuildersRules.blockMatches(Material.OAK_LEAVES,
                "minecraft:oak_leaves[distance=7,persistent=false]",
                Material.OAK_LEAVES,
                "minecraft:oak_leaves[distance=2,persistent=true]"));
        assertTrue(SpeedBuildersRules.blockMatches(Material.REDSTONE_ORE,
                "minecraft:redstone_ore[lit=false]",
                Material.REDSTONE_ORE,
                "minecraft:redstone_ore[lit=true]"));
        assertTrue(SpeedBuildersRules.blockMatches(Material.ANVIL,
                "minecraft:anvil[facing=east]",
                Material.CHIPPED_ANVIL,
                "minecraft:chipped_anvil[facing=east]"));
    }

    @Test
    void sourceScorerCountsBlockDataMatchesButDoesNotPenaliseExtraBlocks() {
        var expected = List.of(
                new SpeedBuildersRules.Cell(0, 0, 0, Material.OAK_STAIRS,
                        "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"),
                new SpeedBuildersRules.Cell(1, 0, 0, Material.STONE, "minecraft:stone"));
        Map<SpeedBuildersRules.Coordinate, SpeedBuildersRules.BlockState> actual = Map.of(
                new SpeedBuildersRules.Coordinate(0, 0, 0),
                new SpeedBuildersRules.BlockState(Material.OAK_STAIRS,
                        "minecraft:oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]"),
                new SpeedBuildersRules.Coordinate(1, 0, 0),
                new SpeedBuildersRules.BlockState(Material.DIRT, "minecraft:dirt"),
                new SpeedBuildersRules.Coordinate(2, 0, 0),
                new SpeedBuildersRules.BlockState(Material.DIAMOND_BLOCK, "minecraft:diamond_block"));
        assertEquals(1, SpeedBuildersRules.scoreBlockStates(expected, actual));
    }

    @Test
    void sourceTemplateNameModifiersAreRemovedFromTheDisplayedName() {
        var timing = SpeedBuildersRules.parseTemplateName("Oak Tree time+5 time*0.5");
        assertEquals("Oak Tree", timing.displayName());
        assertEquals(22, timing.buildSeconds(40));
        assertEquals(12, SpeedBuildersRules.parseTemplateName("Flower time=12").buildSeconds(40));
        assertEquals(35, SpeedBuildersRules.parseTemplateName("Bridge time-5").buildSeconds(40));
        assertEquals(5, SpeedBuildersRules.parseTemplateName("Tiny Model time*0").buildSeconds(40));
    }

    @Test
    void higherCumulativeAccuracyAlwaysBeatsAPlayerWithMoreBuildTime() {
        assertEquals(List.of("accurate-slower"),
                SpeedBuildersRules.winnersByScoreThenBuildTime(
                        Map.of("accurate-slower", 410, "fast-but-less-accurate", 390),
                        Map.of("accurate-slower", 2400L, "fast-but-less-accurate", 900L)));
    }

    @Test
    void tiedCumulativeAccuracyIsBrokenByLowestCumulativeBuildTime() {
        assertEquals(List.of("fastest"),
                SpeedBuildersRules.winnersByScoreThenBuildTime(
                        Map.of("slow", 410, "fastest", 410, "middle", 410, "lower-score", 390),
                        Map.of("slow", 2500L, "fastest", 1800L, "middle", 2100L, "lower-score", 400L)));
    }

    @Test
    void exactAccuracyAndCumulativeBuildTimeTieRemainsShared() {
        assertEquals(List.of("builder-b", "builder-c"),
                SpeedBuildersRules.winnersByScoreThenBuildTime(
                        Map.of("builder-a", 280, "builder-b", 410, "builder-c", 410, "builder-d", 95),
                        Map.of("builder-a", 100L, "builder-b", 1800L, "builder-c", 1800L, "builder-d", 50L)));
        assertEquals(List.of("only"),
                SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of("only", 0), Map.of("only", 0L)));
        assertEquals(List.of(), SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of(), Map.of()));
    }

    @Test
    void scoreTimeWinnerCalculationRejectsMalformedOrMismatchedRosters() {
        assertThrows(IllegalArgumentException.class,
                () -> SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of("", 10), Map.of("", 20L)));
        assertThrows(IllegalArgumentException.class,
                () -> SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of("builder", -1), Map.of("builder", 20L)));
        assertThrows(IllegalArgumentException.class,
                () -> SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of("builder", 10), Map.of("builder", -1L)));
        assertThrows(IllegalArgumentException.class,
                () -> SpeedBuildersRules.winnersByScoreThenBuildTime(Map.of("builder-a", 10), Map.of("builder-b", 20L)));
    }

    @Test
    void accuracyPointsAreNormalizedWithoutEliminatingLowScorers() {
        assertEquals(0, SpeedBuildersRules.roundPoints(0, 10));
        assertEquals(33, SpeedBuildersRules.roundPoints(1, 3));
        assertEquals(67, SpeedBuildersRules.roundPoints(2, 3));
        assertEquals(100, SpeedBuildersRules.roundPoints(10, 10));
        assertEquals(0, SpeedBuildersRules.roundPoints(0, 0));
        assertThrows(IllegalArgumentException.class, () -> SpeedBuildersRules.roundPoints(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> SpeedBuildersRules.roundPoints(11, 10));
    }

}
