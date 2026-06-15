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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Range target acquisition goal.
 * Ported from EntityAIShipRangeTarget (setMutexBits: 1)
 * <p>
 * Target priority: AntiAir > AntiSub > PVPFirst > normal target
 */
public class ShipRangeTargetGoal extends Goal {

    protected final IShipAttackBase host;
    protected final Mob entity;
    protected final java.util.function.Predicate<Entity> targetSelector;
    protected BasicEntityShip hostShip;
    protected Entity targetEntity;
    protected int range;

    public ShipRangeTargetGoal(IShipAttackBase host) {
        this.host = host;
        this.entity = (Mob) host;
        // [PORT] 1.10.2 targetTasks mutex -> 1.20.1 TARGET control flag.
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));

        if (host instanceof BasicEntityShip) {
            this.hostShip = (BasicEntityShip) host;
            this.targetSelector = new TargetHelper.Selector(this.entity);
        } else if (host instanceof BasicEntityShipHostile) {
            this.hostShip = null;
            this.targetSelector = new TargetHelper.SelectorForHostile(this.entity);
        } else {
            this.hostShip = null;
            this.targetSelector = new TargetHelper.Selector(this.entity);
        }

        updateRange();
    }

    private static boolean hasNoTargets(List<LivingEntity> targets) {
        return targets == null || targets.isEmpty();
    }

    @Override
    public boolean canUse() {
        ProfilerFiller profiler = DebugProfiler.push(this.entity.level(), "shincolle.ai.range_target.can_use");
        try {
            if (this.host.getIsSitting() || this.host.getStateMinor(ID.M.CraneState) > 0) {
                DebugProfiler.count(profiler, "shincolle.ai.range_target.blocked.sit_or_crane");
                return false;
            }
            updateRange();

            AABB searchBox = this.entity.getBoundingBox().inflate(this.range, this.range * 2D, this.range * 2D);
            List<LivingEntity> targets = null;

            // Priority-based target selection for friendly ships
            if (this.hostShip != null) {
                // 1. Anti-Air: target flying entities first
                if (this.hostShip.getStateFlag(ID.F.AntiAir)) {
                    targets = findTargetsByType(searchBox, IShipFlyable.class);
                    // also search for vanilla flying mobs
                    List<LivingEntity> flyingTargets = findTargetsByType(searchBox, FlyingMob.class);
                    targets = unionLists(targets, flyingTargets);
                }

                // 2. Anti-Sub: target invisible/submarine entities
                if (targets == null || targets.isEmpty()) {
                    if (this.hostShip.getStateFlag(ID.F.AntiSS)) {
                        targets = findTargetsByType(searchBox, IShipInvisible.class);
                    }
                }

                // 3. PVP First: target other player's ships
                if (targets == null || targets.isEmpty()) {
                    if (this.hostShip.getStateFlag(ID.F.PVPFirst)) {
                        targets = findTargetsByType(searchBox, BasicEntityShip.class);
                    }
                }
            }

            // 4. Normal: any valid target
            if (targets == null || targets.isEmpty()) {
                targets = this.entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        this::isValidTarget);
            }
            System.out.println("Targets found = " + targets.size());

            if (!targets.isEmpty()) {
                // sort by distance
                targets.sort(Comparator.comparingDouble(this.entity::distanceToSqr));

                // pick nearest, or random from top 3
                if (targets.size() > 2) {
                    this.targetEntity = targets.get(this.entity.getRandom().nextInt(3));
                } else {
                    this.targetEntity = targets.get(0);
                }
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
    private <T> List<LivingEntity> findTargetsByType(AABB searchBox, Class<T> targetType) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity e : this.entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                this::isValidTarget)) {
            if (targetType.isInstance(e)) {
                result.add(e);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Union two lists, returning a combined non-null list.
     */
    private List<LivingEntity> unionLists(List<LivingEntity> a, List<LivingEntity> b) {
        if (a == null || a.isEmpty())
            return b;
        if (b == null || b.isEmpty())
            return a;
        List<LivingEntity> result = new ArrayList<>(a);
        for (LivingEntity e : b) {
            if (!result.contains(e)) {
                result.add(e);
            }
        }
        return result;
    }

    @Override
    public void start() {
        if (this.host != null) {
            this.entity.setTarget((LivingEntity) this.targetEntity);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean canContinueToUse() {
        Entity target = this.entity.getTarget();

        if (target == null || !target.isAlive()) {
            return false;
        }

        double d0 = this.range * this.range;
        if (this.entity.distanceToSqr(target) > d0) {
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
