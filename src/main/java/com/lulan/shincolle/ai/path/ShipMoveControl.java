package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.EntityHelper;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Custom move helper for ship/airplane entities.
 * Provides Y-axis movement for water/air navigation
 * that vanilla MoveControl does not support.
 * <p>
 * Ported from 1.10.2 ShipMoveHelper (standalone, not extending vanilla).
 */
public class ShipMoveControl extends MoveControl {

    private final Mob entity;
    private final IShipNavigator entityN;
    private final float rotateLimit;
    private final float speedModifier;

    public ShipMoveControl(Mob entity, float rotLimit, float speedModifier) {
        super(entity);
        this.entity = entity;
        this.entityN = (IShipNavigator) entity;
        this.rotateLimit = rotLimit;
        this.speedModifier = speedModifier;
    }

    /**
     * Get formation-adjusted movement speed
     */
    private static float getFormationMOV(BasicEntityShip ship) {
        int formatType = ship.getStateMinor(ID.M.FormatType);
        int formatSlot = ship.getStateMinor(ID.M.FormatPos);
        float[] buffs = com.lulan.shincolle.utility.FormationHelper.getFormationBuffValue(formatType, formatSlot);
        // formation buff index 4 is MOV multiplier
        float movBuff = buffs.length > 4 ? buffs[4] : 1.0F;
        return (float) ship.getAttributeValue(Attributes.MOVEMENT_SPEED) * movBuff;
    }

    public boolean isUpdating() {
        return this.operation == Operation.MOVE_TO;
    }

    /**
     * Movement update tick - handles Y-axis movement for water/air entities
     */
    public void tick() {
        mob.setZza(0.0F);

        if (this.operation == Operation.MOVE_TO) {
            this.operation = Operation.WAIT;

            double x1 = this.wantedX - this.entity.getX();
            double y1 = this.wantedY - this.entity.getY();
            double z1 = this.wantedZ - this.entity.getZ();
            double moveSq = x1 * x1 + y1 * y1 + z1 * z1;

            if (moveSq > 0.001D) {
                float f = (float) (Math.atan2(z1, x1) * Values.N.DIV_180_PI) - 90F;
                float moveSpeed = (float) this.entity.getAttributeValue(Attributes.MOVEMENT_SPEED);

                // check formation speed bonus
                if (this.entity instanceof BasicEntityShip ship) {
                    if (ship.getStateMinor(ID.M.FormatType) > 0) {
                        moveSpeed = getFormationMOV(ship);
                    }
                } else if (this.entity instanceof BasicEntityMount mount) {
                    BasicEntityShip host = (BasicEntityShip) mount.getHostEntity();
                    if (host != null && host.getStateMinor(ID.M.FormatType) > 0) {
                        moveSpeed = getFormationMOV(host);
                    }
                }

                moveSpeed *= this.speedModifier;

                // limit turn rate per tick
                this.entity.setYRot(this.limitAngle(this.entity.getYRot(), f, this.rotateLimit));
                // [PORT] 1.10.2 -> 1.20.1: restore legacy forward input while MOVE_TO.
                // In 1.20.1 setSpeed() does not implicitly set forward movement.
                mob.setZza(1.0F);

                // Y-axis movement (not handled by vanilla which only does horizontal)
                if (entityN.canFly()) {
                    // flying entity
                    if (y1 > 0.5D) {
                        this.entity.setDeltaMovement(
                                this.entity.getDeltaMovement().add(0, moveSpeed * 0.12D, 0));
                        moveSpeed *= 0.8F;
                    } else if (y1 < -0.5D) {
                        this.entity.setDeltaMovement(
                                this.entity.getDeltaMovement().add(0, -moveSpeed * 0.16D, 0));
                        moveSpeed *= 0.92F;
                    }
                }
                // non-flying in liquid (legacy uses footing-based liquid check)
                else if (EntityHelper.checkEntityIsInLiquid(this.entity)) {
                    if (y1 > 1D) {
                        this.entity.setDeltaMovement(
                                this.entity.getDeltaMovement().add(0, moveSpeed * 0.2D, 0));
                        moveSpeed *= 0.5F;
                    } else if (y1 > 0.35D) {
                        this.entity.setDeltaMovement(
                                this.entity.getDeltaMovement().add(0, moveSpeed * 0.1D, 0));
                        moveSpeed *= 0.5F;
                    } else if (y1 < -1D) {
                        this.entity.setDeltaMovement(
                                this.entity.getDeltaMovement().add(0, -moveSpeed * 0.25D, 0));
                        moveSpeed *= 0.82F;
                    }
                }
                // on land, try jumping if target is higher
                else if (y1 > this.entity.maxUpStep() && x1 * x1 + z1 * z1 < 1D) {
                    this.entity.getJumpControl().jump();
                }

                this.entity.setSpeed(moveSpeed);
            } else {
                mob.setZza(0.0F);
            }
        } else {
            mob.setZza(0.0F);
        }
    }

    /**
     * Smooth angle limiting
     */
    private float limitAngle(float yaw, float degree, float limit) {
        float f = Mth.wrapDegrees(degree - yaw);

        if (f > limit)
            f = limit;
        if (f < -limit)
            f = -limit;

        float f1 = yaw + f;

        if (f1 < 0.0F)
            f1 += 360.0F;
        else if (f1 > 360.0F)
            f1 -= 360.0F;

        return f1;
    }

    public enum Action {
        WAIT,
        MOVE_TO
    }
}
