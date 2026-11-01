package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeGameModuleRegistryTest {
    @Test
    void registrationInitializesTheModuleAndMakesItAvailableToTheArcadeManager() {
        ArcadeGameModuleRegistry registry = new ArcadeGameModuleRegistry();
        FakeModule monsterMaze = new FakeModule(ArcadePortGame.MONSTER_MAZE, new ArrayList<>(),
                ArcadePortGame.MONSTER_MAZE, ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN);

        registry.register(monsterMaze);

        assertEquals(List.of("init:monster-maze"), monsterMaze.events);
        for (ArcadePortGame game : List.of(ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN)) {
            assertSame(monsterMaze, registry.get(game));
            assertTrue(registry.isRegistered(game, monsterMaze));
        }
        assertEquals(List.of(ArcadePortGame.MONSTER_MAZE, ArcadePortGame.MONSTER_MAZE_SPEED,
                ArcadePortGame.MONSTER_MAZE_MODERN), registry.registeredGames());
    }

    @Test
    void rejectsDuplicateGameRegistrationWithoutInitializingTheSecondModule() {
        ArcadeGameModuleRegistry registry = new ArcadeGameModuleRegistry();
        List<String> events = new ArrayList<>();
        FakeModule first = new FakeModule(ArcadePortGame.MONSTER_MAZE, events);
        FakeModule duplicate = new FakeModule(ArcadePortGame.MONSTER_MAZE, events);

        registry.register(first);

        assertThrows(IllegalStateException.class, () -> registry.register(duplicate));
        assertEquals(List.of("init:monster-maze"), events);
        assertSame(first, registry.get(ArcadePortGame.MONSTER_MAZE));
    }

    @Test
    void failedInitializationIsRemovedAndCleanedUpBeforeFailureEscapes() {
        ArcadeGameModuleRegistry registry = new ArcadeGameModuleRegistry();
        FakeModule failing = new FakeModule(ArcadePortGame.MONSTER_MAZE, new ArrayList<>(),
                ArcadePortGame.MONSTER_MAZE, ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN);
        failing.failInitialization = true;

        assertThrows(IllegalStateException.class, () -> registry.register(failing));

        for (ArcadePortGame game : List.of(ArcadePortGame.MONSTER_MAZE,
                ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN)) {
            assertNull(registry.get(game));
            assertFalse(registry.isRegistered(game));
        }
        assertEquals(List.of("init:monster-maze", "shutdown:monster-maze"), failing.events);
    }

    @Test
    void shutdownRunsInReverseRegistrationOrderAndIsIdempotent() {
        ArcadeGameModuleRegistry registry = new ArcadeGameModuleRegistry();
        List<String> events = new ArrayList<>();
        FakeModule monsterMaze = new FakeModule(ArcadePortGame.MONSTER_MAZE, events,
                ArcadePortGame.MONSTER_MAZE, ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN);
        FakeModule plexball = new FakeModule(ArcadePortGame.PLEXBALL, events);

        registry.register(monsterMaze);
        registry.register(plexball);
        registry.shutdown();
        registry.shutdown();

        assertEquals(List.of(
                "init:monster-maze", "init:plexball",
                "shutdown:plexball", "shutdown:monster-maze"), events);
        assertTrue(registry.registeredGames().isEmpty());
        assertThrows(IllegalStateException.class, () -> registry.register(monsterMaze));
    }

    private static final class FakeModule implements ArcadeGameModule {
        private final ArcadePortGame game;
        private final java.util.Set<ArcadePortGame> games;
        private final List<String> events;
        private boolean failInitialization;

        private FakeModule(ArcadePortGame game, List<String> events, ArcadePortGame... gameIds) {
            this.game = game;
            this.games = new java.util.LinkedHashSet<>(gameIds.length == 0 ? List.of(game) : List.of(gameIds));
            this.events = events;
        }

        @Override public ArcadePortGame game() { return game; }
        @Override public java.util.Set<ArcadePortGame> games() { return games; }
        @Override public void initialize() {
            events.add("init:" + game.id());
            if (failInitialization) throw new IllegalStateException("simulated startup failure");
        }
        @Override public void shutdown() { events.add("shutdown:" + game.id()); }
        @Override public boolean isGameRunning() { return false; }
    }
}
