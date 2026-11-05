package au.com.testplex.arcadeports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import me.monstermaze.game.GameState;
import me.monstermaze.game.GameManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameDebugPhaseActionsTest {
    @Test
    void everySharedRoundCanSkipItsStartCountdown() {
        for (ArcadePortGame game : ArcadePortGame.values()) {
            assertEquals(GameDebugPhaseAction.START_COUNTDOWN,
                    GameDebugPhaseActions.resolve(game, false, false, false, false, false), game.id());
        }
    }

    @Test
    void skippingTheStartCountdownMarksTheRoundAsDebugOnly() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "au", "com", "testplex", "arcadeports");
        if (!Files.isDirectory(sourceRoot)) {
            sourceRoot = Path.of("modern-platform", "arcade-games-port", "src", "main", "java", "au",
                    "com", "testplex", "arcadeports");
        }
        String manager = Files.readString(sourceRoot.resolve("ArcadePortManager.java")).replace("\r", "");
        assertTrue(manager.contains("if (type == ArcadePortGame.SKY_WARS) session.skyWarsLiveAt = session.prepareEndsAt;\n"
                + "                GameDebugRuntimeOverrides.markDebugSession();\n"
                + "                return true;"));
    }

    @Test
    void liveRoundTimersAndGameSpecificPhasesResolveToNormalRuntimeHandlers() {
        assertEquals(GameDebugPhaseAction.ROUND_TIMEOUT,
                GameDebugPhaseActions.resolve(ArcadePortGame.BOMB_LOBBERS, true, true, false, false, false));
        assertEquals(GameDebugPhaseAction.DRAW_TURN,
                GameDebugPhaseActions.resolve(ArcadePortGame.DRAW_MY_THING, true, false, true, false, false));
        assertEquals(GameDebugPhaseAction.MASTER_BUILDERS_PHASE,
                GameDebugPhaseActions.resolve(ArcadePortGame.MASTER_BUILDERS, true, false, false, true, false));
        assertEquals(GameDebugPhaseAction.TURF_WARS_PHASE,
                GameDebugPhaseActions.resolve(ArcadePortGame.TURF_WARS, true, false, false, false, true));
        assertEquals(GameDebugPhaseAction.NONE,
                GameDebugPhaseActions.resolve(ArcadePortGame.GLADIATORS, true, false, false, false, false));
    }

    @Test
    void monsterMazePhaseAdvanceRequiresALiveRoundAndCurrentSafePad() {
        assertFalse(GameManager.canDebugAdvancePhase(GameState.IDLE, true));
        assertFalse(GameManager.canDebugAdvancePhase(GameState.LIVE, false));
        assertTrue(GameManager.canDebugAdvancePhase(GameState.LIVE, true));
    }

    @Test
    void superSploorPhaseAdvanceRequiresARunningRuntime() {
        assertFalse(GameDebugPhaseActions.canAdvanceSuperSploor(false));
        assertTrue(GameDebugPhaseActions.canAdvanceSuperSploor(true));
    }
}
