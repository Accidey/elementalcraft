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
public class SteamSmokeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float startSize;
    private final int fadeOutStart;

    protected SteamSmokeParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {

        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0;
        this.lifetime = 60 + level.random.nextInt(21);
        this.fadeOutStart = (int) (this.lifetime * 0.5);

        this.startSize = 0.15f + level.random.nextFloat() * 0.15f;
        this.quadSize = this.startSize;

        this.rCol = 0.9f + level.random.nextFloat() * 0.1f;
        this.gCol = 0.9f + level.random.nextFloat() * 0.1f;
        this.bCol = 0.9f + level.random.nextFloat() * 0.1f;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

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

        if (this.age >= this.fadeOutStart) {
            float progress = (float) (this.age - this.fadeOutStart) / (this.lifetime - this.fadeOutStart);
            this.alpha = 1.0f - progress;
        }

        this.quadSize = this.startSize;

        this.move(this.xd, this.yd, this.zd);
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
            return new SteamSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
