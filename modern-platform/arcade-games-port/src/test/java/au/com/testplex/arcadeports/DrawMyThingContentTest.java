package au.com.testplex.arcadeports;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DrawMyThingContentTest {
    @Test void keepsTheSixteenPaintColoursInTheirExistingHotbarOrder() {
        assertEquals(16, DrawMyThingContent.COLOURS.size());
        assertEquals(Material.WHITE_WOOL, DrawMyThingContent.COLOURS.get(0));
        assertEquals(Material.BLACK_WOOL, DrawMyThingContent.COLOURS.get(15));
    }

    @Test void preservesOriginalWordPoolIncludingRepeatedSourceWords() {
        assertTrue(DrawMyThingContent.WORDS.size() > 500);
        assertTrue(DrawMyThingContent.WORDS.contains("Mineplex"));
        assertTrue(Collections.frequency(DrawMyThingContent.WORDS, "Pizza") >= 2);
    }
}
