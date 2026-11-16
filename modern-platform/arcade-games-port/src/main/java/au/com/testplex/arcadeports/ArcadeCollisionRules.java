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
        // Paper documents setCollidable for non-player entities only. Player
        // pass-through is controlled by scoreboard teams; avoid changing the
        // player's entity collision flag so native melee and projectile targeting
        // continue to see the normal player hitbox.
        // Some game controllers replace their scoreboard while a match is running.
        // Normalize all teams on the active scoreboard to preserve pass-through.
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
