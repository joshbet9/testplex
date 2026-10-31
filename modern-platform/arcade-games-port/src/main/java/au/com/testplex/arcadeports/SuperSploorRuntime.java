package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

/**
 * First playable Super Sploor core: territory, Sprayer, four-mark splats,
 * respawns and friendly-paint Squid movement. The runtime is hosted by the
 * shared Testplex Arcade manager and never creates its own Bukkit lifecycle.
 */
public final class SuperSploorRuntime implements Listener {
    public record PlayerMatchStats(String kit, long territoryPainted, long enemyTerritoryReclaimed,
            int splats, int deaths, int assists, double squidTravelBlocks,
            double wallClimbBlocks, int specialsUsed, long rollerTerritory,
            int chargerFullHits, long lobberEnemyReclaimed, int dualiesPostRollSplats,
            int bestSpecialTargets) { }
    public record TerritorySummary(int redUnits, int blueUnits, int totalUnits) { }

    private static final class MutableStats {
        long territoryPainted;
        long enemyTerritoryReclaimed;
        int splats;
        int deaths;
        int assists;
        double squidTravelBlocks;
        double wallClimbBlocks;
        int specialsUsed;
        String kit = "sprayer";
        long rollerTerritory;
        int chargerFullHits;
        long lobberEnemyReclaimed;
        int dualiesPostRollSplats;
        int bestSpecialTargets;

        PlayerMatchStats snapshot() {
            return new PlayerMatchStats(kit, territoryPainted, enemyTerritoryReclaimed, splats, deaths,
                    assists, squidTravelBlocks, wallClimbBlocks, specialsUsed,
                    rollerTerritory, chargerFullHits, lobberEnemyReclaimed,
                    dualiesPostRollSplats, bestSpecialTargets);
        }
    }

    private static final int RED = 0;
    private static final int BLUE = 1;
    private static final int PRIMARY_SLOT = 0;
    private static final int SQUID_SLOT = 7;

    private final JavaPlugin plugin;
    private final NamespacedKey projectileKey;
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    private final Map<UUID, Integer> teams = new HashMap<>();
    private final Map<UUID, Location> spawns = new HashMap<>();
    private final Map<UUID, Integer> ink = new HashMap<>();
    private final Map<UUID, String> kits = new HashMap<>();
    private final Map<UUID, Integer> marks = new HashMap<>();
    private final Map<UUID, Long> lastShotAt = new HashMap<>();
    private final Map<UUID, Long> lastInkRegenAt = new HashMap<>();
    private final Map<UUID, Long> chargerChargeStartedAt = new HashMap<>();
    private final Map<UUID, Integer> dodgeRollCharges = new HashMap<>();
    private final Map<UUID, Long> nextDodgeRollRechargeAt = new HashMap<>();
    private final Map<UUID, Long> dualiesRollExpiresAt = new HashMap<>();
    private final Map<UUID, Set<UUID>> dualiesRollTargets = new HashMap<>();
    private final Map<UUID, Long> lastHitAt = new HashMap<>();
    private final Map<UUID, Long> nextMarkRecoveryAt = new HashMap<>();
    private final Map<UUID, Long> spawnProtectedUntil = new HashMap<>();
    private final Map<UUID, Long> respawnAt = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, MutableStats> playerStats = new HashMap<>();
    private final Map<UUID, Set<UUID>> contributors = new HashMap<>();
    private final Set<UUID> squidMode = new HashSet<>();
    private final Set<UUID> activeProjectiles = new HashSet<>();
    private final Map<UUID, UUID> projectileOwners = new HashMap<>();
    private final Map<UUID, Snowball> projectileEntities = new HashMap<>();
    private final Map<UUID, Integer> projectilePaintRadii = new HashMap<>();
    private final Set<UUID> projectilePaintLines = new HashSet<>();
    private final Set<UUID> fullyChargedProjectiles = new HashSet<>();
    private final Map<UUID, Location> projectileLastLocations = new HashMap<>();

    private SuperSploorEngine engine;
    private World world;
    private Scoreboard scoreboard;
    private Objective objective;
    private final List<String> sidebarEntries = new ArrayList<>();
    private IntConsumer finishMatch;
    private long matchEndsAt;
    private long overtimeEndsAt;
    private boolean overtime;
    private boolean running;
    private long lastTickAt;

    public SuperSploorRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        this.projectileKey = new NamespacedKey(plugin, "super-sploor-projectile");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean isRunning() { return running; }

