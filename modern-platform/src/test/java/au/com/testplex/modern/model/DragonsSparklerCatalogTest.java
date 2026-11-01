package au.com.testplex.modern.model;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DragonsSparklerCatalogTest {
    @Test
    void dragonsUseASparklerCosmeticLineWithNewSparklerSpecificChoices() {
        GameDefinition dragons = new ArcadeCatalog().getGame("dragons");
        assertNotNull(dragons);

        CosmeticLineDefinition sparkler = dragons.getCosmeticLines().stream()
                .filter(line -> "sparkler-effects".equals(line.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(CosmeticLineDefinition.SelectionMode.SINGLE, sparkler.getSelectionMode());
        assertEquals("golden-thread", sparkler.getDefaultOptionId());

        Set<String> options = sparkler.getOptions().stream()
                .map(CosmeticOption::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of("golden-thread", "cinder-drift", "glacier-dust",
                "void-motes", "starlight", "prismatic"), options);
        assertFalse(options.contains("classic"));
        assertFalse(options.contains("ember"));
        assertFalse(options.contains("frost"));
        assertFalse(options.contains("ender"));
        assertFalse(options.contains("spark"));
        assertFalse(options.contains("aurora"));
    }
}
