package au.com.testplex.arcadeports;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Sheep Quest's carried-sheep hotbar, XP indicator and slowness scaling. */
final class SheepQuestManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.SHEEP_QUEST; }

    static String penMarker(int team) {
        return switch (team) {
            case 0 -> "RED";
            case 1 -> "BLUE";
            default -> ArcadePortRules.sheepQuestTeamPenMarker(team);
        };
    }

    static int penCenterX(int team) { return team <= 1 ? -14 : 14; }
    static int penCenterZ(int team) { return team == 0 || team == 2 ? -14 : 14; }

    static DyeColor teamDyeColor(int team) {
        return switch (team) {
            case 0 -> DyeColor.RED;
            case 1 -> DyeColor.LIGHT_BLUE;
            case 2 -> DyeColor.LIME;
            default -> DyeColor.YELLOW;
        };
    }

    static ChatColor teamChatColor(int team) {
        return switch (team) {
            case 0 -> ChatColor.RED;
            case 1 -> ChatColor.AQUA;
            case 2 -> ChatColor.GREEN;
            default -> ChatColor.YELLOW;
        };
    }

    static Material teamWool(int team) {
        return switch (team) {
            case 0 -> Material.RED_WOOL;
            case 1 -> Material.LIGHT_BLUE_WOOL;
            case 2 -> Material.LIME_WOOL;
            default -> Material.YELLOW_WOOL;
        };
    }

    static String teamName(int team) {
        return switch (team) {
            case 0 -> "Red";
            case 1 -> "Aqua";
            case 2 -> "Green";
            default -> "Yellow";
        };
    }

    static Location penCenter(World world, List<Location> pen, int team) {
        if (!pen.isEmpty()) {
            double x = 0.0D, y = 0.0D, z = 0.0D;
            for (Location block : pen) {
                x += block.getBlockX() + 0.5D;
                y += block.getBlockY();
                z += block.getBlockZ() + 0.5D;
            }
            return new Location(world, x / pen.size(), y / pen.size(), z / pen.size());
        }
        return new Location(world, penCenterX(team) + 0.5D, 64.0D, penCenterZ(team) + 0.5D);
    }

    static boolean insidePen(World world, List<Location> pen, Location location, int team) {
        if (world == null || location == null || team < 0 || team > 3
                || location.getWorld() != world) return false;
        if (!pen.isEmpty()) {
            Block sheepBlock = location.getBlock();
            for (Location marker : pen) {
                if (ArcadePortRules.sheepQuestInsidePenMarker(sheepBlock.getX(), sheepBlock.getY(),
                        sheepBlock.getZ(), marker.getBlockX(), marker.getBlockY(), marker.getBlockZ())) return true;
            }
            return false;
        }
        Location center = penCenter(world, pen, team);
        return Math.abs(location.getX() - center.getX()) <= 2.5D
                && Math.abs(location.getY() - center.getY()) <= 0.75D
                && Math.abs(location.getZ() - center.getZ()) <= 2.5D;
    }

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
