package com.lulan.shincolle.item;

import com.lulan.shincolle.capability.CapaShipSavedValues;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.capability.CapaTeitokuProvider;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.init.ModEntities;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ship Spawn Egg - used to spawn ship entities.
 * Ship class is stored in NBT tag "ShipClass".
 * <p>
 * Egg types (determined by presence of NBT data):
 * - Construction egg: has "BuildType" (0=small, 1=large), "Grudge", "Abyssium",
 * etc.
 * Created by shipyard tile entities after rolling.
 * - Saved egg: has "StateMinor", "StateFlag", etc.
 * Created when a ship entity dies and drops itself as an egg.
 * <p>
 * ShipClass values:
 * 0-84 = friendly ship classes (see ID.ShipClass)
 * +2000 = hostile mob variant
 */
public class ShipSpawnEgg extends BasicItem {

    public static final String TAG_SHIP_CLASS = "ShipClass";
    /**
     * Legacy tag name, kept for backward compatibility
     */
    public static final String TAG_SHIP_TYPE = "ShipType";
    public static final int MOB_OFFSET = 2000;
    private static final String TAG_STATE_MINOR = "StateMinor";
    private static final String TAG_CUSTOM_NAME = "CustomName";
    private static final String TAG_CUSTOM_NAME_LEGACY = "customname";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_OWNER_NAME_LEGACY = "ownername";
    private static final String CHAT_LEVEL_FAIL_KEY = "chat.shincolle:levelfail";
    private static Map<Integer, RegistryObject<? extends EntityType<?>>> ENTITY_MAP;

    public ShipSpawnEgg() {
        super(new Properties().stacksTo(1));
    }

