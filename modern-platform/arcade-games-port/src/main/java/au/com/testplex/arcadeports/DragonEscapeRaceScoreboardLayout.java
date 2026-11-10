package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;

/** Layout and row styling for the compact Dragon Escape race sidebar. */
final class DragonEscapeRaceScoreboardLayout {
    static final int MAX_ROWS = 15;
    static final int RECORD_ROW_COUNT = 4;
    static final int RACER_ROW_COUNT = MAX_ROWS - RECORD_ROW_COUNT;
    static final int MAP_PB_ROW = RACER_ROW_COUNT;
    static final int MAP_WR_ROW = MAP_PB_ROW + 1;
    static final int KIT_PB_ROW = MAP_WR_ROW + 1;
    static final int KIT_WR_ROW = KIT_PB_ROW + 1;

    enum RacerStatus { RUNNING, FINISHED, ELIMINATED }

    private DragonEscapeRaceScoreboardLayout() { }

    static String racerName(String name, RacerStatus status) {
        return color(status).toString() + strikethrough(status) + name;
    }

    static String racerTime(String value, RacerStatus status) {
        return racerTime(value, status, false);
    }

    static String racerTime(String value, RacerStatus status, boolean firstPlace) {
        if (status == RacerStatus.ELIMINATED) return "";
        ChatColor timeColor = status == RacerStatus.RUNNING
                ? firstPlace ? ChatColor.GREEN : ChatColor.RED : color(status);
        return timeColor.toString() + " " + value + ChatColor.RESET;
    }

    private static String strikethrough(RacerStatus status) {
        return status == RacerStatus.ELIMINATED ? ChatColor.STRIKETHROUGH.toString() : "";
    }

    private static ChatColor color(RacerStatus status) {
        return switch (status) {
            case RUNNING -> ChatColor.YELLOW;
            case FINISHED -> ChatColor.AQUA;
            case ELIMINATED -> ChatColor.GRAY;
        };
    }
}
