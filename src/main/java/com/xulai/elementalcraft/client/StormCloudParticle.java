package com.xulai.elementalcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StormCloudParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final int fadeInEnd;
    private final int fadeOutStart;

    protected StormCloudParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {

        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0;
        this.lifetime = 100 + level.random.nextInt(41);

        this.fadeInEnd = (int) (this.lifetime * 0.15);
        this.fadeOutStart = (int) (this.lifetime * 0.7);

        int texIndex = level.random.nextInt(12);
        float sizeMult = 1.5f + level.random.nextFloat() * 2.0f;
        this.quadSize = 0.5f * sizeMult;

        this.alpha = 0.0f;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        this.setSprite(sprites.get(texIndex, 12));
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

        if (this.age < this.fadeInEnd) {
            this.alpha = (float) this.age / this.fadeInEnd;
        } else if (this.age >= this.fadeOutStart) {
            float progress = (float) (this.age - this.fadeOutStart) / (this.lifetime - this.fadeOutStart);
            this.alpha = 1.0f - progress;
        } else {
            this.alpha = 1.0f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new StormCloudParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