    /**
     * Get the ship class from an ItemStack's NBT
     */
    public static int getShipClass(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains(TAG_SHIP_CLASS)) {
                return tag.getInt(TAG_SHIP_CLASS);
            }
            // legacy fallback
            if (tag.contains(TAG_SHIP_TYPE)) {
                int legacyType = tag.getInt(TAG_SHIP_TYPE);

                // [PORT] 1.10.2 -> 1.20.1: legacy ShipType often stored item metadata
                // (ShipClass + 2, hostile = ShipClass + 2002). Convert to modern ShipClass.
                if (legacyType <= 1) {
                    return legacyType;
                }

                int maxLegacyMeta = ID.ShipClass.NorthlandHime + 2;
                int maxLegacyMobMeta = MOB_OFFSET + maxLegacyMeta;
                boolean isLegacyMeta = legacyType <= maxLegacyMeta
                        || legacyType >= MOB_OFFSET + 2 && legacyType <= maxLegacyMobMeta;

                if (isLegacyMeta) {
                    return legacyType - 2;
                }

                return legacyType;
            }
        }
        return 0;
    }

    // ===== Ship Class NBT Accessors =====

    private static boolean hasSpecificShipClassTag(CompoundTag tag) {
        return tag != null && (tag.contains(TAG_SHIP_CLASS) || tag.contains(TAG_SHIP_TYPE));
    }

    /**
     * Set the ship class on an ItemStack's NBT
     */
    public static void setShipClass(ItemStack stack, int shipClass) {
        stack.getOrCreateTag().putInt(TAG_SHIP_CLASS, shipClass);
    }

    /**
     * @deprecated Use getShipClass instead
     */
    @Deprecated
    public static int getShipType(ItemStack stack) {
        return getShipClass(stack);
    }

    /**
     * @deprecated Use setShipClass instead
     */
    @Deprecated
    public static void setShipType(ItemStack stack, int type) {
        setShipClass(stack, type);
    }

    /**
     * Get the texture icon index for an ItemStack, accounting for egg type.
     * Construction eggs (with BuildType tag) return 0 (small) or 1 (large).
     * Specific ship eggs return a ship-type icon via getIconFromShipClass().
     */
    public static int getEggIcon(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (hasSpecificShipClassTag(tag)) {
            return getIconFromShipClass(getShipClass(stack));
        }
        if (tag != null && tag.contains("BuildType")) {
            return tag.getByte("BuildType") == 0 ? 0 : 1;
        }
        return 0;
    }

    // ===== Right Click on Block: Spawn Entity =====

    private static boolean consumeSavedEggXpCost(Player player, CompoundTag nbt) {
        if (player == null || nbt == null) {
            return true;
        }

        if (player.getAbilities().instabuild || !nbt.contains(TAG_STATE_MINOR)) {
            return true;
        }

        int[] attrs = nbt.getIntArray(TAG_STATE_MINOR);
        if (attrs.length <= 0) {
            return true;
        }

        int shipLevel = attrs[0] / 3; // StateMinor[0] = ShipLevel (raw level)
        if (player.experienceLevel < shipLevel) {
            player.sendSystemMessage(Component.translatable(CHAT_LEVEL_FAIL_KEY));
            return false;
        }

        player.giveExperienceLevels(-shipLevel);
        return true;
    }

    private static void applyEggHoverName(BasicEntityShip ship, ItemStack eggStack) {
        if (eggStack.hasCustomHoverName()) {
            ship.setCustomName(eggStack.getHoverName());
        }
    }

    private static void applyEggCustomName(BasicEntityShip ship, CompoundTag nbt) {
        Component resolvedName = resolveEggCustomName(nbt);
        if (resolvedName != null) {
            ship.setCustomName(resolvedName);
        }
    }

    private static Component resolveEggCustomName(CompoundTag nbt) {
        if (nbt == null) {
            return null;
        }

        if (nbt.contains(TAG_CUSTOM_NAME_LEGACY)) {
            String legacyName = nbt.getString(TAG_CUSTOM_NAME_LEGACY);
            if (!legacyName.isEmpty()) {
                return Component.literal(legacyName);
            }
        }

        if (!nbt.contains(TAG_CUSTOM_NAME)) {
            return null;
        }

        String jsonName = nbt.getString(TAG_CUSTOM_NAME);
        if (jsonName.isEmpty()) {
            return null;
        }

        try {
            Component parsed = Component.Serializer.fromJson(jsonName);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception e) {
            // [PORT] 1.10.2 -> 1.20.1: tolerate malformed or non-JSON CustomName data.
        }

        return Component.literal(jsonName);
    }

    // ===== Right Click on Block: Spawn Entity =====

    private static String resolveEggOwnerName(CompoundTag nbt) {
        if (nbt == null) {
            return "";
        }

        if (nbt.contains(TAG_OWNER_NAME)) {
            String ownerName = nbt.getString(TAG_OWNER_NAME);
            if (!ownerName.isEmpty()) {
                return ownerName;
            }
        }

        if (nbt.contains(TAG_OWNER_NAME_LEGACY)) {
            return nbt.getString(TAG_OWNER_NAME_LEGACY);
        }

        return "";
    }

    private static void bootstrapFreshSpawnCombatState(BasicEntityShip ship, CompoundTag nbt) {
        if (nbt != null && nbt.contains(TAG_STATE_MINOR)) {
            return;
        }

        if (ship.getStateMinor(ID.M.NumGrudge) <= 0) {
            ship.setStateMinor(ID.M.NumGrudge, Values.N.BaseGrudge);
        }
        if (ship.getStateMinor(ID.M.NumAmmoLight) <= 0) {
            ship.setStateMinor(ID.M.NumAmmoLight, Values.N.BaseLightAmmo);
        }
        if (ship.getStateMinor(ID.M.NumAmmoHeavy) <= 0) {
            ship.setStateMinor(ID.M.NumAmmoHeavy, Values.N.BaseHeavyAmmo);
        }

        if (ship.getStateMinor(ID.M.NumGrudge) > 0 && ship.getStateFlag(ID.F.NoFuel)) {
            ship.setStateFlag(ID.F.NoFuel, false);
        }
    }

    /**
     * Create a ship entity from a ship class ID.
     * Uses the ENTITY_MAP to look up the EntityType, then creates an instance.
     */
    private static Entity createShipFromClass(ServerLevel level, int shipClass) {
        if (ENTITY_MAP == null) {
            buildEntityMap();
        }

        RegistryObject<? extends EntityType<?>> registryObj = ENTITY_MAP.get(shipClass);
        if (registryObj == null || !registryObj.isPresent()) {
            LogHelper.warn("No entity registered for ship class: " + shipClass);
            return null;
        }

        EntityType<?> entityType = registryObj.get();
        return entityType.create(level);
    }

    /**
     * Get the EntityType for a ship class ID.
     * Used by the guidebook entity gallery to create preview entities.
     */
    public static EntityType<?> getEntityTypeForClass(int shipClass) {
        if (ENTITY_MAP == null) {
            buildEntityMap();
        }

        RegistryObject<? extends EntityType<?>> registryObj = ENTITY_MAP.get(shipClass);
        if (registryObj != null && registryObj.isPresent()) {
            return registryObj.get();
        }
        return null;
    }

    /**
     * Initialize a BasicEntityShip from spawn egg NBT data.
     */
    private static void initShipFromEgg(BasicEntityShip ship, ItemStack eggStack, Player player) {
        CompoundTag nbt = eggStack.getTag();

        // [PORT] 1.10.2 -> 1.20.1: ensure spawned ship is tamed and linked to spawner.
        if (player != null) {
            ship.tame(player);
            ship.setOwnerUUID(player.getUUID());
        }
        ship.setTarget(null);

        // set owner
        CapaTeitoku capa = player != null ? player.getCapability(CapaTeitokuProvider.CAPABILITY).orElse(null) : null;
        if (capa != null) {
            int playerUID = capa.getPlayerUID();
            if (playerUID > 0) {
                ship.setPlayerUID(playerUID);
            }
        }

        // load saved ship data (from death egg)
        if (nbt != null && nbt.contains("StateMinor")) {
            CapaShipSavedValues.loadNBTData(nbt, ship);
            if (ship.getPlayerUID() <= 0 && capa != null) {
                int playerUID = capa.getPlayerUID();
                if (playerUID > 0) {
                    ship.setPlayerUID(playerUID);
                }
            }
        } else {
            // construction egg: set initial level
            ship.setShipLevel(1, true);
        }

        // set can drop flag
        ship.setStateFlag(ID.F.CanDrop, true);
        ship.tickCount = 0;
    }

    /**
     * Build the mapping from ShipClass IDs to EntityType registry objects.
     * Includes both friendly and hostile (MOB_OFFSET) variants.
     */
    private static void buildEntityMap() {
        ENTITY_MAP = new HashMap<>();

        // --- Destroyers ---
        ENTITY_MAP.put((int) ID.ShipClass.DDI, ModEntities.DESTROYER_I);
        ENTITY_MAP.put((int) ID.ShipClass.DDRO, ModEntities.DESTROYER_RO);
        ENTITY_MAP.put((int) ID.ShipClass.DDHA, ModEntities.DESTROYER_HA);
        ENTITY_MAP.put((int) ID.ShipClass.DDNI, ModEntities.DESTROYER_NI);
        ENTITY_MAP.put((int) ID.ShipClass.DDShimakaze, ModEntities.DESTROYER_SHIMAKAZE);
        ENTITY_MAP.put((int) ID.ShipClass.DDAkatsuki, ModEntities.DESTROYER_AKATSUKI);
        ENTITY_MAP.put((int) ID.ShipClass.DDHibiki, ModEntities.DESTROYER_HIBIKI);
        ENTITY_MAP.put((int) ID.ShipClass.DDIkazuchi, ModEntities.DESTROYER_IKAZUCHI);
        ENTITY_MAP.put((int) ID.ShipClass.DDInazuma, ModEntities.DESTROYER_INAZUMA);

        // --- Light Cruisers ---
        ENTITY_MAP.put((int) ID.ShipClass.CLTenryuu, ModEntities.CL_TENRYUU);
        ENTITY_MAP.put((int) ID.ShipClass.CLTatsuta, ModEntities.CL_TATSUTA);

        // --- Heavy Cruisers ---
        ENTITY_MAP.put((int) ID.ShipClass.CAAtago, ModEntities.CA_ATAGO);
        ENTITY_MAP.put((int) ID.ShipClass.CATakao, ModEntities.CA_TAKAO);
        ENTITY_MAP.put((int) ID.ShipClass.CANE, ModEntities.CA_NE);
        ENTITY_MAP.put((int) ID.ShipClass.CARI, ModEntities.CA_RI);

        // --- Battleships ---
        ENTITY_MAP.put((int) ID.ShipClass.BBKongou, ModEntities.BB_KONGOU);
        ENTITY_MAP.put((int) ID.ShipClass.BBHiei, ModEntities.BB_HIEI);
        ENTITY_MAP.put((int) ID.ShipClass.BBHaruna, ModEntities.BB_HARUNA);
        ENTITY_MAP.put((int) ID.ShipClass.BBKirishima, ModEntities.BB_KIRISHIMA);
        ENTITY_MAP.put((int) ID.ShipClass.BBNagato, ModEntities.BB_NAGATO);
        ENTITY_MAP.put((int) ID.ShipClass.BBYamato, ModEntities.BB_YAMATO);
        ENTITY_MAP.put((int) ID.ShipClass.BBRE, ModEntities.BB_RE);
        ENTITY_MAP.put((int) ID.ShipClass.BBRU, ModEntities.BB_RU);
        ENTITY_MAP.put((int) ID.ShipClass.BBTA, ModEntities.BB_TA);

        // --- Carriers ---
        ENTITY_MAP.put((int) ID.ShipClass.CVAkagi, ModEntities.CV_AKAGI);
        ENTITY_MAP.put((int) ID.ShipClass.CVKaga, ModEntities.CV_KAGA);
        ENTITY_MAP.put((int) ID.ShipClass.CVWO, ModEntities.CV_WO);

        // --- Submarines ---
        ENTITY_MAP.put((int) ID.ShipClass.SSRo500, ModEntities.SS_RO500);
        ENTITY_MAP.put((int) ID.ShipClass.SSU511, ModEntities.SS_U511);
        ENTITY_MAP.put((int) ID.ShipClass.SSYO, ModEntities.SS_YO);
        ENTITY_MAP.put((int) ID.ShipClass.SSKA, ModEntities.SS_KA);
        ENTITY_MAP.put((int) ID.ShipClass.SSSO, ModEntities.SS_SO);

        // --- Transport ---
        ENTITY_MAP.put((int) ID.ShipClass.APWA, ModEntities.AP_WA);

        // --- Hime / Boss ---
        ENTITY_MAP.put((int) ID.ShipClass.AirfieldHime, ModEntities.AIRFIELD_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.BBHime, ModEntities.BB_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.CAHime, ModEntities.CA_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.CVHime, ModEntities.CV_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.CVWD, ModEntities.CV_WD);
        ENTITY_MAP.put((int) ID.ShipClass.DDHime, ModEntities.DD_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.HarbourHime, ModEntities.HARBOUR_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.IsolatedHime, ModEntities.ISOLATED_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.MidwayHime, ModEntities.MIDWAY_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.NorthernHime, ModEntities.NORTHERN_HIME);
        ENTITY_MAP.put((int) ID.ShipClass.SSNH, ModEntities.SSNH);
        ENTITY_MAP.put((int) ID.ShipClass.SSHime, ModEntities.SS_HIME);

        // --- Hostile Mob variants (ShipClass + MOB_OFFSET) ---
        ENTITY_MAP.put(ID.ShipClass.DDShimakaze + MOB_OFFSET, ModEntities.DESTROYER_SHIMAKAZE_MOB);
        ENTITY_MAP.put(ID.ShipClass.DDAkatsuki + MOB_OFFSET, ModEntities.DESTROYER_AKATSUKI_MOB);
        ENTITY_MAP.put(ID.ShipClass.DDHibiki + MOB_OFFSET, ModEntities.DESTROYER_HIBIKI_MOB);
        ENTITY_MAP.put(ID.ShipClass.DDIkazuchi + MOB_OFFSET, ModEntities.DESTROYER_IKAZUCHI_MOB);
        ENTITY_MAP.put(ID.ShipClass.DDInazuma + MOB_OFFSET, ModEntities.DESTROYER_INAZUMA_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBKongou + MOB_OFFSET, ModEntities.BB_KONGOU_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBHiei + MOB_OFFSET, ModEntities.BB_HIEI_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBHaruna + MOB_OFFSET, ModEntities.BB_HARUNA_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBKirishima + MOB_OFFSET, ModEntities.BB_KIRISHIMA_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBNagato + MOB_OFFSET, ModEntities.BB_NAGATO_MOB);
        ENTITY_MAP.put(ID.ShipClass.BBYamato + MOB_OFFSET, ModEntities.BB_YAMATO_MOB);
        ENTITY_MAP.put(ID.ShipClass.CLTenryuu + MOB_OFFSET, ModEntities.CL_TENRYUU_MOB);
        ENTITY_MAP.put(ID.ShipClass.CLTatsuta + MOB_OFFSET, ModEntities.CL_TATSUTA_MOB);
        ENTITY_MAP.put(ID.ShipClass.CAAtago + MOB_OFFSET, ModEntities.CA_ATAGO_MOB);
        ENTITY_MAP.put(ID.ShipClass.CATakao + MOB_OFFSET, ModEntities.CA_TAKAO_MOB);
        ENTITY_MAP.put(ID.ShipClass.CVAkagi + MOB_OFFSET, ModEntities.CV_AKAGI_MOB);
        ENTITY_MAP.put(ID.ShipClass.CVKaga + MOB_OFFSET, ModEntities.CV_KAGA_MOB);
        ENTITY_MAP.put(ID.ShipClass.SSRo500 + MOB_OFFSET, ModEntities.SS_RO500_MOB);
        ENTITY_MAP.put(ID.ShipClass.SSU511 + MOB_OFFSET, ModEntities.SS_U511_MOB);
    }

    /**
     * Get the texture icon index for a given ship class.
     * Used by ItemProperties to select model overrides.
     * Returns 0-10 matching the original texture mapping.
     */
    public static int getIconFromShipClass(int shipClass) {
        // Negative ship class = invalid / player morph class
        if (shipClass < 0)
            return 0;

        // Strip mob offset for texture lookup
        int baseClass = shipClass >= MOB_OFFSET ? shipClass - MOB_OFFSET : shipClass;

        switch (baseClass) {
            // DD - Destroyer (icon 2)
            case ID.ShipClass.DDI:
            case ID.ShipClass.DDRO:
            case ID.ShipClass.DDHA:
            case ID.ShipClass.DDNI:
            case ID.ShipClass.DDAkatsuki:
            case ID.ShipClass.DDHibiki:
            case ID.ShipClass.DDIkazuchi:
            case ID.ShipClass.DDInazuma:
            case ID.ShipClass.DDShimakaze:
                return 2;
            // CL - Light Cruiser (icon 3)
            case ID.ShipClass.CLTenryuu:
            case ID.ShipClass.CLTatsuta:
                return 3;
            // CA - Heavy Cruiser (icon 4)
            case ID.ShipClass.CAAtago:
            case ID.ShipClass.CATakao:
            case ID.ShipClass.CARI:
            case ID.ShipClass.CANE:
                return 4;
            // BB - Battleship (icon 5)
            case ID.ShipClass.BBRU:
            case ID.ShipClass.BBTA:
            case ID.ShipClass.BBRE:
            case ID.ShipClass.BBNagato:
            case ID.ShipClass.BBYamato:
            case ID.ShipClass.BBKongou:
            case ID.ShipClass.BBHiei:
            case ID.ShipClass.BBHaruna:
            case ID.ShipClass.BBKirishima:
                return 5;
            // AP - Transport (icon 6)
            case ID.ShipClass.APWA:
                return 6;
            // SS - Submarine (icon 7)
            case ID.ShipClass.SSKA:
            case ID.ShipClass.SSYO:
            case ID.ShipClass.SSSO:
            case ID.ShipClass.SSU511:
            case ID.ShipClass.SSRo500:
                return 7;
            // WD - Demon (icon 8)
            case ID.ShipClass.CVWD:
                return 8;
            // Hime - Princess (icon 9)
            case ID.ShipClass.CVHime:
            case ID.ShipClass.DDHime:
            case ID.ShipClass.CAHime:
            case ID.ShipClass.AirfieldHime:
            case ID.ShipClass.BBHime:
            case ID.ShipClass.HarbourHime:
            case ID.ShipClass.IsolatedHime:
            case ID.ShipClass.MidwayHime:
            case ID.ShipClass.NorthernHime:
            case ID.ShipClass.SSHime:
            case ID.ShipClass.SSNH:
                return 9;
            // CV - Carrier (icon 10)
            case ID.ShipClass.CVWO:
            case ID.ShipClass.CVKaga:
            case ID.ShipClass.CVAkagi:
                return 10;
            default:
                return 0;
        }
    }

    // ===== Entity Creation =====

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos blockPos = context.getClickedPos();
        Direction direction = context.getClickedFace();

        // check block editability
        assert player != null;
        if (!level.mayInteract(player, blockPos)) {
            return InteractionResult.FAIL;
        }

        // spawn position: on top of the clicked face
        BlockPos spawnPos = blockPos.relative(direction);
        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        CompoundTag nbt = stack.getTag();

        int shipClass;
        if (hasSpecificShipClassTag(nbt)) {
            shipClass = getShipClass(stack);
        } else {
            // [PORT] 1.10.2 -> 1.20.1: Random ship rolls happen dynamically if no ship class is present.
            int buildType = (nbt != null && nbt.contains("BuildType")) ? nbt.getByte("BuildType") : 0;
            int[] mats = new int[4];
            if (nbt != null) {
                mats[0] = nbt.getInt("Grudge");
                mats[1] = nbt.getInt("Abyssium");
                mats[2] = nbt.getInt("Ammo");
                mats[3] = nbt.getInt("Polymetal");
            }
            shipClass = com.lulan.shincolle.crafting.ShipCalc.rollShipType(buildType, mats, level.random);
        }

        // XP cost for saved eggs (eggs with stored ship data)
        if (!consumeSavedEggXpCost(player, nbt)) {
            return InteractionResult.FAIL;
        }

        // spawn entity
        ServerLevel serverLevel = (ServerLevel) level;
        Entity entity = createShipFromClass(serverLevel, shipClass);

        if (entity == null) {
            LogHelper.warn("Failed to create ship entity for class: " + shipClass);
            return InteractionResult.FAIL;
        }

        entity.moveTo(x, y, z, player.getYRot(), 0F);

        if (entity instanceof BasicEntityShip ship) {
            // init ship from egg data
            initShipFromEgg(ship, stack, player);

            // [PORT] Preserve legacy priority: renamed egg hover-name first, explicit NBT
            // name overrides afterward.
            applyEggHoverName(ship, stack);

            // set custom name if present
            applyEggCustomName(ship, nbt);

            // [PORT] Keep fresh-spawn ships combat-capable by seeding baseline resources
            // when no saved state is provided.
            bootstrapFreshSpawnCombatState(ship, nbt);

            level.addFreshEntity(ship);

            // recalc attributes
            ship.calcShipAttributes(31, true);
        } else if (entity instanceof BasicEntityShipHostile hostile) {
            // [PORT] 1.10.2 -> 1.20.1: keep hostile spawn silhouette larger than
            // regular ships by biasing hostile scale level away from 0.
            hostile.initAttrs(1 + level.random.nextInt(3));
            level.addFreshEntity(hostile);
            hostile.playAmbientSound();
        }

        // consume item in non-creative
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Override default instance so bare item stacks always have valid NBT.
     * This prevents a nameless/functionless "ship_spawn_egg" from appearing
     * in the creative search tab or via /give without NBT.
     * Default = small construction egg (BuildType=0).
     */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().putByte("BuildType", (byte) 0);
        return stack;
    }

    /**
     * Create an ItemStack with the specified ship class
     */
    public ItemStack createStack(int shipClass) {
        ItemStack stack = new ItemStack(this);
        setShipClass(stack, shipClass);
        return stack;
    }

    // ===== Entity Map =====

    /**
     * Create a construction egg ItemStack with the specified build type (0=small,
     * 1=large)
     */
    public ItemStack createConstructionStack(int buildType) {
        ItemStack stack = new ItemStack(this);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putByte("BuildType", (byte) buildType);
        return stack;
    }

    // ===== Description / Translation =====

    @Override
    public String getDescriptionId(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt != null) {
            // Specific ship egg: name based on ship class (priority over BuildType)
            // [PORT] 1.10.2 -> 1.20.1: shipyard result eggs may contain both BuildType and
            // ShipClass; ShipClass should drive individual egg display.
            if (hasSpecificShipClassTag(nbt)) {
                int shipClass = getShipClass(stack);
                if (shipClass >= 0) {
                    return "item.shincolle.ship_egg_" + (shipClass + 2);
                }
            }

            // Construction egg: name based on build type
            if (nbt.contains("BuildType")) {
                int buildType = nbt.getByte("BuildType");
                if (buildType == 0) {
                    return "item.shincolle.small_egg";
                } else {
                    return "item.shincolle.large_egg";
                }
            }
        }
        return "item.shincolle.ship_spawn_egg";
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack));
    }

    // ===== Tooltip =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag()) {
            CompoundTag nbt = stack.getTag();

            assert nbt != null;
            if (nbt.contains("StateMinor")) {
                // Saved ship egg: show level, name, owner
                int[] stateMinor = nbt.getIntArray("StateMinor");
                if (stateMinor.length > 0) {
                    int shipLevel = stateMinor[0]; // StateMinor[0] = ShipLevel (raw level)
                    tooltip.add(Component
                            .literal(ChatFormatting.AQUA + Component.translatable("gui.shincolle.eggText").getString()
                                    + " " + shipLevel));
                }
                Component customName = resolveEggCustomName(nbt);
                if (customName != null && !customName.getString().isEmpty()) {
                    tooltip.add(Component.literal(ChatFormatting.WHITE + customName.getString()));
                }
                String ownerName = resolveEggOwnerName(nbt);
                if (!ownerName.isEmpty()) {
                    tooltip.add(Component.literal(ChatFormatting.RED + ownerName));
                }
            } else if (nbt.contains("BuildType")) {
                // Construction egg: show material amounts
                int grudge = nbt.getInt("Grudge");
                int abyssium = nbt.getInt("Abyssium");
                int ammo = nbt.getInt("Ammo");
                int polymetal = nbt.getInt("Polymetal");

                tooltip.add(Component
                        .literal(ChatFormatting.WHITE + "" + grudge + " "
                                + Component.translatable("item.shincolle.grudge").getString()));
                tooltip.add(Component
                        .literal(ChatFormatting.RED + "" + abyssium + " "
                                + Component.translatable("item.shincolle.abyss_metal").getString()));
                tooltip.add(
                        Component.literal(ChatFormatting.GREEN + "" + ammo + " "
                                + Component.translatable("item.shincolle.ammo").getString()));
                tooltip.add(Component.literal(
                        ChatFormatting.AQUA + "" + polymetal + " "
                                + Component.translatable("item.shincolle.abyss_metal_1").getString()));
            }

            // Show ship class in advanced tooltip mode
            if (flag.isAdvanced() && nbt.contains(TAG_SHIP_CLASS)) {
                tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "ShipClass: " + nbt.getInt(TAG_SHIP_CLASS)));
            }
        }
    }
}
