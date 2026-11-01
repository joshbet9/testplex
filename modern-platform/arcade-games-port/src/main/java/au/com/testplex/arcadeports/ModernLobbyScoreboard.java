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
import org.bukkit.scoreboard.Team;

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
        String gameName = formatGameName(selectedGameId);
        String selectedKitId = plugin.getConfig().getString(
                "lobby-kit-preferences." + player.getUniqueId() + "." + selectedGameId, "None");
        String kitName = formatKitName(selectedKitId);
        String serverName = plugin.getConfig().getString(
                "lobby-scoreboard.server-name", "Testplex Modern");
        String gems = String.valueOf(plugin.getConfig().getInt(
                "lobby-scoreboard.gems." + player.getUniqueId(), 0));

        List<String> lines = sidebarLines(
                formatPlayerCount(currentPlayers, playerCapacity),
                gameName,
                kitName,
                gems,
                serverName
        );
        int score = lines.size();
        for (String line : lines) sidebar.getScore(line).setScore(score--);
        applyLobbyTabTeams(scoreboard);
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
            String game,
            String kit,
            String gems,
            String server) {
        return List.of(
                spacer(0),
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Players",
                ChatColor.WHITE + players,
                spacer(1),
                ChatColor.AQUA.toString() + ChatColor.BOLD + "Game",
                ChatColor.WHITE + game,
                spacer(2),
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Kit",
                ChatColor.WHITE + kit,
                spacer(3),
                ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems",
                ChatColor.WHITE + gems,
                spacer(4),
                ChatColor.AQUA.toString() + ChatColor.BOLD + "Server",
                ChatColor.WHITE + server
        );
    }

    static String formatPlayerCount(long current, int capacity) {
        int safeCapacity = Math.max(1, capacity);
        long safeCurrent = Math.max(0L, current);
        return safeCurrent + "/" + safeCapacity;
    }

    static String formatGameName(String gameId) {
        if (gameId == null || gameId.isBlank()) return "None";
        if (gameId.startsWith("monster-maze")) return "Monster Maze";
        ArcadePortGame arcadeGame = ArcadePortGame.fromId(gameId);
        if (arcadeGame != null) return arcadeGame.isMonsterMaze()
                ? "Monster Maze" : arcadeGame.displayName();
        ArcadeGameDirectory.Game directoryGame = ArcadeGameDirectory.find(gameId);
        return directoryGame == null ? titleCase(gameId) : directoryGame.name();
    }

    static String formatKitName(String kitId) {
        if (kitId == null || kitId.isBlank()) return "None";
        return titleCase(kitId);
    }

    /**
     * A player belongs to one scoreboard team only. Team games therefore combine
     * the rank sort group and the game-team colour into a single team identifier.
     */
    static String tabTeamSortKey(String rank, int teamId) {
        String rankKey = tabSortKey(rank);
        return teamId >= 0 && teamId < 4 ? rankKey + "_t" + teamId : rankKey;
    }

    /**
     * Keeps the legacy rank priority first while retaining Mineplex's exact
     * primary PermissionGroup name in the team key. Raw enum names alone sort
     * alphabetically (for example ADMIN before OWNER), which is not the Arcade
     * rank order. The suffix also keeps distinct staff/content primary groups
     * separate without collapsing them into their broad display category.
     */
    static String tabTeamSortKeyForGroup(String rank, String permissionGroupName, int teamId) {
        String rankKey = tabSortKey(rank);
        String groupSuffix = permissionGroupName == null || permissionGroupName.isBlank()
                ? "" : "_" + permissionGroupName.trim();
        String teamSuffix = teamId >= 0 && teamId < 4 ? "_t" + teamId : "";
        return rankKey + groupSuffix + teamSuffix;
    }

    /** Uses the real CoreClient PermissionGroup and its legacy rank order when Hub is available. */
    static String tabTeamSortKey(JavaPlugin plugin, Player player, int teamId) {
        if (plugin instanceof ArcadeMechanicsModernPlugin arcade) {
            MineplexCoreRankBridge.RankPresentation presentation = arcade.mineplexRankPresentation(player);
            if (presentation != null) {
                return tabTeamSortKeyForGroup(
                        presentation.rankId(), presentation.groupName(), teamId);
            }
        }
        return tabTeamSortKey(rankId(plugin, player), teamId);
    }

    static String tabSortKey(String rank) {
        return switch (rank == null ? "player" : rank.toLowerCase(Locale.ROOT)) {
            case "owner" -> "r0_owner";
            case "admin" -> "r1_admin";
            case "mod" -> "r2_mod";
            case "helper" -> "r3_helper";
            case "ultra" -> "r4_ultra";
            default -> "r5_player";
        };
    }

    static String rankPrefix(JavaPlugin plugin, Player player) {
        if (plugin instanceof ArcadeMechanicsModernPlugin arcade) {
            MineplexCoreRankBridge.RankPresentation presentation = arcade.mineplexRankPresentation(player);
            if (presentation != null) return presentation.prefix();
        }
        String rank = rankId(plugin, player);
        ChatColor colour = switch (rank) {
            case "owner" -> ChatColor.DARK_RED;
            case "admin" -> ChatColor.RED;
            case "mod" -> ChatColor.GOLD;
            case "helper" -> ChatColor.GREEN;
            case "ultra" -> ChatColor.AQUA;
            default -> ChatColor.GRAY;
        };
        String label = switch (rank) {
            case "owner" -> "OWNER";
            case "admin" -> "ADMIN";
            case "mod" -> "MOD";
            case "helper" -> "HELPER";
            case "ultra" -> "ULTRA";
            default -> "";
        };
        return label.isEmpty() ? "" : colour + "[" + label + "] " + ChatColor.RESET;
    }

    static String rankId(JavaPlugin plugin, Player player) {
        if (plugin instanceof ArcadeMechanicsModernPlugin arcade) {
            String authoritative = arcade.mineplexRankId(player);
            if (authoritative != null) return authoritative;
        }
        String configured = plugin.getConfig().getString("lobby-scoreboard.ranks." + player.getUniqueId(), "");
        if (configured != null && !configured.isBlank()) {
            String value = configured.trim().toLowerCase(Locale.ROOT);
            if (List.of("owner", "admin", "mod", "helper", "ultra", "player").contains(value)) return value;
        }
        if (plugin.getConfig().getStringList("owner-usernames").stream()
                .anyMatch(name -> name.equalsIgnoreCase(player.getName()))) return "owner";
        if (plugin.getConfig().getStringList("admin-usernames").stream()
                .anyMatch(name -> name.equalsIgnoreCase(player.getName()))) return "admin";
        if (player.hasPermission("testplex.rank.owner")) return "owner";
        if (player.hasPermission("testplex.rank.admin") || player.isOp()) return "admin";
        if (player.hasPermission("testplex.rank.mod")) return "mod";
        if (player.hasPermission("testplex.rank.helper")) return "helper";
        if (player.hasPermission("testplex.rank.ultra")) return "ultra";
        return "player";
    }

    private void applyLobbyTabTeams(Scoreboard scoreboard) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!ModernArcadeLobby.WORLD_NAME.equals(online.getWorld().getName())) continue;
            String teamName = tabTeamSortKey(plugin, online, -1);
            Team team = scoreboard.getTeam(teamName);
            if (team == null) team = scoreboard.registerNewTeam(teamName);
            team.setPrefix(rankPrefix(plugin, online));
            team.addEntry(online.getName());
        }
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
