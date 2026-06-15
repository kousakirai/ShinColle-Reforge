package com.lulan.shincolle.entity.mounts;

import com.lulan.shincolle.ai.ShipCarrierAttackGoal;
import com.lulan.shincolle.ai.ShipRangeAttackGoal;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.entity.BasicEntityMountLarge;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

public class EntityMountIsH extends BasicEntityMountLarge {

    public EntityMountIsH(EntityType<? extends EntityMountIsH> type, Level level) {
        super(type, level);
        this.seatPos = new float[]{-0.32F, 1F, 0F};
        this.seatPos2 = new float[]{0.61F, 0.28F, 0F};
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.6F, 2.2F);
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.goalSelector.addGoal(11, new ShipCarrierAttackGoal(this));
        this.goalSelector.addGoal(12, new ShipRangeAttackGoal(this));
    }

    @Override
    public ShipMoveControl getShipMoveControl() {
        return (ShipMoveControl) this.getMoveControl();
    }
}
