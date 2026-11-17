package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/** Solo SkyWars player section as written by the Mineplex Reborn SoloSkywars scoreboard. */
final class SkyWarsSoloScoreboardLayout {
    private SkyWarsSoloScoreboardLayout() {
    }

    static List<String> playersSection(List<String> aliveNames) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Players");
        if (aliveNames.size() > 10) {
            lines.add(ChatColor.WHITE + Integer.toString(aliveNames.size()));
        } else {
            for (String name : aliveNames) lines.add(ChatColor.WHITE + name);
        }
        return lines;
    }
}
