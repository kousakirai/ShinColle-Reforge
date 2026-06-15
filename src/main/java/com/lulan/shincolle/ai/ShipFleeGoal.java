package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.server.ServerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Flee goal - activates when HP below threshold.
 * Ported from EntityAIShipFlee (setMutexBits: 7)
 */
public class ShipFleeGoal extends Goal {

    private final BasicEntityShip ship;
    private LivingEntity owner;
    private int pathfindCooldown;

    public ShipFleeGoal(BasicEntityShip ship) {
        this.ship = ship;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = resolveOwner();
        if (owner == null || !owner.isAlive())
            return false;

        float fleeHP = this.ship.getStateMinor(ID.M.FleeHP) * 0.01F;
        float hpRatio = this.ship.getHealth() / this.ship.getMaxHealth();

        return hpRatio <= fleeHP;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.owner = resolveOwner();
        this.pathfindCooldown = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        ship.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--this.pathfindCooldown <= 0) {
            this.pathfindCooldown = 20;

            if (this.owner != null && this.owner.isAlive()) {
                boolean canMove;
                if (this.ship.isPassenger() && this.ship.getVehicle() instanceof BasicEntityMount mount) {
                    canMove = mount.getNavigation().moveTo(this.owner, 1.2D);
                } else {
                    canMove = ship.getNavigation().moveTo(this.owner, 1.2D);
                }

                // move failed or stuck, teleport entity
                if (!canMove || this.ship.distanceToSqr(this.owner) > 1024D) {
                    this.ship.teleportTo(this.owner.getX(), this.owner.getY(), this.owner.getZ());
                }
            }
        }
    }

    private LivingEntity resolveOwner() {
        int uid = this.ship.getPlayerUID();
        if (uid > 0 && !this.ship.level().isClientSide()) {
            ServerPlayer serverPlayer = ServerDataManager.getPlayerByUID(uid);
            if (serverPlayer != null) {
                return serverPlayer;
            }
        }

        return this.ship.getOwner();
    }
}
