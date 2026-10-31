package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.List;
import java.util.Locale;

/**
 * Paper 26.3 port of the actual 1.8 Nautilus Arcade lobby scoreboard.
 *
 * The source fields are Players, Kit, Gems (or Crowns on a crowns-enabled
 * game), and Server. Division was only added for Minecraft League. This
 * modern acceptance server has no Minecraft League/Crowns integration, so
 * it deliberately renders the four standard fields only.
 */
public final class ModernLobbyScoreboard {
    private final JavaPlugin plugin;

    public ModernLobbyScoreboard(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(Player player) {
        if (player == null || !player.isOnline()) return;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        String status = plugin.getConfig().getString(
                "lobby-countdown-status", "Waiting for players");
        Objective sidebar = scoreboard.registerNewObjective(
                "testplex_lobby", Criteria.DUMMY, scoreboardTitleForStatus(status));
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        long currentPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(online -> ModernArcadeLobby.WORLD_NAME.equals(online.getWorld().getName()))
                .count();
        int playerCapacity = plugin.getConfig().getInt(
                "lobby-scoreboard.player-capacity", Bukkit.getMaxPlayers());
        if (playerCapacity < 1) playerCapacity = Math.max(1, Bukkit.getMaxPlayers());

        String selectedGameId = plugin.getConfig().getString("lobby-selected-game", "death-tag");
        if (selectedGameId == null || selectedGameId.isBlank()) selectedGameId = "death-tag";
        String selectedKitId = plugin.getConfig().getString(
                "lobby-kit-preferences." + player.getUniqueId() + "." + selectedGameId, "None");
        String kitName = formatKitName(selectedKitId);
        String serverName = plugin.getConfig().getString(
                "lobby-scoreboard.server-name", "Testplex Modern");
        String gems = String.valueOf(plugin.getConfig().getInt(
                "lobby-scoreboard.gems." + player.getUniqueId(), 0));

        int score = sidebarLines(
                formatPlayerCount(currentPlayers, playerCapacity),
                kitName,
                gems,
                serverName
        ).size();
        for (String line : sidebarLines(
                formatPlayerCount(currentPlayers, playerCapacity),
                kitName,
                gems,
                serverName
        )) {
            sidebar.getScore(line).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }

    /**
     * Matches ArcadeManager's original draw() status title and colour changes.
     */
    static String scoreboardTitleForStatus(String status) {
        String value = status == null || status.isBlank() ? "Waiting for players" : status.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if (lower.startsWith("vote ends in ")) {
            String timer = value.substring("Vote ends in ".length());
            return ChatColor.BOLD + "Vote ends in "
                    + ChatColor.GREEN + ChatColor.BOLD + timer;
        }
        if (lower.startsWith("starting in ")) {
            String countdown = value.substring("Starting in ".length());
            return ChatColor.BOLD + "Starting in "
                    + ChatColor.GREEN + ChatColor.BOLD + countdown;
        }
        return ChatColor.GREEN.toString() + ChatColor.BOLD + value;
    }

    /**
     * Mirrors ArcadeScoreboardLine registration order:
     * Players, Kit, Gems, Server, each with its original blank spacer.
     */
    static List<String> sidebarLines(
            String players,
            String kit,
            String gems,
            String server) {
        return List.of(
                spacer(0),
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Players",
                ChatColor.WHITE + players,
                spacer(1),
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit",
                ChatColor.WHITE + kit,
                spacer(2),
                ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems",
                ChatColor.WHITE + gems,
                spacer(3),
                ChatColor.AQUA.toString() + ChatColor.BOLD + "Server",
                ChatColor.WHITE + server
        );
    }

    static String formatPlayerCount(long current, int capacity) {
        int safeCapacity = Math.max(1, capacity);
        long safeCurrent = Math.max(0L, Math.min(current, safeCapacity));
        return safeCurrent + "/" + safeCapacity;
    }

    static String formatKitName(String kitId) {
        if (kitId == null || kitId.isBlank()) return "None";
        return titleCase(kitId);
    }

    private static String spacer(int index) {
        return switch (index) {
            case 0 -> ChatColor.BLACK + " ";
            case 1 -> ChatColor.DARK_GRAY + " ";
            case 2 -> ChatColor.GRAY + " ";
            default -> ChatColor.WHITE + " ";
        };
    }

    private static String titleCase(String value) {
        String normalized = value.trim().replace('_', '-');
        if (normalized.isEmpty()) return "None";
        StringBuilder result = new StringBuilder();
        for (String word : normalized.split("-")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.isEmpty() ? "None" : result.toString();
    }
}
