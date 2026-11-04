package au.com.testplex.arcadeports;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/** Shared Mineplex 1.8 player pass-through behavior for active arcade matches. */
final class ArcadeCollisionRules {
    private ArcadeCollisionRules() {
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
