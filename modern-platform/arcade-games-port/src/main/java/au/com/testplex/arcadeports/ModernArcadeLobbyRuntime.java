package au.com.testplex.arcadeports;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Legacy-style kit/team lobby pads, cosmetics demo showcase and Bacon Brawl NPC test bots. */
public final class ModernArcadeLobbyRuntime implements Listener {
    private static final String KIT_TITLE = "Kit Select";
    private static final String QUEUE_TITLE = "Team Select | Queue";
    private static final String BOT_MENU_PREFIX = "Bot Cosmetics | ";
    private static final String[] THEMES = {"porky", "inferno", "frost", "slime", "royal", "confetti"};
    private static final String[] KITS = {"el-muchacho-pigo", "mama-piggles", "pig"};
    private static final String[] KIT_NAMES = {"El Muchacho Pigo", "Mama Piggles", "Pig"};
    private static final int[] KIT_SLOTS = {11, 13, 15};

    private final JavaPlugin plugin;
    private final ArcadePortManager manager;
    private final ModernArcadeLobby lobby;
    private final NamespacedKey actionKey, themeKey, botKey, formKey, displayKey, kitNpcKey, teamNpcKey;
    private final Map<String, Bot> bots = new LinkedHashMap<>();
    private final List<Entity> showcase = new ArrayList<>();
    private final List<Entity> kitNpcs = new ArrayList<>();
    private Entity teamNpc;
    private ArcadePortGame selectedGame = ArcadePortGame.BACON_BRAWL;
    private BukkitTask task;

    ModernArcadeLobbyRuntime(JavaPlugin plugin, ArcadePortManager manager, ModernArcadeLobby lobby) {
        this.plugin = plugin; this.manager = manager; this.lobby = lobby;
        actionKey = new NamespacedKey(plugin, "lobby-runtime-action");
        themeKey = new NamespacedKey(plugin, "lobby-bot-theme");
        botKey = new NamespacedKey(plugin, "test-bot-name");
        formKey = new NamespacedKey(plugin, "test-bot-form");
        displayKey = new NamespacedKey(plugin, "legacy-lobby-datapoint");
        kitNpcKey = new NamespacedKey(plugin, "lobby-kit-npc");
        teamNpcKey = new NamespacedKey(plugin, "lobby-team-npc");
        loadSavedBots();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public void setSelectedGame(ArcadePortGame game) {
        if (game == null) return;
        selectedGame = game;
        plugin.getConfig().set("lobby-selected-game", game.id());
        plugin.saveConfig();
        refreshDisplays();
        refreshKitAndTeamNpcs();
        refreshShowcase();
    }

    public void refreshDisplays() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        String name = selectedGame.displayName();
        display(world, "kit-text", ChatColor.GREEN + "" + ChatColor.BOLD + "KIT SELECT | " + name);
        display(world, "team-text", ChatColor.BLUE + "" + ChatColor.BOLD + "TEAM / QUEUE | " + name);
        display(world, "kit-display", ChatColor.GREEN + "Choose a " + name + " kit");
        display(world, "team-display", ChatColor.BLUE + (selectedGame == ArcadePortGame.BACON_BRAWL ? "FREE FOR ALL" : "TEAM QUEUE")
                + ChatColor.WHITE + " | Queued: " + manager.queuedCount(selectedGame) + ChatColor.GRAY + " | Bots: " + bots.size());
    }

