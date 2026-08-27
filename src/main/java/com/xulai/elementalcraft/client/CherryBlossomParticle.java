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
public class CherryBlossomParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float driftSeed;
    private final float rollSpeed;

    protected CherryBlossomParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {

        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.lifetime = 300;
        this.gravity = 7.5E-4F;
        this.friction = 0.99F;
        this.quadSize = level.random.nextBoolean() ? 0.05F : 0.075F;
        this.setSize(this.quadSize, this.quadSize);
        this.rCol = 0.95f + level.random.nextFloat() * 0.05f;
        this.gCol = 0.5f + level.random.nextFloat() * 0.15f;
        this.bCol = 0.6f + level.random.nextFloat() * 0.15f;
        this.alpha = 0.85f;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.driftSeed = level.random.nextFloat();
        this.rollSpeed = (float)Math.toRadians(level.random.nextBoolean() ? -5.0 : 5.0);
        this.oRoll = (float)Math.toRadians(level.random.nextBoolean() ? -30.0 : 30.0);
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

        float progress = (float)this.age / 300.0F;
        float driftMag = (float)Math.pow(progress, 1.25f) * 2.0f * 0.0025f;
        double dx = Math.cos(Math.toRadians(driftSeed * 60.0f)) * driftMag;
        double dz = Math.sin(Math.toRadians(driftSeed * 60.0f)) * driftMag;
        this.xd += dx;
        this.zd += dz;
        this.yd -= this.gravity;
        this.oRoll += this.rollSpeed / 20.0f;
        this.roll = this.oRoll;

        this.move(this.xd, this.yd, this.zd);

        this.xd *= this.friction;
        this.yd *= this.friction;
        this.zd *= this.friction;

        if (this.age > this.lifetime - 30) {
            float f = (this.lifetime - this.age) / 30.0f;
            this.alpha = 0.85f * f;
        }
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
            return new CherryBlossomParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
