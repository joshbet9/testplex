package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArcadeMatchControllerRegistryTest {
    @Test
    void dispatchesOnlyTheControllerForTheCurrentMatchWithoutChangingLifecycle() {
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        List<String> events = new ArrayList<>();
        FakeController bacon = new FakeController(ArcadePortGame.BACON_BRAWL, events);
        registry.register(bacon);
        SessionState baconSession = session(ArcadePortGame.BACON_BRAWL);

        registry.prepare(baconSession, List.of());
        registry.tick(baconSession, 123L);
        registry.finish(baconSession);
        registry.prepare(session(ArcadePortGame.EVOLUTION), List.of());
        registry.tick(null, 124L);
        registry.finish(null);

        assertSame(bacon, registry.get(ArcadePortGame.BACON_BRAWL));
        assertNull(registry.get(ArcadePortGame.EVOLUTION));
        assertEquals(List.of("prepare:bacon-brawl", "tick:123", "finish:bacon-brawl"), events);
    }

    @Test
    void handlesSelectableAliasesWithOneControllerAndRejectsPartialDuplicateRegistration() {
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        List<String> events = new ArrayList<>();
        FakeController maze = new FakeController(ArcadePortGame.MONSTER_MAZE, events,
                ArcadePortGame.MONSTER_MAZE_SPEED, ArcadePortGame.MONSTER_MAZE_MODERN);
        registry.register(maze);

        assertSame(maze, registry.get(ArcadePortGame.MONSTER_MAZE_SPEED));
        registry.tick(session(ArcadePortGame.MONSTER_MAZE_MODERN), 1L);
        assertEquals(List.of("tick:1"), events);
        assertThrows(IllegalStateException.class, () -> registry.register(
                new FakeController(ArcadePortGame.MONSTER_MAZE_SPEED, events,
                        ArcadePortGame.EVOLUTION)));
        assertNull(registry.get(ArcadePortGame.EVOLUTION));
    }

    @Test
    void rejectsInvalidControllersBeforeModifyingRegistry() {
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new ArcadeMatchController() {
            @Override public ArcadePortGame game() { return ArcadePortGame.BACON_BRAWL; }
            @Override public Set<ArcadePortGame> games() { return Set.of(ArcadePortGame.EVOLUTION); }
        }));
        assertNull(registry.get(ArcadePortGame.BACON_BRAWL));
    }

    private static SessionState session(ArcadePortGame game) {
        return new SessionState(game, null, List.of(), false);
    }

    private static final class FakeController implements ArcadeMatchController {
        private final ArcadePortGame game;
        private final List<String> events;
        private final Set<ArcadePortGame> games;

        private FakeController(ArcadePortGame game, List<String> events, ArcadePortGame... aliases) {
            this.game = game;
            this.events = events;
            this.games = new java.util.LinkedHashSet<>();
            games.add(game);
            games.addAll(List.of(aliases));
        }

        @Override public ArcadePortGame game() { return game; }
        @Override public Set<ArcadePortGame> games() { return games; }
        @Override public void onPrepare(SessionState session, List<org.bukkit.entity.Player> participants) {
            events.add("prepare:" + session.type.id());
        }
        @Override public void onTick(SessionState session, long now) { events.add("tick:" + now); }
        @Override public void onFinish(SessionState session) { events.add("finish:" + session.type.id()); }
    }
}
