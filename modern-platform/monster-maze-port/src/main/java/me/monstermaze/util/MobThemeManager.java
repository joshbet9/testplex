package me.monstermaze.util;
import me.monstermaze.MonsterMazePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class MobThemeManager implements Listener {
    public static final String MOB_METADATA = "monstermaze_mob";
    private static final int SLOT = 6;
    /**
     * Java 26.3 mob forms with at most a two-block HORIZONTAL maze footprint.
     * This is not a two-block height limit: Wither Skeletons, Endermen, Iron
     * Golems, Wardens and other tall but narrow forms remain eligible.
     * Do not include huge horizontal forms (Ender Dragon, Ghast, Happy Ghast,
     * Giant, Wither boss). Slime/Magma/Sulfur Cube default client-only renders
     * use the small form; do not send enlarged size metadata to fake entities.
     *
     * The server's actual mobs remain Snow Golems; these are viewer-only models.
     */
    /**
     * Viewer-local cosmetic mobs restricted to at most TWO SQUARE BLOCKS of
     * horizontal (X/Z) footprint, not two blocks of vertical height.
     * In Java's base entity dimensions the footprint is width * width.
     * Exclude oversized Sniffer (1.9), Camel/Camel Husk (1.7),
     * Elder Guardian (1.9975) and Ravager (1.95) even though several of
     * those were offered previously. Also exclude enormous boss models.
     * Tall/narrow Wither Skeleton (0.7) and Warden (0.9) remain allowed.
     */
    private static final List<String> THEMES = Collections.unmodifiableList(Arrays.asList(
        "map_default", "allay", "armadillo", "axolotl", "bat", "bee", "blaze",
        "bogged", "breeze", "cat", "cave_spider", "chicken", "cod", "copper_golem",
        "cow", "creaking", "creeper", "dolphin", "donkey", "drowned", "enderman",
        "endermite", "evoker", "fox", "frog", "glow_squid", "goat", "guardian",
        "hoglin", "horse", "husk", "illusioner", "iron_golem", "llama", "magma_cube",
        "mooshroom", "mule", "nautilus", "ocelot", "panda", "parched", "parrot",
        "phantom", "pig", "piglin", "piglin_brute", "pillager", "polar_bear", "pufferfish",
        "rabbit", "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse",
        "slime", "snowman", "spider", "squid", "stray", "strider", "sulfur_cube",
        "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator",
        "wandering_trader", "warden", "witch", "wither_skeleton", "wolf", "zoglin", "zombie",
        "zombie_horse", "zombie_nautilus", "zombie_pigman", "zombie_villager", "random"));

    static boolean fitsMazeFloorFootprint(double xWidth, double zLength) {
        return Double.isFinite(xWidth) && Double.isFinite(zLength)
                && xWidth > 0.0D && zLength > 0.0D
                && xWidth * zLength <= 2.0D;
    }

    private final MonsterMazePlugin plugin;

    private final File file;
    private final Map<UUID,String> choices = new LinkedHashMap<UUID,String>();
    private final Map<String,PadVisual> pads = new LinkedHashMap<String,PadVisual>();
    private boolean warnedMissingDisguise;
    private final Map<UUID, Integer> lastVirtualMobClickTick = new HashMap<>();
    public MobThemeManager(MonsterMazePlugin plugin) {
        this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"player-mob-themes.yml"); load();
        Bukkit.getPluginManager().registerEvents(this,plugin.getHostPlugin());
    }
    public String selected(Player player) {
        if(player==null)return defaultTheme();
        String choice = normalize(choices.getOrDefault(player.getUniqueId(), "map_default"));
        List<String> enabled = enabledThemes(player);
        return enabled.contains(choice) ? choice : enabled.get(0);
    }
    public String forViewer(Player viewer, UUID entityId) {
        String s=selected(viewer); if("map_default".equals(s))s=defaultTheme(); return "random".equals(s)?randomConcrete(viewer.getUniqueId(),entityId):s;
    }
    public void giveSelector(Player p) {
        if(p==null||!p.isOnline())return;
        String choice=selected(p),iconTheme="map_default".equals(choice)?defaultTheme():choice;
        Material icon=selectorIcon(iconTheme);ItemStack item=new ItemStack(icon==null?Material.PAPER:icon);
        ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE+"Mob Theme: "+ChatColor.YELLOW+displayName(choice));
        meta.setLore(Arrays.asList(ChatColor.GRAY+"Right-click to cycle mob themes",ChatColor.GRAY+"Uses vanilla mob heads or spawn eggs",ChatColor.GRAY+"Map Default uses the map's normal mob and colours."));
        item.setItemMeta(meta);p.getInventory().setItem(SLOT,item);p.updateInventory();
    }
    static Material selectorIcon(String theme){
        if("random".equals(theme))return Material.NETHER_STAR;
        if("creeper".equals(theme))return Material.CREEPER_HEAD;
        if("skeleton".equals(theme))return Material.SKELETON_SKULL;
        if("wither_skeleton".equals(theme))return Material.WITHER_SKELETON_SKULL;
        if("bogged".equals(theme)||"stray".equals(theme)||"parched".equals(theme))return Material.SKELETON_SKULL;
        if("zombie".equals(theme))return Material.ZOMBIE_HEAD;
        if("snowman".equals(theme))return Material.PUMPKIN;
        return spawnEggMaterial(theme);
    }
    static Material spawnEggMaterial(String theme){
        String id=theme==null?"creeper":theme.toLowerCase(Locale.ROOT);
        if("zombie_pigman".equals(id))id="zombified_piglin";
        if("snowman".equals(id))id="snow_golem";
        Material egg=Material.matchMaterial(id.toUpperCase(Locale.ROOT)+"_SPAWN_EGG");
        if(egg==null)egg=Material.PAPER;
        return egg;
    }
    public void registerSafePad(Location center) {
        if (center == null || center.getWorld() == null) return;
        PadVisual pad = new PadVisual(center.clone(), center.getBlockY() - 1);
        pads.put(key(center), pad);
        refreshSafePad(center);
        schedulePadRefresh(pad, null, new long[] {1L, 2L, 4L, 10L, 20L, 40L, 60L, 100L});
    }
    /** True only for the centre surface block tracked by a currently active or decaying safe pad. */
    public boolean isSafePadCenterSurface(Location location) {
        if(location==null||location.getWorld()==null)return false;
        for(PadVisual pad:pads.values()) {
            if(pad.center.getWorld()!=null
                    &&pad.center.getWorld().equals(location.getWorld())
                    &&pad.center.getBlockX()==location.getBlockX()
                    &&pad.surfaceY==location.getBlockY()
                    &&pad.center.getBlockZ()==location.getBlockZ())return true;
        }
        return false;
    }

    public void setSafePadBeaconActive(Location center, boolean active) {
        refreshSafePad(center);
        PadVisual pad = center == null ? null : pads.get(key(center));
        if (pad != null) schedulePadRefresh(pad, null, new long[] {1L, 2L, 4L, 10L, 20L, 40L, 60L});
    }

    public void refreshSafePad(Location center) {
        PadVisual pad = center == null ? null : pads.get(key(center));
        if (pad == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) if (visibleTo(player, pad)) paintPad(player, pad);
    }

    private void schedulePadRefresh(final PadVisual pad, final Player viewer, long[] delays) {
        if (pad == null) return;
        for (long delay : delays) Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(), new Runnable() {
            @Override public void run() {
                if (pads.get(key(pad.center)) != pad) return;
                if (viewer == null) refreshSafePad(pad.center);
                else if (visibleTo(viewer, pad)) paintPad(viewer, pad);
            }
        }, delay);
    }

    

    private PadVisual findPadForBlock(Location location) {
        if (location == null || location.getWorld() == null) return null;
        for (PadVisual pad : pads.values()) {
            if (pad.center.getWorld() == null || !pad.center.getWorld().equals(location.getWorld())) continue;
            if (Math.abs(location.getBlockX() - pad.center.getBlockX()) > 2
                    || Math.abs(location.getBlockZ() - pad.center.getBlockZ()) > 2) continue;
            if (location.getBlockY() <= pad.surfaceY && location.getBlockY() >= pad.surfaceY - 2) return pad;
        }
        return null;
    }

    public void refreshPlayerPads(Player player) {
        if(player==null||!player.isOnline())return;
        for(PadVisual p:pads.values())if(visibleTo(player,p))paintPad(player,p);
    }

    public void unregisterSafePad(Location center) {
        if(center==null)return; PadVisual pad=pads.remove(key(center)); if(pad==null||pad.center.getWorld()==null)return;
        World w=pad.center.getWorld(); int cx=pad.center.getBlockX(),cz=pad.center.getBlockZ();
        for(Player p:Bukkit.getOnlinePlayers())if(visibleTo(p,pad)) {
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){Location at=new Location(w,cx+x,pad.surfaceY,cz+z);p.sendBlockChange(at,w.getBlockAt(at).getBlockData().clone());}
            Location tint=new Location(w,cx,pad.surfaceY+3,cz);p.sendBlockChange(tint,w.getBlockAt(tint).getBlockData().clone());
        }
    }
    public void shutdown(){save();}
    @EventHandler public void onThemeClick(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND)return;
        Action a=e.getAction(); if(a!=Action.RIGHT_CLICK_AIR&&a!=Action.RIGHT_CLICK_BLOCK)return;
        Player p=e.getPlayer(); if(!isSelector(e.getItem()))return;
        e.setCancelled(true);
        cycleThemeFromVirtualMob(p);
    }

    /**
     * Looking at a mob sends an entity-interact packet rather than RIGHT_CLICK_AIR.
     * Handle that packet using the exact same personal-view selector and only the
     * main hand; the offhand would otherwise advance the choice twice.
     */
    @EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST)
    public void onThemeEntityClick(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (!shouldCycleSelectorEntityInteract(e.getHand() == EquipmentSlot.HAND,
                isSelector(p.getInventory().getItemInMainHand()))) return;
        e.setCancelled(true);
        cycleThemeFromVirtualMob(p);
    }

    /**
     * PacketEvents knows about client-only display mob IDs, which Bukkit cannot
     * resolve to a real PlayerInteractEntityEvent. This callback runs on the
     * Bukkit primary thread after a legitimate main-hand click on one of the
     * viewer's own cosmetic mobs; never accept arbitrary packet IDs.
     */
    public void cycleThemeFromVirtualMob(Player p) {
        if (p == null || !p.isOnline()
                || !isSelector(p.getInventory().getItemInMainHand())) return;
        int tick = Bukkit.getCurrentTick();
        Integer last = lastVirtualMobClickTick.put(p.getUniqueId(), tick);
        if (last != null && last == tick) return; // duplicate INTERACT/INTERACT_AT
        cycleTheme(p);
    }

    private void cycleTheme(Player p) {
        List<String> enabled = enabledThemes(p);
        String c = nextTheme(selected(p), enabled);
        choices.put(p.getUniqueId(), c);
        save();giveSelector(p);plugin.refreshPlayerMobThemes(p);refreshPlayerPads(p);
        if(!plugin.hasPerPlayerMobDisguises()&&!warnedMissingDisguise){p.sendMessage(ChatColor.RED+"Mob appearance needs PacketEvents or ProtocolLib; Safe Pad and beacon colours still update.");warnedMissingDisguise=true;}
        p.sendMessage(ChatColor.LIGHT_PURPLE+"Mob theme: "+ChatColor.YELLOW+displayName(selected(p))+ChatColor.GRAY+" (personal view).");
    }

    private List<String> enabledThemes(Player player) {
        java.util.Set<String> configured = plugin.getHostPlugin()
                .selectedCosmeticOptions(player, "monster-maze", "mob-sets");
        return allowedThemes(configured);
    }

    public static List<String> allowedThemes(java.util.Collection<String> configured) {
        if (configured == null) return THEMES;
        java.util.Set<String> selected = new java.util.LinkedHashSet<>();
        for (String choice : configured) {
            String recognized = recognizedTheme(choice);
            if (recognized != null) selected.add(recognized);
        }
        List<String> enabled = THEMES.stream().filter(selected::contains).toList();
        return enabled.isEmpty() ? List.of("map_default") : enabled;
    }

    static String nextTheme(String current, List<String> enabledThemes) {
        if (enabledThemes == null || enabledThemes.isEmpty()) return "map_default";
        int index = enabledThemes.indexOf(normalize(current));
        return enabledThemes.get((index + 1) % enabledThemes.size());
    }
    @EventHandler(priority=org.bukkit.event.EventPriority.MONITOR)
    public void refreshTintAfterPadInteraction(final PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if ((action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK)
                || event.getClickedBlock() == null) return;
        PadVisual pad = findPadForBlock(event.getClickedBlock().getLocation());
        if (pad != null) {
            schedulePadRefresh(pad, null, new long[] {1L, 2L, 4L, 10L, 20L, 40L, 60L});
        }
    }

    @EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPadBlockDamage(BlockDamageEvent event) {
        PadVisual pad = findPadForBlock(event.getBlock().getLocation());
        if (pad == null) return;
        event.setCancelled(true);
        schedulePadRefresh(pad, null, new long[] {1L, 2L, 4L, 10L, 20L, 40L});
    }

    @EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPadBlockBreak(BlockBreakEvent event) {
        PadVisual pad = findPadForBlock(event.getBlock().getLocation());
        if (pad == null) return;
        event.setCancelled(true);
        schedulePadRefresh(pad, null, new long[] {1L, 2L, 4L, 10L, 20L, 40L});
    }
    @EventHandler(priority=org.bukkit.event.EventPriority.HIGHEST) public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player))return; Player p=(Player)e.getWhoClicked();
        boolean playerInventory = e.getClickedInventory() == p.getInventory();
        boolean selectorClicked = isSelector(e.getCurrentItem());
        if (shouldCycleSelectorInventoryClick(playerInventory, selectorClicked, e.isRightClick())) {
            e.setCancelled(true);
            // Never replace the clicked ItemStack during InventoryClickEvent: some server
            // inventory synchronization paths overwrite that mutation at event completion.
            Bukkit.getScheduler().runTask(plugin.getHostPlugin(), () -> {
                if (p.isOnline() && isSelector(p.getInventory().getItem(SLOT))) cycleTheme(p);
            });
            return;
        }
        if(selectorClicked||isSelector(e.getCursor())||e.getHotbarButton()==SLOT
                ||(playerInventory&&e.getSlot()==SLOT))e.setCancelled(true);
    }

    static boolean shouldCycleSelectorEntityInteract(boolean mainHand, boolean selectorHeld) {
        return mainHand && selectorHeld;
    }

    static boolean shouldCycleSelectorInventoryClick(boolean playerInventory, boolean selectorClicked,
                                                       boolean rightClick) {
        return playerInventory && selectorClicked && rightClick;
    }
    @EventHandler public void onDrop(PlayerDropItemEvent e){if(isSelector(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler public void onJoin(PlayerJoinEvent e){laterRefresh(e.getPlayer());}
    @EventHandler public void onQuit(PlayerQuitEvent e){
        lastVirtualMobClickTick.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent e){laterRefresh(e.getPlayer());}
    @EventHandler public void onTeleport(PlayerTeleportEvent e){laterRefresh(e.getPlayer());}
    @EventHandler public void onMove(PlayerMoveEvent e) {
        Location a=e.getFrom(),b=e.getTo(); if(b==null)return;
        if(a.getWorld().equals(b.getWorld())&&(a.getBlockX()>>4)==(b.getBlockX()>>4)&&(a.getBlockZ()>>4)==(b.getBlockZ()>>4))return;
        laterRefresh(e.getPlayer());
    }
    private void laterRefresh(final Player p){Bukkit.getScheduler().runTaskLater(plugin.getHostPlugin(),new Runnable(){public void run(){if(p.isOnline())refreshPlayerPads(p);}},2L);}
    private void paintPad(Player p,PadVisual pad) {
        World w=pad.center.getWorld();int cx=pad.center.getBlockX(),cz=pad.center.getBlockZ();
        BlockData tint=padMaterial(p).createBlockData();
        boolean mapDefault="map_default".equals(selected(p));
        boolean customPalette=customObjectiveColor(p,"pad")!=null||customObjectiveColor(p,"beacon")!=null;
        BlockData beamGlass=glassMaterial(p).createBlockData();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            Location at=new Location(w,cx+x,pad.surfaceY,cz+z);Block real=w.getBlockAt(at);BlockData visual=real.getBlockData().clone();
            if(!mapDefault||customPalette){
                if(x==0&&z==0&&real.getType()==Material.GLASS)visual=beamGlass.clone();
                else if(real.getType()==Material.LIME_TERRACOTTA)visual=tint.clone();
            }
            p.sendBlockChange(at,visual);
        }
        Location above=new Location(w,cx,pad.surfaceY+3,cz);Block actual=w.getBlockAt(above);p.sendBlockChange(above,actual.getBlockData().clone());
    }
    private Material padMaterial(Player p){String color=customObjectiveColor(p,"pad");if(color==null)color=color(palette(p));Material m=Material.matchMaterial(color+"_TERRACOTTA");return m==null?Material.LIME_TERRACOTTA:m;}
    private Material glassMaterial(Player p){String color=customObjectiveColor(p,"beacon");if(color==null)color=color(palette(p));Material m=Material.matchMaterial(color+"_STAINED_GLASS");return m==null?Material.LIME_STAINED_GLASS:m;}
    private String customObjectiveColor(Player player,String part){
        String mob=normalize(selected(player));
        String group=part+"-"+mob;
        String selected=plugin.getHostPlugin().selectedCosmeticOption(player,"monster-maze",group);
        if(selected==null||selected.equals(group+"-inherit"))return null;
        String prefix=group+"-";
        if(!selected.startsWith(prefix))return null;
        String color=selected.substring(prefix.length()).toUpperCase(Locale.ROOT);
        return org.bukkit.Material.matchMaterial(color+"_TERRACOTTA")!=null?color:null;
    }

    private boolean visibleTo(Player p,PadVisual d){if(p==null||!p.isOnline()||d.center.getWorld()==null||!p.getWorld().equals(d.center.getWorld()))return false;Location l=p.getLocation();return Math.abs(l.getX()-d.center.getX())<=192&&Math.abs(l.getZ()-d.center.getZ())<=192;}
    private String palette(Player p){String s=selected(p);if("map_default".equals(s))return s;return "random".equals(s)?randomConcrete(p.getUniqueId(),null):s;}
    private String defaultTheme(){try{String s=plugin.getMapManager()==null?null:plugin.getMapManager().activeMob();return normalize(s==null?"creeper":s);}catch(Throwable ignored){return "creeper";}}
    private static String recognizedTheme(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String id = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("pig_zombie".equals(id) || "zombified_piglin".equals(id)) id = "zombie_pigman";
        if ("snow_golem".equals(id)) id = "snowman";
        if ("mushroom_cow".equals(id)) id = "mooshroom";
        if ("eyeofender".equals(id) || "eye_of_ender".equals(id)) id = "enderman";
        return THEMES.contains(id) ? id : null;
    }
    static String normalize(String s) {
        String recognized = recognizedTheme(s);
        return recognized == null ? "creeper" : recognized;
    }
    static String randomConcrete(UUID viewer,UUID entity){int h=viewer.hashCode();if(entity!=null)h=31*h+entity.hashCode();return THEMES.get(1+Math.floorMod(h,THEMES.size()-2));}
    static String color(String s) {
        switch(normalize(s)){
            case "creeper":case "slime":return "LIME";case "skeleton":return "LIGHT_GRAY";case "spider":return "BLACK";
            case "silverfish":case "bat":case "wolf":return "GRAY";case "zombie":return "GREEN";
            case "zombie_pigman":case "magma_cube":case "mooshroom":return "RED";case "enderman":case "witch":return "PURPLE";
            case "cave_spider":return "LIGHT_BLUE";case "blaze":case "villager":return "ORANGE";case "endermite":return "MAGENTA";
            case "guardian":return "CYAN";case "pig":case "rabbit":return "PINK";case "sheep":case "chicken":case "snowman":return "WHITE";
            case "cow":case "horse":case "armadillo":return "BROWN";case "squid":case "allay":return "BLUE";
            case "ocelot":case "bee":return "YELLOW";case "axolotl":case "parrot":return "PINK";
            case "fox":return "ORANGE";case "goat":case "panda":return "LIGHT_GRAY";
            case "frog":case "turtle":case "bogged":case "drowned":case "copper_golem":return "GREEN";
            case "wither_skeleton":case "creaking":case "shulker":return "BLACK";
            case "warden":case "nautilus":case "zombie_nautilus":case "glow_squid":return "CYAN";
            case "stray":case "parched":return "LIGHT_GRAY";
            case "husk":case "donkey":case "mule":case "llama":case "trader_llama":return "BROWN";
            case "sulfur_cube":case "breeze":case "tadpole":return "YELLOW";
            default:return "LIME";
        }
    }
    static String displayName(String s){if("map_default".equals(s))return "Map Default";if("random".equals(s))return "Random Mob";StringBuilder b=new StringBuilder();for(String part:normalize(s).split("_")){if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return b.toString();}
    private static boolean isSelector(ItemStack i){if(i==null||!i.hasItemMeta())return false;ItemMeta m=i.getItemMeta();return m!=null&&m.hasDisplayName()&&ChatColor.stripColor(m.getDisplayName()).startsWith("Mob Theme:");}
    private String key(Location l){return l.getWorld().getUID()+":"+l.getBlockX()+":"+l.getBlockY()+":"+l.getBlockZ();}
    private void load(){if(!file.exists())return;YamlConfiguration y=YamlConfiguration.loadConfiguration(file);for(String id:y.getKeys(false)){try{choices.put(UUID.fromString(id),normalize(y.getString(id)));}catch(IllegalArgumentException ignored){}}}
    private void save(){try{File parent=file.getParentFile();if(parent!=null&&!parent.exists())parent.mkdirs();YamlConfiguration y=new YamlConfiguration();for(Map.Entry<UUID,String> e:choices.entrySet())y.set(e.getKey().toString(),e.getValue());y.save(file);}catch(IOException ex){plugin.getLogger().warning("Could not save mob-theme choices: "+ex.getMessage());}}
    private static final class PadVisual{final Location center;final int surfaceY;PadVisual(Location c,int y){center=c;surfaceY=y;}}
}
