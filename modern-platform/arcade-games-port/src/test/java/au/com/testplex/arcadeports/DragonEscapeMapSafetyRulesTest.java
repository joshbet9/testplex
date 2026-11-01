package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DragonEscapeMapSafetyRulesTest {
    @Test
    void lowAltitudeMapsUseTheirAuthoredMinimumY() {
        assertEquals(17.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Steve's Swamp", true, 18));
        assertEquals(0.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Low Course", true, 0));
    }

    @Test
    void legacyNamedLowAltitudeExceptionsRemainStable() {
        assertEquals(24.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Through Hell", true, 10));
        assertEquals(0.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Pirate Bay", false, 0));
    }

    @Test
    void metadataLessCoursesKeepTheGeneratedFallback() {
        assertEquals(58.0D, ArcadePortRules.dragonEscapeVoidThresholdY("Unknown Course", false, 0));
    }
}
