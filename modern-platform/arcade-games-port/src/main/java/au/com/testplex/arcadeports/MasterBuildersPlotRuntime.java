package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Per-player Master Builders plot options, particle markers, and owned mob shop. */
final class MasterBuildersPlotRuntime implements Listener {
    interface Access {
        boolean isBuilding(Player player);
        boolean insidePlot(Player player, Location location);
        Location plotCenter(Player player);
        Player ownerAt(Location location);
        boolean managesWorld(World world);
        UUID currentReviewTarget();
        boolean isReviewing();
    }

    private static final int MOB_LIMIT = 16;
    private static final int PARTICLE_LIMIT = 24;
    private static final List<Material> GROUNDS = List.of(
            Material.STONE, Material.GRASS_BLOCK, Material.DIRT, Material.SAND, Material.WATER,
            Material.LAVA, Material.OAK_PLANKS, Material.COBBLESTONE, Material.NETHERRACK,
            Material.STONE_BRICKS, Material.END_STONE, Material.MYCELIUM, Material.GRAY_TERRACOTTA,
            Material.BLACK_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.BROWN_TERRACOTTA,
            Material.GREEN_TERRACOTTA, Material.PINK_TERRACOTTA, Material.QUARTZ_BLOCK,
            Material.PACKED_ICE, Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK);
    private static final List<Long> TIMES = List.of(0L, 3_000L, 6_000L, 9_000L, 12_000L, 15_000L,
            18_000L, 21_000L, 24_000L, 27_000L, 30_000L, 33_000L);
    private static final List<Material> GROUND_ICONS = List.of(Material.STONE, Material.GRASS_BLOCK,
            Material.DIRT, Material.SAND, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.OAK_PLANKS,
            Material.COBBLESTONE, Material.NETHERRACK, Material.STONE_BRICKS, Material.END_STONE,
            Material.MYCELIUM, Material.GRAY_TERRACOTTA, Material.BLACK_TERRACOTTA, Material.YELLOW_TERRACOTTA,
            Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA, Material.PINK_TERRACOTTA,
            Material.QUARTZ_BLOCK, Material.PACKED_ICE, Material.IRON_BLOCK, Material.GOLD_BLOCK,
            Material.DIAMOND_BLOCK);
    private static final List<ParticleStyle> PARTICLES = List.of(
            new ParticleStyle("Lightning Cloud", Particle.ELECTRIC_SPARK, Material.LIGHTNING_ROD, null),
            new ParticleStyle("White Smoke", Particle.CLOUD, Material.WHITE_DYE, null),
            new ParticleStyle("Brown Magic", Particle.CRIT, Material.BROWN_DYE, null),
            new ParticleStyle("Lava Drip", Particle.DRIPPING_LAVA, Material.LAVA_BUCKET, null),
            new ParticleStyle("Water Drop", Particle.DRIPPING_WATER, Material.WATER_BUCKET, null),
            new ParticleStyle("Water Splash", Particle.SPLASH, Material.BLUE_DYE, null),
            new ParticleStyle("Enchantment Words", Particle.ENCHANT, Material.BOOK, null),
            new ParticleStyle("Big White Smoke", Particle.EXPLOSION, Material.WHITE_CONCRETE, null),
            new ParticleStyle("White Sparkle", Particle.FIREWORK, Material.FIREWORK_ROCKET, null),
            new ParticleStyle("Flame", Particle.FLAME, Material.BLAZE_POWDER, null),
            new ParticleStyle("Foot Step", Particle.ASH, Material.LEATHER_BOOTS, null),
            new ParticleStyle("Emerald Sparkle", Particle.HAPPY_VILLAGER, Material.EMERALD, null),
            new ParticleStyle("Love Heart", Particle.HEART, Material.APPLE, null),
            new ParticleStyle("Huge Explosion", Particle.EXPLOSION_EMITTER, Material.TNT, null),
            new ParticleStyle("Explosion", Particle.EXPLOSION, Material.FIRE_CHARGE, null),
            new ParticleStyle("Black Smoke", Particle.LARGE_SMOKE, Material.BLACK_DYE, null),
            new ParticleStyle("Smoke", Particle.SMOKE, Material.GRAY_DYE, null),
            new ParticleStyle("Lava Debris", Particle.LAVA, Material.MAGMA_BLOCK, null),
            new ParticleStyle("Teal Magic", Particle.CRIT, Material.CYAN_DYE, null),
            new ParticleStyle("Black Swirls", Particle.ENTITY_EFFECT, Material.BLACK_CONCRETE, Color.BLACK),
            new ParticleStyle("Transparent Black Swirls", Particle.ENTITY_EFFECT, Material.GRAY_CONCRETE, Color.GRAY),
            new ParticleStyle("Musical Note", Particle.NOTE, Material.JUKEBOX, null),
            new ParticleStyle("Portal Effect", Particle.PORTAL, Material.ENDER_PEARL, null),
            new ParticleStyle("Red Smoke", Particle.DUST, Material.RED_DYE, Color.RED),
            new ParticleStyle("Slime Particles", Particle.ITEM_SLIME, Material.SLIME_BALL, null),
            new ParticleStyle("Snow Puffs", Particle.SNOWFLAKE, Material.SNOWBALL, null),
            new ParticleStyle("White Swirls", Particle.WITCH, Material.WHITE_DYE, null),
            new ParticleStyle("Black Specks", Particle.ASH, Material.COAL, null),
            new ParticleStyle("Purple Magic", Particle.WITCH, Material.PURPLE_DYE, null));

