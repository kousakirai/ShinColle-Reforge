package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipAttrs;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper for ship attribute buff calculations.
 * <p>
 * Attribute pipeline:
 * Raw (base + level + bonus) -> Equip -> Morale -> Potion -> Formation ->
 * Buffed
 * <p>
 * Ported from 1.10.2 CalcHelper / BuffHelper.
 */
public class BuffHelper {

    /**
     * Calculate raw attributes from base stats, level, and bonus points.
     * <p>
     * Formula per attribute (matching original 1.10.2):
     * HP = (base + (bonus + 1) * level * type) * configScale
     * ATK = (base + (bonus + 1) * level * 0.133 * type) * configScale
     * DEF = (base + (bonus + 1) * level * 0.00133 * type) * configScale
     * SPD = (base + (bonus + 1) * level * 0.004 * type) * configScale
     * MOV = (base + (bonus + 1) * level * 0.002 * type) * configScale
     * HIT = (base + (bonus + 1) * level * 0.02 * type) * configScale
     *
     * @param attrs     the ship attributes object
     * @param shipClass ship class ID (used to look up base stats from
     *                  Values.ShipAttrMap)
     * @param level     current ship level (0-150)
     */
    public static void updateAttrsRaw(AttrsAdv attrs, int shipClass, int level) {
        float[] getStat = Values.ShipAttrMap.get(shipClass);
        if (getStat == null) {
            getStat = Values.ShipAttrMap.get(0);
            if (getStat == null)
                return;
        }

        float[] raw = attrs.getAttrsRaw();
        byte[] bonus = attrs.getAttrsBonus();
        double[] scale = ConfigHandler.scaleShip;

        // type modifiers from ShipAttrMap indices 6-11
        float typeHP = getStat[ID.AttrsBase.modHP];
        float typeATK = getStat[ID.AttrsBase.modATK];
        float typeDEF = getStat[ID.AttrsBase.modDEF];
        float typeSPD = getStat[ID.AttrsBase.modSPD];
        float typeMOV = getStat[ID.AttrsBase.modMOV];
        float typeHIT = getStat[ID.AttrsBase.modHIT];

        float lv = (float) level;

        // HP = (base + (bonus + 1) * level * type) * configScale
        raw[ID.Attrs.HP] = (getStat[ID.AttrsBase.HP] + (bonus[ID.AttrsBase.HP] + 1F) * lv * typeHP)
                * (float) scale[ID.AttrsBase.HP];

        // DEF = (base + (bonus + 1) * level * 0.00133 * type) * configScale
        raw[ID.Attrs.DEF] = (getStat[ID.AttrsBase.DEF] + (bonus[ID.AttrsBase.DEF] + 1F) * lv * 0.00133F * typeDEF)
                * (float) scale[ID.AttrsBase.DEF];

        // SPD = (base + (bonus + 1) * level * 0.004 * type) * configScale
        raw[ID.Attrs.SPD] = (getStat[ID.AttrsBase.SPD] + (bonus[ID.AttrsBase.SPD] + 1F) * lv * 0.004F * typeSPD)
                * (float) scale[ID.AttrsBase.SPD];

        // MOV = (base + (bonus + 1) * level * 0.002 * type) * configScale
        raw[ID.Attrs.MOV] = (getStat[ID.AttrsBase.MOV] + (bonus[ID.AttrsBase.MOV] + 1F) * lv * 0.002F * typeMOV)
                * (float) scale[ID.AttrsBase.MOV];

        // HIT = (base + (bonus + 1) * level * 0.02 * type) * configScale
        raw[ID.Attrs.HIT] = (getStat[ID.AttrsBase.HIT] + (bonus[ID.AttrsBase.HIT] + 1F) * lv * 0.02F * typeHIT)
                * (float) scale[ID.AttrsBase.HIT];

        // ATK base = base + (bonus + 1) * level * 0.133 * type
        float baseATK = getStat[ID.AttrsBase.ATK] + (bonus[ID.AttrsBase.ATK] + 1F) * lv * 0.133F * typeATK;
        raw[ID.Attrs.ATK_L] = baseATK * (float) scale[ID.AttrsBase.ATK];
        raw[ID.Attrs.ATK_H] = baseATK * 3F * (float) scale[ID.AttrsBase.ATK];
        raw[ID.Attrs.ATK_AL] = baseATK * (float) scale[ID.AttrsBase.ATK];
        raw[ID.Attrs.ATK_AH] = baseATK * 3F * (float) scale[ID.AttrsBase.ATK];

        // secondary stats: remain at 0 in raw (come from equipment/potions/formation)
        raw[ID.Attrs.CRI] = 0F;
        raw[ID.Attrs.DHIT] = 0F;
        raw[ID.Attrs.THIT] = 0F;
        raw[ID.Attrs.MISS] = 0F;
        raw[ID.Attrs.AA] = 0F;
        raw[ID.Attrs.ASM] = 0F;
        raw[ID.Attrs.DODGE] = 0F;

        // misc stats
        raw[ID.Attrs.XP] = 1F;
        raw[ID.Attrs.GRUDGE] = 1F;
        raw[ID.Attrs.AMMO] = 1F;
        raw[ID.Attrs.HPRES] = 1F;
        raw[ID.Attrs.KB] = lv * 0.005F;
    }

