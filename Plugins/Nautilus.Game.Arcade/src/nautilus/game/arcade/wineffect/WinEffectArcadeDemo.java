package nautilus.game.arcade.wineffect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

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

/**
 * Small, isolated demonstration rooms for Arcade win-effect concepts.
 *
 * These are deliberately compact themed demos rather than inaccurate copies of
 * whole Mineplex maps. Real map excerpts can replace each room's geometry later
 * without rewriting the staged animation.
 */
public class WinEffectArcadeDemo extends WinEffectGadget
{
    private enum Scene { DRAGONS, EVOLUTION, GLADIATORS, MICRO_BATTLES, MONSTER_MAZE,
        ONE_IN_THE_QUIVER, PAINTBALL, RUNNER, SHEEP_QUEST, SNAKE, SPLEEF, TURF_WARS }

    private final Scene _scene;
    private final List<DisguisePlayer> _actors = new ArrayList<DisguisePlayer>();
    private final List<Entity> _props = new ArrayList<Entity>();
    private final List<Location> _pads = new ArrayList<Location>();
    private final List<Location> _runnerPath = new ArrayList<Location>();
    private final List<Location> _snakePath = new ArrayList<Location>();
    private long _nextActionAt;
    private int _step;
    private boolean _cleaned;
    private boolean _finished;
    private Sheep _sheep;

    private WinEffectArcadeDemo(GadgetManager manager, String name, String description,
                                Material icon, byte data, Scene scene)
    {
        super(manager, name, UtilText.splitLineToArray(C.cGray + description, LineFormat.LORE),
                -2, icon, data, true);
        _scene = scene;
        _length = TimeUnit.SECONDS.toMillis(8);
    }

