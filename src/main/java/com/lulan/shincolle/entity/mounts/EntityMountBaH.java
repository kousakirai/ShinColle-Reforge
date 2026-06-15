package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.ShipRangeAttackGoal;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.entity.BasicEntityMount;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

public class EntityMountBaH extends BasicEntityMount {

    public EntityMountBaH(EntityType<? extends EntityMountBaH> type, Level level) {
        super(type, level);
        this.seatPos = new float[]{1.05F, 2.6F, 0F};
        this.seatPos2 = new float[]{1.2F, 0.7F, -1.3F};
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.9F, 3.1F);
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.goalSelector.addGoal(11, new ShipRangeAttackGoal(this));
    }

    @Override
    public ShipMoveControl getShipMoveControl() {
        return (ShipMoveControl) this.getMoveControl();
    }
}
