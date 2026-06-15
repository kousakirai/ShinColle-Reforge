package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.utility.BlockHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Aircraft entity attack goal (for airplane entities themselves).
 * Ported from EntityAIShipAircraftAttack (setMutexBits: 3)
 */
public class ShipAircraftAttackGoal extends Goal {

    private final BasicEntityAirplane host;
    private LivingEntity target;
    private int atkDelay;
    private int maxDelay;
    private float attackRange;
    private float rangeSq;
    private double[] randPos = new double[3];

    public ShipAircraftAttackGoal(BasicEntityAirplane host) {
        this.host = host;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.host.canFindTarget())
            return false;

        LivingEntity target = this.host.getTarget();
        if (this.host.tickCount > 20 && target != null && target.isAlive() &&
                ((this.host.useAmmoLight() && this.host.hasAmmoLight()) ||
                        (this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()))) {
            this.target = target;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.atkDelay = 0;
        this.maxDelay = (int) (ConfigHandler.baseAttackSpeed[4] / Math.max(this.host.getAttrs().getAttackSpeed(), 0.01F))
                + ConfigHandler.fixedAttackDelay[4];
        this.attackRange = this.host.useAmmoHeavy() ? 16.0F : 6.0F;
        this.rangeSq = this.attackRange * this.attackRange;

        // init movement target position
        if (this.target != null) {
            this.randPos[0] = this.target.getX();
            this.randPos[1] = this.target.getY();
            this.randPos[2] = this.target.getZ();
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.host.canFindTarget())
            return false;
        return this.canUse() || (this.target != null && this.target.isAlive());
    }

    @Override
    public void stop() {
        this.target = null;

        // keep moving - aircraft shouldn't stop in air
        if (this.host.useAmmoHeavy()) {
            this.randPos = BlockHelper.findRandomPosition(this.host, this.host, 12D, 4D, 2);
        } else {
            this.randPos = BlockHelper.findRandomPosition(this.host, this.host, 4.5D, 1.5D, 2);
        }
        this.host.getNavigation().moveTo(randPos[0], randPos[1], randPos[2], 1D);
    }

    @Override
    public void tick() {
        if (this.target == null)
            return;

        boolean onSight = this.host.getSensing().hasLineOfSight(this.target);
        double distSq = this.host.distanceToSqr(this.target);

        // navigate toward target periodically using custom ship navigator
            if (this.host.useAmmoHeavy()) {
                this.randPos = BlockHelper.findRandomPosition(this.host, this.target, 12D, 4D, 2);
            } else {
                this.randPos = BlockHelper.findRandomPosition(this.host, this.target, 4.5D, 1.5D, 2);
            }

            if (distSq > this.rangeSq) {
                this.host.getNavigation().moveTo(randPos[0], randPos[1], randPos[2], 1D);
            } else {
                this.host.getNavigation().moveTo(randPos[0], randPos[1], randPos[2], 0.4D);
            }

        this.atkDelay--;

        // attack when able
        if (this.atkDelay <= 0 && onSight && distSq < this.rangeSq) {
            if (this.host.useAmmoLight() && this.host.hasAmmoLight()) {
                this.host.attackEntityWithAmmo(this.target);
                this.atkDelay = this.maxDelay;
            }

            if (this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()) {
                this.host.attackEntityWithHeavyAmmo(this.target);
                this.atkDelay = this.maxDelay;
            }
        }
    }
}
