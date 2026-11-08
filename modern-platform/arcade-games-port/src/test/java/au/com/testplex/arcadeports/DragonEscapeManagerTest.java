package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class DragonEscapeManagerTest {
    @Test
    void controllerClaimsOnlyDragonEscape() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(escape);

        assertSame(escape, registry.get(ArcadePortGame.DRAGON_ESCAPE));
        assertNull(registry.get(ArcadePortGame.DRAGONS));
    }


    @Test
    void checkpointProgressUsesOnlyNextWaypointAndClampsCurrentIndex() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        World world = worldStub();
        SessionState session = new SessionState(ArcadePortGame.DRAGON_ESCAPE, world, List.of(), false);
        session.dragonEscapeWaypoints.add(new Location(world, 0.0D, 64.0D, 0.0D));
        session.dragonEscapeWaypoints.add(new Location(world, 10.0D, 64.0D, 0.0D));
        session.dragonEscapeWaypoints.add(new Location(world, 0.0D, 64.0D, 0.0D));

        assertEquals(0, escape.waypointIndex(session, new Location(world, 2.0D, 64.0D, 0.0D), 0));
        assertEquals(1, escape.waypointIndex(session, new Location(world, 7.0D, 64.0D, 0.0D), 0));
        assertEquals(2, escape.waypointIndex(session, new Location(world, 7.0D, 64.0D, 0.0D), 99));
    }

    @Test
    void checkpointLookupHandlesMissingMarkersAndWrongWorldLocations() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        World world = worldStub();
        World otherWorld = worldStub();
        SessionState empty = new SessionState(ArcadePortGame.DRAGON_ESCAPE, world, List.of(), false);

        assertEquals(-1, escape.waypointIndex(empty, new Location(world, 0, 64, 0), 0));
        assertEquals(0, escape.closestWaypointIndex(empty, new Location(world, 0, 64, 0)));

        SessionState session = new SessionState(ArcadePortGame.DRAGON_ESCAPE, world, List.of(), false);
        session.dragonEscapeWaypoints.add(new Location(world, 4.0D, 64.0D, 0.0D));
        session.dragonEscapeWaypoints.add(new Location(world, 14.0D, 64.0D, 0.0D));
        assertEquals(1, escape.closestWaypointIndex(session, new Location(world, 13.0D, 64.0D, 0.0D)));
        assertEquals(0, escape.closestWaypointIndex(session, null));
        assertEquals(0, escape.closestWaypointIndex(session, new Location(otherWorld, 13.0D, 64.0D, 0.0D)));
        assertEquals(0, escape.waypointIndex(session, new Location(otherWorld, 0, 64, 0), 0));
    }

    private static World worldStub() {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "Dragon Escape test world";
                    default -> method.getReturnType() == boolean.class ? false
                            : method.getReturnType() == int.class ? 0
                            : method.getReturnType() == long.class ? 0L
                            : method.getReturnType() == double.class ? 0.0D
                            : method.getReturnType() == float.class ? 0.0F
                            : method.getReturnType() == short.class ? (short) 0
                            : method.getReturnType() == byte.class ? (byte) 0
                            : method.getReturnType() == char.class ? '\0' : null;
                });
    }

    @Test
    void emptyRaceScoreboardUpdateAndCleanupDoNotRequireServer() {
        DragonEscapeManager escape = new DragonEscapeManager(Logger.getAnonymousLogger());
        SessionState session = new SessionState(ArcadePortGame.DRAGON_ESCAPE, null, List.of(), false);

        escape.updateScoreboard(session, 10L, List.of(), (leader, racer) -> 0L);
        escape.cleanupScoreboard(session);
        escape.updateScoreboard(null, 10L, List.of(), (leader, racer) -> 0L);
        escape.updateSpectatorCompasses(session);
        escape.updateSpectatorCompasses(null);

        assertTrue(session.dragonEscapeScoreboardTeams.isEmpty());
        assertFalse(escape.isTrackingCompass(null));
    }
}
