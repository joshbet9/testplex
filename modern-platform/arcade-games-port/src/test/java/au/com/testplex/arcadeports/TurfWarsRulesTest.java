package au.com.testplex.arcadeports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class TurfWarsRulesTest {
    @Test
    void infiltratorHotbarPutsSwordBeforeBowAndOtherKitsKeepBowFirst() {
        assertEquals(List.of(Material.IRON_SWORD, Material.BOW),
                TurfWarsRules.startingHotbar("infiltrator"));
        assertEquals(List.of(Material.BOW), TurfWarsRules.startingHotbar("marksman"));
        assertEquals(List.of(Material.BOW), TurfWarsRules.startingHotbar("shredder"));
    }

    @Test
    void meleeHitAddsLegacyImpulseAndPreservesExistingUpwardMomentum() {
        assertEquals(new Vector(0.4D, 0.36D, 0.0D),
                TurfWarsRules.meleeKnockback(new Vector(0.0D, 0.1D, 0.0D),
                        new Vector(3.0D, 0.0D, 0.0D)));
        assertEquals(0.75D,
                TurfWarsRules.meleeKnockback(new Vector(0.0D, 1.5D, 0.0D),
                        new Vector(1.0D, 0.0D, 0.0D)).getY(), 0.00001D);
        org.junit.jupiter.api.Assertions.assertNull(TurfWarsRules.meleeKnockback(
                new Vector(), new Vector()));
    }
}
