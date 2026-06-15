package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.ShipAircraftAttackGoal;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.CombatHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Base class for airplane (carrier aircraft) entities.
 * Extends BasicEntitySummon for host tracking, lifetime, and summon mechanics.
 * Implements IShipCannonAttack for attack capabilities and IShipFlyable marker.
 * <p>
 * Ported from 1.10.2 BasicEntityAirplane.
 */
public abstract class BasicEntityAirplane extends BasicEntitySummon
        implements IShipCannonAttack, IShipFlyable, IShipNavigator {

    protected boolean backHome;
    protected boolean canFindTargetFlag;

    // IShipAttackBase fields
    private Entity revengeTarget;
    private int revengeTime;
    private HashMap<Integer, Integer> buffMap = new HashMap<>();
    private HashMap<Integer, int[]> attackEffectMap = new HashMap<>();

    protected BasicEntityAirplane(EntityType<? extends BasicEntityAirplane> type, Level level) {
        super(type, level);
        this.backHome = false;
        this.canFindTargetFlag = true;
        this.setNoGravity(true);
        this.numAmmoLight = 9;
        this.numAmmoHeavy = 0;
    }

    // ========== Static Attributes ==========

    public static AttributeSupplier.Builder createAirplaneAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    // ========== AI Setup ==========

    @Override
    protected void setAIList() {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.goalSelector.addGoal(1, new ShipAircraftAttackGoal(this));
        this.setTarget(this.getTarget());
    }

    // ========== Target Finding ==========

    @Override
    public boolean canFindTarget() {
        return this.canFindTargetFlag && (this.numAmmoLight > 0 || this.numAmmoHeavy > 0);
    }

    // ========== Lifetime ==========

    @Override
    public int getLifeLength() {
        return 1800; // 90 seconds
    }

    // ========== Movement ==========

    /**
     * Custom travel for flying entities - no gravity, 0.91 friction,
     * rise when colliding horizontally.
     */
    @Override
    public void travel(Vec3 travelVector) {
        // apply movement with reduced speed for air
        this.moveRelative(0.02F, travelVector);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        // apply friction
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.91D, motion.y * 0.91D, motion.z * 0.91D);

        // rise when colliding horizontally
        if (this.horizontalCollision) {
            this.setDeltaMovement(
                    this.getDeltaMovement().add(0, 0.2D, 0));
        }

        // limb swing animation
        this.calculateEntityAnimation(false);
    }

    // ========== No Collision Push ==========

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        // airplanes don't collide with other entities
    }

    // ========== No Fall Damage ==========

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    // ========== Tick / Update ==========

    @Override
    public void tick() {
        // server side airplane logic (before super.tick which handles host/lifetime
        // checks)
        if (!this.level().isClientSide()) {
            if (this.host != null && ((Entity) this.host).isAlive()) {
                Entity hostEnt = (Entity) this.host;

                // return home behavior
                if (this.backHome && this.isAlive()) {
                    float dist = this.distanceTo(hostEnt);

                    if (dist > 2F + hostEnt.getBbHeight()) {
                        // fly toward host every 16 ticks
                        this.getNavigation().moveTo(
                                hostEnt.getX(),
                                hostEnt.getY() + hostEnt.getBbHeight() + 1D,
                                hostEnt.getZ(), 1D);
                        // host is too far away - just despawn
                        if (this.getNavigation().isDone()
                                && this.distanceToSqr(hostEnt) >= 4095F) {
                            this.returnSummonResource();
                            this.discard();
                            return;
                        }
                    } else {
                        // reached home - return resources and despawn
                        this.returnSummonResource();
                        this.discard();
                        return;
                    }
                }

                // initial straight-line movement toward target
                if (this.tickCount < 34 && this.getTarget() != null) {
                    double distX = this.getTarget().getX() - this.getX();
                    double distZ = this.getTarget().getZ() - this.getZ();
                    double distSqrt = Math.sqrt(distX * distX + distZ * distZ);

                    if (distSqrt > 0.01D) {
                        this.setDeltaMovement(
                                distX / distSqrt * 0.375D,
                                0.1D,
                                distZ / distSqrt * 0.375D);
                    }
                }

                // target finding every 16 ticks
                if (this.tickCount % 16 == 0 && this.canFindTarget() && !this.backHome) {
                    boolean findNewTarget = false;

                    if (this.tickCount < 1200) {
                        if (this.getTarget() == null || !this.getTarget().isAlive()) {
                            findNewTarget = true;
                        }
                    }

                    if (this.tickCount >= 20 && findNewTarget) {
                        Entity newTarget = findNearbyTarget();

                        if (newTarget == null && this.host != null) {
                            newTarget = this.getTarget();
                        }

                        if (newTarget != null) {
                            this.setEntityTarget(newTarget);
                            this.backHome = false;
                        } else {
                            this.setEntityTarget(null);
                            this.backHome = true;
                        }
                    }
                }

                // auto-return home at 1200 ticks (60 seconds)
                if (this.tickCount >= 1200) {
                    this.setEntityTarget(null);
                    this.backHome = true;
                }
            }

            // check ammo - if out of ammo, set back home
            if (!this.hasAmmoLight() && !this.hasAmmoHeavy()) {
                this.backHome = true;
                this.setEntityTarget(null);
            }
        }

        // facing calculation (both sides, every 2 ticks)
        Vec3 motion = this.getDeltaMovement();
        double dx = motion.x;
        double dy = motion.y;
        double dz = motion.z;
        double xzDist = Math.sqrt(dx * dx + dz * dz);

        if (xzDist > 0.01D) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
            this.setYRot(yaw);
            this.setXRot((float) -(Mth.atan2(dy, xzDist) * (180D / Math.PI)));
        }

        super.tick();
    }

    /**
     * Find a valid hostile entity nearby for targeting
     */
    private Entity findNearbyTarget() {
        double range = 24D;

        // if host has anti-air flag, search wider for airplanes first
        if (this.host != null && this.host.getStateFlag(ID.F.AntiAir)) {
            AABB searchBox = this.getBoundingBox().inflate(32D, 32D, 32D);
            List<BasicEntityAirplane> airTargets = this.level().getEntitiesOfClass(
                    BasicEntityAirplane.class, searchBox,
                    this::isValidTarget);

            if (!airTargets.isEmpty()) {
                airTargets.sort(Comparator.comparingDouble(this::distanceToSqr));
                return airTargets.get(0);
            }
        }

        // search for general targets
        AABB searchBox = this.getBoundingBox().inflate(range, range, range);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class, searchBox,
                this::isValidTarget);

        if (!targets.isEmpty()) {
            targets.sort(Comparator.comparingDouble(this::distanceToSqr));
            return targets.get(0);
        }

        return null;
    }

    /**
     * Check if entity is a valid attack target
     */
    protected boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive())
            return false;
        if (target == this || target == this.host)
            return false;

        // don't attack same owner's entities
        if (target instanceof IShipOwner owner) {
            if (this.getPlayerUID() > 0 && owner.getPlayerUID() == this.getPlayerUID()) {
                return false;
            }
        }

        // basic visibility/range check
        return this.hasLineOfSight(target);
    }

    // ========== Death Animation ==========

    @Override
    public boolean isOnFire() {
        // display fire effect when dying
        if (this.deathTime > 30)
            return true;
        return super.isOnFire();
    }

    // ========== Damage Source ==========

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (this.level().isClientSide())
            return false;

        // 33% dodge chance against heavy damage
        if (amount > this.getMaxHealth() * 0.5F && this.random.nextInt(3) == 0) {
            return false;
        }

        return super.hurt(source, amount);
    }

    // ========== Attack ==========

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.host == null) {
            this.discard();
            return false;
        }

        // consume ammo
        if (numAmmoLight > 0)
            numAmmoLight--;

        // get attack value
        float atk = this.shipAttrs.getAttackDamageAir();

        // calc distance for combat rate
        float dist = (float) Math.sqrt(this.distanceToSqr(target));

        // apply combat rate (miss/crit/dhit/thit)
        atk = CombatHelper.applyCombatRateToDamage(this, target, false, dist, atk);

        // check friendly fire
        if (CombatHelper.isFriendlyFire(this, target))
            return false;

        // if missed
        if (atk <= 0F)
            return true;

        // deal damage
        boolean isHurt = false;
        if (target instanceof LivingEntity livingTarget) {
            isHurt = livingTarget.hurt(this.damageSources().mobAttack(this), atk);
        }

        return isHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host == null) {
            this.discard();
            return false;
        }

        // consume ammo
        if (numAmmoHeavy > 0)
            numAmmoHeavy--;

        // get heavy attack value
        float atk = this.shipAttrs.getAttackDamageAirHeavy();

        // calc distance for combat rate
        float dist = (float) Math.sqrt(this.distanceToSqr(target));

        // apply combat rate
        atk = CombatHelper.applyCombatRateToDamage(this, target, false, dist, atk);

        // check friendly fire
        if (CombatHelper.isFriendlyFire(this, target))
            return false;

        // if missed
        if (atk <= 0F)
            return true;

        // deal damage
        boolean isHurt = false;
        if (target instanceof LivingEntity livingTarget) {
            isHurt = livingTarget.hurt(this.damageSources().mobAttack(this), atk);
        }

        return isHurt;
    }

    // ========== Resource Return ==========

    @Override
    protected void returnSummonResource() {
        // only return resources to friendly CV ships
        if (this.host instanceof BasicEntityShipCV ship) {
            // light cost 6, plane gets 9 => net cost -3
            this.numAmmoLight -= 3;
            if (this.numAmmoLight < 0)
                this.numAmmoLight = 0;

            // heavy cost 2, plane gets 3 => net cost -1
            this.numAmmoHeavy -= 1;
            if (this.numAmmoHeavy < 0)
                this.numAmmoHeavy = 0;

            // return ammo to host
            int ammoConsumption = ship.getAmmoConsumption();
            ship.setStateMinor(ID.M.NumAmmoLight,
                    ship.getStateMinor(ID.M.NumAmmoLight) + this.numAmmoLight * ammoConsumption);
            ship.setStateMinor(ID.M.NumAmmoHeavy,
                    ship.getStateMinor(ID.M.NumAmmoHeavy) + this.numAmmoHeavy * ammoConsumption);

            // return airplane slot to host
            if (this.useAmmoLight() && !this.useAmmoHeavy()) {
                ship.setNumAircraftLight(ship.getNumAircraftLight() + 1);
            } else {
                ship.setNumAircraftHeavy(ship.getNumAircraftHeavy() + 1);
            }
        }
    }

    // ========== Init Attributes (called by concrete subclass) ==========

    /**
     * Abstract initAttrs is implemented by concrete airplane classes.
     * This base helper can be called from subclass initAttrs for friendly ships.
     */
    protected void initAttrsFromHost(BasicEntityShip ship, Entity target, int scaleLevel, float launchY) {
        this.host = ship;
        this.setTarget((LivingEntity) target);
        this.setScaleLevel(scaleLevel);

        // set spawn position
        this.setPos(ship.getX(), launchY, ship.getZ());

        // copy and modify attrs from host ship
        this.shipAttrs = Attrs.copyAttrs(ship.getAttrs());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.HP,
                ship.getLevel() + ship.getAttrs().getAttrsBuffed(ID.Attrs.HP) * 0.1F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_AL,
                ship.getAttrs().getAttackDamageAir());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_AH,
                ship.getAttrs().getAttackDamageAirHeavy());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_L,
                ship.getAttrs().getAttackDamageAir());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_H,
                ship.getAttrs().getAttackDamageAirHeavy());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.DEF,
                ship.getAttrs().getDefense() * 0.5F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.SPD,
                ship.getAttrs().getAttackSpeed() * 3F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.MOV,
                ship.getAttrs().getMoveSpeed() * 0.2F + 0.3F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.HIT, 16F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.DODGE,
                this.shipAttrs.getAttrsBuffed(ID.Attrs.DODGE) + 0.3F);

        applyInitAttrs();

        // setup navigator and AI
        this.navigation = this.createNavigation(this.level());
        this.moveControl = new ShipMoveControl(this, 60F, 1.5F);
        this.setAIList();
    }

    /**
     * Init attrs from a hostile ship host.
     */
    protected void initAttrsFromHostile(BasicEntityShipHostile hostile, Entity target, int scaleLevel, float launchY) {
        this.host = hostile;
        this.setTarget((LivingEntity) target);
        this.setScaleLevel(scaleLevel);

        this.setPos(hostile.getX(), launchY, hostile.getZ());

        this.shipAttrs = Attrs.copyAttrs(hostile.getAttrs());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.HP,
                hostile.getAttrs().getAttrsBuffed(ID.Attrs.HP) * 0.1F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_AL,
                hostile.getAttrs().getAttackDamageAir());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_AH,
                hostile.getAttrs().getAttackDamageAirHeavy());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_L,
                hostile.getAttrs().getAttackDamageAir());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.ATK_H,
                hostile.getAttrs().getAttackDamageAirHeavy());
        this.shipAttrs.setAttrsBuffed(ID.Attrs.DEF,
                hostile.getAttrs().getDefense() * 0.5F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.SPD,
                hostile.getAttrs().getAttackSpeed() * 3F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.MOV,
                hostile.getAttrs().getMoveSpeed() * 0.2F + 0.3F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.HIT, 16F);
        this.shipAttrs.setAttrsBuffed(ID.Attrs.DODGE,
                this.shipAttrs.getAttrsBuffed(ID.Attrs.DODGE) + 0.3F);

        applyInitAttrs();

        this.navigation = this.createNavigation(this.level());
        this.moveControl = new ShipMoveControl(this, 36F, 1.5F);
        this.setAIList();
    }

    /**
     * Apply computed attrs to entity attributes. Called from both init helpers.
     */
    private void applyInitAttrs() {
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH))
                .setBaseValue(this.shipAttrs.getAttrsBuffed(ID.Attrs.HP));
        Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED))
                .setBaseValue(this.shipAttrs.getAttrsBuffed(ID.Attrs.MOV));
        Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE)).setBaseValue(64D);
        Objects.requireNonNull(this.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(1D);

        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    // ========== IShipCannonAttack ==========

    @Override
    public boolean useAmmoLight() {
        return true;
    }

    @Override
    public boolean useAmmoHeavy() {
        return false;
    }

    // ========== IShipAttackBase ==========

    @Override
    public Entity getEntityRevengeTarget() {
        return this.revengeTarget;
    }

    @Override
    public void setEntityRevengeTarget(Entity target) {
        this.revengeTarget = target;
    }

    @Override
    public int getEntityRevengeTime() {
        return this.revengeTime;
    }

    @Override
    public void setEntityRevengeTime() {
        this.revengeTime = this.tickCount;
    }

    @Override
    public int getDamageType() {
        return ID.ShipDmgType.AIRPLANE;
    }

    @Override
    public boolean getAttackType(int par1) {
        return false;
    }

    @Override
    public int getAmmoLight() {
        return this.numAmmoLight;
    }

    @Override
    public void setAmmoLight(int num) {
        this.numAmmoLight = num;
    }

    @Override
    public int getAmmoHeavy() {
        return this.numAmmoHeavy;
    }

    @Override
    public void setAmmoHeavy(int num) {
        this.numAmmoHeavy = num;
    }

    @Override
    public boolean hasAmmoLight() {
        return this.numAmmoLight > 0;
    }

    @Override
    public boolean hasAmmoHeavy() {
        return this.numAmmoHeavy > 0;
    }

    @Override
    public int getLevel() {
        if (this.host instanceof BasicEntityShip ship) {
            return ship.getLevel();
        }
        return 1;
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    @Override
    public HashMap<Integer, Integer> getBuffMap() {
        if (this.host != null) {
            return this.host.getBuffMap();
        }
        return this.buffMap;
    }

    @Override
    public void setBuffMap(HashMap<Integer, Integer> map) {
        this.buffMap = map;
    }

    @Override
    public HashMap<Integer, int[]> getAttackEffectMap() {
        if (this.host != null) {
            return this.host.getAttackEffectMap();
        }
        return this.attackEffectMap;
    }

    @Override
    public void setAttackEffectMap(HashMap<Integer, int[]> map) {
        this.attackEffectMap = map;
    }

    @Override
    public MissileData getMissileData(int type) {
        if (this.host != null) {
            // 2026/04/07・哦itHub Copilot縺ｫ繧医▲縺ｦ遒ｺ隱肴ｸ医∩
            return this.host.getMissileData(type);
        }
        return new MissileData();
    }

    @Override
    public void setMissileData(int type, MissileData data) {
        if (this.host != null) {
            this.host.setMissileData(type, data);
        }
    }

    // ========== IShipNavigator ==========

    @Override
    public boolean canFly() {
        return true;
    }

    @Override
    public boolean isJumping() {
        return false;
    }

    @Override
    public float getMoveSpeed() {
        return this.shipAttrs != null ? this.shipAttrs.getMoveSpeed() : 0.5F;
    }

    @Override
    public float getJumpSpeed() {
        return 2F;
    }

    // ========== IShipEmotion ==========

    @Override
    public int getStateEmotion(int id) {
        return 0;
    }

    @Override
    public void setStateEmotion(int id, int value, boolean sync) {
    }

    @Override
    public int getStateTimer(int id) {
        return 0;
    }

    @Override
    public void setStateTimer(int id, int value) {
    }

    @Override
    public int getFaceTick() {
        return 0;
    }

    @Override
    public void setFaceTick(int par1) {
    }

    @Override
    public int getHeadTiltTick() {
        return 0;
    }

    @Override
    public void setHeadTiltTick(int par1) {
    }

    @Override
    public int getAttackTick() {
        return 0;
    }

    @Override
    public void setAttackTick(int par1) {
    }

    @Override
    public int getAttackTick2() {
        return 0;
    }

    @Override
    public void setAttackTick2(int par1) {
    }

    @Override
    public int getDeathTick() {
        return 0;
    }

    @Override
    public void setDeathTick(int par1) {
    }

    @Override
    public float getModelRotate(int par1) {
        return 0;
    }

    @Override
    public void setModelRotate(int par1, float par2) {
    }

    @Override
    public int getTickExisted() {
        return this.tickCount;
    }

    @Override
    public float getSwingTime(float partialTick) {
        return 0;
    }

    @Override
    public boolean getIsRiding() {
        return this.isPassenger();
    }

    @Override
    public boolean getIsSprinting() {
        return this.isSprinting();
    }

    @Override
    public boolean getIsSitting() {
        return false;
    }

    @Override
    public boolean getIsSneaking() {
        return this.isShiftKeyDown();
    }

    @Override
    public boolean getIsLeashed() {
        return this.isLeashed();
    }

    @Override
    public void setEntitySit(boolean sit) {
    }

    @Override
    public int getRidingState() {
        return 0;
    }

    @Override
    public void setRidingState(int state) {
    }

    @Override
    public RandomSource getRand() {
        return this.random;
    }

    @Override
    public double getShipDepth(int type) {
        return 0;
    }

    // ========== IShipFlags ==========

    @Override
    public int getStateMinor(int id) {
        return 0;
    }

    @Override
    public void setStateMinor(int state, int par1) {
    }

    @Override
    public boolean getStateFlag(int flag) {
        // for attack AI check
        return flag != ID.F.OnSightChase;
    }

    @Override
    public void setStateFlag(int id, boolean flag) {
    }

    @Override
    public void setUpdateFlag(int id, boolean value) {
    }

    @Override
    public boolean getUpdateFlag(int id) {
        return false;
    }

    // ========== IShipAttrs ==========

    @Override
    public void setAttrs(Attrs data) {
        this.shipAttrs = data;
    }
}
