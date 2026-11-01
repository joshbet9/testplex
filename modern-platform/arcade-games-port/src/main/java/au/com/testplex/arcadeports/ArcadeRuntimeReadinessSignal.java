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

/** Publishes a process-correlated signal that the shared Testplex Arcade lobby is ready. */
final class ArcadeRuntimeReadinessSignal {
    static final String IDENTITY_FILE = ".testplex-modern-deploy-identity.properties";
    static final String READINESS_FILE = ".testplex-arcade-readiness.properties";

    private ArcadeRuntimeReadinessSignal() { }

    static boolean publish(Path pluginDataFolder, String invocationId, long processId,
                           boolean monsterMazeRegistered) throws IOException {
        if (!monsterMazeRegistered) {
            throw new IOException("The integrated Monster Maze game is not registered with Testplex Arcade.");
        }
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
        signal.setProperty("module", "arcade-runtime");
        signal.setProperty("monster_maze", "READY");
        signal.setProperty("manager", "testplex-arcade");
        signal.setProperty("world", ModernArcadeLobby.WORLD_NAME);
        signal.setProperty("plane", "100x100");
        signal.setProperty("floor_y", Integer.toString(ModernArcadeLobby.FLOOR_Y));
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
        Path temporary = Files.createTempFile(parent, ".testplex-arcade-readiness-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Testplex Arcade runtime readiness; generated after the shared lobby and integrated games are initialised");
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
