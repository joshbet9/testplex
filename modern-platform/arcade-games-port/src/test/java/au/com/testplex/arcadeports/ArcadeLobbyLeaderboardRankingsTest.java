package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeLobbyLeaderboardRankingsTest {
    @Test void respectsTiesAndDirection() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        Map<UUID, Long> values = new LinkedHashMap<>();
        values.put(a, 10L);
        values.put(b, 50L);
        values.put(c, 50L);
        List<ArcadeLobbyLeaderboardRankings.Entry> high = ArcadeLobbyLeaderboardRankings.rank(values, false);
        assertEquals(List.of(1, 1, 3), high.stream().map(ArcadeLobbyLeaderboardRankings.Entry::rank).toList());
        assertEquals(a, ArcadeLobbyLeaderboardRankings.rank(values, true).getFirst().player());
    }

    @Test void aroundYouCentersAndClampsToListBounds() {
        Map<UUID, Long> values = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) values.put(new UUID(0L, i + 1L), 100L - i);
        var ranks = ArcadeLobbyLeaderboardRankings.rank(values, false);
        var around = ArcadeLobbyLeaderboardRankings.visible(ranks, new UUID(0L, 13L), true, 10);
        assertEquals(10, around.size());
        assertTrue(around.stream().anyMatch(e -> e.player().equals(new UUID(0L, 13L))));
        assertEquals(8, around.getFirst().rank());
        assertEquals(1, ArcadeLobbyLeaderboardRankings.visible(ranks, ranks.getFirst().player(), true, 10)
                .getFirst().rank());
    }

    @Test void emptyAndUnrankedViewersNeverThrow() {
        assertTrue(ArcadeLobbyLeaderboardRankings.visible(List.of(), UUID.randomUUID(), true, 10).isEmpty());
        var entries = ArcadeLobbyLeaderboardRankings.rank(Map.of(new UUID(0, 1), 1L), false);
        assertEquals(entries, ArcadeLobbyLeaderboardRankings.visible(entries, UUID.randomUUID(), true, 10));
    }
}