    @Override
    public void buildWinnerRoom()
    {
        Location base = getBaseLocation();
        WinEffectRoomUtil.clear(base, 24, 4, 20);
        World world = base.getWorld();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        int floor = by - 1;
        _pads.clear();
        _runnerPath.clear();
        _snakePath.clear();

        Material ground = _scene == Scene.EVOLUTION ? Material.GRASS
                : _scene == Scene.SPLEEF ? Material.SNOW_BLOCK
                : _scene == Scene.MONSTER_MAZE ? Material.SMOOTH_BRICK
                : Material.STONE;
        if (_scene == Scene.DRAGONS || _scene == Scene.MONSTER_MAZE)
            circle(world, bx, floor, bz, 10, ground);
        else
            square(world, bx, floor, bz, _scene == Scene.GLADIATORS ? 8 : 10, ground);

        switch (_scene)
        {
            case DRAGONS:
                ring(world, bx, floor, bz, 9, Material.STAINED_CLAY, (byte) 14);
                for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
                    pillar(world, bx + sx * 7, floor, bz + sz * 7, 4, Material.OBSIDIAN);
                break;

            case EVOLUTION:
                for (int level = 0; level < 4; level++)
                    for (int x = -(4 - level); x <= 4 - level; x++)
                        for (int z = -(4 - level); z <= 4 - level; z++)
                            if (Math.abs(x) + Math.abs(z) <= 5 - level)
                                set(world, bx + x, floor + level * 2, bz + z,
                                        level % 2 == 0 ? Material.SMOOTH_BRICK : Material.MOSSY_COBBLESTONE, (byte) 0);
                set(world, bx, floor + 7, bz, Material.DRAGON_EGG, (byte) 0);
                break;

            case GLADIATORS:
                for (int x = -8; x <= 8; x++) for (int h = 1; h <= 2; h++)
                {
                    set(world, bx + x, floor + h, bz - 8, Material.IRON_FENCE, (byte) 0);
                    set(world, bx + x, floor + h, bz + 8, Material.IRON_FENCE, (byte) 0);
                }
                for (int z = -7; z <= 7; z++) for (int h = 1; h <= 2; h++)
                {
                    set(world, bx - 8, floor + h, bz + z, Material.IRON_FENCE, (byte) 0);
                    set(world, bx + 8, floor + h, bz + z, Material.IRON_FENCE, (byte) 0);
                }
                for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
                    pillar(world, bx + sx * 6, floor, bz + sz * 6, 3, Material.IRON_BLOCK);
                break;

            case MICRO_BATTLES:
                for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
                {
                    int px = bx + sx * 7, pz = bz + sz * 7;
                    pillar(world, px, floor - 1, pz, 3, Material.STAINED_GLASS);
                    set(world, px, floor + 2, pz, Material.GLOWSTONE, (byte) 0);
                }
                set(world, bx, floor, bz, Material.STAINED_GLASS, (byte) 3);
                break;

            case MONSTER_MAZE:
                mazePad(world, bx, floor, bz, -6, 0, (byte) 10);
                mazePad(world, bx, floor, bz, 0, -6, (byte) 5);
                mazePad(world, bx, floor, bz, 6, 0, (byte) 13);
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
                    if (Math.abs(x) == 3 || Math.abs(z) == 3)
                        set(world, bx + x, floor + 1, bz + z, Material.STAINED_CLAY, (byte) 8);
                break;

            case ONE_IN_THE_QUIVER:
                for (int r = 4; r >= 0; r--)
                {
                    byte colour = r >= 3 ? (byte) 14 : r == 2 ? (byte) 1 : r == 1 ? (byte) 11 : (byte) 4;
                    for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++)
                        if (dx * dx + dy * dy <= r * r)
                            set(world, bx + dx, floor + 4 + dy, bz - 8, Material.WOOL, colour);
                }
                break;

            case PAINTBALL:
                for (int x = -9; x <= 9; x++) for (int h = 1; h <= 5; h++)
                {
                    byte colour = (byte) (((x + h) % 4 == 0) ? 4 : ((x + h) % 3 == 0) ? 11 : 14);
                    set(world, bx + x, floor + h, bz - 8, Material.STAINED_CLAY, colour);
                }
                for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1})
                    pillar(world, bx + sx * 8, floor, bz + sz * 7, 2, Material.STAINED_CLAY);
                break;

            case RUNNER:
                int[][] w = {{-7,5},{-5,5},{-3,5},{-1,3},{0,1},{1,3},{3,5},{5,5},{7,3}};
                for (int[] p : w)
                {
                    _runnerPath.add(base.clone().add(p[0] + 0.5, 0, p[1] + 0.5));
                    for (int dx = -1; dx <= 1; dx++)
                        set(world, bx + p[0] + dx, floor, bz + p[1], Material.STAINED_CLAY, (byte) 14);
                }
                break;

            case SHEEP_QUEST:
                for (int x = 3; x <= 8; x++) for (int z = -3; z <= 3; z++)
                    if (Math.abs(z) == 3 || x == 3 || x == 8)
                        set(world, bx + x, floor + 1, bz + z, Material.FENCE, (byte) 0);
                for (int x = 4; x <= 7; x++) for (int z = -2; z <= 2; z++)
                    set(world, bx + x, floor, bz + z, Material.WOOL, (byte) 0);
                for (int x = -8; x <= -4; x++) for (int z = -3; z <= 3; z++)
                    set(world, bx + x, floor, bz + z, Material.WOOL, (byte) 14);
                break;

            case SNAKE:
                int[][] snake = {{-8,5},{-5,5},{-2,5},{-2,2},{1,2},{4,2},{4,-1},{1,-1},{-2,-1},{-2,-4},{1,-4},{4,-4},{7,-4}};
                for (int[] p : snake)
                {
                    _snakePath.add(base.clone().add(p[0] + 0.5, 0, p[1] + 0.5));
                    set(world, bx + p[0], floor, bz + p[1], Material.STAINED_CLAY, (byte) 5);
                    set(world, bx + p[0], floor + 1, bz + p[1], Material.STAINED_CLAY, (byte) 13);
                }
                break;

            case SPLEEF:
                for (int i = -10; i <= 10; i++)
                {
                    set(world, bx + i, floor, bz - 10, Material.ICE, (byte) 0);
                    set(world, bx + i, floor, bz + 10, Material.ICE, (byte) 0);
                    set(world, bx - 10, floor, bz + i, Material.ICE, (byte) 0);
                    set(world, bx + 10, floor, bz + i, Material.ICE, (byte) 0);
                }
                break;

            case TURF_WARS:
                for (int x = -11; x <= -2; x++) for (int z = -7; z <= 7; z++)
                    set(world, bx + x, floor, bz + z, Material.STAINED_CLAY, (byte) 11);
                for (int x = 2; x <= 11; x++) for (int z = -7; z <= 7; z++)
                    set(world, bx + x, floor, bz + z, Material.STAINED_CLAY, (byte) 14);
                for (int x = -1; x <= 1; x++) for (int z = -3; z <= 3; z++)
                    set(world, bx + x, floor, bz + z, Material.SLIME_BLOCK, (byte) 0);
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void set(World world, int x, int y, int z, Material material, byte data)
    {
        if (world == null || y < 1 || y >= world.getMaxHeight()) return;
        Block block = world.getBlockAt(x, y, z);
        block.setType(material, false);
        if (material == Material.STAINED_CLAY || material == Material.STAINED_GLASS
                || material == Material.WOOL || material == Material.SMOOTH_BRICK
                || material == Material.LOG)
            block.setData(data);
    }

    private void square(World world, int x, int y, int z, int radius, Material material)
    {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                set(world, x + dx, y, z + dz, material, (byte) 0);
    }

    private void circle(World world, int x, int y, int z, int radius, Material material)
    {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                if (dx * dx + dz * dz <= radius * radius)
                    set(world, x + dx, y, z + dz, material, (byte) 0);
    }

    private void ring(World world, int x, int y, int z, int radius, Material material, byte data)
    {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
            {
                int d2 = dx * dx + dz * dz;
                if (d2 <= radius * radius && d2 >= (radius - 1) * (radius - 1))
                    set(world, x + dx, y, z + dz, material, data);
            }
    }

    private void pillar(World world, int x, int floor, int z, int height, Material material)
    {
        for (int h = 1; h <= height; h++)
            set(world, x, floor + h, z, material, (byte) 0);
    }

    private void mazePad(World world, int bx, int floor, int bz, int x, int z, byte colour)
    {
        Location center = new Location(world, bx + x + 0.5, floor + 1, bz + z + 0.5);
        _pads.add(center);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            set(world, bx + x + dx, floor, bz + z + dz, Material.STAINED_CLAY, colour);
        set(world, bx + x + 2, floor + 1, bz + z, Material.BEACON, (byte) 0);
    }

    @Override
    public void teleport()
    {
        Location view = getBaseLocation().clone().add(0, 4, 23);
        view.setDirection(getBaseLocation().clone().subtract(view).toVector());
        teleport(view);
    }

    @Override
    public void play()
    {
        _cleaned = false;
        _finished = false;
        _step = 0;
        _nextActionAt = System.currentTimeMillis() + 650L;
        _actors.clear();
        _props.clear();
        _sheep = null;

        Location base = getBaseLocation();
        List<org.bukkit.entity.Player> skins = new ArrayList<org.bukkit.entity.Player>();
        skins.add(_player);
        if (_other != null)
            for (org.bukkit.entity.Player player : _other)
                if (player != null && player.isOnline() && !skins.contains(player)) skins.add(player);

        for (int i = 0; i < 4; i++)
        {
            Location spawn = actorStart(base, i);
            spawn.setDirection(base.toVector().subtract(spawn.toVector()));
            _actors.add(getNPC(skins.get(i % skins.size()), spawn, false));
        }
        if (_scene == Scene.SHEEP_QUEST)
        {
            _sheep = base.getWorld().spawn(base.clone().add(-6, 0, 0), Sheep.class);
            _sheep.setColor(DyeColor.RED);
            _sheep.setCustomName(C.cRed + "Captured Sheep");
            _sheep.setCustomNameVisible(true);
            _props.add(_sheep);
        }
    }

    private Location actorStart(Location base, int index)
    {
        if (_scene == Scene.SNAKE && !_snakePath.isEmpty())
            return _snakePath.get(Math.max(0, _snakePath.size() - 1 - index)).clone();
        if (_scene == Scene.RUNNER && !_runnerPath.isEmpty())
            return _runnerPath.get(0).clone();
        if (_scene == Scene.MONSTER_MAZE)
            return base.clone().add(index == 0 ? -8 : (index - 2) * 2, 0, 2);
        if (_scene == Scene.EVOLUTION)
            return base.clone().add(index == 0 ? 0 : index * 1.5, 0, index == 0 ? 5 : -2);
        return base.clone().add(index == 0 ? 0 : (index % 2 == 0 ? 3 : -3), 0, index == 0 ? 5 : -2);
    }

    @EventHandler
    public void animateDemo(UpdateEvent event)
    {
        if (event.getType() != UpdateType.TICK || _cleaned || !isRunning() || _finished) return;
        long now = System.currentTimeMillis();
        if (now < _nextActionAt) return;

        if (_scene == Scene.SNAKE) { animateSnake(now); return; }
        if (_scene == Scene.RUNNER) { animateRunner(now); return; }
        if (_scene == Scene.EVOLUTION) { animateEvolution(now); return; }
        if (_scene == Scene.MONSTER_MAZE) { animateMaze(now); return; }
        if (_scene == Scene.SHEEP_QUEST) { animateSheep(now); return; }

        if (_step < _actors.size() - 1)
        {
            DisguisePlayer target = _actors.get(_step + 1);
            Entity actor = entity(target);
            if (actor != null)
            {
                if (actor instanceof ArmorStand) ((ArmorStand) actor).setGravity(true);
                Vector away = actor.getLocation().toVector().subtract(getBaseLocation().toVector());
                away.setY(0);
                if (away.lengthSquared() < 0.01) away = new Vector(_step % 2 == 0 ? 1 : -1, 0, 0);
                actor.setVelocity(away.normalize().multiply(1.15).setY(0.72));
                actor.getWorld().playSound(actor.getLocation(), Sound.VILLAGER_HIT, 1.0F, 0.8F + _step * 0.1F);
                Location impact = actor.getLocation();
                ParticleType particle = _scene == Scene.PAINTBALL ? ParticleType.BLOCK_DUST
                        : _scene == Scene.ONE_IN_THE_QUIVER ? ParticleType.FIREWORKS_SPARK
                        : ParticleType.CLOUD;
                UtilParticle.PlayParticle(particle, impact, 0.3F, 0.2F, 0.3F, 0.02F, 14, ViewDist.NORMAL);
                if (_scene == Scene.SPLEEF)
                {
                    Block under = actor.getLocation().subtract(0, 1, 0).getBlock();
                    if (under.getType() == Material.SNOW_BLOCK) under.setType(Material.AIR, false);
                }
            }
            _step++;
            _nextActionAt = now + 650L;
        }
        else
        {
            finale();
            _nextActionAt = now + 1000L;
        }
    }

    private void animateEvolution(long now)
    {
        Entity winner = actorEntity(0);
        if (winner == null) return;
        if (_step < 4)
        {
            Location point = getBaseLocation().clone().add(0.5, _step * 1.5, 5.5 - _step * 1.1);
            winner.teleport(point);
            UtilParticle.PlayParticle(ParticleType.PORTAL, point, 0.3F, 0.3F, 0.3F, 0.02F, 18, ViewDist.NORMAL);
            winner.getWorld().playSound(point, Sound.LEVEL_UP, 1.0F, 1.0F + _step * 0.1F);
            _step++;
            _nextActionAt = now + 700L;
        }
        else { finale(); _nextActionAt = now + 1000L; }
    }

    private void animateMaze(long now)
    {
        Entity winner = actorEntity(0);
        if (winner == null) return;
        if (_step < _pads.size())
        {
            Location pad = _pads.get(_step).clone();
            winner.teleport(pad);
            winner.getWorld().playSound(pad, Sound.LEVEL_UP, 1.0F, 1.2F);
            UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK, pad, 0.3F, 0.3F, 0.3F, 0.02F, 15, ViewDist.NORMAL);
            if (_step + 1 < _actors.size()) knockOff(_actors.get(_step + 1), _step);
            _step++;
            _nextActionAt = now + 700L;
        }
        else { finale(); _nextActionAt = now + 1000L; }
    }

    private void animateRunner(long now)
    {
        Entity winner = actorEntity(0);
        if (winner == null || _runnerPath.isEmpty()) return;
        if (_step < _runnerPath.size())
        {
            Location point = _runnerPath.get(_step).clone().add(0, 0.1, 0);
            winner.teleport(point);
            UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK, point, 0.1F, 0.1F, 0.1F, 0, 4, ViewDist.NORMAL);
            if (_step > 0)
            {
                Block behind = _runnerPath.get(_step - 1).clone().subtract(0, 1, 0).getBlock();
                if (behind.getType() == Material.STAINED_CLAY) behind.setType(Material.AIR, false);
            }
            _step++;
            _nextActionAt = now + 400L;
        }
        else { finale(); _nextActionAt = now + 1000L; }
    }

    private void animateSnake(long now)
    {
        if (_snakePath.isEmpty()) return;
        for (int i = 0; i < _actors.size(); i++)
        {
            int index = (_step - i) % _snakePath.size();
            if (index < 0) index += _snakePath.size();
            Entity actor = actorEntity(i);
            if (actor != null)
            {
                Location point = _snakePath.get(index).clone().add(0, 0.1, 0);
                actor.teleport(point);
                if (i == 0)
                    UtilParticle.PlayParticle(ParticleType.HAPPY_VILLAGER, point, 0.15F, 0.1F, 0.15F, 0.01F, 5, ViewDist.NORMAL);
            }
        }
        if (_step % 2 == 0) getBaseLocation().getWorld().playSound(getBaseLocation(), Sound.CLICK, 0.6F, 1.2F);
        _step++;
        if (_step >= _snakePath.size() + 3) { finale(); _nextActionAt = now + 1000L; }
        else _nextActionAt = now + 420L;
    }

    private void animateSheep(long now)
    {
        if (_sheep != null && _sheep.isValid() && _step < 6)
        {
            Location next = getBaseLocation().clone().add(-6 + _step * 2.2, 0, 0);
            _sheep.teleport(next);
            _sheep.getWorld().playSound(next, Sound.CLICK, 0.8F, 1.1F);
            _step++;
            _nextActionAt = now + 650L;
        }
        else { finale(); _nextActionAt = now + 1000L; }
    }

    private void knockOff(DisguisePlayer disguise, int index)
    {
        Entity actor = entity(disguise);
        if (actor == null) return;
        if (actor instanceof ArmorStand) ((ArmorStand) actor).setGravity(true);
        Vector away = actor.getLocation().toVector().subtract(getBaseLocation().toVector());
        away.setY(0);
        if (away.lengthSquared() < 0.01) away = new Vector(index % 2 == 0 ? 1 : -1, 0, 0);
        actor.setVelocity(away.normalize().multiply(1.15).setY(0.72));
        actor.getWorld().playSound(actor.getLocation(), Sound.VILLAGER_HIT, 1.0F, 0.9F);
    }

    private void finale()
    {
        if (_finished) return;
        _finished = true;
        Location center = getBaseLocation().clone().add(0, 1, 0);
        center.getWorld().playSound(center, Sound.LEVEL_UP, 1.5F, 1.0F);
        center.getWorld().playSound(center, Sound.FIREWORK_BLAST, 1.0F, 1.15F);
        UtilParticle.PlayParticle(ParticleType.FIREWORKS_SPARK, center, 0.75F, 0.65F, 0.75F, 0.08F, 36, ViewDist.NORMAL);
        UtilParticle.PlayParticle(ParticleType.CLOUD, center, 0.4F, 0.25F, 0.4F, 0.02F, 18, ViewDist.NORMAL);
    }

    private Entity actorEntity(int index)
    {
        return index < 0 || index >= _actors.size() ? null : entity(_actors.get(index));
    }

    private Entity entity(DisguisePlayer disguise)
    {
        if (disguise == null || disguise.getEntity() == null) return null;
        Entity entity = disguise.getEntity().getBukkitEntity();
        return entity != null && entity.isValid() ? entity : null;
    }

    private void removeNpc(DisguisePlayer disguise)
    {
        if (disguise == null) return;
        Entity entity = disguise.getEntity() == null ? null : disguise.getEntity().getBukkitEntity();
        Manager.getDisguiseManager().undisguise(disguise);
        if (entity != null && !entity.isDead()) entity.remove();
    }

    @Override
    public void finish()
    {
        if (_cleaned) return;
        _cleaned = true;
        for (DisguisePlayer actor : new ArrayList<DisguisePlayer>(_actors)) removeNpc(actor);
        _actors.clear();
        for (Entity prop : new ArrayList<Entity>(_props))
            if (prop != null && !prop.isDead()) prop.remove();
        _props.clear();
        _sheep = null;
        _pads.clear();
        _runnerPath.clear();
        _snakePath.clear();
    }

    public static final class Dragons extends WinEffectArcadeDemo {
        public Dragons(GadgetManager m) { super(m, "Arcade Dragons - Pyro Demo", "A Pyro-style finale on a floating platform as rivals are blasted away.", Material.FIREWORK, (byte) 0, Scene.DRAGONS); }
    }
    public static final class Evolution extends WinEffectArcadeDemo {
        public Evolution(GadgetManager m) { super(m, "Arcade Evolution - Ascension Demo", "Climb a compact tower to the dragon egg in a staged portal burst.", Material.DRAGON_EGG, (byte) 0, Scene.EVOLUTION); }
    }
    public static final class Gladiators extends WinEffectArcadeDemo {
        public Gladiators(GadgetManager m) { super(m, "Arcade Gladiators - Arena Demo", "A fenced gladiator arena where rivals are knocked away from the winner.", Material.IRON_SWORD, (byte) 0, Scene.GLADIATORS); }
    }
    public static final class MicroBattles extends WinEffectArcadeDemo {
        public MicroBattles(GadgetManager m) { super(m, "Arcade Micro Battles - Pillar Demo", "Keep the centre pillar while three rival dummies fall away.", Material.STAINED_GLASS, (byte) 3, Scene.MICRO_BATTLES); }
    }
    public static final class MonsterMaze extends WinEffectArcadeDemo {
        public MonsterMaze(GadgetManager m) { super(m, "Arcade Monster Maze - Safe Pad Demo", "Pilot from the start to three coloured Safe Pads in a miniature maze.", Material.ROTTEN_FLESH, (byte) 0, Scene.MONSTER_MAZE); }
    }
    public static final class OneInTheQuiver extends WinEffectArcadeDemo {
        public OneInTheQuiver(GadgetManager m) { super(m, "Arcade One in the Quiver - Bullseye Demo", "A compact archery range with a coloured bullseye and staged impact bursts.", Material.ARROW, (byte) 0, Scene.ONE_IN_THE_QUIVER); }
    }
    public static final class Paintball extends WinEffectArcadeDemo {
        public Paintball(GadgetManager m) { super(m, "Arcade Paintball - Clutch Demo", "A colourful target range for the proposed clutch finale and paint-impact visuals.", Material.FIREBALL, (byte) 0, Scene.PAINTBALL); }
    }
    public static final class Runner extends WinEffectArcadeDemo {
        public Runner(GadgetManager m) { super(m, "Arcade Runner - Red W Demo", "Run the red W course while clay segments disappear behind the winner.", Material.STAINED_CLAY, (byte) 14, Scene.RUNNER); }
    }
    public static final class SheepQuest extends WinEffectArcadeDemo {
        public SheepQuest(GadgetManager m) { super(m, "Arcade Sheep Quest - Capture Demo", "A sheep moves into a small capture pen for a last-second celebration.", Material.WOOL, (byte) 0, Scene.SHEEP_QUEST); }
    }
    public static final class Snake extends WinEffectArcadeDemo {
        public Snake(GadgetManager m) { super(m, "Arcade Snake - Zigzag Demo", "A snake of player dummies follows a zigzag course to the finish.", Material.SLIME_BLOCK, (byte) 0, Scene.SNAKE); }
    }
    public static final class Spleef extends WinEffectArcadeDemo {
        public Spleef(GadgetManager m) { super(m, "Arcade Spleef - Breakaway Demo", "Rivals drop from a snow platform while the winner stays standing.", Material.SNOW_BLOCK, (byte) 0, Scene.SPLEEF); }
    }
    public static final class TurfWars extends WinEffectArcadeDemo {
        public TurfWars(GadgetManager m) { super(m, "Arcade Turf Wars - No-Scope Demo", "A red-versus-blue firing line with a central bounce platform and finale.", Material.BOW, (byte) 0, Scene.TURF_WARS); }
    }
}
