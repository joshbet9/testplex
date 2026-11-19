package me.monstermaze.entity;

import me.monstermaze.MonsterMazePlugin;
import me.monstermaze.game.GameManager;
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitTask;

/** Keeps Monster Maze's Snow Golem-backed entities from leaking vanilla gameplay into the game. */
public final class MonsterEntityListener implements Listener {
    private final GameManager game;
    private final BukkitTask tickTask;

    public MonsterEntityListener(MonsterMazePlugin plugin, GameManager game) {
        this.game = game;
        Bukkit.getPluginManager().registerEvents(this, plugin.getHostPlugin());
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                for (LivingEntity entity : game.getMonsterManager().getMonsters()) {
                    MonsterEntityController.tick(entity);
                }
            }
        }, 1L, 1L);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
    }

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (isMonster(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (isMonster(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!isMonster(event.getEntity())) return;
        switch (event.getCause()) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                event.setCancelled(true);
                break;
            case FIRE:
            case FIRE_TICK:
            case MELTING:
            case DROWNING:
            case DRYOUT:
                event.setCancelled(true);
                break;
            default:
                break;
        }
    }

    /**
     * Maze bump() already sets the exact source-game launch velocity. Since mob damage
     * was restored, Paper also produces its normal entity-hit knockback after that
     * custom velocity, changing the launch felt by players. Suppress only the extra
     * vanilla impulse for a live player struck by one of THIS Maze's managed mobs.
     * Never cancel the damage event: players must still lose two hearts per bump.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobKnockback(EntityKnockbackByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isMazeContactKnockback(game.isLive(),
                game.getAlivePlayers().contains(player),
                game.getMonsterManager().isManagedMonster(event.getHitBy()))) return;
        event.setCancelled(true);
    }

    /** Deliberately narrow predicate: never alter arbitrary player or global knockback. */
    static boolean isMazeContactKnockback(boolean mazeLive, boolean activeParticipant,
            boolean managedMazeMonsterSource) {
        return mazeLive && activeParticipant && managedMazeMonsterSource;
    }

    private boolean isMonster(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        for (LivingEntity monster : game.getMonsterManager().getMonsters()) {
            if (monster == entity) return true;
        }
        return false;
    }
}
