package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DragonEscapePersonalBestViewsTest {
    @Test
    void cyclesOverallBestAndAllFiveSelectableKitsWithoutSkippingOne() {
        assertEquals(List.of("best-time", "jumper", "disruptor", "builder", "warper", "digger"),
                DragonEscapePersonalBestViews.all());
        int view = 0;
        for (String expected : DragonEscapePersonalBestViews.all()) {
            assertEquals(expected, DragonEscapePersonalBestViews.selected(view));
            view = DragonEscapePersonalBestViews.next(view);
        }
        assertEquals(0, view);
        assertEquals("Best Time", DragonEscapePersonalBestViews.name("best-time"));
        assertEquals("Builder", DragonEscapePersonalBestViews.name("builder"));
        assertEquals("Unknown (legacy record)", DragonEscapePersonalBestViews.name(null));
    }

    @Test
    void eachKitRecordCanImproveIndependentlyFromTheOverallRecord() {
        assertTrue(DragonEscapePersonalBestViews.improves(-1L, 18_000L));
        assertTrue(DragonEscapePersonalBestViews.improves(20_000L, 18_000L));
        assertFalse(DragonEscapePersonalBestViews.improves(15_000L, 18_000L));
        assertFalse(DragonEscapePersonalBestViews.improves(18_000L, 18_000L));
        assertFalse(DragonEscapePersonalBestViews.improves(-1L, 0L));
    }
}
