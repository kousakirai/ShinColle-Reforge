package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Carrier aircraft launch/attack goal.
 * Ported from EntityAIShipCarrierAttack (setMutexBits: 2)
 * <p>
 * Alternates between light and heavy aircraft launches.
 * Attack delay calculated via CombatHelper.getAttackDelay:
 * light aircraft: type 3
 * heavy aircraft: type 4
 */
public class ShipCarrierAttackGoal extends Goal {

    private final IShipAircraftAttack host;
    private final Mob entity;
    private Entity target;
    private int launchDelay;
    private int launchDelayMax;
    private boolean launchType; // true = light, false = heavy
    private float range;
    private float rangeSq;
    private double distSq;
    private double distX, distY, distZ;

    public ShipCarrierAttackGoal(IShipAircraftAttack host) {
        this.host = host;
        this.entity = (Mob) host;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));

        this.launchDelay = 20;
        this.launchDelayMax = 40;
        this.launchType = false;
    }

    @Override
    public boolean canUse() {
        if (this.host.getIsSitting() || this.host.getStateMinor(ID.M.CraneState) > 0) {
            return false;
        }

        if (this.host.getIsRiding()) {
            if (this.entity.getVehicle() instanceof BasicEntityMount) {
                return false;
            }
        }

        LivingEntity target = this.entity.getTarget();

        if (target != null && target.isAlive() &&
                ((this.host.getAttackType(ID.F.AtkType_AirLight) && this.host.getStateFlag(ID.F.UseAirLight)
                        && this.host.hasAmmoLight() && this.host.hasAirLight()) ||
                        (this.host.getAttackType(ID.F.AtkType_AirHeavy) && this.host.getStateFlag(ID.F.UseAirHeavy)
                                && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()))) {
            this.target = target;
            return true;
        }

        return false;
    }

    @Override
    public void start() {
        // zero out distance state (original zeroes all distance vars)
        this.distSq = 0D;
        this.distX = 0D;
        this.distY = 0D;
        this.distZ = 0D;
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
    }

    @Override
    public void tick() {
        if (this.target == null || this.host == null)
            return;

        boolean onSight = this.entity.getSensing().hasLineOfSight(this.target);

        // on-sight-chase: stop AI if target out of sight and flag set
        if (!onSight) {
            if (this.host.getStateFlag(ID.F.OnSightChase)) {
                this.stop();
                return;
            }
        }

        // update attributes every 64 ticks
        if (this.entity.tickCount % 31 == 0) {
            float atkSpd = this.host.getAttrs().getAttackSpeed();

            // calculate attack delay based on current launch type
            if (this.launchType) {
                // light aircraft: type 3
                this.launchDelayMax = CombatHelper.getAttackDelay(atkSpd, 3);
            } else {
                // heavy aircraft: type 4
                this.launchDelayMax = CombatHelper.getAttackDelay(atkSpd, 4);
            }

            this.range = this.host.getAttrs().getAttackRange();
            this.rangeSq = this.range * this.range;
        }

        // chase / stop logic with cached distance (matching original's two-stage pattern)
        // recalculate distance when out of range
        this.distX = this.target.getX() - this.entity.getX();
        this.distY = this.target.getY() - this.entity.getY();
        this.distZ = this.target.getZ() - this.entity.getZ();
        this.distSq = distX * distX + distY * distY + distZ * distZ;

        if (this.distSq < this.rangeSq && onSight && !this.host.getStateFlag(ID.F.UseMelee)) {
            // in range now, stop moving
            this.entity.getNavigation().stop();
        } else {
            // still out of range, chase every 32 ticks
            this.entity.getNavigation().moveTo(this.target, 1.0D);
        }

        // look at target (original uses target Y + 2D, modern API uses eye height)
        this.entity.getLookControl().setLookAt(this.target, 30.0F, 60.0F);

        this.launchDelay--;

        // handle single ammo type mode
        if (!this.host.getStateFlag(ID.F.UseAirLight)) {
            this.launchType = false;
        } else if (!this.host.getStateFlag(ID.F.UseAirHeavy)) {
            this.launchType = true;
        }

        // launch aircraft when in range, on sight, and delay elapsed
        if (onSight && this.distSq <= this.rangeSq && this.launchDelay <= 0) {
            // light aircraft
            if (this.launchType && this.host.hasAmmoLight() && this.host.hasAirLight()) {
                this.host.attackEntityWithAircraft(this.target);
                this.launchDelay = this.launchDelayMax;
            }

            // heavy aircraft
            if (!this.launchType && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()) {
                this.host.attackEntityWithHeavyAircraft(this.target);
                this.launchDelay = this.launchDelayMax;
            }

            // always toggle type (matching original)
            this.launchType = !this.launchType;
        }

        // reset if stuck too long
        if (this.launchDelay < -80) {
            this.launchDelay = 20;
            this.stop();
        }
    }
}
