package au.com.testplex.arcadeports;

import java.util.List;
import java.util.function.Predicate;

/** Post-rarity filter: hiding earned achievements must not re-order the rest. */
final class ArcadeStatsFilter {
    static <T> List<T> apply(List<T> rarestFirst, Predicate<T> earned, boolean incompleteOnly) {
        return incompleteOnly ? rarestFirst.stream().filter(value -> !earned.test(value)).toList() : rarestFirst;
    }

    private ArcadeStatsFilter() { }
}
