package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.ShipRangeAttackGoal;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.entity.BasicEntityMount;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

public class EntityMountSuH extends BasicEntityMount {

    public EntityMountSuH(EntityType<? extends EntityMountSuH> type, Level level) {
        super(type, level);
        this.seatPos = new float[]{-0.8F, 0.6F, 0F};
        this.seatPos2 = new float[]{0.55F, 1.2F, 0F};
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.8F, 1.6F);
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
