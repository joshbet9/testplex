package au.com.testplex.arcadeports;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Shared Mineplex 1.8 player pass-through behavior for active arcade matches. */
final class ArcadeCollisionRules {
    private ArcadeCollisionRules() {
    }

    static void disable(Player player) {
        if (player == null || !player.isOnline()) return;
        if (player.isCollidable()) player.setCollidable(false);
        // Scoreboard teams can override the entity flag, and some game controllers
        // replace their scoreboard while a match is running. Normalize all teams.
        Scoreboard scoreboard = player.getScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getOption(Team.Option.COLLISION_RULE) != Team.OptionStatus.NEVER) {
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            }
        }
        if (scoreboard.getEntryTeam(player.getName()) == null) {
            Team passthrough = scoreboard.getTeam("arcadepass");
            if (passthrough == null) passthrough = scoreboard.registerNewTeam("arcadepass");
            passthrough.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            passthrough.addEntry(player.getName());
        }
    }
}
