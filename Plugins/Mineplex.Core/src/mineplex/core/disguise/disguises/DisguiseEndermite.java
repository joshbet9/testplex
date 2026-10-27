package mineplex.core.disguise.disguises;

import org.bukkit.entity.EntityType;

/** Packet disguise for the 1.8 endermite entity. */
public class DisguiseEndermite extends DisguiseMonster
{
	public DisguiseEndermite(org.bukkit.entity.Entity entity)
	{
		super(EntityType.ENDERMITE, entity);
	}

	@Override
	protected String getHurtSound()
	{
		return "mob.endermite.hit";
	}
}
