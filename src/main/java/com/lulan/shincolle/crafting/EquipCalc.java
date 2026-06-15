package com.lulan.shincolle.crafting;

import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Equipment roll calculator for shipyard construction.
 * <p>
 * Roll system:
 * 1. rollEquipType() - determines equipment category via normal distribution
 * 2. rollEquipsOfTheType() - determines specific equipment within category
 * 3. getItemStackFromId() - converts equip ID to ItemStack
 */
public class EquipCalc {

    /**
     * Roll tables: {equipType, materialMean, modifiedMaterialType}
     */
    private static final List<int[]> EQUIP_SMALL = new ArrayList<>();
    private static final List<int[]> EQUIP_LARGE = new ArrayList<>();
    // Enchantment tables by type: 0=weapon, 1=armor, 2=misc
    // Each array contains vanilla enchantment IDs suitable for that equipment type.
    // In 1.20.1, we use Enchantment registry objects looked up by resource
    // location.
    private static final String[][] ENCHANT_TABLE = {
            // Weapon enchants
            {"minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods",
                    "minecraft:power", "minecraft:knockback", "minecraft:punch",
                    "minecraft:fire_aspect", "minecraft:flame", "minecraft:looting",
                    "minecraft:infinity", "minecraft:respiration"},
            // Armor enchants
            {"minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
                    "minecraft:projectile_protection", "minecraft:unbreaking", "minecraft:thorns",
                    "minecraft:mending"},
            // Misc enchants
            {"minecraft:feather_falling", "minecraft:aqua_affinity", "minecraft:depth_strider",
                    "minecraft:efficiency", "minecraft:unbreaking", "minecraft:lure",
                    "minecraft:mending"}
    };

