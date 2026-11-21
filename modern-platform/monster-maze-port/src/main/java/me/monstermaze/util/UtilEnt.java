package me.monstermaze.util;

import me.monstermaze.entity.MonsterEntityController;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.WeakHashMap;

/** Paper 1.21 entity utilities used by Monster Maze. */
public final class UtilEnt {
    private UtilEnt() {}

    /** Keep one native navigation target per live mob so a per-tick update does not restart its path. */
    private static final Map<Mob, Location> NAVIGATION_TARGETS = new WeakHashMap<Mob, Location>();

    /**
     * Entity spawning sends the client spawn packet before spawnEntity returns, so the normal
     * UUID->skin map is populated one instruction too late for that first packet. The spawn
     * packet listener reads this main-thread handoff while the spawn call is in progress.
     */
    private static final ThreadLocal<String> PENDING_SPAWN_SKIN = new ThreadLocal<String>();

    public static String getPendingSpawnSkin() {
        return PENDING_SPAWN_SKIN.get();
    }

    public static boolean CreatureMoveFast(Entity ent, Location target, float speed) {
        return CreatureMoveFast(ent, target, speed, true);
    }

    public static boolean CreatureMoveFast(Entity ent, Location target, float speed, boolean slow) {
        if (!(ent instanceof Mob) || target == null || ent.getWorld() != target.getWorld()) return false;
        Mob mob = (Mob) ent;
        double distSq = offsetSquared(ent.getLocation(), target);
        if (distSq < 0.01D) {
            stopNavigation(mob);
            return false;
        }
        if (distSq < 4.0D) speed = Math.min(speed, 1.0F);

        Location currentTarget = NAVIGATION_TARGETS.get(mob);
        if (currentTarget != null && currentTarget.getWorld() == target.getWorld()
                && currentTarget.distanceSquared(target) < 0.01D && mob.getPathfinder().hasPath()) {
            return true;
        }

        // Use Paper's native mob navigator instead of teleporting a fixed distance each update.
        // This gives the entity real movement, collision and vertical path handling like the source
        // MoveControl call, while keeping the maze controller's waypoint selection authoritative.
        boolean accepted = mob.getPathfinder().moveTo(target, speed);
        if (accepted) NAVIGATION_TARGETS.put(mob, target.clone());
        else NAVIGATION_TARGETS.remove(mob);
        return accepted;
    }

    public static double offsetSquared(Location a, Location b) {
        if (a == null || b == null || a.getWorld() != b.getWorld()) return Double.MAX_VALUE;
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean isGrounded(Entity ent) {
        if (ent == null) return false;
        if (ent.isOnGround()) return true;
        try {
            Location loc = ent.getLocation();
            return loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getType().isSolid();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void vegetate(Entity ent) {
        if (ent == null) return;
        if (ent instanceof Creature) {
            try { ((Creature) ent).setTarget(null); } catch (Throwable ignored) { }
        }
        if (ent instanceof Mob) {
            Mob mob = (Mob) ent;
            try { Bukkit.getMobGoals().removeAllGoals(mob); }
            catch (Throwable t) { Bukkit.getLogger().warning("[MonsterMaze] Failed to remove vanilla mob goals: " + t); }
        }
    }

    public static void stopNavigation(Entity ent) {
        if (!(ent instanceof Mob)) return;
        Mob mob = (Mob) ent;
        NAVIGATION_TARGETS.remove(mob);
        try { mob.getPathfinder().stopPathfinding(); }
        catch (Throwable t) { Bukkit.getLogger().warning("[MonsterMaze] stopNavigation failed: " + t); }
    }

    /** Every Monster Maze mob is physically a Snow Golem; mobType is client-facing only. */
    public static LivingEntity spawnGhostMob(Location loc, String mobType) {
        if (loc == null || loc.getWorld() == null) return null;
        String skin = normalizeSkin(mobType);
        PENDING_SPAWN_SKIN.set(skin);
        try {
            Entity entity = loc.getWorld().spawnEntity(loc, EntityType.SNOW_GOLEM, CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (!(entity instanceof LivingEntity)) {
                entity.remove();
                return null;
            }
            LivingEntity living = (LivingEntity) entity;
            MonsterEntityController.setSkin(living, skin);
            vegetate(living);
            MonsterEntityController.configure(living);
            return living;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[MonsterMaze] spawnGhostMob '" + mobType + "' failed: " + t);
            return null;
        } finally {
            PENDING_SPAWN_SKIN.remove();
        }
    }

    private static String normalizeSkin(String mobType) {
        String id = mobType == null ? "" : mobType.trim().toLowerCase(java.util.Locale.ROOT);
        if (id.isEmpty() || "snow_golem".equals(id)) return "snowman";
        if ("pig_zombie".equals(id) || "zombie_pigman".equals(id)) return "zombified_piglin";
        if ("eye_of_ender".equals(id)) return "eyeofender";
        return id;
    }

    public static org.bukkit.entity.Snowman spawnGhostSnowman(Location loc) {
        LivingEntity ent = spawnGhostMob(loc, "snowman");
        return ent instanceof org.bukkit.entity.Snowman ? (org.bukkit.entity.Snowman) ent : null;
    }
}