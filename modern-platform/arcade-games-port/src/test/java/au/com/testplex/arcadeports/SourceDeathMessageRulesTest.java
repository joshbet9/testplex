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
    void dragonEscapeDisruptorMessageKeepsTheEnvironmentalCauseAfterTheAttackerPrefix() {
        assertEquals("Victim was disrupted by Builder and was caught by Douglas the Dragon!",
                SourceDeathMessageRules.dragonEscapeDisruptorMessage("Victim", "Builder", "caught by dragon"));
        assertEquals("Victim was disrupted by Builder and was lost to the water!",
                SourceDeathMessageRules.dragonEscapeDisruptorMessage("Victim", "Builder", "was lost to the water"));
        assertEquals("Victim was disrupted by Builder and was burned by lava!",
                SourceDeathMessageRules.dragonEscapeDisruptorMessage("Victim", "Builder", "was burned by lava"));
        assertEquals("Victim was disrupted by Builder and fell into the void!",
                SourceDeathMessageRules.dragonEscapeDisruptorMessage("Victim", "Builder", "fell into the void"));
        assertEquals("Victim was disrupted by Builder and fell from the Skylands course!",
                SourceDeathMessageRules.dragonEscapeDisruptorMessage("Victim", "Builder", "fell from course"));
    }

    @Test
    void genericVoidCauseTakesPriorityOverFallWording() {
        assertEquals("fell into the void", SourceDeathMessageRules.genericCause("fell into the void"));
        assertEquals("fell into the void", SourceDeathMessageRules.genericCause("VOID"));
        assertEquals("fell to their death", SourceDeathMessageRules.genericCause("fell from a ledge"));
    }
}
