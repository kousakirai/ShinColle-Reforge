package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.reference.ID;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Look at idle random direction goal.
 * Ported from EntityAIShipLookIdle (setMutexBits: 0)
 */
@Deprecated
public class ShipLookIdleGoal extends Goal {

    private final Mob entity;
    private double lookX;
    private double lookZ;
    private int idleTime;

    public ShipLookIdleGoal(Mob entity) {
        this.entity = entity;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.entity instanceof IShipAttackBase ship) {
            if (ship.getStateFlag(ID.F.NoFuel)) return false;
        }
        return this.entity.getRandom().nextFloat() < 0.02F;
    }

    @Override
    public boolean canContinueToUse() {
        return this.idleTime >= 0;
    }

    @Override
    public void start() {
        double d0 = (Math.PI * 2D) * this.entity.getRandom().nextDouble();
        this.lookX = Math.cos(d0);
        this.lookZ = Math.sin(d0);
        this.idleTime = 20 + this.entity.getRandom().nextInt(20);
    }

    @Override
    public void tick() {
        --this.idleTime;
        this.entity.getLookControl().setLookAt(
                this.entity.getX() + this.lookX,
                this.entity.getEyeY(),
                this.entity.getZ() + this.lookZ);
    }
}
