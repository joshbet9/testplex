package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;

/** Wool-only wall templates available to the Turf Wars Fortifier kit. */
enum TurfFortifierTemplate {
    PANEL_2X2("2x2", new int[] {2, 2}),
    PANEL_3X3("3x3", new int[] {3, 3, 3}),
    FORT("Fort", new int[] {4, 3, 4, 3, 4});

    private final String displayName;
    private final int[] columnHeights;

    TurfFortifierTemplate(String displayName, int[] columnHeights) {
        this.displayName = displayName;
        this.columnHeights = columnHeights;
    }

    String displayName() {
        return displayName;
    }

    int blockCount() {
        int count = 0;
        for (int height : columnHeights) count += height;
        return count;
    }

    TurfFortifierTemplate next() {
        TurfFortifierTemplate[] templates = values();
        return templates[(ordinal() + 1) % templates.length];
    }

    List<Offset> offsets(int availableBlocks) {
        int limit = Math.max(0, Math.min(availableBlocks, blockCount()));
        List<Offset> result = new ArrayList<>(limit);
        int center = columnHeights.length / 2;
        for (int column = 0; column < columnHeights.length && result.size() < limit; column++) {
            for (int y = 0; y < columnHeights[column] && result.size() < limit; y++) {
                result.add(new Offset(column - center, y));
            }
        }
        return List.copyOf(result);
    }

    record Offset(int lateral, int up) { }
}
