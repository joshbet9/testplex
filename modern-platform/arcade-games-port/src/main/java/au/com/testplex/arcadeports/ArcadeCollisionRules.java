package au.com.testplex.arcadeports;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Shared Mineplex 1.8 player pass-through behavior for active arcade matches. */
final class ArcadeCollisionRules {
    private ArcadeCollisionRules() {
    }

    static List<String> participantEntries(Collection<String> playerNames) {
        if (playerNames == null || playerNames.isEmpty()) return List.of();
        return playerNames.stream().filter(name -> name != null && !name.isBlank()).distinct().toList();
    }

    static void addNoCollisionTeam(Scoreboard scoreboard, Collection<String> playerNames) {
        if (scoreboard == null) return;
        Team team = scoreboard.getTeam("de_pass");
        if (team == null) team = scoreboard.registerNewTeam("de_pass");
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        for (String playerName : participantEntries(playerNames)) team.addEntry(playerName);
    }

    static void disable(Player player) {
        if (player == null || !player.isOnline()) return;
        player.setCollidable(false);
        // Scoreboard teams can override the entity flag, and some game controllers
        // replace their scoreboard while a match is running. Normalize all teams.
        for (Team team : player.getScoreboard().getTeams()) {
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
    }
}
