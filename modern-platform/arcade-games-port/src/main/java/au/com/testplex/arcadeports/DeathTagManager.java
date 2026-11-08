package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

/**
 * Death Tag's game-specific sidebar and tracking compass.
 * Shared match ownership, infection rules and event listeners remain in
 * {@link ArcadePortManager}; this controller uses the existing game cadence.
 */
final class DeathTagManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DEATH_TAG; }

    interface DamageActions {
        boolean isCurrent(SessionState session);
        void playerHit(SessionState session, Player attacker, Player target);
        void soloPracticeHit(SessionState session, Player target, Zombie chaser);
        void respawnChaser(SessionState session, Player target);
        void infectRunner(SessionState session, Player target, String reason);
        void checkRoundEnd(SessionState session);
        void damageFeedback(Player target);
    }

    enum DamageOutcome { IGNORE, DAMAGE, RESPAWN_CHASER, INFECT_RUNNER }

    static double environmentalDamage(boolean chaser, double incomingDamage, boolean alphaChaser, boolean fireTick) {
        return chaser
                ? ArcadePortRules.deathTagEnvironmentalDamageAfterIronSkin(incomingDamage, alphaChaser, fireTick)
                : incomingDamage;
    }

    static DamageOutcome environmentalDamageOutcome(boolean chaser, double health, double damage) {
        if (damage <= 0.0D) return DamageOutcome.IGNORE;
        if (!ArcadePortRules.deathTagHitIsLethal(health, damage)) return DamageOutcome.DAMAGE;
        return chaser ? DamageOutcome.RESPAWN_CHASER : DamageOutcome.INFECT_RUNNER;
    }

    /** Applies Death Tag's own damage semantics; the shared manager supplies only shared match actions. */
    boolean handleDamage(SessionState session, Player target, EntityDamageEvent event, double incomingDamage,
            DamageActions actions) {
        if (session == null || target == null || event == null || actions == null
                || !actions.isCurrent(session) || !session.alive.contains(target.getUniqueId())
                || incomingDamage <= 0.0D) return false;
        if (session.deathTagRespawnGraceUntil.getOrDefault(target.getUniqueId(), 0L)
                > System.currentTimeMillis()) return true;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player attacker) {
                actions.playerHit(session, attacker, target);
                return true;
            }
            if (damager instanceof Projectile projectile
                    && projectile.getShooter() instanceof Player shooter
                    && session.players.containsKey(shooter.getUniqueId())) {
                // ProjectileHitEvent resolves this once with the source Archer damage and knockback perks.
                session.deathTagProjectileDamage.put(projectile.getUniqueId(), incomingDamage);
                return true;
            }
            if (damager instanceof Zombie zombie && session.entities.contains(zombie)
                    && session.participantsAtStart <= 1) {
                actions.soloPracticeHit(session, target, zombie);
                return true;
            }
        }

        boolean chaser = session.tagged.contains(target.getUniqueId());
        double damage = environmentalDamage(chaser, incomingDamage,
                session.alphaChasers.contains(target.getUniqueId()),
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK);
        DamageOutcome outcome = environmentalDamageOutcome(chaser, target.getHealth(), damage);
        switch (outcome) {
            case IGNORE -> { return true; }
            case RESPAWN_CHASER -> actions.respawnChaser(session, target);
            case INFECT_RUNNER -> {
                String cause = event.getCause().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
                actions.infectRunner(session, target, "was killed by " + cause);
                actions.checkRoundEnd(session);
            }
            case DAMAGE -> {
                target.setHealth(target.getHealth() - damage);
                actions.damageFeedback(target);
            }
        }
        return true;
    }


    static boolean isRespawnHazard(Material material) {
        return material == Material.LAVA || material == Material.FIRE || material == Material.SOUL_FIRE
                || material == Material.MAGMA_BLOCK || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE || material == Material.CACTUS
                || material == Material.SWEET_BERRY_BUSH || material == Material.POWDER_SNOW
                || material == Material.WITHER_ROSE;
    }

    boolean isSafeRespawnSpawn(Location candidate, SourceMapCatalog.MapData mapData) {
        if (candidate == null || candidate.getWorld() == null) return false;
        double voidThresholdY = ArcadePortRules.sourceMapVoidThresholdY(
                mapData != null && mapData.hasVerticalBounds(), mapData == null ? 0 : mapData.minY());
        if (candidate.getY() < voidThresholdY) return false;
        Block feet = candidate.getWorld().getBlockAt(candidate.getBlockX(), candidate.getBlockY(), candidate.getBlockZ());
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        return feet.isPassable() && head.isPassable() && ground.getType().isSolid()
                && !feet.isLiquid() && !head.isLiquid() && !ground.isLiquid()
                && !isRespawnHazard(feet.getType())
                && !isRespawnHazard(head.getType())
                && !isRespawnHazard(ground.getType());
    }

    /**
     * Legacy spawn markers can be blocked or misaligned with a platform. Search
     * authored collision-free terrain only; never construct replacement platforms.
     */
    Location findSafeRespawnNearby(Location base, SourceMapCatalog.MapData mapData) {
        if (base == null || base.getWorld() == null) return null;
        World world = base.getWorld();
        int centreX = base.getBlockX();
        int centreY = base.getBlockY();
        int centreZ = base.getBlockZ();
        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int offset = 0; offset <= 8; offset++) {
                        for (int sign = 1; sign >= -1; sign -= 2) {
                            if (offset == 0 && sign == -1) continue;
                            int y = centreY + offset * sign;
                            if (y < world.getMinHeight() + 1 || y > world.getMaxHeight() - 2) continue;
                            Location candidate = new Location(world,
                                    centreX + dx + 0.5D, y, centreZ + dz + 0.5D,
                                    base.getYaw(), base.getPitch());
                            if (isSafeRespawnSpawn(candidate, mapData)) return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Select the safest distant authored spawn, then search existing terrain for a valid fallback. */
    Location selectChaserRespawn(SessionState session, Player player, SourceMapCatalog.MapData mapData,
            Supplier<Location> defaultSpawn) {
        if (session == null || player == null || session.world == null) return null;
        if (mapData != null && !mapData.spawnLocations().isEmpty()) {
            List<Player> activePlayers = alivePlayers(session).stream()
                    .filter(other -> !other.getUniqueId().equals(player.getUniqueId())).toList();
            List<Location> safeSpawns = mapData.spawnLocations().stream()
                    .filter(candidate -> isSafeRespawnSpawn(candidate, mapData)).toList();
            if (!safeSpawns.isEmpty()) {
                return safeSpawns.stream().max(java.util.Comparator.comparingDouble(candidate ->
                        activePlayers.isEmpty() ? Double.POSITIVE_INFINITY
                                : activePlayers.stream().mapToDouble(other ->
                                        candidate.distanceSquared(other.getLocation())).min()
                                        .orElse(Double.POSITIVE_INFINITY))).orElseThrow().clone();
            }
            // Resolve invalid legacy marker heights against actual source terrain.
            for (Location base : mapData.spawnLocations()) {
                Location nearby = findSafeRespawnNearby(base, mapData);
                if (nearby != null) return nearby;
            }
        }
        // Lazily resolve the shared lobby spawn only after authored spawns are exhausted.
        Location fallback = defaultSpawn == null ? null : defaultSpawn.get();
        if (isSafeRespawnSpawn(fallback, mapData)) return fallback;
        Location nearbyFallback = findSafeRespawnNearby(fallback, mapData);
        if (nearbyFallback != null) return nearbyFallback;
        Location worldSpawn = session.world.getSpawnLocation();
        if (isSafeRespawnSpawn(worldSpawn, mapData)) return worldSpawn;
        Location nearbyWorldSpawn = findSafeRespawnNearby(worldSpawn, mapData);
        if (nearbyWorldSpawn != null) return nearbyWorldSpawn;
        if (isSafeRespawnSpawn(player.getLocation(), mapData)) return player.getLocation().clone();
        return findSafeRespawnNearby(player.getLocation(), mapData);
    }

    @Override public void onPrepare(SessionState session, List<Player> participants) {
        setupDeathTagScoreboard(session);
    }

    void updateLights(SessionState session, long now) {
        if (session.deathTagOriginalLightBlocks.isEmpty() || now < session.deathTagNextLightAt) return;
        session.deathTagNextLightAt = now + 1_000L;
        for (Block block : session.deathTagOriginalLightBlocks.keySet()) {
            if (block == null || block.getWorld() != session.world) continue;
            block.setType(ArcadePortRules.deathTagNextLightMaterial(block.getType()), false);
        }
    }

    Location soloChaserSpawn(SessionState session, Player runner, SourceMapCatalog.MapData mapData) {
        Location runnerLocation = runner.getLocation();
        if (mapData != null) {
            Location otherSpawn = mapData.spawnLocations().stream()
                    .filter(spawn -> horizontalDistanceSquared(spawn, runnerLocation) >= 36.0D)
                    .max(java.util.Comparator.comparingDouble(spawn ->
                            horizontalDistanceSquared(spawn, runnerLocation)))
                    .orElse(null);
            if (otherSpawn != null) return otherSpawn.clone();
        }

        double direction = runnerLocation.getX() <= 0.0D ? 1.0D : -1.0D;
        double x = runnerLocation.getX() + direction * 8.0D;
        double z = runnerLocation.getZ();
        if (mapData != null && mapData.hasBounds()) {
            x = Math.max(mapData.minX() + 1.0D, Math.min(mapData.maxX() - 1.0D, x));
            z = Math.max(mapData.minZ() + 1.0D, Math.min(mapData.maxZ() - 1.0D, z));
        }
        return new Location(session.world, x, runnerLocation.getY(), z,
                runnerLocation.getYaw() + 180.0F, 0.0F);
    }

    void applyDefaultMeleeKnockback(Player attacker, Player target, boolean alphaTarget) {
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        direction.setY(0.0D);
        Vector velocity = ArcadePortRules.deathTagDefaultMeleeKnockback(
                target.getVelocity(), direction, alphaTarget);
        if (velocity != null) target.setVelocity(velocity);
    }

    boolean applyTraitorKnockback(SessionState session, Player attacker, Player target) {
        String cooldownKey = attacker.getUniqueId() + ":" + target.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUse = session.deathTagTraitorKnockbackAt.getOrDefault(cooldownKey, 0L);
        if (!ArcadePortRules.deathTagTraitorKnockbackReady(now, lastUse)) return false;
        Vector direction = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        if (direction.lengthSquared() <= 0.001D) return false;
        session.deathTagTraitorKnockbackAt.put(cooldownKey, now);
        double multiplier = ArcadePortRules.deathTagKnockbackMultiplier(
                session.alphaChasers.contains(target.getUniqueId()));
        double horizontal = GameDebugRuntimeOverrides.doubleValue(
                "DEATH_TAG_TRAITOR_KNOCKBACK_HORIZONTAL", ArcadePortRules.DEATH_TAG_TRAITOR_KNOCKBACK_HORIZONTAL);
        double vertical = GameDebugRuntimeOverrides.doubleValue(
                "DEATH_TAG_TRAITOR_KNOCKBACK_VERTICAL", ArcadePortRules.DEATH_TAG_TRAITOR_KNOCKBACK_VERTICAL);
        target.setVelocity(direction.normalize().multiply(Math.max(0.0D, horizontal) * multiplier)
                .setY(Math.max(0.0D, vertical)));
        return true;
    }

    private static double horizontalDistanceSquared(Location left, Location right) {
        double dx = left.getX() - right.getX();
        double dz = left.getZ() - right.getZ();
        return dx * dx + dz * dz;
    }

    void updateRunnerTrails(SessionState session) {
        if (session.ticks % 3 != 0) return;
        for (Player player : alivePlayers(session)) {
            UUID id = player.getUniqueId();
            if (session.tagged.contains(id)) continue;
            Location point = player.getLocation().clone().add(0.0D, 0.15D, 0.0D);
            Material glass = trailGlass(session.deathTagRunnerTrailColours.get(id));
            session.world.spawnParticle(Particle.BLOCK, point, 2,
                    0.06D, 0.06D, 0.06D, 0.01D, glass.createBlockData());

            String style = session.deathTagRunnerTrailStyles.getOrDefault(id, "classic");
            switch (style) {
                case "soul" -> session.world.spawnParticle(Particle.WITCH, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "sparks" -> session.world.spawnParticle(Particle.FIREWORK, point, 3,
                        0.12D, 0.12D, 0.12D, 0.02D);
                case "smoke" -> session.world.spawnParticle(Particle.SMOKE, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "ender" -> session.world.spawnParticle(Particle.PORTAL, point, 3,
                        0.12D, 0.12D, 0.12D, 0.01D);
                case "hearts" -> session.world.spawnParticle(Particle.HEART, point, 2,
                        0.12D, 0.12D, 0.12D, 0.01D);
                default -> { /* Classic uses only the selected stained-glass trail. */ }
            }
        }
    }

    private static Material trailGlass(String colour) {
        String name = switch (colour == null ? "white" : colour.toLowerCase(java.util.Locale.ROOT)) {
            case "orange" -> "ORANGE_STAINED_GLASS";
            case "magenta" -> "MAGENTA_STAINED_GLASS";
            case "lightblue" -> "LIGHT_BLUE_STAINED_GLASS";
            case "yellow" -> "YELLOW_STAINED_GLASS";
            case "lime" -> "LIME_STAINED_GLASS";
            case "pink" -> "PINK_STAINED_GLASS";
            case "gray" -> "GRAY_STAINED_GLASS";
            case "silver" -> "LIGHT_GRAY_STAINED_GLASS";
            case "cyan" -> "CYAN_STAINED_GLASS";
            case "purple" -> "PURPLE_STAINED_GLASS";
            case "blue" -> "BLUE_STAINED_GLASS";
            case "brown" -> "BROWN_STAINED_GLASS";
            case "green" -> "GREEN_STAINED_GLASS";
            case "red" -> "RED_STAINED_GLASS";
            case "black" -> "BLACK_STAINED_GLASS";
            default -> "WHITE_STAINED_GLASS";
        };
        Material material = Material.matchMaterial(name);
        return material == null ? Material.WHITE_STAINED_GLASS : material;
    }

    void updateDeathTagCompass(SessionState session) {
        List<Player> alive = alivePlayers(session);
        for (Player viewer : alive) {
            boolean viewerChaser = session.tagged.contains(viewer.getUniqueId());
            Player target = alive.stream()
                    .filter(candidate -> session.tagged.contains(candidate.getUniqueId()) != viewerChaser)
                    .min(java.util.Comparator.comparingDouble(candidate ->
                            candidate.getLocation().distanceSquared(viewer.getLocation())))
                    .orElse(null);
            if (target == null) continue;

            boolean hasCompass = false;
            for (ItemStack item : viewer.getInventory().getContents()) {
                if (isDeathTagTrackingCompass(item)) {
                    hasCompass = true;
                    break;
                }
            }
            if (!hasCompass) {
                viewer.getInventory().addItem(namedTrackingCompass("Tracks the nearest opposing Death Tag player."));
            }
            viewer.setCompassTarget(target.getLocation());
        }
    }

    boolean isDeathTagTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && "Tracking Compass".equals(ChatColor.stripColor(meta.getDisplayName()));
    }

    private void setupDeathTagScoreboard(SessionState session) {
        if (session == null || Bukkit.getScoreboardManager() == null) return;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("deathtag", "dummy",
                ChatColor.GOLD + "DEATH TAG");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        session.deathTagScoreboard = scoreboard;
        session.deathTagScoreboardObjective = objective;

        session.deathTagRunnerScoreboardLine = scoreboard.registerNewTeam("dtrunners");
        session.deathTagRunnerScoreboardEntry = ChatColor.GREEN.toString();
        session.deathTagRunnerScoreboardLine.addEntry(session.deathTagRunnerScoreboardEntry);
        session.deathTagRunnerScoreboardLine.setPrefix(ChatColor.GREEN.toString());
        objective.getScore(session.deathTagRunnerScoreboardEntry).setScore(3);

        Team blankLine = scoreboard.registerNewTeam("dtblank");
        String blankEntry = ChatColor.BLACK.toString();
        blankLine.addEntry(blankEntry);
        blankLine.setPrefix(" ");
        objective.getScore(blankEntry).setScore(2);

        session.deathTagChaserScoreboardLine = scoreboard.registerNewTeam("dtchasers");
        session.deathTagChaserScoreboardEntry = ChatColor.RED.toString();
        session.deathTagChaserScoreboardLine.addEntry(session.deathTagChaserScoreboardEntry);
        session.deathTagChaserScoreboardLine.setPrefix(ChatColor.RED.toString());
        objective.getScore(session.deathTagChaserScoreboardEntry).setScore(1);

        for (Player player : session.players.values()) {
            if (player != null && player.isOnline()) player.setScoreboard(scoreboard);
        }
        updateDeathTagScoreboard(session);
    }

    void updateDeathTagScoreboard(SessionState session) {
        if (session == null || session.deathTagScoreboardObjective == null) return;
        int chasers = (int) session.tagged.stream().filter(session.alive::contains).count();
        int runners = Math.max(0, session.alive.size() - chasers);
        if (session.deathTagRunnerScoreboardLine != null) {
            session.deathTagRunnerScoreboardLine.setSuffix(ChatColor.WHITE + " " + runners + " Runners");
        }
        if (session.deathTagChaserScoreboardLine != null) {
            session.deathTagChaserScoreboardLine.setSuffix(ChatColor.WHITE + " " + chasers + " Chasers");
        }
    }

    private static ItemStack namedTrackingCompass(String lore) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Compass");
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static List<Player> alivePlayers(SessionState session) {
        List<Player> players = new ArrayList<>();
        for (UUID id : session.alive) {
            Player player = session.players.get(id);
            if (player != null && player.isOnline()) players.add(player);
        }
        return players;
    }
}
