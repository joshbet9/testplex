package au.com.testplex.arcadeports;

import java.util.UUID;

/** Resolves Bacon Brawl's fall kill credit before the source-style death message is sent. */
final class BaconBrawlKillCreditRules {
    private BaconBrawlKillCreditRules() { }

    static UUID resolveForDeathMessage(UUID suppliedKillerId, boolean suppliedKillerIsParticipant,
            UUID victimId, UUID lastDamagerId, boolean lastDamagerIsParticipant,
            long nowMillis, long lastHitAtMillis, boolean arenaKnockout) {
        if (suppliedKillerId != null && !suppliedKillerId.equals(victimId) && suppliedKillerIsParticipant) {
            return suppliedKillerId;
        }
        if (lastDamagerId == null || lastDamagerId.equals(victimId) || !lastDamagerIsParticipant) return null;
        return ArcadePortRules.shouldCreditBaconFallKill(nowMillis, lastHitAtMillis, arenaKnockout)
                ? lastDamagerId : null;
    }
}
