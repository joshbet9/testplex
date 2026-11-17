package mineplex.core.disguise.disguises;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/** Packet disguise for the 1.8 silverfish entity. */
public class DisguiseSilverfishModern extends DisguiseMonster
{
	public DisguiseSilverfishModern(Entity entity)
	{
		super(EntityType.SILVERFISH, entity);
	}

	@Override
	protected String getHurtSound()
	{
		return "mob.silverfish.say";
	}
}
