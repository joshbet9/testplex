package mineplex.core.disguise;

import org.bukkit.entity.*;

import mineplex.core.disguise.disguises.*;

public class DisguiseFactory
{
	public static DisguiseBase createDisguise(Entity disguised, EntityType disguiseType)
	{
		switch (disguiseType)
		{
			case BAT:
				return new DisguiseBat(disguised);
			case BLAZE:
				return new DisguiseBlaze(disguised);
			case OCELOT:
				return new DisguiseCat(disguised);
			case CHICKEN:
				return new DisguiseChicken(disguised);
			case COW:
				return new DisguiseCow(disguised);
			case CAVE_SPIDER:
				return new DisguiseCaveSpider(disguised);
			case CREEPER:
				return new DisguiseCreeper(disguised);
			case ENDERMAN:
				return new DisguiseEnderman(disguised);
			case ENDERMITE:
				return new DisguiseEndermite(disguised);
			case HORSE:
				return new DisguiseHorse(disguised);
			case IRON_GOLEM:
				return new DisguiseIronGolem(disguised);
			case MAGMA_CUBE:
				return new DisguiseMagmaCube(disguised);
			case MUSHROOM_COW:
				return new DisguiseMushroomCow(disguised);
			case PIG:
				return new DisguisePig(disguised);
			case PIG_ZOMBIE:
				return new DisguisePigZombie(disguised);
			case RABBIT:
				return new DisguiseRabbit(disguised);
			case SILVERFISH:
				return new DisguiseSilverfish(disguised);
			case PLAYER:
				throw new UnsupportedOperationException("Player disguises must be initialized via constructor");
			case SHEEP:
				return new DisguiseSheep(disguised);
			case SKELETON:
				return new DisguiseSkeleton(disguised);
			case SLIME:
				return new DisguiseSlime(disguised);
			case SNOWMAN:
				return new DisguiseSnowman(disguised);
			case SPIDER:
				return new DisguiseSpider(disguised);
			case SQUID:
				return new DisguiseSquid(disguised);
			case VILLAGER:
				return new DisguiseVillager(disguised);
			case GUARDIAN:
				return new DisguiseGuardian(disguised);
			case WITCH:
				return new DisguiseWitch(disguised);
			case WOLF:
				return new DisguiseWolf(disguised);
			case ZOMBIE:
				return new DisguiseZombie(disguised);
			default:
				return null;
		}
	}
}
