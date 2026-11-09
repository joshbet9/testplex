package au.com.testplex.arcadeports;

import org.bukkit.configuration.Configuration;

/** Per-map, per-kit timing alongside the pre-existing overall PB and ghost records. */
final class DragonEscapePersonalBestRecords {
    private DragonEscapePersonalBestRecords() { }

    static String timePath(String basePath, String kitId) {
        return basePath == null || basePath.isEmpty()
                || DragonEscapePersonalBestViews.BEST_TIME.equals(kitId)
                || !DragonEscapePersonalBestViews.all().contains(kitId)
                ? "" : basePath + "kits." + kitId + ".time-ms";
    }

    static long get(Configuration config, String basePath, String kitId) {
        String path = timePath(basePath, kitId);
        return path.isEmpty() ? -1L : config.getLong(path, -1L);
    }

    /** True only when a genuine faster kit record was stored; caller controls disk flushing. */
    static boolean update(Configuration config, String basePath, String kitId, long elapsedMillis) {
        String path = timePath(basePath, kitId);
        if (path.isEmpty() || !DragonEscapePersonalBestViews.improves(
                config.getLong(path, -1L), elapsedMillis)) return false;
        config.set(path, elapsedMillis);
        return true;
    }
}
