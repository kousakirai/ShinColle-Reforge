package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.ai.path.ShipNavigation;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.network.S2CEntitySyncPacket;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Objects;

/**
 * Base class for hostile/enemy ship entities (mob variants).
 * Extends Mob (equivalent to EntityMob in 1.10.2).
 */
public abstract class BasicEntityShipHostile extends Mob
        implements IShipCannonAttack, IShipFloating, IShipNavigator, IShipCustomTexture {

    // ========== Fields ==========

    protected ShipNavigation shipNavigator;
    protected Entity rvgTarget;
    // AI calculation
    protected double ShipDepth;
    protected double ShipFloatingDepth;
    /**
     * ship attributes
     */
    protected AttrsAdv shipAttrs;
    /**
     * minor states, index by {@link ID.M}
     */
    protected int[] StateMinor;
    /**
     * timer array, index by {@link ID.T}
     */
    protected int[] StateTimer;
    /**
     * EntityState, index by {@link ID.S}
     */
    protected int[] StateEmotion;
    /**
     * EntityFlag, index by {@link ID.F}
     */
    protected boolean[] StateFlag;
    /**
     * ModelPos: posX, posY, posZ, scale
     */
    protected float[] ModelPos;
    /**
     * Update Flag
     */
    protected boolean[] UpdateFlag;
    /**
     * attack attributes
     */
    protected HashMap<Integer, Integer> BuffMap;
    protected HashMap<Integer, int[]> AttackEffectMap;
    protected MissileData[] MissileData;
    // model render
    protected float[] rotateAngle;
    // texture
    protected int textureID;
    // riding state
    protected int ridingState;
    // scale level: 0=small mob, 1=large mob, 2=small boss, 3=large boss
    protected int scaleLevel;
    // boss bar
    protected ServerBossEvent bossEvent;
    protected BossEvent.BossBarColor bossBarColor = BossEvent.BossBarColor.RED;
    // smoke particle positions (for client rendering)
    protected float smokeX, smokeY;
    protected int emoteDelay;
    // initialization
    private boolean initAI;
    private int revengeTime;

    protected BasicEntityShipHostile(EntityType<? extends BasicEntityShipHostile> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.invulnerableTime = 2;

        // init arrays
        this.StateMinor = new int[]{
                1, 0, 0, 40, 0,
                0, 0, 0, 0, 3,
                3, 12, 35, 1, -1,
                -1, -1, 0, -1, 0,
                0, -1, -1, -1, 0,
                0, 0, 0, 0, 0,
                60, 0, 10, 0, 0,
                -1, 0, 0, 0, 0,
                -1, -1, -1, 0, 0
        };
        this.StateTimer = new int[21];
        this.StateEmotion = new int[8];
        this.StateFlag = new boolean[]{
                false, false, false, false, true,
                true, true, true, false, true,
                true, false, true, true, true,
                true, true, true, true, false,
                false, false, true, true, false,
                true, false
        };
        this.UpdateFlag = new boolean[8];
        this.ModelPos = new float[]{0F, 0F, 0F, 50F};
        this.BuffMap = new HashMap<>();
        this.AttackEffectMap = new HashMap<>();
        this.resetMissileData();

        // AI
        this.ShipDepth = 0D;
        this.ShipFloatingDepth = 0D;
        this.setMaxUpStep(1.0F);

        // render
        this.rotateAngle = new float[3];
        this.textureID = 0;
        this.ridingState = 0;
        this.scaleLevel = 0;

        // init
        this.initAI = false;
        this.revengeTime = 0;
    }

    public static AttributeSupplier.Builder createHostileShipAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    // ========== Fields ==========
    @Override
    protected @NotNull ShipNavigation createNavigation(@NotNull Level level) {
        return new ShipNavigation(this, level, this.canFly());
    }

    // ========== Static Attribute Builder ==========

    /**
     * init values, called at the end of subclass constructor
     */
    protected void postInit() {
        this.shipNavigator = this.createNavigation(this.level());
        // [PORT] 1.10.2 -> 1.20.1: restore legacy hostile ship turn-rate cap.
        this.moveControl = new ShipMoveControl(this, this.canFly(), 10F);
        this.shipAttrs = new AttrsAdv(this.getShipClass());

    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);

        if (!level.isClientSide()) {
            int initScale;
            if (dataTag != null && dataTag.contains("ScaleLevel", Tag.TAG_INT)) {
                initScale = Mth.clamp(dataTag.getInt("ScaleLevel"), 0, 3);
            } else {
                // [PORT] 1.10.2 -> 1.20.1: keep naturally spawned hostile ships larger
                // than regular ships by default.
                initScale = 1 + this.random.nextInt(3);
            }
            this.initAttrs(initScale);
        }

        return result;
    }

    // ========== Init / Abstract Methods ==========

    /**
     * Initialize attributes for a hostile ship entity.
     * Sets scale level, adjusts entity size, and recalculates ship attributes.
     * Called when spawning from a spawn egg.
     *
     * @param scaleLevel 0=small mob, 1=large mob, 2=small boss, 3=large boss
     */
    public void initAttrs(int scaleLevel) {
        setScaleLevel(scaleLevel);
        if (!this.level().isClientSide()) {
            calcShipAttributes(31, false);
        }
        creatBossEvent();
        setHealth(this.getMaxHealth());
    }

    /**
     * Set size based on scale level
     */
    public abstract void setSizeWithScaleLevel();

    // ========== Fire Immunity ==========

    @Override
    public boolean fireImmune() {
        return true;
    }

    // ========== Despawn Control ==========

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return this.scaleLevel < 2; // bosses don't despawn
    }

    // ========== Invulnerability ==========

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source == damageSources().fellOutOfWorld())
            return false;
        if (StateTimer[ID.T.ImmuneTime] > 0)
            return true;
        return super.isInvulnerableTo(source);
    }

    // ========== Visual Effects ==========

    @Override
    public boolean isOnFire() {
        return this.getStateEmotion(ID.S.HPState) == ID.HPState.HEAVY;
    }

    // ========== Sound Methods ==========

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        // Hostile ships are normally silent
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SHIP_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SHIP_DEATH.get();
    }

    // ========== Movement ==========

    @Override
    public void travel(Vec3 travelVec) {
        super.travel(travelVec);
    }

    // ========== AI Setup ==========

    protected void setAIList() {
        // floating (highest priority, no mutex)
        this.goalSelector.addGoal(0, new ShipFloatingGoal(this));
        // [PORT] 1.10.2 -> 1.20.1: restore legacy hostile mobility goals
        this.goalSelector.addGoal(21, new ShipOpenDoorGoal(this, true));
        this.goalSelector.addGoal(23, new ShipHostileWanderGoal(this, 12, 1, 0.8D));
        // ranged cannon attack
        this.goalSelector.addGoal(11, new ShipRangeAttackGoal(this));
        // melee attack on collide
        this.goalSelector.addGoal(15, new ShipAttackOnCollideGoal(this, 1.0D));
        // watch + look idle
        this.goalSelector.addGoal(25,
                new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(26, new RandomLookAroundGoal(this));
    }

    public void setAITargetList() {
        this.targetSelector.addGoal(1, new ShipRevengeTargetGoal(this));
        // [PORT] 1.10.2 -> 1.20.1: legacy hostile range target priority is 3.
        this.targetSelector.addGoal(3, new ShipRangeTargetGoal(this));
    }

    protected void clearAITasks() {
        this.goalSelector.removeAllGoals(goal -> true);
    }

    protected void clearAITargetTasks() {
        this.setTarget(null);
        this.setEntityTarget(null);
        this.targetSelector.removeAllGoals(goal -> true);
    }

    // ========== NBT Save/Load ==========

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("ScaleLevel", this.scaleLevel);
        nbt.putIntArray("StateMinor", this.StateMinor);
        nbt.putIntArray("StateEmotion", this.StateEmotion);
        // Save StateFlag as byte array (boolean[] -> byte[])
        byte[] flagBytes = new byte[StateFlag.length];
        for (int i = 0; i < StateFlag.length; i++)
            flagBytes[i] = (byte) (StateFlag[i] ? 1 : 0);
        nbt.putByteArray("StateFlag", flagBytes);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("ScaleLevel")) {
            this.scaleLevel = nbt.getInt("ScaleLevel");
            setSizeWithScaleLevel();
        }
        if (nbt.contains("StateMinor")) {
            int[] minors = nbt.getIntArray("StateMinor");
            int len = Math.min(minors.length, this.StateMinor.length);
            System.arraycopy(minors, 0, this.StateMinor, 0, len);
        }
        if (nbt.contains("StateEmotion")) {
            int[] emotions = nbt.getIntArray("StateEmotion");
            int len = Math.min(emotions.length, this.StateEmotion.length);
            System.arraycopy(emotions, 0, this.StateEmotion, 0, len);
        }
        // Load StateFlag from byte array (byte[] -> boolean[])
        if (nbt.contains("StateFlag")) {
            byte[] flagBytes = nbt.getByteArray("StateFlag");
            int len = Math.min(flagBytes.length, this.StateFlag.length);
            for (int i = 0; i < len; i++)
                this.StateFlag[i] = flagBytes[i] != 0;
        }

        // recalc attributes
        calcShipAttributes(31, false);
    }

    // ========== Boss Bar ==========

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        ModNetworking.sendToPlayer(S2CEntitySyncPacket.syncScale(this, this.getScaleLevel()), player);
        if (this.bossEvent != null) {
            this.bossEvent.addPlayer(player);
        }
    }

    public void creatBossEvent() {
        if (this.scaleLevel >= 2 && !this.level().isClientSide()) {
            this.bossEvent = new ServerBossEvent(
                    this.getDisplayName(), bossBarColor, BossEvent.BossBarOverlay.PROGRESS);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    // ========== Tick / aiStep ==========

    @Override
    public void tick() {
        super.tick();
        updateSwingTime();
        if (this.emoteDelay > 0) {
            this.emoteDelay--;
        }
        if (this.tickCount == 5) {
            this.initAI = false;
        }

        // server side
        if (!level().isClientSide()) {
            EntityHelper.updateShipNavigator(this);
            TargetHelper.updateTarget(this);

            // [PORT] 1.10.2 -> 1.20.1: keep legacy bridge from vanilla target AI.
            // Some hostile targeting paths only update Mob#getTarget; mirror it to
            // custom entityTarget so ship attack goals can engage.
            if (this.getTarget() != null) {
                this.setEntityTarget(this.getTarget());
            }

            super.aiStep();

            // timer ticking
            updateServerTimer();

            // check every 8 ticks
            if ((tickCount & 7) == 0) {
                // reset AI once
                if (!this.initAI && tickCount > 10) {
                    clearAITasks();
                    clearAITargetTasks();
                    setAIList();
                    setAITargetList();
                    this.initAI = true;
                }

                // check every 64 ticks
                if ((tickCount & 63) == 0) {
                    if (this.isAlive()) {
                        // [PORT] 1.10.2 -> 1.20.1: hostile searchlight update on periodic server tick
                        updateSearchlight();
                    }

                    updateEmotionState();

                    // check every 128 ticks
                    if ((tickCount & 127) == 0) {
                        this.calcShipAttributes(31, false);

                        // check every 256 ticks
                        if ((tickCount & 255) == 0) {
                            if (this.isAlive()) {
                                // HP regen
                                if (this.getHealth() < this.getMaxHealth()) {
                                    this.heal(this.getMaxHealth() * 0.04F + 1F);
                                }
                            }
                        }
                    }
                }
            }

            // update boss bar
            if (this.bossEvent != null) {
                this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            }
        }
        // client side
        else {
            super.aiStep();
        }

        // both sides: prevent suffocation underwater
        if ((this.tickCount & 127) == 0) {
            this.setAirSupply(300);
        }
    }

    protected void updateServerTimer() {
        if (StateTimer[ID.T.ImmuneTime] > 0)
            StateTimer[ID.T.ImmuneTime]--;
        if (StateTimer[ID.T.SoundTime] > 0)
            StateTimer[ID.T.SoundTime]--;
        if (StateTimer[ID.T.EmoteDelay] > 0)
            StateTimer[ID.T.EmoteDelay]--;
        if (StateTimer[ID.T.Emotion3Time] > 0)
            StateTimer[ID.T.Emotion3Time]--;
    }

    // ========== Attribute Calculation ==========

    public void calcShipAttributes(int flag, boolean sync) {
        if (this.shipAttrs == null)
            this.shipAttrs = new AttrsAdv(this.getShipClass());

        if (!this.level().isClientSide()) {
            // recalc raw attrs from level-based stat tables
            if ((flag & 1) == 1) {
                BuffHelper.updateAttrsRawHostile(this.shipAttrs, this.getScaleLevel(), this.getShipClass());
                this.calcShipAttributesAddRaw();
            }
            if ((flag & 2) == 2) {
                this.calcShipAttributesAddEquip();
            }
            // apply buffs and finalize
            BuffHelper.applyBuffOnAttrs(this);
            this.calcShipAttributesAdd();
        }

        // set MC entity attributes
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(
                    this.shipAttrs.getAttrsBuffed(ID.Attrs.HP));
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(
                    this.shipAttrs.getAttrsBuffed(ID.Attrs.MOV));
        }
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.FOLLOW_RANGE)).setBaseValue(64);
        }
        if (this.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            Objects.requireNonNull(this.getAttribute(Attributes.KNOCKBACK_RESISTANCE)).setBaseValue(
                    this.shipAttrs.getAttrsBuffed(ID.Attrs.KB));
        }
    }

    public void calcShipAttributesAdd() {
    }

    public void calcShipAttributesAddRaw() {
    }

    public void calcShipAttributesAddEquip() {
    }

    // ========== Combat Methods ==========

    @Override
    public boolean doHurtTarget(Entity target) {
        float atk = getAttackBaseDamage(0, target);
        setCombatTick(this.tickCount);
        boolean isTargetHurt = target.hurt(this.damageSources().mobAttack(this), atk);
        if (isTargetHurt) {
            applyEmotesReaction(3);
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        ProfilerFiller profiler = DebugProfiler.push(this.level(), "shincolle.hostile.attack.light");
        try {
            setCombatTick(this.tickCount);
            float atk = getAttackBaseDamage(1, target);

            // calc distance for miss rate
            float dist = (float) Math.sqrt(this.distanceToSqr(target));

            // apply combat rate (miss/crit/dhit/thit)
            atk = CombatHelper.applyCombatRateToDamage(this, target, true, dist, atk);

            // hostile light attack uses direct damage (not missiles)
            applySoundAtAttacker(1, target);
            applyParticleAtAttacker(1, target, target);

            // if missed
            if (atk <= 0F) {
                DebugProfiler.count(profiler, "shincolle.hostile.attack.light.missed_or_zero");
                return true;
            }

            // check friendly fire
            if (CombatHelper.isFriendlyFire(this, target)) {
                atk = 0F;
                DebugProfiler.count(profiler, "shincolle.hostile.attack.light.friendly_fire_blocked");
            }

            if (atk <= 0F)
                return true;

            boolean isTargetHurt = target.hurt(this.damageSources().mobProjectile(this, this), atk);
            if (isTargetHurt) {
                DebugProfiler.count(profiler, "shincolle.hostile.attack.light.hit_success");
                // Legacy light cannon hits show the impact particle on the
                // target in addition to the attacker's muzzle flash.
                ModNetworking.sendToAllTracking(
                        new S2CSpawnParticle(target, 9, false), this);
                applyEmotesReaction(3);
            } else {
                DebugProfiler.count(profiler, "shincolle.hostile.attack.light.hit_fail");
            }
            return isTargetHurt;
        } finally {
            DebugProfiler.pop(profiler);
        }
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        setCombatTick(this.tickCount);
        float atk = getAttackBaseDamage(2, target);
        float kbValue = 0.15F;

        // launch position
        float launchPos = (float) this.getY() + this.getBbHeight() * 0.5F;

        // play attack sound
        applySoundAtAttacker(2, target);
        applyParticleAtAttacker(2, target, target);

        // target position
        float tarX = (float) target.getX();
        float tarY = (float) target.getY() + target.getBbHeight() * 0.1F;
        float tarZ = (float) target.getZ();

        // spawn missile
        MissileData md = this.getMissileData(2);
        int moveType = CombatHelper.calcMissileMoveType(this, target.getY(), 2);
        if (moveType == 0) {
            launchPos = (float) this.getY() + this.getBbHeight() * 0.3F;
        }
        EntityAbyssMissile missile = new EntityAbyssMissile(
                ModEntities.ABYSS_MISSILE.get(), this.level());
        // 2026/04/07・哦itHub Copilot縺ｫ繧医▲縺ｦ遒ｺ隱肴ｸ医∩
        missile.initMissile(this, md.type, moveType,
                atk, kbValue, launchPos, tarX, tarY, tarZ,
                160, 0.25F, md.vel0, md.accY1, md.accY2);
        this.level().addFreshEntity(missile);

        applyEmotesReaction(3);
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide())
            return false;

        // damage disabled
        if (source == this.damageSources().inWall() || source == this.damageSources().starve()
                || source == this.damageSources().cactus() || source == this.damageSources().fall()) {
            return false;
        }
        if (source == this.damageSources().fellOutOfWorld()) {
            this.teleportTo(this.getX(), 4D, this.getZ());
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        // apply defense
        float reducedAtk = amount;
        reducedAtk = CombatHelper.applyDamageReduceByDEF(reducedAtk, this);

        // ship vs ship damage scaling
        if (source.getEntity() != null) {
            reducedAtk = CombatHelper.applyShipVsShipDamage(reducedAtk, source.getEntity(), this);
        }

        if (reducedAtk < 1F && reducedAtk > 0F)
            reducedAtk = 1F;
        else if (reducedAtk <= 0F)
            reducedAtk = 0F;

        if (source.getEntity() != null) {
            this.setEntityRevengeTarget(source.getEntity());
            this.setEntityRevengeTime();
        }

        setCombatTick(this.tickCount);
        this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, false);

        return super.hurt(source, reducedAtk);
    }

    public float getAttackBaseDamage(int type, Entity target) {
        if (this.shipAttrs == null)
            return 1F;
        return switch (type) {
            case 1 -> // light cannon: apply AA/ASM bonus
                    CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2 -> this.shipAttrs.getAttackDamageHeavy();
            case 3 -> this.shipAttrs.getAttackDamageAir();
            case 4 -> this.shipAttrs.getAttackDamageAirHeavy();
            default -> this.shipAttrs.getAttackDamage() * 0.125F;
        };
    }

    public boolean decrAmmoNum(int type, int amount) {
        int ammoType = (type == 1) ? ID.M.NumAmmoHeavy : ID.M.NumAmmoLight;
        if (StateMinor[ammoType] >= amount) {
            StateMinor[ammoType] -= amount;
            return true;
        }
        return false;
    }

    public void decrGrudgeNum(int amount) {
        if (amount > 0) {
            StateMinor[ID.M.NumGrudge] -= amount;
            if (StateMinor[ID.M.NumGrudge] < 0)
                StateMinor[ID.M.NumGrudge] = 0;
        }
    }

    public void decrMorale(int type) {
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    // ========== Emotion / Reaction ==========

    protected void updateEmotionState() {
        float hpRatio = this.getHealth() / this.getMaxHealth();
        if (hpRatio > 0.75F) {
            this.setStateEmotion(ID.S.HPState, ID.HPState.NORMAL, false);
        } else if (hpRatio > 0.5F) {
            this.setStateEmotion(ID.S.HPState, ID.HPState.MINOR, false);
        } else if (hpRatio > 0.25F) {
            this.setStateEmotion(ID.S.HPState, ID.HPState.MODERATE, false);
        } else {
            this.setStateEmotion(ID.S.HPState, ID.HPState.HEAVY, false);
        }

        // [PORT] 1.10.2 -> 1.20.1: restore hostile legacy emotion roll chain.
        if (getStateFlag(ID.F.NoFuel)) {
            if (this.getStateEmotion(ID.S.Emotion) != ID.Emotion.HUNGRY) {
                this.setStateEmotion(ID.S.Emotion, ID.Emotion.HUNGRY, false);
            }
        } else if (hpRatio < 0.35F) {
            if (this.getStateEmotion(ID.S.Emotion) != ID.Emotion.T_T) {
                this.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, false);
            }
        } else {
            if (this.getStateEmotion(ID.S.Emotion) == ID.Emotion.NORMAL) {
                if (this.random.nextInt(4) == 0) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, false);
                }
            } else {
                if (this.random.nextInt(2) == 0) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
                }
            }

            if (this.getStateEmotion(ID.S.Emotion4) == ID.Emotion.NORMAL) {
                if (this.random.nextInt(3) == 0) {
                    this.setStateEmotion(ID.S.Emotion4, ID.Emotion.BORED, false);
                }
            } else {
                if (this.random.nextInt(2) == 0) {
                    this.setStateEmotion(ID.S.Emotion4, ID.Emotion.NORMAL, false);
                }
            }
        }

        if (!this.level().isClientSide()) {
            this.sendSyncPacket(0);
        }
    }

    public void applyEmotesReaction(int type) {
        switch (type) {
            case 2: // damaged
                if (this.emoteDelay <= 0) {
                    this.emoteDelay = 40;
                    reactionDamaged();
                }
                break;
            case 3: // attack
                if (this.random.nextInt(7) == 0 && this.emoteDelay <= 0) {
                    this.emoteDelay = 60;
                    reactionAttack();
                }
                break;
            case 6: // shock
                reactionShock();
                break;
            default: // idle
                if (this.random.nextInt(3) == 0 && this.emoteDelay <= 0) {
                    this.emoteDelay = 20;
                    reactionIdle();
                }
                break;
        }
    }

    public void applyParticleEmotion(int type) {
        if (!this.level().isClientSide()) {
            S2CSpawnParticle packet = new S2CSpawnParticle(this, type, false);  // Entity_Animateで十分
            ModNetworking.sendToAllTracking(packet, this);
        } else {
            ParticleHelper.spawnEmotionParticle(this, type);
        }
    }

    protected void reactionShock() {
        switch (this.random.nextInt(6)) {
            case 1 -> applyParticleEmotion(0); // drop
            case 2 -> applyParticleEmotion(8); // cry
            case 3 -> applyParticleEmotion(4); // !
            default -> applyParticleEmotion(12); // omg
        }
    }

    protected void reactionAttack() {
        switch (this.random.nextInt(15)) {
            case 1 -> applyParticleEmotion(33); // :p
            case 2 -> applyParticleEmotion(17); // gg
            case 3 -> applyParticleEmotion(7); // note
            case 4 -> applyParticleEmotion(9); // hungry
            case 5 -> applyParticleEmotion(1); // love
            case 7 -> applyParticleEmotion(16); // haha
            case 8 -> applyParticleEmotion(14); // +_+
            case 10 -> applyParticleEmotion(18); // sigh
            default -> applyParticleEmotion(4); // !
        }
    }

    protected void reactionDamaged() {
        switch (this.random.nextInt(15)) {
            case 1 -> applyParticleEmotion(4); // !
            case 2 -> applyParticleEmotion(5); // ...
            case 3 -> applyParticleEmotion(2); // panic
            case 4 -> applyParticleEmotion(3); // ?
            case 5 -> applyParticleEmotion(8); // cry
            case 7 -> applyParticleEmotion(10); // dizzy
            case 8 -> applyParticleEmotion(0); // sweat
            default -> applyParticleEmotion(6); // angry
        }
    }

    protected void reactionIdle() {
        switch (this.random.nextInt(15)) {
            case 3 -> applyParticleEmotion(7); // note
            case 6 -> applyParticleEmotion(3); // ?
            case 7 -> applyParticleEmotion(16); // haha
            case 9 -> applyParticleEmotion(29); // blink
            case 10 -> applyParticleEmotion(18); // sigh
            default -> applyParticleEmotion(11); // find
        }
    }

    public void applySoundAtAttacker(int type, Entity target) {
    }

    /**
     * Send the legacy attack animation packet to tracking clients.
     * Type 1 has the light-cannon muzzle visual; the other attack types only
     * set the ship attack timer, matching the legacy animation contract.
     */
    public void applyParticleAtAttacker(int type, Entity target, Entity target2) {
        if (target != null && !this.level().isClientSide()) {
            if (type == 1 && target2 != null) {
                double lookX = target2.getX() - this.getX();
                double lookY = target2.getY() - this.getY();
                double lookZ = target2.getZ() - this.getZ();
                double lookLength = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
                if (lookLength > 1.0E-7D) {
                    lookX /= lookLength;
                    lookY /= lookLength;
                    lookZ /= lookLength;
                } else {
                    Vec3 look = this.getLookAngle();
                    lookX = look.x;
                    lookY = look.y;
                    lookZ = look.z;
                }
                ModNetworking.sendToAllTracking(
                        new S2CSpawnParticle(this, 6,
                                this.getX(), this.getY(), this.getZ(),
                                lookX, lookY, lookZ, true),
                        this);
            } else {
                ModNetworking.sendToAllTracking(
                        new S2CSpawnParticle(this, 0, true), this);
            }
        }
    }

    public SoundEvent getCustomSound(int type, BasicEntityShipHostile ship) {
        return SoundEvents.GENERIC_HURT;
    }

    // ========== Missile Data ==========

    public void resetMissileData() {
        this.MissileData = new MissileData[5];
        for (int i = 0; i < 5; i++) {
            this.MissileData[i] = new MissileData();
        }
    }

    // ========== Network Sync ==========

    /**
     * Send sync packet to tracking clients.
     * type: 0=emotion, 1=motion, 2=rotation, 3=posrot
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
                case 3:
                    ModNetworking.sendToAllTracking(
                            S2CEntitySyncPacket.syncPosRot(this), this);
                    break;
            }
        }
    }

    // ========== IShipNavigator Implementation ==========

    @Override
    public PathNavigation getNavigation() {
        return this.shipNavigator;
    }

    @Override
    public ShipMoveControl getShipMoveControl() {
        return (ShipMoveControl) this.moveControl;
    }

    @Override
    public boolean canFly() {
        return false;
    }

    @Override
    public boolean isJumping() {
        return this.jumping;
    }

    @Override
    public float getMoveSpeed() {
        if (this.shipAttrs == null)
            return 0.3F;
        return this.shipAttrs.getMoveSpeed(false);
    }

    @Override
    public float getJumpSpeed() {
        return 1F;
    }

    // ========== IShipFlags Implementation ==========

    public int getStateMinor(int id) {
        return StateMinor[id];
    }

    public void setStateMinor(int id, int par1) {
        StateMinor[id] = par1;
    }

    public boolean getStateFlag(int flag) {
        if (flag == ID.F.NoFuel && (this.isDeadOrDying() || this.deathTime > 0))
            return true;
        return StateFlag[flag];
    }

    public void setStateFlag(int id, boolean par1) {
        this.StateFlag[id] = par1;
    }

    public void setUpdateFlag(int id, boolean par1) {
        UpdateFlag[id] = par1;
    }

    public boolean getUpdateFlag(int id) {
        return UpdateFlag[id];
    }

    // ========== IShipEmotion Implementation ==========

    public int getStateEmotion(int id) {
        return StateEmotion[id];
    }

    public void setStateEmotion(int id, int value, boolean sync) {
        StateEmotion[id] = value;
        if (sync && !this.level().isClientSide()) {
            this.sendSyncPacket(0);
        }
    }

    public int getStateTimer(int id) {
        return StateTimer[id];
    }

    public void setStateTimer(int id, int value) {
        StateTimer[id] = value;
    }

    public int getFaceTick() {
        return this.StateTimer[ID.T.FaceTime];
    }

    public void setFaceTick(int par1) {
        this.StateTimer[ID.T.FaceTime] = par1;
    }

    public int getHeadTiltTick() {
        return this.StateTimer[ID.T.HeadTilt];
    }

    public void setHeadTiltTick(int par1) {
        this.StateTimer[ID.T.HeadTilt] = par1;
    }

    public int getAttackTick() {
        return this.StateTimer[ID.T.AttackTime];
    }

    public void setAttackTick(int par1) {
        this.StateTimer[ID.T.AttackTime] = par1;
    }

    public int getAttackTick2() {
        return this.StateTimer[ID.T.AttackTime2];
    }

    public void setAttackTick2(int par1) {
        this.StateTimer[ID.T.AttackTime2] = par1;
    }

    public int getDeathTick() {
        return this.deathTime;
    }

    public void setDeathTick(int par1) {
        this.deathTime = par1;
    }

    public float getModelRotate(int par1) {
        if (par1 >= 0 && par1 < rotateAngle.length)
            return rotateAngle[par1];
        return rotateAngle[0];
    }

    public void setModelRotate(int par1, float par2) {
        if (par1 >= 0 && par1 < rotateAngle.length)
            rotateAngle[par1] = par2;
    }

    public int getTickExisted() {
        return this.tickCount;
    }

    public float getSwingTime(float partialTick) {
        return this.getAttackAnim(partialTick);
    }

    public boolean getIsRiding() {
        return this.isPassenger();
    }

    public boolean getIsSprinting() {
        return this.isSprinting() || this.walkAnimation.speed() > 0.9F;
    }

    public boolean getIsSitting() {
        return false;
    }

    public boolean getIsSneaking() {
        return this.isShiftKeyDown();
    }

    public boolean getIsLeashed() {
        return false;
    }

    public void setEntitySit(boolean sit) {
    }

    public int getRidingState() {
        return this.ridingState;
    }

    public void setRidingState(int state) {
        this.ridingState = state;
    }

    public int getScaleLevel() {
        return this.scaleLevel;
    }

    public void setScaleLevel(int par1) {
        this.scaleLevel = Mth.clamp(par1, 0, 3);
        setSizeWithScaleLevel();

        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTrackingAndSelf(S2CEntitySyncPacket.syncScale(this, this.scaleLevel), this);
        }
    }

    public RandomSource getRand() {
        return this.random;
    }

    public double getShipDepth(int type) {
        return this.ShipDepth;
    }

    // ========== IShipOwner Implementation ==========

    public int getPlayerUID() {
        return -1;
    }

    public void setPlayerUID(int par1) {
    }

    public Entity getHostEntity() {
        return this;
    }

    // ========== IShipAttrs Implementation ==========

    public Attrs getAttrs() {
        return this.shipAttrs;
    }

    public void setAttrs(Attrs data) {
        if (data instanceof AttrsAdv) {
            this.shipAttrs = (AttrsAdv) data;
        }
    }

    // ========== IShipAttackBase Implementation ==========

    public Entity getEntityTarget() {
        return this.getTarget();
    }

    public void setEntityTarget(Entity target) {
        this.setTarget((LivingEntity) target);
    }

    public Entity getEntityRevengeTarget() {
        return this.rvgTarget;
    }

    public void setEntityRevengeTarget(Entity target) {
        this.rvgTarget = target;
    }

    public int getEntityRevengeTime() {
        return this.revengeTime;
    }

    public void setEntityRevengeTime() {
        this.revengeTime = this.tickCount;
    }

    public int getDamageType() {
        return getStateMinor(ID.M.DamageType);
    }

    public boolean getAttackType(int par1) {
        return this.getStateFlag(par1);
    }

    public int getAmmoLight() {
        return this.StateMinor[ID.M.NumAmmoLight];
    }

    public void setAmmoLight(int num) {
        this.StateMinor[ID.M.NumAmmoLight] = num;
    }

    public int getAmmoHeavy() {
        return this.StateMinor[ID.M.NumAmmoHeavy];
    }

    public void setAmmoHeavy(int num) {
        this.StateMinor[ID.M.NumAmmoHeavy] = num;
    }

    public boolean hasAmmoLight() {
        return true;
    }

    public boolean hasAmmoHeavy() {
        return true;
    }

    public boolean useAmmoLight() {
        return true;
    }

    public boolean useAmmoHeavy() {
        return true;
    }

    public int getLevel() {
        return StateMinor[ID.M.ShipLevel];
    }

    public HashMap<Integer, Integer> getBuffMap() {
        return this.BuffMap;
    }

    public void setBuffMap(HashMap<Integer, Integer> map) {
        this.BuffMap = map;
    }

    public HashMap<Integer, int[]> getAttackEffectMap() {
        return this.AttackEffectMap;
    }

    public void setAttackEffectMap(HashMap<Integer, int[]> map) {
        this.AttackEffectMap = map;
    }

    public MissileData getMissileData(int type) {
        if (type >= 0 && type < this.MissileData.length) {
            return this.MissileData[type];
        }
        return new MissileData();
    }

    public void setMissileData(int type, MissileData data) {
        if (type >= 0 && type < this.MissileData.length) {
            this.MissileData[type] = data;
        }
    }

    // ========== IShipFloating Implementation ==========

    @Override
    public double getShipDepth() {
        return this.ShipDepth;
    }

    @Override
    public void setShipDepth(double par1) {
        this.ShipDepth = par1;
    }

    @Override
    public double getShipFloatingDepth() {
        return this.ShipFloatingDepth;
    }

    @Override
    public void setShipFloatingDepth(double par1) {
        this.ShipFloatingDepth = par1;
    }

    // ========== IShipCustomTexture Implementation ==========

    @Override
    public int getTextureID() {
        return this.textureID;
    }

    @Override
    public void setTextureID(int id) {
        this.textureID = id;
    }

    // ========== Convenience ==========

    public byte getShipType() {
        return (byte) getStateMinor(ID.M.ShipType);
    }

    public short getShipClass() {
        return (short) getStateMinor(ID.M.ShipClass);
    }

    public int getAmmoConsumption() {
        return getStateMinor(ID.M.AmmoCon);
    }

    public void setAmmoConsumption(int par1) {
        this.setStateMinor(ID.M.AmmoCon, par1);
    }

    public int getCombatTick() {
        return this.StateTimer[ID.T.LastCombat];
    }

    public void setCombatTick(int par1) {
        this.StateTimer[ID.T.LastCombat] = par1;
    }

    public float[] getModelPos() {
        return ModelPos;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ========== Equip Type (for AI) ==========

    public int getEquipType() {
        return 1;
    }

    // ========== Searchlight stubs ==========

    public void updateSearchlight() {
        if (this.getStateMinor(ID.M.LevelSearchlight) <= 0)
            return;
        if (this.getStateFlag(ID.F.NoFuel))
            return;
        if (!this.isAlive())
            return;

        // [PORT] 1.10.2 -> 1.20.1: keep night-time searchlight behavior using dayTime
        // window.
        int time = (int) (this.level().getDayTime() % 24000L);
        if (time < 12500 || time > 23500)
            return;

        BlockPos pos = this.blockPosition().above(2);
        BlockHelper.placeSearchlight(this.level(), pos);
    }
}
