package com.lulan.shincolle.ai.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;

/**
 * Brain integration point for friendly ships.
 *
 * <p>The legacy GoalSelector continues to own movement and weapon execution.
 * This class only publishes the authoritative ship state as Brain memory and
 * selects an activity.  Keeping those responsibilities separate allows Goals
 * to be migrated one at a time without two systems issuing navigation commands
 * in the same tick.</p>
 */
public final class ShipBrain {
    private static final List<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE
    );

    private static final List<SensorType<? extends Sensor<? super BasicEntityShip>>> SENSOR_TYPES = ImmutableList.of();

    private ShipBrain() {
    }

    public static List<MemoryModuleType<?>> getMemoryTypes() {
        return MEMORY_TYPES;
    }

    /**
     * Ship perception still uses TargetHelper and the existing target Goals.
     * Sensors are intentionally added only when a behavior is migrated, so no
     * second broad entity scan is introduced during the transition.
     */
    public static List<SensorType<? extends Sensor<? super BasicEntityShip>>> getSensorTypes() {
        return SENSOR_TYPES;
    }

    public static void registerGoals(Brain<BasicEntityShip> brain) {
        brain.addActivity(Activity.CORE, ImmutableList.of());
        brain.addActivity(Activity.IDLE, ImmutableList.of());
        brain.addActivity(Activity.FIGHT, ImmutableList.of());
        brain.addActivity(Activity.PANIC, ImmutableList.of());
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    /**
     * Mirrors state that is already resolved by the stable Goal system.  The
     * selected activity is an observable contract for subsequent behaviors;
     * it does not yet replace the legacy goals.
     */
    public static void syncState(Brain<BasicEntityShip> brain, BasicEntityShip ship) {
        LivingEntity target = ship.getTarget();
        if (target != null && target.isAlive()) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } else {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }

        LivingEntity attacker = ship.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, attacker);
        } else {
            brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        }

        float fleeHealthRatio = ship.getStateMinor(ID.M.FleeHP) * 0.01F;
        boolean shouldFlee = fleeHealthRatio > 0.0F
                && ship.getHealth() / ship.getMaxHealth() <= fleeHealthRatio
                && ship.getOwner() != null;
        if (shouldFlee) {
            brain.setActiveActivityIfPossible(Activity.PANIC);
        } else if (target != null && target.isAlive()) {
            brain.setActiveActivityIfPossible(Activity.FIGHT);
        } else {
            brain.setActiveActivityIfPossible(Activity.IDLE);
        }
    }
}
