package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import mineplex.core.common.util.C;
import mineplex.core.common.util.LineFormat;
import mineplex.core.common.util.UtilText;
import mineplex.core.disguise.disguises.DisguisePlayer;
import mineplex.core.gadget.GadgetManager;
import mineplex.core.gadget.types.WinEffectGadget;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

/** A mini Barbarians Village: player lookalikes are knocked from the tree canopy. */
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
        super(manager, "Arcade Death Tag - Barbarians Village",
                UtilText.splitLineToArray(C.cGray + "A miniature Barbarians Village tree: runners perch in its branches before being knocked down.", LineFormat.LORE),
                -2, Material.SAPLING, (byte) 1, true);
        _length = TimeUnit.SECONDS.toMillis(8);
    }

    @Override
    public void buildWinnerRoom()
    {
        Location base = getBaseLocation();
        WinEffectRoomUtil.clear(base, 24, 3, 18);
        World world = base.getWorld();
        int bx = base.getBlockX(), bz = base.getBlockZ(), y = base.getBlockY() - 1;
        for (int x = -14; x <= 14; x++)
            for (int z = -14; z <= 14; z++)
            {
                if (x * x + z * z > 196) continue;
                world.getBlockAt(bx + x, y - 1, bz + z).setType(Material.DIRT, false);
                world.getBlockAt(bx + x, y, bz + z).setType(Material.GRASS, false);
            }

        // A small village lane leads toward the landmark tree.
        for (int z = -12; z <= 12; z++)
            for (int x = -1; x <= 1; x++)
                world.getBlockAt(bx + x, y, bz + z).setType((z % 4 == 0) ? Material.GRAVEL : Material.COBBLESTONE, false);
        for (int x = -12; x <= 12; x++)
            for (int z = -1; z <= 1; z++)
                world.getBlockAt(bx + x, y, bz + z).setType((x % 4 == 0) ? Material.GRAVEL : Material.COBBLESTONE, false);

        buildHouse(world, bx - 9, y, bz - 8);
        buildHouse(world, bx + 9, y, bz - 8);
        buildHouse(world, bx - 9, y, bz + 8);
        buildHouse(world, bx + 9, y, bz + 8);

        // Old oak tree, with broad branches and a layered canopy.
        for (int h = 0; h <= 7; h++)
            world.getBlockAt(bx, y + h, bz).setType(Material.LOG, false);
        for (int h = 4; h <= 6; h++)
        {
            for (int sign : new int[]{-1, 1})
                for (int step = 1; step <= 4; step++)
                    world.getBlockAt(bx + sign * step, y + h, bz).setType(Material.LOG, false);
            for (int sign : new int[]{-1, 1})
                for (int step = 1; step <= 3; step++)
                    world.getBlockAt(bx, y + h, bz + sign * step).setType(Material.LOG, false);
        }
        for (int layer = 0; layer < 5; layer++)
        {
            int radius = layer == 4 ? 2 : layer == 3 ? 3 : 4;
            int leafY = y + 4 + layer;
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++)
                {
                    if (dx * dx + dz * dz > radius * radius) continue;
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && layer < 4) continue;
                    org.bukkit.block.Block leaf = world.getBlockAt(bx + dx, leafY, bz + dz);
                    leaf.setType(Material.LEAVES, false);
                    leaf.setData((byte) 0);
                }
        }
        // Branch shelves remain exposed so the runners visibly stand in the tree.
        for (int sign : new int[]{-1, 1})
            for (int dx = 0; dx <= 2; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    world.getBlockAt(bx + sign * (3 + dx), y + 4, bz + dz).setType(Material.WOOD, false);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = 3; dz <= 5; dz++)
                world.getBlockAt(bx + dx, y + 5, bz + dz).setType(Material.WOOD, false);
    }

    private void buildHouse(World world, int x, int floorY, int z)
    {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
            {
                world.getBlockAt(x + dx, floorY, z + dz).setType(Material.COBBLESTONE, false);
                for (int dy = 1; dy <= 3; dy++)
                {
                    boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    org.bukkit.block.Block block = world.getBlockAt(x + dx, floorY + dy, z + dz);
                    if (!wall) block.setType(Material.AIR, false);
                    else if (dy == 1 && (dx == 0 || dz == 0)) block.setType(Material.GLASS, false);
                    else block.setType(Material.WOOD, false);
                }
            }
        for (int size = 2; size >= 0; size--)
            for (int dx = -size; dx <= size; dx++)
                for (int dz = -size; dz <= size; dz++)
                    world.getBlockAt(x + dx, floorY + 4 + (2 - size), z + dz).setType(Material.WOOD, false);
    }

    @Override
    public void teleport()
    {
        Location view = getBaseLocation().clone().add(0, 3, 20);
        view.setDirection(getBaseLocation().clone().subtract(view).toVector());
        teleport(view);
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _next = 0;
        _targets.clear();
        _stands.clear();
        Location base = getBaseLocation();
        _winner = getNPC(_player, base.clone().add(0, 0, 8));
        List<Player> skins = new ArrayList<Player>();
        if (_other != null)
            for (Player player : _other)
                if (player != null && player.isOnline() && !skins.contains(player)) skins.add(player);
        if (skins.isEmpty()) skins.add(_player);

        Location[] branchLocations = {
                base.clone().add(-3.5, 4.0, 0.5),
                base.clone().add(3.5, 4.0, 0.5),
                base.clone().add(0.5, 5.0, 4.0)
        };
        for (int i = 0; i < branchLocations.length; i++)
        {
            Location loc = branchLocations[i];
            loc.setDirection(base.toVector().subtract(loc.toVector()));
            DisguisePlayer disguise = getNPC(skins.get(i % skins.size()), loc, false);
            ArmorStand stand = (ArmorStand) disguise.getEntity().getBukkitEntity();
            stand.setGravity(false);
            _targets.add(disguise);
            _stands.add(stand);
        }
        _nextImpact = System.currentTimeMillis() + 700L;
    }

    @EventHandler
    public void animate(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || !isRunning() || _cleaned
                || _next >= _stands.size() || System.currentTimeMillis() < _nextImpact) return;
        ArmorStand target = _stands.get(_next);
        if (target == null || target.isDead() || !target.isValid())
        {
            _next++;
            _nextImpact = System.currentTimeMillis() + 700L;
            return;
        }
        Vector away = target.getLocation().toVector().subtract(getBaseLocation().toVector());
        away.setY(0);
        if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
        target.setGravity(true);
        target.setVelocity(away.normalize().multiply(1.25).setY(0.88));
        target.getWorld().playSound(target.getLocation(), Sound.VILLAGER_HIT, 1.2F, 0.75F);
        target.getWorld().playSound(target.getLocation(), Sound.WOOD_BREAK, 0.8F, 1.1F);
        _next++;
        _nextImpact = System.currentTimeMillis() + 700L;
    }

    @Override
    public void finish()
    {
        if (_cleaned) return;
        _cleaned = true;
        for (DisguisePlayer disguise : _targets) removeNpc(disguise);
        _targets.clear();
        _stands.clear();
        removeNpc(_winner);
        _winner = null;
    }

    private void removeNpc(DisguisePlayer disguise)
    {
        if (disguise == null) return;
        Entity entity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(disguise);
        if (entity != null && !entity.isDead()) entity.remove();
    }
}
