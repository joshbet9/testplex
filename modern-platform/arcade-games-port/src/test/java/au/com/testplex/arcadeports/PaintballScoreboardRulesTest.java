package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaintballScoreboardRulesTest {
    @Test
    void hidesOnlyAssignedPaintballParticipants() {
        assertTrue(PaintballScoreboardRules.hideParticipantNameTag(ArcadePortGame.PAINTBALL, 0));
        assertTrue(PaintballScoreboardRules.hideParticipantNameTag(ArcadePortGame.PAINTBALL, 1));
        assertFalse(PaintballScoreboardRules.hideParticipantNameTag(ArcadePortGame.PAINTBALL, -1));
        assertFalse(PaintballScoreboardRules.hideParticipantNameTag(ArcadePortGame.GLADIATORS, 0));
    }
}
