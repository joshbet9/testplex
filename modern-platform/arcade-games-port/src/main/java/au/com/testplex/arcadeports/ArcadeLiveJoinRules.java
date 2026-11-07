package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

/** Shared admission rules for the five respawning team games that accept live joins. */
final class ArcadeLiveJoinRules {
    static final int MAX_PARTICIPANTS = ArcadeTestPlayerManager.TARGET_ROSTER_SIZE;

    enum Admission { WAIT_FOR_LIVE, READY, REPLACE_TEST_PLAYER, FULL }

    private ArcadeLiveJoinRules() { }

    static boolean supports(ArcadePortGame type) {
        return type == ArcadePortGame.CASTLE_ASSAULT
                || type == ArcadePortGame.HEROES_OF_GWEN
                || type == ArcadePortGame.SUPER_SPLOOR
                || type == ArcadePortGame.TURF_WARS
                || type == ArcadePortGame.SHEEP_QUEST;
    }

    static Admission admission(boolean live, int participants, int availableTestPlayers) {
        if (!live) return Admission.WAIT_FOR_LIVE;
        if (participants < MAX_PARTICIPANTS) return Admission.READY;
        if (availableTestPlayers > 0) return Admission.REPLACE_TEST_PLAYER;
        return Admission.FULL;
    }

    static String teamName(ArcadePortGame game, int team) {
        if (game == ArcadePortGame.SHEEP_QUEST) {
            return switch (team) {
                case 0 -> "Red";
                case 1 -> "Aqua";
                case 2 -> "Green";
                case 3 -> "Yellow";
                default -> "Team " + (team + 1);
            };
        }
        return switch (team) {
            case 0 -> "Red";
            case 1 -> "Blue";
            default -> "Team " + (team + 1);
        };
    }

    static ChatColor teamColour(ArcadePortGame game, int team) {
        if (game == ArcadePortGame.SHEEP_QUEST) {
            return switch (team) {
                case 0 -> ChatColor.RED;
                case 1 -> ChatColor.AQUA;
                case 2 -> ChatColor.GREEN;
                case 3 -> ChatColor.YELLOW;
                default -> ChatColor.LIGHT_PURPLE;
            };
        }
        return team == 0 ? ChatColor.RED : team == 1 ? ChatColor.AQUA : ChatColor.LIGHT_PURPLE;
    }
}
