package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Melee attack on collide goal.
 * Ported from EntityAIShipAttackOnCollide (setMutexBits: 4)
 * <p>
 * Attack delay is dynamic, using CombatHelper.getAttackDelay(aspd, 0).
 * Melee range is width^2 * 16 (matching original formula).
 * Pathfinds every 32 ticks, only when out of melee range.
 */
public class ShipAttackOnCollideGoal extends Goal {

    private final IShipAttackBase host;
    private final Mob entity;
    private final double speed;
    private LivingEntity target;
    private int delayAttack;
    private int delayMax;

    public ShipAttackOnCollideGoal(IShipAttackBase host, double speed) {
        this.host = host;
        this.entity = (Mob) host;
        this.speed = speed;
        this.delayMax = 20;
        this.delayAttack = 20;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // check riding and sitting first (original order)
        if (this.entity.isPassenger() || this.host.getIsSitting())
            return false;

        this.target = this.entity.getTarget();
        return this.target != null && this.target.isAlive();

    }

    @Override
    public void start() {
        // original startExecuting is empty
    }

    @Override
    public boolean canContinueToUse() {
        if (this.host == null) return false;
        if (this.target != null && this.target.isAlive() && !this.entity.getNavigation().isDone()) {
            return true;
        }
        return this.canUse();
    }

    @Override
    public void stop() {
        this.target = null;
        this.entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null || !this.target.isAlive()) {
            this.stop();
            return;
        }

        // look at target
        this.entity.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        // calculate melee range: width^2 * 16 (matching original)
        double distAttack = this.entity.getBbWidth() * this.entity.getBbWidth() * 16F;
        double distTarget = this.entity.distanceToSqr(
                this.target.getX(), this.target.getBoundingBox().minY, this.target.getZ());

        // every 32 ticks: update attack delay and pathfind conditionally
        if (this.entity.tickCount % 32 == 0) {
            // dynamically recalculate attack delay from ship stats
            this.delayMax = CombatHelper.getAttackDelay(
                    this.host.getAttrs().getAttackSpeed(), 0);

            // only pathfind when out of melee range; clear path when in range
            if (distTarget > distAttack) {
                this.entity.getNavigation().moveTo(this.target, this.speed);
            } else {
                this.entity.getNavigation().stop();
            }
        }

        // decrement attack delay
        this.delayAttack--;

        // attack when in range and delay elapsed
        if (distTarget <= distAttack && this.delayAttack <= 0) {
            this.delayAttack = this.delayMax;

            // arm swing animation
            if (!this.entity.getMainHandItem().isEmpty()) {
                this.entity.swing(InteractionHand.MAIN_HAND);
            }

            this.entity.doHurtTarget(this.target);
        }
    }
}
