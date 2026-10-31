package au.com.testplex.wizards;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;

/** Writes the three lifetime Wizards milestones into the shared Testplex Arcade config. */
public final class WizardsProgressStore {
    private static final Set<String> ALLOWED_STATS = Set.of("wins", "kills", "spells-bound");

    private WizardsProgressStore() { }

    public static void increment(FileConfiguration config, UUID playerId, String statKey, long amount) {
        if (config == null || playerId == null || !ALLOWED_STATS.contains(statKey) || amount <= 0L) return;
        String path = "local-stats." + playerId + ".wizards." + statKey;
        long current = Math.max(0L, config.getLong(path, 0L));
        config.set(path, current + amount);
    }
}
