package com.lulan.shincolle.utility;

import com.lulan.shincolle.client.particle.*;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.network.ModNetworking;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.reference.Values;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor.TargetPoint;

import java.util.Objects;
import java.util.Random;

/**
 * particle helper
 */
public class ParticleHelper {

    private static final Random rand = new Random();
    private static final float TEAM_CIRCLE_ENTITY_SCALE = 0.35F;
    /**
     * spawn attack text particle on entity, SERVER SIDE
     * type: 0:miss, 1:critical, 2:double hit, 3:triple hit, 4:dodge
     */
    public static void spawnAttackTextParticle(Entity host, int type) {
        //null check
        if (host == null || host.level() == null || !host.level().isClientSide()) return;

        TargetPoint point = new TargetPoint(host.getX(), host.getY(), host.getZ(), 64D, host.level().dimension());

        switch (type) {
            case 0:  //miss
                ModNetworking.sendToNear(new S2CSpawnParticle(host, 10, false), point);
                break;
            case 1:  //critical
                ModNetworking.sendToNear(new S2CSpawnParticle(host, 11, false), point);
                break;
            case 2:  //double hit
                ModNetworking.sendToNear(new S2CSpawnParticle(host, 12, false), point);
                break;
            case 3:  //triple hit
                ModNetworking.sendToNear(new S2CSpawnParticle(host, 13, false), point);
                break;
            case 4:  //dodge
                ModNetworking.sendToNear(new S2CSpawnParticle(host, 34, false), point);
                break;
            default:
                break;
        }
    }

