package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

final class SheepQuestScoreboardRules {
    private static final String[] TEAM_NAMES = {"Red", "Aqua", "Green", "Yellow"};
    private static final ChatColor[] TEAM_COLORS = {
            ChatColor.RED, ChatColor.AQUA, ChatColor.GREEN, ChatColor.YELLOW
    };

    private SheepQuestScoreboardRules() { }

    static List<String> teamCountLines(int[] sheepCounts) {
        if (sheepCounts == null || sheepCounts.length != TEAM_NAMES.length) {
            throw new IllegalArgumentException("Sheep Quest requires one count for each of its four teams");
        }
        List<String> lines = new ArrayList<>(TEAM_NAMES.length);
        for (int team = 0; team < TEAM_NAMES.length; team++) {
            lines.add(TEAM_COLORS[team] + TEAM_NAMES[team] + ChatColor.WHITE + ": "
                    + Math.max(0, sheepCounts[team]));
        }
        return List.copyOf(lines);
    }
}
