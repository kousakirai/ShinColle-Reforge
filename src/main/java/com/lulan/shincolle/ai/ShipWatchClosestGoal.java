package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.ID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Watch closest entity goal.
 * Ported from EntityAIShipWatchClosest (setMutexBits: 0)
 */
@Deprecated
public class ShipWatchClosestGoal extends Goal {

    private final Mob entity;
    private final Class<? extends LivingEntity> watchedClass;
    private final float maxDistance;
    private final float chance;
    private LivingEntity closestEntity;
    private int lookTime;

    public ShipWatchClosestGoal(Mob entity, Class<? extends LivingEntity> watchedClass, float maxDistance, float chance) {
        this.entity = entity;
        this.watchedClass = watchedClass;
        this.maxDistance = maxDistance;
        this.chance = chance;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.entity instanceof IShipAttackBase ship) {
            if (ship.getStateFlag(ID.F.NoFuel)) return false;
        }

        if (this.entity.getRandom().nextFloat() >= this.chance) return false;

        this.closestEntity = this.entity.level().getNearestEntity(
                this.entity.level().getEntitiesOfClass(this.watchedClass,
                        this.entity.getBoundingBox().inflate(this.maxDistance, 3.0D, this.maxDistance)),
                net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat()
                        .range(this.maxDistance),
                this.entity,
                this.entity.getX(), this.entity.getEyeY(), this.entity.getZ());

        return this.closestEntity != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.closestEntity == null || !this.closestEntity.isAlive()) return false;
        if (this.entity.distanceToSqr(this.closestEntity) > (double) (this.maxDistance * this.maxDistance))
            return false;
        return this.lookTime > 0;
    }

    @Override
    public void start() {
        this.lookTime = 40 + this.entity.getRandom().nextInt(40);
    }

    @Override
    public void stop() {
        this.closestEntity = null;
    }

    @Override
    public void tick() {
        this.entity.getLookControl().setLookAt(
                this.closestEntity.getX(),
                this.closestEntity.getEyeY(),
                this.closestEntity.getZ());
        --this.lookTime;
    }
}
