package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.entity.IShipCannonAttack;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.FormationHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Ship guarding AI - guard a position or entity.
 * Ported from EntityAIShipGuarding (setMutexBits: 7)
 * <p>
 * Guard types:
 * 0: guard a block, force moving only
 * 1: guard a block, attack while moving
 * 2: guard an entity
 * <p>
 * Attack-while-moving:
 * When GuardType > 0 and not passive AI, ship can attack
 * targets within range while moving toward guard position.
 * Supports light/heavy cannon and aircraft attacks.
 */
public class ShipGuardingGoal extends Goal {

    private final IShipGuardian host;
    private final Mob hostEntity;
    private final PathNavigation shipNavigator;
    private final TargetHelper.Sorter targetSorter;
    private final TargetHelper.Selector targetSelector;
    private final double[] guardPosOld;              // last known guarded entity position
    private final int[] delayTime;                   // attack delay: 0=light 1=heavy 2=aircraft
    private final int[] maxDelayTime;                // max delay per type
    private Entity guarded;
    private int checkTP_T, checkTP_D;         // teleport cooldown counters
    private int findCooldown;                  // path navigation cooldown
    private double maxDistSq, minDistSq;
    private double distSq;                     // distance to guard target squared
    private double[] pos;                      // guard position: x, y, z
    // attack-while-moving parameters (for BasicEntityShip only)
    private IShipCannonAttack ship;            // host can use cannon
    private IShipAircraftAttack ship2;         // host can use aircraft
    private LivingEntity attackTarget;         // current attack target
    private int onSightTime;                   // target on-sight accumulator
    private int aimTime;                       // ticks before can fire
    private float range, rangeSq;              // attack range
    private boolean launchType;                // airplane type toggle, true = light
    private boolean isMoving;                  // currently moving toward guard pos