    private final Access access;
    private final Consumer<Entity> trackEntity;
    private final Map<UUID, PlotSettings> settings = new HashMap<>();
    private final Map<UUID, UUID> mobOwners = new HashMap<>();
    private final Map<UUID, List<ParticleMarker>> particles = new HashMap<>();
    private final Map<UUID, ParticleStyle> selectedParticle = new HashMap<>();
    private final Map<UUID, Long> nextStormEffectAt = new HashMap<>();
    private final Map<UUID, Long> groundChangeReadyAt = new HashMap<>();

    MasterBuildersPlotRuntime(Access access, Consumer<Entity> trackEntity) {
        this.access = access;
        this.trackEntity = trackEntity;
    }

    void begin(Iterable<Player> players) {
        settings.clear();
        mobOwners.clear();
        particles.clear();
        selectedParticle.clear();
        nextStormEffectAt.clear();
        groundChangeReadyAt.clear();
        for (Player player : players) {
            if (player == null) continue;
            settings.put(player.getUniqueId(), new PlotSettings());
            player.setFlySpeed(0.1F);
            player.getInventory().setItem(8, optionsItem());
        }
    }

    void tick(Iterable<Player> players) {
        long now = System.currentTimeMillis();
        UUID reviewTarget = access.isReviewing() ? access.currentReviewTarget() : null;
        PlotSettings reviewSettings = reviewTarget == null ? null : settings.get(reviewTarget);
        if (reviewTarget != null && reviewSettings != null && reviewSettings.weather == PlotWeather.STORMING
                && now >= nextStormEffectAt.getOrDefault(reviewTarget, 0L)) {
            nextStormEffectAt.put(reviewTarget, now + 8_000L);
            Player builder = Bukkit.getPlayer(reviewTarget);
            Location center = builder == null ? null : access.plotCenter(builder);
            if (center != null) {
                Location flash = center.clone().add(0.0D, 6.0D, 0.0D);
                for (Player viewer : players) {
                    if (viewer == null || !viewer.isOnline()) continue;
                    viewer.spawnParticle(Particle.ELECTRIC_SPARK, flash, 12, 2.0D, 2.5D, 2.0D, 0.02D);
                    viewer.playSound(flash, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25F, 1.4F);
                }
            }
        }
        for (Player viewer : players) {
            if (viewer == null || !viewer.isOnline()) continue;
            PlotSettings options = reviewSettings != null ? reviewSettings : settings.get(viewer.getUniqueId());
            if (options == null) continue;
            viewer.setPlayerTime(options.time, false);
            viewer.setPlayerWeather(options.weather == PlotWeather.SUNNY ? WeatherType.CLEAR : WeatherType.DOWNFALL);
            UUID settingsOwner = viewer.getUniqueId();
            if (reviewTarget == null && options.weather == PlotWeather.STORMING
                    && now >= nextStormEffectAt.getOrDefault(settingsOwner, 0L)) {
                nextStormEffectAt.put(settingsOwner, now + 8_000L);
                Location center = access.plotCenter(viewer);
                if (center != null) {
                    Location flash = center.clone().add(0.0D, 6.0D, 0.0D);
                    viewer.spawnParticle(Particle.ELECTRIC_SPARK, flash, 12, 2.0D, 2.5D, 2.0D, 0.02D);
                    viewer.playSound(flash, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.25F, 1.4F);
                }
            }
            if (!isOptionsItem(viewer.getInventory().getItem(8))) viewer.getInventory().setItem(8, optionsItem());
        }
        Set<UUID> visiblePlots = access.isReviewing() && reviewTarget != null
                ? Set.of(reviewTarget) : playersVisibleToBuilder(players);
        for (UUID ownerId : visiblePlots) {
            List<ParticleMarker> visibleMarkers = particles.getOrDefault(ownerId, List.of());
            Iterator<ParticleMarker> markers = visibleMarkers.iterator();
            while (markers.hasNext()) {
                ParticleMarker marker = markers.next();
                if (marker.location.getWorld() == null) {
                    markers.remove();
                    continue;
                }
                if (now - marker.lastShownAt >= 500L) {
                    marker.lastShownAt = now;
                    for (Player viewer : players) {
                        if (viewer != null && viewer.isOnline()
                                && (access.isReviewing() || viewer.getUniqueId().equals(ownerId))) {
                            Location particleAt = marker.location.clone().add(0.0D, 0.05D, 0.0D);
                            if (marker.style.particle == Particle.DUST && marker.style.colour != null) {
                                viewer.spawnParticle(Particle.DUST, particleAt, 2, 0.08D, 0.08D, 0.08D,
                                        new Particle.DustOptions(marker.style.colour, 1.0F));
                            } else if (marker.style.particle == Particle.ENTITY_EFFECT && marker.style.colour != null) {
                                viewer.spawnParticle(Particle.ENTITY_EFFECT, particleAt, 2, 0.08D, 0.08D,
                                        0.08D, marker.style.colour);
                            } else {
                                viewer.spawnParticle(marker.style.particle, particleAt, 2,
                                        0.08D, 0.08D, 0.08D, 0.0D);
                            }
                        }
                    }
                }
            }
        }
        mobOwners.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            return entity == null || !entity.isValid();
        });
    }

    void finish(Iterable<Player> players, Iterable<Entity> spawned) {
        for (Player player : players) {
            if (player == null || !player.isOnline()) continue;
            player.resetPlayerTime();
            player.resetPlayerWeather();
        }
        for (Entity entity : spawned) {
            if (entity != null && entity.isValid() && mobOwners.containsKey(entity.getUniqueId())) entity.remove();
        }
        settings.clear();
        mobOwners.clear();
        particles.clear();
        selectedParticle.clear();
        nextStormEffectAt.clear();
        groundChangeReadyAt.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOptionsItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!access.isBuilding(player)) return;
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && ChatColor.stripColor(item.getItemMeta().getDisplayName()).startsWith("Place Particle: ")) {
            if (!event.getAction().isRightClick()) return;
            event.setCancelled(true);
            placeParticle(player, event);
            return;
        }
        if (!isOptionsItem(item)) return;
        event.setCancelled(true);
        openOptions(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlotMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !(event.getView().getTopInventory().getHolder() instanceof PlotMenu menu)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!access.isBuilding(player) || !menu.owner.equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        switch (menu.kind) {
            case OPTIONS -> handleOptionsClick(player, slot, event.isShiftClick());
            case GROUND -> handleGroundClick(player, slot);
            case TIME -> handleTimeClick(player, slot);
            case WEATHER -> handleWeatherClick(player, slot);
            case PARTICLES -> handleParticlesClick(player, slot);
            case MOB -> handleMobClick(player, menu.entityId, slot);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMobInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        UUID ownerId = mobOwners.get(event.getRightClicked().getUniqueId());
        if (ownerId == null) return;
        event.setCancelled(true);
        if (!access.isBuilding(player) || !ownerId.equals(player.getUniqueId())) return;
        openMobMenu(player, event.getRightClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOptionsItemMove(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && isOptionsItem(event.getCurrentItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;
        Player owner = access.ownerAt(event.getLocation());
        if (owner == null || !access.isBuilding(owner)) return;
        if (event.getEntityType() == EntityType.GHAST) {
            event.setCancelled(true);
            owner.sendMessage(ChatColor.RED + "Ghasts cannot be used in Master Builders.");
            return;
        }
        long mobCount = mobOwners.values().stream().filter(owner.getUniqueId()::equals).count();
        if (!canAddMob(event.getEntityType(), (int) mobCount)) {
            event.setCancelled(true);
            owner.sendMessage(ChatColor.RED + "A plot can have at most " + MOB_LIMIT + " mobs.");
            return;
        }
        LivingEntity mob = event.getEntity();
        configureMob(mob, owner.getUniqueId());
        mobOwners.put(mob.getUniqueId(), owner.getUniqueId());
        trackEntity.accept(mob);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlotFluidFlow(BlockFromToEvent event) {
        if (!access.managesWorld(event.getBlock().getWorld())) return;
        Player owner = access.ownerAt(event.getBlock().getLocation());
        if (owner == null || !access.insidePlot(owner, event.getToBlock().getLocation())) event.setCancelled(true);
    }

    private void handleOptionsClick(Player player, int slot, boolean shiftClick) {
        switch (slot) {
            case 10 -> openGround(player);
            case 12 -> changeFlight(player, shiftClick);
            case 14 -> openParticles(player);
            case 16 -> openWeather(player);
            case 22 -> openTime(player);
            case 26 -> player.sendMessage(ChatColor.YELLOW + "Spawn an egg inside your plot, then right-click that mob to change its options.");
            default -> { return; }
        }
    }

    private void handleGroundClick(Player player, int slot) {
        if (slot < 0 || slot >= GROUNDS.size()) return;
        PlotSettings options = settings.get(player.getUniqueId());
        if (options == null) return;
        long now = System.currentTimeMillis();
        long readyAt = groundChangeReadyAt.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.65F);
            return;
        }
        options.ground = GROUNDS.get(slot);
        applyGround(player, options.ground);
        groundChangeReadyAt.put(player.getUniqueId(), now + 2_000L);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
        player.sendMessage(ChatColor.GREEN + "Plot ground changed to " + ChatColor.YELLOW + options.ground + ChatColor.GREEN + ".");
        openGround(player);
    }

    private void handleTimeClick(Player player, int slot) {
        if (slot < 0 || slot >= TIMES.size()) return;
        PlotSettings options = settings.get(player.getUniqueId());
        if (options == null) return;
        options.time = TIMES.get(slot);
        player.setPlayerTime(options.time, false);
        player.sendMessage(ChatColor.GREEN + "Your plot time has been updated.");
        openTime(player);
    }

    private void handleWeatherClick(Player player, int slot) {
        PlotSettings options = settings.get(player.getUniqueId());
        if (options == null || slot < 0 || slot > 2) return;
        options.weather = PlotWeather.values()[slot];
        player.setPlayerWeather(options.weather == PlotWeather.SUNNY ? WeatherType.CLEAR : WeatherType.DOWNFALL);
        player.sendMessage(ChatColor.GREEN + "Your plot weather is now " + ChatColor.YELLOW + options.weather.label + ChatColor.GREEN + ".");
        openWeather(player);
    }

    private void handleParticlesClick(Player player, int slot) {
        if (slot == 49) {
            particles.remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Your plot particles have been cleared.");
            openParticles(player);
            return;
        }
        if (slot < 0 || slot >= PARTICLES.size()) return;
        ParticleStyle style = PARTICLES.get(slot);
        selectedParticle.put(player.getUniqueId(), style);
        player.getInventory().addItem(namedItem(style.icon, "Place Particle: " + style.name,
                "Right-click inside your plot to place this effect."));
        player.closeInventory();
        player.sendMessage(ChatColor.GREEN + "Selected " + ChatColor.YELLOW + style.name + ChatColor.GREEN
                + ". Right-click within your plot to place it (" + particleCount(player.getUniqueId()) + "/" + PARTICLE_LIMIT + ").");
    }

    private void handleMobClick(Player player, UUID entityId, int slot) {
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        if (!(entity instanceof LivingEntity mob) || !mob.isValid()) {
            player.closeInventory();
            return;
        }
        if (slot == 22) {
            mobOwners.remove(entityId);
            mob.remove();
            player.sendMessage(ChatColor.GREEN + "Mob removed from your plot.");
            player.closeInventory();
            return;
        }
        if (slot == 10 && mob instanceof Ageable ageable) {
            if (ageable.isAdult()) ageable.setBaby(); else ageable.setAdult();
        } else if (slot == 12) {
            mob.setCollidable(!mob.isCollidable());
        } else if (slot == 14 && mob instanceof Slime slime) {
            slime.setSize(Math.max(1, slime.getSize() - 1));
        } else if (slot == 15 && mob instanceof Slime slime) {
            slime.setSize(Math.min(10, slime.getSize() + 1));
        } else if (slot == 16 && mob instanceof Villager villager) {
            Villager.Profession[] professions = Villager.Profession.values();
            villager.setProfession(professions[(villager.getProfession().ordinal() + 1) % professions.length]);
        } else if (slot == 18) {
            Location facing = mob.getLocation();
            facing.setDirection(player.getLocation().toVector().subtract(facing.toVector()));
            mob.teleport(facing);
        }
        openMobMenu(player, mob);
    }

    private void openOptions(Player player) {
        Inventory inventory = menu(player, MenuKind.OPTIONS, null, 27, "Master Builders Options");
        put(inventory, 10, Material.GRASS_BLOCK, "Change Ground", "Select a plot floor block.");
        put(inventory, 12, Material.FEATHER, "Flight Speed", "Current: " + flightLevel(player) + "/8",
                "Click to increase; shift-click to decrease.");
        put(inventory, 14, Material.NETHER_STAR, "Particles", "Choose and place up to 24 plot effects.");
        put(inventory, 16, Material.WATER_BUCKET, "Weather", "Current: " + settings.get(player.getUniqueId()).weather.label);
        put(inventory, 22, Material.CLOCK, "Time of Day", "Select one of the source game's 12 times.");
        put(inventory, 26, Material.SPAWNER, "Mob Shop", "Spawn an egg, then click your mob to configure it.");
        player.openInventory(inventory);
    }

    private void openGround(Player player) {
        Inventory inventory = menu(player, MenuKind.GROUND, null, 54, "Plot Ground Options");
        for (int i = 0; i < GROUNDS.size(); i++) put(inventory, i, GROUND_ICONS.get(i), GROUNDS.get(i).name(),
                settings.get(player.getUniqueId()).ground == GROUNDS.get(i) ? "Selected" : "Click to apply to your plot.");
        player.openInventory(inventory);
    }

    private void changeFlight(Player player, boolean decrease) {
        PlotSettings options = settings.get(player.getUniqueId());
        int level = flightLevel(player);
        options.flightLevel = level;
        float[] speeds = flightSpeeds();
        if (decrease) level = Math.max(1, level - 1); else level = Math.min(8, level + 1);
        options.flightLevel = level;
        player.setFlySpeed(speeds[level - 1]);
        player.sendMessage(ChatColor.GREEN + "Flight speed: " + Math.round(speeds[level - 1] * 100.0F)
                + "% (" + level + "/8).");
        openOptions(player);
    }

    private void openTime(Player player) {
        Inventory inventory = menu(player, MenuKind.TIME, null, 27, "Plot Time of Day");
        for (int i = 0; i < TIMES.size(); i++) put(inventory, i, Material.CLOCK,
                String.format(java.util.Locale.ROOT, "%02d:00", (i * 3) % 24),
                settings.get(player.getUniqueId()).time == TIMES.get(i) ? "Selected" : "Click to set plot time.");
        player.openInventory(inventory);
    }

    private void openWeather(Player player) {
        Inventory inventory = menu(player, MenuKind.WEATHER, null, 27, "Plot Weather");
        Material[] icons = {Material.SUNFLOWER, Material.WATER_BUCKET, Material.LIGHTNING_ROD};
        for (int i = 0; i < PlotWeather.values().length; i++) put(inventory, i, icons[i], PlotWeather.values()[i].label,
                settings.get(player.getUniqueId()).weather == PlotWeather.values()[i] ? "Selected" : "Click to apply.");
        player.openInventory(inventory);
    }

    private void openParticles(Player player) {
        Inventory inventory = menu(player, MenuKind.PARTICLES, null, 54, "Plot Particles");
        for (int i = 0; i < PARTICLES.size(); i++) put(inventory, i, PARTICLES.get(i).icon,
                PARTICLES.get(i).name, "Click, then right-click to place.");
        put(inventory, 49, Material.BARRIER, "Clear particles", "Remove all placed particles from your plot.");
        player.openInventory(inventory);
    }

    private void openMobMenu(Player player, Entity entity) {
        Inventory inventory = menu(player, MenuKind.MOB, entity.getUniqueId(), 27, "Mob Options");
        if (entity instanceof Ageable ageable) put(inventory, 10, Material.BLAZE_ROD,
                ageable.isAdult() ? "Make Baby" : "Make Adult", "Toggle this mob's age.");
        if (entity instanceof LivingEntity living) {
            put(inventory, 12, Material.FEATHER, living.isCollidable() ? "Disable Pushing" : "Allow Pushing",
                    "Pushing is currently " + (living.isCollidable() ? "enabled" : "disabled") + ".");
            put(inventory, 18, Material.GHAST_TEAR, "Look at Me", "Turn this mob to face you.");
        }
        if (entity instanceof Slime slime) {
            put(inventory, 14, Material.CLAY_BALL, "Decrease Size", "Current size " + slime.getSize() + "; minimum 1.");
            put(inventory, 15, Material.CLAY, "Increase Size", "Current size " + slime.getSize() + "; maximum 10.");
        }
        if (entity instanceof Villager villager) put(inventory, 16, Material.EMERALD,
                "Change Profession", "Currently " + villager.getProfession().name() + ".");
        put(inventory, 22, Material.BARRIER, "Remove Mob", "Delete this mob from your plot.");
        player.openInventory(inventory);
    }

    private Inventory menu(Player player, MenuKind kind, UUID entityId, int size, String title) {
        PlotMenu holder = new PlotMenu(player.getUniqueId(), kind, entityId);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.inventory = inventory;
        return inventory;
    }

    private void applyGround(Player player, Material material) {
        Location plotCenter = access.plotCenter(player);
        if (plotCenter == null) return;
        for (int x = plotCenter.getBlockX() - 5; x <= plotCenter.getBlockX() + 5; x++) {
            for (int z = plotCenter.getBlockZ() - 5; z <= plotCenter.getBlockZ() + 5; z++) {
                Block block = plotCenter.getWorld().getBlockAt(x, 63, z);
                if (!access.insidePlot(player, block.getLocation().add(0.0D, 1.0D, 0.0D))) continue;
                block.setType(material, false);
            }
        }
    }

    private void placeParticle(Player player, PlayerInteractEvent event) {
        PlotSettings options = settings.get(player.getUniqueId());
        if (options == null) return;
        List<ParticleMarker> markers = particles.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());
        Location point = player.getEyeLocation().add(player.getLocation().getDirection()).add(0.0D, -0.5D, 0.0D);
        point.setX(Math.floor(point.getX()) + 0.5D);
        point.setY(Math.floor(point.getY()) + 0.1D);
        point.setZ(Math.floor(point.getZ()) + 0.5D);
        boolean inPlot = access.insidePlot(player, point);
        if (!canPlaceParticle(inPlot, markers.size())) {
            if (markers.size() >= PARTICLE_LIMIT) {
                player.sendMessage(ChatColor.RED + "You cannot place more than " + PARTICLE_LIMIT + " particles.");
                return;
            }
            player.sendMessage(ChatColor.RED + "You can only place particles inside your plot.");
            return;
        }
        ParticleStyle style = selectedParticle.get(player.getUniqueId());
        if (style == null) return;
        markers.add(new ParticleMarker(point, style));
        ItemStack held = event.getItem();
        if (held != null && held.getAmount() > 1) held.setAmount(held.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);
        player.sendMessage(ChatColor.GREEN + "Placed " + style.name + " (" + markers.size() + "/" + PARTICLE_LIMIT + ").");
    }

    private void configureMob(LivingEntity mob, UUID ownerId) {
        mob.setInvulnerable(true);
        mob.setAI(false);
        mob.setCollidable(false);
        mob.setRemoveWhenFarAway(false);
        if (mob instanceof Ageable ageable) ageable.setAdult();
        if (mob instanceof Slime slime) slime.setSize(1);
    }

    private int particleCount(UUID id) { return particles.getOrDefault(id, List.of()).size(); }

    private Set<UUID> playersVisibleToBuilder(Iterable<Player> players) {
        java.util.HashSet<UUID> visible = new java.util.HashSet<>();
        for (Player player : players) {
            if (player != null && player.isOnline() && access.isBuilding(player)) visible.add(player.getUniqueId());
        }
        return visible;
    }

    private int flightLevel(Player player) {
        PlotSettings options = settings.get(player.getUniqueId());
        if (options != null && player.getFlySpeed() == flightSpeeds()[Math.max(0, Math.min(7, options.flightLevel - 1))]) {
            return options.flightLevel;
        }
        float speed = player.getFlySpeed();
        int best = 1;
        double distance = Double.MAX_VALUE;
        float[] speeds = flightSpeeds();
        for (int i = 0; i < speeds.length; i++) {
            double next = Math.abs(speed - speeds[i]);
            if (next < distance) { distance = next; best = i + 1; }
        }
        return best;
    }

    private static float[] flightSpeeds() {
        float[] values = new float[8];
        for (int i = 0; i < values.length; i++) values[i] = flightSpeedForLevel(i + 1);
        return values;
    }

    static boolean canAddMob(EntityType type, int ownedMobCount) {
        return type != null && type != EntityType.GHAST && ownedMobCount >= 0 && ownedMobCount < MOB_LIMIT;
    }

    static boolean canPlaceParticle(boolean inOwnPlot, int currentParticleCount) {
        return inOwnPlot && currentParticleCount >= 0 && currentParticleCount < PARTICLE_LIMIT;
    }

    static float flightSpeedForLevel(int level) {
        if (level < 1 || level > 8) throw new IllegalArgumentException("flight level must be 1-8");
        return 0.1F + (0.9F * (level - 1) / 7.0F);
    }

    static int groundOptionCount() { return GROUNDS.size(); }
    static int particleOptionCount() { return PARTICLES.size(); }

    private ItemStack optionsItem() { return namedItem(Material.DIAMOND, "Build Options", "Change ground, flight speed, particles, weather and time."); }
    private boolean isOptionsItem(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Build Options"));
    }

    private ItemStack namedItem(Material material, String name, String loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            meta.setLore(List.of(ChatColor.GRAY + loreText));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void put(Inventory inventory, int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(java.util.Arrays.stream(lore).map(text -> ChatColor.GRAY + text).toList());
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private enum MenuKind { OPTIONS, GROUND, TIME, WEATHER, PARTICLES, MOB }
    private enum PlotWeather {
        SUNNY("Sunny"), RAINING("Raining"), STORMING("Storming");
        final String label;
        PlotWeather(String label) { this.label = label; }
    }

    private static final class PlotSettings {
        Material ground = Material.GRASS_BLOCK;
        long time = 6_000L;
        PlotWeather weather = PlotWeather.SUNNY;
        int flightLevel = 1;
    }

    private static final class ParticleMarker {
        final Location location;
        final ParticleStyle style;
        long lastShownAt;
        ParticleMarker(Location location, ParticleStyle style) { this.location = location.clone(); this.style = style; }
    }

    private record ParticleStyle(String name, Particle particle, Material icon, Color colour) { }

    private static final class PlotMenu implements InventoryHolder {
        final UUID owner;
        final MenuKind kind;
        final UUID entityId;
        Inventory inventory;
        PlotMenu(UUID owner, MenuKind kind, UUID entityId) { this.owner = owner; this.kind = kind; this.entityId = entityId; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
