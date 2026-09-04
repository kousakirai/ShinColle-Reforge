package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.DebugProfiler;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Range target acquisition goal.
 * Ported from EntityAIShipRangeTarget (setMutexBits: 1)
 * <p>
 * Target priority: AntiAir > AntiSub > PVPFirst > normal target
 */
public class ShipRangeTargetGoal extends TargetGoal {

    /**
     * Target acquisition is an expensive world query.  Five ticks keeps the
     * response snappy while avoiding a full scan for every idle ship on every
     * server tick.  This follows the same cadence used by LittleMaid's custom
     * nearest-target goal.
     */
    private static final int TARGET_SCAN_INTERVAL = 5;

    protected final IShipAttackBase host;
    protected final java.util.function.Predicate<Entity> targetSelector;
    protected BasicEntityShip hostShip;
    protected Entity targetEntity;
    protected int range;
    private int nextTargetScanTick;
    private TargetingConditions targetingConditions;

    public ShipRangeTargetGoal(IShipAttackBase host) {
        super((Mob)host, false);
        this.host = host;

        if (host instanceof BasicEntityShip) {
            this.hostShip = (BasicEntityShip) host;
            this.targetSelector = new TargetHelper.Selector(this.mob);
        } else if (host instanceof BasicEntityShipHostile) {
            this.hostShip = null;
            this.targetSelector = new TargetHelper.SelectorForHostile(this.mob);
        } else {
            this.hostShip = null;
            this.targetSelector = new TargetHelper.Selector(this.mob);
        }

        updateRange();
        this.nextTargetScanTick = 0;
        this.targetingConditions = TargetingConditions.forCombat().range(this.range);
    }

    @Override
    public boolean canUse() {
        ProfilerFiller profiler = DebugProfiler.push(this.mob.level(), "shincolle.ai.range_target.can_use");
        try {
            if (this.host.getIsSitting() || this.host.getStateMinor(ID.M.CraneState) > 0) {
                DebugProfiler.count(profiler, "shincolle.ai.range_target.blocked.sit_or_crane");
                return false;
            }

            if (this.mob.tickCount < this.nextTargetScanTick) {
                DebugProfiler.count(profiler, "shincolle.ai.range_target.can_use.cooldown");
                return false;
            }
            this.nextTargetScanTick = this.mob.tickCount + TARGET_SCAN_INTERVAL;
            updateRange();
            this.targetingConditions = TargetingConditions.forCombat().range(this.range);

            AABB searchBox = this.mob.getBoundingBox().inflate(this.range, this.range * 2D, this.range * 2D);
            LivingEntity target = null;

            // Priority-based target selection for friendly ships
            if (this.hostShip != null) {
                // 1. Anti-Air: target flying entities first
                if (this.hostShip.getStateFlag(ID.F.AntiAir)) {
                    target = findNearestTargetByType(searchBox, IShipFlyable.class);
                    // also search for vanilla flying mobs
                    LivingEntity flyingTarget = findNearestTargetByType(searchBox, FlyingMob.class);
                    target = nearestOf(target, flyingTarget);
                }

                // 2. Anti-Sub: target invisible/submarine entities
                if (target == null) {
                    if (this.hostShip.getStateFlag(ID.F.AntiSS)) {
                        target = findNearestTargetByType(searchBox, IShipInvisible.class);
                    }
                }

                // 3. PVP First: target other player's ships
                if (target == null) {
                    if (this.hostShip.getStateFlag(ID.F.PVPFirst)) {
                        target = findNearestTargetByType(searchBox, BasicEntityShip.class);
                    }
                }
            }

            // 4. Normal: any valid target
            if (target == null) {
                target = findNearestTarget(searchBox);
            }

            if (target != null) {
                this.targetEntity = target;
                DebugProfiler.count(profiler, "shincolle.ai.range_target.can_use.success");
                return true;
            }

            DebugProfiler.count(profiler, "shincolle.ai.range_target.can_use.no_target");
            return false;
        } finally {
            DebugProfiler.pop(profiler);
        }
    }

    /**
     * Find targets that implement a specific interface/class within the search box.
     * Also applies the target selector predicate.
     */
    private LivingEntity findNearestTarget(AABB searchBox) {
        List<LivingEntity> candidates = this.mob.level().getEntitiesOfClass(
                LivingEntity.class, searchBox, this::isValidTarget);
        return this.mob.level().getNearestEntity(candidates, this.targetingConditions, this.mob,
                this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
    }

    private <T> LivingEntity findNearestTargetByType(AABB searchBox, Class<T> targetType) {
        List<LivingEntity> candidates = this.mob.level().getEntitiesOfClass(
                LivingEntity.class, searchBox, entity -> targetType.isInstance(entity) && isValidTarget(entity));
        return this.mob.level().getNearestEntity(candidates, this.targetingConditions, this.mob,
                this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
    }

    private LivingEntity nearestOf(LivingEntity first, LivingEntity second) {
        if (first == null) {
            return second;
        }
        if (second == null || this.mob.distanceToSqr(first) <= this.mob.distanceToSqr(second)) {
            return first;
        }
        return second;
    }

    @Override
    public void start() {
        if (this.host != null) {
            this.mob.setTarget((LivingEntity) this.targetEntity);
        }
    }

    @Override
    public void stop() {
        this.mob.setTarget(null);
        this.targetEntity = null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        double d0 = this.range * this.range;
        if (this.mob.distanceToSqr(target) > d0) {
            return false;
        }

        // don't attack invincible players
        return !(target instanceof Player player) || !player.getAbilities().invulnerable;
    }

    /**
     * Check if an entity is a valid target.
     * Delegates to TargetHelper.Selector or SelectorForHostile.
     */
    protected boolean isValidTarget(LivingEntity candidate) {
        return this.targetSelector.test(candidate);
    }

    private void updateRange() {
        this.range = Math.round(this.host.getAttrs().getAttackRange());
        if (this.range < 2) {
            this.range = Math.max(2, this.host.getStateMinor(ID.M.FollowMax) + 2);
        }
    }
}
