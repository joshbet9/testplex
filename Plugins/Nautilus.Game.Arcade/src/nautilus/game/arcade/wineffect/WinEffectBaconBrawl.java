package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilShapes;
import mineplex.core.common.util.UtilText;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

/** A true pig-versus-pig body slam on a standalone Mighty Lava Arena-inspired island. */
public class WinEffectBaconBrawl extends WinEffectGadget
{
    private static final int TARGET_COUNT = 4;
    private static final long FIRST_IMPACT_DELAY_MS = 800L;
    private static final long IMPACT_INTERVAL_MS = 850L;
    private Pig _winnerPig;
    private final List<Pig> _targets = new ArrayList<Pig>();
    private int _nextTarget;
    private long _nextImpact;
    private boolean _cleaned;

    public WinEffectBaconBrawl(GadgetManager manager)
    {
        super(manager, "Arcade Bacon Brawl - Mighty Lava Arena Demo",
                UtilText.splitLineToArray(C.cGray + "A pig body-slams four pigs from a miniature Mighty Lava Arena.", LineFormat.LORE),
                -2, Material.PORK, (byte) 0, true);
        _length = TimeUnit.SECONDS.toMillis(8);
    }

    @Override
    public void buildWinnerRoom()
    {
        Location base = getBaseLocation();
        WinEffectRoomUtil.clear(base, 24, 3, 18);
        World world = base.getWorld();
        int bx = base.getBlockX(), bz = base.getBlockZ(), y = base.getBlockY() - 1;
        for (int x = -8; x <= 8; x++)
        {
            for (int z = -8; z <= 8; z++)
            {
                int d2 = x * x + z * z;
                if (d2 > 64) continue;
                org.bukkit.block.Block floor = world.getBlockAt(bx + x, y, bz + z);
                if (d2 >= 49) floor.setType(Material.SMOOTH_BRICK, false);
                else
                {
                    floor.setType(Material.SMOOTH_BRICK, false);
                    if ((x + z) % 7 == 0) floor.setData((byte) 2);
                }
                world.getBlockAt(bx + x, y - 1, bz + z).setType(Material.NETHERRACK, false);
            }
        }
        // A raised stone-brick ring holds the lower stationary-lava moat outside the arena.
        for (int x = -11; x <= 11; x++)
        {
            for (int z = -11; z <= 11; z++)
            {
                int d2 = x * x + z * z;
                if (d2 < 81 || d2 > 121) continue;
                world.getBlockAt(bx + x, y - 1, bz + z).setType(Material.NETHERRACK, false);
                world.getBlockAt(bx + x, y, bz + z).setType(Material.STATIONARY_LAVA, false);
            }
        }
        // Four volcanic corner pylons make the room instantly distinct from the default podium.
        for (int sx : new int[]{-1, 1})
            for (int sz : new int[]{-1, 1})
            {
                int px = bx + sx * 7, pz = bz + sz * 7;
                for (int h = 0; h < 4; h++)
                    world.getBlockAt(px, y + h, pz).setType(h == 3 ? Material.GLASS : Material.NETHERRACK, false);
                world.getBlockAt(px, y + 3, pz).setType(Material.STATIONARY_LAVA, false);
            }
    }

    @Override
    public void teleport()
    {
        Location view = getBaseLocation().clone().add(0, 3, 19);
        view.setDirection(getBaseLocation().clone().subtract(view).toVector());
        teleport(view);
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _nextTarget = 0;
        _targets.clear();
        Location base = getBaseLocation();
        _winnerPig = spawnPig(base.clone().add(0, 0, 5.5));
        for (Location position : UtilShapes.getPointsInCircle(base, TARGET_COUNT, 4.2))
            _targets.add(spawnPig(position));
        _nextImpact = System.currentTimeMillis() + FIRST_IMPACT_DELAY_MS;
    }

    private Pig spawnPig(Location at)
    {
        Pig pig = at.getWorld().spawn(at, Pig.class);
        pig.setSaddle(true);
        pig.setVelocity(new Vector(0, 0, 0));
        return pig;
    }

    @EventHandler
    public void onUpdate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || _cleaned || !isRunning()) return;
        if (_nextTarget >= _targets.size() || System.currentTimeMillis() < _nextImpact) return;
        Pig target = _targets.get(_nextTarget);
        if (target == null || target.isDead() || !target.isValid())
        {
            _nextTarget++;
            _nextImpact = System.currentTimeMillis() + IMPACT_INTERVAL_MS;
            return;
        }
        if (_winnerPig != null && _winnerPig.isValid())
        {
            Vector approach = target.getLocation().toVector().subtract(getBaseLocation().toVector());
            approach.setY(0);
            if (approach.lengthSquared() < 0.01) approach = new Vector(1, 0, 0);
            Location winnerSpot = target.getLocation().clone().subtract(approach.clone().normalize().multiply(1.3));
            winnerSpot.setY(target.getLocation().getY());
            winnerSpot.setDirection(target.getLocation().toVector().subtract(winnerSpot.toVector()));
            _winnerPig.teleport(winnerSpot);
            _winnerPig.getWorld().playSound(winnerSpot, Sound.VILLAGER_HIT, 1.0F, 1.1F);
        }
        Vector flight = target.getLocation().toVector().subtract(getBaseLocation().toVector());
        flight.setY(0);
        if (flight.lengthSquared() < 0.01) flight = new Vector(1, 0, 0);
        flight.normalize().multiply(1.55);
        flight.setY(0.82);
        target.setVelocity(flight);
        target.getWorld().playSound(target.getLocation(), Sound.VILLAGER_HIT, 1.6F, 0.8F);
        target.getWorld().playSound(target.getLocation(), Sound.EXPLODE, 0.7F, 1.5F);
        _nextTarget++;
        _nextImpact = System.currentTimeMillis() + IMPACT_INTERVAL_MS;
    }

    @Override
    public void finish()
    {
        if (_cleaned) return;
        _cleaned = true;
        if (_winnerPig != null && !_winnerPig.isDead()) _winnerPig.remove();
        for (Pig target : _targets) if (target != null && !target.isDead()) target.remove();
        _targets.clear();
        _winnerPig = null;
    }
}