    /**
     * Update raw attributes for hostile (mob) ships.
     * Uses HostileShipAttrMap (per-class modifiers) and scale config.
     * <p>
     * Scale levels:
     * 0 = scaleMobSmall (default), KB 0.2
     * 1 = scaleMobLarge, KB 0.4
     * 2 = scaleBossSmall, KB 0.85
     * 3 = scaleBossLarge, KB 1.0
     */
    public static void updateAttrsRawHostile(Attrs attrs, int shipScale, int shipClass) {
        float[] attrmod = Values.HostileShipAttrMap.get(shipClass);
        if (attrmod == null)
            return;

        double[] attrbase;
        float kb = 0.2F;

        attrs.resetAttrsRaw();
        float[] raw = attrs.getAttrsRaw();

        switch (shipScale) {
            case 1:
                attrbase = ConfigHandler.scaleMobLarge;
                kb = 0.4F;
                break;
            case 2:
                attrbase = ConfigHandler.scaleBossSmall;
                kb = 0.85F;
                break;
            case 3:
                attrbase = ConfigHandler.scaleBossLarge;
                kb = 1F;
                break;
            default:
                attrbase = ConfigHandler.scaleMobSmall;
                break;
        }

        raw[ID.Attrs.HP] = (float) attrbase[ID.AttrsBase.HP] * attrmod[ID.AttrsBase.HP];
        raw[ID.Attrs.ATK_L] = (float) attrbase[ID.AttrsBase.ATK] * attrmod[ID.AttrsBase.ATK];
        raw[ID.Attrs.ATK_H] = (float) attrbase[ID.AttrsBase.ATK] * attrmod[ID.AttrsBase.ATK] * 3F;
        raw[ID.Attrs.ATK_AL] = (float) attrbase[ID.AttrsBase.ATK] * attrmod[ID.AttrsBase.ATK];
        raw[ID.Attrs.ATK_AH] = (float) attrbase[ID.AttrsBase.ATK] * attrmod[ID.AttrsBase.ATK] * 3F;
        raw[ID.Attrs.DEF] = (float) attrbase[ID.AttrsBase.DEF] * attrmod[ID.AttrsBase.DEF];
        raw[ID.Attrs.SPD] = (float) attrbase[ID.AttrsBase.SPD] * attrmod[ID.AttrsBase.SPD];
        raw[ID.Attrs.MOV] = (float) attrbase[ID.AttrsBase.MOV] * attrmod[ID.AttrsBase.MOV];
        raw[ID.Attrs.HIT] = (float) attrbase[ID.AttrsBase.HIT] * attrmod[ID.AttrsBase.HIT];
        raw[ID.Attrs.CRI] = 0.15F;
        raw[ID.Attrs.DHIT] = 0.1F;
        raw[ID.Attrs.THIT] = 0.1F;
        raw[ID.Attrs.MISS] = 0F;
        raw[ID.Attrs.AA] = 0F;
        raw[ID.Attrs.ASM] = 0F;
        raw[ID.Attrs.DODGE] = 0.15F;
        raw[ID.Attrs.XP] = 1F;
        raw[ID.Attrs.GRUDGE] = 1F;
        raw[ID.Attrs.AMMO] = 1F;
        raw[ID.Attrs.HPRES] = 1F;
        raw[ID.Attrs.KB] = kb;
    }

