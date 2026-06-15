package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.ai.path.ShipNavigation;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.network.S2CEntitySyncPacket;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.EntityHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for summoned entities (rensouhou, floating fort, airplanes, etc.).
 * Manages lifetime, host tracking, and ammo resource return.
 * Ported from 1.10.2 BasicEntitySummon.
 */
public abstract class BasicEntitySummon extends Mob implements IShipOwner, IShipNavigator {

    protected IShipAttackBase host;
    protected Attrs shipAttrs;
    protected int numAmmoLight;
    protected int numAmmoHeavy;
    protected int scaleLevel;
    protected boolean initScale;

    protected BasicEntitySummon(EntityType<? extends BasicEntitySummon> type, Level level) {
        super(type, level);
        this.invulnerableTime = 2;
        this.numAmmoLight = 6;
        this.numAmmoHeavy = 0;
        this.scaleLevel = 0;
        this.initScale = false;
        this.shipAttrs = new Attrs();
        this.setMaxUpStep(7.0F);
    }

    public static AttributeSupplier.Builder createSummonAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected @NotNull ShipNavigation createNavigation(@NotNull Level level) {
        return new ShipNavigation(this, level);
    }

    // ========== Static Attributes ==========

    /**
     * Called at end of subclass constructor
     */
    protected void postInit() {
        // [PORT] 1.10.2 -> 1.20.1: restore legacy summon turn-rate cap for ship-type
        // summons.
        this.moveControl = new ShipMoveControl(this, 60F, 1.5F);
    }

    // ========== Abstract Methods ==========

    /**
     * Set AI goals - override in subclass
     */
    protected abstract void setAIList();

    /**
     * Return ammo/resources to host when despawning
     */
    protected abstract void returnSummonResource();

    /**
     * Initialize attributes from host ship. Called after spawn.
     */
    public abstract void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float... par2);

    /**
     * Get max lifetime in ticks
     */
    public int getLifeLength() {
        return 1800; // 90 seconds default
    }

    /**
     * Whether this entity can still find/acquire targets
     */
    public boolean canFindTarget() {
        return this.numAmmoLight > 0 || this.numAmmoHeavy > 0;
    }

    // ========== Fire Immunity ==========

    @Override
    public boolean fireImmune() {
        return true;
    }

    // ========== Despawn Control ==========

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return false;
    }

    // ========== Tick / Update ==========

    @Override
    public void tick() {
        super.tick();

        // server side only
        if (!this.level().isClientSide()) {
            if (this.tickCount == 1) {
                sendSyncPacket(0);
            }

            boolean shouldDie = false;

            // host validity check
            if (this.host == null) {
                shouldDie = true;
            } else {
                Entity hostEnt = this.host.getHostEntity();
                if (hostEnt == null || (hostEnt instanceof LivingEntity le && !le.isAlive())) {
                    shouldDie = true;
                }
            }

            // lifetime check
            if (!shouldDie && this.tickCount > this.getLifeLength()) {
                shouldDie = true;
            }

            // target validity check - if can't find more targets, die
            LivingEntity target = this.getTarget();

            if (!shouldDie && !canFindTarget()
                    && (target == null || !target.isAlive())) {
                // try host's target
                if (this.host != null && target != null
                        && target.isAlive()) {
                    Entity host_target = this.host.getEntityTarget();

                    if (host_target instanceof LivingEntity living) {
                        this.setTarget(living);
                    }
                } else {
                    shouldDie = true;
                }
            }

            if (shouldDie) {
                if (this.host != null) {
                    this.returnSummonResource();
                }
                this.discard();
                return;
            }
        }

        // both sides: prevent drowning
        if ((this.tickCount & 127) == 0) {
            this.setAirSupply(300);
        }
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide()) {
            EntityHelper.updateShipNavigator(this);
            super.aiStep();
        } else {
            super.aiStep();
        }
    }

    // ========== IShipOwner ==========

    @Override
    public int getPlayerUID() {
        if (this.host != null)
            return this.host.getPlayerUID();
        return 0;
    }

    @Override
    public void setPlayerUID(int uid) {
    }

    @Override
    public Entity getHostEntity() {
        if (this.host != null)
            return this.host.getHostEntity();
        return null;
    }

    // ========== Host and Target ==========

    public IShipAttackBase getHost() {
        return this.host;
    }

    public void setHost(IShipAttackBase host) {
        this.host = host;
    }

    public Entity getEntityTarget() {
        return this.getTarget();
    }

    public void setEntityTarget(Entity target) {
        this.setTarget((LivingEntity) target);
    }

    public Attrs getAttrs() {
        return this.shipAttrs;
    }

    public ShipMoveControl getShipMoveControl() {
        return (ShipMoveControl) this.moveControl;
    }

    public int getScaleLevel() {
        return this.scaleLevel;
    }

    public void setScaleLevel(int level) {
        this.scaleLevel = level;
    }

    // ========== Ammo ==========

    public int getNumAmmoLight() {
        return this.numAmmoLight;
    }

    public void setNumAmmoLight(int num) {
        this.numAmmoLight = num;
    }

    public int getNumAmmoHeavy() {
        return this.numAmmoHeavy;
    }

    public void setNumAmmoHeavy(int num) {
        this.numAmmoHeavy = num;
    }

    // ========== Network Sync ==========

    /**
     * Send sync packet to tracking clients.
     * type: 0=emotion, 1=motion, 2=rotation
     */
    public void sendSyncPacket(int type) {
        if (!this.level().isClientSide()) {
            switch (type) {
                case 0:
                    ModNetworking.sendToAllTracking(
                            S2CEntitySyncPacket.syncEmotion(this), this);
                    break;
                case 1:
                    ModNetworking.sendToAllTracking(
                            S2CEntitySyncPacket.syncMotion(this), this);
                    break;
                case 2:
                    ModNetworking.sendToAllTracking(
                            S2CEntitySyncPacket.syncRotation(this), this);
                    break;
            }
        }
    }

    // ========== AI Task Management ==========

    protected void clearAITasks() {
        this.goalSelector.removeAllGoals(goal -> true);
    }

    protected void clearAITargetTasks() {
        this.setTarget(null);
        this.targetSelector.removeAllGoals(goal -> true);
    }
}
