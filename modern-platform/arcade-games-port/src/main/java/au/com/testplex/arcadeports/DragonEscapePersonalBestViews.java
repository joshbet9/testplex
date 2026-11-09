package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;

/** Stable PB view order: overall record, followed by every selectable Dragon Escape kit. */
final class DragonEscapePersonalBestViews {
    static final String BEST_TIME = "best-time";
    private static final List<String> VIEWS = createViews();

    private DragonEscapePersonalBestViews() { }

    private static List<String> createViews() {
        List<String> views = new ArrayList<>();
        views.add(BEST_TIME);
        for (ArcadeLobbyKitCatalogue.KitChoice kit :
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.DRAGON_ESCAPE, List.of())) {
            views.add(kit.id);
        }
        return List.copyOf(views);
    }

    static List<String> all() { return VIEWS; }

    static String selected(int index) { return VIEWS.get(Math.floorMod(index, VIEWS.size())); }

    static int next(int index) { return Math.floorMod(index + 1, VIEWS.size()); }

    static String name(String kitId) {
        if (BEST_TIME.equals(kitId)) return "Best Time";
        for (ArcadeLobbyKitCatalogue.KitChoice kit :
                ArcadeLobbyKitCatalogue.forGame(ArcadePortGame.DRAGON_ESCAPE, List.of())) {
            if (kit.id.equals(kitId)) return kit.name;
        }
        return "Unknown (legacy record)";
    }

    static boolean improves(long existingMillis, long elapsedMillis) {
        return elapsedMillis > 0L && (existingMillis <= 0L || elapsedMillis < existingMillis);
    }
}
