package com.lulan.shincolle.ai.path;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;

public class ShipNavigation extends FlyingPathNavigation {
    public ShipNavigation(Mob mob, Level level) {
        super(mob, level);
        this.setMaxVisitedNodesMultiplier(4.0F);
    }
}