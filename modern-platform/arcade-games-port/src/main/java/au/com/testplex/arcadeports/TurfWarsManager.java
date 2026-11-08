package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/** Turf Wars' terrain-material and Shredder ammunition handling. */
final class TurfWarsManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.TURF_WARS; }

    boolean isWool(Material material) {
        return material != null && material.name().endsWith("_WOOL");
    }

    boolean isTerracotta(Material material) {
        return material != null && material.name().endsWith("_TERRACOTTA");
    }

    int countFletchedArrows(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != Material.ARROW || !stack.hasItemMeta()) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName()
                    && ChatColor.stripColor(meta.getDisplayName()).contains("Fletched Arrow")) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    boolean isFletchedArrow(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ARROW || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && ChatColor.stripColor(meta.getDisplayName()).contains("Fletched Arrow");
    }

    double barrageSpread(double spread) {
        return spread <= 0.0D ? 0.0D : ThreadLocalRandom.current().nextDouble(-spread, spread);
    }

    void updateBarrages(SessionState session, long now, Predicate<Player> shredderEnabled) {
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (!"shredder".equals(session.turfWarsKits.getOrDefault(id, "marksman"))) continue;
            if (!shredderEnabled.test(player)) {
                session.turfBarrageCharges.remove(id);
                session.turfBarrageNextChargeAt.remove(id);
                session.turfBarrageFiring.remove(id);
                session.turfBarrageRemaining.remove(id);
                continue;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            boolean holdingBow = held != null && held.getType() == Material.BOW;

            if (session.turfBarrageCharges.containsKey(id) && !session.turfBarrageFiring.contains(id)) {
                if (!holdingBow) {
                    session.turfBarrageCharges.remove(id);
                    session.turfBarrageNextChargeAt.remove(id);
                } else if (session.turfBarrageCharges.getOrDefault(id, 0)
                        < GameDebugRuntimeOverrides.intValue("TURF_WARS_BARRAGE_MAX_CHARGE", ArcadePortRules.TURF_WARS_BARRAGE_MAX_CHARGE)
                        && now >= session.turfBarrageNextChargeAt.getOrDefault(id, Long.MAX_VALUE)) {
                    int charge = session.turfBarrageCharges.getOrDefault(id, 0) + 1;
                    session.turfBarrageCharges.put(id, charge);
                    session.turfBarrageNextChargeAt.put(id,
                            now + GameDebugRuntimeOverrides.longValue("TURF_WARS_BARRAGE_CHARGE_INTERVAL_MILLIS", ArcadePortRules.TURF_WARS_BARRAGE_CHARGE_INTERVAL_MILLIS));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT,
                            0.6F, 1.0F + charge * 0.1F);
                    player.sendActionBar(ChatColor.GOLD + "BARRAGE "
                            + ChatColor.YELLOW + charge + "/" + GameDebugRuntimeOverrides.intValue("TURF_WARS_BARRAGE_MAX_CHARGE", ArcadePortRules.TURF_WARS_BARRAGE_MAX_CHARGE));
                }
            }

            if (!session.turfBarrageFiring.contains(id)) continue;
            int remaining = session.turfBarrageRemaining.getOrDefault(id, 0);
            if (!holdingBow || remaining <= 0) {
                session.turfBarrageFiring.remove(id);
                session.turfBarrageRemaining.remove(id);
                session.turfBarrageCharges.remove(id);
                session.turfBarrageNextChargeAt.remove(id);
                continue;
            }

            double spread = ArcadePortRules.turfWarsBarrageArrowSpread();
            Vector jitter = new Vector(
                    barrageSpread(spread),
                    barrageSpread(spread),
                    barrageSpread(spread));
            Arrow arrow = player.launchProjectile(Arrow.class);
            arrow.setVelocity(player.getLocation().getDirection().add(jitter)
                    .multiply(ArcadePortRules.turfWarsBarrageArrowSpeed()));
            session.entities.add(arrow);
            session.turfTrackedArrows.add(arrow.getUniqueId());
            session.turfBarrageArrowOwners.put(arrow.getUniqueId(), id);
            session.turfBarrageRemaining.put(id, remaining - 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
        }
    }

    void startBarrage(SessionState session, Player player, boolean abilityAllowed) {
        if (!abilityAllowed) return;
        UUID id = player.getUniqueId();
        if (!"shredder".equals(session.turfWarsKits.getOrDefault(id, "marksman"))
                || session.turfBarrageCharges.containsKey(id)
                || session.turfBarrageFiring.contains(id)
                || countFletchedArrows(player) <= 0) return;
        session.turfBarrageTargetsByOwner.put(id, new HashSet<>());
        session.turfBarrageCharges.put(id, 0);
        session.turfBarrageNextChargeAt.put(id,
                System.currentTimeMillis() + GameDebugRuntimeOverrides.longValue("TURF_WARS_BARRAGE_FIRST_CHARGE_MILLIS", ArcadePortRules.TURF_WARS_BARRAGE_FIRST_CHARGE_MILLIS));
        player.sendMessage(ChatColor.YELLOW + "Charging Barrage. Release your bow to fire.");
    }

    private List<Player> alivePlayers(SessionState session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }
}
