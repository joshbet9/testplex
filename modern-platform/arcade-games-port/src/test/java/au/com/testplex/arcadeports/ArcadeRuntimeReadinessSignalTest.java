package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeRuntimeReadinessSignalTest {
    private static final String SOURCE_SHA = "b8b9ee52ec4a7086fa4fcd06fd4b5f4b62570a80";
    private static final String NONCE = "e21c1c90-967b-4c5c-80f4-1de0a4e3aa83";
    private static final String INVOCATION_ID = "0123456789abcdef0123456789abcdef";

    @TempDir Path pluginDataFolder;

    @Test void publishesFullArcadeReadinessAfterIntegratedMonsterMazeRegistration() throws Exception {
        writeDeploymentIdentity();

        assertTrue(ArcadeRuntimeReadinessSignal.publish(pluginDataFolder, INVOCATION_ID, 4312, true));
        Properties signal = loadSignal();
        assertEquals("READY", signal.getProperty("status"));
        assertEquals("arcade-runtime", signal.getProperty("module"));
        assertEquals("READY", signal.getProperty("monster_maze"));
        assertEquals("testplex-arcade", signal.getProperty("manager"));
        assertEquals(ModernArcadeLobby.WORLD_NAME, signal.getProperty("world"));
        assertEquals("100x100", signal.getProperty("plane"));
        assertEquals("100", signal.getProperty("floor_y"));
        assertEquals(SOURCE_SHA, signal.getProperty("source_sha"));
        assertEquals(NONCE, signal.getProperty("nonce"));
        assertEquals(INVOCATION_ID, signal.getProperty("invocation_id"));
        assertEquals("4312", signal.getProperty("process_id"));
    }

    @Test void doesNotPublishOutsideADeployment() throws Exception {
        assertFalse(ArcadeRuntimeReadinessSignal.publish(pluginDataFolder, INVOCATION_ID, 4312, true));
        assertFalse(Files.exists(pluginDataFolder.resolve(ArcadeRuntimeReadinessSignal.READINESS_FILE)));
    }

    @Test void refusesReadinessWhenIntegratedMonsterMazeIsNotRegistered() throws Exception {
        writeDeploymentIdentity();
        assertThrows(java.io.IOException.class,
                () -> ArcadeRuntimeReadinessSignal.publish(pluginDataFolder, INVOCATION_ID, 4312, false));
        assertFalse(Files.exists(pluginDataFolder.resolve(ArcadeRuntimeReadinessSignal.READINESS_FILE)));
    }

    @Test void rejectsInvalidInvocationIdentity() throws Exception {
        writeDeploymentIdentity();
        assertThrows(java.io.IOException.class,
                () -> ArcadeRuntimeReadinessSignal.publish(pluginDataFolder, "invalid", 4312, true));
        assertFalse(Files.exists(pluginDataFolder.resolve(ArcadeRuntimeReadinessSignal.READINESS_FILE)));
    }

    private void writeDeploymentIdentity() throws Exception {
        Files.writeString(pluginDataFolder.resolve(ArcadeRuntimeReadinessSignal.IDENTITY_FILE),
                "source_sha=" + SOURCE_SHA + "\nnonce=" + NONCE + "\n");
    }

    private Properties loadSignal() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(
                pluginDataFolder.resolve(ArcadeRuntimeReadinessSignal.READINESS_FILE))) {
            properties.load(input);
        }
        return properties;
    }
}
