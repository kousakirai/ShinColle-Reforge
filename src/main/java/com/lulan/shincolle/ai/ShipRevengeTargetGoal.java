package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Revenge target goal - sets revenge target as attack target.
 * Ported from EntityAIShipRevengeTarget (setMutexBits: 1)
 */
public class ShipRevengeTargetGoal extends Goal {

    private final IShipAttackBase host;
    private final java.util.function.Predicate<Entity> targetSelector;
    private int oldRevengeTime;

    public ShipRevengeTargetGoal(IShipAttackBase host) {
        this.host = host;
        if (host instanceof BasicEntityShipHostile) {
            this.targetSelector = new TargetHelper.RevengeSelectorForHostile((Entity) host);
        } else {
            this.targetSelector = new TargetHelper.RevengeSelector((Entity) host);
        }
        this.oldRevengeTime = 0;
        // [PORT] 1.10.2 targetTasks mutex -> 1.20.1 TARGET control flag.
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.oldRevengeTime != this.host.getEntityRevengeTime() && this.host.getEntityRevengeTarget() != null) {
            Entity revengeTarget = this.host.getEntityRevengeTarget();
            return this.targetSelector.test(revengeTarget);
        }
        return false;
    }

    @Override
    public void start() {
        this.host.setEntityTarget(this.host.getEntityRevengeTarget());
        this.oldRevengeTime = this.host.getEntityRevengeTime();
        this.host.setEntityRevengeTarget(null);
    }

    @Override
    public void stop() {
        super.stop();
    }
}
