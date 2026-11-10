package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DragonEscapeGhostSelectionTest {
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test void defaultsToPersonalBestAndRecognizesModes() {
        assertEquals(DragonEscapeGhostSelection.Mode.PERSONAL_BEST, DragonEscapeGhostSelection.mode(null));
        assertEquals(DragonEscapeGhostSelection.Mode.CURRENT_KIT, DragonEscapeGhostSelection.mode("KIT"));
        assertEquals(DragonEscapeGhostSelection.Mode.TARGET_PLAYER, DragonEscapeGhostSelection.mode("target"));
    }

    @Test void optOutOnlyBlocksOtherViewers() {
        assertTrue(DragonEscapeGhostSelection.canShare(a, b, true));
        assertFalse(DragonEscapeGhostSelection.canShare(a, b, false));
        assertTrue(DragonEscapeGhostSelection.canShare(a, a, false));
    }

    @Test void exposesPbAndWrSourcesWithFourConcurrentSlots() {
        assertEquals(java.util.List.of("personal-pb", "kit-pb", "world-record", "kit-record", "target"),
                DragonEscapeGhostSelection.sourceKeys());
        assertEquals(4, DragonEscapeGhostSelection.MAX_CONCURRENT_SOURCES);
        var sources = DragonEscapeGhostSelection.sourceKeys().stream()
                .map(key -> new DragonEscapeGhostSelection.Source(key, a, 1_000L, null, key))
                .toList();
        assertEquals(java.util.List.of("personal-pb", "kit-pb", "world-record", "kit-record"),
                DragonEscapeGhostSelection.limitConcurrent(sources).stream()
                        .map(DragonEscapeGhostSelection.Source::key).toList());
    }

    @Test void finishSummaryUsesSmallerTimeAsFaster() {
        assertTrue(DragonEscapeGhostSelection.relativeFinish(9_000L, 10_000L).endsWith("faster"));
        assertTrue(DragonEscapeGhostSelection.relativeFinish(11_000L, 10_000L).endsWith("slower"));
        assertEquals("exactly matched", DragonEscapeGhostSelection.relativeFinish(10_000L, 10_000L));
    }

    @Test void simultaneousGhostProfilesHaveUniqueClientTeamEntries() {
        UUID viewer = a;
        String personal = DragonEscapePacketGhostBridge.profileName(
                UUID.nameUUIDFromBytes(("testplex:dragon-escape-ghost:" + viewer + ":personal-pb")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        String kit = DragonEscapePacketGhostBridge.profileName(
                UUID.nameUUIDFromBytes(("testplex:dragon-escape-ghost:" + viewer + ":kit-pb")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertNotEquals(personal, kit);
        assertTrue(personal.length() <= 16);
        assertTrue(kit.length() <= 16);
    }
}
