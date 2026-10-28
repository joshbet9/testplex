package me.monstermaze;

import me.monstermaze.util.GameScoreboard;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OptionalProtocolLibIsolationTest {
    @Test
    void coreGameScoreboardHasNoProtocolLibTypeReferences() throws Exception {
        String bytecode = classBytecode(GameScoreboard.class);
        assertFalse(bytecode.contains("com/comphenix/protocol"));
        assertFalse(bytecode.contains("com.comphenix.protocol"));
        assertFalse(bytecode.contains("Lme/monstermaze/util/ProtocolLibVisibility;"));
    }

    @Test
    void pluginLoadsTheOptionalDisguiseAdapterOnlyByName() throws Exception {
        String bytecode = classBytecode(MonsterMazePlugin.class);
        assertFalse(bytecode.contains("com/comphenix/protocol"));
        assertFalse(bytecode.contains("com.comphenix.protocol"));
        assertFalse(bytecode.contains("Lme/monstermaze/entity/MonsterDisguiseListener;"));
    }

    private static String classBytecode(Class<?> type) throws Exception {
        String resource = type.getSimpleName() + ".class";
        try (InputStream in = type.getResourceAsStream(resource);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            assertNotNull(in, "could not locate bytecode for " + type.getName());
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
        }
    }
}
