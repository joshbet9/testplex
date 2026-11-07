package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

/** Captures one participant's lobby state for deterministic post-game restoration. */
final class PlayerSnapshot {
    final Location location;
    final ItemStack[] inventory;
    final ItemStack[] armor;
    final ItemStack offhand;
    final GameMode mode;
    final List<PotionEffect> potionEffects;
    final float exp;
    final int level;
    final boolean allowFlight;
    final boolean flying;
    final double health;
    final double maxHealthBase;
    final double absorption;
    final double attackSpeedBase;
    final int food;
    final float saturation;
    final float walkSpeed;
    final float flySpeed;
    final boolean sprinting;
    final int fireTicks;
    final Location compassTarget;
    final boolean collidable;
    final boolean invulnerable;
    final Scoreboard scoreboard;
    final String playerListName;
    PlayerSnapshot(Player p) {
        location = p.getLocation().clone();
        inventory = cloneItems(p.getInventory().getContents());
        armor = cloneItems(p.getInventory().getArmorContents());
        ItemStack off = p.getInventory().getItemInOffHand();
        offhand = off == null ? null : off.clone();
        mode = p.getGameMode();
        potionEffects = new ArrayList<>(p.getActivePotionEffects());
        exp = p.getExp();
        level = p.getLevel();
        allowFlight = p.getAllowFlight();
        flying = p.isFlying();
        health = p.getHealth();
        absorption = p.getAbsorptionAmount();
        var attackSpeedAttribute = p.getAttribute(Attribute.ATTACK_SPEED);
        attackSpeedBase = attackSpeedAttribute == null ? 4.0D : attackSpeedAttribute.getBaseValue();
        var maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
        maxHealthBase = maxHealthAttribute == null ? p.getMaxHealth() : maxHealthAttribute.getBaseValue();
        food = p.getFoodLevel();
        saturation = p.getSaturation();
        walkSpeed = p.getWalkSpeed();
        flySpeed = p.getFlySpeed();
        sprinting = p.isSprinting();
        fireTicks = p.getFireTicks();
        Location currentCompassTarget = p.getCompassTarget();
        compassTarget = currentCompassTarget == null ? null : currentCompassTarget.clone();
        collidable = p.isCollidable();
        invulnerable = readMarkedInvulnerability(p);
        scoreboard = p.getScoreboard();
        playerListName = p.getPlayerListName();
    }
    void restore(Player p) {
        if (p == null || !p.isOnline()) return;
        p.leaveVehicle();
        if (location != null && location.getWorld() != null) p.teleport(location);
        p.getInventory().clear();
        p.getInventory().setContents(cloneItems(inventory));
        p.getInventory().setArmorContents(cloneItems(armor));
        p.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
        p.setGameMode(mode);
        for (PotionEffect effect : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(effect.getType());
        for (PotionEffect effect : potionEffects) p.addPotionEffect(effect);
        p.setExp(exp);
        p.setLevel(level);
        p.setFoodLevel(food);
        p.setSaturation(saturation);
        p.setWalkSpeed(walkSpeed);
        p.setFlySpeed(flySpeed);
        p.setSprinting(sprinting);
        p.setFireTicks(fireTicks);
        var attackSpeedAttribute = p.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeedAttribute != null) attackSpeedAttribute.setBaseValue(attackSpeedBase);
        p.setCompassTarget(compassTarget == null ? null : compassTarget.clone());
        p.setCollidable(collidable);
        p.setInvulnerable(invulnerable);
        p.setScoreboard(scoreboard);
        p.setPlayerListName(playerListName);
        p.setAllowFlight(allowFlight);
        var maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) maxHealthAttribute.setBaseValue(maxHealthBase);
        double maxHealth = p.getMaxHealth();
        p.setHealth(Math.max(1.0D, Math.min(health, maxHealth)));
        p.setAbsorptionAmount(absorption);
        if (allowFlight) p.setFlying(flying);
    }
    private static ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i=0;i<items.length;i++) copy[i] = items[i] == null ? null : items[i].clone();
        return copy;
    }
    private static boolean readMarkedInvulnerability(Player player) {
        try {
            Object marked = player.getClass().getMethod("isMarkedInvulnerable").invoke(player);
            if (marked instanceof Boolean flag) return flag;
        } catch (ReflectiveOperationException | SecurityException ignored) { }
        // Paper 26.3 adds the marked-state getter; keep the older API fallback
        // for compatibility with the project's shared Bukkit compile surface.
        return player.isInvulnerable();
    }
}
