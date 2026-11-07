package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DragonEscapeGhostPathStoreTest {
    @TempDir Path pluginFolder;

    @Test void persistsExistingV1FrameFormatAcrossNewStoreInstances() throws Exception {
        UUID player = UUID.randomUUID();
        List<DragonEscapeGhostFrame> frames = List.of(
                frame(0, 10, 170, false), frame(100, 14, -170, true));
        DragonEscapeGhostPathStore store = store();
        store.save(player, "StevesSwamp", frames);

        assertEquals(frames, store.load(player, "StevesSwamp"));
        assertEquals(frames, store().load(player, "StevesSwamp"));
        try (var paths = Files.walk(pluginFolder)) {
            assertEquals(1L, paths.filter(path -> path.toString().endsWith(".ghost.gz")).count());
        }
    }

    @Test void rejectsUnsupportedFileVersionWithoutChangingTheSavedFormat() throws Exception {
        UUID player = UUID.randomUUID();
        store().save(player, "ThroughHell", List.of(frame(0, 1, 0, false)));
        Path ghost;
        try (var paths = Files.walk(pluginFolder)) {
            ghost = paths.filter(path -> path.toString().endsWith(".ghost.gz")).findFirst().orElseThrow();
        }
        try (var output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(ghost)))) {
            output.writeInt(0x44454748);
            output.writeInt(2); // v2 is intentionally unsupported by the v1 reader
            output.writeInt(1);
        }
        assertTrue(store().load(player, "ThroughHell").isEmpty());
    }

    @Test void compactionAndInterpolationPreserveMonotonicTimeAndShortestYawTurn() {
        List<DragonEscapeGhostFrame> frames = new ArrayList<>();
        DragonEscapeGhostPathStore.append(frames, frame(0, 0, 170, false));
        DragonEscapeGhostPathStore.append(frames, frame(0, 5, 0, true));
        DragonEscapeGhostPathStore.append(frames, frame(100, 10, -170, true));
        assertEquals(2, frames.size(), "same-time frames must be ignored");
        DragonEscapeGhostFrame midway = DragonEscapeGhostPathStore.interpolate(frames, 50);
        assertEquals(5.0, midway.x(), 0.0001);
        assertEquals(180.0, midway.yaw(), 0.0001);
        assertTrue(midway.sneaking());
        assertSame(frames.get(0), DragonEscapeGhostPathStore.interpolate(frames, -1));
        assertSame(frames.get(1), DragonEscapeGhostPathStore.interpolate(frames, 500));
        assertNull(DragonEscapeGhostPathStore.interpolate(List.of(), 50));

        for (int index = 101; index <= 120_200; index++) {
            DragonEscapeGhostPathStore.append(frames, frame(index, index, 0, false));
        }
        assertTrue(frames.size() <= 120_000, "excessive recordings must remain bounded");
        assertEquals(120_200L, frames.get(frames.size() - 1).timeMillis());
    }

    private DragonEscapeGhostPathStore store() {
        return new DragonEscapeGhostPathStore(pluginFolder, Logger.getLogger("ghost-path-test"));
    }

    private static DragonEscapeGhostFrame frame(long time, double x, float yaw, boolean sneaking) {
        return new DragonEscapeGhostFrame(time, x, 64, 0, yaw, 0,
                sneaking, false, true);
    }
}
