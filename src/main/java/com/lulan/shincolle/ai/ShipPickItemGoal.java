package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Item pickup goal.
 * Ported from EntityAIShipPickItem (setMutexBits: 7)
 */
public class ShipPickItemGoal extends Goal {

    private final BasicEntityShip ship;
    private final float pickRangeBase;
    private Entity entItem;
    private int pickDelay;
    private int pickDelayMax;
    private float pickRange;

    public ShipPickItemGoal(BasicEntityShip ship, float pickRangeBase) {
        this.ship = ship;
        this.pickRangeBase = pickRangeBase;
        this.pickDelay = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));

        updateShipParms();
    }

    @Override
    public boolean canUse() {
        AABB box = this.ship.getBoundingBox().inflate(this.pickRange, this.pickRange * 0.5F + 1.0F, this.pickRange * 1.2F);
        List<ItemEntity> items = this.ship.level().getEntitiesOfClass(ItemEntity.class, box);

        if (items.isEmpty()) {
            return false;
        }

        // sitting, riding, disabled, no fuel, crane state active: skip
        // check Flag PickItem
        if (this.ship.isPassenger() || this.ship.isOrderedToSit() ||
                !this.ship.getStateFlag(ID.F.PickItem) ||
                this.ship.getStateMinor(ID.M.CraneState) > 0 ||
                this.ship.getStateFlag(ID.F.NoFuel)) {
            return false;
        }

        // check inventory space
        return this.ship.getCapaShipInventory().getFirstSlotForItem() >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        this.pickDelay--;

        // check every 16 ticks
        if (this.ship.tickCount % 15 == 0) {
            updateShipParms();

            // find nearby items
            this.entItem = getNearbyItemEntity();

            if (this.entItem != null && this.entItem.isAlive()) {
                ship.getNavigation().moveTo(this.entItem, 1.0D);
            }
        }

        // pick up nearby item
        if (this.pickDelay <= 0 && this.entItem != null) {
            this.pickDelay = this.pickDelayMax;

            // within 3 blocks
            if (this.ship.distanceToSqr(this.entItem) < 9.0D) {
                if (this.entItem instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    int count = stack.getCount();

                    if (!itemEntity.hasPickUpDelay() &&
                            this.ship.getCapaShipInventory().addItemStackToInventory(stack)) {

                        // play pickup sound
                        this.ship.level().playSound(null,
                                this.ship.getX(), this.ship.getY(), this.ship.getZ(),
                                SoundEvents.ITEM_PICKUP, this.ship.getSoundSource(),
                                0.8F,
                                ((this.ship.getRandom().nextFloat() - this.ship.getRandom().nextFloat()) * 0.7F + 1.0F)
                                        * 2.0F);

                        // pickup animation
                        this.ship.take(itemEntity, count);

                        // remove item entity if fully consumed
                        if (stack.isEmpty()) {
                            itemEntity.discard();
                            this.entItem = null;
                        }
                    }
                }

                ship.getNavigation().stop();
            }
        }
    }

    private ItemEntity getNearbyItemEntity() {
        AABB box = this.ship.getBoundingBox().inflate(this.pickRange, this.pickRange * 0.5F + 1.0F, this.pickRange);
        List<ItemEntity> items = this.ship.level().getEntitiesOfClass(ItemEntity.class, box);

        if (!items.isEmpty()) {
            items.sort(Comparator.comparingDouble(this.ship::distanceToSqr));
            return items.get(0);
        }
        return null;
    }

    private void updateShipParms() {
        float speed = this.ship.getAttrs().getAttackSpeed();
        if (speed < 1.0F)
            speed = 1.0F;

        this.pickDelayMax = (int) (10.0F / speed);

        float tempRange = this.pickRangeBase + this.ship.getStateMinor(ID.M.FollowMax);
        this.pickRange = this.pickRangeBase + this.ship.getAttrs().getAttackRange() * 0.5F;
        this.pickRange = Math.min(tempRange, this.pickRange);
    }
}
