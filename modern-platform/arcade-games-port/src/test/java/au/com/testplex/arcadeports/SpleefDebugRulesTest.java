package au.com.testplex.arcadeports;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpleefDebugRulesTest {
    @AfterEach
    void clearOverrides() {
        GameDebugRuntimeOverrides.clear();
    }

    @Test
    void brawlerSmasherRadiusPreservesTheSixFaceDefaultAndChangesItsReach() {
        assertEquals(Set.of("1,0,0", "-1,0,0", "0,1,0", "0,-1,0", "0,0,1", "0,0,-1"),
                offsetSet());

        GameDebugRuntimeOverrides.activate(Map.of("SPLEEF_SMASHER_RADIUS_BLOCKS", 2));
        Set<String> expanded = offsetSet();
        assertEquals(12, expanded.size());
        assertTrue(expanded.contains("2,0,0"));
        assertTrue(expanded.contains("0,-2,0"));

        GameDebugRuntimeOverrides.resetActiveValues();
        assertEquals(6, offsetSet().size());
        assertEquals(Set.of("1,0,0", "-1,0,0", "0,1,0", "0,-1,0", "0,0,1", "0,0,-1"),
                offsetSet());
    }

    @Test
    void smasherReachIsAvailableAsABoundedSpleefProfileSetting() {
        GameDebugVariableCatalog.Variable radius = GameDebugVariableCatalog.variable(ArcadePortGame.SPLEEF,
                "SPLEEF_SMASHER_RADIUS_BLOCKS");
        assertNotNull(radius);
        assertEquals(0.0D, radius.minimum());
        assertEquals(8.0D, radius.maximum());
        assertEquals(1.0D, radius.defaultValue());
    }

    private static Set<String> offsetSet() {
        return ArcadePortRules.spleefSmasherOffsets().stream()
                .map(offset -> offset.x() + "," + offset.y() + "," + offset.z())
                .collect(Collectors.toSet());
    }
}
