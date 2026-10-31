package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MonsterMazeRegistrationReadinessTest {
    private static final String SOURCE_SHA = "955d9e60d74b985ee79c87f90efddadcad26902d";
    private static final String NONCE = "5c4f782e-f5f3-44c8-a69e-b617e043fb3f";
    private static final String INVOCATION_ID = "0123456789abcdef0123456789abcdef";

    @TempDir Path pluginDataFolder;

    @Test void publishesCorrelatedSignalOnlyAfterInitializationAndManagerAcceptance() throws Exception {
        MonsterMazeRegistrationReadiness readiness = new MonsterMazeRegistrationReadiness();
        boolean[] initialized = { false };
        boolean[] attached = { false };
        readiness.initializeAndAttach(() -> initialized[0] = true, () -> initialized[0],
                () -> attached[0] = true, () -> attached[0]);
        writeDeploymentIdentity();
        Files.writeString(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE),
                "status=READY\nsource_sha=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nnonce=stale\n");

        assertTrue(MonsterMazeRegistrationSignal.publish(pluginDataFolder, readiness, INVOCATION_ID, 4312));
        Properties record = loadReadinessRecord();
        assertEquals("READY", record.getProperty("status"));
        assertEquals("monster-maze", record.getProperty("module"));
        assertEquals("testplex-arcade", record.getProperty("manager"));
        assertEquals(SOURCE_SHA, record.getProperty("source_sha"));
        assertEquals(NONCE, record.getProperty("nonce"));
        assertEquals(INVOCATION_ID, record.getProperty("invocation_id"));
        assertEquals("4312", record.getProperty("process_id"));
    }

    @Test void rejectedManagerRegistrationFailsAndCannotPublishReadiness() throws Exception {
        MonsterMazeRegistrationReadiness readiness = new MonsterMazeRegistrationReadiness();
        assertThrows(IllegalStateException.class, () -> readiness.initializeAndAttach(
                () -> { }, () -> true, () -> { }, () -> false));
        assertEquals(MonsterMazeRegistrationReadiness.State.FAILED, readiness.state());
        writeDeploymentIdentity();

        assertThrows(IllegalStateException.class,
                () -> MonsterMazeRegistrationSignal.publish(pluginDataFolder, readiness, INVOCATION_ID, 4312));
        assertFalse(Files.exists(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE)));
    }

    @Test void initializationExceptionFailsClosed() throws Exception {
        MonsterMazeRegistrationReadiness readiness = new MonsterMazeRegistrationReadiness();
        assertThrows(IllegalStateException.class, () -> readiness.initializeAndAttach(
                () -> { throw new IllegalStateException("simulated Monster Maze initialization failure"); },
                () -> true, () -> { }, () -> true));
        assertEquals(MonsterMazeRegistrationReadiness.State.FAILED, readiness.state());
        writeDeploymentIdentity();

        assertThrows(IllegalStateException.class,
                () -> MonsterMazeRegistrationSignal.publish(pluginDataFolder, readiness, INVOCATION_ID, 4312));
        assertFalse(Files.exists(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE)));
    }

    @Test void readinessCannotBePublishedBeforeManagerAttachment() throws Exception {
        MonsterMazeRegistrationReadiness readiness = new MonsterMazeRegistrationReadiness();
        writeDeploymentIdentity();

        assertThrows(IllegalStateException.class, () -> readiness.initializeAndAttach(
                () -> { }, () -> true, readiness::requireReady, () -> true));
        assertEquals(MonsterMazeRegistrationReadiness.State.FAILED, readiness.state());
        assertThrows(IllegalStateException.class,
                () -> MonsterMazeRegistrationSignal.publish(pluginDataFolder, readiness, INVOCATION_ID, 4312));
        assertFalse(Files.exists(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE)));
    }

    @Test void deploymentSignalRequiresFreshSystemdInvocationAndValidIdentity() throws Exception {
        MonsterMazeRegistrationReadiness readiness = new MonsterMazeRegistrationReadiness();
        readiness.initializeAndAttach(() -> { }, () -> true, () -> { }, () -> true);
        writeDeploymentIdentity();

        assertThrows(java.io.IOException.class,
                () -> MonsterMazeRegistrationSignal.publish(pluginDataFolder, readiness, "", 4312));
        assertFalse(Files.exists(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE)));
    }

    private void writeDeploymentIdentity() throws Exception {
        Files.writeString(pluginDataFolder.resolve(MonsterMazeRegistrationSignal.IDENTITY_FILE),
                "source_sha=" + SOURCE_SHA + "\nnonce=" + NONCE + "\n");
    }

    private Properties loadReadinessRecord() throws Exception {
        Properties record = new Properties();
        try (InputStream input = Files.newInputStream(
                pluginDataFolder.resolve(MonsterMazeRegistrationSignal.READINESS_FILE))) {
            record.load(input);
        }
        return record;
    }
}
