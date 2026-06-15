package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Sit goal - locks ship in place when commanded.
 * Ported from EntityAIShipSit (setMutexBits: 7)
 */
public class ShipSitGoal extends Goal {

    private final BasicEntityShip ship;

    public ShipSitGoal(BasicEntityShip ship) {
        this.ship = ship;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.ship.isOrderedToSit();
    }

    @Override
    public void start() {
        this.ship.setEntitySit(true);
        this.ship.setJumping(false);
    }

    @Override
    public void tick() {
        this.ship.getNavigation().stop();
        this.ship.setTarget(null);
        this.ship.setEntityTarget(null);
    }

    @Override
    public void stop() {
        this.ship.setEntitySit(false);
    }
}
