package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/** Persists each player's Like, Dislike or Neutral rating for every Arcade game. */
public final class GamePreferenceStore {
    public enum Preference {
        LIKE,
        DISLIKE,
        NEUTRAL
    }

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public GamePreferenceStore(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create " + plugin.getDataFolder());
        }
        this.file = new File(plugin.getDataFolder(), "game-preferences.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Preference getPreference(UUID playerId, String gameId) {
        String stored = data.getString(path(playerId, gameId), Preference.NEUTRAL.name());
        if (stored == null) return Preference.NEUTRAL;
        try {
            return Preference.valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Preference.NEUTRAL;
        }
    }

    public void setPreference(UUID playerId, String gameId, Preference preference) {
        if (playerId == null || gameId == null || gameId.isBlank() || preference == null) {
            throw new IllegalArgumentException("Player, game and preference are required.");
        }
        String path = path(playerId, gameId);
        if (preference == Preference.NEUTRAL) {
            data.set(path, null);
        } else {
            data.set(path, preference.name());
        }
        save();
    }

    private String path(UUID playerId, String gameId) {
        return "players." + playerId + "." + gameId;
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save game preferences: " + exception.getMessage());
        }
    }
}
