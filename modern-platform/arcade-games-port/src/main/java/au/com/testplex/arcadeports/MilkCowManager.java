package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Milk the Cow's score, comeback, near-win and undisputed-leader bookkeeping. */
final class MilkCowManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.MILK_THE_COW; }

    /** Keep the authored map animals associated with the shared session cleanup roster. */
    void spawnMapAnimals(SessionState session, List<Location> locations, EntityType type) {
        if (session == null || locations == null || locations.isEmpty()) return;
        for (Location source : locations) {
            Location at = source.clone();
            at.setWorld(session.world);
            Entity animal = session.world.spawnEntity(at, type);
            if (animal instanceof Ageable ageable) {
                ageable.setBaby();
                ageable.setAgeLock(true);
            } else {
                animal.setCustomName("Bob");
                animal.setCustomNameVisible(true);
            }
            session.entities.add(animal);
        }
    }

    /** Cow hunger regeneration/sprint drain; called from the existing shared match tick. */
    void updateCowHunger(SessionState session, Player cowPlayer, long now) {
        if (session == null || cowPlayer == null) return;
        UUID id = cowPlayer.getUniqueId();
        long nextAt = session.milkChargeHungerAt.getOrDefault(id, 0L);
        if (now < nextAt) return;
        int food = cowPlayer.getFoodLevel();
        if (cowPlayer.isSprinting()) {
            if (food <= 0) cowPlayer.setSprinting(false);
            else cowPlayer.setFoodLevel(food - 1);
            cowPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                    MilkCowRules.cowSprintSpeedDurationTicks(), MilkCowRules.cowSprintSpeedAmplifier(),
                    false, false, false));
        } else if (food < 20) {
            cowPlayer.setFoodLevel(food + 1);
        }
        session.milkChargeHungerAt.put(id, now + MilkCowRules.cowChargeHungerIntervalMillis());
    }

    /** Herd spawn/follow/cleanup mechanics; no independent listener or scheduled task. */
    void updateHerd(SessionState session, Player cowPlayer, long now, boolean spawningEnabled) {
        if (session == null || cowPlayer == null || !cowPlayer.isOnline()
                || !session.alive.contains(cowPlayer.getUniqueId())
                || session.teams.getOrDefault(cowPlayer.getUniqueId(), 0) != 1
                || now < session.milkNextHerdUpdateAt) return;
        if (!spawningEnabled) {
            for (Cow cow : new ArrayList<>(session.milkCowHerd)) {
                if (cow != null && cow.isValid()) cow.remove();
                session.entities.remove(cow);
            }
            session.milkCowHerd.clear();
            return;
        }
        session.milkNextHerdUpdateAt = now + MilkCowRules.herdUpdateIntervalMillis();
        Location target = cowPlayer.getLocation();
        session.milkCowHerd.removeIf(cow -> cow == null || !cow.isValid());
        for (Cow cow : session.milkCowHerd) {
            Vector direction = target.toVector().subtract(cow.getLocation().toVector());
            direction.setY(Math.max(-0.08D, Math.min(0.12D, direction.getY())));
            if (direction.lengthSquared() > MilkCowRules.herdFollowMinDistanceSquared()) cow.setVelocity(direction.normalize().multiply(
                    GameDebugRuntimeOverrides.doubleValue("MILK_THE_COW_HERD_MOVE_SPEED", MilkCowRules.HERD_MOVE_SPEED)));
        }
        while (session.milkCowHerd.size() < GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_HERD_SIZE", MilkCowRules.HERD_SIZE)) {
            double spawnRadius = MilkCowRules.herdSpawnRadiusBlocks();
            Location spawn = target.clone().add(
                    ThreadLocalRandom.current().nextDouble(-spawnRadius, spawnRadius), 0.0D,
                    ThreadLocalRandom.current().nextDouble(-spawnRadius, spawnRadius));
            Cow cow = session.world.spawn(spawn, Cow.class);
            cow.setRemoveWhenFarAway(false);
            session.milkCowHerd.add(cow);
            session.entities.add(cow);
        }
    }

    /** Returns true when the shared match owner must announce and finish the match. */
    boolean incrementScore(SessionState session, Player player, int change, String reason) {
        UUID id = player.getUniqueId();
        int score = MilkCowRules.scoreAfterChange(session.scores.getOrDefault(id, 0), change);
        session.scores.put(id, score);
        int scoreToWin = GameDebugRuntimeOverrides.intValue(
                "MILK_THE_COW_SCORE_TO_WIN", MilkCowRules.SCORE_TO_WIN);
        if (score != scoreToWin - 1 && !session.milkSpiltMilkCandidates.contains(id)) {
            session.milkNearWinBucketEmptyings.remove(id);
        }
        int bestOpponentScore = session.scores.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(id))
                .mapToInt(Map.Entry::getValue).max().orElse(0);
        if (bestOpponentScore - score >= 5) session.milkComebackCandidates.add(id);
        UUID nextLeader = uniqueLeader(session.scores);
        if (nextLeader != null && !nextLeader.equals(session.milkCurrentLeader)) {
            session.milkTimesLed.merge(nextLeader, 1, Integer::sum);
        }
        session.milkCurrentLeader = nextLeader;
        player.sendMessage((change >= 0 ? ChatColor.GREEN : ChatColor.RED)
                + reason + ": " + score + "/" + GameDebugRuntimeOverrides.intValue(
                        "MILK_THE_COW_SCORE_TO_WIN", MilkCowRules.SCORE_TO_WIN));
        return MilkCowRules.hasWon(score);
    }

    static UUID uniqueLeader(Map<UUID, Integer> scores) {
        int currentLeaderScore = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<UUID> currentLeaders = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == currentLeaderScore)
                .map(Map.Entry::getKey).toList();
        return currentLeaders.size() == 1 ? currentLeaders.get(0) : null;
    }
}
