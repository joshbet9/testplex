package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeStatsFilterTest {
    @Test void incompleteOnlyPreservesRarityOrderAndDynamicPageClamping() {
        var sorted = List.of("rarest", "earned", "second", "common");
        Set<String> unlocked = Set.of("earned");
        assertEquals(sorted, ArcadeStatsFilter.apply(sorted, unlocked::contains, false));
        assertEquals(List.of("rarest", "second", "common"),
                ArcadeStatsFilter.apply(sorted, unlocked::contains, true));
        assertEquals(0, ArcadeStatsLayout.clampPage(12, 3, 14));
        assertTrue(ArcadeStatsFilter.apply(sorted, value -> true, true).isEmpty());
    }
}
