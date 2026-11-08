package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
