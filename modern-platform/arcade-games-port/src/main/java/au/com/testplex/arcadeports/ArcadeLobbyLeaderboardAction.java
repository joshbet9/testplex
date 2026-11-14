package au.com.testplex.arcadeports;

import java.util.List;

/** Saved interaction behavior for the left and right click on one leaderboard. */
enum ArcadeLobbyLeaderboardAction {
    DEFAULT_CYCLE("default-cycle", "Default Cycle", "Preserve the current period or PB cycling."),
    CYCLE_PERIOD("cycle-period", "Cycle Period", "Move through enabled time windows."),
    CYCLE_STAT("cycle-stat", "Cycle Stat", "Select the next tracked statistic."),
    CYCLE_MAP("cycle-map", "Cycle Map", "Select the next Dragon Escape map."),
    CYCLE_KIT("cycle-kit", "Cycle Kit", "Select the next Dragon Escape kit."),
    NONE("none", "No Action", "Leave this click unused.");

    private final String id;
    private final String label;
    private final String description;

    ArcadeLobbyLeaderboardAction(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    String id() { return id; }
    String label() { return label; }
    String description() { return description; }

    static ArcadeLobbyLeaderboardAction byId(Object id, ArcadeLobbyLeaderboardAction fallback) {
        if (id != null) for (ArcadeLobbyLeaderboardAction action : values())
            if (action.id.equals(String.valueOf(id))) return action;
        return fallback;
    }

    static List<ArcadeLobbyLeaderboardAction> choices(boolean dragonEscapePb) {
        return dragonEscapePb ? List.of(values())
                : List.of(DEFAULT_CYCLE, CYCLE_PERIOD, CYCLE_STAT, NONE);
    }
}