    public ShipGuardingGoal(IShipGuardian host) {
        this.host = host;
        this.hostEntity = (Mob) host;
        this.shipNavigator = hostEntity.getNavigation();
        this.targetSorter = new TargetHelper.Sorter(hostEntity);
        this.targetSelector = new TargetHelper.Selector(hostEntity);
        this.distSq = 1D;
        this.isMoving = false;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));

        // check if host supports cannon/aircraft attack
        if (host instanceof IShipCannonAttack) {
            this.ship = (IShipCannonAttack) host;

            if (host instanceof IShipAircraftAttack) {
                this.ship2 = (IShipAircraftAttack) host;
            }
        }

        // init values
        this.pos = new double[]{-1D, -1D, -1D};
        this.guardPosOld = new double[]{-1D, -100D, -1D};
        this.delayTime = new int[]{20, 20, 20};
        this.maxDelayTime = new int[]{20, 40, 40};
        this.onSightTime = 0;
        this.aimTime = 20;
        this.range = 1;
        this.rangeSq = 1;
    }

    @Override
    public boolean canUse() {
        if (host == null)
            return false;

        // not sitting, not riding, not in follow mode, not being craned, has grudge
        if (host.getIsRiding() || host.getIsSitting() ||
                host.getStateFlag(ID.F.CanFollow) ||
                host.getStateMinor(ID.M.CraneState) >= 1 ||
                host.getStateMinor(ID.M.NumGrudge) <= 0) {
            return false;
        }

        // check if guard target exists
        return checkGuardTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (host == null)
            return false;

        // not sitting, not riding, not in follow mode, not being craned, has grudge
        if (host.getIsRiding() || host.getIsSitting() ||
                host.getStateFlag(ID.F.CanFollow) ||
                host.getStateMinor(ID.M.CraneState) >= 1 ||
                host.getStateMinor(ID.M.NumGrudge) <= 0) {
            this.stop();
            return false;
        }

        // still outside min range, keep going
        if (this.distSq > this.minDistSq) {
            return true;
        }

        // other cases: still has path or can re-evaluate
        return (shipNavigator != null && !shipNavigator.isDone()) || canUse();
    }

    @Override
    public void start() {
        this.findCooldown = 10;
        this.checkTP_T = 0;
        this.checkTP_D = 0;
    }

    @Override
    public void stop() {
        this.guarded = null;
        this.isMoving = false;
        this.findCooldown = 10;
        if (this.shipNavigator != null) {
            this.shipNavigator.stop();
        }
    }

    @Override
    public void tick() {
        // ===== Attack while moving =====
        // Active when: is ship entity, not passive AI, guard type > 0
        if (isMoving && ship != null &&
                !ship.getStateFlag(ID.F.PassiveAI) &&
                ship.getStateMinor(ID.M.GuardType) > 0) {

            // update attack parameters every 64 ticks
            if (hostEntity.tickCount % 64 == 0) {
                this.updateAttackParms();
            }

            // decrement attack delays
            this.delayTime[0]--;
            this.delayTime[1]--;
            this.delayTime[2]--;

            // find target every 32 ticks
            if (hostEntity.tickCount % 32 == 0) {
                this.findTarget();

                // clear dead target
                if (this.attackTarget != null && !this.attackTarget.isAlive()) {
                    this.attackTarget = null;
                }
            }

            // attack target if on sight
            if (this.attackTarget != null && this.hostEntity.getSensing().hasLineOfSight(this.attackTarget)) {
                this.onSightTime++;

                // calc distance to attack target
                double tarDistX = this.attackTarget.getX() - this.hostEntity.getX();
                double tarDistY = this.attackTarget.getY() - this.hostEntity.getY();
                double tarDistZ = this.attackTarget.getZ() - this.hostEntity.getZ();
                double tarDistSq = tarDistX * tarDistX + tarDistY * tarDistY + tarDistZ * tarDistZ;

                // attack within range after aim time
                if (tarDistSq <= this.rangeSq && this.onSightTime >= this.aimTime) {
                    this.attackTarget();
                }
            } else {
                // no target or not on sight
                this.onSightTime = 0;
            }
        }

        // ===== Update guarding movement =====
        if (host == null)
            return;

        this.findCooldown--;

        // stuck detection: if barely moving, increment teleport timer
        double motX = this.hostEntity.getDeltaMovement().x;
        double motZ = this.hostEntity.getDeltaMovement().z;
        if (motX * motX < 0.0003D && motZ * motZ < 0.0003D) {
            this.checkTP_T++;
        }

        // update guard position every 8 ticks
        if (hostEntity.tickCount % 8 == 0) {
            if (!checkGuardTarget())
                return;
        }

        // reached min distance, stop moving
        if (this.distSq <= this.minDistSq) {
            this.isMoving = false;
            if (this.shipNavigator != null) {
                this.shipNavigator.stop();
            }
        }

        // try pathfinding every cooldown cycle
        if (this.findCooldown <= 0) {
            this.findCooldown = 32;
            if (this.shipNavigator != null) {
                this.isMoving = this.shipNavigator.moveTo(pos[0], pos[1], pos[2], 1D);
            }
        }

        // look toward guard position
        this.hostEntity.getLookControl().setLookAt(pos[0], pos[1], pos[2], 30F,
                (float) this.hostEntity.getMaxHeadXRot());

        // ===== Teleport check =====
        if (!ConfigHandler.canTeleport())
            return;

        // distance-based teleport
        if (this.distSq > ConfigHandler.shipTeleport[1]) {
            this.checkTP_D++;

            if (this.checkTP_D > ConfigHandler.shipTeleport[0]) {
                this.checkTP_D = 0;
                LogHelper.debug("DEBUG: guard AI: distSQ > " + ConfigHandler.shipTeleport[1] +
                        " , teleport to target.");
                this.hostEntity.teleportTo(pos[0], pos[1] + 0.75D, pos[2]);
                return;
            }
        }

        // stuck-time-based teleport
        if (this.checkTP_T > ConfigHandler.shipTeleport[0]) {
            this.checkTP_T = 0;
            LogHelper.debug("DEBUG: guard AI: stuck time exceeded, teleport to target.");
            this.hostEntity.teleportTo(pos[0], pos[1] + 0.75D, pos[2]);
        }
    }

    /**
     * Update attack parameters: range, delay, aim time.
     * Called every 64 ticks when attack-while-moving is active.
     */
    private void updateAttackParms() {
        if (this.ship == null)
            return;

        // attack range
        this.range = (int) this.ship.getAttrs().getAttackRange();
        if (this.range < 1)
            this.range = 1;
        this.rangeSq = this.range * this.range;

        // attack delay per type (using config base speed)
        float atkSpd = Math.max(this.ship.getAttrs().getAttackSpeed(), 0.01F);
        this.maxDelayTime[0] = (int) (ConfigHandler.baseAttackSpeed[1] / atkSpd)
                + ConfigHandler.fixedAttackDelay[1];
        this.maxDelayTime[1] = (int) (ConfigHandler.baseAttackSpeed[2] / atkSpd)
                + ConfigHandler.fixedAttackDelay[2];
        this.maxDelayTime[2] = (int) (ConfigHandler.baseAttackSpeed[3] / atkSpd)
                + ConfigHandler.fixedAttackDelay[3];

        // aim time scales inversely with level
        this.aimTime = (int) (20F * (150 - this.host.getLevel()) / 150F) + 10;
    }

    /**
     * Find attack target within range using TargetHelper selectors.
     * Called every 32 ticks when attack-while-moving is active.
     */
    private void findTarget() {
        AABB searchBox = this.hostEntity.getBoundingBox().inflate(
                this.range * 0.9D, this.range * 0.6D, this.range * 0.9D);

        List<LivingEntity> list = this.hostEntity.level().getEntitiesOfClass(
                LivingEntity.class, searchBox, this.targetSelector);

        // sort by distance (nearest first)
        list.sort(this.targetSorter);

        // pick target: random from top 3 if available, else nearest
        if (list.size() > 2) {
            this.attackTarget = list.get(this.hostEntity.level().random.nextInt(3));
        } else if (!list.isEmpty()) {
            this.attackTarget = list.get(0);
        }
    }

    /**
     * Execute attack on current target.
     * Handles light cannon, heavy cannon, and aircraft attacks.
     */
    private void attackTarget() {
        // light cannon attack
        if (this.ship.getStateFlag(ID.F.AtkType_Light) && this.delayTime[0] <= 0 &&
                this.ship.useAmmoLight() && this.ship.hasAmmoLight()) {
            this.ship.attackEntityWithAmmo(this.attackTarget);
            this.delayTime[0] = this.maxDelayTime[0];
        }

        // heavy cannon attack
        if (this.ship.getStateFlag(ID.F.AtkType_Heavy) && this.delayTime[1] <= 0 &&
                this.ship.useAmmoHeavy() && this.ship.hasAmmoHeavy()) {
            this.ship.attackEntityWithHeavyAmmo(this.attackTarget);
            this.delayTime[1] = this.maxDelayTime[1];
        }

        // aircraft attack
        if (this.ship2 != null && this.delayTime[2] <= 0 &&
                (this.ship2.getStateFlag(ID.F.UseAirLight) || this.ship2.getStateFlag(ID.F.UseAirHeavy))) {

            // if only one ammo type enabled, lock launch type to that one
            if (!this.ship2.getStateFlag(ID.F.UseAirLight)) {
                this.launchType = false;
            }
            if (!this.ship2.getStateFlag(ID.F.UseAirHeavy)) {
                this.launchType = true;
            }

            // light aircraft
            if (this.launchType && this.ship2.hasAmmoLight() && this.ship2.hasAirLight()) {
                this.ship2.attackEntityWithAircraft(this.attackTarget);
                this.delayTime[2] = this.maxDelayTime[2];
            }

            // heavy aircraft
            if (!this.launchType && this.ship2.hasAmmoHeavy() && this.ship2.hasAirHeavy()) {
                this.ship2.attackEntityWithHeavyAircraft(this.attackTarget);
                this.delayTime[2] = this.maxDelayTime[2];
            }

            // alternate launch type for next attack
            this.launchType = !this.launchType;
        }
    }

    /**
     * Check and update guard target position.
     * Handles guarded entity (with formation support) and guarded block position.
     *
     * @return true if ship needs to move toward guard position
     */
    private boolean checkGuardTarget() {
        this.guarded = host.getGuardedEntity();

        if (this.guarded != null) {
            // guarded entity is dead or invalid
            if (!this.guarded.isAlive()) {
                host.setGuardedPos(-1, -1, -1, 0, 0);
                host.setGuardedEntity(null);
                host.setStateFlag(ID.F.CanFollow, true);
                this.stop();
                return false;
            }

            // if guard with formation
            if (host.getStateMinor(ID.M.FormatType) > 0) {
                // if guarded entity moved significantly (distSq > 6), recalculate position
                double dx = guardPosOld[0] - guarded.getX();
                double dy = guardPosOld[1] - guarded.getY();
                double dz = guardPosOld[2] - guarded.getZ();
                double dsq = dx * dx + dy * dy + dz * dz;

                if (dsq > 6) {
                    // get formation-adjusted guard position
                    pos = FormationHelper.getFormationGuardingPos(host, guarded,
                            guardPosOld[0], guardPosOld[2]);

                    // backup position
                    guardPosOld[0] = guarded.getX();
                    guardPosOld[1] = guarded.getY();
                    guardPosOld[2] = guarded.getZ();
                }
            }
            // no formation - just follow guarded entity directly
            else {
                pos[0] = guarded.getX();
                pos[1] = guarded.getY();
                pos[2] = guarded.getZ();
            }
        }
        // guard a block position
        else {
            pos[0] = host.getStateMinor(ID.M.GuardX) + 0.5D;
            pos[1] = host.getStateMinor(ID.M.GuardY) + 0.5D;
            pos[2] = host.getStateMinor(ID.M.GuardZ) + 0.5D;
        }

        // if guard Y <= 0, cancel guard mode
        if (pos[1] <= 0) {
            host.setGuardedPos(-1, -1, -1, 0, 0);
            host.setGuardedEntity(null);
            host.setStateFlag(ID.F.CanFollow, true);
            this.stop();
            return false;
        }

        // calculate follow distances
        if (this.ship != null && this.ship.getStateMinor(ID.M.FormatType) > 0) {
            // formation mode: tighter follow distances
            if (this.ship.getStateMinor(ID.M.GuardType) == 2) {
                // guard entity
                this.minDistSq = 5D;
                this.maxDistSq = 9D;
            } else {
                // guard block
                this.minDistSq = 4D;
                this.maxDistSq = 7D;
            }

            if (host.getStateFlag(ID.F.PickItem))
                this.maxDistSq = 64D;
        }
        // non-formation mode: use FollowMin/FollowMax
        else {
            float fMin = host.getStateMinor(ID.M.FollowMin) + hostEntity.getBbWidth() * 0.75F;
            float fMax = host.getStateMinor(ID.M.FollowMax) + hostEntity.getBbWidth() * 0.75F;

            if (host.getStateFlag(ID.F.PickItem))
                fMax += 5F;

            this.minDistSq = fMin * fMin;
            this.maxDistSq = fMax * fMax;
        }

        // calculate distance to guard position
        double dx = pos[0] - this.hostEntity.getX();
        double dy = pos[1] - this.hostEntity.getY();
        double dz = pos[2] - this.hostEntity.getZ();
        this.distSq = dx * dx + dy * dy + dz * dz;

        // needs to move if outside max range
        return this.distSq > this.maxDistSq;
    }
}
