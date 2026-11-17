package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Source-shaped live scoreboard content shared by the Heroes runtime and its regression tests. */
final class HeroesOfGwenScoreboardRules {
    private HeroesOfGwenScoreboardRules() { }

    static List<String> lines(int playerTeam, String redBase, String blueBase,
                              List<String> beaconStatuses, int gold, long elapsedMillis) {
        List<String> lines = new ArrayList<>(14);
        lines.add(playerTeam == 0 ? ChatColor.GOLD + "Your Team" : playerTeam == 1
                ? ChatColor.RED + "Enemy Team" : ChatColor.RED + "Red Team");
        lines.add(ChatColor.GRAY + "Base: " + redBase);
        lines.add(" ");
        lines.add(playerTeam == 1 ? ChatColor.GOLD + "Your Team" : playerTeam == 0
                ? ChatColor.AQUA + "Enemy Team" : ChatColor.AQUA + "Blue Team");
        lines.add(ChatColor.GRAY + "Base: " + blueBase);
        lines.add("  ");
        lines.add(ChatColor.GREEN + "Beacons");
        lines.add(beaconStatuses.isEmpty() ? ChatColor.GRAY + "None"
                : String.join(ChatColor.DARK_GRAY + " | ", beaconStatuses));
        lines.add("   ");
        lines.add(ChatColor.GOLD + "Your Gold");
        lines.add(ChatColor.WHITE + Integer.toString(Math.max(0, gold)));
        lines.add("    ");
        lines.add(ChatColor.YELLOW + "Time");
        long seconds = Math.max(0L, elapsedMillis / 1_000L);
        lines.add(ChatColor.WHITE + String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L));
        return lines;
    }

    static String baseStatus(List<String> towerIcons, boolean towersRemain,
                             boolean witherAlive, int witherHealth) {
        if (towersRemain) return String.join("", towerIcons == null ? List.of() : towerIcons);
        return witherAlive
                ? ChatColor.DARK_PURPLE + "Wither " + ChatColor.WHITE + Math.max(0, witherHealth)
                : ChatColor.GRAY + "Wither down";
    }
}
