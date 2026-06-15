package com.lulan.shincolle.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SPRAY PARTICLE
 * Modified from cloud particle, used for liquid movement effects.
 * Supports multiple color/behavior types.
 * <p>
 * Ported from 1.10.2 to 1.20.1.
 */
@OnlyIn(Dist.CLIENT)
public class ParticleSpray extends TextureSheetParticle {

    private static SpriteSet sharedSprites;
    private final int ptype;
    private float pScale;
    private double speedLimit;

    /**
     * Constructor with explicit position and motion.
     */
    public ParticleSpray(ClientLevel level, double posX, double posY, double posZ,
                         double motionX, double motionY, double motionZ, int type) {
        super(level, posX, posY, posZ);

        this.ptype = type;
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;

        // Color, speed, life setting
        switch (this.ptype) {
            case 1: // white
                this.speedLimit = 0.25D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 1F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 2: // cyan
                this.speedLimit = 0.3D;
                this.rCol = 0.5F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 1F;
                this.quadSize *= 1.5F;
                this.pScale = (float) (this.quadSize * motionZ);
                this.lifetime = 40;
                this.xd = 0D;
                this.zd = 0D;
                break;
            case 3: // green
                this.speedLimit = 0.3D;
                this.rCol = 0.2F;
                this.gCol = 1F;
                this.bCol = 0.6F;
                this.alpha = 0.7F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 10;
                break;
            case 4: // 0.8A red
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 0F;
                this.bCol = 0F;
                this.alpha = 0.8F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 5: // 0.5A white
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.5F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 6: // 0.5A LARGE white
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.5F;
                this.quadSize *= 1.5F;
                this.pScale = 15F;
                this.lifetime = 50;
                this.yd = 0D;
                break;
            case 7: // light cyan
                this.speedLimit = 0.3D;
                this.rCol = 0.7F;
                this.gCol = 0.94F;
                this.bCol = 1F;
                this.alpha = 1F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 8: // yellow
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 0.6F;
                this.alpha = 1F;
                this.quadSize *= 3F;
                this.pScale = this.quadSize;
                this.lifetime = 20;
                break;
            case 9: // orange
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 0.35F;
                this.bCol = 0F;
                this.alpha = 0.8F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 10: // transparent cyan
                this.speedLimit = 0.3D;
                this.rCol = 0.5F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.2F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 11: // transparent red
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 0F;
                this.bCol = 0F;
                this.alpha = 0.2F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 12: // transparent white
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.5F;
                this.quadSize *= 0.75F;
                this.pScale = this.quadSize;
                this.lifetime = 50;
                break;
            case 13: // next waypoint
                this.speedLimit = 2D;
                this.rCol = 1F;
                this.gCol = 0F;
                this.bCol = 0F;
                this.alpha = 0.5F;
                this.quadSize *= 3F;
                this.pScale = this.quadSize;
                this.lifetime = 100;
                break;
            case 14: // paired chest
                this.speedLimit = 2D;
                this.rCol = 0.5F;
                this.gCol = 0F;
                this.bCol = 0.5F;
                this.alpha = 0.5F;
                this.quadSize *= 3F;
                this.pScale = this.quadSize;
                this.lifetime = 100;
                break;
            case 15: // transparent cyan
                this.speedLimit = 0.3D;
                this.rCol = 0.7F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.75F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                break;
            case 16: // XZ moving white, motionY = scale
                this.yd = 0D;
                this.speedLimit = 0.25D;
                this.rCol = 1F;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 1F;
                this.quadSize = (float) motionY * 3F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
                this.x += (random.nextDouble() - 0.5D) * motionY;
                this.y += (random.nextDouble() - 0.5D) * motionY * 0.15D;
                this.z += (random.nextDouble() - 0.5D) * motionY;
                this.xd *= 1.5D;
                this.zd *= 1.5D;
                break;
            default: // default = type 0 = 1A red
                this.speedLimit = 0.3D;
                this.rCol = 1F;
                this.gCol = 0F;
                this.bCol = 0F;
                this.alpha = 0.7F;
                this.quadSize *= 1.5F;
                this.pScale = 15F;
                this.lifetime = 40;
                this.yd = 0D;
                break;
        }

        // Speed limit
        double motsq = this.xd * this.xd + this.yd * this.yd + this.zd * this.zd;

        if (motsq > this.speedLimit * this.speedLimit) {
            motsq = Math.sqrt(motsq);
            this.xd = this.speedLimit * this.xd / motsq;
            this.yd = this.speedLimit * this.yd / motsq;
            this.zd = this.speedLimit * this.zd / motsq;
        }

        // Reset pos
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Pick sprite
        if (sharedSprites != null) {
            this.pickSprite(sharedSprites);
        }
    }

