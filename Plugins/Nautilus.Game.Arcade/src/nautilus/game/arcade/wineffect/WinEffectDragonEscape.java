package nautilus.game.arcade.wineffect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilParticle;
import mineplex.core.common.util.UtilParticle.ParticleType;
import mineplex.core.common.util.UtilParticle.ViewDist;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;
/** Floating Skylands-inspired platforms and a short series of jumps. */
public class WinEffectDragonEscape extends WinEffectGadget
{
    private final List<Location> _platforms=new ArrayList<Location>();
    private DisguisePlayer _winner;
    private Location _jumpStart;
    private long _jumpStarted;
    private int _next;
    private boolean _cleaned;
    public WinEffectDragonEscape(GadgetManager manager)
    {
        super(manager,"Arcade Dragon Escape - Skylands Finale",
            UtilText.splitLineToArray(C.cGray+"Jump across floating islands inspired by Skylands' final jumps.",LineFormat.LORE),
            -2,Material.DRAGON_EGG,(byte)0,true);
        _length=TimeUnit.SECONDS.toMillis(8);
    }
    @Override public void buildWinnerRoom()
    {
        super.buildWinnerRoom();
        Location base=getBaseLocation(); int[] xs={-8,-4,0,4,8},hs={0,1,2,1,3}; int floor=base.getBlockY()-1; _platforms.clear();
        for(int i=0;i<xs.length;i++)
        {
            int x=base.getBlockX()+xs[i],y=floor+hs[i],z=base.getBlockZ()-2;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            {
                base.getWorld().getBlockAt(x+dx,y,z+dz).setType(Material.STONE,false);
                base.getWorld().getBlockAt(x+dx,y+1,z+dz).setType(Material.GRASS,false);
            }
            base.getWorld().getBlockAt(x,y+1,z).setType(Material.EMERALD_BLOCK,false);
            _platforms.add(new Location(base.getWorld(),x+0.5,y+2,z+0.5));
        }
    }
    @Override public void play()
    {
        _cleaned=false;_next=1;_winner=getNPC(_player,getBaseLocation());
        if(_platforms.isEmpty()){Location b=getBaseLocation();for(int i=0;i<5;i++)_platforms.add(b.clone().add(-8+i*4,i%3,-2));}
        Entity e=_winner.getEntity().getBukkitEntity();e.teleport(_platforms.get(0));_jumpStart=_platforms.get(0).clone();_jumpStarted=System.currentTimeMillis();
    }
    @EventHandler public void animate(UpdateEvent event)
    {
        if(event.getType()!=UpdateType.TICK||!isRunning()||_cleaned||_winner==null||_next>=_platforms.size())return;
        Entity e=_winner.getEntity()==null?null:_winner.getEntity().getBukkitEntity();if(e==null||!e.isValid())return;
        long now=System.currentTimeMillis();double p=Math.min(1.0,(now-_jumpStarted)/650.0);Location target=_platforms.get(_next);
        Location current=_jumpStart.clone().add((target.getX()-_jumpStart.getX())*p,(target.getY()-_jumpStart.getY())*p+Math.sin(p*Math.PI)*1.25,(target.getZ()-_jumpStart.getZ())*p);
        e.teleport(current);UtilParticle.PlayParticle(ParticleType.CLOUD,current,0.12F,0.08F,0.12F,0.01F,3,ViewDist.NORMAL);
        if(p>=1.0){e.getWorld().playSound(target,Sound.FIREWORK_LAUNCH,0.8F,1.25F);UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK,target,0.25F,0.3F,0.25F,0.04F,12,ViewDist.NORMAL);_jumpStart=target.clone();_jumpStarted=now;_next++;}
    }
    @Override public void finish()
    {
        if(_cleaned)return;_cleaned=true;
        if(_winner!=null){Entity e=_winner.getEntity()==null?null:_winner.getEntity().getBukkitEntity();Manager.getDisguiseManager().undisguise(_winner);if(e!=null&&!e.isDead())e.remove();}
        _winner=null;_platforms.clear();
    }
}
