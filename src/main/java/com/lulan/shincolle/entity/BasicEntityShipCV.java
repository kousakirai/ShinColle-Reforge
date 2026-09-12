package com.lulan.shincolle.entity;

import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.entity.other.EntityAirplane;
import com.lulan.shincolle.entity.other.EntityAirplaneTakoyaki;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.item.EquipAirplane;
import com.lulan.shincolle.reference.ID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Base class for carrier-type ships with aircraft attacks.
 * Manages aircraft counts and provides light/heavy aircraft spawn methods.
 * Subclasses can override getAttackAirplane() to change airplane types.
 * <p>
 * Aircraft counts are stored in StateMinor[ID.M.NumAirLight] and
 * StateMinor[ID.M.NumAirHeavy] to unify with save/load.
 * <p>
 * Ported from 1.10.2 BasicEntityShipCV.
 */
public abstract class BasicEntityShipCV extends BasicEntityShip implements IShipAircraftAttack {

    protected double launchHeight = 2.0D;

    /**
     * Max aircraft counts - dynamically recalculated from level and equipment
     */
    protected int maxAircraftLight = 6;
    protected int maxAircraftHeavy = 3;

    /**
     * Aircraft recovery delay counter (ticks down each tick, restocks when <= 0)
     */
    protected int delayAircraft = 0;

    protected BasicEntityShipCV(EntityType<? extends BasicEntityShipCV> type, Level level) {
        super(type, level);
    }

    // ========== Aircraft Count (delegates to StateMinor) ==========

    @Override
    public int getNumAircraftLight() {
        return getStateMinor(ID.M.NumAirLight);
    }

    @Override
    public void setNumAircraftLight(int par1) {
        if (this.level().isClientSide()) {
            // client side has no max value reference, set directly
            setStateMinor(ID.M.NumAirLight, par1);
        } else {
            setStateMinor(ID.M.NumAirLight, par1);
            if (getNumAircraftLight() > maxAircraftLight) setStateMinor(ID.M.NumAirLight, maxAircraftLight);
            if (getNumAircraftLight() < 0) setStateMinor(ID.M.NumAirLight, 0);
        }
    }

    @Override
    public int getNumAircraftHeavy() {
        return getStateMinor(ID.M.NumAirHeavy);
    }

    @Override
    public void setNumAircraftHeavy(int par1) {
        if (this.level().isClientSide()) {
            setStateMinor(ID.M.NumAirHeavy, par1);
        } else {
            setStateMinor(ID.M.NumAirHeavy, par1);
            if (getNumAircraftHeavy() > maxAircraftHeavy) setStateMinor(ID.M.NumAirHeavy, maxAircraftHeavy);
            if (getNumAircraftHeavy() < 0) setStateMinor(ID.M.NumAirHeavy, 0);
        }
    }

    @Override
    public boolean hasAirLight() {
        return getStateMinor(ID.M.NumAirLight) > 0;
    }

    @Override
    public boolean hasAirHeavy() {
        return getStateMinor(ID.M.NumAirHeavy) > 0;
    }

    /**
     * Carrier requires 6x ammo consumption for light aircraft launch
     */
    @Override
    public boolean hasAmmoLight() {
        return getStateMinor(ID.M.NumAmmoLight) >= 6 * getAmmoConsumption();
    }

    /**
     * Carrier requires 2x ammo consumption for heavy aircraft launch
     */
    @Override
    public boolean hasAmmoHeavy() {
        return getStateMinor(ID.M.NumAmmoHeavy) >= 2 * getAmmoConsumption();
    }

    public double getLaunchHeight() {
        return this.launchHeight;
    }

    // ========== Aircraft Restocking (attack-speed-based delay) ==========

    @Override
    public void aiStep() {
        super.aiStep();

        // server-side aircraft restocking with attack-speed-based delay
        if (!this.level().isClientSide()) {
            delayAircraft--;
            if (this.delayAircraft <= 0) {
                // calc delay from attack speed (original formula)
                float atkSpd = this.getAttrs().getAttackSpeed();
                delayAircraft = (int) ((float) ConfigHandler.cdAirplaneRecovery() / Math.max(atkSpd, 0.01F));
                if (delayAircraft > ConfigHandler.cdAirplaneRecovery()) {
                    delayAircraft = ConfigHandler.cdAirplaneRecovery();
                }
                // base delay
                delayAircraft += 20;

                this.setNumAircraftLight(this.getNumAircraftLight() + 1);
                this.setNumAircraftHeavy(this.getNumAircraftHeavy() + 1);
            }
        }
    }

    // ========== Dynamic Max Aircraft Calculation ==========

