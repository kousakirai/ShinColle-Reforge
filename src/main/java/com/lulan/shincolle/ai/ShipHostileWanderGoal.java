package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShipHostile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Random wander goal for hostile ship entities.
 * <p>
 * [PORT] 1.10.2 -> 1.20.1: restores hostile idle movement behavior using
 * ship-specific navigation rather than PathfinderMob goals.
 */
public class ShipHostileWanderGoal extends Goal {

    private final BasicEntityShipHostile ship;
    private final int rangeXZ;
    private final int rangeY;
    private final double speed;
    private double targetX;
    private double targetY;
    private double targetZ;

    public ShipHostileWanderGoal(BasicEntityShipHostile ship, int rangeXZ, int rangeY, double speed) {
        this.ship = ship;
        this.rangeXZ = rangeXZ;
        this.rangeY = rangeY;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.ship.isPassenger()) {
            return false;
        }
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
                ship.getNavigation().moveTo(this.targetX, this.targetY, this.targetZ, this.speed);
                return;
            }
        }
    }
}
