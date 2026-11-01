package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        assertTrue(SpeedBuildersRules.blockMatches(Material.END_PORTAL_FRAME,
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
    void emptyPlotsAreEliminatedAndAUniqueLowestBuildIsRemoved() {
        var scores = List.of(
                new SpeedBuildersRules.PlayerScore("empty", 0, 10, true),
                new SpeedBuildersRules.PlayerScore("low", 2, 10, false),
                new SpeedBuildersRules.PlayerScore("high", 8, 10, false));
        assertEquals(Set.of("empty"), SpeedBuildersRules.eliminationCandidates(scores));
    }

    @Test
    void uniqueLowestNonEmptyBuildIsEliminated() {
        var scores = List.of(
                new SpeedBuildersRules.PlayerScore("low", 2, 10, false),
                new SpeedBuildersRules.PlayerScore("middle", 5, 10, false),
                new SpeedBuildersRules.PlayerScore("high", 8, 10, false));
        assertEquals(Set.of("low"), SpeedBuildersRules.eliminationCandidates(scores));
    }

    @Test
    void fullTieAndAllPerfectRoundDoNotEliminatePlayers() {
        var tied = List.of(
                new SpeedBuildersRules.PlayerScore("one", 5, 10, false),
                new SpeedBuildersRules.PlayerScore("two", 5, 10, false),
                new SpeedBuildersRules.PlayerScore("three", 5, 10, false));
        var perfect = List.of(
                new SpeedBuildersRules.PlayerScore("one", 10, 10, false),
                new SpeedBuildersRules.PlayerScore("two", 10, 10, false));
        assertTrue(SpeedBuildersRules.eliminationCandidates(tied).isEmpty());
        assertTrue(SpeedBuildersRules.eliminationCandidates(perfect).isEmpty());
    }
}
