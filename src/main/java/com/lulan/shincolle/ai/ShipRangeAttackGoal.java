package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipCannonAttack;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.DebugProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Range attack goal (cannon fire).
 * Ported from EntityAIShipRangeAttack (setMutexBits: 1)
 */
public class ShipRangeAttackGoal extends Goal {
    private static final int INITIAL_LIGHT_DELAY = 20;
    private static final int INITIAL_HEAVY_DELAY = 40;
    private static final int STUCK_RESET_THRESHOLD = -40;

    private final IShipCannonAttack host;
    private final Mob entity;
    private Entity target;
    private int delayLight;
    private int maxDelayLight;
    private int delayHeavy;
    private int maxDelayHeavy;
    private int onSightTime;
    private float range;
    private float rangeSq;
    private int aimTime;

    public ShipRangeAttackGoal(IShipCannonAttack host) {
        this.host = host;
        this.entity = (Mob) host;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));

        this.delayLight = INITIAL_LIGHT_DELAY;
        this.delayHeavy = INITIAL_HEAVY_DELAY;
        this.maxDelayLight = INITIAL_LIGHT_DELAY;
        this.maxDelayHeavy = INITIAL_HEAVY_DELAY;
    }

    @Override
    public boolean canUse() {
        ProfilerFiller profiler = DebugProfiler.push(this.entity.level(), "shincolle.ai.range_attack.can_use");
        try {
            if (this.host.getIsSitting() || this.host.getStateMinor(ID.M.CraneState) > 0) {
                DebugProfiler.count(profiler, "shincolle.ai.range_attack.blocked.sit_or_crane");
                return false;
            }

            if (this.host.getIsRiding()) {
                if (this.entity.getVehicle() instanceof BasicEntityMount) {
                    DebugProfiler.count(profiler, "shincolle.ai.range_attack.blocked.mount_controls_attack");
                    return false;
                }
            }

            LivingEntity target = this.entity.getTarget();
            if (target != null && target.isAlive() &&
                    ((this.host.getAttackType(ID.F.AtkType_Light) && this.host.getStateFlag(ID.F.UseAmmoLight)
                            && this.host.hasAmmoLight()) ||
                            (this.host.getAttackType(ID.F.AtkType_Heavy) && this.host.getStateFlag(ID.F.UseAmmoHeavy)
                                    && this.host.hasAmmoHeavy()))) {
                this.target = target;
                DebugProfiler.count(profiler, "shincolle.ai.range_attack.can_use.success");
                return true;
            }

            DebugProfiler.count(profiler, "shincolle.ai.range_attack.can_use.no_valid_target_or_ammo");
            return false;
        } finally {
            DebugProfiler.pop(profiler);
        }
    }

    @Override
    public void start() {
        this.updateAttackParms();

        if (this.delayLight <= this.aimTime) {
            this.delayLight = this.aimTime;
        }
        if (this.delayHeavy <= this.aimTime * 2) {
            this.delayHeavy = this.aimTime * 2;
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.target.isAlive()
                && !this.host.getIsSitting();
    }

    @Override
    public void stop() {
        this.target = null;
        this.onSightTime = 0;
        this.entity.getNavigation().stop();
    }

    @Override
    public void tick() {
        ProfilerFiller profiler = DebugProfiler.push(this.entity.level(), "shincolle.ai.range_attack.tick");
        try {
            if (this.target == null) {
                DebugProfiler.count(profiler, "shincolle.ai.range_attack.tick.no_target");
                return;
            }

            // update attributes periodically
            if (this.entity.tickCount % 32 == 0) {
                this.updateAttackParms();
            }

            this.delayLight--;
            this.delayHeavy--;

            double distSq = this.entity.distanceToSqr(this.target);
            boolean onSight = this.entity.getSensing().hasLineOfSight(this.target);

            if (onSight) {
                ++this.onSightTime;
            } else {
                this.onSightTime = 0;

                if (this.host.getStateFlag(ID.F.OnSightChase)) {
                    DebugProfiler.count(profiler, "shincolle.ai.range_attack.tick.lost_sight_stop");
                    this.stop();
                    return;
                }
            }
            // Close the distance until the target is both in range and visible.
            // The former condition required a lost line of sight as well, which
            // left ships stationary when they could see a target beyond range.
            if (distSq > this.rangeSq || !onSight) {
                this.entity.getNavigation().moveTo(this.target, 1.0D);
            } else {
                this.entity.getNavigation().stop();
            }

            this.entity.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            // fire if delay done, on sight, in range, and aimed long enough
            if (onSight && distSq <= this.rangeSq && this.onSightTime >= this.aimTime) {
                // light attack
                if (this.delayLight <= 0 && this.host.useAmmoLight() && this.host.hasAmmoLight()) {
                    DebugProfiler.count(profiler, "shincolle.ai.range_attack.tick.fire_light");
                    this.host.attackEntityWithAmmo(this.target);
                    this.delayLight = this.maxDelayLight;
                }
                // heavy attack
                if (this.delayHeavy <= 0 && this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()) {
                    DebugProfiler.count(profiler, "shincolle.ai.range_attack.tick.fire_heavy");
                    this.host.attackEntityWithHeavyAmmo(this.target);
                    this.delayHeavy = this.maxDelayHeavy;
                }
            }

            // reset if stuck too long without hitting
            if (this.delayHeavy < -40 && this.delayLight < -40) {
                DebugProfiler.count(profiler, "shincolle.ai.range_attack.tick.stuck_reset");
                this.delayLight = 20;
                this.delayHeavy = 20;
                this.stop();
            }
        } finally {
            DebugProfiler.pop(profiler);
        }
    }

    private boolean isMountedOnShipMount() {
        return this.host.getIsRiding() && this.entity.getVehicle() instanceof BasicEntityMount;
    }

    private boolean canUseAnyRangedAttack() {
        boolean canUseLight = this.host.getAttackType(ID.F.AtkType_Light)
                && this.host.getStateFlag(ID.F.UseAmmoLight)
                && this.host.hasAmmoLight();
        boolean canUseHeavy = this.host.getAttackType(ID.F.AtkType_Heavy)
                && this.host.getStateFlag(ID.F.UseAmmoHeavy)
                && this.host.hasAmmoHeavy();
        return canUseLight || canUseHeavy;
    }

    private void updateAttackParms() {
        float atkSpd = this.host.getAttrs().getAttackSpeed();
        // attack delay = baseAttackSpeed / attackSpeed + fixedAttackDelay
        this.maxDelayLight = Math.max(5,
                (int) (ConfigHandler.baseAttackSpeed[1] / Math.max(atkSpd, 0.01F))
                        + ConfigHandler.fixedAttackDelay[1]);
        this.maxDelayHeavy = Math.max(10,
                (int) (ConfigHandler.baseAttackSpeed[2] / Math.max(atkSpd, 0.01F))
                        + ConfigHandler.fixedAttackDelay[2]);
        this.aimTime = (int) (20.0F * (150 - this.host.getLevel()) / 150.0F) + 10;
        this.range = this.host.getAttrs().getAttackRange();
        this.rangeSq = this.range * this.range;
    }
}
