package au.com.testplex.arcadeports;

/** Deterministic role rules for Dragon Escape finishers and eliminated racers. */
public final class DragonEscapeSpectatorRules {
    private DragonEscapeSpectatorRules() { }

    /** Active finishers remain in the race roster for placement but receive spectator tools. */
    public static boolean shouldReceiveSpectatorTools(boolean inRaceRoster, boolean completed) {
        return !inRaceRoster || completed;
    }

    /** Compass targets are online, unfinished racers who are not in spectator mode. */
    public static boolean isCompassTarget(boolean online, boolean inRaceRoster,
            boolean completed, boolean spectatorMode) {
        return online && inRaceRoster && !completed && !spectatorMode;
    }
}
