package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.junit.jupiter.api.Test;

class MicroBattlesManagerTest {
    @Test
    void controllerClaimsOnlyMicroBattles() {
        MicroBattlesManager micro = new MicroBattlesManager();
        ArcadeMatchControllerRegistry registry = new ArcadeMatchControllerRegistry();
        registry.register(micro);

        assertSame(micro, registry.get(ArcadePortGame.MICRO_BATTLES));
        assertNull(registry.get(ArcadePortGame.TURF_WARS));
    }

    @Test
    void noWorldOrPlayerListDoesNotSpawnParticles() {
        MicroBattlesManager micro = new MicroBattlesManager();
        SessionState session = new SessionState(ArcadePortGame.MICRO_BATTLES, null, List.of(), false);

        micro.updateBorderParticles(session, List.of(), ignored -> true);
        micro.updateBorderParticles(null, List.of(), ignored -> true);
        micro.updateBorderParticles(session, null, ignored -> true);
    }

    @Test
    void fourTeamIdentityMatchesOriginalColorsAndBadgeRules() {
        MicroBattlesManager micro = new MicroBattlesManager();
        assertEquals(Color.RED, micro.teamDyeColor(0));
        assertEquals(Color.AQUA, micro.teamDyeColor(1));
        assertEquals(Color.LIME, micro.teamDyeColor(2));
        assertEquals(Color.YELLOW, micro.teamDyeColor(3));
        assertEquals(ChatColor.GREEN, micro.teamChatColor(2));
        assertEquals("Red", micro.teamName(0));
        assertEquals("Blue", micro.teamName(1));
        assertEquals("Green", micro.teamName(2));
        assertEquals("Yellow", micro.teamName(3));
        assertFalse(micro.isTeamLockedItem(null, null));
        assertFalse(micro.isTeamBadgeItem(null, null));
    }

    @Test
    void emptySpectatorRosterKeepsCompassStateAndTickRoutingSafe() {
        MicroBattlesManager micro = new MicroBattlesManager();
        SessionState session = new SessionState(ArcadePortGame.MICRO_BATTLES, null, List.of(), false);
        session.ticks = 4L;

        micro.updateSpectatorCompass(session);
        micro.updateSpectatorCompass(null);
        micro.giveTrackingCompass(null);

        assertNull(micro.nearestSpectatorTarget(session, null));
        assertFalse(micro.isTrackingCompass(null));
        assertTrue(session.players.isEmpty());
    }
}
