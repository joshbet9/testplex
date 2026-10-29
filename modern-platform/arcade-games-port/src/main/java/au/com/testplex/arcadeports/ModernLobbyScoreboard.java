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

import java.util.ArrayList;
import java.util.List;

/**
 * Paper 26.3 rendition of the 1.8 Testplex lobby sidebar.
 *
 * Game and kit are live lobby selections. Gems/Shards are isolated sandbox
 * values because the production Core account database is not connected.
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
        String title = plugin.getConfig().getString("lobby-scoreboard.title", "Testplex");
        Objective sidebar = scoreboard.registerNewObjective(
                "testplex_lobby", Criteria.DUMMY,
                org.bukkit.ChatColor.WHITE.toString() + ChatColor.BOLD + title);
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        String serverName = plugin.getConfig().getString("lobby-scoreboard.server-name", "Testplex Modern");
        String website = plugin.getConfig().getString("lobby-scoreboard.website", "www.mineplex.com");

        List<String> owners = new ArrayList<>(plugin.getConfig().getStringList("owner-usernames"));
        if (owners.stream().noneMatch(name -> "joshbet9".equalsIgnoreCase(name))) owners.add("joshbet9");
        boolean owner = owners.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));

        List<String> configuredAdmins = new ArrayList<>(plugin.getConfig().getStringList("admin-usernames"));
        if (configuredAdmins.stream().noneMatch(name -> "joshbet9".equalsIgnoreCase(name))) configuredAdmins.add("joshbet9");
        boolean configuredAdmin = configuredAdmins.stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        boolean admin = player.isOp() || configuredAdmin
                || player.hasPermission("testplex.arcadeports.admin")
                || player.hasPermission("testplex.modern.admin");
        String rank = rankName(owner, admin);

        String selectedGameId = plugin.getConfig().getString("lobby-selected-game", "bacon-brawl");
        if (selectedGameId == null || selectedGameId.isBlank()) selectedGameId = "bacon-brawl";
        String selectedKitId = plugin.getConfig().getString(
                "lobby-kit-preferences." + player.getUniqueId() + "." + selectedGameId, "Default");
        String gameName = formatGameName(selectedGameId);
        String kitName = formatKitName(selectedKitId);

        String gems = String.valueOf(plugin.getConfig().getInt("lobby-scoreboard.gems." + player.getUniqueId(), 0));
        String shards = String.valueOf(plugin.getConfig().getInt("lobby-scoreboard.shards." + player.getUniqueId(), 0));

        // Sidebar objectives are limited to 15 visible score entries. Keep the
        // familiar Testplex sections while reserving two live fields for Game/Kit.
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.AQUA.toString() + ChatColor.BOLD + "Server");
        lines.add(ChatColor.WHITE + serverName);
        lines.add(ChatColor.GREEN.toString() + ChatColor.BOLD + "Game");
        lines.add(ChatColor.AQUA + gameName);
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit");
        lines.add(ChatColor.LIGHT_PURPLE + kitName);
        lines.add(ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems");
        lines.add(ChatColor.WHITE + gems);
        lines.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Shards");
        lines.add(ChatColor.GRAY + shards);
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "Rank");
        lines.add(ChatColor.DARK_GREEN + rank);
        lines.add(ChatColor.RED.toString() + ChatColor.BOLD + "Website");
        lines.add(ChatColor.DARK_AQUA + website);
        lines.add("----------------");

        int score = lines.size();
        for (String line : lines) {
            sidebar.getScore(line).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }

    static String formatGameName(String gameId) {
        if (gameId == null || gameId.isBlank()) return "Not selected";
        ArcadePortGame game = ArcadePortGame.fromId(gameId);
        return game != null ? game.displayName() : titleCase(gameId);
    }

    static String formatKitName(String kitId) {
        if (kitId == null || kitId.isBlank()) return "Default";
        return titleCase(kitId);
    }

    static String rankName(boolean owner, boolean admin) {
        if (owner) return "Owner";
        return admin ? "Admin" : "No Rank";
    }

    private static String titleCase(String value) {
        String normalized = value.trim().replace('_', '-');
        if (normalized.isEmpty()) return "Default";
        StringBuilder result = new StringBuilder();
        for (String word : normalized.split("-")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) result.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return result.isEmpty() ? "Default" : result.toString();
    }
}
