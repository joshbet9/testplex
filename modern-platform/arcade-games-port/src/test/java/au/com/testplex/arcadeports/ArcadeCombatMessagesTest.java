package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcadeCombatMessagesTest {
    @Test void combatLabelsRemainMineplexCompatibleAfterExtraction() {
        assertEquals("Body Slam", ArcadeCombatMessages.baconKillMode("body-slam"));
        assertEquals("Bacon Blast", ArcadeCombatMessages.baconKillMode("bacon-blast"));
        assertEquals("Iron Axe", ArcadeCombatMessages.baconKillMode("projectile hit"));
        assertEquals("Iron Axe", ArcadeCombatMessages.baconKillMode(null));
        assertEquals("Ender Pearl", ArcadeCombatMessages.titleCase("ender_pearl"));
        assertEquals("Melee", ArcadeCombatMessages.titleCase(" "));
    }

    @Test void deathCauseAndPrefixFormattingAreUnchanged() {
        assertEquals("has died", ArcadeCombatMessages.sourceDeathCause(null));
        assertEquals("fell to their death", ArcadeCombatMessages.sourceDeathCause("Fell"));
        assertEquals("fell into the void", ArcadeCombatMessages.sourceDeathCause("void"));
        assertEquals("starved to death", ArcadeCombatMessages.sourceDeathCause("starvation"));
        assertEquals("was killed by dragon", ArcadeCombatMessages.sourceDeathCause("Dragon"));
        assertEquals(ChatColor.BLUE + "Combat> " + ChatColor.GRAY + "message",
                ArcadeCombatMessages.sourceStyleChat("Combat", "message"));
    }
}
