package au.com.testplex.arcadeports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Publishes a per-process, deployment-correlated positive registration record. */
final class MonsterMazeRegistrationSignal {
    static final String IDENTITY_FILE = ".testplex-modern-deploy-identity.properties";
    static final String READINESS_FILE = ".monster-maze-readiness.properties";

    private MonsterMazeRegistrationSignal() { }

    static boolean publish(Path pluginDataFolder, MonsterMazeRegistrationReadiness readiness,
                           String invocationId, long processId) throws IOException {
        readiness.requireReady();
        Path identity = pluginDataFolder.resolve(IDENTITY_FILE);
        if (!Files.isRegularFile(identity)) return false;

        Properties deployment = new Properties();
        try (InputStream input = Files.newInputStream(identity)) {
            deployment.load(input);
        }
        String sourceSha = deployment.getProperty("source_sha", "");
        String nonce = deployment.getProperty("nonce", "");
        if (!sourceSha.matches("[0-9a-f]{40}")) {
            throw new IOException("Deployment identity has an invalid source_sha.");
        }
        try {
            UUID.fromString(nonce);
        } catch (IllegalArgumentException invalidNonce) {
            throw new IOException("Deployment identity has an invalid nonce.", invalidNonce);
        }
        if (invocationId == null || !invocationId.matches("[0-9a-fA-F]{32}")) {
            throw new IOException("Systemd did not provide a valid INVOCATION_ID.");
        }
        if (processId <= 0) throw new IOException("Java process ID is invalid.");

        Properties signal = new Properties();
        signal.setProperty("status", "READY");
        signal.setProperty("module", "monster-maze");
        signal.setProperty("manager", "testplex-arcade");
        signal.setProperty("source_sha", sourceSha);
        signal.setProperty("nonce", nonce);
        signal.setProperty("invocation_id", invocationId);
        signal.setProperty("process_id", Long.toString(processId));
        writeAtomically(pluginDataFolder, pluginDataFolder.resolve(READINESS_FILE), signal);
        return true;
    }

    static void remove(Path pluginDataFolder) throws IOException {
        Files.deleteIfExists(pluginDataFolder.resolve(READINESS_FILE));
    }

    private static void writeAtomically(Path parent, Path destination, Properties values) throws IOException {
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".monster-maze-readiness-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Testplex Arcade runtime readiness; generated after module attachment");
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
