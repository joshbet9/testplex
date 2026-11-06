package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks a Body Slam used to recover mid-fall, through the first landing or knockout.
 * A successful opposing hit must occur after the Slam but before that landing for
 * the attacker to earn Got em' when the player is knocked out of the arena.
 */
final class BaconSlamRecoveryTracker {
    private final Set<UUID> airborneRecoveries = new HashSet<>();
    private final Map<UUID, UUID> interruptedBy = new HashMap<>();

    void bodySlam(UUID playerId, boolean airborne) {
        clear(playerId);
        if (playerId != null && airborne) airborneRecoveries.add(playerId);
    }

    void successfulHit(UUID victimId, UUID attackerId, boolean victimAirborne) {
        if (!victimAirborne) {
            landed(victimId);
        } else if (airborneRecoveries.contains(victimId) && attackerId != null
                && !attackerId.equals(victimId)) {
            interruptedBy.put(victimId, attackerId);
        }
    }

    void landed(UUID playerId) {
        clear(playerId);
    }

    /** Returns the interrupter, if the recovery ended in an off-arena knockout. */
    UUID knockout(UUID victimId, boolean offArenaKnockout, boolean stillAirborne) {
        UUID attacker = offArenaKnockout && stillAirborne && airborneRecoveries.contains(victimId)
                ? interruptedBy.get(victimId) : null;
        clear(victimId);
        return attacker;
    }

    void clear(UUID playerId) {
        if (playerId == null) return;
        airborneRecoveries.remove(playerId);
        interruptedBy.remove(playerId);
    }

    void clearAll() {
        airborneRecoveries.clear();
        interruptedBy.clear();
    }
}
