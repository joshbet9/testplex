package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurfFortifierTemplateTest {
    @Test
    void buildsTheRequestedPanelAndFortShapes() {
        assertEquals(4, TurfFortifierTemplate.PANEL_2X2.offsets(99).size());
        assertEquals(9, TurfFortifierTemplate.PANEL_3X3.offsets(99).size());
        assertEquals(18, TurfFortifierTemplate.FORT.offsets(99).size());
        assertEquals(2, TurfFortifierTemplate.PANEL_2X2.offsets(99).stream()
                .filter(offset -> offset.up() == 0).count());
        assertEquals(2, TurfFortifierTemplate.PANEL_2X2.offsets(99).stream()
                .filter(offset -> offset.up() == 1).count());
        assertEquals(2, TurfFortifierTemplate.PANEL_3X3.offsets(99).stream()
                .filter(offset -> offset.up() == 2).count());
        assertEquals(4, TurfFortifierTemplate.FORT.offsets(99).stream()
                .filter(offset -> offset.lateral() == -2).count());
        assertEquals(3, TurfFortifierTemplate.FORT.offsets(99).stream()
                .filter(offset -> offset.lateral() == -1).count());
        assertEquals(4, TurfFortifierTemplate.FORT.offsets(99).stream()
                .filter(offset -> offset.lateral() == 0).count());
        assertEquals(3, TurfFortifierTemplate.FORT.offsets(99).stream()
                .filter(offset -> offset.lateral() == 1).count());
        assertEquals(4, TurfFortifierTemplate.FORT.offsets(99).stream()
                .filter(offset -> offset.lateral() == 2).count());
    }

    @Test
    void partialTemplatesNeverUseMoreBlocksThanThePlayerHas() {
        assertEquals(3, TurfFortifierTemplate.PANEL_2X2.offsets(3).size());
        assertEquals(5, TurfFortifierTemplate.PANEL_3X3.offsets(5).size());
        assertEquals(12, TurfFortifierTemplate.FORT.offsets(12).size());
        assertTrue(TurfFortifierTemplate.FORT.offsets(0).isEmpty());
    }

    @Test
    void cyclesThroughAllThreeTemplates() {
        assertEquals(TurfFortifierTemplate.PANEL_3X3, TurfFortifierTemplate.PANEL_2X2.next());
        assertEquals(TurfFortifierTemplate.FORT, TurfFortifierTemplate.PANEL_3X3.next());
        assertEquals(TurfFortifierTemplate.PANEL_2X2, TurfFortifierTemplate.FORT.next());
    }
}
