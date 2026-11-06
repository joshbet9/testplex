package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameDebugFeatureConsumerTest {
    private static final Pattern LITERAL_FEATURE = Pattern.compile("features\\.put\\(\\\"([^\\\"]+)\\\"");

    @Test
    void everyLiteralGameFeatureSwitchHasAProductionGameplayReference() throws IOException {
        Path moduleRoot = Path.of("src", "main", "java");
        Path productionRoot = Path.of("..");
        if (!Files.isDirectory(moduleRoot)) {
            Path project = Path.of("modern-platform", "arcade-games-port");
            moduleRoot = project.resolve("src/main/java");
            productionRoot = Path.of("modern-platform");
        } else if (Files.isDirectory(Path.of("modern-platform"))) {
            productionRoot = Path.of("modern-platform");
        }

        Path menu = moduleRoot.resolve(Path.of("au", "com", "testplex", "arcadeports",
                "ModernArcadeLobbyRuntime.java"));
        String menuSource = Files.readString(menu);
        Set<String> featureKeys = new LinkedHashSet<>();
        Matcher matcher = LITERAL_FEATURE.matcher(menuSource);
        while (matcher.find()) featureKeys.add(matcher.group(1));
        assertFalse(featureKeys.isEmpty(), "debug menu exposes literal game switches");

        try (Stream<Path> paths = Files.walk(productionRoot)) {
            String production = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().replace('\\', '/').contains("src/main/java"))
                    .filter(path -> !path.equals(menu))
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .reduce("", (left, right) -> left + "\n" + right);
            for (String key : featureKeys) {
                assertTrue(production.contains("\"" + key + "\"")
                                || production.contains("'" + key + "'"),
                        key + " is present in the debug UI but has no production consumer reference");
            }
        }
    }

    @Test
    void wizardsSpellFeatureMenuIsGeneratedFromImplementedSpellsAndGatesEachCast() throws IOException {
        Path project = Files.isDirectory(Path.of("modern-platform"))
                ? Path.of("modern-platform", "arcade-games-port") : Path.of(".");
        String menu = Files.readString(project.resolve(Path.of("src", "main", "java", "au", "com",
                "testplex", "arcadeports", "ModernArcadeLobbyRuntime.java")));
        String manager = Files.readString(project.resolve(Path.of("src", "main", "java", "au", "com",
                "testplex", "wizards", "WizardsGameManager.java")));
        assertTrue(menu.contains("WizardsSpell.implementedSpells()"));
        assertTrue(menu.contains("\"spell-\" + spell.id()"));
        assertTrue(manager.contains("\"spell-\" + spell.id()"));
        assertTrue(manager.contains("Spell casting is disabled for your debug profile."));
    }
}
