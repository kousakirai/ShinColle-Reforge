package com.lulan.shincolle.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Custom scale smoke particle.
 * Ported from 1.10.2 ParticleSmoke which extended ParticleSmokeNormal.
 */
@OnlyIn(Dist.CLIENT)
public class ParticleSmoke extends TextureSheetParticle {

    private static SpriteSet sharedSprites;

    public static void setSharedSprites(SpriteSet sprites) {
        sharedSprites = sprites;
    }

    private final SpriteSet sprites;

    // 旧来のコードから直接呼べるよう、sprites省略版のコンストラクタを追加
    public ParticleSmoke(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed, float scale) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, scale, sharedSprites);
    }

    public ParticleSmoke(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed, float scale, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.quadSize *= scale * 0.5F;
        this.lifetime = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        this.gravity = 3.0E-6F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.setSpriteFromAge(this.sprites);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;
        this.move(this.xd, this.yd, this.zd);
    }

}
