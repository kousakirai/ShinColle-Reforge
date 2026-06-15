package com.lulan.shincolle.entity;

import com.lulan.shincolle.ai.*;
import com.lulan.shincolle.ai.path.ShipMoveControl;
import com.lulan.shincolle.ai.path.ShipNavigation;
import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.capability.CapaShipSavedValues;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.client.gui.inventory.ContainerShipInventory;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.other.BasicEntityItem;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityShipFishingHook;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.item.IShipEffectItem;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.network.*;
import com.lulan.shincolle.reference.Enums.EnumEquipEffectSP;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.server.ServerDataManager;
import com.lulan.shincolle.utility.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * SHIP DATA
 * Core abstract class for all friendly ship entities.
 * Ported from 1.10.2 EntityTameable to 1.20.1 TamableAnimal.`
 */
public abstract class BasicEntityShip extends TamableAnimal
        implements IShipCannonAttack, IShipGuardian, IShipFloating, IShipNavigator, IShipCustomTexture, MenuProvider {

    // ========== Fields ==========

    // misc flags
    public static boolean stopAI = false;
    /**
     * owner name
     */
    public String ownerName;
    /**
     * unit names
     */
    public ArrayList<String> unitNames;
    // fishing hook
    public EntityShipFishingHook fishHook;
    protected CapaShipInventory itemHandler;
    protected LivingEntity aiTarget;
    protected Entity guardedEntity;
    protected Entity atkTarget;
    protected Entity rvgTarget;
    // AI calculation
    protected double ShipDepth;
    protected double ShipFloatingDepth;
    protected double ShipPrevX;
    protected double ShipPrevY;
    protected double ShipPrevZ;
    /**
     * ship attributes: hp, def, atk, ...
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
     * BodyHeightRange
     */
    protected byte[] BodyHeightStand;
    protected byte[] BodyHeightSit;
    /**
     * ModelPos: posX, posY, posZ, scale (in ship inventory)
     */
    protected float[] ModelPos;
    /**
     * Update Flag, index by {@link ID.FlagUpdate}
     */
    protected boolean[] UpdateFlag;
    /**
     * waypoints: 0:last wp
     */
    protected BlockPos[] waypoints;
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
    // scale level
    protected int scaleLevel;
    // dynamic entity size for hitbox scaling
    protected float entityWidth = 0.6F;
    protected float entityHeight = 1.875F;
    // initialization
    private boolean initAI, initWaitAI;
    private boolean isUpdated;
    private int updateTime = 16;
    protected BasicEntityShip(EntityType<? extends BasicEntityShip> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.invulnerableTime = 2;
        this.ownerName = "";
        this.unitNames = new ArrayList<>();

        // init value
        this.itemHandler = new CapaShipInventory(CapaShipInventory.SlotMax, this);
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
        this.BodyHeightStand = new byte[]{92, 78, 73, 58, 47, 37};
        this.BodyHeightSit = new byte[]{64, 49, 44, 29, 23, 12};
        this.ModelPos = new float[]{0F, 0F, 0F, 50F};
        this.waypoints = new BlockPos[]{BlockPos.ZERO};
        this.BuffMap = new HashMap<>();
        this.AttackEffectMap = new HashMap<>();
        this.resetMissileData();

        // AI
        this.ShipDepth = 0D;
        this.ShipFloatingDepth = 0D;
        this.ShipPrevX = getX();
        this.ShipPrevY = getY();
        this.ShipPrevZ = getZ();
        this.setMaxUpStep(1.0F);
        this.moveControl = new ShipMoveControl(this, 60F, 1.5F);

        // render
        this.rotateAngle = new float[3];
        this.textureID = 0;
        this.ridingState = 0;
        this.scaleLevel = 0;

        // init
        this.initAI = false;
        this.initWaitAI = false;
        this.isUpdated = false;
    }

    // ========== Constructor ==========

    public static AttributeSupplier.Builder createShipAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        System.out.println("ShipNavigation created");
        return new ShipNavigation(this, level);
    }

    // ========== Static Attribute Builder (1.20.1) ==========

    /**
     * init values, called at the end of subclass constructor
     */
    protected void postInit() {
        this.createNavigation(this.level());
        // [PORT] 1.10.2 -> 1.20.1: restore legacy ship turn-rate cap (60 deg/tick).
        this.shipAttrs = new AttrsAdv(this.getShipClass());
    }

    // ========== Abstract Methods ==========

    /**
     * 1:cannon only, 2:both, 3:aircraft only
     */
    public abstract int getEquipType();

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

    // ========== Invulnerability ==========

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Always allow void damage
        if (source == damageSources().fellOutOfWorld())
            return false;
        // Check immunity timer
        if (getStateTimer(ID.T.ImmuneTime) > 0)
            return true;
        return super.isInvulnerableTo(source);
    }

    // ========== Visual Effects ==========

    @Override
    public boolean isOnFire() {
        return this.getStateEmotion(ID.S.HPState) == ID.HPState.HEAVY;
    }

    // ========== Breeding (N/A) ==========

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    // ========== Sound Methods ==========

    @Override
    protected float getSoundVolume() {
        return (float) ConfigHandler.volumeShip();
    }

    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        // Ships are normally silent; custom ambient sounds are played via
        // playAmbientSound override
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.getCustomSound(2, this.getShipClass());
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.getCustomSound(3, this.getShipClass());
    }

    @Override
    public void playAmbientSound() {
        // 30% chance to play sound; silent if no fuel
        if (this.getStateFlag(ID.F.NoFuel) || this.random.nextInt(10) > 3)
            return;

        SoundEvent sound;

        // married ship: 20% chance to play marriage sound instead of ambient
        if (this.getStateFlag(ID.F.IsMarried)) {
            if (this.random.nextInt(5) == 0) {
                sound = getCustomSound(4, this);
            } else {
                sound = ModSounds.getCustomSound(0, this.getShipClass());
            }
        }
        // normal ship
        else {
            sound = ModSounds.getCustomSound(0, this.getShipClass());
        }

        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    // ========== Movement ==========

    @Override
    public void travel(Vec3 travelVec) {
        if (this.isInWater()) {
            EntityHelper.moveEntityInFluid(this, travelVec);
            // Apply position change from deltaMovement; don't call super.travel()
            // to avoid vanilla water buoyancy stacking with custom fluid motion
            this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            super.travel(travelVec);
        }
    }

    @Override
    public boolean canBeLeashed(Player player) {
        if (!player.level().isClientSide()) {
            return this.getPlayerUID() > 0 && this.isOwnedBy(player);
        }
        return true;
    }

    public boolean shouldPassengerFaceForward() {
        return false;
    }

    // ========== Name Tag ==========

    @Override
    public void setCustomName(@Nullable Component name) {
        // Allow programmatic name setting (from NBT load, spawn egg, commands, etc.)
        // Vanilla name tag usage is blocked in mobInteract instead
        super.setCustomName(name);
    }

    // ========== AI Setup ==========

    protected void setAIList() {
        // high priority
        this.goalSelector.addGoal(0, new ShipSkillAttackGoal(this));
        this.goalSelector.addGoal(1, new ShipSitGoal(this));
        this.goalSelector.addGoal(2, new ShipFleeGoal(this));
        this.goalSelector.addGoal(3, new ShipGuardingGoal(this));
        this.goalSelector.addGoal(4, new ShipFollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new ShipOpenDoorGoal(this, true));
        this.goalSelector.addGoal(11, new ShipRangeAttackGoal(this));

        // melee attack
        if (getStateFlag(ID.F.UseMelee)) {
            this.goalSelector.addGoal(15, new ShipAttackOnCollideGoal(this, 1.0D));
        }

        // idle AI
        this.goalSelector.addGoal(23, new ShipFloatingGoal(this));
        this.goalSelector.addGoal(24, new ShipWanderGoal(this, 10, 5, 0.8D));
        // Replace ShipWatchClosestGoal -> LookAtPlayerGoal
        this.goalSelector.addGoal(25, new LookAtPlayerGoal(this, Player.class, 4.0F, 0.06F) {
            @Override
            public boolean canUse() {
                if (BasicEntityShip.this.getStateFlag(ID.F.NoFuel)) return false;
                return super.canUse();
            }
        });
        // Replace: ShipLookIdleGoal -> RandomLookAroundGoal
        this.goalSelector.addGoal(26, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                if (BasicEntityShip.this.getStateFlag(ID.F.NoFuel)) return false;
                return super.canUse();
            }
        });
    }

    public void setAITargetList() {
        if (this.getStateFlag(ID.F.PassiveAI)) {
            // passive: only revenge targeting
            this.targetSelector.addGoal(1, new ShipRevengeTargetGoal(this));
        } else {
            // active: revenge + range targeting
            this.targetSelector.addGoal(1, new ShipRevengeTargetGoal(this));
            this.targetSelector.addGoal(5, new ShipRangeTargetGoal(this));
        }
    }

    protected void clearAITasks() {
        this.goalSelector.removeAllGoals(goal -> true);
    }

    protected void clearAITargetTasks() {
        this.setTarget(null);
        this.setEntityTarget(null);
        this.targetSelector.removeAllGoals(goal -> true);
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        // save ship attributes
        CapaShipSavedValues.saveNBTData(nbt, this);

        // save ship inventory
        nbt.put(CapaShipInventory.InvName, itemHandler.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        // load ship attributes
        CapaShipSavedValues.loadNBTData(nbt, this);

        // load ship inventory
        if (nbt.contains(CapaShipInventory.InvName)) {
            itemHandler.deserializeNBT(nbt.getCompound(CapaShipInventory.InvName));
        }
    }

    // ========== Tick / aiStep (Update Loop) ==========

    @Override
    public void tick() {
        super.tick();
        if (stopAI)
            return;

        // update arm swing
        updateSwingTime();

        // client side
        if (this.level().isClientSide()) {
            // water splash particles when moving in water
            if (this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.01) {
                ParticleHelper.spawnSprayParticle(this.level(), this.getX(), this.getY() + 0.1, this.getZ(), 2);
            }
            // HP state particle effects (smoke when damaged) - every 16 ticks
            if ((this.tickCount & 15) == 0) {
                int hpState = this.getStateEmotion(ID.S.HPState);
                if (hpState >= ID.HPState.MINOR) {
                    // MINOR (蟆冗ｴ): light smoke only, 1 particle
                    this.level().addParticle(ParticleTypes.SMOKE,
                            this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                            this.getY() + this.getBbHeight() * 0.5 + this.random.nextDouble() * 0.5,
                            this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                            0, 0.02, 0);
                    if (hpState >= ID.HPState.MODERATE) {
                        // MODERATE (荳ｭ遐ｴ): additional fire particle
                        this.level().addParticle(ParticleTypes.FLAME,
                                this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                                this.getY() + this.random.nextDouble() * this.getBbHeight() * 0.5,
                                this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                                0, 0.01, 0);
                    }
                    if (hpState >= ID.HPState.HEAVY) {
                        // HEAVY (螟ｧ遐ｴ): heavy smoke + fire (isOnFire overlay handled separately)
                        this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                                this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                                this.getY() + this.random.nextDouble() * this.getBbHeight(),
                                this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                                0, 0.03, 0);
                    }
                }
            }

            // Pointer visualization (legacy behavior): owner holding pointer mode 0-2
            // periodically shows team circles on this ship and guard target.
            updateClientPointerIndicator();
        }
    }

    private void updateClientPointerIndicator() {
        if ((this.tickCount & 31) != 0) {
            return;
        }

        Player clientPlayer = ClientRuntimeHelper.getClientPlayer();
        if (clientPlayer == null || clientPlayer.level() != this.level() || !this.isOwnedBy(clientPlayer)) {
            return;
        }

        ItemStack pointer = getClientPointerInUse(clientPlayer);
        int mode = PointerItem.getMode(pointer);
        if (pointer.isEmpty() || mode > PointerItem.MODE_FORMATION) {
            return;
        }

        CapaTeitoku capa = clientPlayer.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null);


        int teamId = capa.getSelectTeam();
        boolean isSelected = false;
        for (int i = 0; i < CapaTeitoku.SLOT_NUM; i++) {
            if (capa.getTeamSID(teamId, i) == this.getId()) {
                isSelected = true;
                break;
            }
        }

        int circleType;
        if (isSelected) {
            circleType = switch (mode) {
                case PointerItem.MODE_GROUP -> 2;
                case PointerItem.MODE_FORMATION -> 3;
                default -> 1;
            };
        } else {
            circleType = mode == PointerItem.MODE_FORMATION ? 3 : 0;
        }

        ParticleHelper.spawnTeamCircle(this, circleType);

        if (!this.getStateFlag(ID.F.CanFollow)) {
            updateClientGuardedEntity();
            Entity guarded = this.getGuardedEntity();
            if (guarded != null && guarded.level() == this.level()) {
                ParticleHelper.spawnTeamCircle(guarded, 6);
            }
        }
    }

    private void updateClientGuardedEntity() {
        int guardedId = this.getStateMinor(ID.M.GuardID);
        if (guardedId > 0) {
            this.setGuardedEntity(this.level().getEntity(guardedId));
        } else {
            this.setGuardedEntity(null);
        }
    }

    private ItemStack getClientPointerInUse(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == ModItems.POINTER.get()) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() == ModItems.POINTER.get()) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private ItemStack getPointerInUse(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == ModItems.POINTER.get()) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() == ModItems.POINTER.get()) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void aiStep() {
        if (this.tickCount == 5) {
            this.random.setSeed(((long) this.getShipUID() << 4) + System.currentTimeMillis());
            this.initAI = false;
            this.initWaitAI = false;
        }

        // server side
        if (!level().isClientSide()) {
            EntityHelper.updateShipNavigator(this);
            TargetHelper.updateTarget(this);

            super.aiStep();

            // timer ticking
            updateServerTimer();
            updateBothSideTimer();

            // register/update ship ID and owner ID in ServerDataManager
            updateShipCacheData(false);

            // Timekeeping: play hourly voice announcements
            if (ConfigHandler.canTimekeeping() && this.getStateFlag(ID.F.TimeKeeper) && this.isAlive()) {
                playTimeSound();
            }

            // check every 8 ticks
            if ((tickCount & 7) == 0) {
                // reset AI and sync once
                if (!this.initAI && tickCount > 10) {
                    setStateFlag(ID.F.CanDrop, true);
                    // check fuel state first (sets NoFuel flag but won't clear
                    // goals since none are registered yet)
                    decrGrudgeNum(0);
                    // then register goals — they stay registered because
                    // updateFuelStateByAITaskPresence already ran with empty selectors
                    clearAITasks();
                    clearAITargetTasks();
                    setAIList();
                    setAITargetList();
                    updateChunkLoader();
                    this.initAI = true;
                }

                // formation buff fast update: recalc when flag is set
                if (this.getUpdateFlag(ID.FlagUpdate.FormationBuff)) {
                    this.calcShipAttributes(16, true);
                    this.setUpdateFlag(ID.FlagUpdate.FormationBuff, false);
                }

                // [PORT] 1.10.2 -> 1.20.1: restore periodic task update hook.
                TaskHelper.onUpdateTask(this);

                // check every 16 ticks
                if ((tickCount & 15) == 0) {
                    if (this.isAlive()) {
                        // cancel mounts if can't summon
                        if (this.hasShipMounts() && this.canSummonMounts()) {
                            if (this.isPassenger() && this.getVehicle() instanceof BasicEntityMount) {
                                this.stopRiding();
                            }
                        }

                        // recalc potion attrs
                        this.calcShipAttributes(8, true);
                    }

                    // check every 32 ticks
                    if ((tickCount & 31) == 0) {
                        if (this.isAlive()) {
                            // apply periodic buff effects (regen, wither, saturation)
                            BuffHelper.applyBuffOnTicks(this);

                            // auto bucket: if missing health > threshold, use bucket
                            if ((getMaxHealth() - getHealth()) > (getMaxHealth() * 0.1F + 5F)) {
                                if (decrSupplies(7)) {
                                    this.heal(this.getMaxHealth() * 0.08F + 15F);
                                    // recover airplane for CV ships
                                    if (this instanceof BasicEntityShipCV cv) {
                                        cv.setNumAircraftLight(cv.getNumAircraftLight() + 1);
                                        cv.setNumAircraftHeavy(cv.getNumAircraftHeavy() + 1);
                                    }
                                }
                            }

                            // update searchlight block placement at night
                            updateSearchlight();

                            // update mount entity summoning
                            updateMountSummon();

                            // update chunk loader based on compass equip
                            updateChunkLoader();
                        }

                        // check every 64 ticks
                        if ((tickCount & 63) == 0) {
                            updateEmotionState();

                            // check every 128 ticks
                            if ((tickCount & 127) == 0) {
                                this.calcShipAttributes(31, false);

                                if (!this.initWaitAI && tickCount >= 128) {
                                    setUpdateFlag(ID.FlagUpdate.FormationBuff, true);
                                    this.initWaitAI = true;
                                }

                                if (this.isAlive()) {
                                    // fuel consumption based on movement distance
                                    updateConsumeItem();

                                    // auto combat ration: use when current morale level is at or worse than setting
                                    // getMoraleLevel returns 0=Excited..4=Exhausted (higher = worse)
                                    // UseCombatRation stores a morale level ID (e.g. 3=Tired)
                                    int moraleLevel = BuffHelper.getMoraleLevel(getMorale());
                                    if (moraleLevel >= getStateMinor(ID.M.UseCombatRation)
                                            && getFoodSaturation() < getFoodSaturationMax()) {
                                        useCombatRation();
                                    }

                                    // update morale
                                    if (!getStateFlag(ID.F.NoFuel)) {
                                        updateMorale();
                                    }
                                }

                                // Periodic sync every 128 ticks
                                S2CEntitySyncPacket pkt = S2CEntitySyncPacket.syncAllMisc(this);
                                ModNetworking.sendToAllTracking(pkt, this);

                                // check every 256 ticks
                                if ((this.tickCount & 255) == 0) {
                                    if (this.isAlive()) {
                                        // HP regen
                                        if (this.getHealth() < this.getMaxHealth()) {
                                            this.heal(this.getMaxHealth() * 0.03F + 1F);
                                        }

                                        // idle emotes
                                        if (!getStateFlag(ID.F.NoFuel)) {
                                            applyEmotesReaction(4);
                                        }
                                    }

                                    // food saturation--
                                    int f = this.getFoodSaturation();
                                    if (f > 0) {
                                        this.setFoodSaturation(--f);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // client side
        else {
            super.aiStep();

            // client-side timers (attack animation, emotion3 timer)
            updateClientTimer();

            // both-side timers (mount skill cooldowns)
            updateBothSideTimer();

            // body rotation from movement direction
            updateClientBodyRotate();
        }

        // both sides: prevent suffocation underwater
        if ((this.tickCount & 127) == 0) {
            this.setAirSupply(300);
        }
    }

    /**
     * update server side timers
     */
    protected void updateServerTimer() {
        // decrement timers
        if (StateTimer[ID.T.ImmuneTime] > 0)
            StateTimer[ID.T.ImmuneTime]--;
        if (StateTimer[ID.T.CrandDelay] > 0)
            StateTimer[ID.T.CrandDelay]--;
        if (StateTimer[ID.T.SoundTime] > 0)
            StateTimer[ID.T.SoundTime]--;
        if (StateTimer[ID.T.EmoteDelay] > 0)
            StateTimer[ID.T.EmoteDelay]--;
        if (StateTimer[ID.T.Emotion3Time] > 0)
            StateTimer[ID.T.Emotion3Time]--;
    }

    // ========== Attribute Calculation ==========

    /**
     * Calc ship attributes.
     * flag bits: A(1)=raw, B(2)=equips, C(4)=morale, D(8)=potion, E(16)=formation
     */
    public void calcShipAttributes(int flag, boolean sync) {
        if (this.shipAttrs == null)
            this.shipAttrs = new AttrsAdv(this.getShipClass());

        if (!this.level().isClientSide()) {
            // recalc raw attrs
            if ((flag & 1) == 1) {
                BuffHelper.updateAttrsRaw(this.shipAttrs, this.getShipClass(), this.getLevel());
                this.calcShipAttributesAddRaw();
                this.setUpdateFlag(ID.FlagUpdate.AttrsRaw, true);
                this.setUpdateFlag(ID.FlagUpdate.AttrsBonus, true);
            }
            // recalc equips
            if ((flag & 2) == 2) {
                // Equipment attribute bonuses are applied via calcShipAttributesAddEquip(),
                // which subclasses override to add equip-specific stat modifiers.
                // EquipCalc handles equipment roll/construction only, not runtime attr updates.
                this.calcShipAttributesAddEquip();
                this.setUpdateFlag(ID.FlagUpdate.AttrsEquip, true);
            }
            // recalc morale buff
            if ((flag & 4) == 4) {
                BuffHelper.updateBuffMorale(this.shipAttrs, this.getMorale());
                this.setUpdateFlag(ID.FlagUpdate.AttrsMorale, true);
            }
            // recalc potion buff
            if ((flag & 8) == 8) {
                BuffHelper.updateBuffPotion(this);
                this.setUpdateFlag(ID.FlagUpdate.AttrsPotion, true);
            }
            // recalc formation buff
            if ((flag & 16) == 16) {
                BuffHelper.updateBuffFormation(this.shipAttrs,
                        getStateMinor(ID.M.FormatType), getStateMinor(ID.M.FormatPos));
                this.setUpdateFlag(ID.FlagUpdate.AttrsFormation, true);
            }
            BuffHelper.applyBuffOnAttrs(this);
            this.calcShipAttributesAdd();
            this.setUpdateFlag(ID.FlagUpdate.AttrsBuffed, true);
        }

        // set attrs to MC entity attributes (server-side only)
        if (!this.level().isClientSide()) {
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

            // sync to client if requested
            if (sync) {
                this.sendSyncPacketAttrs();
            }
        }
    }

    /**
     * calc additional attributes - override in subclass
     */
    public void calcShipAttributesAdd() {
    }

    public void calcShipAttributesAddRaw() {
    }

    public void calcShipAttributesAddEquip() {
        if (this.shipAttrs == null)
            return;

        // reset equip-related values
        this.shipAttrs.resetAttrsEquip();
        this.resetMissileData();
        this.calcShipAttributesAddEffect();
        this.setStateMinor(ID.M.DrumState, 0);
        this.setStateMinor(ID.M.LevelChunkLoader, 0);
        this.setStateMinor(ID.M.LevelFlare, 0);
        this.setStateMinor(ID.M.LevelSearchlight, 0);

        // iterate over equip slots (0-5)
        CapaShipInventory inv = this.getCapaShipInventory();
        if (inv == null)
            return;

        for (int i = 0; i < ContainerShipInventory.EQUIP_SLOTS; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BasicEquip equipItem))
                continue;

            int meta = BasicEquip.getEquipMeta(stack);
            int equipID = equipItem.getEquipID(meta);

            // look up stat modifiers from tables
            float[] itemRaw = Values.EquipAttrsMain.get(equipID);
            int[] itemMisc = Values.EquipAttrsMisc.get(equipID);

            if (itemRaw == null || itemMisc == null)
                continue;

            // check equip type compatibility
            // EquipMisc.EQUIP_TYPE: 0=none, 1=cannon, 2=misc, 3=aircraft
            // Ship getEquipType(): 1=cannon only, 2=both, 3=aircraft only
            if (this.getEquipType() != 2 && itemMisc[ID.EquipMisc.EQUIP_TYPE] != 2) {
                if (this.getEquipType() != itemMisc[ID.EquipMisc.EQUIP_TYPE])
                    continue;
            }

            // apply enchant effect to raw stats
            float[] enchant = EnchantHelper.calcEnchantEffect(stack);
            float[] equipStats = EquipCalc.calcEquipStatWithEnchant(
                    itemMisc[ID.EquipMisc.EQUIP_TYPE], itemRaw, enchant);

            // add modifiers to ship's equip attrs
            float[] attrsEquip = this.shipAttrs.getAttrsEquip();
            for (int j = 0; j < Attrs.AttrsLength; j++) {
                attrsEquip[j] += equipStats[j];
            }

            // apply special equip effects (drum, compass, flare, searchlight)
            EnumEquipEffectSP effect = equipItem.getSpecialEffect(stack);
            switch (effect) {
                case DRUM:
                case DRUM_LIQUID:
                case DRUM_EU:
                    this.setStateMinor(ID.M.DrumState,
                            this.getStateMinor(ID.M.DrumState) + 1);
                    break;
                case COMPASS:
                    this.setStateMinor(ID.M.LevelChunkLoader,
                            this.getStateMinor(ID.M.LevelChunkLoader) + 1);
                    break;
                case FLARE:
                    this.setStateMinor(ID.M.LevelFlare,
                            this.getStateMinor(ID.M.LevelFlare) + 1);
                    break;
                case SEARCHLIGHT:
                    this.setStateMinor(ID.M.LevelSearchlight,
                            this.getStateMinor(ID.M.LevelSearchlight) + 1);
                    break;
                default:
                    break;
            }

            // apply IShipEffectItem effects (missile type, move type, speed)
            if (stack.getItem() instanceof IShipEffectItem eitem) {

                // apply missile type
                int mtype = eitem.getMissileType(meta);
                if (mtype > 0) {
                    for (int m = 0; m < 5; m++) {
                        this.getMissileData(m).type = mtype;
                    }
                }

                // apply missile move type
                mtype = eitem.getMissileMoveType(meta);
                if (mtype >= 0) {
                    for (int m = 0; m < 5; m++) {
                        this.getMissileData(m).movetype = mtype;
                    }
                }

                // apply missile speed
                int speedLv = eitem.getMissileSpeedLevel(meta);
                if (speedLv != 0) {
                    float addSpeed = speedLv * 0.025F;
                    float addAccY = speedLv * 0.004F;
                    for (int m = 0; m < 5; m++) {
                        this.getMissileData(m).vel0 += addSpeed;
                        this.getMissileData(m).accY1 += addAccY;
                        this.getMissileData(m).accY2 = this.getMissileData(m).accY1;
                    }
                }
            }
        }

        // apply config scale to equip attrs
        float[] equip = this.shipAttrs.getAttrsEquip();
        equip[ID.Attrs.HP] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.HP];
        equip[ID.Attrs.ATK_L] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.ATK];
        equip[ID.Attrs.ATK_H] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.ATK];
        equip[ID.Attrs.ATK_AL] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.ATK];
        equip[ID.Attrs.ATK_AH] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.ATK];
        equip[ID.Attrs.DEF] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.DEF];
        equip[ID.Attrs.SPD] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.SPD];
        equip[ID.Attrs.MOV] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.MOV];
        equip[ID.Attrs.HIT] *= (float) ConfigHandler.scaleShip[ID.AttrsBase.HIT];
    }

    /**
     * reset attack effect map
     */
    public void calcShipAttributesAddEffect() {
        this.AttackEffectMap = new HashMap<>();
    }

    // ========== Level / Experience ==========

    public void setExpNext() {
        int expMod = ConfigHandler.expModifier();
        StateMinor[ID.M.ExpNext] = StateMinor[ID.M.ShipLevel] * expMod + expMod;
    }

    public void addShipExp(int exp) {
        int capLevel = getStateFlag(ID.F.IsMarried) ? 150 : 100;

        exp = (int) ((float) exp * this.shipAttrs.getAttrsBuffed(ID.Attrs.XP));

        if (StateMinor[ID.M.ShipLevel] != capLevel && StateMinor[ID.M.ShipLevel] < 150) {
            StateMinor[ID.M.ExpCurrent] += exp;
            if (StateMinor[ID.M.ExpCurrent] >= StateMinor[ID.M.ExpNext]) {
                // level up sound
                if (this.random.nextInt(4) == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 0.75F, 1F);
                }
                StateMinor[ID.M.ExpCurrent] -= StateMinor[ID.M.ExpNext];
                int expMod = ConfigHandler.expModifier();
                StateMinor[ID.M.ExpNext] = (StateMinor[ID.M.ShipLevel] + 2) * expMod;
                setShipLevel(++StateMinor[ID.M.ShipLevel], true);
            }
        }
    }

    public void setShipLevel(int par1, boolean update) {
        if (par1 < 151) {
            StateMinor[ID.M.ShipLevel] = par1;
        }
        if (update) {
            this.calcShipAttributes(31, true);
            this.setHealth(this.getMaxHealth());
        }
    }

    // ========== Combat Methods ==========

    @Override
    public boolean doHurtTarget(Entity target) {
        float atk = getAttackBaseDamage(0, target);
        addShipExp(ConfigHandler.expGain[0]);
        decrMorale(0);
        setCombatTick(this.tickCount);

        boolean isTargetHurt = target.hurt(this.damageSources().mobAttack(this), atk);

        if (isTargetHurt) {
            applyEmotesReaction(3);
        }

        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (!decrAmmoNum(0, this.getAmmoConsumption()))
            return false;

        // consume resources first (always consumed even on miss)
        addShipExp(ConfigHandler.expGain[1]);
        decrGrudgeNum(ConfigHandler.consumeGrudgeAction[0]);
        decrMorale(1);
        setCombatTick(this.tickCount);

        float atk = getAttackBaseDamage(1, target);

        // calc distance for miss rate
        float dist = (float) Math.sqrt(this.distanceToSqr(target));

        // apply combat rate (miss/crit/dhit/thit)
        atk = CombatHelper.applyCombatRateToDamage(this, target, true, dist, atk);

        // play attack sound
        applySoundAtAttacker(1, target);
        applyParticleAtAttacker(1, target, target);

        // if missed (damage is 0), still return true (attack was attempted)
        if (atk <= 0F) {
            return true;
        }

        // check friendly fire - set damage to 0 but still animate
        if (CombatHelper.isFriendlyFire(this, target)) {
            atk = 0F;
        }

        // apply player damage cap
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);

        if (atk <= 0F)
            return true;

        // direct damage for light attack (projectile type)
        boolean isTargetHurt = target.hurt(this.damageSources().mobProjectile(this, this), atk);

        if (isTargetHurt) {
            applyEmotesReaction(3);

            // Apply attack effect buffs
            if (this.AttackEffectMap != null && !this.AttackEffectMap.isEmpty()) {
                BuffHelper.applyBuffOnTarget(target, this.AttackEffectMap);
            }
        }

        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (!decrAmmoNum(1, this.getAmmoConsumption()))
            return false;

        addShipExp(ConfigHandler.expGain[2]);
        decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        decrMorale(2);
        setCombatTick(this.tickCount);

        float atk = getAttackBaseDamage(2, target);

        // play attack sound
        applySoundAtAttacker(2, target);
        applyParticleAtAttacker(2, target, target);

        // target position
        float tarX = (float) target.getX();
        float tarY = (float) target.getY();
        float tarZ = (float) target.getZ();

        // spawn missile
        summonMissile(2, atk, tarX, tarY, tarZ, target.getBbHeight());

        applyEmotesReaction(3);
        return true;
    }

    /**
     * Spawn a missile entity targeting the given position
     */
    public void summonMissile(int attackType, float atk, float tarX, float tarY, float tarZ, float targetHeight) {
        float launchPos = (float) this.getY() + this.getBbHeight() * 0.5F;
        int moveType = CombatHelper.calcMissileMoveType(this, tarY, attackType);

        MissileData md = this.getMissileData(attackType);
        EntityAbyssMissile missile = new EntityAbyssMissile(
                ModEntities.ABYSS_MISSILE.get(), this.level());
        missile.initMissile(this, md.type, moveType,
                atk, 0.15F, launchPos,
                tarX, tarY + targetHeight * 0.1F, tarZ,
                140, 0.25F, md.vel0, md.accY1, md.accY2);
        this.level().addFreshEntity(missile);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide())
            return false;

        boolean checkDEF = true;

        // randomize sensitive body (10% chance)
        if (this.random.nextInt(10) == 0)
            randomSensitiveBody();

        // damage disabled: immune sources
        if (source == this.damageSources().inWall() || source == this.damageSources().starve()
                || source == this.damageSources().cactus() || source == this.damageSources().fall()) {
            return false;
        }
        // magic/dragonBreath: bypass all DEF/dodge/resist/light/repair goddess
        else if (source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.DRAGON_BREATH)) {
            // ignore tiny magic damage (< 1% max HP)
            if (amount < this.getMaxHealth() * 0.01F)
                return false;

            this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
            return super.hurt(source, amount);
        }
        // out of world: rescue teleport
        else if (source == this.damageSources().fellOutOfWorld()) {
            this.setEntitySit(false);
            this.stopRiding();
            this.teleportTo(this.getX(), 4D, this.getZ());
            return false;
        }

        // check if attacker is a potion source 竊・recalculate damage, bypass DEF
        float potionAtk = BuffHelper.getPotionDamage(this, source, amount);
        if (potionAtk > 0F) {
            amount = potionAtk;
            checkDEF = false;
        }

        // owner damage bypass: skip DEF, dodge, friendly fire, SvS, resist, light
        if (source.getEntity() instanceof Player &&
                TeamHelper.checkSameOwner(source.getEntity(), this)) {
            this.setEntitySit(false);
            this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
            return super.hurt(source, amount);
        }

        // invulnerable check (Repair Goddess immunity timer)
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        if (source.getEntity() != null) {
            Entity attacker = source.getEntity();

            // self damage immunity
            if (attacker.equals(this)) {
                this.setEntitySit(false);
                return false;
            }

            // friendly fire check (player attacker + config)
            if (attacker instanceof Player) {
                if (!ConfigHandler.friendlyFire()) {
                    return false;
                }
            }

            // dodge calculation
            float distSq = (float) this.distanceToSqr(attacker);
            if (CombatHelper.canDodge(this, distSq)) {
                return false;
            }

            // DEF calculation (skipped for potion damage)
            float reducedAtk = amount;
            if (checkDEF) {
                reducedAtk = CombatHelper.applyDamageReduceByDEF(this.random, this.shipAttrs, reducedAtk);
            }

            // ship vs ship config damage scaling (friendly ships only)
            reducedAtk = CombatHelper.applyShipVsShipDamage(reducedAtk, attacker, this);

            // resist potion modifier
            reducedAtk = BuffHelper.applyBuffOnDamageByResist(this, source, reducedAtk);

            // light / night vision modifier (ship vs ship only)
            reducedAtk = BuffHelper.applyBuffOnDamageByLight(this, source, reducedAtk);

            // minimum damage clamp
            if (reducedAtk < 1F && reducedAtk > 0F)
                reducedAtk = 1F;
            else if (reducedAtk <= 0F)
                reducedAtk = 0F;

            // cancel sitting
            this.setEntitySit(false);

            // set revenge target
            this.setEntityRevengeTarget(attacker);
            this.setEntityRevengeTime();

            // repair goddess check: if damage would kill or leave at 1 HP
            if (reducedAtk >= (this.getHealth() - 1F)) {
                if (tryRepairGoddess()) {
                    return false;
                }
            }

            // morale penalty
            decrMorale(5);
            setCombatTick(this.tickCount);

            // hit cosmetics (20% chance)
            if (this.random.nextInt(5) == 0) {
                applyEmotesReaction(2);
            }

            // set hurt face
            this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);

            return super.hurt(source, reducedAtk);
        }

        // attackerless damage (fire, lightning, etc.) with no special source: ignored
        return false;
    }

    /**
     * Check inventory for a Repair Goddess item.
     * If found, consume it, restore HP to max, and set immunity timer.
     * Uses decrSupplies(8) to search entire inventory (not just equip slots).
     *
     * @return true if a Repair Goddess was consumed and death was prevented
     */
    private boolean tryRepairGoddess() {
        if (this.decrSupplies(8)) {
            this.setHealth(this.getMaxHealth());
            this.StateTimer[ID.T.ImmuneTime] = 120;
            return true;
        }
        return false;
    }

    /**
     * get base attack damage. type: 0=melee, 1=light, 2=heavy, 3=light air, 4=heavy
     * air
     */
    public float getAttackBaseDamage(int type, Entity target) {
        if (this.shipAttrs == null)
            return 1F;

        return switch (type) {
            case 1 -> // light cannon: apply AA/ASM bonus
                    CombatHelper.modDamageByAdditionAttrs(this, target, this.shipAttrs.getAttackDamage(), 0);
            case 2 -> // heavy cannon
                    this.shipAttrs.getAttackDamageHeavy();
            case 3 -> // light aircraft
                    this.shipAttrs.getAttackDamageAir();
            case 4 -> // heavy aircraft
                    this.shipAttrs.getAttackDamageAirHeavy();
            default -> // melee
                    this.shipAttrs.getAttackDamage() * 0.125F;
        };
    }

    /**
     * decr morale value, type: 0:melee, 1:light, 2:heavy, 3:light air, 4:light
     * heavy, 5:damaged
     */
    public void decrMorale(int type) {
        switch (type) {
            case 0 -> this.addMorale(-2);
            case 1 -> this.addMorale(-4);
            case 2 -> this.addMorale(-6);
            case 3 -> this.addMorale(-6);
            case 4 -> this.addMorale(-8);
            case 5 -> this.addMorale(-5);
        }
    }

    /**
     * decr ammo, type: 0:light, 1:heavy. Refills from inventory items if needed.
     */
    public boolean decrAmmoNum(int type, int amount) {
        int ammoType = ID.M.NumAmmoLight;
        boolean useItem = !hasAmmoLight();
        float modAmmo = this.shipAttrs != null ? this.shipAttrs.getAttrsBuffed(ID.Attrs.AMMO) : 1F;

        if (type == 1) {
            ammoType = ID.M.NumAmmoHeavy;
            useItem = !hasAmmoHeavy();
        }

        // first check: try to refill from inventory if ammo low
        if (StateMinor[ammoType] <= amount || useItem) {
            int addAmmo = 0;

            if (ammoType == ID.M.NumAmmoLight) {
                // try single light ammo item first
                if (decrSupplies(0)) {
                    addAmmo = (int) (Values.N.BaseLightAmmo * modAmmo);
                }
                // try light ammo container (gives 9x)
                else if (decrSupplies(2)) {
                    addAmmo = (int) (Values.N.BaseLightAmmo * 9 * modAmmo);
                }
            } else {
                // try single heavy ammo item first
                if (decrSupplies(1)) {
                    addAmmo = (int) (Values.N.BaseHeavyAmmo * modAmmo);
                }
                // try heavy ammo container (gives 9x)
                else if (decrSupplies(3)) {
                    addAmmo = (int) (Values.N.BaseHeavyAmmo * 9 * modAmmo);
                }
            }

            // easy mode multiplier
            if (ConfigHandler.easyMode()) {
                addAmmo *= 10;
            }

            StateMinor[ammoType] += addAmmo;
        }

        // second check: consume ammo
        if (StateMinor[ammoType] < amount) {
            return false;
        } else {
            StateMinor[ammoType] -= amount;
            return true;
        }
    }

    /**
     * Load ammo from inventory proactively (called by updateConsumeItem). type:
     * 0=light, 1=heavy
     */
    public void loadAmmoFromInventory(int type) {
        // simply delegate to decrAmmoNum with amount=0 to trigger refill logic
        decrAmmoNum(type, 0);
    }

    /**
     * consume grudge with buff and item calculation
     */
    public void decrGrudgeNum(int value) {
        // get grudge magnification from buffs
        float modGrudge = 1F;
        if (this.shipAttrs != null) {
            modGrudge = this.shipAttrs.getAttrsBuffed(ID.Attrs.GRUDGE);
            if (modGrudge <= 0F)
                modGrudge = 1F;
        }

        // if grudge--, check buff: hunger (potion effect increases consumption)
        if (value > 0) {
            int level = 0;
            MobEffectInstance hunger = this.getEffect(MobEffects.HUNGER);
            if (hunger != null)
                level = hunger.getAmplifier() + 1;
            value = (int) ((float) value * (1F + level * 2F));
        }
        // if grudge++, apply grudge modifier
        else if (value < 0) {
            value = (int) ((float) value * modGrudge);
        }

        // consume grudge
        if (!getStateFlag(ID.F.NoFuel)) {
            this.addGrudge(-value);
        }

        // auto-refuel: eat grudge items from inventory when grudge reaches 0
        if (this.getGrudge() <= 0) {
            // try grudge item first
            if (decrSupplies(4)) {
                if (ConfigHandler.easyMode()) {
                    this.addGrudge((int) (Values.N.BaseGrudge * 10F * modGrudge));
                } else {
                    this.addGrudge((int) (Values.N.BaseGrudge * modGrudge));
                }
            }
            // try grudge block
            else if (decrSupplies(5)) {
                if (ConfigHandler.easyMode()) {
                    this.addGrudge((int) (Values.N.BaseGrudge * 90F * modGrudge));
                } else {
                    this.addGrudge((int) (Values.N.BaseGrudge * 9F * modGrudge));
                }
            }
        }

        // update fuel state
        if (StateMinor[ID.M.NumGrudge] <= 0) {
            if (!getStateFlag(ID.F.NoFuel)) {
                setStateFlag(ID.F.NoFuel, true);
            }
        } else {
            if (getStateFlag(ID.F.NoFuel)) {
                setStateFlag(ID.F.NoFuel, false);
            }
        }

        // [PORT] 1.10.2 -> 1.20.1: keep legacy safeguard that restores AI when
        // target tasks unexpectedly become empty while still having fuel.
        updateFuelStateByAITaskPresence();
    }

    private void updateFuelStateByAITaskPresence() {
        boolean noFuel = this.getStateFlag(ID.F.NoFuel);
        int targetTaskCount = this.targetSelector.getAvailableGoals().size();

        if (noFuel) {
            // Clear all AI when fuel runs out — ship becomes inert.
            // Water buoyancy is handled by travel()/moveEntityInFluid()
            // independently of AI goals, so the ship won't sink.
            if (targetTaskCount > 0) {
                this.setMorale(0);
                clearAITasks();
                clearAITargetTasks();
                this.setTarget(null);
                this.setEntityTarget(null);
                if (this.getVehicle() instanceof BasicEntityMount mount) {
                    mount.clearAITasks();
                }
                sendSyncPacketEmotion();
            }
        } else {
            if (targetTaskCount < 1) {
                clearAITasks();
                clearAITargetTasks();
                setAIList();
                setAITargetList();
                if (this.getVehicle() instanceof BasicEntityMount mount) {
                    mount.clearAITasks();
                    mount.setAIList();
                }
                sendSyncPacketEmotion();
            }
        }
    }

    /**
     * Find item in inventory (excluding equipment slots), respecting page
     * boundaries.
     *
     * @param target item to search for
     * @param noMeta if true, match any item of same type (ignore NBT/damage); if
     *               false, exact match
     * @return slot index or -1 if not found
     */
    public int findItemInSlot(ItemStack target, boolean noMeta) {
        int startSlot = ContainerShipInventory.EQUIP_SLOTS; // skip equipment slots (0-5)
        int maxSlot = this.itemHandler.getSlots();

        // respect inventory page size boundaries
        int pageSize = getInventoryPageSize();
        // page 2 = all slots
        maxSlot = switch (pageSize) {
            case 0 -> Math.min(maxSlot, startSlot + 18);
            case 1 -> Math.min(maxSlot, startSlot + 36);
            default -> maxSlot;
        };

        for (int i = startSlot; i < maxSlot; i++) {
            ItemStack stack = this.itemHandler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                if (noMeta) {
                    return i; // match any variant of this item type
                } else if (ItemStack.isSameItemSameTags(stack, target)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * use supplies from inventory. type: 0=ammoL, 1=ammoH, 2=ammoL container,
     * 3=ammoH container,
     * 4=grudge, 5=grudge block, 6=grudge block heavy, 7=bucket, 8=goddess
     */
    public boolean decrSupplies(int type) {
        ItemStack itemType;

        switch (type) {
            case 0: // light ammo (meta 0)
                itemType = new ItemStack(ModItems.AMMO.get());
                break;
            case 1: // heavy ammo (meta 2)
                itemType = new ItemStack(ModItems.AMMO_2.get());
                break;
            case 2: // light ammo container (meta 1)
                itemType = new ItemStack(ModItems.AMMO_1.get());
                break;
            case 3: // heavy ammo container (meta 3)
                itemType = new ItemStack(ModItems.AMMO_3.get());
                break;
            case 4:
                itemType = new ItemStack(ModItems.GRUDGE.get());
                break;
            case 5:
                itemType = new ItemStack(ModBlocks.GRUDGE.get());
                break;
            case 6:
                itemType = new ItemStack(ModBlocks.GRUDGE_HEAVY.get());
                break;
            case 7:
                itemType = new ItemStack(ModItems.BUCKET_REPAIR.get());
                break;
            case 8:
                itemType = new ItemStack(ModItems.REPAIR_GODDESS.get());
                break;
            default:
                return false;
        }

        // search item in ship inventory
        int slot = findItemInSlot(itemType, false);
        if (slot == -1)
            return false;

        // decr item stacksize
        ItemStack getItem = this.itemHandler.getStackInSlot(slot);
        if (getItem.getCount() < 1)
            return false;

        getItem.shrink(1);
        if (getItem.isEmpty()) {
            this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            // [PORT] 1.10.2 -> 1.20.1: keep legacy behavior by writing back decremented
            // stacks so inventory handlers can observe content changes.
            this.itemHandler.setStackInSlot(slot, getItem);
        }

        return true;
    }

    /**
     * auto use combat ration from inventory when morale triggers
     */
    protected void useCombatRation() {
        // search for ANY combat ration variant (noMeta=true matches any item of same
        // type)
        // try all 6 variants (COMBAT_RATION through COMBAT_RATION_5)
        int slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION.get()), true);
        if (slot < 0)
            slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION_1.get()), true);
        if (slot < 0)
            slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION_2.get()), true);
        if (slot < 0)
            slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION_3.get()), true);
        if (slot < 0)
            slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION_4.get()), true);
        if (slot < 0)
            slot = findItemInSlot(new ItemStack(ModItems.COMBAT_RATION_5.get()), true);

        if (slot >= 0) {
            ItemStack getItem = this.itemHandler.getStackInSlot(slot);

            // use full feed pipeline (morale, grudge, saturation, debuff removal, sounds)
            InteractHelper.interactFeed(this, null, getItem);

            // decr item
            getItem.shrink(1);
            if (getItem.isEmpty()) {
                this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                // [PORT] 1.10.2 -> 1.20.1: write back modified stack to preserve
                // inventory update semantics used by older implementation.
                this.itemHandler.setStackInSlot(slot, getItem);
            }
        }
    }

    /**
     * check if ship has mounts
     */
    public boolean hasShipMounts() {
        // The first bit of State emotion indicates whether this ship type supports
        // mounts
        return (this.getStateEmotion(ID.S.State) & 1) != 0;
    }

    /**
     * check if ship can summon mounts
     */
    public boolean canSummonMounts() {
        // Can summon mounts if: has fuel, is alive, and not sitting
        return this.getStateFlag(ID.F.NoFuel) || !this.isAlive() || this.isOrderedToSit();
    }

    @Override
    public boolean updateSkillAttack(Entity target) {
        return false;
    }

    public void applyAttackPostMotion(int type, Entity target, boolean isTargetHurt, float atk) {
    }

    // ========== Emotion / Reaction ==========

    protected void updateEmotionState() {
        // update HP state
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

        // [PORT] 1.10.2 -> 1.20.1: restore legacy emotion roll chain
        // hungry > T_T > random bored/normal.
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
                if (this.random.nextInt(3) == 0) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, false);
                }
            } else {
                if (this.random.nextInt(4) == 0) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
                }
            }

            if (this.getStateEmotion(ID.S.Emotion4) == ID.Emotion.NORMAL) {
                if (this.random.nextInt(3) == 0) {
                    this.setStateEmotion(ID.S.Emotion4, ID.Emotion.BORED, false);
                }
            } else {
                if (this.random.nextInt(3) == 0) {
                    this.setStateEmotion(ID.S.Emotion4, ID.Emotion.NORMAL, false);
                }
            }
        }

        if (!this.level().isClientSide()) {
            this.sendSyncPacketEmotion();
        }
    }

    /**
     * update morale over time. called every 128 ticks
     */
    protected void updateMorale() {
        int m = this.getMorale();
        // out of combat: last combat > 600 ticks ago (~30 sec)
        boolean outOfCombat = (this.tickCount - this.getCombatTick()) > 600;

        if (outOfCombat) {
            if (m < ID.Morale.L_Normal + 1000) {
                // recover toward 3100 (takes ~5 min from 0)
                this.setStateMinor(ID.M.Morale, m + 40);
            } else if (m > ID.Morale.L_Happy) {
                // decrease high morale slowly
                this.setStateMinor(ID.M.Morale, m - 10);
            }
        } else {
            // in combat: morale always decreases
            if (m < ID.Morale.L_Tired) {
                this.setStateMinor(ID.M.Morale, m - 11);
            } else if (m < ID.Morale.L_Normal) {
                this.setStateMinor(ID.M.Morale, m - 7);
            } else if (m < ID.Morale.L_Happy) {
                this.setStateMinor(ID.M.Morale, m - 5);
            } else {
                this.setStateMinor(ID.M.Morale, m - 11);
            }
        }
    }

    /**
     * apply emotes reaction. type: 0=normal,1=stranger,2=damaged,3=attack,4=idle
     */
    public void applyEmotesReaction(int type) {
        if (this.getEmotesTick() <= 0) {
            switch (type) {
                case 0 -> this.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, true);
                case 1 -> this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
                case 2 -> this.setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, true);
                case 3 -> this.setStateEmotion(ID.S.Emotion, ID.Emotion.ANGRY, true);
                case 4 -> this.setStateEmotion(ID.S.Emotion, ID.Emotion.BORED, true);
            }
            this.setEmotesTick(20);
        }
    }

    /**
     * play sound at attacker position. type: 0=light,1=heavy_near,2=heavy_far
     */
    public void applySoundAtAttacker(int type, Entity target) {
        if (!this.level().isClientSide()) {
            SoundEvent sound = switch (type) {
                case 1 -> ModSounds.SHIP_FIRELIGHT.get();
                case 2 -> ModSounds.SHIP_FIREHEAVY.get();
                default -> ModSounds.SHIP_HIT.get();
            };
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    sound, this.getSoundSource(), (float) ConfigHandler.volumeAttack(), this.getVoicePitch() * 0.85F);

            if (this.random.nextInt(8) == 0) {
                this.playSound(this.getCustomSound(1, this), this.getSoundVolume(), this.getVoicePitch());
            }
        }
    }

    /**
     * get custom sound for this entity. type index varies by entity
     */
    public SoundEvent getCustomSound(int type, BasicEntityShip ship) {
        SoundEvent sound = ModSounds.getCustomSound(type, ship.getShipClass());
        return sound != null ? sound : SoundEvents.GENERIC_HURT;
    }

    /**
     * mark target with flare for visibility
     */
    public void flareTarget(Entity target) {
        if (target != null && !this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CReactPacket.flareEffect((int) target.getX(), (int) target.getY(), (int) target.getZ()),
                    this);
        }
    }

    /**
     * mark target with flare (position variant)
     */
    public void flareTarget(BlockPos target) {
        if (target != null && !this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CReactPacket.flareEffect(target.getX(), target.getY(), target.getZ()),
                    this);
        }
    }

    /**
     * send attack time particle packet
     */
    public void applyParticleAtAttacker(int type, Entity target, Entity target2) {
        if (target != null && !this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    new S2CSpawnParticlePacket((byte) type, this.getId(), null),
                    this);
        }
    }

    /**
     * randomize sensitive body part
     */
    public void randomSensitiveBody() {
        this.setStateMinor(ID.M.SensBody, this.random.nextInt(11));
    }

    // ========== Reset Missile Data ==========

    public void resetMissileData() {
        this.MissileData = new MissileData[5];
        for (int i = 0; i < 5; i++) {
            this.MissileData[i] = new MissileData();
        }
    }

    // ========== Network Sync ==========

    /**
     * Send full misc state sync (minor + emotion + flags) to all tracking clients.
     */
    public void sendSyncPacketAll() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncAllMisc(this), this);
        }
    }

    /**
     * Send attribute layers sync to all tracking clients.
     */
    public void sendSyncPacketAttrs() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncAttrs(this), this);
        }
    }

    /**
     * Send minor states sync to all tracking clients.
     */
    public void sendSyncPacketMinor() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncMinor(this), this);
        }
    }

    /**
     * Send emotion/model state sync to all tracking clients.
     */
    public void sendSyncPacketEmotion() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncEmotion(this), this);
        }
    }

    /**
     * Send rider list sync to all tracking clients.
     */
    public void sendSyncPacketRiders() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncRiders(this), this);
        }
    }

    /**
     * Send flags sync to all tracking clients.
     */
    public void sendSyncPacketFlags() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncFlags(this), this);
        }
    }

    /**
     * Send formation sync to all tracking clients.
     */
    public void sendSyncPacketFormation() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncFormation(this), this);
        }
    }

    /**
     * Send guard position sync to all tracking clients.
     */
    public void sendSyncPacketGuard() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncGuard(this), this);
        }
    }

    /**
     * Send ID fields sync to all tracking clients.
     */
    public void sendSyncPacketID() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncID(this), this);
        }
    }

    /**
     * Send timer sync to all tracking clients.
     */
    public void sendSyncPacketTimer() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncTimer(this), this);
        }
    }

    /**
     * Send unit name sync to all tracking clients.
     */
    public void sendSyncPacketUnitName() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncUnitName(this), this);
        }
    }

    /**
     * Send buff map sync to all tracking clients.
     */
    public void sendSyncPacketBuffmap() {
        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    S2CEntitySyncPacket.syncBuffMap(this), this);
        }
    }

    /**
     * Client-side: send a sync request to the server.
     * type: 0=model sync, 1=unit name, 2=buff map
     */
    public void sendSyncRequest(int type) {
        if (this.level().isClientSide()) {
            switch (type) {
                case 0:
                    ModNetworking.sendToServer(
                            new C2SInputPacket(C2SInputPacket.Request_SyncModel, this.getId()));
                    break;
                case 1:
                    ModNetworking.sendToServer(
                            new C2SInputPacket(C2SInputPacket.Request_UnitName, this.getId()));
                    break;
                case 2:
                    ModNetworking.sendToServer(
                            new C2SInputPacket(C2SInputPacket.Request_Buffmap, this.getId()));
                    break;
            }
        }
    }

    // ========== IShipNavigator Implementation ==========

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

    public float getAiMoveSpeed() {
        return getMoveSpeed();
    }

    // ========== IShipFlags Implementation ==========

    @Override
    public int getStateMinor(int id) {
        return StateMinor[id];
    }

    @Override
    public void setStateMinor(int id, int par1) {
        switch (id) {
            case ID.M.Morale:
                if (par1 < 0)
                    par1 = 0;
                break;
            case ID.M.CraneState:
                if (par1 > 0) {
                    if (getStateTimer(ID.T.CrandDelay) > 0)
                        return;
                    else
                        setStateTimer(ID.T.CrandDelay, 20);
                }
                break;
        }
        StateMinor[id] = par1;
    }

    @Override
    public boolean getStateFlag(int flag) {
        if (flag == ID.F.NoFuel && (this.isDeadOrDying() || this.deathTime > 0))
            return true;
        return StateFlag[flag];
    }

    @Override
    public void setStateFlag(int id, boolean par1) {
        this.StateFlag[id] = par1;

        if (!this.level().isClientSide()) {
            if (id == ID.F.UseMelee) {
                clearAITasks();
                setAIList();
                if (this.getVehicle() instanceof BasicEntityMount mount) {
                    mount.clearAITasks();
                    mount.setAIList();
                }
            } else if (id == ID.F.PassiveAI) {
                clearAITargetTasks();
                setAITargetList();
            }
        }
    }

    @Override
    public void setUpdateFlag(int id, boolean par1) {
        UpdateFlag[id] = par1;
    }

    @Override
    public boolean getUpdateFlag(int id) {
        return UpdateFlag[id];
    }

    // ========== IShipEmotion Implementation ==========

    @Override
    public int getStateEmotion(int id) {
        return StateEmotion[id];
    }

    @Override
    public void setStateEmotion(int id, int value, boolean sync) {
        StateEmotion[id] = value;
        if (sync && !this.level().isClientSide()) {
            this.sendSyncPacketEmotion();
        }
    }

    @Override
    public int getStateTimer(int id) {
        return StateTimer[id];
    }

    @Override
    public void setStateTimer(int id, int value) {
        StateTimer[id] = value;
    }

    @Override
    public int getFaceTick() {
        return this.StateTimer[ID.T.FaceTime];
    }

    @Override
    public void setFaceTick(int par1) {
        this.StateTimer[ID.T.FaceTime] = par1;
    }

    @Override
    public int getHeadTiltTick() {
        return this.StateTimer[ID.T.HeadTilt];
    }

    @Override
    public void setHeadTiltTick(int par1) {
        this.StateTimer[ID.T.HeadTilt] = par1;
    }

    @Override
    public int getAttackTick() {
        return this.StateTimer[ID.T.AttackTime];
    }

    @Override
    public void setAttackTick(int par1) {
        this.StateTimer[ID.T.AttackTime] = par1;
    }

    @Override
    public int getAttackTick2() {
        return this.StateTimer[ID.T.AttackTime2];
    }

    @Override
    public void setAttackTick2(int par1) {
        this.StateTimer[ID.T.AttackTime2] = par1;
    }

    @Override
    public int getDeathTick() {
        return this.deathTime;
    }

    @Override
    public void setDeathTick(int par1) {
        this.deathTime = par1;
    }

    @Override
    public float getModelRotate(int par1) {
        if (par1 >= 0 && par1 < rotateAngle.length)
            return rotateAngle[par1];
        return rotateAngle[0];
    }

    @Override
    public void setModelRotate(int par1, float par2) {
        if (par1 >= 0 && par1 < rotateAngle.length)
            rotateAngle[par1] = par2;
    }

    @Override
    public int getTickExisted() {
        return this.tickCount;
    }

    @Override
    public float getSwingTime(float partialTick) {
        return this.getAttackAnim(partialTick);
    }

    @Override
    public boolean getIsRiding() {
        return this.isPassenger();
    }

    @Override
    public boolean getIsSprinting() {
        return this.isSprinting() || this.walkAnimation.speed() > 0.9F;
    }

    @Override
    public boolean getIsSitting() {
        return this.isInSittingPose();
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
        this.setOrderedToSit(sit);
        this.setInSittingPose(sit);
        if (sit) {
            this.jumping = false;
            this.getNavigation().stop();
            this.setTarget(null);
            this.setEntityTarget(null);
        }
    }

    @Override
    public int getRidingState() {
        return this.ridingState;
    }

    @Override
    public void setRidingState(int state) {
        this.ridingState = state;
    }

    @Override
    public int getScaleLevel() {
        return this.scaleLevel;
    }

    @Override
    public void setScaleLevel(int par1) {
        this.scaleLevel = par1;
        setSizeWithScaleLevel();
    }

    /**
     * Set entity dimensions based on scale level. Override in subclass for specific
     * sizes.
     */
    public void setSizeWithScaleLevel() {
        float scaleFactor = 1.0F + this.scaleLevel * 0.5F;
        this.entityWidth = 0.6F * scaleFactor;
        this.entityHeight = 1.875F * scaleFactor;
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(this.entityWidth, this.entityHeight);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions size) {
        return this.entityHeight * 0.85F;
    }

    @Override
    public RandomSource getRand() {
        return this.random;
    }

    @Override
    public double getShipDepth(int type) {
        switch (type) {
            case 1:
                if (this.getVehicle() instanceof IShipEmotion) {
                    return ((IShipEmotion) this.getVehicle()).getShipDepth(0);
                } else {
                    return this.ShipDepth;
                }
            case 2:
                return 0D;
            default:
                return this.ShipDepth;
        }
    }

    // ========== IShipOwner Implementation ==========

    @Override
    public int getPlayerUID() {
        return getStateMinor(ID.M.PlayerUID);
    }

    @Override
    public void setPlayerUID(int par1) {
        this.setStateMinor(ID.M.PlayerUID, par1);
    }

    @Override
    public Entity getHostEntity() {
        int uid = this.getPlayerUID();
        if (uid > 0) {
            if (this.level().isClientSide()) {
                return this.getOwner();
            }

            ServerPlayer player = ServerDataManager.getPlayerByUID(uid);
            if (player != null) {
                return player;
            }
        }

        return this.getOwner();
    }

    // ========== IShipAttrs Implementation ==========

    @Override
    public Attrs getAttrs() {
        return this.shipAttrs;
    }

    @Override
    public void setAttrs(Attrs data) {
        if (data instanceof AttrsAdv) {
            this.shipAttrs = (AttrsAdv) data;
        }
    }

    // ========== IShipAttackBase Implementation ==========

    @Override
    public Entity getEntityTarget() {
        return this.getTarget();
    }

    @Override
    public void setEntityTarget(Entity target) {
        this.setTarget((LivingEntity) target);
    }

    @Override
    public Entity getEntityRevengeTarget() {
        return this.rvgTarget;
    }

    @Override
    public void setEntityRevengeTarget(Entity target) {
        this.rvgTarget = target;
    }

    @Override
    public int getEntityRevengeTime() {
        return this.StateTimer[ID.T.RevengeTime];
    }

    @Override
    public void setEntityRevengeTime() {
        this.StateTimer[ID.T.RevengeTime] = this.tickCount;
    }

    @Override
    public int getDamageType() {
        return getStateMinor(ID.M.DamageType);
    }

    @Override
    public boolean getAttackType(int par1) {
        return this.getStateFlag(par1);
    }

    @Override
    public int getAmmoLight() {
        return this.StateMinor[ID.M.NumAmmoLight];
    }

    @Override
    public void setAmmoLight(int num) {
        this.StateMinor[ID.M.NumAmmoLight] = num;
    }

    @Override
    public int getAmmoHeavy() {
        return this.StateMinor[ID.M.NumAmmoHeavy];
    }

    @Override
    public void setAmmoHeavy(int num) {
        this.StateMinor[ID.M.NumAmmoHeavy] = num;
    }

    @Override
    public boolean hasAmmoLight() {
        return StateMinor[ID.M.NumAmmoLight] >= StateMinor[ID.M.AmmoCon];
    }

    @Override
    public boolean hasAmmoHeavy() {
        return StateMinor[ID.M.NumAmmoHeavy] >= StateMinor[ID.M.AmmoCon];
    }

    @Override
    public boolean useAmmoLight() {
        return StateFlag[ID.F.UseAmmoLight];
    }

    @Override
    public boolean useAmmoHeavy() {
        return StateFlag[ID.F.UseAmmoHeavy];
    }

    @Override
    public int getLevel() {
        return StateMinor[ID.M.ShipLevel];
    }

    @Override
    public HashMap<Integer, Integer> getBuffMap() {
        return this.BuffMap;
    }

    @Override
    public void setBuffMap(HashMap<Integer, Integer> map) {
        this.BuffMap = map;
    }

    @Override
    public HashMap<Integer, int[]> getAttackEffectMap() {
        return this.AttackEffectMap;
    }

    @Override
    public void setAttackEffectMap(HashMap<Integer, int[]> map) {
        this.AttackEffectMap = map;
    }

    @Override
    public MissileData getMissileData(int type) {
        if (type >= 0 && type < this.MissileData.length) {
            return this.MissileData[type];
        }
        return new MissileData();
    }

    @Override
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

    // ========== IShipGuardian Implementation ==========

    @Override
    public Entity getGuardedEntity() {
        return this.guardedEntity;
    }

    @Override
    public void setGuardedEntity(Entity entity) {
        this.guardedEntity = entity;
    }

    @Override
    public int getGuardedPos(int vec) {
        return switch (vec) {
            case 0 -> getStateMinor(ID.M.GuardX);
            case 1 -> getStateMinor(ID.M.GuardY);
            case 2 -> getStateMinor(ID.M.GuardZ);
            case 3 -> getStateMinor(ID.M.GuardDim);
            case 4 -> getStateMinor(ID.M.GuardType);
            default -> 0;
        };
    }

    @Override
    public void setGuardedPos(int x, int y, int z, int dim, int type) {
        setStateMinor(ID.M.GuardX, x);
        setStateMinor(ID.M.GuardY, y);
        setStateMinor(ID.M.GuardZ, z);
        setStateMinor(ID.M.GuardDim, dim);
        setStateMinor(ID.M.GuardType, type);
    }

    @Override
    public BlockPos getLastWaypoint() {
        return this.waypoints[0];
    }

    @Override
    public void setLastWaypoint(BlockPos pos) {
        this.waypoints[0] = pos;
    }

    @Override
    public int getWpStayTime() {
        return this.StateTimer[ID.T.WpStayTime];
    }

    @Override
    public void setWpStayTime(int time) {
        this.StateTimer[ID.T.WpStayTime] = time;
    }

    @Override
    public int getWpStayTimeMax() {
        return getStateMinor(ID.M.WpStay);
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

    // ========== Convenience Getters/Setters ==========

    public LivingEntity getAITarget() {
        return aiTarget;
    }

    public void setAITarget(LivingEntity target) {
        this.aiTarget = target;
    }

    public byte getShipType() {
        return (byte) getStateMinor(ID.M.ShipType);
    }

    public short getShipClass() {
        return (short) getStateMinor(ID.M.ShipClass);
    }

    public int getShipUID() {
        return getStateMinor(ID.M.ShipUID);
    }

    public void setShipUID(int par1) {
        this.setStateMinor(ID.M.ShipUID, par1);
    }

    public int getMorale() {
        return this.StateMinor[ID.M.Morale];
    }

    public void setMorale(int value) {
        this.StateMinor[ID.M.Morale] = value;
    }

    public void addMorale(int value) {
        this.StateMinor[ID.M.Morale] += value;
        if (this.StateMinor[ID.M.Morale] < 0)
            this.StateMinor[ID.M.Morale] = 0;
        else if (this.StateMinor[ID.M.Morale] > 16000)
            this.StateMinor[ID.M.Morale] = 16000;
    }

    public void addAmmoLight(int value) {
        this.StateMinor[ID.M.NumAmmoLight] += value;
        if (this.StateMinor[ID.M.NumAmmoLight] < 0)
            this.StateMinor[ID.M.NumAmmoLight] = 0;
    }

    public void addAmmoHeavy(int value) {
        this.StateMinor[ID.M.NumAmmoHeavy] += value;
        if (this.StateMinor[ID.M.NumAmmoHeavy] < 0)
            this.StateMinor[ID.M.NumAmmoHeavy] = 0;
    }

    public void addGrudge(int value) {
        this.StateMinor[ID.M.NumGrudge] += value;
        if (this.StateMinor[ID.M.NumGrudge] < 0)
            this.StateMinor[ID.M.NumGrudge] = 0;
    }

    public void addKills() {
        StateMinor[ID.M.Kills]++;
    }

    public int getEmotesTick() {
        return this.StateTimer[ID.T.EmoteDelay];
    }

    public void setEmotesTick(int par1) {
        this.StateTimer[ID.T.EmoteDelay] = par1;
    }

    public int getCombatTick() {
        return this.StateTimer[ID.T.LastCombat];
    }

    public void setCombatTick(int par1) {
        this.StateTimer[ID.T.LastCombat] = par1;
    }

    public int getHitHeight() {
        return this.StateMinor[ID.M.HitHeight];
    }

    public void setHitHeight(int par1) {
        this.StateMinor[ID.M.HitHeight] = par1;
    }

    public int getHitAngle() {
        return this.StateMinor[ID.M.HitAngle];
    }

    public void setHitAngle(int par1) {
        this.StateMinor[ID.M.HitAngle] = par1;
    }

    public int getGrudgeConsumption() {
        return getStateMinor(ID.M.GrudgeCon);
    }

    public void setGrudgeConsumption(int par1) {
        this.setStateMinor(ID.M.GrudgeCon, par1);
    }

    public int getAmmoConsumption() {
        return getStateMinor(ID.M.AmmoCon);
    }

    public void setAmmoConsumption(int par1) {
        this.setStateMinor(ID.M.AmmoCon, par1);
    }

    public int getFoodSaturation() {
        return getStateMinor(ID.M.Food);
    }

    public void setFoodSaturation(int par1) {
        setStateMinor(ID.M.Food, par1);
    }

    public int getFoodSaturationMax() {
        return getStateMinor(ID.M.FoodMax);
    }

    public void setFoodSaturationMax(int par1) {
        setStateMinor(ID.M.FoodMax, par1);
    }

    public CapaShipInventory getCapaShipInventory() {
        return this.itemHandler;
    }

    public float[] getModelPos() {
        return ModelPos;
    }

    public byte getStateFlagI(int flag) {
        return StateFlag[flag] ? (byte) 1 : (byte) 0;
    }

    public void setStateFlagI(int id, int par1) {
        setStateFlag(id, par1 > 0);
    }

    public void setRiderAndMountSit() {
        if (this.getVehicle() instanceof BasicEntityShip mountShip) {
            mountShip.setEntitySit(this.isOrderedToSit());
            if (mountShip.getRidingState() > 0) {
                for (Entity r : mountShip.getPassengers()) {
                    if (r instanceof BasicEntityShip bs) {
                        bs.setEntitySit(this.isOrderedToSit());
                    }
                }
            }
        }
        for (Entity r : this.getPassengers()) {
            if (r instanceof BasicEntityShip bs) {
                bs.setEntitySit(this.isOrderedToSit());
            }
        }
    }

    // ========== Array Accessors (for NBT save/load) ==========

    public int[] getStateMinorArray() {
        return this.StateMinor;
    }

    public boolean[] getStateFlagArray() {
        return this.StateFlag;
    }

    public int[] getStateEmotionArray() {
        return this.StateEmotion;
    }

    // ========== Container Fields (for GUI sync) ==========

    public int getFieldCount() {
        return 35;
    }

    public int getField(int id) {
        return switch (id) {
            case 0 -> this.StateMinor[ID.M.ExpCurrent];
            case 1 -> this.StateMinor[ID.M.NumAmmoLight];
            case 2 -> this.StateMinor[ID.M.NumAmmoHeavy];
            case 3 -> this.StateMinor[ID.M.NumAirLight];
            case 4 -> this.StateMinor[ID.M.NumAirHeavy];
            case 5 -> this.getStateFlagI(ID.F.UseMelee);
            case 6 -> this.getStateFlagI(ID.F.UseAmmoLight);
            case 7 -> this.getStateFlagI(ID.F.UseAmmoHeavy);
            case 8 -> this.getStateFlagI(ID.F.UseAirLight);
            case 9 -> this.getStateFlagI(ID.F.UseAirHeavy);
            case 10 -> this.getStateFlagI(ID.F.IsMarried);
            case 11 -> this.StateMinor[ID.M.FollowMin];
            case 12 -> this.StateMinor[ID.M.FollowMax];
            case 13 -> this.StateMinor[ID.M.FleeHP];
            case 14 -> this.getStateFlagI(ID.F.PassiveAI);
            case 15 -> this.getStateFlagI(ID.F.UseRingEffect);
            case 16 -> this.getStateFlagI(ID.F.OnSightChase);
            case 17 -> this.getStateFlagI(ID.F.PVPFirst);
            case 18 -> this.getStateFlagI(ID.F.AntiAir);
            case 19 -> this.getStateFlagI(ID.F.AntiSS);
            case 20 -> this.getStateFlagI(ID.F.TimeKeeper);
            case 21 -> this.getMorale();
            case 22 -> this.StateMinor[ID.M.DrumState];
            case 23 -> this.getStateFlagI(ID.F.PickItem);
            case 24 -> this.StateMinor[ID.M.WpStay];
            case 25 -> this.StateMinor[ID.M.Kills];
            case 26 -> this.StateMinor[ID.M.NumGrudge];
            case 27 -> this.itemHandler.getInventoryPage();
            case 28 -> this.getStateFlagI(ID.F.ShowHeldItem);
            case 29 -> this.StateMinor[ID.M.UseCombatRation];
            case 30 -> this.getStateFlagI(ID.F.AutoPump);
            case 31 -> this.getStateEmotion(ID.S.State);
            case 32 -> this.StateMinor[ID.M.Task];
            case 33 -> this.StateMinor[ID.M.TaskSide];
            case 34 -> this.getStateFlagI(ID.F.NoFuel);
            default -> 0;
        };
    }

    public void setField(int id, int value) {
        switch (id) {
            case 0:
                this.StateMinor[ID.M.ExpCurrent] = value;
                break;
            case 1:
                this.StateMinor[ID.M.NumAmmoLight] = value;
                break;
            case 2:
                this.StateMinor[ID.M.NumAmmoHeavy] = value;
                break;
            case 3:
                this.StateMinor[ID.M.NumAirLight] = value;
                break;
            case 4:
                this.StateMinor[ID.M.NumAirHeavy] = value;
                break;
            case 5:
                this.setStateFlagI(ID.F.UseMelee, value);
                break;
            case 6:
                this.setStateFlagI(ID.F.UseAmmoLight, value);
                break;
            case 7:
                this.setStateFlagI(ID.F.UseAmmoHeavy, value);
                break;
            case 8:
                this.setStateFlagI(ID.F.UseAirLight, value);
                break;
            case 9:
                this.setStateFlagI(ID.F.UseAirHeavy, value);
                break;
            case 10:
                this.setStateFlagI(ID.F.IsMarried, value);
                break;
            case 11:
                this.StateMinor[ID.M.FollowMin] = value;
                break;
            case 12:
                this.StateMinor[ID.M.FollowMax] = value;
                break;
            case 13:
                this.StateMinor[ID.M.FleeHP] = value;
                break;
            case 14:
                this.setStateFlagI(ID.F.PassiveAI, value);
                break;
            case 15:
                this.setStateFlagI(ID.F.UseRingEffect, value);
                break;
            case 16:
                this.setStateFlagI(ID.F.OnSightChase, value);
                break;
            case 17:
                this.setStateFlagI(ID.F.PVPFirst, value);
                break;
            case 18:
                this.setStateFlagI(ID.F.AntiAir, value);
                break;
            case 19:
                this.setStateFlagI(ID.F.AntiSS, value);
                break;
            case 20:
                this.setStateFlagI(ID.F.TimeKeeper, value);
                break;
            case 21:
                this.StateMinor[ID.M.Morale] = value;
                break;
            case 22:
                this.StateMinor[ID.M.DrumState] = value;
                break;
            case 23:
                this.setStateFlagI(ID.F.PickItem, value);
                break;
            case 24:
                this.StateMinor[ID.M.WpStay] = value;
                break;
            case 25:
                this.StateMinor[ID.M.Kills] = value;
                break;
            case 26:
                this.StateMinor[ID.M.NumGrudge] = value;
                break;
            case 27:
                this.itemHandler.setInventoryPage(value);
                break;
            case 28:
                this.setStateFlagI(ID.F.ShowHeldItem, value);
                break;
            case 29:
                this.StateMinor[ID.M.UseCombatRation] = value;
                break;
            case 30:
                this.setStateFlagI(ID.F.AutoPump, value);
                break;
            case 31:
                this.setStateEmotion(ID.S.State, value, false);
                break;
            case 32:
                this.StateMinor[ID.M.Task] = value;
                break;
            case 33:
                this.StateMinor[ID.M.TaskSide] = value;
                break;
            case 34:
                this.setStateFlagI(ID.F.NoFuel, value);
                break;
        }
    }

    // ========== MenuProvider Implementation ==========

    @Override
    public Component getDisplayName() {
        return this.getCustomName() != null ? this.getCustomName() : this.getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ContainerShipInventory(containerId, playerInv, this);
    }

    /**
     * Open this ship's GUI for a server player.
     */
    public void openGUI(ServerPlayer player) {
        NetworkHooks.openScreen(player, this,
                buf -> buf.writeInt(this.getId()));
    }

    // ========== Simple Overrides ==========

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean canPassengerSteer() {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight();
    }

    @Override
    public int getPortalCooldown() {
        return 40;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public int getExperienceReward() {
        return 0;
    }

    public int getGrudge() {
        return this.StateMinor[ID.M.NumGrudge];
    }

    // ========== Player Interaction (Right-Click) ==========

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // disable off-hand, dead entities don't respond
        if (hand == InteractionHand.OFF_HAND || !this.isAlive())
            return InteractionResult.FAIL;

        // server side
        if (!this.level().isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);

            // use item
            if (!stack.isEmpty()) {
                // use name tag, owner only
                if (stack.getItem() == Items.NAME_TAG && TeamHelper.checkSameOwner(player, this)) {
                    if (stack.hasCustomHoverName()) {
                        this.setCustomName(stack.getHoverName());
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
                // use repair bucket, owner only
                else if (stack.getItem() == ModItems.BUCKET_REPAIR.get()
                        && TeamHelper.checkSameOwner(player, this)) {
                    if (InteractHelper.interactBucket(this, player, stack))
                        return InteractionResult.SUCCESS;
                }
                // use owner paper, owner only
                else if (stack.getItem() == ModItems.OWNER_PAPER.get()
                        && TeamHelper.checkSameOwner(this, player) && player.isShiftKeyDown()) {
                    if (InteractHelper.interactOwnerPaper(this, player, stack))
                        return InteractionResult.SUCCESS;
                }
                // use modernization kit, owner only
                else if (stack.getItem() == ModItems.MODERN_KIT.get()
                        && TeamHelper.checkSameOwner(player, this)) {
                    if (InteractHelper.interactModernKit(this, player, stack))
                        return InteractionResult.SUCCESS;
                }
                // use pointer item (caress head mode server side)
                else if (stack.getItem() == ModItems.POINTER.get() && !player.isShiftKeyDown()) {
                    // [PORT] Keep pointer command modes (0-2) in PointerItem packet flow.
                    // Only consume direct entity interaction in caress mode (>2).
                    if (PointerItem.getMode(stack) > PointerItem.MODE_FORMATION) {
                        InteractHelper.interactPointer(this, player, stack);
                        return InteractionResult.SUCCESS;
                    }
                }
                // use kaitai hammer, OWNER and OP only
                else if (stack.getItem() == ModItems.KAITAI_HAMMER.get() && player.isShiftKeyDown()
                        && (TeamHelper.checkSameOwner(this, player) || ServerDataManager.checkOP(player))) {
                    InteractHelper.interactKaitaiHammer(this, player, stack);
                    return InteractionResult.SUCCESS;
                }
                // use wedding ring: requires not married, sneaking, same owner
                else if (stack.getItem() == ModItems.MARRIAGE_RING.get() && !this.getStateFlag(ID.F.IsMarried)
                        && player.isShiftKeyDown() && TeamHelper.checkSameOwner(this, player)) {
                    InteractHelper.interactWeddingRing(this, player, stack);
                    return InteractionResult.SUCCESS;
                }
                // use training book, owner only
                else if (stack.getItem() == ModItems.TRAINING_BOOK.get()
                        && TeamHelper.checkSameOwner(player, this)) {
                    if (this.getLevel() < 150) {
                        int lv = this.getLevel() + 5 + this.random.nextInt(6);
                        int lvcap = this.getStateFlag(ID.F.IsMarried) ? 150 : 100;
                        if (lv > lvcap)
                            lv = lvcap;

                        this.setShipLevel(lv, true);

                        // level up sound
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), 0.75F, 1F);

                        // item--
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    return InteractionResult.PASS;
                }
                // use lead: clear path
                else if (stack.getItem() == Items.LEAD) {
                    this.getNavigation().stop();
                    return InteractionResult.SUCCESS;
                }
                // feed
                else if (InteractHelper.interactFeed(this, player, stack)) {
                    return InteractionResult.SUCCESS;
                }
            }

            // owner right click
            // [PORT] 1.10.2 -> 1.20.1: allow UUID ownership fallback when UID capability
            // has not synced yet, so sit toggle does not silently fail.
            if (TeamHelper.checkSameOwner(this, player) || this.isOwnedBy(player)) {
                // sneak: open GUI
                if (player.isShiftKeyDown()) {
                    if (player instanceof ServerPlayer sp) {
                        this.openGUI(sp);
                    }
                    return InteractionResult.SUCCESS;
                } else {
                    // [PORT] 1.10.2 parity: pointer in use should not toggle sit here.
                    if (getPointerInUse(player).isEmpty()) {
                        // toggle sitting (bare hand / non-pointer interaction)
                        this.setEntitySit(!this.isOrderedToSit());
                        this.setRiderAndMountSit();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        // client side
        else {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                // use pointer item (caress head mode CLIENT side)
                if (stack.getItem() == ModItems.POINTER.get() && !player.isShiftKeyDown()
                        && PointerItem.getMode(stack) > PointerItem.MODE_FORMATION) {
                    // calc hit height: ratio of player eye Y relative to entity bounding box
                    double eyeY = player.getEyeY();
                    double entBottom = this.getY();
                    double entHeight = this.getBbHeight();
                    int hitH = (entHeight > 0) ? (int) (((eyeY - entBottom) / entHeight) * 100.0) : 50;
                    hitH = Mth.clamp(hitH, 0, 100);
                    this.setHitHeight(hitH);

                    // calc hit angle: angle from entity facing to player
                    double dx = player.getX() - this.getX();
                    double dz = player.getZ() - this.getZ();
                    int hitA = (int) (Math.toDegrees(Math.atan2(dz, dx)) - this.getYRot()) % 360;
                    if (hitA < 0)
                        hitA += 360;
                    this.setHitAngle(hitA);

                    this.checkCaressed();
                }
            }
        }

        return InteractionResult.PASS;
    }

    // ========== Death ==========

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide()) {
            // Drop ship inventory contents
            for (int i = 0; i < this.itemHandler.getSlots(); i++) {
                ItemStack stack = this.itemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    this.spawnAtLocation(stack);
                    this.itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }

            // Do NOT drop a spawn egg here - tickDeath() handles the saved egg drop
            // with full NBT data (attributes, name, level, etc.)
        }

        super.die(source);
        LogHelper.info("Ship died: class=" + this.getShipClass() + " source=" + source.getMsgId());
    }

    // ========== Death Update ==========

    @Override
    protected void tickDeath() {
        ++this.deathTime;

        // spawn smoke on client
        if (this.level().isClientSide()) {
            if ((this.tickCount & 3) == 0) {
                double range = this.getBbWidth() * 1.2D;
                for (int i = 0; i < 5; i++) {
                    ParticleHelper.spawnAttackParticleAt(this.level(),
                            this.getX() - range + this.random.nextDouble() * range * 2D,
                            this.getY() + 0.1D + this.random.nextDouble() * 0.3D,
                            this.getZ() - range + this.random.nextDouble() * range * 2D,
                            1.5D, 0D, 0D, 43);
                }
            }
        }

        if (this.deathTime == ConfigHandler.deathTime()) {
            // spawn ship egg
            if (!this.level().isClientSide() && this.getStateFlag(ID.F.CanDrop)) {
                // prevent multiple drops
                this.setStateFlag(ID.F.CanDrop, false);

                // save ship attributes to ship spawn egg
                ItemStack egg = new ItemStack(ModItems.SHIP_SPAWN_EGG.get());
                CompoundTag eggNbt = new CompoundTag();
                CapaShipSavedValues.saveNBTData(eggNbt, this);
                eggNbt.putInt("ShipClass", this.getShipClass());
                // [PORT] 1.10.2 -> 1.20.1: keep legacy pickup-protection tags used by
                // BasicEntityItem (owner only for saved ship eggs).
                if (this.getOwnerUUID() != null) {
                    eggNbt.putString("owner", this.getOwnerUUID().toString());
                }
                if (this.ownerName != null && !this.ownerName.isEmpty()) {
                    eggNbt.putString("ownername", this.ownerName);
                } else if (this.getOwner() != null) {
                    eggNbt.putString("ownername", this.getOwner().getName().getString());
                }
                egg.setTag(eggNbt);

                // [PORT] 1.10.2 -> 1.20.1: use custom item entity to preserve legacy
                // fire-proof/non-push/owner-check behavior and reduce loss risk.
                BasicEntityItem entityItem = new BasicEntityItem(
                        ModEntities.BASIC_ENTITY_ITEM.get(),
                        this.level(),
                        this.getX(),
                        this.getY() + 0.5D,
                        this.getZ(),
                        egg);
                this.level().addFreshEntity(entityItem);
            }

            // set dead
            this.discard();

            // spawn smoke particle
            if (this.level().isClientSide()) {
                for (int k = 0; k < 20; ++k) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(ParticleTypes.EXPLOSION,
                            this.getX() + (this.random.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth(),
                            this.getY() + (this.random.nextFloat() * this.getBbHeight()),
                            this.getZ() + (this.random.nextFloat() * this.getBbWidth() * 2.0F) - this.getBbWidth(),
                            d2, d0, d1);
                }
            }
        }
        // clear bug entity
        else if (this.deathTime > ConfigHandler.deathTime() && this.isAlive()) {
            this.discard();
        }
    }

    // ========== Custom Heal ==========

    @Override
    public void heal(float healAmount) {
        // server side
        if (!this.level().isClientSide()) {
            // apply heal particle
            ModNetworking.sendToAllTracking(
                    new S2CSpawnParticlePacket((byte) 23, this.getId(), null), this);
        }

        // apply HPRES buff multiplier
        if (this.shipAttrs != null) {
            healAmount *= this.shipAttrs.getAttrsBuffed(ID.Attrs.HPRES);
        }

        super.heal(healAmount);
    }

    // ========== Remove / Cleanup ==========

    @Override
    public void remove(RemovalReason reason) {
        // clear chunk loader
        this.clearChunkLoader();

        super.remove(reason);

        // update ship cache
        this.updateShipCacheDataWithoutNewID();
    }

    // ========== Ship Cache Data ==========

    /**
     * Register or update ship ID and owner ID in ServerDataManager.
     */
    public void updateShipCacheData(boolean forceUpdate) {
        if (!this.isUpdated && tickCount % updateTime == 0 || forceUpdate) {
            LogHelper.debug("DEBUG: update ship: initial SID, PID  cd: " + updateTime + " force: " + forceUpdate);

            // update owner uid
            if (this.getPlayerUID() <= 0) {
                ServerDataManager.updateShipOwnerID(this);
            }

            // update ship uid
            ServerDataManager.updateShipID(this);

            // update success
            if (getPlayerUID() > 0 && getShipUID() > 0) {
                this.sendSyncPacketAll();
                this.isUpdated = true;
            }

            // prolong update time
            if (updateTime > 4096) {
                updateTime = 4096;
            } else {
                updateTime *= 2;
            }
        }
    }

    /**
     * Update ship cache data without assigning new ID (for removal/death).
     */
    public void updateShipCacheDataWithoutNewID() {
        if (!this.level().isClientSide()) {
            int uid = this.getShipUID();

            if (uid > 0) {
                // ship dupe bug checking
                BasicEntityShip ship = ServerDataManager.checkShipIsDupe(this, uid);

                if (this.equals(ship)) {
                    // update cache with current data
                    ServerDataManager.updateShipID(this);
                } else {
                    this.discard();
                }
            }
        }
    }

    // ========== Consume Items (Fuel) ==========

    /**
     * Update fuel/resource consumption. Called every 128 ticks.
     */
    protected void updateConsumeItem() {
        // try to load ammo from inventory if empty
        if (!this.hasAmmoLight()) {
            this.loadAmmoFromInventory(0);
        }
        if (!this.hasAmmoHeavy()) {
            this.loadAmmoFromInventory(1);
        }

        // calc move distance
        double distX = getX() - ShipPrevX;
        double distY = getY() - ShipPrevY;
        double distZ = getZ() - ShipPrevZ;

        // calc total consumption
        int valueConsume = (int) Mth.sqrt((float) (distX * distX + distY * distY + distZ * distZ));
        if (ShipPrevY <= 0D)
            valueConsume = 0; // do not decrGrudge if ShipPrev not inited

        // morale-- per 2 blocks
        int m = (int) ((float) valueConsume * 0.5F);
        if (m < 5)
            m = 5;
        if (m > 50)
            m = 50;
        this.addMorale(-m);

        // moving grudge cost per block
        valueConsume *= ConfigHandler.consumeGrudgeAction[ID.ShipConsume.Move];

        // get exp if transport
        if (this.getShipType() == ID.ShipType.TRANSPORT && this.tickCount > 200) {
            int moveExp = (int) (valueConsume * 0.2F);
            addShipExp(moveExp);
        }

        // add idle grudge cost
        valueConsume += this.getGrudgeConsumption();

        // eat grudge
        decrGrudgeNum(valueConsume);

        // update pos
        ShipPrevX = getX();
        ShipPrevY = getY();
        ShipPrevZ = getZ();
    }

    // ========== Body / Outfit ==========

    /**
     * Get body height stand for scale level.
     */
    public byte getBodyHeightStand(int par1) {
        if (par1 >= 0 && par1 < BodyHeightStand.length)
            return BodyHeightStand[par1];
        return BodyHeightStand[0];
    }

    /**
     * Get body height sit for scale level.
     */
    public byte getBodyHeightSit(int par1) {
        if (par1 >= 0 && par1 < BodyHeightSit.length)
            return BodyHeightSit[par1];
        return BodyHeightSit[0];
    }

    /**
     * Get body ID from hit height.
     */
    public int getBodyIDFromHeight(int hitHeight) {
        if (hitHeight > 80)
            return ID.Body.Head;
        else if (hitHeight > 70)
            return ID.Body.Face;
        else if (hitHeight > 60)
            return ID.Body.Neck;
        else if (hitHeight > 50)
            return ID.Body.Chest;
        else if (hitHeight > 40)
            return ID.Body.UBelly;
        else if (hitHeight > 30)
            return ID.Body.Belly;
        else if (hitHeight > 20)
            return ID.Body.Butt;
        else
            return ID.Body.Leg;
    }

    /**
     * Get hit angle ID. 0:front, 1:back
     */
    public int getHitAngleID(int hitAngle) {
        return (hitAngle > 90 && hitAngle < 270) ? 1 : 0;
    }

    /**
     * Check if ship can show held item.
     */
    public boolean canShowHeldItem() {
        return this.getStateFlag(ID.F.ShowHeldItem) && !this.getStateFlag(ID.F.NoFuel);
    }

    /**
     * Get sensitive body part.
     */
    public int getSensitiveBody() {
        return this.getStateMinor(ID.M.SensBody);
    }

    /**
     * Set sensitive body part.
     */
    public void setSensitiveBody(int par1) {
        this.setStateMinor(ID.M.SensBody, par1);
    }

    /**
     * Set ship outfit. Override in subclass for custom outfits.
     */
    public void setShipOutfit(boolean update) {
        // default: no outfit change
    }

    // ========== Chunk Loader ==========

    /**
     * Clear chunk loader ticket.
     */
    public void clearChunkLoader() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int chunkX = Mth.floor(this.getX()) >> 4;
            int chunkZ = Mth.floor(this.getZ()) >> 4;
            ForgeChunkManager.forceChunk(
                    serverLevel, "shincolle", this.getUUID(), chunkX, chunkZ, false, true);
        }
    }

    /**
     * Update chunk loader based on config and level.
     */
    public void updateChunkLoader() {
        if (ConfigHandler.chunkLoaderMode() <= 0)
            return;
        if (this.getStateMinor(ID.M.LevelChunkLoader) <= 0)
            return;
        if (this.getStateFlag(ID.F.NoFuel) || !this.isAlive())
            return;

        applyChunkLoader();
    }

    /**
     * Set chunk loader for this ship.
     */
    public void setChunkLoader(boolean enable) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int chunkX = Mth.floor(this.getX()) >> 4;
            int chunkZ = Mth.floor(this.getZ()) >> 4;
            ForgeChunkManager.forceChunk(
                    serverLevel, "shincolle", this.getUUID(), chunkX, chunkZ, enable, true);
        }
    }

    /**
     * Apply chunk loader effect. Loads surrounding chunks based on level.
     */
    public void applyChunkLoader() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            int chunkX = Mth.floor(this.getX()) >> 4;
            int chunkZ = Mth.floor(this.getZ()) >> 4;
            int radius = Math.min(this.getStateMinor(ID.M.LevelChunkLoader), 3);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ForgeChunkManager.forceChunk(
                            serverLevel, "shincolle", this.getUUID(),
                            chunkX + dx, chunkZ + dz, true, true);
                }
            }
        }
    }

    // ========== Caress / Emotion Check ==========

    /**
     * Check if player is caressing the ship's body.
     */
    public void checkCaressed() {
        int sensBody = this.getSensitiveBody();
        int hitBody = this.getBodyIDFromHeight(this.getHitHeight());
        int hitSide = this.getHitAngleID(this.getHitAngle());

        // sensitive body matches hit location
        if (hitBody == sensBody) {
            this.setStateEmotion(ID.S.Emotion3, ID.Emotion3.CARESS, false);
            this.setStateTimer(ID.T.Emotion3Time, 10);
        }
    }

    /**
     * Push AI target toward player for interaction.
     */
    public void pushAITarget(Player player) {
        this.setAITarget(player);
    }

    // ========== Misc Methods ==========

    /**
     * Get main hand item (from ship inventory slot 22).
     */
    public ItemStack getMainHandItemShip() {
        return this.itemHandler.getStackInSlot(22);
    }

    /**
     * Get off hand item (from ship inventory slot 23).
     */
    public ItemStack getOffHandItemShip() {
        return this.itemHandler.getStackInSlot(23);
    }

    @Override
    public void push(Entity entity) {
        // ships don't push each other
        if (entity instanceof BasicEntityShip || entity instanceof BasicEntityMount)
            return;
        super.push(entity);
    }

    /**
     * Get world hour time (0-23) at exact hour boundaries.
     * Returns the hour only when worldTime % 1000 == 0 (exact start of a new
     * in-game hour),
     * returns -1 otherwise. This ensures timekeeping sounds play exactly once per
     * hour.
     */
    public int getWorldHourTime() {
        long time = this.level().getDayTime();
        int checkTime = (int) (time % 1000L);
        if (checkTime == 0) {
            return (int) ((time / 1000L) % 24);
        }
        return -1;
    }

    /**
     * convert WpStay value to ticks
     */
    public int wpStayTime2Ticks(int wpStay) {
        if (wpStay <= 0)
            return 0;
        return wpStay * 20;
    }

    /**
     * Play timekeeping sound for the current hour.
     * Called every tick from aiStep; getWorldHourTime() ensures this only
     * triggers at exact hour boundaries (worldTime % 1000 == 0).
     * Sound type = hour + 10 (types 10-33 for hours 0-23).
     */
    public void playTimeSound() {
        int hour = getWorldHourTime();
        if (hour >= 0) {
            SoundEvent sound = ModSounds.getCustomSound(hour + 10, this.getShipClass());
            if (sound != null) {
                this.playSound(sound, (float) ConfigHandler.volumeTimekeeping(), this.getVoicePitch());
            }
        }
    }

    /**
     * Update searchlight block placement.
     */
    public void updateSearchlight() {
        if (this.getStateMinor(ID.M.LevelSearchlight) <= 0)
            return;
        if (this.getStateFlag(ID.F.NoFuel) || !this.isAlive())
            return;

        // check if it's night time (using raw day time ticks, not hour-based
        // getWorldHourTime)
        int time = (int) (this.level().getDayTime() % 24000L);
        if (time < 12500 || time > 23500)
            return;

        // place light block at ship position
        BlockPos pos = this.blockPosition().above(2);
        BlockHelper.placeSearchlight(this.level(), pos);
    }

    /**
     * Update mount entity summoning.
     */
    public void updateMountSummon() {
        if (!this.hasShipMounts() || this.canSummonMounts())
            return;

        // check if already riding
        if (this.isPassenger())
            return;

        // summon mount
        summonMountEntity();
    }

    /**
     * Summon mount entity for this ship. Override in subclass for specific mount.
     */
    public void summonMountEntity() {
        // default: no mount
    }

    /**
     * Get inventory page size.
     */
    public int getInventoryPageSize() {
        return this.itemHandler.getInventoryPage();
    }

    /**
     * Set inventory page size.
     */
    public void setInventoryPageSize(int par1) {
        this.itemHandler.setInventoryPage(par1);
    }

    // ========== Batch 2: Timer/Update Methods ==========

    /**
     * Update client-side timers for animation.
     */
    protected void updateClientTimer() {
        // attack motion timer
        if (this.StateTimer[ID.T.AttackTime] > 0)
            this.StateTimer[ID.T.AttackTime]--;

        // caress reaction time
        if (this.StateTimer[ID.T.Emotion3Time] > 0) {
            this.StateTimer[ID.T.Emotion3Time]--;

            if (this.StateTimer[ID.T.Emotion3Time] == 0) {
                this.setStateEmotion(ID.S.Emotion3, 0, false);
            }
        }
    }

    /**
     * Update both-side timers (mount skill cooldowns).
     */
    protected void updateBothSideTimer() {
        if (this.StateTimer[ID.T.MountSkillCD1] > 0)
            this.StateTimer[ID.T.MountSkillCD1]--;
        if (this.StateTimer[ID.T.MountSkillCD2] > 0)
            this.StateTimer[ID.T.MountSkillCD2]--;
        if (this.StateTimer[ID.T.MountSkillCD3] > 0)
            this.StateTimer[ID.T.MountSkillCD3]--;
        if (this.StateTimer[ID.T.MountSkillCD4] > 0)
            this.StateTimer[ID.T.MountSkillCD4]--;
        if (this.StateTimer[ID.T.MountSkillCD5] > 0)
            this.StateTimer[ID.T.MountSkillCD5]--;
    }

    /**
     * Update client body rotation based on movement direction.
     */
    protected void updateClientBodyRotate() {
        if (!this.isPassenger()) {
            if (Math.abs(this.getX() - this.xOld) > 0.1F
                    || Math.abs(this.getZ() - this.zOld) > 0.1F) {
                double dx = this.getX() - this.xOld;
                double dz = this.getZ() - this.zOld;
                float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
                this.setYRot(yaw);
            }
        }
    }

    // ========== Batch 3: Emotion/Reaction System ==========

    /**
     * Full applyParticleEmotion - sends particle packets.
     */
    public void applyParticleEmotion(int type) {
        float h = this.isOrderedToSit() ? this.getBbHeight() * 0.4F : this.getBbHeight() * 0.45F;

        if (!this.level().isClientSide()) {
            ModNetworking.sendToAllTracking(
                    new S2CSpawnParticlePacket((byte) 36, this.getId(),
                            new byte[]{(byte) (((int) (h * 100)) >> 8), (byte) ((int) (h * 100) & 0xFF),
                                    0, (byte) type}),
                    this);
        } else {
            ParticleHelper.spawnEmotionParticle(this, type);
        }
    }

    /**
     * Reaction when owner caresses the ship (normal mode).
     * Shows different emotions based on morale level and hit body part.
     */
    public void reactionNormal() {
        int m = this.getMorale();
        int body = this.getBodyIDFromHeight(this.getHitHeight());
        int baseMorale = (int) ((float) ConfigHandler.caressBaseMorale() * 2.5F);

        switch (BuffHelper.getMoraleLevel(m)) {
            case ID.Morale.Excited:
                if (body == getSensitiveBody()) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);
                    if (this.random.nextInt(2) == 0)
                        applyParticleEmotion(31); // shy
                    else
                        applyParticleEmotion(10); // dizzy
                    if (m < (int) (ID.Morale.L_Excited * 1.5F))
                        this.addMorale(baseMorale * 3 + this.random.nextInt(baseMorale + 1));
                } else {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.XD, true);
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            if (this.getStateFlag(ID.F.IsMarried))
                                applyParticleEmotion(15); // kiss
                            else
                                applyParticleEmotion(1); // heart
                            break;
                        default:
                            if (this.random.nextInt(2) == 0)
                                applyParticleEmotion(1); // heart
                            else
                                applyParticleEmotion(7); // note
                            break;
                    }
                }
                break;
            case ID.Morale.Happy:
                this.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);
                if (body == getSensitiveBody()) {
                    if (this.getStateFlag(ID.F.IsMarried)) {
                        if (this.random.nextInt(2) == 0)
                            applyParticleEmotion(31);
                        else
                            applyParticleEmotion(10);
                    } else {
                        applyParticleEmotion(10);
                    }
                    this.addMorale(baseMorale + this.random.nextInt(baseMorale + 1));
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            if (this.getStateFlag(ID.F.IsMarried))
                                applyParticleEmotion(1);
                            else
                                applyParticleEmotion(16); // haha
                            break;
                        default:
                            if (this.random.nextInt(2) == 0)
                                applyParticleEmotion(1);
                            else
                                applyParticleEmotion(7);
                            break;
                    }
                }
                break;
            case ID.Morale.Normal:
                if (body == getSensitiveBody()) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);
                    if (this.getStateFlag(ID.F.IsMarried))
                        applyParticleEmotion(19); // lick
                    else
                        applyParticleEmotion(18); // sigh
                    this.addMorale(baseMorale + this.random.nextInt(baseMorale + 1));
                    if (this.random.nextInt(6) == 0) {
                        this.pushAITarget(null);
                        this.playSound(this.getCustomSound(5, this), this.getSoundVolume(), this.getVoicePitch());
                    }
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            if (this.getStateFlag(ID.F.IsMarried))
                                applyParticleEmotion(1);
                            else
                                applyParticleEmotion(27); // -w-
                            if (this.random.nextInt(8) == 0) {
                                this.pushAITarget(null);
                                this.playSound(this.getCustomSound(5, this), this.getSoundVolume(),
                                        this.getVoicePitch());
                            }
                            break;
                        default:
                            switch (this.random.nextInt(7)) {
                                case 1 -> applyParticleEmotion(30); // pif
                                case 3 -> applyParticleEmotion(7); // note
                                case 4 -> applyParticleEmotion(26); // ya
                                case 6 -> applyParticleEmotion(11); // find
                                default -> applyParticleEmotion(29); // blink
                            }
                            break;
                    }
                }
                break;
            case ID.Morale.Tired:
                if (body == getSensitiveBody()) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);
                    applyParticleEmotion(32); // hmm
                    this.addMorale(this.random.nextInt(baseMorale + 1));
                    if (this.random.nextInt(2) == 0) {
                        this.pushAITarget(null);
                        this.playSound(this.getCustomSound(5, this), this.getSoundVolume(), this.getVoicePitch());
                    } else if (this.aiTarget != null && this.random.nextInt(8) == 0) {
                        switch (this.random.nextInt(3)) {
                            case 0 -> attackEntityWithAmmo(this.aiTarget);
                            case 1 -> attackEntityWithHeavyAmmo(this.aiTarget);
                            default -> doHurtTarget(this.aiTarget);
                        }
                    }
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
                            applyParticleEmotion(32);
                            if (this.random.nextInt(4) == 0) {
                                this.pushAITarget(null);
                                this.playSound(this.getCustomSound(5, this), this.getSoundVolume(),
                                        this.getVoicePitch());
                            }
                            break;
                        default:
                            switch (this.random.nextInt(5)) {
                                case 1 -> applyParticleEmotion(30);
                                case 2 -> applyParticleEmotion(2); // panic
                                case 4 -> applyParticleEmotion(3); // ?
                                default -> applyParticleEmotion(0); // sweat
                            }
                            break;
                    }
                }
                break;
            default: // exhausted
                if (body == getSensitiveBody()) {
                    this.setStateEmotion(ID.S.Emotion, ID.Emotion.SHY, true);
                    applyParticleEmotion(6); // angry
                    this.addMorale((baseMorale * 10 + this.random.nextInt(baseMorale * 5 + 1)) * -1);
                    this.pushAITarget(null);
                    this.playSound(this.getCustomSound(5, this), this.getSoundVolume(), this.getVoicePitch());
                    if (this.aiTarget != null && this.random.nextInt(3) == 0) {
                        switch (this.random.nextInt(3)) {
                            case 0 -> attackEntityWithAmmo(this.aiTarget);
                            case 1 -> attackEntityWithHeavyAmmo(this.aiTarget);
                            default -> doHurtTarget(this.aiTarget);
                        }
                    }
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            setStateEmotion(ID.S.Emotion, ID.Emotion.T_T, true);
                            if (this.random.nextInt(3) == 0)
                                applyParticleEmotion(6);
                            else
                                applyParticleEmotion(32);
                            if (this.random.nextInt(2) == 0) {
                                this.pushAITarget(null);
                                this.playSound(this.getCustomSound(5, this), this.getSoundVolume(),
                                        this.getVoicePitch());
                            } else if (this.aiTarget != null && this.random.nextInt(5) == 0) {
                                switch (this.random.nextInt(3)) {
                                    case 0 -> attackEntityWithAmmo(this.aiTarget);
                                    case 1 -> attackEntityWithHeavyAmmo(this.aiTarget);
                                    default -> doHurtTarget(this.aiTarget);
                                }
                            }
                            break;
                        default:
                            switch (this.random.nextInt(5)) {
                                case 1 -> applyParticleEmotion(8); // cry
                                case 2 -> applyParticleEmotion(2); // panic
                                case 3 -> applyParticleEmotion(20); // orz
                                case 4 -> applyParticleEmotion(5); // ...
                                default -> applyParticleEmotion(34); // lll
                            }
                            break;
                    }
                }
                break;
        }
    }

    /**
     * Reaction when stranger caresses the ship.
     */
    public void reactionStranger() {
        int body = this.getBodyIDFromHeight(this.getHitHeight());

        if (body == getSensitiveBody()) {
            this.setStateEmotion(ID.S.Emotion, ID.Emotion.ANGRY, true);
            if (this.random.nextInt(2) == 0)
                applyParticleEmotion(6);
            else
                applyParticleEmotion(22); // x
            if (this.random.nextInt(2) == 0) {
                this.pushAITarget(null);
                this.playSound(this.getCustomSound(5, this), this.getSoundVolume(), this.getVoicePitch());
            } else if (this.aiTarget != null && this.random.nextInt(4) == 0) {
                switch (this.random.nextInt(3)) {
                    case 0 -> attackEntityWithAmmo(this.aiTarget);
                    case 1 -> attackEntityWithHeavyAmmo(this.aiTarget);
                    default -> doHurtTarget(this.aiTarget);
                }
            }
        } else {
            this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
            switch (body) {
                case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                    if (this.random.nextInt(2) == 0)
                        applyParticleEmotion(6);
                    else
                        applyParticleEmotion(5); // ...
                    if (this.random.nextInt(4) == 0) {
                        this.pushAITarget(null);
                        this.playSound(this.getCustomSound(5, this), this.getSoundVolume(), this.getVoicePitch());
                    } else if (this.aiTarget != null && this.random.nextInt(8) == 0) {
                        switch (this.random.nextInt(3)) {
                            case 0 -> attackEntityWithAmmo(this.aiTarget);
                            case 1 -> attackEntityWithHeavyAmmo(this.aiTarget);
                            default -> doHurtTarget(this.aiTarget);
                        }
                    }
                    break;
                default:
                    switch (this.random.nextInt(7)) {
                        case 1 -> applyParticleEmotion(9); // hungry
                        case 2 -> applyParticleEmotion(2); // panic
                        case 3 -> applyParticleEmotion(20); // orz
                        case 4 -> applyParticleEmotion(8); // cry
                        case 5 -> applyParticleEmotion(0); // sweat
                        default -> applyParticleEmotion(34); // lll
                    }
                    break;
            }
        }
    }

    /**
     * Reaction emotes when attacking.
     */
    public void reactionAttack() {
        if (BuffHelper.getMoraleLevel(this.getMorale()) == ID.Morale.Excited) {
            this.setStateEmotion(ID.S.Emotion, ID.Emotion.XD, true);
            switch (this.random.nextInt(8)) {
                case 1 -> applyParticleEmotion(33); // :p
                case 2 -> applyParticleEmotion(17); // gg
                case 3 -> applyParticleEmotion(19); // lick
                case 4 -> applyParticleEmotion(16); // ha
                default -> applyParticleEmotion(7); // note
            }
        } else {
            switch (this.random.nextInt(8)) {
                case 1 -> applyParticleEmotion(14); // +_+
                case 2 -> applyParticleEmotion(30); // pif
                case 3 -> applyParticleEmotion(7); // note
                case 4 -> applyParticleEmotion(4); // !
                case 5 -> applyParticleEmotion(7); // note
                default -> applyParticleEmotion(6); // angry
            }
        }
    }

    /**
     * Reaction emotes when damaged.
     */
    public void reactionDamaged() {
        int body = this.getBodyIDFromHeight(this.getHitHeight());

        switch (BuffHelper.getMoraleLevel(this.getMorale())) {
            case ID.Morale.Excited, ID.Morale.Happy, ID.Morale.Normal:
                if (body == getSensitiveBody()) {
                    applyParticleEmotion(6);
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            applyParticleEmotion(6);
                            break;
                        default:
                            switch (this.random.nextInt(7)) {
                                case 1 -> applyParticleEmotion(30);
                                case 2 -> applyParticleEmotion(5);
                                case 3 -> applyParticleEmotion(2);
                                case 4 -> applyParticleEmotion(3);
                                default -> applyParticleEmotion(8);
                            }
                            break;
                    }
                }
                break;
            default: // tired, exhausted
                if (body == getSensitiveBody()) {
                    applyParticleEmotion(10);
                } else {
                    switch (body) {
                        case ID.Body.UBelly, ID.Body.Butt, ID.Body.Chest, ID.Body.Face:
                            applyParticleEmotion(10);
                            break;
                        default:
                            switch (this.random.nextInt(7)) {
                                case 1 -> applyParticleEmotion(30);
                                case 2 -> applyParticleEmotion(5);
                                case 3 -> applyParticleEmotion(2);
                                case 4 -> applyParticleEmotion(3);
                                case 5 -> applyParticleEmotion(0);
                                default -> applyParticleEmotion(8);
                            }
                            break;
                    }
                }
                break;
        }
    }

    /**
     * Reaction emotes when idle.
     */
    public void reactionIdle() {
        switch (BuffHelper.getMoraleLevel(this.getMorale())) {
            case ID.Morale.Excited, ID.Morale.Happy:
                if (this.getStateFlag(ID.F.IsMarried) && this.random.nextInt(2) == 0) {
                    if (this.random.nextInt(3) == 1) {
                        applyParticleEmotion(31);
                    } else {
                        applyParticleEmotion(15);
                    }
                    return;
                }
                switch (this.random.nextInt(10)) {
                    case 1 -> applyParticleEmotion(33);
                    case 2 -> applyParticleEmotion(17);
                    case 3 -> applyParticleEmotion(19);
                    case 4 -> applyParticleEmotion(9);
                    case 5 -> applyParticleEmotion(1);
                    case 6 -> applyParticleEmotion(15);
                    case 7 -> applyParticleEmotion(16);
                    case 8 -> applyParticleEmotion(14);
                    default -> applyParticleEmotion(7);
                }
                break;
            case ID.Morale.Normal:
                if (this.getStateFlag(ID.F.IsMarried) && this.random.nextInt(2) == 0) {
                    if (this.random.nextInt(3) == 1) {
                        applyParticleEmotion(1);
                    } else {
                        applyParticleEmotion(15);
                    }
                    return;
                }
                switch (this.random.nextInt(8)) {
                    case 1 -> applyParticleEmotion(11);
                    case 2 -> applyParticleEmotion(3);
                    case 3 -> applyParticleEmotion(13);
                    case 4 -> applyParticleEmotion(9);
                    case 5 -> applyParticleEmotion(18);
                    case 7 -> applyParticleEmotion(16);
                    default -> applyParticleEmotion(29);
                }
                break;
            default: // tired, exhausted
                switch (this.random.nextInt(8)) {
                    case 1 -> applyParticleEmotion(0);
                    case 2 -> applyParticleEmotion(2);
                    case 3 -> applyParticleEmotion(3);
                    case 4 -> applyParticleEmotion(8);
                    case 5 -> applyParticleEmotion(10);
                    case 6 -> applyParticleEmotion(20);
                    default -> applyParticleEmotion(32);
                }
                break;
        }
    }

    /**
     * Reaction emotes on command.
     */
    public void reactionCommand() {
        switch (BuffHelper.getMoraleLevel(this.getMorale())) {
            case ID.Morale.Excited, ID.Morale.Happy, ID.Morale.Normal:
                switch (this.random.nextInt(7)) {
                    case 1 -> applyParticleEmotion(21); // o
                    case 2 -> applyParticleEmotion(4); // !
                    case 3 -> applyParticleEmotion(14); // +_+
                    case 4 -> applyParticleEmotion(11); // find
                    default -> applyParticleEmotion(13); // nod
                }
                break;
            default: // tired, exhausted
                switch (this.random.nextInt(8)) {
                    case 1 -> applyParticleEmotion(0);
                    case 2 -> applyParticleEmotion(33);
                    case 3 -> applyParticleEmotion(3);
                    case 5 -> applyParticleEmotion(10);
                    case 6 -> applyParticleEmotion(13);
                    default -> applyParticleEmotion(32);
                }
                break;
        }
    }

    /**
     * Reaction emotes on shock.
     */
    public void reactionShock() {
        switch (this.random.nextInt(8)) {
            case 1 -> applyParticleEmotion(0); // drop
            case 2 -> applyParticleEmotion(8); // cry
            case 3 -> applyParticleEmotion(4); // !
            default -> applyParticleEmotion(12); // omg
        }
    }

    // ========== Batch 4: Coordinate-based Heavy Attack ==========

    /**
     * Heavy ammo attack on coordinate position (for naval bombardment).
     */
    public boolean attackEntityWithHeavyAmmo(BlockPos target) {
        if (target == null)
            return false;
        if (!decrAmmoNum(1, this.getAmmoConsumption()))
            return false;

        addShipExp(ConfigHandler.expGain[2]);
        decrGrudgeNum(ConfigHandler.consumeGrudgeAction[1]);
        decrMorale(2);
        setCombatTick(this.tickCount);

        // play attack sound
        applySoundAtAttacker(2, this);
        applyParticleAtAttacker(2, this, this);

        float tarX = (float) target.getX();
        float tarY = (float) target.getY();
        float tarZ = (float) target.getZ();

        // get attack value
        float atk = getAttackBaseDamage(2, null);

        // spawn missile
        summonMissile(2, atk, tarX, tarY, tarZ, 1F);

        applyEmotesReaction(3);

        if (ConfigHandler.canFlare())
            flareTarget(target);

        return true;
    }
}