    /**
     * Update morale buff array based on current morale value.
     * <p>
     * Morale levels (from ID.Morale, original uses strictly greater than):
     * Excited: morale > 5100
     * Happy: morale > 3900
     * Normal: morale > 2100
     * Tired: morale > 900
     * Exhausted: morale <= 900
     */
    public static void updateBuffMorale(AttrsAdv attrs, int morale) {
        int moraleKey;

        if (morale > ID.Morale.L_Excited) {
            moraleKey = 0; // Excited
        } else if (morale > ID.Morale.L_Happy) {
            moraleKey = 1; // Happy
        } else if (morale <= ID.Morale.L_Tired) {
            moraleKey = 3; // Exhausted (check before Tired to match original order)
        } else if (morale <= ID.Morale.L_Normal) {
            moraleKey = 2; // Tired
        } else {
            moraleKey = -1; // Normal (no buff)
        }

        float[] moraleData = Values.MoraleAttrs.get(moraleKey);
        if (moraleData != null) {
            attrs.setAttrsMorale(moraleData.clone());
        } else {
            attrs.resetAttrsMorale();
        }
    }

    /**
     * Update formation buff array.
     * Delegates to the AttrsAdv which uses FormationHelper.
     *
     * @param attrs      the ship attributes object
     * @param formatID   formation type (0=none, 1=Line Ahead, etc.)
     * @param formatSlot slot position in formation (0-5)
     */
    public static void updateBuffFormation(AttrsAdv attrs, int formatID, int formatSlot) {
        if (formatID <= 0) {
            attrs.resetAttrsFormation();
        } else {
            attrs.setAttrsFormation(formatID, formatSlot);
        }
    }

    /**
     * Update potion buff array from active potion effects on the entity.
     * Matches original 1.10.2 convertPotionToBuffMap + convertBuffMapToAttrs.
     */
    public static void updateBuffPotion(BasicEntityShip ship) {
        AttrsAdv attrs = (AttrsAdv) ship.getAttrs();
        if (attrs == null)
            return;

        // reset potion attrs
        float[] potion = new float[Attrs.AttrsLength];

        // Speed (id 1): MOV +0.08 per level
        MobEffectInstance speed = ship.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed != null) {
            int lv = Math.min(speed.getAmplifier() + 1, 5);
            potion[ID.Attrs.MOV] += 0.08F * lv;
        }

