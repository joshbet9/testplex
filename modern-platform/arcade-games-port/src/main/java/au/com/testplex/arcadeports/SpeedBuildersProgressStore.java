package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;

/** Writes Speed Builders lifetime progress to the shared Arcade stats config. */
public final class SpeedBuildersProgressStore {
    private static final Set<String> ALLOWED_STATS = Set.of("wins", "perfect-builds", "blocks-placed", "perfect-first", "speediest-builderizer", "perfectionist-wins", "blocks-broken", "mobs-built");

    private SpeedBuildersProgressStore() { }

    public static void increment(FileConfiguration config, UUID playerId, String statKey, long amount) {
        if (config == null || playerId == null || !ALLOWED_STATS.contains(statKey) || amount <= 0L) return;
        String path = "local-stats." + playerId + ".speed-builders-arcade." + statKey;
        long current = Math.max(0L, config.getLong(path, 0L));
        config.set(path, current + amount);
    }
}
