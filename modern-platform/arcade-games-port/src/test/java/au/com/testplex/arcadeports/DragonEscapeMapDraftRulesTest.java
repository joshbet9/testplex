package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class DragonEscapeMapDraftRulesTest {
    @Test
    void acceptsOnlySafeSingleSegmentDraftNames() {
        assertEquals("zodiac-repair", DragonEscapeMapDraftRules.draftSlug("Zodiac-Repair"));
        for (String unsafe : List.of("", "../world", "map/name", "draft name", "a".repeat(33), "..")) {
            assertNull(DragonEscapeMapDraftRules.draftSlug(unsafe), unsafe);
        }
    }

    @Test
    void mapIdsNormalizeApostrophesWithoutColliding() {
        assertEquals("a-builders-bedroom", DragonEscapeMapDraftRules.mapSlug(
                "source-maps/Dragon Escape/DragonEscape_A Builder's Bedroom.zip"));
        assertEquals("poseidons-realm", DragonEscapeMapDraftRules.mapSlug(
                "source-maps/Dragon Escape/DragonEscape_Poseidon's Realm.zip"));
    }

    @Test
    void derivesStableReservedWorldNamesForMapAndDraftPairs() {
        String resource = "source-maps/Dragon Escape/DragonEscape_Zodiac.zip";
        String first = DragonEscapeMapDraftRules.worldName(resource, "repair");
        assertEquals(first, DragonEscapeMapDraftRules.worldName(resource, "repair"));
        assertTrue(SourceMapCatalog.isReservedSourceWorldName(first));
        assertNotEquals(first, DragonEscapeMapDraftRules.worldName(resource, "review"));
        assertNotEquals(first, DragonEscapeMapDraftRules.worldName(
                "source-maps/Dragon Escape/DragonEscape_Through_Hell.zip", "repair"));
    }

    @Test
    void addsAndRemovesOnlyTheRequestedAuthoredMarker() {
        List<String> initial = List.of("MAP_NAME: Test Course", "DATA_NAME: BLACK", "DATA_LOCS: 1,2,3");
        List<String> added = DragonEscapeMapDraftRules.addPoint(initial, "DATA", "BLACK", 4.5, 70, -3);
        assertEquals(2, count(added, "DATA_NAME: BLACK"));
        assertTrue(added.contains("DATA_LOCS: 4.5,70,-3"));

        List<String> removed = DragonEscapeMapDraftRules.removePoint(added, "DATA", "BLACK", 2);
        assertEquals(1, count(removed, "DATA_LOCS: 1,2,3"));
        assertTrue(removed.stream().noneMatch(line -> line.equals("DATA_LOCS: 4.5,70,-3")));
        assertTrue(removed.contains("MAP_NAME: Test Course"));
    }

    @Test
    void rejectsUnsafeMarkerGroupsAndMissingIndexes() {
        assertThrows(IllegalArgumentException.class,
                () -> DragonEscapeMapDraftRules.addPoint(List.of(), "DATA", "RED\nDATA_NAME: BLACK", 1, 2, 3));
        assertThrows(IllegalArgumentException.class,
                () -> DragonEscapeMapDraftRules.removePoint(List.of("DATA_NAME: RED", "DATA_LOCS: 1,2,3"),
                        "DATA", "RED", 2));
    }

    @Test
    void replacesBoundsWithoutLeavingDuplicateConflictingValues() {
        List<String> edited = DragonEscapeMapDraftRules.setBounds(
                List.of("MAP_NAME: Test", "MIN_X: 0", "MAX_X: 10", "DATA_NAME: RED"),
                -5, 25, 0, 120, -8, 28);
        assertEquals(1, count(edited, "MIN_X: -5"));
        assertEquals(1, count(edited, "MAX_X: 25"));
        assertTrue(edited.contains("MIN_Y: 0"));
        assertTrue(edited.contains("MAX_Y: 120"));
        assertTrue(edited.contains("DATA_NAME: RED"));
        assertThrows(IllegalArgumentException.class,
                () -> DragonEscapeMapDraftRules.setBounds(List.of(), 1, 1, 0, 10, 0, 10));
    }

    private static long count(List<String> lines, String expected) {
        return lines.stream().filter(expected::equals).count();
    }
}
