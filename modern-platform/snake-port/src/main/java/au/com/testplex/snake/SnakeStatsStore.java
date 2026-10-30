package au.com.testplex.snake;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Local equivalents of the original Snake stat trackers for the isolated modern server. */
public final class SnakeStatsStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public SnakeStatsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create SnakeModern data folder.");
        }
        file = new File(plugin.getDataFolder(), "stats.yml");
        data = YamlConfiguration.loadConfiguration(file);
    }

    public void recordKill(Player player, int killsThisRound) {
        String path = base(player.getUniqueId());
        data.set(path + ".lifetime-kills", data.getInt(path + ".lifetime-kills") + 1);
        if (killsThisRound == SnakeRules.CANNIBAL_KILLS_REQUIRED
                && award(player.getUniqueId(), "Cannibal")) save();
    }

    public void recordSlime(Player player, int slimesThisRound) {
        String path = base(player.getUniqueId());
        data.set(path + ".lifetime-slimes", data.getInt(path + ".lifetime-slimes") + 1);
        if (slimesThisRound == SnakeRules.SLIMY_SHEEP_REQUIRED
                && award(player.getUniqueId(), "SlimySheep")) save();
    }

    public void recordTailLength(Player player, int tailLength) {
        String path = base(player.getUniqueId());
        if (tailLength > data.getInt(path + ".best-tail-length")) {
            data.set(path + ".best-tail-length", tailLength);
        }
        if (tailLength == SnakeRules.CHOO_CHOO_LENGTH_REQUIRED
                && award(player.getUniqueId(), "ChooChoo")) save();
    }

    private boolean award(UUID player, String stat) {
        String path = base(player) + ".achievements." + stat;
        if (data.getBoolean(path)) return false;
        data.set(path, true);
        plugin.getLogger().info("Snake achievement " + stat + " awarded to " + player + ".");
        return true;
    }

    public void recordGameEnd(UUID player, boolean won, int killsThisRound) {
        String path = base(player);
        data.set(path + ".games-played", data.getInt(path + ".games-played") + 1);
        if (won) data.set(path + ".wins", data.getInt(path + ".wins") + 1);
        data.set(path + ".score-kills", data.getInt(path + ".score-kills") + Math.max(0, killsThisRound));
        save();
    }

    public void publishToArcadeStats(UUID player, boolean won, int kills, boolean knockedOut) {
        org.bukkit.plugin.Plugin arcade = plugin.getServer().getPluginManager().getPlugin("ArcadeMechanicsModern");
        if (arcade == null || !arcade.isEnabled()) return;
        try {
            java.lang.reflect.Method method = arcade.getClass().getMethod("recordSnakeStats",
                    UUID.class, boolean.class, int.class, boolean.class, int.class, int.class, int.class, Set.class);
            method.invoke(arcade, player, won, kills, knockedOut,
                    lifetimeKills(player), lifetimeSlimes(player), bestTailLength(player), achievements(player));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not publish Snake stats to the shared Arcade stats page: "
                    + exception.getMessage());
        }
    }

    public int lifetimeKills(UUID player) { return data.getInt(base(player) + ".lifetime-kills"); }
    public int lifetimeSlimes(UUID player) { return data.getInt(base(player) + ".lifetime-slimes"); }
    public int bestTailLength(UUID player) { return data.getInt(base(player) + ".best-tail-length"); }
    public Set<String> achievements(UUID player) {
        Set<String> result = new LinkedHashSet<>();
        for (String stat : Set.of("Cannibal", "ChooChoo", "SlimySheep")) {
            if (data.getBoolean(base(player) + ".achievements." + stat)) result.add(stat);
        }
        return Set.copyOf(result);
    }

    public String summary(Player player) {
        String path = base(player.getUniqueId());
        Set<String> achievements = achievements(player.getUniqueId());
        return "Snake lifetime stats — Games: " + data.getInt(path + ".games-played")
                + ", Wins: " + data.getInt(path + ".wins")
                + ", Kills: " + data.getInt(path + ".lifetime-kills")
                + ", Slimes: " + data.getInt(path + ".lifetime-slimes")
                + ", Best tail: " + data.getInt(path + ".best-tail-length")
                + ", Achievements: " + (achievements.isEmpty() ? "none" : String.join(", ", achievements));
    }

    public void flush() { save(); }

    private String base(UUID player) { return "players." + player; }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save Snake stats: " + exception.getMessage());
        }
    }
}
