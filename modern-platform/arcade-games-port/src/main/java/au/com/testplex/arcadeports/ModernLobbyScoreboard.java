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
import java.util.Locale;

/**
 * Consistent Testplex lobby sidebar for every Arcade game.
 *
 * Keeps a compact, Mineplex-style hierarchy while showing the modern lobby's
 * live game and kit selections. The layout always uses the same 15-line order;
 * no individual game gets a different colour scheme or a different sidebar.
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
        String selectedGameId = plugin.getConfig().getString("lobby-selected-game", "death-tag");
        if (selectedGameId == null || selectedGameId.isBlank()) selectedGameId = "death-tag";

        String serverName = plugin.getConfig().getString("lobby-scoreboard.server-name", "Testplex Modern");
        String website = plugin.getConfig().getString("lobby-scoreboard.website", "www.mineplex.com");

        List<String> owners = new ArrayList<>(plugin.getConfig().getStringList("owner-usernames"));
        if (owners.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) owners.add("jn_playzmc");
        boolean owner = owners.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));

        List<String> configuredAdmins = new ArrayList<>(plugin.getConfig().getStringList("admin-usernames"));
        if (configuredAdmins.stream().noneMatch(name -> "jn_playzmc".equalsIgnoreCase(name))) configuredAdmins.add("jn_playzmc");
        boolean configuredAdmin = configuredAdmins.stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        boolean admin = player.isOp() || configuredAdmin
                || player.hasPermission("testplex.arcadeports.admin")
                || player.hasPermission("testplex.modern.admin");
        String rank = rankName(owner, admin);

        String selectedKitId = plugin.getConfig().getString(
                "lobby-kit-preferences." + player.getUniqueId() + "." + selectedGameId, "Default");
        String gameName = formatGameName(selectedGameId);
        String kitName = formatKitName(selectedKitId);
        String gems = String.valueOf(plugin.getConfig().getInt(
                "lobby-scoreboard.gems." + player.getUniqueId(), 0));
        String shards = String.valueOf(plugin.getConfig().getInt(
                "lobby-scoreboard.shards." + player.getUniqueId(), 0));

        String title = scoreboardTitleForGame(
                selectedGameId, plugin.getConfig().getString("lobby-scoreboard.title", "Testplex"));
        Objective sidebar = scoreboard.registerNewObjective("testplex_lobby", Criteria.DUMMY, title);
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 15;
        for (String line : sidebarLines(serverName, gameName, kitName, gems, shards, rank, website)) {
            sidebar.getScore(line).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }

    /**
     * Exactly 15 unique entries, with consistent spacing, labels and colours
     * regardless of the currently selected game.
     */
    static List<String> sidebarLines(
            String serverName, String gameName, String kitName,
            String gems, String shards, String rank, String website) {
        return List.of(
                ChatColor.AQUA + "Server: " + ChatColor.WHITE + serverName,
                spacer(0),
                ChatColor.GREEN.toString() + ChatColor.BOLD + "Game",
                ChatColor.WHITE + gameName,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit",
                ChatColor.LIGHT_PURPLE + kitName,
                spacer(1),
                ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems",
                ChatColor.WHITE + gems,
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Shards",
                ChatColor.GRAY + shards,
                spacer(2),
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Rank",
                ChatColor.DARK_GREEN + rank,
                ChatColor.RED + "Website: " + ChatColor.DARK_AQUA + website
        );
    }

    private static String spacer(int index) {
        return switch (index) {
            case 0 -> ChatColor.BLACK + " ";
            case 1 -> ChatColor.DARK_GRAY + " ";
            default -> ChatColor.GRAY + " ";
        };
    }

    static boolean usesBaconBrawlLobbySidebar(String gameId) {
        return gameId != null && "bacon-brawl".equalsIgnoreCase(gameId.trim().replace('_', '-'));
    }

    /** The sidebar header is intentionally stable across games. */
    static String scoreboardTitleForGame(String gameId, String defaultTitle) {
        String title = defaultTitle == null || defaultTitle.isBlank() ? "Testplex" : defaultTitle.trim();
        return ChatColor.WHITE.toString() + ChatColor.BOLD + title.toUpperCase(Locale.ROOT);
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
            if (word.length() > 1) result.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.isEmpty() ? "Default" : result.toString();
    }
}
