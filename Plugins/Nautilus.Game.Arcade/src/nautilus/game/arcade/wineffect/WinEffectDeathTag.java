package nautilus.game.arcade.wineffect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.common.util.UtilShapes;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
/** Barbarians Village tree-inspired winner room and harmless runner lookalikes. */
public class WinEffectDeathTag extends WinEffectGadget
{
    private final List<DisguisePlayer> _targets = new ArrayList<DisguisePlayer>();
    private final List<ArmorStand> _stands = new ArrayList<ArmorStand>();
    private DisguisePlayer _winner;
    private int _next;
    private long _nextImpact;
    private boolean _cleaned;
    public WinEffectDeathTag(GadgetManager manager)
    {
        super(manager, "Arcade Death Tag - Barbarians Tree",
            UtilText.splitLineToArray(C.cGray + "Meet under the Barbarians Village tree as runner lookalikes are knocked from the ring.", LineFormat.LORE),
            -2, Material.SAPLING, (byte) 1, true);
        _length = TimeUnit.SECONDS.toMillis(7);
    }
    @Override public void buildWinnerRoom()
    {
        super.buildWinnerRoom();
        Location base = getBaseLocation();
        int x = base.getBlockX()+6, y = base.getBlockY()-1, z = base.getBlockZ();
        for(int dy=0;dy<5;dy++) base.getWorld().getBlockAt(x,y+dy,z).setType(Material.LOG,false);
        for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) for(int dy=3;dy<=5;dy++)
        {
            if(Math.abs(dx)+Math.abs(dz)+Math.max(0,dy-4)>5) continue;
            org.bukkit.block.Block b=base.getWorld().getBlockAt(x+dx,y+dy,z+dz);
            b.setType(Material.LEAVES,false); b.setData((byte)1);
        }
    }
    @Override public void play()
    {
        _cleaned=false; _next=0; _targets.clear(); _stands.clear();
        _winner=getNPC(_player,getBaseLocation());
        List<Location> points=UtilShapes.getPointsInCircle(getBaseLocation(),3,3.2);
        List<Player> skins=new ArrayList<Player>();
        if(_other!=null) for(Player p:_other) if(p!=null&&p.isOnline()&&!skins.contains(p)) skins.add(p);
        if(skins.isEmpty()) skins.add(_player);
        for(int i=0;i<3;i++)
        {
            Location loc=points.get(i).clone(); loc.setDirection(getBaseLocation().toVector().subtract(loc.toVector()));
            DisguisePlayer d=getNPC(skins.get(i%skins.size()),loc,false);
            ArmorStand stand=(ArmorStand)d.getEntity().getBukkitEntity(); stand.setGravity(false);
            _targets.add(d); _stands.add(stand);
        }
        _nextImpact=System.currentTimeMillis()+650L;
    }
    @EventHandler public void animate(UpdateEvent event)
    {
        if(event.getType()!=UpdateType.TICK||!isRunning()||_cleaned||_next>=_stands.size()||System.currentTimeMillis()<_nextImpact)return;
        ArmorStand target=_stands.get(_next);
        if(target==null||target.isDead()||!target.isValid()){_next++;_nextImpact=System.currentTimeMillis()+700L;return;}
        Vector away=target.getLocation().toVector().subtract(getBaseLocation().toVector()); away.setY(0);
        if(away.lengthSquared()<0.01) away=new Vector(1,0,0);
        target.setGravity(true); target.setVelocity(away.normalize().multiply(1.35).setY(0.85));
        target.getWorld().playSound(target.getLocation(),Sound.VILLAGER_HIT,1.2F,0.8F);
        UtilParticle.PlayParticle(ParticleType.HAPPY_VILLAGER,target.getLocation().clone().add(0,1,0),0.35F,0.45F,0.35F,0.05F,14,ViewDist.NORMAL);
        _next++; _nextImpact=System.currentTimeMillis()+700L;
    }
    @Override public void finish()
    {
        if(_cleaned)return; _cleaned=true;
        for(DisguisePlayer d:_targets) removeNpc(d);
        _targets.clear(); _stands.clear(); removeNpc(_winner); _winner=null;
    }
    private void removeNpc(DisguisePlayer d)
    {
        if(d==null)return;
        Entity e=d.getEntity()==null?null:d.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(d);
        if(e!=null&&!e.isDead())e.remove();
    }
}
