package au.com.testplex.arcadeports;

import net.kyori.adventure.text.Component;
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
import java.util.UUID;

/**
 * Paper 26.3 rendition of the 1.8 Mineplex HubScoreboard.
 *
 * The sidebar preserves the original five sections, ordering, spacer lines,
 * website footer and white bold server title. Core's coin/account database is
 * not present in this isolated server, so Gems/Shards use configured sandbox
 * values and default to zero rather than inventing production balances.
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
                "testplex_lobby", Criteria.DUMMY, Component.text(ChatColor.WHITE.toString() + ChatColor.BOLD + title));
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        String serverName = plugin.getConfig().getString("lobby-scoreboard.server-name", "Testplex Modern");
        String website = plugin.getConfig().getString("lobby-scoreboard.website", "www.mineplex.com");
        List<String> configuredAdmins = plugin.getConfig().getStringList("admin-usernames");
        boolean configuredAdmin = configuredAdmins.stream()
                .anyMatch(name -> name != null && name.equalsIgnoreCase(player.getName()));
        // The lobby's join listener may run before the OP grant listener in the
        // same tick, so resolve the configured sandbox owner here as well.
        if (configuredAdmins.stream().noneMatch(name -> "joshbet9".equalsIgnoreCase(name))
                && "joshbet9".equalsIgnoreCase(player.getName())) configuredAdmin = true;
        String rank = player.isOp() || configuredAdmin
                || player.hasPermission("testplex.arcadeports.admin")
                || player.hasPermission("testplex.modern.admin") ? "Admin" : "No Rank";
        String gems = String.valueOf(plugin.getConfig().getInt("lobby-scoreboard.gems." + player.getUniqueId(), 0));
        String shards = String.valueOf(plugin.getConfig().getInt("lobby-scoreboard.shards." + player.getUniqueId(), 0));

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.AQUA.toString() + ChatColor.BOLD + "Server");
        lines.add(ChatColor.WHITE + serverName);
        lines.add(ChatColor.BLACK.toString());
        lines.add(ChatColor.GREEN.toString() + ChatColor.BOLD + "Gems");
        lines.add(ChatColor.WHITE + gems);
        lines.add(ChatColor.DARK_GRAY.toString());
        lines.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Shards");
        lines.add(ChatColor.WHITE + shards);
        lines.add(ChatColor.GRAY.toString());
        lines.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "Rank");
        lines.add(ChatColor.WHITE + rank);
        lines.add(ChatColor.BLUE.toString());
        lines.add(ChatColor.RED.toString() + ChatColor.BOLD + "Website");
        lines.add(ChatColor.WHITE + website);
        lines.add("----------------");

        int score = lines.size();
        for (String line : lines) {
            sidebar.getScore(line).setScore(score--);
        }
        player.setScoreboard(scoreboard);
    }
}
