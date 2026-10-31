package au.com.testplex.wizards;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;

/** Stores Wizards lifetime milestones in the shared Arcade local-stats configuration. */
public final class WizardsProgressStore {
    private static final Set<String> ALLOWED_STATS = Set.of("wins", "kills", "spells-bound");

    private WizardsProgressStore() { }

    public static void increment(FileConfiguration config, UUID playerId, String statKey, long amount) {
        if (config == null || playerId == null || !ALLOWED_STATS.contains(statKey) || amount <= 0L) return;
        String path = "local-stats." + playerId + ".wizards." + statKey;
        long current = Math.max(0L, config.getLong(path, 0L));
        long next = amount > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + amount;
        config.set(path, next);
    }
}
