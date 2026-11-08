package au.com.testplex.arcadeports;

import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Mineplex-style Arcade combat messages and attributed kill mode labels. */
final class ArcadeCombatMessages {
    private ArcadeCombatMessages() { }

    /** Mineplex F.main format: blue module header, then gray message body. */
    static String sourceStyleChat(String module, String body) {
        return ChatColor.BLUE + module + "> " + ChatColor.GRAY + body;
    }

    /** Recreate the source CombatManager death output for Arcade games using detailed messages. */
    static void broadcastDeath(ArcadePortGame game, java.util.Collection<Player> participants, Player victim,
                               Player killer, String reason, String killMode) {
        if (game == ArcadePortGame.EVOLUTION || game == ArcadePortGame.SUPER_SPLOOR) {
            // These source games select DeathMessageType.Simple: only the victim and killer receive it.
            if (killer != null && killer.isOnline() && !killer.getUniqueId().equals(victim.getUniqueId())) {
                killer.sendMessage(sourceStyleChat("Combat", "You killed " + ChatColor.YELLOW
                        + victim.getName() + ChatColor.GRAY + " with " + ChatColor.AQUA + killMode + ChatColor.GRAY + "."));
                victim.sendMessage(sourceStyleChat("Combat", ChatColor.YELLOW + killer.getName()
                        + ChatColor.GRAY + " killed you with " + ChatColor.AQUA + killMode + ChatColor.GRAY + "."));
            } else {
                String cause = sourceDeathCause(reason);
                String body = cause.equals("has died") ? "You have died."
                        : cause.startsWith("was ") ? "You " + cause + "."
                        : "You " + cause + ".";
                victim.sendMessage(sourceStyleChat("Combat", body));
            }
            return;
        }
        String body;
        if (game == ArcadePortGame.DRAGON_ESCAPE && killer != null && killer.isOnline()
                && !killer.getUniqueId().equals(victim.getUniqueId())) {
            body = ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " was killed by "
                    + ChatColor.YELLOW + killer.getName() + ChatColor.GRAY + " with "
                    + ChatColor.AQUA + killMode + ChatColor.GRAY + ".";
        } else if (game == ArcadePortGame.DRAGON_ESCAPE) {
            String normalized = reason == null ? "" : reason.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("dragon")) {
                body = victim.getName() + " was caught by Douglas the Dragon!";
            } else if (normalized.contains("fell") || normalized.contains("fall")) {
                body = victim.getName() + " fell from the Skylands course!";
            } else {
                body = victim.getName() + " couldn't escape the dragon!";
            }
        } else if (killer != null && killer.isOnline() && !killer.getUniqueId().equals(victim.getUniqueId())) {
            body = ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " was killed by "
                    + ChatColor.YELLOW + killer.getName() + ChatColor.GRAY + " with "
                    + ChatColor.AQUA + killMode + ChatColor.GRAY + ".";
        } else {
            body = ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " " + sourceDeathCause(reason) + ".";
        }
        String message = sourceStyleChat("Combat", body);
        for (Player participant : participants) {
            if (participant != null && participant.isOnline()) participant.sendMessage(message);
        }
    }

    static String baconKillMode(String source) {
        if (source == null) return "Iron Axe";
        return switch (source) {
            case "body-slam" -> "Body Slam";
            case "bacon-blast" -> "Bacon Blast";
            case "crispy-bacon" -> "Crispy Fire";
            case "melee", "projectile hit" -> "Iron Axe";
            default -> titleCase(source);
        };
    }

    static String sourceKillModeFromDamager(Entity damager, Player attacker) {
        if (damager instanceof Projectile projectile) {
            if (projectile instanceof Arrow) return "Bow";
            if (projectile instanceof Snowball) return "Snowball";
            if (projectile instanceof Egg) return "Egg";
            if (projectile instanceof Fireball) return "Fireball";
            if (projectile instanceof EnderPearl) return "Ender Pearl";
            return titleCase(projectile.getType().name());
        }
        if (damager instanceof TNTPrimed) return "TNT";
        if (attacker != null) {
            ItemStack held = attacker.getInventory().getItemInMainHand();
            if (held != null && !held.getType().isAir()) {
                ItemMeta meta = held.getItemMeta();
                String displayName = meta == null ? null : meta.getDisplayName();
                return displayName == null || displayName.isBlank()
                        ? titleCase(held.getType().name()) : ChatColor.stripColor(displayName);
            }
        }
        return damager == null ? "Melee" : titleCase(damager.getType().name());
    }

    static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Melee";
        StringBuilder result = new StringBuilder();
        for (String word : value.replace('-', ' ').replace('_', ' ').trim().split("\\s+")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? "Melee" : result.toString();
    }

    static String sourceDeathCause(String reason) {
        if (reason == null || reason.isBlank()) return "has died";
        String normalized = reason.toLowerCase(java.util.Locale.ROOT).trim();
        if (normalized.contains("fell") || normalized.contains("fall")) return "fell to their death";
        if (normalized.contains("void")) return "fell into the void";
        if (normalized.contains("starv")) return "starved to death";
        if (normalized.contains("burn")) return "burned to death";
        if (normalized.startsWith("was ")) return normalized;
        return "was killed by " + normalized;
    }
}
