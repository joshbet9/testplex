package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceDeathMessageRulesTest {
    @Test
    void dragonEscapeDistinguishesVoidFromCourseFalls() {
        assertEquals("fell into the void!", SourceDeathMessageRules.dragonEscapeCause("fell into the void"));
        assertEquals("fell into the void!", SourceDeathMessageRules.dragonEscapeCause("VOID"));
        assertEquals("fell from the Skylands course!", SourceDeathMessageRules.dragonEscapeCause("fell from course"));
    }

    @Test
    void dragonEscapeKeepsDragonAndUnknownCausesReadable() {
        assertEquals("was caught by Douglas the Dragon!", SourceDeathMessageRules.dragonEscapeCause("Douglas dragon"));
        assertEquals("couldn't escape the dragon!", SourceDeathMessageRules.dragonEscapeCause("hazard"));
    }

    @Test
    void genericVoidCauseTakesPriorityOverFallWording() {
        assertEquals("fell into the void", SourceDeathMessageRules.genericCause("fell into the void"));
        assertEquals("fell into the void", SourceDeathMessageRules.genericCause("VOID"));
        assertEquals("fell to their death", SourceDeathMessageRules.genericCause("fell from a ledge"));
    }
}
