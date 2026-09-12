package com.lulan.shincolle.entity.carrier;

import com.lulan.shincolle.ai.ShipCarrierAttackGoal;
import com.lulan.shincolle.entity.BasicEntityShipHostileCV;
import com.lulan.shincolle.reference.ID;

import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

/**
 * Carrier Kaga mob (hostile) entity.
 */
public class EntityCarrierKagaMob extends BasicEntityShipHostileCV {

    private float entityWidth = 0.6F;
    private float entityHeight = 1.875F;

    public EntityCarrierKagaMob(EntityType<? extends EntityCarrierKagaMob> type, Level level) {
        super(type, level);

        // init values
        this.setStateMinor(ID.M.ShipClass, ID.ShipClass.CVKaga);
        this.setStateMinor(ID.M.ShipType, ID.ShipType.STANDARD_CARRIER);
        this.setStateMinor(ID.M.DamageType, ID.ShipDmgType.CARRIER);
        this.setStateMinor(ID.M.NumState, 8);

        // unlimited ammo/grudge for hostile entity
        this.StateMinor[ID.M.NumAmmoLight] = 999;
        this.StateMinor[ID.M.NumAmmoHeavy] = 999;
        this.StateMinor[ID.M.NumGrudge] = 999;
        this.setAmmoConsumption(1);

        // boss bar color
        this.bossBarColor = BossEvent.BossBarColor.BLUE;

        // model display
        this.setStateEmotion(ID.S.State, this.random.nextInt(128), false);

        this.postInit();
        this.launchHeight = this.getBbHeight() * 0.65D;
    }

    @Override
    public void setSizeWithScaleLevel() {
        switch (this.getScaleLevel()) {
            case 3:
                this.entityWidth = 2.4F;
                this.entityHeight = 7.5F;
                break;
            case 2:
                this.entityWidth = 1.8F;
                this.entityHeight = 5.625F;
                break;
            case 1:
                this.entityWidth = 1.2F;
                this.entityHeight = 3.75F;
                break;
            default:
                this.entityWidth = 0.6F;
                this.entityHeight = 1.875F;
                break;
        }
        this.refreshDimensions();
        this.launchHeight = this.getBbHeight() * 0.65D;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        // Legacy hostile Kaga launches aircraft at the carrier goal priority.
        this.goalSelector.addGoal(11, new ShipCarrierAttackGoal(this));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(this.entityWidth, this.entityHeight);
    }

    @Override
    public int getEquipType() {
        return 3;
    }
}
