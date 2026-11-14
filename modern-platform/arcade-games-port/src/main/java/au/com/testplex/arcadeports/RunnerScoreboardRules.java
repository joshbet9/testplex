package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;

/** Source-shaped player listing for the solo Runner scoreboard. */
final class RunnerScoreboardRules {
    private RunnerScoreboardRules() { }

    static List<String> lines(List<PlayerEntry> players) {
        if (players == null || players.isEmpty()) return List.of();
        List<PlayerEntry> alive = players.stream().filter(PlayerEntry::alive).toList();
        if (players.size() < 15) {
            List<String> lines = new ArrayList<>(players.size());
            for (PlayerEntry player : players) {
                lines.add((player.alive() ? "§a" : "§7") + player.name());
            }
            return List.copyOf(lines);
        }
        if (alive.size() < 16) {
            return alive.stream().map(player -> "§a" + player.name()).toList();
        }
        return List.of("§aPlayers Alive", Integer.toString(alive.size()), "§cPlayers Dead",
                Integer.toString(players.size() - alive.size()));
    }

    record PlayerEntry(String name, boolean alive) { }
}
