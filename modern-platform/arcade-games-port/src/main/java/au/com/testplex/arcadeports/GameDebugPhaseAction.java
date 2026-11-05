package au.com.testplex.arcadeports;

/** Describes which live phase deadline a debug action can safely advance. */
enum GameDebugPhaseAction {
    NONE,
    START_COUNTDOWN,
    DRAW_TURN,
    MASTER_BUILDERS_PHASE,
    TURF_WARS_PHASE,
    ROUND_TIMEOUT
}

final class GameDebugPhaseActions {
    private GameDebugPhaseActions() { }

    static GameDebugPhaseAction resolve(ArcadePortGame game, boolean live, boolean hasRoundTimeout,
            boolean hasDrawTurn, boolean hasMasterBuildersPhase, boolean hasTurfWarsPhase) {
        if (game == null) return GameDebugPhaseAction.NONE;
        if (!live) return GameDebugPhaseAction.START_COUNTDOWN;
        if (game == ArcadePortGame.DRAW_MY_THING && hasDrawTurn) return GameDebugPhaseAction.DRAW_TURN;
        if (game == ArcadePortGame.MASTER_BUILDERS && hasMasterBuildersPhase) {
            return GameDebugPhaseAction.MASTER_BUILDERS_PHASE;
        }
        if (game == ArcadePortGame.TURF_WARS && hasTurfWarsPhase) return GameDebugPhaseAction.TURF_WARS_PHASE;
        return hasRoundTimeout ? GameDebugPhaseAction.ROUND_TIMEOUT : GameDebugPhaseAction.NONE;
    }
}
