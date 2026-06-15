package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.BuffHelper;
import com.lulan.shincolle.utility.CombatHelper;
import com.lulan.shincolle.utility.ParticleHelper;
import com.lulan.shincolle.utility.TargetHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Floating fortress airplane entity.
 * A heavier aircraft variant that uses heavy ammo.
 * Ported from 1.10.2 EntityFloatingFort.
 */
public class EntityFloatingFort extends BasicEntityAirplane {

    public EntityFloatingFort(EntityType<? extends EntityFloatingFort> type, Level level) {
        super(type, level);
    }

    @Override
    public void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float... par2) {
        if (host instanceof BasicEntityShip ship) {
            float launchY = (float) ship.getY();
            if (par2 != null && par2.length > 0)
                launchY = par2[0];
            this.initAttrsFromHost(ship, target, scaleLevel, launchY);
            this.numAmmoLight = 0;
            this.numAmmoHeavy = 1;
        }
    }

    @Override
    public void tick() {
        if (this.level().isClientSide() && this.isAlive() && (this.tickCount & 1) == 0) {
            // 2026/04/07・哦itHub Copilot縺ｫ繧医▲縺ｦ遒ｺ隱肴ｸ医∩
            ParticleHelper.spawnAttackParticleAt(this.level(), this.getX(), this.getY() + 0.2D, this.getZ(),
                    -this.getDeltaMovement().x * 0.5D, 0.07D, -this.getDeltaMovement().z * 0.5D, 29);
        }

        if (!this.level().isClientSide() && this.isAlive()) {
            if (this.backHome || this.getTarget() == null || !this.getTarget().isAlive() || this.tickCount >= 500) {
                impactExplosion();
                return;
            }
        }

        super.tick();
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host == null) {
            this.discard();
            return false;
        }

        if (target == null || !target.isAlive()) {
            impactExplosion();
            return false;
        }

        // Old behavior detonates only when close to the target.
        if (this.distanceToSqr(target) > 36D) {
            return false;
        }

        if (this.numAmmoHeavy > 0) {
            this.numAmmoHeavy--;
        }

        // 2026/04/07・哦itHub Copilot縺ｫ繧医▲縺ｦ遒ｺ隱肴ｸ医∩
        impactExplosion();
        return true;
    }

    private void impactExplosion() {
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }

        MissileData md = this.getMissileData(2);
        int specialType = md != null ? md.type : 0;
        CombatHelper.specialAttackEffect(this, specialType,
                new float[]{(float) this.getX(), (float) this.getY(), (float) this.getZ()});

        AABB hitBox = this.getBoundingBox().inflate(4.5D, 4.5D, 4.5D);
        List<Entity> hitList = this.level().getEntities(this, hitBox);

        for (Entity ent : hitList) {
            if (!ent.isPickable() || ent == this || ent == this.getHostEntity())
                continue;
            if (TargetHelper.isEntityInvulnerable(ent))
                continue;

            if (ent instanceof IShipOwner owner && this.getPlayerUID() > 0
                    && owner.getPlayerUID() == this.getPlayerUID()) {
                continue;
            }

            float atk = this.shipAttrs.getAttackDamageHeavy();
            atk = CombatHelper.modDamageByAdditionAttrs(this, ent, atk, 0);
            atk = CombatHelper.applyCombatRateToDamage(this, ent, false, 1F, atk);
            atk = CombatHelper.applyDamageReduceOnPlayer(ent, atk);

            if (CombatHelper.isFriendlyFire(this, ent) || atk <= 0F) {
                continue;
            }

            if (ent instanceof LivingEntity living && living.hurt(this.damageSources().mobAttack(this), atk)) {
                BuffHelper.applyBuffOnTarget(ent, this.getAttackEffectMap());
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0D, 0D, 0D, 0D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.8D, 0.8D, 0.8D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.6D, 0.6D, 0.6D, 0.06D);
        }

        // 2026/04/07・哦itHub Copilot縺ｫ繧医▲縺ｦ遒ｺ隱肴ｸ医∩
        this.discard();
    }

    @Override
    public boolean useAmmoLight() {
        return false;
    }

    @Override
    public boolean useAmmoHeavy() {
        return true;
    }
}
