package au.com.testplex.arcadeports;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Evolution's kit cooldowns, pity-clock and assist attribution. */
final class EvolutionManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.EVOLUTION; }

    long kitCooldown(SessionState session, UUID playerId, long baseMillis) {
        return "darwinist".equals(session.evolutionKits.getOrDefault(playerId, "darwinist"))
                ? EvolutionAchievementRules.darwinistCooldown(baseMillis) : baseMillis;
    }

    void reducePity(SessionState session, UUID playerId, long reductionMillis, long now, boolean killBased) {
        boolean neanderthal = "neanderthal".equals(session.evolutionKits.get(playerId));
        if (neanderthal) reductionMillis = EvolutionAchievementRules.neanderthalPityReduction(reductionMillis);
        if (neanderthal && killBased) {
            session.evolutionPityKillReductionsThisCycle.merge(playerId, 1, Integer::sum);
        }
        long deadline = session.evolutionPityUntil.getOrDefault(playerId,
                now + GameDebugRuntimeOverrides.longValue("EVOLUTION_PITY_CLOCK_MILLIS", ArcadePortRules.EVOLUTION_PITY_CLOCK_MILLIS));
        long reduced = ArcadePortRules.reduceEvolutionPityDeadline(deadline, now, reductionMillis);
        session.evolutionPityUntil.put(playerId, reduced);
        Player player = session.players.get(playerId);
        if (reduced <= now) {
            boolean newlyAvailable = session.evolutionAvailable.add(playerId);
            session.evolutionPityReady.add(playerId);
            if (newlyAvailable && player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GOLD + ("neanderthal".equals(session.evolutionKits.get(playerId))
                        ? "Neanderthal pity evolution ready!" : "Pity evolution ready! Hold sneak while grounded to evolve."));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.25F);
            }
        } else if (player != null && player.isOnline() && !session.evolutionAvailable.contains(playerId)) {
            player.sendActionBar(ChatColor.GOLD + "Pity evolution "
                    + ArcadePortRules.formatEvolutionPityRemaining(reduced - now)
                    + ChatColor.GRAY + " | kill/assist bonus");
        }
    }

    void recordDamage(SessionState session, Player victim, Player attacker, long now) {
        if (session == null || session.type != ArcadePortGame.EVOLUTION || victim == null || attacker == null) return;
        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();
        if (victimId.equals(attackerId) || !session.players.containsKey(attackerId)) return;
        session.evolutionDamageContributors.computeIfAbsent(victimId, ignored -> new HashMap<>())
                .put(attackerId, now);
    }

    void recordAssists(SessionState session, UUID victimId, UUID killerId, long now) {
        Map<UUID, Long> contributors = session.evolutionDamageContributors.remove(victimId);
        if (contributors == null) return;
        for (Map.Entry<UUID, Long> contribution : contributors.entrySet()) {
            UUID contributorId = contribution.getKey();
            if (contributorId == null || contributorId.equals(victimId) || contributorId.equals(killerId)
                    || now - contribution.getValue() > GameDebugRuntimeOverrides.longValue("EVOLUTION_ASSIST_WINDOW_MILLIS", ArcadePortRules.EVOLUTION_ASSIST_WINDOW_MILLIS)
                    || !session.players.containsKey(contributorId)) continue;
            reducePity(session, contributorId,
                    GameDebugRuntimeOverrides.longValue("EVOLUTION_PITY_ASSIST_REDUCTION_MILLIS", ArcadePortRules.EVOLUTION_PITY_ASSIST_REDUCTION_MILLIS), now, false);
            session.evolutionAssistCreditsSinceEvolution.merge(contributorId, 1, Integer::sum);
            Player contributor = session.players.get(contributorId);
            if (contributor != null && contributor.isOnline()) {
                contributor.sendMessage(ChatColor.GREEN + "Evolution assist! Your pity timer was reduced by 7.5 seconds.");
            }
        }
    }
}
