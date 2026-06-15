package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Random wander goal.
 * Ported from EntityAIShipWander (setMutexBits: 7)
 */
public class ShipWanderGoal extends Goal {

    private final BasicEntityShip ship;
    private final int rangeXZ;
    private final int rangeY;
    private final double speed;
    private double targetX, targetY, targetZ;

    public ShipWanderGoal(BasicEntityShip ship, int rangeXZ, int rangeY, double speed) {
        this.ship = ship;
        this.rangeXZ = rangeXZ;
        this.rangeY = rangeY;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.ship.isOrderedToSit())
            return false;
        if (this.ship.isPassenger())
            return false;
        return this.ship.getRandom().nextInt(180) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return !ship.getNavigation().isDone();
    }

    @Override
    public void start() {
        Level level = this.ship.level();
        for (int i = 0; i < 10; i++) {
            double x = this.ship.getX() + (this.ship.getRandom().nextInt(rangeXZ * 2 + 1) - rangeXZ);
            double y = this.ship.getY() + (this.ship.getRandom().nextInt(rangeY * 2 + 1) - rangeY);
            double z = this.ship.getZ() + (this.ship.getRandom().nextInt(rangeXZ * 2 + 1) - rangeXZ);

            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.isLoaded(pos)) {
                this.targetX = x;
                this.targetY = y;
                this.targetZ = z;
                ship.getNavigation().moveTo(targetX, targetY, targetZ, speed);
                return;
            }
        }
    }
}