    public Map<UUID, PlayerMatchStats> snapshotStats() {
        Map<UUID, PlayerMatchStats> copy = new HashMap<>();
        for (Map.Entry<UUID, MutableStats> entry : playerStats.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().snapshot());
        }
        return Map.copyOf(copy);
    }

    public TerritorySummary snapshotTerritory() {
        if (engine == null) return new TerritorySummary(0, 0, 0);
        return new TerritorySummary(engine.redUnits(), engine.blueUnits(), engine.totalUnits());
    }

    public int leadingTeam() {
        if (engine == null) return -1;
        return SuperSploorRules.leadingTeam(engine.redUnits(), engine.blueUnits());
    }

    public void start(SuperSploorEngine engine, Map<UUID, Player> participants,
            Map<UUID, Integer> participantTeams, Map<UUID, Location> participantSpawns,
            Map<UUID, String> participantKits, IntConsumer finishMatch) {
        stop();
        if (engine == null || participants == null || participantTeams == null
                || participantSpawns == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Super Sploor requires a validated map and participants.");
        }
        this.engine = engine;
        this.world = participants.values().iterator().next().getWorld();
        this.finishMatch = finishMatch;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Player> entry : participants.entrySet()) {
            UUID id = entry.getKey();
            Player player = entry.getValue();
            if (player == null || !player.isOnline()) continue;
            int team = participantTeams.getOrDefault(id, -1);
            Location spawn = participantSpawns.get(id);
            if ((team != RED && team != BLUE) || spawn == null || spawn.getWorld() != world) {
                throw new IllegalArgumentException("Super Sploor participant has no valid team or authored spawn: " + id);
            }
            players.put(id, player);
            teams.put(id, team);
            spawns.put(id, spawn.clone());
            String kit = normalizeKit(participantKits == null ? null : participantKits.get(id));
            kits.put(id, kit);
            ink.put(id, SuperSploorRules.MAX_INK);
            marks.put(id, 0);
            lastShotAt.put(id, now - 2_000L);
            lastInkRegenAt.put(id, now);
            spawnProtectedUntil.put(id, now + SuperSploorRules.SPAWN_PROTECTION_MILLIS);
            kills.put(id, 0);
            MutableStats stats = new MutableStats();
            stats.kit = kit;
            playerStats.put(id, stats);
            contributors.put(id, new HashSet<>());
            if ("dualies".equals(kit)) {
                dodgeRollCharges.put(id, 2);
                nextDodgeRollRechargeAt.put(id, Long.MAX_VALUE);
                dualiesRollTargets.put(id, new HashSet<>());
            }
            giveStarterLoadout(player, team, kit);
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);
            updateInkBar(player, id);
        }
        if (players.isEmpty()) throw new IllegalArgumentException("Super Sploor has no online participants.");
        this.matchEndsAt = now + SuperSploorRules.ROUND_MILLIS;
        this.overtimeEndsAt = 0L;
        this.lastTickAt = now;
        this.overtime = false;
        this.running = true;
        setupScoreboard();
        announce(ChatColor.GOLD + "" + ChatColor.BOLD
                + "Super Sploor! " + ChatColor.RED + "Red" + ChatColor.GRAY + " vs "
                + ChatColor.AQUA + "Blue" + ChatColor.GRAY + " — paint the most territory.");
        for (Player player : players.values()) {
            player.sendMessage(ChatColor.YELLOW + "Right-click your selected primary weapon to paint. Select the Ink Sac on slot 8 while standing on friendly paint to enter Squid Mode.");
        }
        updateScoreboard(now);
    }

    public void tick(long now) {
        if (!running || engine == null) return;
        updateChargerPaintLines();
        rechargeDodgeRolls(now);
        for (Map.Entry<UUID, Long> charge : new ArrayList<>(chargerChargeStartedAt.entrySet())) {
            Player chargingPlayer = players.get(charge.getKey());
            if (chargingPlayer != null && chargingPlayer.isOnline()) {
                long percent = Math.min(100L, Math.max(0L, (now - charge.getValue()) * 100L / 900L));
                chargingPlayer.sendActionBar(ChatColor.AQUA + "CHARGER " + percent + "%");
            }
        }
        lastTickAt = now;
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player player = players.get(id);
            if (player == null || !player.isOnline()) continue;
            if (respawnAt.containsKey(id)) {
                if (now >= respawnAt.get(id)) respawn(id, now);
                continue;
            }

            int currentInk = ink.getOrDefault(id, SuperSploorRules.MAX_INK);
            boolean inFriendlyPaint = engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1));
            boolean squid = squidMode.contains(id);
            if (squid && !inFriendlyPaint) {
                exitSquidMode(player, id);
                squid = false;
            }
            long lastRegen = lastInkRegenAt.getOrDefault(id, now);
            if (now - lastRegen >= 1_000L) {
                int seconds = (int) Math.min(10L, (now - lastRegen) / 1_000L);
                boolean canRegen = squid && inFriendlyPaint
                        || now - lastShotAt.getOrDefault(id, 0L) >= 1_000L;
                if (canRegen && seconds > 0) {
                    int updated = SuperSploorRules.nextInkAfterRegeneration(
                            currentInk, seconds, squid && inFriendlyPaint);
                    if (updated != currentInk) {
                        ink.put(id, updated);
                        updateInkBar(player, id);
                    }
                }
                lastInkRegenAt.put(id, lastRegen + seconds * 1_000L);
            }

            long protectedUntil = spawnProtectedUntil.getOrDefault(id, 0L);
            if (protectedUntil > 0L && now >= protectedUntil) {
                spawnProtectedUntil.remove(id);
                player.setInvulnerable(false);
            } else if (protectedUntil > now) {
                player.setInvulnerable(true);
            }

            recoverMarkIfEligible(id, player, now);
            if (squid && engine.isFriendlyWallAdjacent(player.getLocation(), teams.getOrDefault(id, -1))) {
                Vector velocity = player.getVelocity();
                if (velocity.getY() < SuperSploorRules.SQUID_CLIMB_VELOCITY) {
                    velocity.setY(SuperSploorRules.SQUID_CLIMB_VELOCITY);
                    player.setVelocity(velocity);
                }
                world.spawnParticle(Particle.DUST, player.getLocation().add(0.0D, 0.15D, 0.0D),
                        2, 0.2D, 0.1D, 0.2D, 0.0D,
                        new Particle.DustOptions(teamColour(teams.get(id)), 0.8F));
            }
        }

        if (now >= matchEndsAt && !overtime) {
            int leader = leadingTeam();
            if (leader >= 0) {
                finish(leader);
                return;
            }
            overtime = true;
            overtimeEndsAt = now + SuperSploorRules.OVERTIME_MILLIS;
            announce(ChatColor.GOLD + "" + ChatColor.BOLD
                    + "Turf War is tied! First team to lead by 1% wins — 30 seconds of overtime.");
        } else if (overtime) {
            int red = engine.redUnits();
            int blue = engine.blueUnits();
            if (SuperSploorRules.hasDecisiveOvertimeLead(red, blue, engine.totalUnits())) {
                finish(SuperSploorRules.leadingTeam(red, blue));
                return;
            }
            if (now >= overtimeEndsAt) {
                announce(ChatColor.YELLOW + "Super Sploor overtime ended in a draw.");
                finish(-1);
                return;
            }
        }
        if (now - lastScoreboardAt >= 250L) updateScoreboard(now);
    }

    private long lastScoreboardAt;

    private static String normalizeKit(String requested) {
        if (requested == null) return "sprayer";
        String value = requested.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        return Set.of("sprayer", "roller", "charger", "lobber", "dualies").contains(value) ? value : "sprayer";
    }

    private void giveStarterLoadout(Player player, int team, String kit) {
        player.getInventory().clear();
        String name = switch (kit) {
            case "roller" -> "Paint Roller";
            case "charger" -> "Ink Charger";
            case "lobber" -> "Paint Lobber";
            case "dualies" -> "Twin Blasters";
            default -> "Splattershot";
        };
        Material weapon = switch (kit) {
            case "roller" -> Material.IRON_HOE;
            case "charger" -> Material.CROSSBOW;
            case "lobber" -> Material.SNOWBALL;
            case "dualies" -> Material.FLINT;
            default -> Material.BLAZE_ROD;
        };
        player.getInventory().setItem(0, namedItem(weapon, ChatColor.GOLD + name,
                "Right-click to use your primary • ink cost depends on kit"));
        if ("dualies".equals(kit)) {
            player.getInventory().setItem(1, namedItem(Material.ENDER_PEARL,
                    ChatColor.AQUA + "Dodge Roll", "Right-click to dodge. Two charges; one recharges every 5 seconds."));
        }
        player.getInventory().setItem(7, namedItem(Material.INK_SAC, ChatColor.AQUA + "Squid Mode",
                "Select this slot on your team's paint to swim, climb and refill ink"));
        player.getInventory().setHeldItemSlot(PRIMARY_SLOT);
        player.getInventory().setArmorContents(teamArmour(team, 0));
        player.setExp(1.0F);
        player.setLevel(0);
        player.setWalkSpeed(0.2F);
    }

    private ItemStack namedItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(ChatColor.GRAY + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] teamArmour(int team, int currentMarks) {
        ItemStack[] armour = new ItemStack[4];
        Material material = Material.LEATHER_BOOTS;
        for (int slot = 0; slot < armour.length; slot++) {
            armour[slot] = new ItemStack(switch (slot) {
                case 0 -> Material.LEATHER_BOOTS;
                case 1 -> Material.LEATHER_LEGGINGS;
                case 2 -> Material.LEATHER_CHESTPLATE;
                default -> Material.LEATHER_HELMET;
            });
            LeatherArmorMeta meta = (LeatherArmorMeta) armour[slot].getItemMeta();
            meta.setColor(slot < currentMarks ? Color.PURPLE : teamColour(team));
            armour[slot].setItemMeta(meta);
        }
        return armour;
    }

    private Color teamColour(int team) {
        return team == RED ? Color.fromRGB(235, 65, 65) : Color.fromRGB(70, 155, 240);
    }

    private void paintArmour(Player player, int team, int markCount) {
        player.getInventory().setArmorContents(teamArmour(team, markCount));
    }

    private void updateInkBar(Player player, UUID id) {
        int value = ink.getOrDefault(id, 0);
        player.setExp(value / 100.0F);
        player.setLevel(value);
        player.sendActionBar(ChatColor.AQUA + "INK " + value + "%"
                + (squidMode.contains(id) ? ChatColor.GREEN + "  SQUID" : "")
                + ChatColor.GRAY + "  |  " + ChatColor.RED + engine.redUnits() + ChatColor.GRAY
                + " : " + ChatColor.AQUA + engine.blueUnits());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!running || event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || !player.isOnline() || respawnAt.containsKey(id)) return;
        event.setCancelled(true);
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta() || held.getItemMeta() == null
                || !held.getItemMeta().hasDisplayName()) return;
        String heldName = ChatColor.stripColor(held.getItemMeta().getDisplayName());
        String kit = kits.getOrDefault(id, "sprayer");

        if ("dualies".equals(kit) && "Dodge Roll".equals(heldName)
                && event.getAction().isRightClick()) {
            useDodgeRoll(player, id);
            return;
        }

        String expected = switch (kit) {
            case "roller" -> "Paint Roller";
            case "charger" -> "Ink Charger";
            case "lobber" -> "Paint Lobber";
            case "dualies" -> "Twin Blasters";
            default -> "Splattershot";
        };
        if (!expected.equals(heldName)) return;
        if ("charger".equals(kit)) {
            if (event.getAction().isRightClick()) beginChargerCharge(player, id);
            else if (event.getAction().isLeftClick()) releaseChargerCharge(player, id);
            return;
        }
        if (event.getAction().isRightClick()) firePrimary(player, id);
    }

    private void beginChargerCharge(Player player, UUID id) {
        if (chargerChargeStartedAt.containsKey(id)) return;
        long now = System.currentTimeMillis();
        if (ink.getOrDefault(id, 0) < 8) {
            player.sendActionBar(ChatColor.RED + "Not enough ink to charge.");
            return;
        }
        long lastShot = lastShotAt.getOrDefault(id, 0L);
        if (now - lastShot < 1_000L) {
            player.sendActionBar(ChatColor.YELLOW + "Charger recharging.");
            return;
        }
        chargerChargeStartedAt.put(id, now);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.35F, 1.7F);
        player.sendActionBar(ChatColor.AQUA + "CHARGER 0%");
    }

    private void releaseChargerCharge(Player player, UUID id) {
        Long started = chargerChargeStartedAt.remove(id);
        if (started == null) return;
        long elapsed = Math.max(0L, System.currentTimeMillis() - started);
        double charge = Math.max(0.2D, Math.min(1.0D, elapsed / 900.0D));
        fireChargedCharger(player, id, charge);
    }

    private void fireChargedCharger(Player player, UUID id, double charge) {
        long now = System.currentTimeMillis();
        if (now - lastShotAt.getOrDefault(id, 0L) < 1_000L) return;
        int cost = 8 + (int) Math.ceil(10.0D * charge);
        int current = ink.getOrDefault(id, 0);
        if (current < cost) {
            player.sendActionBar(ChatColor.RED + "Not enough ink for that charge.");
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, cost));
        lastShotAt.put(id, now);
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.get(id) == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        projectile.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.8D + charge * 1.2D));
        UUID projectileId = projectile.getUniqueId();
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        projectileOwners.put(projectileId, id);
        projectileEntities.put(projectileId, projectile);
        activeProjectiles.add(projectileId);
        projectilePaintRadii.put(projectileId, 0);
        projectilePaintLines.add(projectileId);
        projectileLastLocations.put(projectileId, projectile.getLocation().clone());
        if (charge >= 0.99D) fullyChargedProjectiles.add(projectileId);
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.9F, 1.9F);
        player.sendActionBar(ChatColor.AQUA + "CHARGER " + Math.round(charge * 100.0D) + "%");
        updateInkBar(player, id);
    }

    private void useDodgeRoll(Player player, UUID id) {
        long now = System.currentTimeMillis();
        int charges = dodgeRollCharges.getOrDefault(id, 0);
        if (charges <= 0) {
            long readyAt = nextDodgeRollRechargeAt.getOrDefault(id, now + 5_000L);
            player.sendActionBar(ChatColor.YELLOW + "Dodge Roll recharging: "
                    + Math.max(0L, (readyAt - now + 999L) / 1_000L) + "s");
            return;
        }
        dodgeRollCharges.put(id, charges - 1);
        if (charges == 2) nextDodgeRollRechargeAt.put(id, now + 5_000L);
        dualiesRollExpiresAt.put(id, now + 5_000L);
        dualiesRollTargets.computeIfAbsent(id, ignored -> new HashSet<>()).clear();
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) direction = new Vector(0.0D, 0.0D, 1.0D);
        direction.normalize().multiply(1.15D).setY(0.18D);
        player.setVelocity(direction);
        player.setFallDistance(0.0F);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7F, 1.5F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12,
                0.25D, 0.2D, 0.25D, 0.04D);
        player.sendActionBar(ChatColor.AQUA + "DODGE ROLL  •  " + (charges - 1) + "/2 charges");
    }

    private void rechargeDodgeRolls(long now) {
        for (UUID id : new ArrayList<>(players.keySet())) {
            if (!"dualies".equals(kits.get(id)) || dodgeRollCharges.getOrDefault(id, 0) >= 2) continue;
            long readyAt = nextDodgeRollRechargeAt.getOrDefault(id, Long.MAX_VALUE);
            if (now < readyAt) continue;
            dodgeRollCharges.merge(id, 1, (oldValue, one) -> Math.min(2, oldValue + 1));
            nextDodgeRollRechargeAt.put(id, dodgeRollCharges.getOrDefault(id, 0) < 2
                    ? now + 5_000L : Long.MAX_VALUE);
        }
    }

    private void firePrimary(Player player, UUID id) {
        long now = System.currentTimeMillis();
        String kit = kits.getOrDefault(id, "sprayer");
        int current = ink.getOrDefault(id, 0);
        long previous = lastShotAt.getOrDefault(id, 0L);
        int cost = switch (kit) {
            case "roller" -> 10;
            case "charger" -> 18;
            case "lobber" -> 14;
            case "dualies" -> 4;
            default -> SuperSploorRules.INK_PER_PRIMARY_SHOT;
        };
        long interval = switch (kit) {
            case "roller" -> 500L;
            case "charger" -> 1_000L;
            case "lobber" -> 1_100L;
            case "dualies" -> 350L;
            default -> SuperSploorRules.PRIMARY_SHOT_INTERVAL_MILLIS;
        };
        if (current < cost || now < previous || now - previous < interval) {
            if (current < cost) player.sendActionBar(ChatColor.RED + "Out of ink! Swim through your team's paint to refill.");
            return;
        }
        ink.put(id, SuperSploorRules.spendInk(current, cost));
        lastShotAt.put(id, now);

        if ("roller".equals(kit)) {
            rollPaintPath(player, id);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_HOE_TILL, 0.8F, 0.8F);
            updateInkBar(player, id);
            return;
        }

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(teams.get(id) == RED ? Material.RED_DYE : Material.LIGHT_BLUE_DYE));
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double speed = switch (kit) {
            case "charger" -> 3.0D;
            case "lobber" -> 1.15D;
            case "dualies" -> 1.45D;
            default -> 1.85D;
        };
        Vector velocity = direction.multiply(speed);
        if ("lobber".equals(kit)) velocity.setY(velocity.getY() + 0.16D);
        projectile.setVelocity(velocity);
        projectile.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);
        UUID projectileId = projectile.getUniqueId();
        projectileOwners.put(projectileId, id);
        projectileEntities.put(projectileId, projectile);
        activeProjectiles.add(projectileId);
        projectilePaintRadii.put(projectileId, switch (kit) {
            case "lobber" -> 3;
            case "dualies" -> 1;
            case "charger" -> 0;
            default -> 2;
        });
        if ("charger".equals(kit)) {
            projectilePaintLines.add(projectileId);
            projectileLastLocations.put(projectileId, projectile.getLocation().clone());
        }
        player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.7F,
                "charger".equals(kit) ? 1.8F : "lobber".equals(kit) ? 0.75F : 1.45F);
        updateInkBar(player, id);
    }

    private void rollPaintPath(Player player, UUID id) {
        int team = teams.getOrDefault(id, -1);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() < 1.0E-5D) return;
        direction.normalize();
        Location origin = player.getLocation().clone();
        for (int step = 0; step <= 5; step++) {
            Location point = origin.clone().add(direction.clone().multiply(step));
            point.setY(player.getLocation().getY() - 0.35D);
            recordPaint(id, engine.paint(point, team, 1));
        }
        // The Roller pressures close opponents but cannot repeatedly mark one target every tick.
        for (Player target : new ArrayList<>(players.values())) {
            if (target == null || !target.isOnline() || target.getUniqueId().equals(id)
                    || teams.getOrDefault(target.getUniqueId(), -1) == team
                    || respawnAt.containsKey(target.getUniqueId())) continue;
            Vector relative = target.getLocation().toVector().subtract(origin.toVector());
            double along = relative.dot(direction);
            if (along < 0.0D || along > 4.5D) continue;
            double lateral = relative.clone().subtract(direction.clone().multiply(along)).length();
            if (lateral <= 1.25D) {
                applyMark(id, target.getUniqueId(), target);
            }
        }
    }

    private void recordPaint(UUID ownerId, SuperSploorEngine.PaintResult result) {
        MutableStats stats = playerStats.get(ownerId);
        if (stats == null || result == null) return;
        stats.territoryPainted += result.changedUnits();
        stats.enemyTerritoryReclaimed += result.enemyReclaimed();
        if ("roller".equals(kits.get(ownerId))) stats.rollerTerritory += result.changedUnits();
        if ("lobber".equals(kits.get(ownerId))) stats.lobberEnemyReclaimed += result.enemyReclaimed();
    }

    private void updateChargerPaintLines() {
        if (engine == null || world == null) return;
        for (UUID projectileId : new ArrayList<>(projectilePaintLines)) {
            Snowball projectile = projectileEntities.get(projectileId);
            UUID ownerId = projectileOwners.get(projectileId);
            if (projectile == null || !projectile.isValid() || ownerId == null) {
                projectilePaintLines.remove(projectileId);
                projectileLastLocations.remove(projectileId);
                continue;
            }
            Location current = projectile.getLocation();
            Location previous = projectileLastLocations.put(projectileId, current.clone());
            if (previous == null || previous.getWorld() != current.getWorld()) continue;
            double distance = previous.distance(current);
            int samples = Math.max(1, Math.min(16, (int) Math.ceil(distance / 0.35D)));
            Vector segment = current.toVector().subtract(previous.toVector());
            for (int sample = 1; sample <= samples; sample++) {
                Location point = previous.clone().add(segment.clone().multiply(sample / (double) samples));
                recordPaint(ownerId, engine.paint(point, teams.getOrDefault(ownerId, -1), 0));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!running || !activeProjectiles.remove(event.getEntity().getUniqueId())) return;
        Projectile projectile = event.getEntity();
        UUID projectileId = projectile.getUniqueId();
        UUID ownerId = projectileOwners.remove(projectileId);
        int paintRadius = projectilePaintRadii.getOrDefault(projectileId, 2);
        projectileEntities.remove(projectileId);
        projectilePaintRadii.remove(projectileId);
        projectilePaintLines.remove(projectileId);
        projectileLastLocations.remove(projectileId);
        Player owner = ownerId == null ? null : players.get(ownerId);
        Location impact = projectile.getLocation().clone();
        if (event.getHitEntity() instanceof Player target && owner != null) {
            if (fullyChargedProjectiles.remove(projectileId)) {
                MutableStats ownerStats = playerStats.get(ownerId);
                if (ownerStats != null) ownerStats.chargerFullHits++;
            }
            applyMark(ownerId, target.getUniqueId(), target);
        } else {
            fullyChargedProjectiles.remove(projectileId);
        }
        if (owner != null && owner.isOnline()) {
            int team = teams.getOrDefault(ownerId, -1);
            SuperSploorEngine.PaintResult result = engine.paint(impact, team, paintRadius);
            recordPaint(ownerId, result);
            if (result.changedUnits() > 0) {
                impact.getWorld().spawnParticle(Particle.DUST, impact.clone().add(0.0D, 0.3D, 0.0D),
                        18, 0.55D, 0.35D, 0.55D, 0.0D,
                        new Particle.DustOptions(teamColour(team), 1.4F));
                owner.playSound(impact, Sound.BLOCK_WOOL_PLACE, 0.65F, 1.4F);
            }
        }
        projectile.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())
                && event.getBlock().getWorld() == world) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())
                && event.getBlock().getWorld() == world) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        if (running && players.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!running || !(event.getEntity() instanceof Player target)
                || !players.containsKey(target.getUniqueId())) return;
        event.setCancelled(true);
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player attacker) {
            UUID attackerId = attacker.getUniqueId();
            if (players.containsKey(attackerId) && teams.getOrDefault(attackerId, -1)
                    != teams.getOrDefault(target.getUniqueId(), -2)) {
                applyMark(attackerId, target.getUniqueId(), target);
            }
        }
    }

    private void applyMark(UUID attackerId, UUID targetId, Player target) {
        if (!running || target == null || !target.isOnline()
                || !players.containsKey(targetId) || respawnAt.containsKey(targetId)) return;
        if (spawnProtectedUntil.getOrDefault(targetId, 0L) > System.currentTimeMillis()) return;
        int attackerTeam = teams.getOrDefault(attackerId, -1);
        int targetTeam = teams.getOrDefault(targetId, -2);
        if (attackerId == null || attackerTeam < 0 || attackerTeam == targetTeam) return;
        long now = System.currentTimeMillis();
        int count = SuperSploorRules.markCountAfterHit(marks.getOrDefault(targetId, 0));
        marks.put(targetId, count);
        lastHitAt.put(targetId, now);
        nextMarkRecoveryAt.put(targetId, now + SuperSploorRules.MARK_RECOVERY_DELAY_MILLIS
                + SuperSploorRules.MARK_RECOVERY_INTERVAL_MILLIS);
        paintArmour(target, targetTeam, count);
        target.playEffect(org.bukkit.EntityEffect.HURT);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0D, 1.0D, 0.0D),
                12, 0.35D, 0.5D, 0.35D, 0.0D,
                new Particle.DustOptions(Color.PURPLE, 1.2F));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8F, 1.0F);
        contributors.computeIfAbsent(targetId, ignored -> new HashSet<>()).add(attackerId);
        if (count >= SuperSploorRules.MAX_PAINT_MARKS) {
            kills.merge(attackerId, 1, Integer::sum);
            MutableStats attackerStats = playerStats.get(attackerId);
            if (attackerStats != null) {
                attackerStats.splats++;
                if ("dualies".equals(kits.get(attackerId))
                        && dualiesRollExpiresAt.getOrDefault(attackerId, 0L) >= now
                        && dualiesRollTargets.computeIfAbsent(attackerId, ignored -> new HashSet<>()).add(targetId)) {
                    attackerStats.dualiesPostRollSplats++;
                }
            }
            splat(targetId, target, attackerId);
        } else {
            target.sendActionBar(ChatColor.RED + "SPLAT MARK " + count + "/4");
        }
    }

    private void splat(UUID targetId, Player target, UUID killerId) {
        if (!running || respawnAt.containsKey(targetId)) return;
        long now = System.currentTimeMillis();
        respawnAt.put(targetId, now + SuperSploorRules.SPLAT_RESPAWN_MILLIS);
        exitSquidMode(target, targetId);
        target.setInvulnerable(true);
        target.setGameMode(GameMode.SPECTATOR);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_DEATH, 0.9F, 1.1F);
        target.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "SPLATTED",
                ChatColor.GRAY + "Respawning in 2.5 seconds", 0, 30, 5);
        target.getWorld().spawnParticle(Particle.SPLASH, target.getLocation().add(0.0D, 1.0D, 0.0D),
                45, 0.7D, 0.8D, 0.7D, 0.1D);
        announce(ChatColor.GRAY + target.getName() + " was splatted.");
        MutableStats victimStats = playerStats.get(targetId);
        if (victimStats != null) victimStats.deaths++;
        Set<UUID> helpers = contributors.remove(targetId);
        if (helpers != null) {
            for (UUID helperId : helpers) {
                if (helperId.equals(killerId)) continue;
                MutableStats helperStats = playerStats.get(helperId);
                if (helperStats != null) helperStats.assists++;
            }
        }
    }

    private void respawn(UUID id, long now) {
        Player player = players.get(id);
        Location spawn = spawns.get(id);
        if (player == null || !player.isOnline() || spawn == null) {
            respawnAt.remove(id);
            return;
        }
        respawnAt.remove(id);
        marks.put(id, 0);
        lastHitAt.remove(id);
        nextMarkRecoveryAt.remove(id);
        ink.put(id, SuperSploorRules.MAX_INK);
        spawnProtectedUntil.put(id, now + SuperSploorRules.SPAWN_PROTECTION_MILLIS);
        contributors.remove(id);
        player.teleport(spawn.clone());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setInvulnerable(true);
        giveStarterLoadout(player, teams.getOrDefault(id, RED), kits.getOrDefault(id, "sprayer"));
        player.sendMessage(ChatColor.GREEN + "Respawned! Your ink tank is full.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.3F);
    }

    private void recoverMarkIfEligible(UUID id, Player player, long now) {
        int count = marks.getOrDefault(id, 0);
        if (count <= 0) return;
        boolean friendly = engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1));
        long lastHit = lastHitAt.getOrDefault(id, 0L);
        long nextAt = nextMarkRecoveryAt.getOrDefault(id, Long.MAX_VALUE);
        if (!SuperSploorRules.canRecoverMark(now, lastHit, nextAt, friendly, count)) return;
        int next = count - 1;
        marks.put(id, next);
        nextMarkRecoveryAt.put(id, now + SuperSploorRules.MARK_RECOVERY_INTERVAL_MILLIS);
        paintArmour(player, teams.get(id), next);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.45F, 1.8F);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!running || event instanceof PlayerTeleportEvent || event.getTo() == null) return;
        UUID id = event.getPlayer().getUniqueId();
        if (!players.containsKey(id) || respawnAt.containsKey(id) || !squidMode.contains(id)) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != world || to.getWorld() != world) return;
        int team = teams.getOrDefault(id, -1);
        MutableStats stats = playerStats.get(id);
        if (stats == null) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (engine.isFriendlySurface(to, team)
                && SuperSploorRules.shouldCountSquidTravel(horizontal, false)) {
            stats.squidTravelBlocks += horizontal;
        }
        double vertical = to.getY() - from.getY();
        if (vertical > 0.0D && vertical <= 1.25D
                && engine.isFriendlyWallAdjacent(from, team)
                && engine.isFriendlyWallAdjacent(to, team)) {
            stats.wallClimbBlocks += vertical;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onHeldSlot(PlayerItemHeldEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || respawnAt.containsKey(id)) return;
        if (event.getNewSlot() == SQUID_SLOT) {
            if (!engine.isFriendlySurface(player.getLocation(), teams.getOrDefault(id, -1))) {
                event.setCancelled(true);
                player.sendActionBar(ChatColor.RED + "You can only enter Squid Mode on your team's paint.");
                return;
            }
            squidMode.add(id);
            player.setWalkSpeed((float) SuperSploorRules.SQUID_WALK_SPEED);
            player.setSneaking(true);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 0.7F, 1.4F);
            player.sendActionBar(ChatColor.GREEN + "SQUID MODE — swim, climb and refill!");
        } else if (squidMode.contains(id)) {
            exitSquidMode(player, id);
        }
    }

    private void exitSquidMode(Player player, UUID id) {
        squidMode.remove(id);
        if (player != null && player.isOnline()) {
            player.setWalkSpeed(0.2F);
            player.setSneaking(false);
        }
    }

    private void setupScoreboard() {
        if (Bukkit.getScoreboardManager() == null) return;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("sploor", "dummy", ChatColor.GOLD + "" + ChatColor.BOLD + "SUPER SPLOOR");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (Player player : players.values()) player.setScoreboard(scoreboard);
    }

    private void updateScoreboard(long now) {
        if (objective == null || engine == null) return;
        for (String old : sidebarEntries) scoreboard.resetScores(old);
        sidebarEntries.clear();
        int total = Math.max(1, engine.totalUnits());
        int red = (int) Math.round(engine.redUnits() * 100.0D / total);
        int blue = (int) Math.round(engine.blueUnits() * 100.0D / total);
        int neutral = Math.max(0, 100 - red - blue);
        long remain = overtime ? Math.max(0L, overtimeEndsAt - now) : Math.max(0L, matchEndsAt - now);
        String timer = String.format(java.util.Locale.ROOT, "%d:%02d", remain / 60_000L, (remain / 1_000L) % 60L);
        List<String> lines = List.of(
                ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timer + (overtime ? ChatColor.GOLD + " OT" : ""),
                ChatColor.RED + "Red: " + red + "%",
                ChatColor.AQUA + "Blue: " + blue + "%",
                ChatColor.GRAY + "Neutral: " + neutral + "%",
                ChatColor.DARK_GRAY + " ",
                ChatColor.WHITE + "Your ink: " + ChatColor.AQUA + "shown below");
        int score = lines.size();
        for (String line : lines) {
            sidebarEntries.add(line);
            objective.getScore(line).setScore(score--);
        }
        lastScoreboardAt = now;
        int teamRed = engine.redUnits();
        int teamBlue = engine.blueUnits();
        for (Player player : players.values()) {
            if (!player.isOnline()) continue;
            player.sendActionBar(ChatColor.AQUA + "INK " + ink.getOrDefault(player.getUniqueId(), 0)
                    + "%" + ChatColor.GRAY + " | " + ChatColor.RED + teamRed
                    + ChatColor.GRAY + " : " + ChatColor.AQUA + teamBlue);
        }
    }

    private void announce(String message) {
        for (Player player : players.values()) {
            if (player != null && player.isOnline()) player.sendMessage(message);
        }
    }

    private void finish(int winningTeam) {
        if (!running) return;
        running = false;
        IntConsumer callback = finishMatch;
        if (callback != null) callback.accept(winningTeam);
    }

    public void stop() {
        running = false;
        if (world != null) {
            Set<UUID> projectileIds = new HashSet<>(activeProjectiles);
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (projectileIds.contains(entity.getUniqueId())) entity.remove();
            }
        }
        activeProjectiles.clear();
        projectileOwners.clear();
        projectileEntities.clear();
        fullyChargedProjectiles.clear();
        projectilePaintRadii.clear();
        projectilePaintLines.clear();
        projectileLastLocations.clear();
        for (UUID id : new ArrayList<>(squidMode)) {
            Player player = players.get(id);
            exitSquidMode(player, id);
        }
        if (engine != null) engine.restore();
        if (objective != null) {
            try { objective.unregister(); } catch (IllegalArgumentException ignored) { }
        }
        for (Player player : players.values()) {
            if (player != null && player.isOnline()) {
                player.setInvulnerable(false);
                player.setWalkSpeed(0.2F);
            }
        }
        players.clear();
        teams.clear();
        spawns.clear();
        kits.clear();
        ink.clear();
        marks.clear();
        lastShotAt.clear();
        lastInkRegenAt.clear();
        chargerChargeStartedAt.clear();
        dodgeRollCharges.clear();
        nextDodgeRollRechargeAt.clear();
        dualiesRollExpiresAt.clear();
        dualiesRollTargets.clear();
        lastHitAt.clear();
        nextMarkRecoveryAt.clear();
        spawnProtectedUntil.clear();
        respawnAt.clear();
        kills.clear();
        playerStats.clear();
        contributors.clear();
        squidMode.clear();
        sidebarEntries.clear();
        engine = null;
        world = null;
        scoreboard = null;
        objective = null;
        finishMatch = null;
        matchEndsAt = 0L;
        overtimeEndsAt = 0L;
        overtime = false;
        lastScoreboardAt = 0L;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!players.containsKey(id)) return;
        Player player = players.remove(id);
        if (player != null) exitSquidMode(player, id);
        teams.remove(id);
        spawns.remove(id);
        kits.remove(id);
        chargerChargeStartedAt.remove(id);
        dodgeRollCharges.remove(id);
        nextDodgeRollRechargeAt.remove(id);
        dualiesRollExpiresAt.remove(id);
        dualiesRollTargets.remove(id);
        ink.remove(id);
        marks.remove(id);
        respawnAt.remove(id);
        spawnProtectedUntil.remove(id);
        lastShotAt.remove(id);
        lastInkRegenAt.remove(id);
        lastHitAt.remove(id);
        nextMarkRecoveryAt.remove(id);
        contributors.remove(id);
        for (Set<UUID> values : contributors.values()) values.remove(id);
        if (players.isEmpty()) finish(-1);
    }
}
