package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Pure selection rules for non-persistent Draw My Thing debug actions. */
final class DrawMyThingDebugRules {
    private DrawMyThingDebugRules() { }

    static int nextHiddenLetterIndex(String word, Set<Integer> revealed, RandomGenerator random) {
        if (word == null || word.isEmpty() || revealed == null || random == null) return -1;
        ArrayList<Integer> hidden = new ArrayList<>();
        for (int index = 0; index < word.length(); index++) {
            if (word.charAt(index) != ' ' && !revealed.contains(index)) hidden.add(index);
        }
        return hidden.isEmpty() ? -1 : hidden.get(random.nextInt(hidden.size()));
    }
}