    /**
     * Constructor with entity host.
     * Type:
     * 1: transparent cyan missile trail
     * 2: transparent red missile trail
     * <p>
     * Data: [0] = velocity magnitude, [1] = trail index
     */
    public ParticleSpray(Entity host, int type, double[] data) {
        super((ClientLevel) host.level(), host.getX(), host.getY(), host.getZ());
        this.ptype = type;

        double hostDx = host.getDeltaMovement().x();
        double hostDy = host.getDeltaMovement().y();
        double hostDz = host.getDeltaMovement().z();

        // Color, speed, life setting
        switch (this.ptype) {
            case 1: // transparent cyan
            {
                this.x = host.getX() + hostDx * 2D - hostDx * 1.5D * data[1];
                this.y = host.getY() + hostDy * 2D - hostDy * 1.5D * data[1] + 0.5D;
                this.z = host.getZ() + hostDz * 2D - hostDz * 1.5D * data[1];
                this.xd = -hostDx * 0.1D;
                this.yd = -hostDy * 0.1D;
                this.zd = -hostDz * 0.1D;
                this.speedLimit = 2D;

                float velred = 1.4F - (float) data[0];
                if (velred > 1F)
                    velred = 1F;
                else if (velred < 0F)
                    velred = 0F;

                this.rCol = velred;
                this.gCol = 1F;
                this.bCol = 1F;
                this.alpha = 0.75F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
            }
            break;
            case 2: // transparent red
            {
                this.x = host.getX() + hostDx * 2D - hostDx * 1.5D * data[1];
                this.y = host.getY() + hostDy * 2D - hostDy * 1.5D * data[1] + 0.5D;
                this.z = host.getZ() + hostDz * 2D - hostDz * 1.5D * data[1];
                this.xd = -hostDx * 0.1D;
                this.yd = -hostDy * 0.1D;
                this.zd = -hostDz * 0.1D;
                this.speedLimit = 2D;

                float velgb = ((float) data[0] - 0.2F) * 3.333F;
                if (velgb > 1F)
                    velgb = 1F;
                else if (velgb < 0F)
                    velgb = 0F;

                this.rCol = 1F;
                this.gCol = velgb;
                this.bCol = velgb;
                this.alpha = 0.75F;
                this.quadSize *= 1.5F;
                this.pScale = this.quadSize;
                this.lifetime = 40;
            }
            break;
        }

        // Speed limit
        double motsq = this.xd * this.xd + this.yd * this.yd + this.zd * this.zd;

        if (motsq > this.speedLimit * this.speedLimit) {
            motsq = Math.sqrt(motsq);
            this.xd = this.speedLimit * this.xd / motsq;
            this.yd = this.speedLimit * this.yd / motsq;
            this.zd = this.speedLimit * this.zd / motsq;
        }

        // Reset pos
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Pick sprite
        if (sharedSprites != null) {
            this.pickSprite(sharedSprites);
        }
    }

    /**
     * Set the shared SpriteSet used by all ParticleSpray instances.
     * Should be called during particle provider registration.
     */
    public static void setSharedSprites(SpriteSet s) {
        sharedSprites = s;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 240 | (240 << 16); // Full brightness (like original getBrightnessForRender returning 240)
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        // Skip first frame like original
        if (this.age == 1)
            return;

        // Scale based on age (matches original f6 logic)
        float f6 = (this.age + partialTick) / this.lifetime * 32F;
        if (f6 < 0F)
            f6 = 0F;
        if (f6 > 1F)
            f6 = 1F;
        this.quadSize = this.pScale * f6;

        super.render(buffer, camera, partialTick);
    }

    @Override
    public void tick() {
        // Update pos
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        // Set dead if max age
        if (this.age++ > this.lifetime) {
            this.remove();
            return;
        }

        switch (this.ptype) {
            case 13:
            case 14: {
                // Animate sprite (slower animation: 4 frames over lifetime)
                if (sharedSprites != null) {
                    int spriteIdx = 7 - this.age * 4 / this.lifetime;
                    if (spriteIdx < 0)
                        spriteIdx = 0;
                    if (spriteIdx > 7)
                        spriteIdx = 7;
                    this.setSprite(sharedSprites.get(spriteIdx, 7));
                }

                this.x += this.xd;
                this.y += this.yd;
                this.z += this.zd;
            }
            break;
            default: {
                // Animate sprite (standard animation: 8 frames over lifetime)
                if (sharedSprites != null) {
                    int spriteIdx = 7 - this.age * 8 / this.lifetime;
                    if (spriteIdx < 0)
                        spriteIdx = 0;
                    if (spriteIdx > 7)
                        spriteIdx = 7;
                    this.setSprite(sharedSprites.get(spriteIdx, 7));
                }

                this.xd *= 0.96D;
                this.yd *= 0.96D;
                this.zd *= 0.96D;

                if (this.onGround) {
                    this.xd *= 0.7D;
                    this.zd *= 0.7D;
                }

                this.move(this.xd, this.yd, this.zd);
            }
            break;
        }
    }

}
