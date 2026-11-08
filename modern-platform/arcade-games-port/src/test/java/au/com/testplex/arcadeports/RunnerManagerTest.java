package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class RunnerManagerTest {
    @Test
    void controllerClaimsRunnerOnly() {
        RunnerManager runner = new RunnerManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(runner);
        assertSame(runner, registry.get(ArcadePortGame.RUNNER));
        assertNull(registry.get(ArcadePortGame.DRAGONS));
    }

    @Test
    void paletteRecognizesExposedCourseColoursWithoutFalseMatches() {
        RunnerManager runner = new RunnerManager();
        assertEquals("light-blue", runner.paletteColour(Material.LIGHT_BLUE_CONCRETE));
        assertEquals("cyan", runner.paletteColour(Material.CYAN_WOOL));
        assertNull(runner.paletteColour(Material.RED_WOOL));
        assertNull(runner.paletteColour(Material.STONE));
    }

    @Test
    void emptySessionAdvancesExistingCadenceWithoutPlayerSearches() {
        RunnerManager runner = new RunnerManager();
        SessionState session = new SessionState(ArcadePortGame.RUNNER, null, List.of(), false);
        runner.updateDistanceAndSpectatorCompass(session, 1_000L);

        assertEquals(2_000L, session.runnerMarathonNextSampleAt);
        assertEquals(1_250L, session.runnerCompassNextUpdateAt);
        assertFalse(runner.isTrackingCompass(null));
        assertEquals(0, session.runnerMarathonDistance.size());
    }
}