    /**
     * Spawn attack particles with default look direction (upward).
     */
    public static void spawnAttackParticleAt(double x, double y, double z, byte type) {
        spawnAttackParticleAt(x, y, z, 0.0, 1.0, 0.0, type);
    }
    /**
     * SPAWN ATTACK PARTICLE WITH CUSTOM POSITION
     *
     * @parm posX, posY, posZ, lookX, lookY, lookZ, type
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleCustomVector(Entity target, double posX, double posY, double posZ, double lookX, double lookY, double lookZ, byte type, boolean isShip) {
        if (target != null) {
            if (isShip && target instanceof IShipEmotion) {
                ((IShipEmotion) target).setAttackTick(50);
            }

            //spawn particle
            spawnAttackParticleAt(posX, posY, posZ, lookX, lookY, lookZ, type);
        }
    }

    /**
     * SPAWN ATTACK PARTICLE
     * spawn particle and set attack time for model rendering
     *
     * @parm entity, type
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticle(Entity target, byte type, boolean setAtkTime) {
        //null check
        if (target == null) return;

        if (setAtkTime && target instanceof IShipEmotion) {
            ((IShipEmotion) target).setAttackTick(50);
        }

        //0 = no particle
        if (type == 0) return;

        //target look
        double lookX = 0;
        double lookY = 0;
        double lookZ = 0;

        //get target position
        if (type > 9) {
            lookY = target.getBbHeight() * 1.3D;
        } else {
            lookX = target.getLookAngle().x;
            lookY = target.getLookAngle().y;
            lookZ = target.getLookAngle().z;
        }

        //spawn particle
        spawnAttackParticleAt(target.getX(), target.getY(), target.getZ(), lookX, lookY, lookZ, type);
    }

    /**
     * Spawn particle at xyz position
     *
     * @parm posX, posY, posZ, lookX, lookY, lookZ, particleID
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleAt(double posX, double posY, double posZ, double lookX, double lookY, double lookZ, byte type) {
        ClientLevel level = Minecraft.getInstance().level;

        //get target position
        double ran1 = 0D;
        double ran2 = 0D;
        double ran3 = 0D;
        float[] newPos1;
        float[] newPos2;
        float degYaw = 0F;

        //spawn particle
        //parameters除了ITEM_CRACK BLOCK_CRACK BLOCK_DUST以外都是傳入new int[0]即可
        //addParticle(EnumParticleTypes particleType, boolean ignoreRange,
        //              double xCoord, double yCoord, double zCoord,
        //              double xSpeed, double ySpeed, double zSpeed,
        //              int... parameters)
        if (level == null) {
            return;
        }

        switch (type) {
            case 1:        //Large explode -> Explosion_Emitter
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, posX, posY + 2, posZ, 0.0D, 0.0D, 0.0D);
                break;
            case 2:        //Huge explosion -> Explosion
                level.addParticle(ParticleTypes.EXPLOSION, posX, posY + 1, posZ, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 24; ++i) {
                    ran1 = rand.nextFloat() * 6F - 3F;
                    ran2 = rand.nextFloat() * 6F - 3F;
                    level.addParticle(ParticleTypes.LAVA, posX + ran1, posY + 1, posZ + ran2, 0D, 0D, 0D);
                }
                break;
            case 3:        //hearts effect
                for (int i = 0; i < 7; ++i) {
                    double d0 = rand.nextGaussian() * 0.02D;
                    double d1 = rand.nextGaussian() * 0.02D;
                    double d2 = rand.nextGaussian() * 0.02D;
                    level.addParticle(ParticleTypes.HEART, posX + rand.nextFloat() * 2D - 1D, posY + 0.5D + rand.nextFloat() * 2D, posZ + rand.nextFloat() * 2.0F - 1D, d0, d1, d2);
                }
                break;
            case 4:    //smoke: for minor damage
                for (int i = 0; i < 3; i++) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2D;
                    ran2 = rand.nextFloat() * lookX - lookX / 2D;
                    ran3 = rand.nextFloat() * lookX - lookX / 2D;
                    level.addParticle(ParticleTypes.SMOKE, posX + ran1, posY + ran2, posZ + ran3, 0D, lookY, 0D);
                }
                break;
            case 5:        //flame+smoke: for moderate damage
                for (int i = 0; i < 3; i++) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2D;
                    ran2 = rand.nextFloat() * lookX - lookX / 2D;
                    ran3 = rand.nextFloat() * lookX - lookX / 2D;
                    level.addParticle(ParticleTypes.SMOKE, posX + ran1, posY + ran2, posZ + ran3, 0D, lookY, 0D);
                    level.addParticle(ParticleTypes.FLAME, posX + ran3, posY + ran2, posZ + ran1, 0D, lookY, 0D);
                }
                break;
            case 6:    //largesmoke
                for (int i = 0; i < 24; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    double pX = posX + lookX - 0.5D + 0.05D * i;
                    double pZ = posZ + lookZ - 0.5D + 0.05D * i;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, pX, posY + 0.6D + ran1, pZ, lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, pX, posY + 1.0D + ran1, pZ, lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                }
                break;
            case 7:    //flame+large smoke: for heavy damage
                for (int i = 0; i < 4; i++) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2D;
                    ran2 = rand.nextFloat() * lookX - lookX / 2D;
                    ran3 = rand.nextFloat() * lookX - lookX / 2D;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + ran1, posY + ran2, posZ + ran3, 0D, 0D, 0D);
                    level.addParticle(ParticleTypes.FLAME, posX + ran3, posY + ran2, posZ + ran1, 0D, 0.05D, 0D);
                }
                break;
            case 8:        //flame
                level.addParticle(ParticleTypes.FLAME, posX, posY - 0.1, posZ, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, posX, posY + 0.1, posZ, 0.0D, 0.0D, 0.0D);
                break;
            case 9:    //lava + largeexplode
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, posX, posY + 1.5, posZ, 0.0D, 0.0D, 0.0D);
                for (int i = 0; i < 15; i++) {
                    ran1 = rand.nextFloat() * 3F - 1.5F;
                    ran2 = rand.nextFloat() * 3F - 1.5F;
                    level.addParticle(ParticleTypes.LAVA, posX + ran1, posY + 1, posZ + ran2, 0D, 0D, 0D);
                }
                break;
            case 10:    //miss
                ParticleTexts particleMiss = new ParticleTexts(level,
                        posX, posY + lookY, posZ, 1F, 0);
                Minecraft.getInstance().particleEngine.add(particleMiss);
                break;
            case 11:    //cri
                ParticleTexts particleCri = new ParticleTexts(level,
                        posX, posY + lookY, posZ, 1F, 1);
                Minecraft.getInstance().particleEngine.add(particleCri);
                break;
            case 12:    //double hit
                ParticleTexts particleDHit = new ParticleTexts(level,
                        posX, posY + lookY, posZ, 1F, 2);
                Minecraft.getInstance().particleEngine.add(particleDHit);
                break;
            case 13:    //triple hit
                ParticleTexts particleTHit = new ParticleTexts(level,
                        posX, posY + lookY, posZ, 1F, 3);
                Minecraft.getInstance().particleEngine.add(particleTHit);
                break;
            case 14:    //laser
                ParticleLaser particleLaser = new ParticleLaser(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 1F, 0);
                Minecraft.getInstance().particleEngine.add(particleLaser);
                break;
            case 15:    //white spray
                ParticleSpray particleSpray = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 1);
                Minecraft.getInstance().particleEngine.add(particleSpray);
                break;
            case 16:    //cyan spray
                ParticleSpray particleSpray2 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 2);
                Minecraft.getInstance().particleEngine.add(particleSpray2);
                break;
            case 17:    //green spray
                ParticleSpray particleSpray3 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 3);
                Minecraft.getInstance().particleEngine.add(particleSpray3);
                break;
            case 18:    //red spray
                ParticleSpray particleSpray4 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 4);
                Minecraft.getInstance().particleEngine.add(particleSpray4);
                break;
            case 19:    //double largesmoke for 14~20 inch cannon
                //計算煙霧位置
                degYaw = CalcHelper.getLookDegree(lookX, 0D, lookZ, false)[0];
                newPos1 = CalcHelper.rotateXZByAxis(0F, (float) lookY, degYaw, 1F);
                newPos2 = CalcHelper.rotateXZByAxis(0F, (float) -lookY, degYaw, 1F);

                for (int i = 0; i < 15; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos1[1], posY + 0.6D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos1[0], lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos2[1], posY + 0.6D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos2[0], lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos1[1], posY + 0.9D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos1[0], lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos2[1], posY + 0.9D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos2[0], lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                }
                break;
            case 20:    //smoke: for nagato equip
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.SMOKE, posX, posY + i * 0.1D, posZ, lookX, lookY, lookZ);
                }
                break;
            case 21:    //Type 91 AP Fist: phase 4 hit particle
                //draw speed blur
                ParticleLaser particleLaser2 = new ParticleLaser(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 4F, 1);
                Minecraft.getInstance().particleEngine.add(particleLaser2);
                ParticleLaser particleLaser3 = new ParticleLaser(level,
                        posX, posY + 0.4D, posZ, lookX, lookY + 0.4D, lookZ, 4F, 1);
                Minecraft.getInstance().particleEngine.add(particleLaser3);
                ParticleLaser particleLaser4 = new ParticleLaser(level,
                        posX, posY + 0.8D, posZ, lookX, lookY + 0.8D, lookZ, 4F, 1);
                Minecraft.getInstance().particleEngine.add(particleLaser4);

                //draw hit particle
                for (int i = 0; i < 20; ++i) {
                    newPos1 = CalcHelper.rotateXZByAxis(1, 0, 6.28F / 20F * i, 1);
                    //motionY傳入4, 表示為特殊設定
                    ParticleSpray particleSpray5 = new ParticleSpray(level,
                            lookX, lookY + 0.3D, lookZ, newPos1[0] * 0.35D, 0D, newPos1[1] * 0.35D, 0);
                    Minecraft.getInstance().particleEngine.add(particleSpray5);
                }

                //draw hit text
                Particle91Type particle91Type = new Particle91Type(level,
                        lookX, lookY + 3D, lookZ, 0.6F);
                Minecraft.getInstance().particleEngine.add(particle91Type);
                break;
            case 22:    //Type 91 AP Fist: phase 1,3 particle
                for (int i = 0; i < 20; ++i) {
                    newPos1 = CalcHelper.rotateXZByAxis((float) lookX, 0, 6.28F / 20F * i, 1);
                    //motionY傳入4, 表示為特殊設定
                    ParticleSpray particleSpray7 = new ParticleSpray(level,
                            posX + newPos1[0], posY + lookY, posZ + newPos1[1], -newPos1[0] * 0.06D, 0D, -newPos1[1] * 0.06D, 5);
                    Minecraft.getInstance().particleEngine.add(particleSpray7);
                }
                break;
            case 23:    //Type 91 AP Fist: phase 2 particle
                for (int i = 0; i < 20; ++i) {
                    newPos1 = CalcHelper.rotateXZByAxis((float) lookX, 0, 6.28F / 20F * i, 1);
                    //motionY傳入4, 表示為特殊設定
                    ParticleSpray particleSpray8 = new ParticleSpray(level,
                            posX, posY + lookY, posZ, newPos1[0], 0D, newPos1[1], 6);
                    Minecraft.getInstance().particleEngine.add(particleSpray8);
                }
                break;
            case 24:    //smoke: for nagato BOSS equip
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX, posY + i * 0.3D, posZ, lookX, lookY, lookZ);
                }
                break;
            case 25:    //arrow particle: for move or attack target mark
                ParticleTeam particleTeam = new ParticleTeam(level, (float) lookX, (int) lookY, posX, posY, posZ);
                Minecraft.getInstance().particleEngine.add(particleTeam);
                break;
            case 26:    //white spray
                ParticleSpray particleSpray7 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 7);
                Minecraft.getInstance().particleEngine.add(particleSpray7);
                break;
            case 27:    //yellow spray
                ParticleSpray particleSpray8 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 8);
                Minecraft.getInstance().particleEngine.add(particleSpray8);
                break;
            case 28:    //drip water
                ran1 = rand.nextFloat() * 0.7D - 0.35D;
                ran2 = rand.nextFloat() * 0.7D - 0.35D;
                level.addParticle(ParticleTypes.DRIPPING_WATER, posX + ran1, posY, posZ + ran2, lookX, lookY, lookZ);
                break;
            case 29:    //orange spray
                ParticleSpray particleSpray9 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 9);
                Minecraft.getInstance().particleEngine.add(particleSpray9);
                break;
            case 30:    //snow hit
                for (int i = 0; i < 15; i++) {
                    ran1 = rand.nextFloat() * 2F - 1F;
                    ran2 = rand.nextFloat() * 2F - 1F;
                    ran3 = rand.nextFloat() * 2F - 1F;
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL, posX + ran1, posY + 0.8D + ran2, posZ + ran3, lookX * 0.2D, 0.5D, lookZ * 0.2D);
                }
                break;
            case 31:    //throw snow smoke
                for (int i = 0; i < 22; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL, posX + lookX - 0.5D + 0.05D * i, posY + 0.7D + ran1, posZ + lookZ - 0.5D + 0.05D * i, lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.ITEM_SNOWBALL, posX + lookX - 0.5D + 0.05D * i, posY + 0.9D + ran1, posZ + lookZ - 0.5D + 0.05D * i, lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                }
                break;
            case 32:    //transparent cyan spray
                ParticleSpray particleSpray10 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 10);
                Minecraft.getInstance().particleEngine.add(particleSpray10);
                break;
            case 33:    //transparent red spray
                ParticleSpray particleSpray11 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 11);
                Minecraft.getInstance().particleEngine.add(particleSpray11);
                break;
            case 34:    //dodge
                ParticleTexts particleTDodge = new ParticleTexts(level,
                        posX, posY + lookY, posZ, 1F, 4);
                Minecraft.getInstance().particleEngine.add(particleTDodge);
                break;
            case 35:    //triple largesmoke for boss ship
                //計算煙霧位置
                degYaw = CalcHelper.getLookDegree(lookX, 0D, lookZ, false)[0];
                newPos1 = CalcHelper.rotateXZByAxis(0F, (float) lookY, degYaw, 1F);
                newPos2 = CalcHelper.rotateXZByAxis(0F, (float) -lookY, degYaw, 1F);

                for (int i = 0; i < 15; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos1[1] + ran2, posY + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos1[0] + ran2, lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos2[1] + ran3, posY + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos2[0] + ran3, lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos1[1] + ran3, posY + 0.3D + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos1[0] + ran3, lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos2[1] + ran2, posY + 0.3D + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos2[0] + ran2, lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos1[1] + ran2, posY + 0.6D + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos1[0] + ran2, lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.6D + 0.1D * i + newPos2[1] + ran3, posY + 0.6D + ran1, posZ + lookZ - 0.6D + 0.1D * i + newPos2[0] + ran3, lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                }
                break;
            case 36:    //emotion
                ParticleEmotion partEmo = new ParticleEmotion(level, null,
                        posX, posY, posZ, (float) lookX, (int) lookY, (int) lookZ);
                Minecraft.getInstance().particleEngine.add(partEmo);
                break;
            case 37:    //white spray
                ParticleSpray particleSpray12 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 12);
                Minecraft.getInstance().particleEngine.add(particleSpray12);
                break;
            case 38:    //next waypoint spray
                ParticleSpray particleSpray13 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 13);
                Minecraft.getInstance().particleEngine.add(particleSpray13);
                break;
            case 39:    //paired chest spray
                ParticleSpray particleSpray14 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 14);
                Minecraft.getInstance().particleEngine.add(particleSpray14);
                break;
            case 40:    //craning
                ParticleCraning particleCrane = new ParticleCraning(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 0);
                Minecraft.getInstance().particleEngine.add(particleCrane);
                break;
            case 41:    //cyan spray 2
                ParticleSpray particleSpray15 = new ParticleSpray(level,
                        posX, posY, posZ, lookX, lookY, lookZ, 15);
                Minecraft.getInstance().particleEngine.add(particleSpray15);
                break;
            case 42:    //double largesmoke for mounts with 14~20 inch cannon: lookX: entity.yBodyRot, lookY: cannon spacing
                //計算煙霧位置
                newPos1 = CalcHelper.rotateXZByAxis(0F, (float) lookY, (float) (lookX * Values.N.DIV_PI_180), 1F);
                newPos2 = CalcHelper.rotateXZByAxis(0F, (float) -lookY, (float) (lookX * Values.N.DIV_PI_180), 1F);

                for (int i = 0; i < 15; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos1[1], posY + 0.6D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos1[0], lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos2[1], posY + 0.6D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos2[0], lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos1[1], posY + 0.9D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos1[0], lookX * 0.3D * ran3, 0.05D * ran3, lookZ * 0.3D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, posX + lookX - 0.5D + 0.05D * i + newPos2[1], posY + 0.9D + ran1, posZ + lookZ - 0.5D + 0.05D * i + newPos2[0], lookX * 0.3D * ran2, 0.05D * ran2, lookZ * 0.3D * ran2);
                }
                break;
            case 43:    //custom size smoke: parms: lookY: motionY, lookX: scale
            {
                for (int i = 0; i < 3; i++) {
                    ParticleSmoke smoke1 = new ParticleSmoke(level, posX, posY + i * 0.1D, posZ, 0D, lookY, 0D, (float) lookX);
                    Minecraft.getInstance().particleEngine.add(smoke1);
                }
            }
            break;
            case 44:    //high speed movement blur THICK YELLOW
            {
                //draw speed blur: H, Wfor, Wback, R, G, B, A, px, py, pz, mx, my, mz
                ParticleLine line1 = new ParticleLine(level, 0, new float[]{2.5F, 8F, 22F, 1F, 1F, 0.4F, 0.8F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line2 = new ParticleLine(level, 0, new float[]{1F, 8F, 20F, 1F, 1F, 0.7F, 0.9F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line3 = new ParticleLine(level, 0, new float[]{0.8F, 7F, 18F, 1F, 1F, 1F, 1F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                Minecraft.getInstance().particleEngine.add(line1);
                Minecraft.getInstance().particleEngine.add(line2);
                Minecraft.getInstance().particleEngine.add(line3);
            }
            break;
            case 45:    //high speed movement blur THIN + THICK Aura
            {
                //draw speed blur: H, Wfor, Wback, R, G, B, A, px, py, pz, mx, my, mz
                ParticleLine line1 = new ParticleLine(level, 0, new float[]{2.4F, 8F, 22F, 1F, 0F, 0F, 0.4F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line2 = new ParticleLine(level, 0, new float[]{0.24F, 8F, 20F, 1F, 0F, 1F, 0.85F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line3 = new ParticleLine(level, 0, new float[]{0.2F, 7F, 18F, 1F, 1F, 1F, 1F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                Minecraft.getInstance().particleEngine.add(line1);
                Minecraft.getInstance().particleEngine.add(line2);
                Minecraft.getInstance().particleEngine.add(line3);
            }
            break;
            case 46:    //high speed movement blur THICK PINK
            {
                //draw speed blur: H, Wfor, Wback, R, G, B, A, px, py, pz, mx, my, mz
                ParticleLine line1 = new ParticleLine(level, 0, new float[]{0.6F, 7F, 7F, 1F, 0.6F, 1F, 0.3F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line2 = new ParticleLine(level, 0, new float[]{0.3F, 4F, 4F, 1F, 0.8F, 1F, 0.8F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                ParticleLine line3 = new ParticleLine(level, 0, new float[]{0.2F, 3F, 3F, 1F, 1F, 1F, 1F,
                        (float) posX, (float) posY, (float) posZ, (float) lookX, (float) lookY, (float) lookZ});
                Minecraft.getInstance().particleEngine.add(line1);
                Minecraft.getInstance().particleEngine.add(line2);
                Minecraft.getInstance().particleEngine.add(line3);
            }
            break;
            case 47:    //lots white spray
            {
                int maxpar = (int) ((3 - Minecraft.getInstance().options.particles().get().ordinal()) * 1.8F);
                for (int i = 0; i < maxpar; i++) {
                    ParticleSpray spray = new ParticleSpray(level, posX, posY, posZ, lookX, lookY, lookZ, 16);
                    Minecraft.getInstance().particleEngine.add(spray);
                }
            }
            break;
            case 48:    //water bubble
            {
                for (int i = 0; i < 14; i++) {
                    ran1 = (rand.nextFloat() - 0.5F) * lookY;
                    ran2 = (rand.nextFloat() - 0.5F) * lookX;
                    ran3 = (rand.nextFloat() - 0.5F) * lookZ;
                    level.addParticle(ParticleTypes.BUBBLE, posX + ran2, posY + ran1, posZ + ran3, 0D, 0D, 0D);
                    level.addParticle(ParticleTypes.SPLASH, posX + ran2, posY + ran1, posZ + ran3, 0D, 0D, 0D);
                }
            }
            break;
            case 49:    //drip lava
                ran1 = rand.nextFloat() * 0.7D - 0.35D;
                ran2 = rand.nextFloat() * 0.7D - 0.35D;
                level.addParticle(ParticleTypes.DRIPPING_LAVA, posX + ran1, posY, posZ + ran2, lookX, lookY, lookZ);
                break;
            default:
                break;
        }
    }

    /**
     * Spawn particle at entity position
     *
     * @parm host, par1, par2, par3, particleID
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity ent, double par1, double par2, double par3, byte type) {
        //null check
        if (ent == null) return;

        ClientLevel level = Minecraft.getInstance().level;
        LivingEntity host = null;

        //get target position
        double ran1 = 0D;
        double ran2 = 0D;
        double ran3 = 0D;
        double ran4 = 0D;
        float[] newPos1;
        float[] newPos2;
        float[] newPos3;
        float degYaw = 0F;

        //spawn particle
        switch (type) {
            case 1:        //氣彈特效 par1:scale par2:type
            {
                ParticleChi fxChi1 = new ParticleChi(level, ent, (float) par1, (int) par2);
                Minecraft.getInstance().particleEngine.add(fxChi1);
            }
            break;
            case 2:        //隊伍圈選特效 par1:scale par2:type
            {
                ParticleTeam fxTeam = new ParticleTeam(level, ent, (float) par1, (int) par2);
                Minecraft.getInstance().particleEngine.add(fxTeam);
            }
            break;
            case 3: {
                ParticleLightning fxLightning = new ParticleLightning(level, ent, (float) par1, (int) par2);
                Minecraft.getInstance().particleEngine.add(fxLightning);
            }
            break;
            case 4:        //sticky lightning
            {
                for (int i = 0; i < 4; i++) {
                    ParticleStickyLightning light = new ParticleStickyLightning(level, ent, (float) par1, (int) par2, (int) par3);
                    Minecraft.getInstance().particleEngine.add(light);
                }
            }
            break;
            case 5:    //custom largesmoke: par1:wide, par2:length, par3:height, EntityLivingBase ONLY
            {
                //計算煙霧位置
                degYaw = (((LivingEntity) ent).yBodyRot % 360) * Values.N.DIV_PI_180;
                newPos1 = CalcHelper.rotateXZByAxis((float) par2, (float) par1, degYaw, 1F);
                newPos2 = CalcHelper.rotateXZByAxis((float) par2, (float) -par1, degYaw, 1F);
                newPos3 = CalcHelper.rotateXZByAxis(0.25F, 0F, degYaw, 1F);

                for (int i = 0; i < 24; i++) {
                    ran1 = (rand.nextFloat() - 0.5F) * 2F;
                    ran2 = (rand.nextFloat() - 0.5F) * 2F;
                    ran3 = (rand.nextFloat() - 0.5F) * 2F;
                    ran4 = rand.nextFloat() * 2F;
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos1[1] + ran1, ent.getY() + par3 + ran2, ent.getZ() + newPos1[0] + ran3, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos2[1] + ran1, ent.getY() + par3 + ran3, ent.getZ() + newPos2[0] + ran2, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos1[1] + ran2, ent.getY() + par3 + ran1, ent.getZ() + newPos1[0] + ran3, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos2[1] + ran2, ent.getY() + par3 + ran3, ent.getZ() + newPos2[0] + ran1, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos1[1] + ran3, ent.getY() + par3 + ran1, ent.getZ() + newPos1[0] + ran2, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() + newPos2[1] + ran3, ent.getY() + par3 + ran2, ent.getZ() + newPos2[0] + ran1, newPos3[1] * ran4, 0.05D * ran4, newPos3[0] * ran4);
                }
            }
            break;
            case 6:        //lightning sphere + lightning radiation
            {
                //in
                for (int i = 0; i < 4; i++) {
                    ParticleStickyLightning light11 = new ParticleStickyLightning(level, ent, (float) par1, (int) par2, 2);
                    Minecraft.getInstance().particleEngine.add(light11);
                }
                //out
                for (int i = 0; i < 4; i++) {
                    ParticleStickyLightning light21 = new ParticleStickyLightning(level, ent, (float) par1, (int) par2, 3);
                    Minecraft.getInstance().particleEngine.add(light21);
                }
            }
            break;
            case 7:        //vibrate cube
            {
                //host check
                if (ent instanceof LivingEntity) {
                    host = (LivingEntity) ent;
                } else {
                    return;
                }

                //in
                ParticleCube cube1 = new ParticleCube(level, host, par1, par2, par3, 1.5F, 0);
                Minecraft.getInstance().particleEngine.add(cube1);

                //out
                for (int i = 0; i < 6; i++) {
                    ParticleStickyLightning light21 = new ParticleStickyLightning(level, ent, (float) par1, 40, 3);
                    Minecraft.getInstance().particleEngine.add(light21);
                }
            }
            break;
            case 8:        //守衛標示線: block類
            {
                //host check
                if (ent instanceof LivingEntity) {
                    host = (LivingEntity) ent;
                } else {
                    return;
                }

                ParticleLaserNoTexture laser1 = new ParticleLaserNoTexture(level, host, par1, par2, par3, 0.1F, 3);
                Minecraft.getInstance().particleEngine.add(laser1);
            }
            break;
            case 9:        //small sticky lightning
            {
                ParticleStickyLightning light5 = new ParticleStickyLightning(level, ent, (float) par1, (int) par2, (int) par3);
                Minecraft.getInstance().particleEngine.add(light5);
                ParticleStickyLightning light6 = new ParticleStickyLightning(level, ent, (float) par1, (int) par2, (int) par3);
                Minecraft.getInstance().particleEngine.add(light6);
            }
            break;
            case 10:    //double largesmoke for mounts: par1: cannon width, par2: cannon height, par3: cannon x pos
            {
                //煙霧出現位置: 依照身體旋轉
                newPos1 = CalcHelper.rotateXZByAxis((float) par3, (float) par1, (float) (((LivingEntity) ent).yBodyRot * Values.N.DIV_PI_180), 1F);
                newPos2 = CalcHelper.rotateXZByAxis((float) par3, (float) -par1, (float) (((LivingEntity) ent).yBodyRot * Values.N.DIV_PI_180), 1F);
                //煙霧噴射方向: 依照頭部旋轉
                newPos3 = CalcHelper.rotateXZByAxis(1.5F, 0F, (float) (((LivingEntity) ent).yHeadRot  * Values.N.DIV_PI_180), 1F);

                //實際煙霧位置: 身體旋轉xz位移(達到砲台底座位置)+頭部旋轉xz位移(達到砲管旋轉位置)
                for (int i = 0; i < 24; i++) {
                    ran1 = rand.nextFloat() - 0.5F;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() - 0.5D + 0.05D * i + newPos1[1] + newPos3[1], ent.getY() + par2 + 0.6D + ran1, ent.getZ() - 0.5D + 0.05D * i + newPos1[0] + newPos3[0], newPos3[1] * 0.5D * ran2, 0.05D * ran2, newPos3[0] * 0.5D * ran2);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() - 0.5D + 0.05D * i + newPos2[1] + newPos3[1], ent.getY() + par2 + 0.6D + ran1, ent.getZ() - 0.5D + 0.05D * i + newPos2[0] + newPos3[0], newPos3[1] * 0.5D * ran3, 0.05D * ran3, newPos3[0] * 0.5D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() - 0.5D + 0.05D * i + newPos1[1] + newPos3[1], ent.getY() + par2 + 0.9D + ran1, ent.getZ() - 0.5D + 0.05D * i + newPos1[0] + newPos3[0], newPos3[1] * 0.5D * ran3, 0.05D * ran3, newPos3[0] * 0.5D * ran3);
                    level.addParticle(ParticleTypes.LARGE_SMOKE, ent.getX() - 0.5D + 0.05D * i + newPos2[1] + newPos3[1], ent.getY() + par2 + 0.9D + ran1, ent.getZ() - 0.5D + 0.05D * i + newPos2[0] + newPos3[0], newPos3[1] * 0.5D * ran2, 0.05D * ran2, newPos3[0] * 0.5D * ran2);
                }
            }
            break;
            case 11:    //color beam INWARD
            {
                //													 ent, type, scale, radius, beam speed, beam thick, 4~7:RGBA, height
                ParticleSphereLight light1 = new ParticleSphereLight(ent, 0, ent.getBbHeight() * 1.2F, ent.getBbHeight() * 0.75F, 0.8F, 0.03F, (float) par1, (float) par2, (float) par3, 1F, ent.getBbHeight() * 0.4F);
                Minecraft.getInstance().particleEngine.add(light1);
            }
            break;
            case 12:    //color gradient OUTWARD
            {
                if (ent instanceof IShipEmotion) ((IShipEmotion) ent).setAttackTick(100);
                //											  ent, type, scale, fade, speed, space, 4~7:RGBA, height
                ParticleGradient grad1 = new ParticleGradient(level,ent, 1, ent.getBbHeight() * 1.2F, 0.85F, 0.08F, 8F, (float) par1, (float) par2, (float) par3, 0.7F);
                Minecraft.getInstance().particleEngine.add(grad1);
            }
            break;
            case 13:    //abyssal goddess particle
            {
                //											 ent, type, scale, rad, NO_USE, NO_USE, 4~7:RGBA, height
                ParticleSparkle spark1 = new ParticleSparkle(ent, 8, 0.1F, ent.getBbWidth() * 2F, 0F, 0F, (float) par1, (float) par2, (float) par3, 1F, ent.getBbHeight() * 0.4F);
                Minecraft.getInstance().particleEngine.add(spark1);
            }
            break;
            case 14:    //double color gradient OUTWARD
            {
                //											  ent, type, scale, fade, speed, space, 4~7:RGBA, height
                ParticleGradient grad1 = new ParticleGradient(level, ent, 2, ent.getBbHeight() * 1.5F, 0.7F, 0.4F, 0F, (float) par1, (float) par2, (float) par3, 0.8F, 40F, 1.6F);
                ParticleGradient grad2 = new ParticleGradient(level, ent, 2, ent.getBbHeight() * 1.3F, 0.7F, 0.3F, 0F, 1F, 1F, 1F, 1F, 40F, 1.5F);
                Minecraft.getInstance().particleEngine.add(grad1);
                Minecraft.getInstance().particleEngine.add(grad2);
            }
            break;
            case 15:    //color sword sweep vertical
            {
                if (ent instanceof IShipEmotion) ((IShipEmotion) ent).setAttackTick(50);
                //									   ent, type, scale1, scale2, scale3, fade, age, RGBA
                ParticleSweep swp1 = new ParticleSweep(level, ent, 0, ent.getBbHeight(), ent.getBbHeight() * 5.6F, ent.getBbHeight() * 2F, 0.95F, 4F, (float) par1, (float) par2, (float) par3, 1F);
                Minecraft.getInstance().particleEngine.add(swp1);
            }
            break;
            case 16:    //color sword sweep horizontal
            {
                if (ent instanceof IShipEmotion) ((IShipEmotion) ent).setAttackTick(50);
                //									   ent, type, scale1, scale2, scale3, fade, age, RGBA
                ParticleSweep swp1 = new ParticleSweep(level, ent, 0, ent.getBbHeight() * 0.1F, ent.getBbHeight() * 5.6F, ent.getBbHeight() * 6F, 0.95F, 4F, (float) par1, (float) par2, (float) par3, 1F);
                Minecraft.getInstance().particleEngine.add(swp1);
            }
            break;
            case 17:    //eye sparkle
            {
                //											 ent, type, height, eye x, eye z, 4~7:RGBA, height
                ParticleSparkle spark1 = new ParticleSparkle(ent, 1, (float) par1, (float) par2, (float) par3, 0F, 1F, 1F, 1F);
                Minecraft.getInstance().particleEngine.add(spark1);
            }
            break;
            case 18:    //raytrace target indicator, parms: target entity, type, hit height, hit side, NO_USE
            {
                ParticleDebugPlane plane = new ParticleDebugPlane(ent, 0, (float) par1, (float) par2, (float) par3);
                Minecraft.getInstance().particleEngine.add(plane);
            }
            break;
            case 19:    //raytrace body cube indicator, parms: target entity, type, cube top, cube bottom, bodyID
            {
                if (ent instanceof BasicEntityShip) {
                    ParticleDebugPlane plane = new ParticleDebugPlane(ent, 1, (float) par1, (float) par2, (float) par3);
                    Minecraft.getInstance().particleEngine.add(plane);
                }
            }
            break;
            case 20:    //raytrace body cube indicator, parms: target entity, type, cube top, cube bottom, bodyID
            {
                if (ent instanceof BasicEntityShip) {
                    ParticleDebugPlane plane = new ParticleDebugPlane(ent, 2, (float) par1, (float) par2, (float) par3);
                    Minecraft.getInstance().particleEngine.add(plane);
                }
            }
            break;
            case 21:    //fill fluid particle
            {
                //											 ent, type, scale, rad, NO_USE, NO_USE, 4~7:RGBA, height
                ParticleSparkle spark1 = new ParticleSparkle(ent, 0, 0.025F, ent.getBbWidth() * 1.5F, 0F, 0F, (float) par1, (float) par2, (float) par3, 1F, ent.getBbHeight() * 0.4F);
                Minecraft.getInstance().particleEngine.add(spark1);
            }
            break;
            case 22:    //craning particle
            {
                //											 ent, type, scale, rad, NO_USE, NO_USE, 4~7:RGBA, height
                ParticleSparkle spark1 = new ParticleSparkle(ent, 3, 0.05F, ent.getBbWidth(), 0F, 0F, (float) par1, (float) par2, (float) par3, 1F, ent.getBbHeight() * 0.4F);
                Minecraft.getInstance().particleEngine.add(spark1);
            }
            break;
            case 23:    //healing particle
            {
                //											 ent, type, scale, rad, NO_USE, NO_USE, 4~7:RGBA, height
                ParticleSparkle spark1 = new ParticleSparkle(ent, 2, 0.075F, ent.getBbWidth() * 1.5F, 0F, 0F, (float) par1, (float) par2, (float) par3, 1F, ent.getBbHeight() * 0.4F);
                Minecraft.getInstance().particleEngine.add(spark1);
            }
            break;
            case 24:    //color beam INWARD custom
            {
                //													 ent, type, life, scale
                ParticleSphereLight light1 = new ParticleSphereLight(ent, (int) par1, (float) par2, (float) par3);
                Minecraft.getInstance().particleEngine.add(light1);
            }
            break;
            case 36:    //emotion
            {
                ParticleEmotion partEmo = new ParticleEmotion(level, ent,
                        ent.getX(), ent.getY(), ent.getZ(), (float) par1, (int) par2, (int) par3);
                Minecraft.getInstance().particleEngine.add(partEmo);
            }
            break;
            default:
                break;
        }
    }

    /**
     * Spawn particle at entity position with diverse parms
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity ent, byte type, double[] parms) {
        //null check
        if (ent == null) return;

        Level level = Minecraft.getInstance().level;

        switch (type) {
            case 1:   //missile spray
            case 2: {
                ParticleSpray particleSpray1 = new ParticleSpray(ent, type, parms);
                Minecraft.getInstance().particleEngine.add(particleSpray1);
            }
            break;
        }
    }
    @OnlyIn(Dist.CLIENT)
    private static void spawnSparkleParticleClient(Entity entity, int type, float... parms) {
        Minecraft.getInstance().particleEngine.add(
                new ParticleSparkle(entity, type, parms));
    }

    /**
     * Spawn a sparkle particle on an entity.
     *
     * @param entity the entity
     * @param type   sparkle visual type
     * @param parms  additional parameters
     */
    public static void spawnSparkleParticle(Entity entity, int type, float... parms) {
        if (entity.level().isClientSide()) {
            spawnSparkleParticleClient(entity, type, parms);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnSprayParticleClient(Level level, double x, double y, double z, int count) {
        ClientLevel clientLevel = (ClientLevel) level;
        for (int i = 0; i < count; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;
            double speedY = level.random.nextDouble() * 0.2 + 0.1;
            Minecraft.getInstance().particleEngine.add(
                    new ParticleSpray(clientLevel, x + offsetX, y, z + offsetZ, 0, speedY, 0, 0));
        }
    }

    public static void spawnSprayParticle(Level level, double x, double y, double z, int count) {
        if (level.isClientSide()) {
            spawnSprayParticleClient(level, x, y, z, count);
        }
    }

    public static void spawnTeamCircleAtPlayer(net.minecraft.server.level.ServerPlayer player, double x, double y, double z, int teamId) {
        if (player == null) return;
        ModNetworking.sendToPlayer(
                new S2CSpawnParticle(25, x, y, z, TEAM_CIRCLE_ENTITY_SCALE, teamId, 0.0),
                player);
    }

    /**
     * Spawn particle with host, target and setting attack time
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity host, Entity target, double par1, double par2, double par3, byte type, boolean setAtkTime) {
        //null check
        if (host == null || target == null) return;

        ClientLevel level = Minecraft.getInstance().level;
        LivingEntity host2 = null;

        //set attack time, EntityLivingBase only
        if (setAtkTime && host instanceof IShipEmotion) {
            ((IShipEmotion) host).setAttackTick(50);
        }

        //get target position
        double ran1 = 0D;
        double ran2 = 0D;
        double ran3 = 0D;
        float[] newPos1;
        float[] newPos2;
        float degYaw = 0F;

        //spawn particle
        switch (type) {
            case 0:        //雙光束砲
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                ParticleLaserNoTexture laser1 = new ParticleLaserNoTexture(level, host2, target, 0.9F, par1, 0F, 0.05F, 0);
                Minecraft.getInstance().particleEngine.add(laser1);

                ParticleLaserNoTexture laser2 = new ParticleLaserNoTexture(level, host2, target, -0.9F, par1, 0F, 0.05F, 0);
                Minecraft.getInstance().particleEngine.add(laser2);
            }
            break;
            case 1:        //yamato cannon beam
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                //beam head
                ParticleCube cube1 = new ParticleCube(level, host2, par1, par2, par3, 2.5F, 1);
                Minecraft.getInstance().particleEngine.add(cube1);

                //beam body
                ParticleLaserNoTexture laser3 = new ParticleLaserNoTexture(level, host2, target, par1, par2, par3, 2F, 1);
                Minecraft.getInstance().particleEngine.add(laser3);
            }
            break;
            case 2:        //yamato cannon beam for boss
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                //beam head
                ParticleCube cube2 = new ParticleCube(level, host2, par1, par2, par3, 5F, 1);
                Minecraft.getInstance().particleEngine.add(cube2);

                //beam body
                ParticleLaserNoTexture laser4 = new ParticleLaserNoTexture(level, host2, target, par1, par2, par3, 4F, 1);
                Minecraft.getInstance().particleEngine.add(laser4);
            }
            break;
            case 3:        //守衛標示線: entity類
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                ParticleLaserNoTexture laser5 = new ParticleLaserNoTexture(level, host2, target, 0D, 0D, 0D, 0.1F, 2);
                Minecraft.getInstance().particleEngine.add(laser5);
            }
            break;
            case 4:        //補給標示線
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                ParticleLaserNoTexture laser6 = new ParticleLaserNoTexture(level, host2, target, 0D, 0D, 0D, 0.1F, 4);
                Minecraft.getInstance().particleEngine.add(laser6);
            }
            break;
            case 5:        //位置標示線
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }

                ParticleLaserNoTexture laser = new ParticleLaserNoTexture(level, host2, target, 0D, 0D, 0D, 0.1F, 5);
                Minecraft.getInstance().particleEngine.add(laser);
            }
            break;
            case 6:        //紫色可調粗細光束
            {
                //host check
                if (host instanceof LivingEntity) {
                    host2 = (LivingEntity) host;
                } else {
                    return;
                }
                //																			   height, scale out, scale in, NO_USE, type
                ParticleLaserNoTexture laser = new ParticleLaserNoTexture(level, host2, target, par1, par2, par3, 0F, 6);
                Minecraft.getInstance().particleEngine.add(laser);
            }
            break;
            default:
                break;
        }
    }

    /**
     * render text
     */
    @OnlyIn(Dist.CLIENT)
    public static void spawnAttackParticleAt(String text, double posX, double posY, double posZ, byte type, int... parms) {
        //null check
        if (text == null || text.length() < 1) return;

        ClientLevel level = Minecraft.getInstance().level;

        //spawn particle
        switch (type) {
            case 0:        //show text on fixed position
            {
                ParticleTextsCustom ptx = new ParticleTextsCustom(null, level, posX, posY, posZ, 1F, 0, text, parms);
                Minecraft.getInstance().particleEngine.add(ptx);
            }
            break;
            case 1:        //show text on entity
            {
                Entity host = Objects.requireNonNull(level).getEntity((int) parms[2]);
                if (host == null) return;

                ParticleTextsCustom ptx = new ParticleTextsCustom(host, level, posX, posY, posZ, 1F, 1, text, parms);
                Minecraft.getInstance().particleEngine.add(ptx);
            }
            break;
        }
    }
    /**
     * Spawn emotion particles on an entity using custom ParticleEmotion.
     * Emotion types correspond to the original mod's emotion system:
     * 0 = none, 1 = heart, 2 = note, 3 = angry, 4 = sweat, etc.
     *
     * @param entity      the entity to spawn particles on
     * @param emotionType the emotion type ID
     */
    public static void spawnEmotionParticle(Entity entity, int emotionType) {
        if (entity.level().isClientSide() && emotionType > 0) {
            spawnEmotionParticleClient(entity, emotionType);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnEmotionParticleClient(Entity entity, int emotionType) {
        ClientLevel level = (ClientLevel) entity.level();
        Minecraft.getInstance().particleEngine.add(
                new ParticleEmotion(level, entity,
                        entity.getX(), entity.getY() + entity.getBbHeight(),
                        entity.getZ(), entity.getBbHeight(), 0, emotionType));
    }
    /**
     * Spawn team circle indicator particle at a world position.
     * Used for pointer block/waypoint target visualization.
     */
    public static void spawnTeamCircleAt(Level level, double x, double y, double z, int teamId) {
        if (level.isClientSide()) {
            spawnTeamCircleAtClient((ClientLevel) level, x, y, z, teamId);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnTeamCircleAtClient(ClientLevel level, double x, double y, double z, int teamId) {
        Minecraft.getInstance().particleEngine.add(
                new ParticleTeam(level, TEAM_CIRCLE_ENTITY_SCALE, teamId, x, y, z));
    }
    /**
     * Spawn team circle indicator particle below an entity using custom
     * ParticleTeam.
     *
     * @param entity the entity to show the team circle for
     * @param teamId the team ID (determines color)
     */
    public static void spawnTeamCircle(Entity entity, int teamId) {
        if (entity.level().isClientSide()) {
            spawnTeamCircleClient(entity, teamId);
        }
    }
    @OnlyIn(Dist.CLIENT)
    private static void spawnTeamCircleClient(Entity entity, int teamId) {
        ClientLevel level = (ClientLevel) entity.level();
        Minecraft.getInstance().particleEngine.add(
                new ParticleTeam(level, entity, TEAM_CIRCLE_ENTITY_SCALE, teamId));
    }

    public static void spawnStickyLightningParticle(Entity entity, float scale, int life, int type) {
        if (entity.level().isClientSide()) {
            spawnStickyLightningParticleClient(entity, scale, life, type);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnStickyLightningParticleClient(Entity entity, float scale, int life, int type) {
        ClientLevel level = (ClientLevel) entity.level();
        // 2026/04/07：GitHub Copilotによって確認済み
        // Keep legacy visual density: railgun beam emitted 4 sticky-lightning strips
        // per tick.
        for (int i = 0; i < 4; i++) {
            Minecraft.getInstance().particleEngine.add(
                    new ParticleStickyLightning(level, entity, scale, life, type));
        }
    }

    public static void spawnSphereLightParticle(Entity entity, int type, float... parms) {
        if (entity.level().isClientSide()) {
            spawnSphereLightParticleClient(entity, type, parms);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void spawnSphereLightParticleClient(Entity entity, int type, float... parms) {
        Minecraft.getInstance().particleEngine.add(
                new ParticleSphereLight(entity, type, parms));
    }
}
