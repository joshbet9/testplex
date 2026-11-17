package au.com.testplex.arcadeports;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** Small ordering rule shared by Paintball's live scoreboard and its regression tests. */
final class PaintballScoreboardLayout {
    private PaintballScoreboardLayout() {
    }

    static <T> void forEachInRosterOrder(Iterable<T> roster, ToIntFunction<T> teamIndex,
                                        int requestedTeam, Consumer<T> consumer) {
        if (roster == null || teamIndex == null || consumer == null) return;
        for (T player : roster) {
            if (player != null && teamIndex.applyAsInt(player) == requestedTeam) consumer.accept(player);
        }
    }
}