    static {
        // Small build table
        EQUIP_SMALL.add(new int[]{ID.EquipType.ARMOR_LO, 80, 1});
        EQUIP_SMALL.add(new int[]{ID.EquipType.FLARE_LO, 80, 2});
        EQUIP_SMALL.add(new int[]{ID.EquipType.SEARCHLIGHT_LO, 80, 0});
        EQUIP_SMALL.add(new int[]{ID.EquipType.COMPASS_LO, 90, 0});
        EQUIP_SMALL.add(new int[]{ID.EquipType.GUN_LO, 100, 2});
        EQUIP_SMALL.add(new int[]{ID.EquipType.DRUM_LO, 120, 1});
        EQUIP_SMALL.add(new int[]{ID.EquipType.AMMO_LO, 120, 2});
        EQUIP_SMALL.add(new int[]{ID.EquipType.CANNON_SI, 128, 2});
        EQUIP_SMALL.add(new int[]{ID.EquipType.TORPEDO_LO, 160, 2});
        EQUIP_SMALL.add(new int[]{ID.EquipType.RADAR_LO, 200, 0});
        EQUIP_SMALL.add(new int[]{ID.EquipType.AIR_R_LO, 256, 3});
        EQUIP_SMALL.add(new int[]{ID.EquipType.CANNON_TW_LO, 320, 2});

        // Large build table
        EQUIP_LARGE.add(new int[]{ID.EquipType.ARMOR_HI, 500, 1});
        EQUIP_LARGE.add(new int[]{ID.EquipType.GUN_HI, 800, 2});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AMMO_HI, 1000, 2});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_R_HI, 1000, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.TORPEDO_HI, 1200, 2});
        EQUIP_LARGE.add(new int[]{ID.EquipType.TURBINE_LO, 1400, 0});
        EQUIP_LARGE.add(new int[]{ID.EquipType.CANNON_TW_HI, 1600, 2});
        EQUIP_LARGE.add(new int[]{ID.EquipType.RADAR_HI, 2000, 0});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_T_LO, 2400, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_F_LO, 2400, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_B_LO, 2400, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.CATAPULT_LO, 2800, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.TURBINE_HI, 3200, 0});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_T_HI, 3800, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_F_HI, 3800, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.AIR_B_HI, 3800, 3});
        EQUIP_LARGE.add(new int[]{ID.EquipType.CANNON_TR, 4400, 2});
        EQUIP_LARGE.add(new int[]{ID.EquipType.CATAPULT_HI, 5000, 3});
    }

    /**
     * Roll equipment type by total amount of materials.
     *
     * @param buildType 0=small, 1=large
     * @param matAmount int[4] of {grudge, abyssium, ammo, polymetal}
     * @return equipment type ID, or -1 if roll fails
     */
    public static int rollEquipType(int buildType, int[] matAmount, RandomSource random) {
        List<int[]> eqList = buildType == 0 ? EQUIP_SMALL : EQUIP_LARGE;
        int totalMats = matAmount[0] + matAmount[1] + matAmount[2] + matAmount[3];

        // Build probability map: equipType -> probability
        Map<Integer, Float> probList = new HashMap<>();

        for (int[] entry : eqList) {
            int meanNew;
            // Reduce mean by specific material amount
            if (entry[2] >= 0 && entry[2] <= 3) {
                meanNew = entry[1] - matAmount[entry[2]];
            } else {
                meanNew = entry[1];
            }

            // Distance from mean
            int meanDist = Mth.abs(totalMats - meanNew);

            // Scale small build to large resolution (256 range -> 4000 range)
            if (buildType == 0) {
                meanDist = (int) (meanDist * 15.625F);
            }

            float prob = CalcHelper.getNormDist(meanDist);
            probList.put(entry[0], prob);
            LogHelper.debug("DEBUG: roll equip type: ID " + entry[0] +
                    " MEAN(ORG) " + entry[1] + " MEAN(NEW) " + meanNew +
                    " MD " + meanDist + " PR " + prob);
        }

        return weightedRoll(probList, random);
    }

    /**
     * Roll a specific equipment of the given type.
     *
     * @param type      equipment type
     * @param totalMats total material amount
     * @param buildType 0=small, 1=large
     * @return resulting ItemStack, or ItemStack.EMPTY
     */
    public static ItemStack rollEquipsOfTheType(int type, int totalMats, int buildType, RandomSource random) {
        if (type == -1)
            return ItemStack.EMPTY;

        Map<Integer, Float> equipList = new HashMap<>();

        // Find all equipment of this type from EquipAttrsMisc
        for (Map.Entry<Integer, int[]> entry : Values.EquipAttrsMisc.entrySet()) {
            int eid = entry.getKey();
            int[] val = entry.getValue();

            if (val[ID.EquipMisc.RARE_TYPE] == type) {
                int totalMat = totalMats;
                // Scale small build resolution
                if (buildType == 0) {
                    totalMat = (int) (totalMats * 15.625F);
                }

                int meanDist = Mth.abs(totalMat - val[ID.EquipMisc.RARE_MEAN]);
                float prob = CalcHelper.getNormDist(meanDist);
                equipList.put(eid, prob);
                LogHelper.debug("DEBUG: roll equip: ID " + eid +
                        " MEAN " + val[ID.EquipMisc.RARE_MEAN] +
                        " MD " + meanDist + " PR " + prob);
            }
        }

        int rollResult = weightedRoll(equipList, random);

        // Calculate enchant level based on total materials
        int enchLv = 0;
        if (buildType == 0) { // small: max 256
            if (totalMats > 220)
                enchLv = 3;
            else if (totalMats > 200)
                enchLv = 2;
            else if (totalMats > 180)
                enchLv = 1;
        } else { // large: max 4000
            if (totalMats > 3500)
                enchLv = 3;
            else if (totalMats > 3000)
                enchLv = 2;
            else if (totalMats > 2000)
                enchLv = 1;
        }

        return getItemStackFromId(rollResult, enchLv);
    }

    /**
     * Weighted random roll from probability map.
     *
     * @return the key that was rolled, or -1
     */
    private static int weightedRoll(Map<Integer, Float> probList, RandomSource random) {
        float totalProb = 0F;
        for (float p : probList.values()) {
            totalProb += p;
        }

        if (totalProb <= 0F)
            return -1;

        float roll = random.nextFloat() * totalProb;
        float sumProb = 0.0125F; // small offset to prevent float comparison bug

        for (Map.Entry<Integer, Float> entry : probList.entrySet()) {
            sumProb += entry.getValue();
            if (sumProb > roll) {
                return entry.getKey();
            }
        }

        return -1;
    }

    /**
     * Convert equipment ID to ItemStack.
     * EquipID = EquipType + EquipSubID * 100
     */
    private static ItemStack getItemStackFromId(int itemID, int enchLv) {
        if (itemID == -1)
            return ItemStack.EMPTY;

        int itemType = itemID % 100; // equip type
        int itemMeta = itemID / 100; // equip sub ID (variant)
        int enchType = 0; // enchant type: 0=weapon, 1=armor, 2=misc

        ItemStack item;
        switch (itemType) {
            case ID.EquipType.CANNON_SI:
            case ID.EquipType.CANNON_TW_LO:
            case ID.EquipType.CANNON_TW_HI:
            case ID.EquipType.CANNON_TR:
                item = new ItemStack(ModItems.EQUIP_CANNON.get());

                break;
            case ID.EquipType.GUN_LO:
            case ID.EquipType.GUN_HI:
                item = new ItemStack(ModItems.EQUIP_MACHINEGUN.get());

                break;
            case ID.EquipType.TORPEDO_LO:
            case ID.EquipType.TORPEDO_HI:
                item = new ItemStack(ModItems.EQUIP_TORPEDO.get());

                break;
            case ID.EquipType.AIR_T_LO:
            case ID.EquipType.AIR_T_HI:
            case ID.EquipType.AIR_F_LO:
            case ID.EquipType.AIR_F_HI:
            case ID.EquipType.AIR_B_LO:
            case ID.EquipType.AIR_B_HI:
            case ID.EquipType.AIR_R_LO:
            case ID.EquipType.AIR_R_HI:
                item = new ItemStack(ModItems.EQUIP_AIRPLANE.get());

                break;
            case ID.EquipType.RADAR_LO:
            case ID.EquipType.RADAR_HI:
                item = new ItemStack(ModItems.EQUIP_RADAR.get());
                enchType = 2;
                break;
            case ID.EquipType.TURBINE_LO:
            case ID.EquipType.TURBINE_HI:
                item = new ItemStack(ModItems.EQUIP_TURBINE.get());
                enchType = 2;
                break;
            case ID.EquipType.ARMOR_LO:
            case ID.EquipType.ARMOR_HI:
                item = new ItemStack(ModItems.EQUIP_ARMOR.get());
                enchType = 1;
                break;
            case ID.EquipType.CATAPULT_LO:
            case ID.EquipType.CATAPULT_HI:
                item = new ItemStack(ModItems.EQUIP_CATAPULT.get());
                enchType = 2;
                break;
            case ID.EquipType.DRUM_LO:
                item = new ItemStack(ModItems.EQUIP_DRUM.get());
                enchType = 2;
                break;
            case ID.EquipType.COMPASS_LO:
                item = new ItemStack(ModItems.EQUIP_COMPASS.get());
                enchType = 2;
                break;
            case ID.EquipType.FLARE_LO:
                item = new ItemStack(ModItems.EQUIP_FLARE.get());
                enchType = 2;
                break;
            case ID.EquipType.SEARCHLIGHT_LO:
                item = new ItemStack(ModItems.EQUIP_SEARCHLIGHT.get());
                enchType = 2;
                break;
            case ID.EquipType.AMMO_LO:
            case ID.EquipType.AMMO_HI:
                item = new ItemStack(ModItems.EQUIP_AMMO.get());

                break;
            default:
                return ItemStack.EMPTY;
        }

        // Set the equipment variant/meta from the sub ID
        if (!item.isEmpty() && item.getItem() instanceof BasicEquip) {
            BasicEquip.setEquipMeta(item, itemMeta);
        }

        // Apply random enchantments based on enchant level
        if (enchLv > 0) {
            applyRandomEnchantToEquip(item, enchType, enchLv);
        }

        LogHelper.debug("DEBUG: equip calc: get itemstack: type=" + itemType + " meta=" + itemMeta + " enchLv=" + enchLv
                + " item=" + item);
        return item;
    }

    /**
     * Apply random enchantments to equipment based on level and type.
     * <p>
     * enchLv determines the number of enchantments rolled:
     * 1: 40% chance of 1 enchant
     * 2: 30% chance of 1, 30% chance of 2
     * 3: 30% chance of 1, 30% chance of 2, 20% chance of 3
     *
     * @param stack    the equipment ItemStack to enchant
     * @param enchType 0=weapon, 1=armor, 2=misc
     * @param enchLv   enchantment level (1-3)
     */
    private static void applyRandomEnchantToEquip(ItemStack stack, int enchType, int enchLv) {
        if (stack.isEmpty() || enchLv <= 0)
            return;
        if (enchType < 0 || enchType >= ENCHANT_TABLE.length)
            return;

        RandomSource rand = RandomSource.create();
        int enchNum = 0;
        int ranNum = rand.nextInt(10);

        enchNum = switch (enchLv) {
            case 1 -> ranNum > 5 ? 1 : 0;
            case 2 -> ranNum > 6 ? 2 : ranNum > 3 ? 1 : 0;
            case 3 -> ranNum > 7 ? 3 : ranNum > 4 ? 2 : ranNum > 1 ? 1 : 0;
            default -> enchNum;
        };

        if (enchNum <= 0)
            return;

        String[] enchIds = ENCHANT_TABLE[enchType];
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchMap = new java.util.HashMap<>();

        for (int i = 0; i < enchNum; i++) {
            String enchId = enchIds[rand.nextInt(enchIds.length)];
            net.minecraft.resources.ResourceLocation enchRL = new net.minecraft.resources.ResourceLocation(enchId);
            net.minecraft.world.item.enchantment.Enchantment ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                    .getValue(enchRL);

            if (ench != null) {
                if (enchMap.containsKey(ench)) {
                    // Same enchant rolled again: increase level (capped at max)
                    int lv = enchMap.get(ench) + 1;
                    if (lv > ench.getMaxLevel())
                        lv = ench.getMaxLevel();
                    enchMap.put(ench, lv);
                } else {
                    enchMap.put(ench, 1);
                }
            }
        }

        // Apply enchantments to the item
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(enchMap, stack);
    }

    /**
     * Calculate equip stats with enchantment effect.
     *
     * @param equipType equipment type (1=weapon, 2=armor, 3=misc)
     * @param raw       raw equipment stats
     * @param enchant   enchantment effect values
     * @return enchanted equipment stats
     */
    public static float[] calcEquipStatWithEnchant(int equipType, float[] raw, float[] enchant) {
        float[] newstat = new float[Attrs.AttrsLength];
        float modTemp;

        // HP
        newstat[ID.Attrs.HP] = raw[ID.Attrs.HP] * (1F + enchant[ID.Attrs.HP]);

        // ATK (weapon only)
        modTemp = equipType == 1 ? 1F + enchant[ID.Attrs.ATK_L] : 1F;
        newstat[ID.Attrs.ATK_L] = raw[ID.Attrs.ATK_L] * modTemp;
        newstat[ID.Attrs.ATK_H] = raw[ID.Attrs.ATK_H] * modTemp;
        newstat[ID.Attrs.ATK_AL] = raw[ID.Attrs.ATK_AL] * modTemp;
        newstat[ID.Attrs.ATK_AH] = raw[ID.Attrs.ATK_AH] * modTemp;

        // DEF (armor only)
        modTemp = equipType == 2 ? 1F + enchant[ID.Attrs.DEF] : 1F;
        newstat[ID.Attrs.DEF] = raw[ID.Attrs.DEF] * modTemp;

        // SPD
        newstat[ID.Attrs.SPD] = raw[ID.Attrs.SPD] * (1F + enchant[ID.Attrs.SPD]);

        // MOV (negative: reduce, positive: increase)
        if (raw[ID.Attrs.MOV] < 0F) {
            modTemp = 1F - enchant[ID.Attrs.MOV];
            if (modTemp < 0F)
                modTemp = 0F;
        } else {
            modTemp = 1F + enchant[ID.Attrs.MOV];
        }
        newstat[ID.Attrs.MOV] = raw[ID.Attrs.MOV] * modTemp;

        // Range
        newstat[ID.Attrs.HIT] = raw[ID.Attrs.HIT] * (1F + enchant[ID.Attrs.HIT]);

        // CRI
        newstat[ID.Attrs.CRI] = raw[ID.Attrs.CRI] * (1F + enchant[ID.Attrs.CRI]);

        // DHIT
        newstat[ID.Attrs.DHIT] = raw[ID.Attrs.DHIT] * (1F + enchant[ID.Attrs.DHIT]);

        // THIT
        newstat[ID.Attrs.THIT] = raw[ID.Attrs.THIT] * (1F + enchant[ID.Attrs.THIT]);

        // MISS
        newstat[ID.Attrs.MISS] = raw[ID.Attrs.MISS] * (1F + enchant[ID.Attrs.MISS]);

        // AA
        newstat[ID.Attrs.AA] = raw[ID.Attrs.AA] * (1F + enchant[ID.Attrs.AA]);

        // ASM
        newstat[ID.Attrs.ASM] = raw[ID.Attrs.ASM] * (1F + enchant[ID.Attrs.ASM]);

        // DODGE (negative: reduce, positive: increase)
        if (raw[ID.Attrs.DODGE] < 0F) {
            modTemp = 1F - enchant[ID.Attrs.DODGE];
            if (modTemp < 0F)
                modTemp = 0F;
        } else {
            modTemp = 1F + enchant[ID.Attrs.DODGE];
        }
        newstat[ID.Attrs.DODGE] = raw[ID.Attrs.DODGE] * modTemp;

        // XP gain (weapon only)
        newstat[ID.Attrs.XP] = equipType == 1 ? raw[ID.Attrs.XP] + enchant[ID.Attrs.XP] : raw[ID.Attrs.XP];

        // Grudge gain (non-weapon only)
        newstat[ID.Attrs.GRUDGE] = equipType != 1 ? raw[ID.Attrs.GRUDGE] + enchant[ID.Attrs.GRUDGE]
                : raw[ID.Attrs.GRUDGE];

        // Ammo gain (weapon only)
        newstat[ID.Attrs.AMMO] = equipType == 1 ? raw[ID.Attrs.AMMO] + enchant[ID.Attrs.AMMO] : raw[ID.Attrs.AMMO];

        // HP restore (non-weapon only)
        newstat[ID.Attrs.HPRES] = equipType != 1 ? raw[ID.Attrs.HPRES] + enchant[ID.Attrs.HPRES] : raw[ID.Attrs.HPRES];

        // Knockback resist (non-weapon only)
        newstat[ID.Attrs.KB] = equipType != 1 ? raw[ID.Attrs.KB] + enchant[ID.Attrs.KB] : raw[ID.Attrs.KB];

        return newstat;
    }
}
