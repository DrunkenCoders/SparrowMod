package net.canarymod.api.entity.living.animal;

import net.canarymod.api.entity.EntityType;
import net.minecraft.entity.passive.EntityOcelot;

/**
 * Ocelot wrapper implementation
 *
 * @author Jason (darkdiplomat)
 */
public class CanaryOcelot extends CanaryTameable implements Ocelot {

    /**
     * Constructs a new wrapper for EntityOcelot
     *
     * @param entity
     *         the EntityOcelot to wrap
     */
    public CanaryOcelot(EntityOcelot entity) {
        super(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.OCELOT;
    }

    @Override
    public String getFqName() {
        return "Ocelot";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SkinType getSkinType() {
        return SkinType.values()[getHandle().ce()];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSkinType(SkinType type) {
        getHandle().s(type.ordinal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityOcelot getHandle() {
        return (EntityOcelot) entity;
    }
}