    private void display(World world, String id, String title) {
        for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
            if (id.equals(stand.getPersistentDataContainer().get(displayKey, PersistentDataType.STRING))) {
                stand.setCustomName(title);
            }
        }
    }

    private void refreshKitAndTeamNpcs() {
        World world = lobby.ensureWorld();
        for (Entity e : new ArrayList<>(kitNpcs)) if (e != null && e.isValid()) e.remove();
        kitNpcs.clear();
        if (teamNpc != null && teamNpc.isValid()) teamNpc.remove();
        teamNpc = null;
        if (world == null || selectedGame != ArcadePortGame.BACON_BRAWL) return;
        double[] z = {-2.0D, 0.0D, 2.0D};
        for (int i=0; i<KITS.length; i++) {
            Location at = new Location(world, -16.5D, ModernArcadeLobby.FLOOR_Y + 1.0D, z[i] + 0.5D);
            LivingEntity npc;
            if (i == 2) {
                Sheep sheep = world.spawn(at, Sheep.class); sheep.setColor(DyeColor.PINK); npc = sheep;
            } else npc = world.spawn(at, Pig.class);
            npc.setCustomName(ChatColor.YELLOW + KIT_NAMES[i]); npc.setCustomNameVisible(true);
            npc.setInvulnerable(true); npc.setRemoveWhenFarAway(false); npc.setPersistent(false); npc.setSilent(true);
            if (npc instanceof Mob mob) mob.setAI(false);
            npc.getPersistentDataContainer().set(kitNpcKey, PersistentDataType.STRING, KITS[i]);
            kitNpcs.add(npc);
        }
        Sheep team = world.spawn(new Location(world, 18.5D, ModernArcadeLobby.FLOOR_Y + 1.0D, 0.5D), Sheep.class);
        team.setColor(DyeColor.GRAY); team.setCustomName(ChatColor.AQUA + "Free For All" + ChatColor.GRAY + " | right-click to queue");
        team.setCustomNameVisible(true); team.setInvulnerable(true); team.setRemoveWhenFarAway(false);
        team.setPersistent(false); team.setSilent(true); team.setAI(false);
        team.getPersistentDataContainer().set(teamNpcKey, PersistentDataType.STRING, "queue");
        teamNpc = team;
    }

    private void refreshShowcase() {
        World world = lobby.ensureWorld();
        if (world == null) return;
        for (Entity e : new ArrayList<>(showcase)) if (e != null && e.isValid()) e.remove();
        showcase.clear();
        for (Entity e : world.getEntities()) {
            String tag = e.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
            if (tag != null && tag.startsWith("showcase:")) e.remove();
        }
        if (selectedGame != ArcadePortGame.BACON_BRAWL) return;
        int[] xs = {-9,-6,-3,0,3,6}, zs = {18,26};
        String[] names = {"Porky","Inferno","Frost","Slime","Royal","Confetti"};
        for (int row=0; row<2; row++) for (int col=0; col<THEMES.length; col++) {
            Location at = new Location(world, xs[col]+0.5D, ModernArcadeLobby.FLOOR_Y+1.0D, zs[row]+0.5D);
            LivingEntity demo;
            if (row == 0) demo = world.spawn(at, Pig.class);
            else {
                Sheep sheep = world.spawn(at, Sheep.class); sheep.setColor(DyeColor.PINK); demo = sheep;
            }
            demo.setCustomName((row==0 ? ChatColor.GOLD+"Pig - " : ChatColor.LIGHT_PURPLE+"Pink Sheep - ")+names[col]+" Set");
            demo.setCustomNameVisible(true); demo.setInvulnerable(true); demo.setRemoveWhenFarAway(false);
            demo.setPersistent(false); demo.setSilent(true); if(demo instanceof Mob mob) mob.setAI(false);
            demo.getPersistentDataContainer().set(themeKey, PersistentDataType.STRING, "showcase:"+THEMES[col]);
            showcase.add(demo);
        }
    }

    @EventHandler
    public void onLobbyPadUse(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND || (e.getAction()!=Action.RIGHT_CLICK_BLOCK && e.getAction()!=Action.RIGHT_CLICK_AIR)) return;
        Player p=e.getPlayer();
        if(!lobby.isLobbyWorld(p.getWorld()) || e.getAction()==Action.RIGHT_CLICK_AIR || e.getClickedBlock()==null) return;
        int x=e.getClickedBlock().getX(), z=e.getClickedBlock().getZ();
        if(nearPad(x,z,-17,0)){e.setCancelled(true);openKitMenu(p);}
        else if(nearPad(x,z,18,0)){e.setCancelled(true);openQueueMenu(p);}
    }
    private boolean nearPad(int x,int z,int cx,int cz){return Math.abs(x-cx)<=3 && Math.abs(z-cz)<=3;}

    @EventHandler
    public void onLobbyNpcUse(PlayerInteractEntityEvent e) {
        Entity entity=e.getRightClicked();
        String kit=entity.getPersistentDataContainer().get(kitNpcKey,PersistentDataType.STRING);
        String queue=entity.getPersistentDataContainer().get(teamNpcKey,PersistentDataType.STRING);
        String bot=entity.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        if(kit!=null){
            e.setCancelled(true); manager.selectKit(e.getPlayer(),kit);
            plugin.getConfig().set("lobby-kit-preferences."+e.getPlayer().getUniqueId()+"."+selectedGame.id(),kit); plugin.saveConfig();
            e.getPlayer().sendMessage(ChatColor.GREEN+"Selected "+kit.replace('-',' ')+" for "+selectedGame.displayName()+".");
        }else if(queue!=null){e.setCancelled(true);openQueueMenu(e.getPlayer());}
        else if(bot!=null && (e.getPlayer().isOp() || e.getPlayer().hasPermission("testplex.arcadeports.admin"))){
            e.setCancelled(true); Bot b=bots.get(bot); if(b!=null)openBotCosmetics(e.getPlayer(),b);
        }
    }

    private void openKitMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,27,KIT_TITLE);
        if(selectedGame!=ArcadePortGame.BACON_BRAWL) inv.setItem(13,item(Material.BARRIER,ChatColor.RED+"Kits not ready","That game's modern parity gate is not open yet."));
        else for(int i=0;i<KITS.length;i++){ItemStack it=item(i==2?Material.PINK_WOOL:Material.PORKCHOP,ChatColor.YELLOW+KIT_NAMES[i],ChatColor.GRAY+"Choose this kit for your next Bacon Brawl.",ChatColor.GREEN+"Click to select");mark(it,"kit:"+KITS[i],null);inv.setItem(KIT_SLOTS[i],it);}
        inv.setItem(22,item(Material.RED_BED,ChatColor.GRAY+"Back / Close")); p.openInventory(inv);
    }
    private void openQueueMenu(Player p){
        Inventory inv=Bukkit.createInventory(null,27,QUEUE_TITLE);
        inv.setItem(4,item(Material.PORKCHOP,ChatColor.GOLD+selectedGame.displayName(),
                ChatColor.GRAY+(selectedGame==ArcadePortGame.BACON_BRAWL?"Free-for-all: no team is required.":"Team queue for this game."),
                ChatColor.GRAY+"Human players queued: "+manager.queuedCount(selectedGame),ChatColor.GRAY+"NPC bots available: "+bots.size()));
        ItemStack join=item(Material.LIME_DYE,ChatColor.GREEN+"Join Queue",ChatColor.GRAY+"Join the selected game's pre-game queue.");mark(join,"queue-join",null);inv.setItem(11,join);
        ItemStack leave=item(Material.RED_DYE,ChatColor.RED+"Leave Queue",ChatColor.GRAY+"Remove yourself from all waiting queues.");mark(leave,"queue-leave",null);inv.setItem(15,leave);
        inv.setItem(22,item(Material.RED_BED,ChatColor.GRAY+"Back / Close"));p.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e){
        String title=ChatColor.stripColor(e.getView().getTitle());
        if(title==null || !(title.equals(KIT_TITLE)||title.equals(QUEUE_TITLE)||title.startsWith(BOT_MENU_PREFIX)))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)||e.getClickedInventory()==null||e.getClickedInventory()!=e.getView().getTopInventory())return;
        ItemStack it=e.getCurrentItem();if(it==null||it.getType().isAir()||!it.hasItemMeta())return;
        String action=it.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
        if(KIT_TITLE.equals(title)){
            if(action!=null&&action.startsWith("kit:")){
                String kit=action.substring(4);manager.selectKit(p,kit);
                plugin.getConfig().set("lobby-kit-preferences."+p.getUniqueId()+"."+selectedGame.id(),kit);plugin.saveConfig();p.closeInventory();
            }else if(e.getSlot()==22)p.closeInventory();
        }else if(QUEUE_TITLE.equals(title)){
            if("queue-join".equals(action)){manager.queue(selectedGame,p,false);refreshDisplays();openQueueMenu(p);}
            else if("queue-leave".equals(action)){manager.removeFromQueues(p);p.sendMessage(ChatColor.YELLOW+"You have left the Arcade queue.");refreshDisplays();openQueueMenu(p);}
            else if(e.getSlot()==22)p.closeInventory();
        }else{
            String key=it.getItemMeta().getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
            Bot b=key==null?null:bots.get(key);if(b==null)return;
            if(action!=null&&action.startsWith("theme:")){b.theme=action.substring(6);saveBot(b);refreshBotAppearance(b);openBotCosmetics(p,b);}
            else if(action!=null&&action.startsWith("form:")){b.form=action.substring(5);saveBot(b);replaceBotEntity(b);openBotCosmetics(p,b);}
            else if("bot-close".equals(action))p.closeInventory();
        }
    }

    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of(lore));i.setItemMeta(meta);return i;}
    private void mark(ItemStack i,String action,String target){ItemMeta m=i.getItemMeta();m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);if(target!=null)m.getPersistentDataContainer().set(botKey,PersistentDataType.STRING,target);i.setItemMeta(m);}

    private void loadSavedBots(){
        var section=plugin.getConfig().getConfigurationSection("test-bots");if(section==null)return;
        for(String key:section.getKeys(false)){
            Bot b=new Bot(key,plugin.getConfig().getString("test-bots."+key+".name",key),
                plugin.getConfig().getString("test-bots."+key+".theme","porky"),
                plugin.getConfig().getString("test-bots."+key+".form","pig"),
                plugin.getConfig().getString("test-bots."+key+".kit","el-muchacho-pigo"));
            bots.put(key,b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);
        }
    }

    public void handleBotCommand(org.bukkit.command.CommandSender sender,String[] args){
        if(!sender.isOp()&&!sender.hasPermission("testplex.arcadeports.admin")){sender.sendMessage(ChatColor.RED+"Operator permission required to manage test bots.");return;}
        if(args.length==0){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot <summon|remove|clear|list|cosmetics|form|kit> ...");return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "summon","add","spawn"->{String name=args.length>1?args[1]:"TestBot"+(bots.size()+1);Bot b=createBot(name);if(b==null)sender.sendMessage(ChatColor.RED+"Bot name invalid or already used.");else sender.sendMessage(ChatColor.GREEN+"Summoned NPC bot "+b.name+". Use /arcadeport bot cosmetics "+b.name+" to customise it.");}
            case "remove","despawn"->{if(args.length<2){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot remove <name>");return;}if(!removeBot(args[1]))sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");else sender.sendMessage(ChatColor.GREEN+"Removed bot "+args[1]+".");}
            case "clear"->{for(String k:new ArrayList<>(bots.keySet()))removeBot(k);sender.sendMessage(ChatColor.GREEN+"Removed all test NPC bots.");}
            case "list"->sender.sendMessage(ChatColor.GOLD+"Test bots: "+(bots.isEmpty()?"none":String.join(", ",bots.values().stream().map(b->b.name).toList())));
            case "cosmetics","menu"->{if(!(sender instanceof Player p)){sender.sendMessage("Players only.");return;}if(args.length<2){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot cosmetics <name>");return;}Bot b=bots.get(key(args[1]));if(b==null)sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");else openBotCosmetics(p,b);}
            case "form"->{if(args.length<3){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot form <name> <pig|pink-sheep>");return;}Bot b=bots.get(key(args[1]));if(b==null){sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");return;}b.form="pink-sheep".equalsIgnoreCase(args[2])?"pink-sheep":"pig";saveBot(b);replaceBotEntity(b);sender.sendMessage(ChatColor.GREEN+"Updated "+b.name+"'s form.");}
            case "kit"->{if(args.length<3){sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot kit <name> <kit-id>");return;}Bot b=bots.get(key(args[1]));if(b==null){sender.sendMessage(ChatColor.RED+"No bot named "+args[1]+".");return;}String kit=ArcadePortRules.normalizeBaconBrawlKit(args[2]);if(kit==null){sender.sendMessage(ChatColor.YELLOW+"Bacon Brawl bot kits: el-muchacho-pigo, mama-piggles, pig.");return;}b.kit=kit;b.form=ArcadePortRules.baconBrawlUsesSheepAvatar(kit)?"pink-sheep":"pig";saveBot(b);replaceBotEntity(b);sender.sendMessage(ChatColor.GREEN+"Updated "+b.name+"'s kit to "+kit+".");}
            default->sender.sendMessage(ChatColor.YELLOW+"/arcadeport bot <summon|remove|clear|list|cosmetics|form|kit> ...");
        }
    }

    private Bot createBot(String name){String k=key(name);if(k.isBlank()||bots.containsKey(k))return null;Bot b=new Bot(k,name,"porky","pig","el-muchacho-pigo");bots.put(k,b);saveBot(b);spawnBotEntity(b,lobby.spawnLocation().add(bots.size()*1.6D-3.0D,-3.0D,8.0D),false);refreshDisplays();return b;}
    private String key(String n){return n==null?"":n.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9_-]","-");}
    private boolean removeBot(String n){Bot b=bots.remove(key(n));if(b==null)return false;if(b.actor!=null&&b.actor.isValid())b.actor.remove();plugin.getConfig().set("test-bots."+b.key,null);plugin.saveConfig();refreshDisplays();return true;}
    private void saveBot(Bot b){String p="test-bots."+b.key+".";plugin.getConfig().set(p+"name",b.name);plugin.getConfig().set(p+"theme",b.theme);plugin.getConfig().set(p+"form",b.form);plugin.getConfig().set(p+"kit",b.kit);plugin.saveConfig();}
    private void replaceBotEntity(Bot b){if(b.actor!=null&&b.actor.isValid())b.actor.remove();Location at=b.actorLocation==null||b.actorLocation.getWorld()!=lobby.ensureWorld()?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,manager.isActiveGame(at.getWorld(),selectedGame));}
    private void spawnBotEntity(Bot b,Location at,boolean inGame){
        if(at==null||at.getWorld()==null)return;LivingEntity actor;
        if("pink-sheep".equals(b.form)){Sheep sheep=at.getWorld().spawn(at,Sheep.class);sheep.setColor(DyeColor.PINK);actor=sheep;}else actor=at.getWorld().spawn(at,Pig.class);
        actor.setInvulnerable(true);actor.setRemoveWhenFarAway(false);actor.setPersistent(false);actor.setSilent(true);actor.setCustomNameVisible(true);actor.setCollidable(true);
        actor.getPersistentDataContainer().set(botKey,PersistentDataType.STRING,b.key);actor.getPersistentDataContainer().set(themeKey,PersistentDataType.STRING,b.theme);actor.getPersistentDataContainer().set(formKey,PersistentDataType.STRING,b.form);
        if(actor instanceof Mob mob)mob.setAI(false);b.actor=actor;b.actorLocation=at.clone();b.inGame=inGame;b.arenaSpawn=at.clone();refreshBotAppearance(b);
    }
    private void refreshBotAppearance(Bot b){if(b.actor==null||!b.actor.isValid())return;if(b.actor instanceof Sheep s)s.setColor(DyeColor.PINK);b.actor.setCustomName(ChatColor.GOLD+b.name+ChatColor.GRAY+" ["+pretty(b.theme)+"]");b.actor.setCustomNameVisible(true);b.actor.getPersistentDataContainer().set(themeKey,PersistentDataType.STRING,b.theme);b.actor.getPersistentDataContainer().set(formKey,PersistentDataType.STRING,b.form);b.actor.getPersistentDataContainer().set(botKey,PersistentDataType.STRING,b.key);}
    private void openBotCosmetics(Player p,Bot b){
        Inventory inv=Bukkit.createInventory(null,27,BOT_MENU_PREFIX+b.key);int[] slots={10,11,12,13,14,15};
        for(int i=0;i<THEMES.length;i++){String theme=THEMES[i];ItemStack it=item(themeMaterial(theme),(b.theme.equals(theme)?ChatColor.GREEN+"✓ ":ChatColor.WHITE)+pretty(theme),ChatColor.GRAY+"Theme particle preview",b.theme.equals(theme)?ChatColor.GREEN+"Selected":ChatColor.YELLOW+"Click to equip");mark(it,"theme:"+theme,b.key);inv.setItem(slots[i],it);}
        ItemStack form=item(Material.SHEARS,ChatColor.AQUA+"Toggle Bot Form",ChatColor.GRAY+"Current: "+b.form,ChatColor.YELLOW+"Switch Pig / Pink Sheep");mark(form,"form:"+("pig".equals(b.form)?"pink-sheep":"pig"),b.key);inv.setItem(22,form);
        ItemStack close=item(Material.RED_BED,ChatColor.RED+"Close");mark(close,"bot-close",b.key);inv.setItem(26,close);p.openInventory(inv);
    }
    private Material themeMaterial(String theme){return switch(theme){case "inferno"->Material.FIRE_CHARGE;case "frost"->Material.SNOWBALL;case "slime"->Material.SLIME_BALL;case "royal"->Material.GOLDEN_APPLE;case "confetti"->Material.FIREWORK_ROCKET;default->Material.PORKCHOP;};}
    private String pretty(String value){StringBuilder out=new StringBuilder();for(String part:value.split("-")){if(part.isEmpty())continue;if(out.length()>0)out.append(' ');out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}return out.toString();}

    private void tick(){
        refreshDisplays();World world=lobby.ensureWorld();
        for(Entity e:new ArrayList<>(showcase)){if(e==null||!e.isValid()||e.getWorld()!=world)continue;String value=e.getPersistentDataContainer().get(themeKey,PersistentDataType.STRING);String theme=value!=null&&value.startsWith("showcase:")?value.substring(9):"porky";e.getWorld().spawnParticle(particle(theme),e.getLocation().add(0,1,0),3,0.2,0.3,0.2,0.01);}
        long now=System.currentTimeMillis();for(Bot b:bots.values())tickBot(b,now,world);
    }
    private void tickBot(Bot b,long now,World lobbyWorld){
        Entity e=b.actor;if(!(e instanceof LivingEntity actor)||!e.isValid()){Location at=b.actorLocation==null?lobby.spawnLocation().add(0,-3,8):b.actorLocation.clone();spawnBotEntity(b,at,b.inGame);return;}
        if(selectedGame!=ArcadePortGame.BACON_BRAWL||!manager.isActiveGame(e.getWorld(),selectedGame)){if(e.getWorld()!=lobbyWorld&&b.inGame)returnBotsToLobby();return;}
        List<Player> targets=manager.botTargets(e.getWorld(),selectedGame);Player nearest=null;double distance=Double.POSITIVE_INFINITY;
        for(Player p:targets){double d=p.getLocation().distanceSquared(e.getLocation());if(d<distance){distance=d;nearest=p;}}
        if(nearest==null)return;
        if(e.getLocation().getY()<e.getWorld().getMinHeight()+8){if(b.arenaSpawn!=null)actor.teleport(b.arenaSpawn);return;}
        if(distance>4){Vector v=nearest.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);if(v.lengthSquared()>0.01){v.normalize().multiply(0.22);v.setY(actor.isOnGround()?0.15:actor.getVelocity().getY());actor.setVelocity(v);}}
        else if(now>=b.nextAttackAt){Vector away=nearest.getLocation().toVector().subtract(e.getLocation().toVector()).setY(0);if(away.lengthSquared()<0.01)away=new Vector(0,0,1);nearest.setVelocity(away.normalize().multiply(Math.min(1.35,Math.max(0.65,manager.baconBrawlKnockbackValue(e.getWorld())))).setY(0.42));nearest.sendActionBar(ChatColor.RED+b.name+ChatColor.GRAY+" hit you!");nearest.getWorld().spawnParticle(particle(b.theme),nearest.getLocation().add(0,1,0),8,0.4,0.3,0.4,0.01);b.nextAttackAt=now+1400;}
        e.getWorld().spawnParticle(particle(b.theme),e.getLocation().add(0,1,0),2,0.2,0.2,0.2,0.01);
    }
    private Particle particle(String theme){return switch(theme){case "inferno"->Particle.FLAME;case "frost"->Particle.SNOWFLAKE;case "slime"->Particle.SLIME;case "royal"->Particle.END_ROD;case "confetti"->Particle.HAPPY_VILLAGER;default->Particle.CRIT;};}

    public void deployBots(ArcadePortGame game,List<Location> spawns){
        if(game!=ArcadePortGame.BACON_BRAWL||spawns.isEmpty()){returnBotsToLobby();return;}int index=0;
        for(Bot b:bots.values()){Location target=spawns.get(index++%spawns.size()).clone();target.add(index%2==0?3:-3,0,index%3==0?2:-2);
            if(b.actor==null||!b.actor.isValid())spawnBotEntity(b,target,true);else b.actor.teleport(target);b.inGame=true;b.arenaSpawn=target.clone();b.actorLocation=target.clone();refreshBotAppearance(b);}
    }
    public void returnBotsToLobby(){Location base=lobby.spawnLocation();if(base==null)return;int index=0;
        for(Bot b:bots.values()){Location home=base.clone().add(index++*1.6D-3,-3,8);if(b.actor==null||!b.actor.isValid())spawnBotEntity(b,home,false);else{b.actor.teleport(home);b.actor.setVelocity(new Vector());if(b.actor instanceof Mob mob)mob.setAI(false);}b.inGame=false;b.actorLocation=home.clone();b.arenaSpawn=null;refreshBotAppearance(b);}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false)
    public void onBotDamage(EntityDamageEvent e){
        Entity entity=e.getEntity();String key=entity.getPersistentDataContainer().get(botKey,PersistentDataType.STRING);
        if(key==null){String tag=entity.getPersistentDataContainer().get(themeKey,PersistentDataType.STRING);if(entity.getPersistentDataContainer().has(kitNpcKey,PersistentDataType.STRING)||entity.getPersistentDataContainer().has(teamNpcKey,PersistentDataType.STRING)||(tag!=null&&tag.startsWith("showcase:")))e.setCancelled(true);return;}
        e.setCancelled(true);if(!(entity instanceof LivingEntity actor)||!(e instanceof EntityDamageByEntityEvent be)||!(be.getDamager() instanceof Player attacker)||!manager.isActiveGame(entity.getWorld(),ArcadePortGame.BACON_BRAWL))return;
        Vector dir=entity.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);if(dir.lengthSquared()<0.01)dir=new Vector(0,0,1);
        actor.setVelocity(dir.normalize().multiply(Math.min(1.5,Math.max(0.65,manager.baconBrawlKnockbackValue(entity.getWorld())))).setY(0.4));Bot b=bots.get(key);
        if(b!=null){attacker.sendActionBar(ChatColor.GREEN+"Hit bot "+b.name);entity.getWorld().spawnParticle(particle(b.theme),entity.getLocation().add(0,1,0),8,0.4,0.3,0.4,0.01);}
    }
    public void shutdown(){if(task!=null)task.cancel();returnBotsToLobby();for(Entity e:new ArrayList<>(showcase))if(e!=null&&e.isValid())e.remove();for(Entity e:new ArrayList<>(kitNpcs))if(e!=null&&e.isValid())e.remove();if(teamNpc!=null&&teamNpc.isValid())teamNpc.remove();}

    private final class Bot{
        final String key,name;String theme,form,kit;Entity actor;Location actorLocation,arenaSpawn;boolean inGame;long nextAttackAt;
        Bot(String key,String name,String theme,String form,String kit){this.key=key;this.name=name;this.theme=List.of(THEMES).contains(theme)?theme:"porky";this.form="pink-sheep".equals(form)?"pink-sheep":"pig";this.kit=ArcadePortRules.normalizeBaconBrawlKit(kit)==null?"el-muchacho-pigo":kit;}
    }
}
