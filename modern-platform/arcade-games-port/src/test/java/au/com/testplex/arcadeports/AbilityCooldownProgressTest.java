package au.com.testplex.arcadeports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbilityCooldownProgressTest {
    @Test
    void displaysTheCooldownThatWillFinishSoonestThenContinuesTheLongerOne() {
        var brick = new AbilityCooldownProgress.Cooldown("builder:brick", "Brick", 1_000L, 4_000L);
        var leap = new AbilityCooldownProgress.Cooldown("builder:leap", "Leap", 1_000L, 7_000L);

        assertEquals(brick, AbilityCooldownProgress.nextCooldown(List.of(leap, brick), 1_000L));

        var displayedAfterBrickRecharge = AbilityCooldownProgress.nextCooldown(List.of(leap), 5_000L);
        assertEquals(leap, displayedAfterBrickRecharge);
        assertEquals(3_000L,
                displayedAfterBrickRecharge.startedAt() + displayedAfterBrickRecharge.durationMillis() - 5_000L);
    }

    @Test
    void expiredCooldownsDoNotHideLaterActiveCooldowns() {
        var expired = new AbilityCooldownProgress.Cooldown("brick", "Brick", 1_000L, 4_000L);
        var active = new AbilityCooldownProgress.Cooldown("leap", "Leap", 1_000L, 7_000L);

        assertEquals(active, AbilityCooldownProgress.nextCooldown(List.of(expired, active), 5_000L));
        assertNull(AbilityCooldownProgress.nextCooldown(List.of(expired), 5_000L));
    }

    @Test
    void equalEndTimesAreStableForTheSameInputOrder() {
        var first = new AbilityCooldownProgress.Cooldown("first", "First", 1_000L, 5_000L);
        var second = new AbilityCooldownProgress.Cooldown("second", "Second", 2_000L, 4_000L);

        assertEquals(first, AbilityCooldownProgress.nextCooldown(List.of(first, second), 2_000L));
    }
}
