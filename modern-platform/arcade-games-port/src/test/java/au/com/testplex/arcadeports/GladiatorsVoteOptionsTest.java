package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GladiatorsVoteOptionsTest {
    @Test
    void centersThreeOptionsOnTheMiddleRow() {
        assertArrayEquals(new int[]{11, 13, 15}, GladiatorsVoteOptions.centeredVoteSlots(3));
    }

    @Test
    void centersSixOptionsAroundTheMiddleSlotWithoutSpillingRows() {
        assertArrayEquals(new int[]{10, 11, 12, 14, 15, 16}, GladiatorsVoteOptions.centeredVoteSlots(6));
    }

    @Test
    void eachTournamentGetsThreeUniqueOptionsFromOnlyTheDiscussedModePool() {
        List<GladiatorsMode> first = GladiatorsVoteOptions.randomOptions(new Random(4L));
        assertEquals(3, first.size());
        assertEquals(3L, first.stream().distinct().count());
        assertEquals(6, GladiatorsMode.values().length);
        assertTrue(first.stream().allMatch(mode -> List.of(GladiatorsMode.values()).contains(mode)));
        Set<List<GladiatorsMode>> samples = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            samples.add(GladiatorsVoteOptions.randomOptions(new Random(seed)));
        }
        assertTrue(samples.size() > 1);
    }

    @Test
    void voteWinnerIgnoresUnavailableModesAndBreaksTiesByDisplayedOrder() {
        List<GladiatorsMode> options = List.of(
                GladiatorsMode.NORMAL, GladiatorsMode.MODERN, GladiatorsMode.POT);

        assertEquals(GladiatorsMode.MODERN, GladiatorsVoteOptions.winner(options,
                List.of("modern", "modern", "rodless")));
        assertEquals(GladiatorsMode.NORMAL, GladiatorsVoteOptions.winner(options,
                List.of("modern", "normal")));
        assertEquals(GladiatorsMode.NORMAL, GladiatorsVoteOptions.winner(options, List.of()));
    }
}
