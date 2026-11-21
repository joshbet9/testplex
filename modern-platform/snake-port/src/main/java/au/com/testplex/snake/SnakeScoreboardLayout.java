package au.com.testplex.snake;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/** Scoreboard layout used by Mineplex Reborn's inherited SoloGame scoreboard. */
final class SnakeScoreboardLayout {
    private SnakeScoreboardLayout() {
    }

    record Participant(String name, boolean alive) {
    }

    static List<String> lines(List<Participant> roster) {
        List<String> result = new ArrayList<>();
        long alive = roster.stream().filter(Participant::alive).count();
        if (roster.size() < 15) {
            for (Participant participant : roster) {
                result.add((participant.alive() ? ChatColor.GREEN : ChatColor.GRAY) + participant.name());
            }
        } else if (alive < 16) {
            for (Participant participant : roster) {
                if (participant.alive()) result.add(ChatColor.GREEN + participant.name());
            }
        } else {
            result.add(ChatColor.GREEN + "Players Alive");
            result.add(ChatColor.WHITE + Long.toString(alive));
            result.add(" ");
            result.add(ChatColor.RED + "Players Dead");
            result.add(ChatColor.WHITE + Long.toString(roster.size() - alive));
        }
        return result;
    }
}
