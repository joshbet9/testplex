package nautilus.game.arcade.game.games.runner;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileHitEvent;

import mineplex.core.Managers;
import mineplex.core.common.Pair;
import mineplex.core.common.util.C;
import mineplex.core.common.util.MapUtil;
import mineplex.core.common.util.UtilBlock;
import mineplex.core.common.util.UtilEnt;
import mineplex.core.common.util.UtilPlayer;
import mineplex.core.common.util.UtilTime;
import mineplex.core.projectile.IThrown;
import mineplex.core.projectile.ProjectileUser;
import mineplex.core.updater.UpdateType;
import mineplex.core.updater.event.UpdateEvent;

import nautilus.game.arcade.ArcadeManager;
import nautilus.game.arcade.GameType;
import nautilus.game.arcade.cosmetics.ArcadeCosmeticsMenu;
import nautilus.game.arcade.game.SoloGame;
import nautilus.game.arcade.game.games.runner.kits.KitArcher;
import nautilus.game.arcade.game.games.runner.kits.KitFrosty;
import nautilus.game.arcade.game.games.runner.kits.KitLeaper;
import nautilus.game.arcade.game.modules.compass.CompassModule;
import nautilus.game.arcade.kit.Kit;
import nautilus.game.arcade.managers.chat.ChatStatData;
import nautilus.game.arcade.stats.DistanceTraveledStatTracker;

public class Runner extends SoloGame implements IThrown
{

	private static final String[] DESCRIPTION =
			{
					C.cGreen + "Blocks Fall" + C.Reset + " from underneath you.",
					C.cAqua + "Keep Running" + C.Reset + " to stay alive.",
					"Avoid " + C.cRed + "Falling Blocks" + C.Reset + " from above.",
					C.cYellow + "Last Player" + C.Reset + " alive wins!"
			};
	private static final long BLOCK_DECAY = 600;
	private static final long BLOCK_LIFETIME = 1200;

	private final Map<Block, Long> _blocks = new HashMap<>();
	private final Map<Block, Byte> _blockColours = new HashMap<>();

	public Runner(ArcadeManager manager)
	{
		super(manager, GameType.Runner, new Kit[]
				{
						new KitLeaper(manager),
						new KitArcher(manager),
						new KitFrosty(manager)
				}, DESCRIPTION);

		DamagePvP = false;
		HungerSet = 20;
		WorldWaterDamage = 4;
		PrepareFreeze = false;
		NightVision = true;

		new CompassModule()
				.register(this);

		registerStatTrackers(
				new DistanceTraveledStatTracker(this, "MarathonRunner")
		);

		registerChatStats(
				new ChatStatData("MarathonRunner", "Distance ran", true),
				BlankLine,
				new ChatStatData("kit", "Kit", true)
		);
	}

	@EventHandler
	public void arrowDamage(ProjectileHitEvent event)
	{
		Entity entity = event.getEntity();

		if (!(entity instanceof Arrow) || !IsLive())
		{
			return;
		}

		Manager.runSyncLater(() ->
		{
			Block block = UtilEnt.getHitBlock(entity);

			double radius = 2.5;

			Player owner = ((Arrow) entity).getShooter() instanceof Player ? (Player) ((Arrow) entity).getShooter() : null;
			for (Block other : UtilBlock.getInRadius(block.getLocation().add(0.5, 0.5, 0.5), radius).keySet())
			{
				addBlock(other, owner);
			}

			entity.remove();
		}, 0);
	}

	private void addBlock(Block block, Player owner)
	{
		if (block == null || block.getType() == Material.AIR || block.getType() == Material.BEDROCK || block.isLiquid() || block.getRelative(BlockFace.UP).getTypeId() != 0 || _blocks.containsKey(block))
		{
			return;
		}

		byte colour = getTrailColour(owner);
		_blocks.put(block, System.currentTimeMillis());
		_blockColours.put(block, Byte.valueOf(colour));
		MapUtil.QuickChangeBlockAt(block.getLocation(), Material.STAINED_CLAY, colour);
	}

	/** No selection keeps Reborn's original red trail; equipped colours are visible to everyone. */
	private byte getTrailColour(Player owner)
	{
		if (owner == null) return (byte) 14;
		String selected = ArcadeCosmeticsMenu.getEquippedChoice(owner.getUniqueId(), "runner");
		if ("white".equals(selected)) return (byte) 0;
		if ("orange".equals(selected)) return (byte) 1;
		if ("magenta".equals(selected)) return (byte) 2;
		if ("lightblue".equals(selected)) return (byte) 3;
		if ("yellow".equals(selected)) return (byte) 4;
		if ("lime".equals(selected)) return (byte) 5;
		if ("pink".equals(selected)) return (byte) 6;
		if ("gray".equals(selected)) return (byte) 7;
		return (byte) 14;
	}

	@EventHandler
	public void blockBreak(UpdateEvent event)
	{
		if (event.getType() != UpdateType.TICK || !IsLive())
		{
			return;
		}

		for (Player player : GetPlayers(true))
		{
			Pair<Location, Location> box = UtilEnt.getSideStandingBox(player);
			Location min = box.getLeft(), max = box.getRight();

			for (int x = min.getBlockX(); x <= max.getBlockX(); x++)
			{
				for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
				{
					addBlock(player.getLocation().add(x, -0.5, z).getBlock(), player);
				}
			}
		}

		_blocks.entrySet().removeIf(entry ->
		{
			Block block = entry.getKey();
			long time = entry.getValue();

			if (!UtilTime.elapsed(time, BLOCK_DECAY))
			{
				return false;
			}

			MapUtil.QuickChangeBlockAt(block.getLocation(), Material.AIR);
			Byte selectedColour = _blockColours.remove(block);
			byte fallingColour = selectedColour == null ? (byte) 14 : selectedColour.byteValue();
			FallingBlock ent = block.getWorld().spawnFallingBlock(block.getLocation(), Material.STAINED_CLAY, fallingColour);
			Manager.GetProjectile().AddThrow(ent, null, this, BLOCK_LIFETIME, true, false, false, false, 0.3F);
			return true;
		});
	}

	@EventHandler
	public void blockForm(EntityChangeBlockEvent event)
	{
		blockSmash(event.getEntity());
		event.setCancelled(true);
	}

	private void blockSmash(Entity entity)
	{
		if (!(entity instanceof FallingBlock))
		{
			return;
		}

		if (Math.random() < 0.3)
		{
			FallingBlock block = (FallingBlock) entity;
			block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
		}

		entity.remove();
	}

	@Override
	public void Collide(LivingEntity target, Block block, ProjectileUser data)
	{
		if (target == null || UtilPlayer.isSpectator(target) || target.getLocation().getY() > data.getThrown().getLocation().getY() + 0.5)
		{
			return;
		}

		Manager.GetDamage().NewDamageEvent(target, data.getThrower(), null, DamageCause.ENTITY_ATTACK, 6, true, true, false, "Falling Block", "Falling Block");
		blockSmash(data.getThrown());
	}

	@Override
	public void Idle(ProjectileUser data)
	{

	}

	@Override
	public void Expire(ProjectileUser data)
	{
		data.getThrown().remove();
	}

	@Override
	public void ChunkUnload(ProjectileUser data)
	{
		data.getThrown().remove();
	}
}
