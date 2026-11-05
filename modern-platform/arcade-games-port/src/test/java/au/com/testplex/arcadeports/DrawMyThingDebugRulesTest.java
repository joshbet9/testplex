package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.*;

class DrawMyThingDebugRulesTest {
    @Test
    void revealsOnlyAnUnshownNonSpaceLetterAndStopsWhenComplete() {
        RandomGenerator random = RandomGeneratorFactory.of("L64X128MixRandom").create(23L);
        String word = "cat dog";
        int next = DrawMyThingDebugRules.nextHiddenLetterIndex(word, Set.of(0, 1, 2, 4, 5, 6), random);
        assertEquals(-1, next);
        next = DrawMyThingDebugRules.nextHiddenLetterIndex(word, Set.of(0, 1, 4, 5, 6), random);
        assertEquals(2, next);
        assertEquals(-1, DrawMyThingDebugRules.nextHiddenLetterIndex(null, Set.of(), random));
    }
}