    @Override
    public void calcShipAttributesAddRaw() {
        super.calcShipAttributesAddRaw();

        // calc basic airplane max from level
        this.maxAircraftLight = 8 + getLevel() / 5;
        this.maxAircraftHeavy = 4 + getLevel() / 10;
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();

        // add airplane max from equipped aircraft items
        int numair = getNumOfAircraftEquip();
        this.maxAircraftLight += (numair * 4);
        this.maxAircraftHeavy += (numair * 2);
    }

    /**
     * Count number of airplane equipment in equip slots
     */
    public int getNumOfAircraftEquip() {
        int airNum = 0;

        for (int i = 0; i < ContainerShipInventory.EQUIP_SLOTS; i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof EquipAirplane) {
                airNum++;
            }
        }

        return airNum;
    }

    // ========== Airplane Factory ==========

    /**
     * Create an airplane entity for attacking.
     * Subclasses can override to change airplane types.
     *
     * @param isLight true for light airplane, false for heavy (takoyaki)
     * @return new airplane entity (not yet spawned)
     */
    protected BasicEntityAirplane getAttackAirplane(boolean isLight) {
        if (isLight) {
            return new EntityAirplane(ModEntities.AIRPLANE.get(), this.level());
        } else {
            return new EntityAirplaneTakoyaki(ModEntities.AIRPLANE_TAKOYAKI.get(), this.level());
        }
    }

    // ========== Light Aircraft Attack ==========

    @Override
    public boolean attackEntityWithAircraft(Entity target) {
        // check aircraft and ammo availability
        // light air: costs 6 ammo light per launch
        if (this.getNumAircraftLight() <= 0
                || !decrAmmoNum(0, 6 * this.getAmmoConsumption())) {
            return false;
        }

        // 50% chance to clear target each attack (forces retargeting)
        if (this.random.nextInt(2) == 0) {
            this.setEntityTarget(null);
        }

        // consume aircraft slot (returned when airplane despawns)
        this.setNumAircraftLight(this.getNumAircraftLight() - 1);

        // experience, grudge, morale
        addShipExp(ConfigHandler.expGain[3]);
        decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.LAir]);
        decrMorale(3);
        setCombatTick(this.tickCount);

        // calculate launch position
        float summonHeight = (float) (this.getY() + launchHeight);

        // check if launch position is safe - if not, spawn just above ship
        if (!level().getBlockState(
                        new net.minecraft.core.BlockPos(
                                (int) this.getX(),
                                (int) (this.getY() + launchHeight),
                                (int) this.getZ()))
                .isAir()) {
            summonHeight = (float) this.getY() + 1F;
        }

        // adjust for riding mount
        if (this.getVehicle() instanceof BasicEntityMount) {
            summonHeight -= 1.5F;
        }

        // spawn airplane
        BasicEntityAirplane plane = getAttackAirplane(true);
        plane.initAttrs(this, target, 0, summonHeight);
        this.level().addFreshEntity(plane);

        // play sounds and particles
        applySoundAtAttacker(3, target);
        applyParticleAtAttacker(3, target, target);
        applyEmotesReaction(3);

        return true;
    }

    // ========== Heavy Aircraft Attack ==========

    @Override
    public boolean attackEntityWithHeavyAircraft(Entity target) {
        // check aircraft and ammo availability
        // heavy air: costs 2 ammo heavy per launch
        if (this.getNumAircraftHeavy() <= 0
                || !decrAmmoNum(1, 2 * this.getAmmoConsumption())) {
            return false;
        }

        // 50% chance to clear target each attack
        if (this.random.nextInt(2) == 0) {
            this.setEntityTarget(null);
        }

        // consume aircraft slot
        this.setNumAircraftHeavy(this.getNumAircraftHeavy() - 1);

        // experience, grudge, morale
        addShipExp(ConfigHandler.expGain[4]);
        decrGrudgeNum(ConfigHandler.consumeGrudgeAction[ID.ShipConsume.HAir]);
        decrMorale(4);
        setCombatTick(this.tickCount);

        // calculate launch position
        float summonHeight = (float) (this.getY() + launchHeight);

        if (!level().getBlockState(
                        new net.minecraft.core.BlockPos(
                                (int) this.getX(),
                                (int) (this.getY() + launchHeight),
                                (int) this.getZ()))
                .isAir()) {
            summonHeight = (float) this.getY() + 0.5F;
        }

        if (this.getVehicle() instanceof BasicEntityMount) {
            summonHeight -= 1.5F;
        }

        // spawn heavy airplane
        BasicEntityAirplane plane = getAttackAirplane(false);
        plane.initAttrs(this, target, 0, summonHeight);
        this.level().addFreshEntity(plane);

        // play sounds and particles
        applySoundAtAttacker(4, target);
        applyParticleAtAttacker(4, target, target);
        applyEmotesReaction(3);

        return true;
    }
}
