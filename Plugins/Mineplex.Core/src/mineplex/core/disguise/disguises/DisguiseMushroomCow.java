package mineplex.core.disguise.disguises;

import org.bukkit.entity.EntityType;

/** Packet disguise for the 1.8 mushroom cow (mooshroom) entity. */
public class DisguiseMushroomCow extends DisguiseAnimal
{
	public DisguiseMushroomCow(org.bukkit.entity.Entity entity)
	{
		super(EntityType.MUSHROOM_COW, entity);
	}

	@Override
	public String getHurtSound()
	{
		return "mob.sheep.say";
	}
}
