package com.lulan.shincolle.entity;

import com.lulan.shincolle.reference.unitclass.MissileData;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;

public interface IShipAttackBase extends IShipNavigator, IShipEmotion, IShipOwner, IShipAttrs {
    @Deprecated(forRemoval = true)
    Entity getEntityTarget();

    @Deprecated(forRemoval = true)
    void setEntityTarget(Entity target);

    Entity getEntityRevengeTarget();

    void setEntityRevengeTarget(Entity target);

    int getEntityRevengeTime();

    void setEntityRevengeTime();

    int getDamageType();

    boolean getAttackType(int par1);

    int getAmmoLight();

    void setAmmoLight(int num);

    int getAmmoHeavy();

    void setAmmoHeavy(int num);

    boolean hasAmmoLight();

    boolean hasAmmoHeavy();

    int getLevel();

    boolean updateSkillAttack(Entity target);

    HashMap<Integer, Integer> getBuffMap();

    void setBuffMap(HashMap<Integer, Integer> map);

    HashMap<Integer, int[]> getAttackEffectMap();

    void setAttackEffectMap(HashMap<Integer, int[]> map);

    MissileData getMissileData(int type);

    void setMissileData(int type, MissileData data);
}
