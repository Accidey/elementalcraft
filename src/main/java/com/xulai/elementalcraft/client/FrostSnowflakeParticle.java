package com.xulai.elementalcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class FrostSnowflakeParticle extends TextureSheetParticle {

    private static final Random RANDOM = new Random();

    private final SpriteSet sprites;
    private final float startSize;

    private final int fadeInTicks;
    private final int fadeOutTicks;

    private final double fallSpeed;

    protected FrostSnowflakeParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 60, 20);
    }

    protected FrostSnowflakeParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites,
            int lifetime) {
        this(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, lifetime, Math.max(1, lifetime / 3));
    }


    protected FrostSnowflakeParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites,
            int lifetime,
            int fadeInTicks) {

        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0;
        this.lifetime = Math.max(1, lifetime);
        this.fadeInTicks = Math.min(fadeInTicks, this.lifetime / 2);
        this.fadeOutTicks = Math.max(1, this.lifetime / 4);

        this.startSize = (0.1f + RANDOM.nextFloat() * 0.2f) * 0.3f;
        this.quadSize = 0;

        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;

        this.fallSpeed = -0.008 - RANDOM.nextDouble() * 0.024;

        this.xd = 0;
        this.yd = this.fallSpeed;
        this.zd = 0;

        this.alpha = 1.0f;

        this.pickSprite(sprites);
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

        this.quadSize = this.startSize;

        this.alpha = 1.0f;

        this.oRoll = this.roll;
        this.roll += 0.01f;

        this.yd = this.fallSpeed;

        this.move(this.xd, this.yd, this.zd);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {

            int lifetime = 20 + level.random.nextInt(41);
            return new FrostSnowflakeParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, lifetime);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LongLivedFactory implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public LongLivedFactory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {

            int lifetime = 40 + level.random.nextInt(101);
            return new FrostSnowflakeParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, lifetime);
        }
    }
}