        // Slowness (id 2): MOV -0.15, KB +0.15 per level
        MobEffectInstance slow = ship.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null) {
            int lv = Math.min(slow.getAmplifier() + 1, 5);
            potion[ID.Attrs.MOV] -= 0.15F * lv;
            potion[ID.Attrs.KB] += 0.15F * lv;
        }

        // Haste (id 3): SPD +0.6 per level
        MobEffectInstance haste = ship.getEffect(MobEffects.DIG_SPEED);
        if (haste != null) {
            int lv = Math.min(haste.getAmplifier() + 1, 5);
            potion[ID.Attrs.SPD] += 0.6F * lv;
        }

        // Mining Fatigue (id 4): SPD -0.6 per level
        MobEffectInstance fatigue = ship.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null) {
            int lv = Math.min(fatigue.getAmplifier() + 1, 5);
            potion[ID.Attrs.SPD] -= 0.6F * lv;
        }

        // Strength (id 5): ATK_L/H/AL/AH +15, KB +0.15 per level
        MobEffectInstance strength = ship.getEffect(MobEffects.DAMAGE_BOOST);
        if (strength != null) {
            int lv = Math.min(strength.getAmplifier() + 1, 5);
            potion[ID.Attrs.ATK_L] += 15F * lv;
            potion[ID.Attrs.ATK_H] += 15F * lv;
            potion[ID.Attrs.ATK_AL] += 15F * lv;
            potion[ID.Attrs.ATK_AH] += 15F * lv;
            potion[ID.Attrs.KB] += 0.15F * lv;
        }

        // Jump Boost (id 8): HIT +2 per level
        MobEffectInstance jump = ship.getEffect(MobEffects.JUMP);
        if (jump != null) {
            int lv = Math.min(jump.getAmplifier() + 1, 5);
            potion[ID.Attrs.HIT] += 2F * lv;
        }

        // Water Breathing (id 13): DODGE +0.15, ASM +20 per level
        MobEffectInstance waterBreathing = ship.getEffect(MobEffects.WATER_BREATHING);
        if (waterBreathing != null) {
            int lv = Math.min(waterBreathing.getAmplifier() + 1, 5);
            potion[ID.Attrs.DODGE] += 0.15F * lv;
            potion[ID.Attrs.ASM] += 20F * lv;
        }

        // Blindness (id 15): HIT -24 (fixed level 1)
        MobEffectInstance blindness = ship.getEffect(MobEffects.BLINDNESS);
        if (blindness != null) {
            potion[ID.Attrs.HIT] -= 24F;
        }

        // Weakness (id 18): ATK_L/H/AL/AH -15, KB -0.15 per level
        MobEffectInstance weakness = ship.getEffect(MobEffects.WEAKNESS);
        if (weakness != null) {
            int lv = Math.min(weakness.getAmplifier() + 1, 5);
            potion[ID.Attrs.ATK_L] -= 15F * lv;
            potion[ID.Attrs.ATK_H] -= 15F * lv;
            potion[ID.Attrs.ATK_AL] -= 15F * lv;
            potion[ID.Attrs.ATK_AH] -= 15F * lv;
            potion[ID.Attrs.KB] -= 0.15F * lv;
        }

        // Poison (id 19): DEF -0.25, KB -0.1 per level
        MobEffectInstance poison = ship.getEffect(MobEffects.POISON);
        if (poison != null) {
            int lv = Math.min(poison.getAmplifier() + 1, 5);
            potion[ID.Attrs.DEF] -= 0.25F * lv;
            potion[ID.Attrs.KB] -= 0.1F * lv;
        }

        // Health Boost (id 21): HP +150, HPRES +0.5 per level
        MobEffectInstance healthBoost = ship.getEffect(MobEffects.HEALTH_BOOST);
        if (healthBoost != null) {
            int lv = Math.min(healthBoost.getAmplifier() + 1, 5);
            potion[ID.Attrs.HP] += 150F * lv;
            potion[ID.Attrs.HPRES] += 0.5F * lv;
        }

        // Absorption (id 22): HP +100, DEF +0.2 per level
        MobEffectInstance absorption = ship.getEffect(MobEffects.ABSORPTION);
        if (absorption != null) {
            int lv = Math.min(absorption.getAmplifier() + 1, 5);
            potion[ID.Attrs.HP] += 100F * lv;
            potion[ID.Attrs.DEF] += 0.2F * lv;
        }

        // Saturation (id 23): GRUDGE +0.5, AMMO +0.5 per level
        MobEffectInstance saturation = ship.getEffect(MobEffects.SATURATION);
        if (saturation != null) {
            int lv = Math.min(saturation.getAmplifier() + 1, 5);
            potion[ID.Attrs.GRUDGE] += 0.5F * lv;
            potion[ID.Attrs.AMMO] += 0.5F * lv;
        }

        // Levitation (id 25): DODGE +0.1, AA +20, KB -0.2 per level
        MobEffectInstance levitation = ship.getEffect(MobEffects.LEVITATION);
        if (levitation != null) {
            int lv = Math.min(levitation.getAmplifier() + 1, 5);
            potion[ID.Attrs.DODGE] += 0.1F * lv;
            potion[ID.Attrs.AA] += 20F * lv;
            potion[ID.Attrs.KB] -= 0.2F * lv;
        }

        // Luck (id 26): CRI/DHIT/THIT +0.2 per level
        MobEffectInstance luck = ship.getEffect(MobEffects.LUCK);
        if (luck != null) {
            int lv = Math.min(luck.getAmplifier() + 1, 5);
            potion[ID.Attrs.CRI] += 0.2F * lv;
            potion[ID.Attrs.DHIT] += 0.2F * lv;
            potion[ID.Attrs.THIT] += 0.2F * lv;
        }

        // Bad Luck (id 27): CRI/DHIT/THIT -0.3 per level
        MobEffectInstance unluck = ship.getEffect(MobEffects.UNLUCK);
        if (unluck != null) {
            int lv = Math.min(unluck.getAmplifier() + 1, 5);
            potion[ID.Attrs.CRI] -= 0.3F * lv;
            potion[ID.Attrs.DHIT] -= 0.3F * lv;
            potion[ID.Attrs.THIT] -= 0.3F * lv;
        }

        attrs.setAttrsPotion(potion);
    }

    /**
     * Aggregate all buff layers into final AttrsBuffed.
     * <p>
     * For each attribute:
     * Additive attributes: buffed = (raw + equip + potion) * morale + formation
     * Multiplicative attributes: buffed = (raw + equip + potion) * morale *
     * formation
     * <p>
     * Original formula reference (from calcAttrsBuffed):
     * <p>
     * HP: raw + equip + (morale + potion + formation) * scaleShip[HP]
     * HIT: raw + equip + (morale + potion + formation) * scaleShip[HIT]
     * MOV: raw + equip + (morale + potion) * scaleShip[MOV] (NO formation)
     * ATK_L: (raw + equip + potion * scaleShip[ATK]) * morale * formation
     * ATK_H: (raw + equip + potion * 3 * scaleShip[ATK]) * morale * formation
     * ATK_AL: (raw + equip + potion * scaleShip[ATK]) * morale * formation
     * ATK_AH: (raw + equip + potion * 3 * scaleShip[ATK]) * morale * formation
     * SPD: (raw + equip + potion * scaleShip[SPD]) * morale * formation
     * DEF: (raw + equip + (morale + potion) * scaleShip[DEF]) * formation
     * CRI/DHIT/THIT/MISS/AA/ASM: (raw + equip + potion) * morale * formation
     * DODGE/XP/GRUDGE/AMMO/HPRES/KB: raw + equip + morale + potion + formation
     */
    public static void applyBuffOnAttrs(IShipAttrs ship) {
        AttrsAdv attrs = (AttrsAdv) ship.getAttrs();
        if (attrs == null)
            return;

        float[] raw = attrs.getAttrsRaw();
        float[] equip = attrs.getAttrsEquip();
        float[] potion = attrs.getAttrsPotion();
        float[] morale = attrs.getAttrsMorale();
        float[] formation = attrs.getAttrsFormation();
        float[] buffed = new float[Attrs.AttrsLength];

        float scaleHP = (float) ConfigHandler.scaleShip[ID.AttrsBase.HP];
        float scaleATK = (float) ConfigHandler.scaleShip[ID.AttrsBase.ATK];
        float scaleDEF = (float) ConfigHandler.scaleShip[ID.AttrsBase.DEF];
        float scaleSPD = (float) ConfigHandler.scaleShip[ID.AttrsBase.SPD];
        float scaleMOV = (float) ConfigHandler.scaleShip[ID.AttrsBase.MOV];
        float scaleHIT = (float) ConfigHandler.scaleShip[ID.AttrsBase.HIT];

        int id;

        // HP: raw + equip + (morale + potion + formation) * scaleHP
        id = ID.Attrs.HP;
        buffed[id] = raw[id] + equip[id] + (morale[id] + potion[id] + formation[id]) * scaleHP;

        // HIT: raw + equip + (morale + potion + formation) * scaleHIT
        id = ID.Attrs.HIT;
        buffed[id] = raw[id] + equip[id] + (morale[id] + potion[id] + formation[id]) * scaleHIT;

        // DODGE: additive (no scale)
        id = ID.Attrs.DODGE;
        buffed[id] = raw[id] + equip[id] + morale[id] + potion[id] + formation[id];

        // XP, GRUDGE, AMMO, HPRES, KB: additive (no scale)
        for (int idx : new int[]{ID.Attrs.XP, ID.Attrs.GRUDGE, ID.Attrs.AMMO, ID.Attrs.HPRES, ID.Attrs.KB}) {
            buffed[idx] = raw[idx] + equip[idx] + morale[idx] + potion[idx] + formation[idx];
        }

        // MOV: raw + equip + (morale + potion) * scaleMOV (NO formation!)
        id = ID.Attrs.MOV;
        buffed[id] = raw[id] + equip[id] + (morale[id] + potion[id]) * scaleMOV;

        // ATK_L: (raw + equip + potion * scaleATK) * morale * formation
        id = ID.Attrs.ATK_L;
        buffed[id] = (raw[id] + equip[id] + potion[id] * scaleATK) * morale[id] * formation[id];

        // ATK_H: (raw + equip + potion * 3 * scaleATK) * morale * formation
        id = ID.Attrs.ATK_H;
        buffed[id] = (raw[id] + equip[id] + potion[id] * 3F * scaleATK) * morale[id] * formation[id];

        // ATK_AL: (raw + equip + potion * scaleATK) * morale * formation
        id = ID.Attrs.ATK_AL;
        buffed[id] = (raw[id] + equip[id] + potion[id] * scaleATK) * morale[id] * formation[id];

        // ATK_AH: (raw + equip + potion * 3 * scaleATK) * morale * formation
        id = ID.Attrs.ATK_AH;
        buffed[id] = (raw[id] + equip[id] + potion[id] * 3F * scaleATK) * morale[id] * formation[id];

        // SPD: (raw + equip + potion * scaleSPD) * morale * formation
        id = ID.Attrs.SPD;
        buffed[id] = (raw[id] + equip[id] + potion[id] * scaleSPD) * morale[id] * formation[id];

        // CRI, DHIT, THIT, MISS, AA, ASM: (raw + equip + potion) * morale * formation
        for (int idx : new int[]{ID.Attrs.CRI, ID.Attrs.DHIT, ID.Attrs.THIT, ID.Attrs.MISS, ID.Attrs.AA,
                ID.Attrs.ASM}) {
            buffed[idx] = (raw[idx] + equip[idx] + potion[idx]) * morale[idx] * formation[idx];
        }

        // DEF: (raw + equip + (morale + potion) * scaleDEF) * formation
        id = ID.Attrs.DEF;
        buffed[id] = (raw[id] + equip[id] + (morale[id] + potion[id]) * scaleDEF) * formation[id];

        attrs.setAttrsBuffed(buffed);
        attrs.checkAttrsLimit();
    }

    /**
     * Apply periodic buff effects (called every 32 ticks from aiStep).
     * <p>
     * - Auto grudge consumption based on ship type
     * - Regeneration: heal (1%maxHP + 4) * (1 + lv * 0.5) per tick
     * - Wither: damage (1%maxHP + 4) * (1 + lv * 0.5) per tick
     * - Saturation: heal (1%maxHP + 2) * (0.8 + lv * 0.2), morale + config per tick
     */
    public static void applyBuffOnTicks(BasicEntityShip ship) {
        // grudge consumption (idle)
        int shipType = ship.getShipType();
        if (shipType >= 0 && shipType < ConfigHandler.consumeGrudgeShip.length) {
            int grudgeCon = ConfigHandler.consumeGrudgeShip[shipType];
            ship.decrGrudgeNum(grudgeCon);
        }

        // get host's 1% hp
        float hp1p = ship.getMaxHealth() * 0.01F;
        if (hp1p < 1F)
            hp1p = 1F;

        // Regeneration (id 10): heal per tick
        MobEffectInstance regen = ship.getEffect(MobEffects.REGENERATION);
        if (regen != null) {
            int lv = Math.min(regen.getAmplifier() + 1, 5);
            if (ship.getHealth() < ship.getMaxHealth()) {
                ship.heal((hp1p + 4F) * (1F + lv * 0.5F));
            }
        }

        // Wither (id 20): damage per tick
        MobEffectInstance wither = ship.getEffect(MobEffects.WITHER);
        if (wither != null) {
            int lv = Math.min(wither.getAmplifier() + 1, 5);
            ship.hurt(ship.damageSources().magic(), (hp1p + 4F) * (1F + lv * 0.5F));
        }

        // Saturation (id 23): heal + morale per tick
        MobEffectInstance saturation = ship.getEffect(MobEffects.SATURATION);
        if (saturation != null) {
            int lv = Math.min(saturation.getAmplifier() + 1, 5);
            if (ship.getHealth() < ship.getMaxHealth()) {
                ship.heal((hp1p + 2F) * (0.8F + lv * 0.2F));
            }
            ship.addMorale(100 * lv); // original: ConfigHandler.buffSaturation (hardcoded 100)
        }
    }

    /**
     * Get morale level ID from morale value.
     *
     * @return 0=Excited, 1=Happy, 2=Normal, 3=Tired, 4=Exhausted
     */
    public static int getMoraleLevel(int morale) {
        if (morale > ID.Morale.L_Excited)
            return ID.Morale.Excited;
        if (morale > ID.Morale.L_Happy)
            return ID.Morale.Happy;
        if (morale > ID.Morale.L_Normal)
            return ID.Morale.Normal;
        if (morale > ID.Morale.L_Tired)
            return ID.Morale.Tired;
        return ID.Morale.Exhausted;
    }

    /**
     * Check heal effect is in list, return heal level (first found).
     * <p>
     * heal effects: HEAL (instant_health), REGENERATION
     *
     * @return 0 = not heal potion, 1~N = heal potion level
     */
    public static int checkPotionHeal(java.util.List<MobEffectInstance> list) {
        if (list != null) {
            for (MobEffectInstance pe : list) {
                if (pe.getEffect() == net.minecraft.world.effect.MobEffects.HEAL
                        || pe.getEffect() == net.minecraft.world.effect.MobEffects.REGENERATION) {
                    return pe.getAmplifier() + 1;
                }
            }
        }
        return 0;
    }

    /**
     * Check damage effect is in list, return damage level (first found).
     * <p>
     * damage effects: HARM (instant_damage), POISON, WITHER
     *
     * @return 0 = not damage potion, 1~N = damage potion level
     */
    public static int checkPotionDamage(java.util.List<MobEffectInstance> list) {
        if (list != null) {
            for (MobEffectInstance pe : list) {
                if (pe.getEffect() == net.minecraft.world.effect.MobEffects.HARM
                        || pe.getEffect() == net.minecraft.world.effect.MobEffects.POISON
                        || pe.getEffect() == net.minecraft.world.effect.MobEffects.WITHER) {
                    return pe.getAmplifier() + 1;
                }
            }
        }
        return 0;
    }

    /**
     * Remove debuff potion effects from entity.
     * <p>
     * Debuffs: slowness, mining_fatigue, instant_damage, nausea,
     * blindness, hunger, weakness, poison, wither, bad_luck
     */
    public static <T extends net.minecraft.world.entity.LivingEntity> void removeDebuffs(T host) {
        if (host != null) {
            java.util.List<net.minecraft.world.effect.MobEffect> toRemove = new java.util.ArrayList<>();

            for (MobEffectInstance pe : host.getActiveEffects()) {
                net.minecraft.world.effect.MobEffect effect = pe.getEffect();
                if (effect == net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN
                        || effect == net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN
                        || effect == net.minecraft.world.effect.MobEffects.HARM
                        || effect == net.minecraft.world.effect.MobEffects.CONFUSION
                        || effect == net.minecraft.world.effect.MobEffects.BLINDNESS
                        || effect == net.minecraft.world.effect.MobEffects.HUNGER
                        || effect == net.minecraft.world.effect.MobEffects.WEAKNESS
                        || effect == net.minecraft.world.effect.MobEffects.POISON
                        || effect == net.minecraft.world.effect.MobEffects.WITHER
                        || effect == net.minecraft.world.effect.MobEffects.UNLUCK) {
                    toRemove.add(effect);
                }
            }

            for (net.minecraft.world.effect.MobEffect effect : toRemove) {
                host.removeEffect(effect);
            }
        }
    }

    /**
     * Apply attack effect buffs from the attacker's equipment to the target.
     * Accepts integer-keyed map (from ship AttackEffectMap).
     * Map format: key=potion ID, value=int[]{ampLevel, ticks, chance(0~100)}
     */
    public static void applyBuffOnTarget(Entity target, java.util.HashMap<Integer, int[]> effectMap) {
        if (target == null || effectMap == null || effectMap.isEmpty())
            return;
        if (!(target instanceof net.minecraft.world.entity.LivingEntity living))
            return;

        effectMap.forEach((id, content) -> {
            if (content == null || content.length < 3)
                return;

            // chance check: content[2] is percent chance 0-100
            if (living.getRandom().nextInt(100) >= content[2])
                return;

            net.minecraft.world.effect.MobEffect effect = net.minecraft.world.effect.MobEffect.byId(id);
            if (effect == null)
                return;

            MobEffectInstance pe;

            // instant heal (6) and instant damage (7): force duration to 5 ticks
            if (id == 6 || id == 7) {
                pe = new MobEffectInstance(effect, 5, content[0]);
            } else {
                // content[0] = amplifier, content[1] = duration
                pe = new MobEffectInstance(effect, content[1], content[0]);
            }

            living.addEffect(pe);
        });
    }

    /**
     * Apply attack effect buffs from the attacker's equipment to the target.
     */
    public static void applyBuffOnTarget(Entity target,
                                         java.util.Map<net.minecraft.world.effect.MobEffect, int[]> effectMap) {
        if (target == null || effectMap == null || effectMap.isEmpty())
            return;
        if (!(target instanceof net.minecraft.world.entity.LivingEntity living))
            return;

        for (java.util.Map.Entry<net.minecraft.world.effect.MobEffect, int[]> entry : effectMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length >= 2) {
                int duration = entry.getValue()[0];
                int amplifier = entry.getValue()[1];
                living.addEffect(new MobEffectInstance(entry.getKey(), duration, amplifier));
            }
        }
    }

    // ======== Potion Level Helpers ========

    /**
     * Get specific potion level from buff map.
     *
     * @return 0 = not present, 1+ = potion level (amplifier + 1)
     */
    public static int getPotionLevel(Map<Integer, Integer> buffmap, int pid) {
        if (buffmap != null) {
            return buffmap.containsKey(pid) ? buffmap.get(pid) + 1 : 0;
        }
        return 0;
    }

    /**
     * Get specific potion level from entity's buff map.
     */
    public static int getPotionLevel(Entity host, int pid) {
        if (host instanceof IShipAttackBase ship) {
            return getPotionLevel(ship.getBuffMap(), pid);
        }
        return 0;
    }

    /**
     * Get specific potion level from potion effect list.
     * Matches by raw potion ID.
     */
    public static int getPotionLevel(List<MobEffectInstance> list, int pid) {
        if (list != null && !list.isEmpty()) {
            for (MobEffectInstance pe : list) {
                int id = net.minecraft.world.effect.MobEffect.getId(pe.getEffect());
                if (id == pid) {
                    return pe.getAmplifier() + 1;
                }
            }
        }
        return 0;
    }

    // ======== Damage Pipeline Methods ========

    /**
     * Calculate potion damage. Used in entity.hurt().
     * <p>
     * Checks if the damage source is a thrown potion (ThrownPotion) or
     * area effect cloud (AreaEffectCloud). If so, recalculates damage as:
     * (2% maxHP + 2) * potionLevel + rawAtk
     *
     * @return recalculated potion damage, or 0F if not a potion source
     */
    public static float getPotionDamage(LivingEntity host, DamageSource source, float atk) {
        if (host == null || source == null)
            return 0F;

        int level = 1;

        // get potion level from thrown potion entity
        if (source.getDirectEntity() instanceof ThrownPotion potion) {
            ItemStack pot = potion.getItem();

            level = getPotionLevel(PotionUtils.getMobEffects(pot), 7); // 7 = Instant Damage
        }

        // from area effect cloud entity
        else if (source.getDirectEntity() instanceof AreaEffectCloud) {
            // area effect clouds don't easily expose their potion effects in 1.20.1
            // use level 1 as default (matches most common case)

        }
        // not a potion source
        else {
            return 0F;
        }

        // damage = (2% maxHP + 2) * level + raw potion damage
        float hp1p = host.getMaxHealth() * 0.01F;
        if (hp1p < 1F)
            hp1p = 1F;

        return (hp1p * 2F + 2F) * level + atk;
    }

    /**
     * Apply resistance potion effect to reduce incoming damage.
     * <p>
     * Potion buff IDs:
     * 11: Resistance - reduces missile damage
     * 12: Fire Resistance - reduces non-missile ship attacks
     * <p>
     * Each level reduces damage by 20%, max level 4 = 80% reduction.
     *
     * @return modified damage value
     */
    public static float applyBuffOnDamageByResist(IShipAttackBase host, DamageSource source, float atk) {
        if (host == null || source == null)
            return atk;

        HashMap<Integer, Integer> buffmap = host.getBuffMap();
        int level;

        // Resistance potion (buff 11): reduces missile damage
        if (source.getDirectEntity() instanceof EntityAbyssMissile) {
            level = getPotionLevel(buffmap, 11);
            if (level > 0) {
                if (level > 4)
                    level = 4;
                atk = atk * (1F - level * 0.2F);
            }
        }
        // Fire Resistance potion (buff 12): reduces non-missile ship attacks
        else if (source.getDirectEntity() instanceof IShipAttackBase) {
            level = getPotionLevel(buffmap, 12);
            if (level > 0) {
                if (level > 4)
                    level = 4;
                atk = atk * (1F - level * 0.2F);
            }
        }

        return atk;
    }

    /**
     * Apply light-based damage modifier for ship-vs-ship combat.
     * Uses block light level at target position and attacker's Night Vision potion.
     * <p>
     * Light coefficient:
     * 0 = night (light level <= 2)
     * 1 = day (light level >= 8)
     * Night Vision potion on attacker adds +0.8 (can exceed 1.0)
     *
     * @return modified damage value
     */
    public static float applyBuffOnDamageByLight(IShipAttackBase host, DamageSource source, float atk) {
        if (host == null || source == null)
            return atk;

        // only applies in ship vs ship combat
        if (!(source.getEntity() instanceof IShipAttackBase attacker))
            return atk;
        if (!(host instanceof LivingEntity hostLiving))
            return atk;

        // light coefficient: (blockLight - 2) / 6, clamped [0, 1] before night vision
        BlockPos pos = hostLiving.blockPosition();
        float lightCoeff = ((float) hostLiving.level().getMaxLocalRawBrightness(pos) - 2F) / 6F;

        if (lightCoeff < 0F)
            lightCoeff = 0F;
        else if (lightCoeff > 1F)
            lightCoeff = 1F;

        // check Night Vision potion level on the ATTACKER (buff ID 16)
        float nightVisionLevel = getPotionLevel(attacker.getBuffMap(), 16);

        // apply night vision potion to coefficient
        if (nightVisionLevel > 0)
            lightCoeff += 0.8F;

        atk = CombatHelper.modDamageByLight(atk, attacker.getDamageType(), host.getDamageType(), lightCoeff);

        return atk;
    }
}
