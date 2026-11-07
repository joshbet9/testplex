package au.com.testplex.arcadeports;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvolutionEnabledConfigTest {
    @Test
    void evolutionIsInTheProductionEnabledQueue() throws Exception {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(resource, "production Arcade configuration must be packaged");
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(resource, StandardCharsets.UTF_8));
            List<String> enabled = configuration.getStringList("enabled-games");
            assertEquals(32, enabled.size(), "all 32 currently ready selections should be enabled");
            assertEquals(enabled.size(), enabled.stream().distinct().count(), "no duplicate game selections");
            assertTrue(enabled.contains("evolution"), "Evolution must be selectable for live testing");
            assertTrue(SourceMapCatalog.hasMap(ArcadePortGame.EVOLUTION),
                    "Evolution must use its recovered source arena instead of a generated replacement");

            Field allowListField = ArcadeMechanicsModernPlugin.class.getDeclaredField("READY_FOR_TEST_GAME_IDS");
            allowListField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> startupAllowList = (List<String>) allowListField.get(null);
            assertEquals(enabled, startupAllowList,
                    "the startup whitelist must not silently remove configured games");

            Field revisionField = ArcadeMechanicsModernPlugin.class.getDeclaredField("PLAYABLE_GAMES_REVISION");
            revisionField.setAccessible(true);
            assertTrue(((String) revisionField.get(null)).contains("32-selectable-ids"),
                    "existing servers need an allow-list revision bump to pick up Evolution");
        }
    }
}
