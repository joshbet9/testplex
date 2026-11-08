package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Sheep Quest's carried-sheep hotbar, XP indicator and slowness scaling. */
final class SheepQuestManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.SHEEP_QUEST; }

    void updateCarryUi(SessionState session, Player player, boolean shepherdKitAllowed) {
        if (session == null || player == null || !player.isOnline()) return;
        List<Sheep> carried = session.sheepCarriers.entrySet().stream()
                .filter(entry -> player.getUniqueId().equals(entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        boolean shepherd = "shepherd".equals(session.sheepQuestKits.get(player.getUniqueId()))
                && shepherdKitAllowed;
        int carryLimit = Math.max(1, Math.min(64, GameDebugRuntimeOverrides.intValue(
                "SHEEP_QUEST_MAX_CARRY", ArcadePortRules.SHEEP_QUEST_MAX_CARRY)));
        int count = shepherd ? carried.size() : Math.min(carryLimit, carried.size());
        if (shepherd) {
            player.setLevel(count);
            player.setExp(0.0F);
            session.sheepQuestMaxCarried.merge(player.getUniqueId(), count, Math::max);
        } else {
            player.setLevel(0);
            player.setExp(count / (float) carryLimit);
        }
        int visibleCount = Math.min(count, 4);
        for (int index = 0; index < 4; index++) {
            player.getInventory().setItem(4 + index,
                    index < visibleCount ? woolIcon(carried.get(index)) : null);
        }
        if (count > 0) {
            int amplifier = shepherd
                    ? ArcadePortRules.sheepQuestShepherdSlownessAmplifier(count)
                    : ArcadePortRules.sheepQuestCarrySlownessAmplifier(count);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    GameDebugRuntimeOverrides.intValue("SHEEP_QUEST_CARRY_SLOWNESS_TICKS", ArcadePortRules.SHEEP_QUEST_CARRY_SLOWNESS_TICKS),
                    Math.max(0, amplifier), false, false, true));
        } else {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    static ItemStack woolIcon(Sheep sheep) {
        Material wool = Material.matchMaterial((sheep == null ? "WHITE" : sheep.getColor().name()) + "_WOOL");
        return new ItemStack(wool == null ? Material.WHITE_WOOL : wool);
    }
}
