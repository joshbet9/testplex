package au.com.testplex.arcadeports;

/** Paintball-only scoreboard presentation rules for match participants. */
final class PaintballScoreboardRules {
    private PaintballScoreboardRules() { }

    static boolean hideParticipantNameTag(ArcadePortGame game, int teamId) {
        return game == ArcadePortGame.PAINTBALL && teamId >= 0;
    }
}
