package au.com.testplex.arcadeports;

import au.com.testplex.modern.model.ArcadeCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GameCosmeticRuntimeHooksTest {
    @Test
    void everyApprovedCosmeticLineHasAProductionRuntimeReference() throws IOException {
        String productionSources = java.util.stream.Stream.of(
                        Path.of("src", "main", "java"),
                        Path.of("..", "monster-maze-port", "src", "main", "java"),
                        Path.of("..", "snake-port", "src", "main", "java"),
                        Path.of("..", "src", "main", "java"))
                .filter(Files::isDirectory)
                .flatMap(root -> {
                    try (var files = Files.walk(root)) {
                        return files.filter(path -> path.toString().endsWith(".java"))
                                .filter(path -> !"ArcadeCatalog.java".equals(path.getFileName().toString()))
                                .map(path -> {
                                    try {
                                        return Files.readString(path);
                                    } catch (IOException exception) {
                                        throw new java.io.UncheckedIOException(exception);
                                    }
                                }).toList().stream();
                    } catch (IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                })
                .collect(Collectors.joining("\n"));

        for (var game : new ArcadeCatalog().getGames()) {
            for (var line : game.getCosmeticLines()) {
                assertTrue(productionSources.contains("\"" + line.getId() + "\""),
                        game.getId() + "/" + line.getId() + " has no production runtime reference");
            }
        }
    }
}
