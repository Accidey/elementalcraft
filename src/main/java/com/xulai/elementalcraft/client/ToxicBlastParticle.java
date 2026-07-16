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
public class ToxicBlastParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float startSize;
    private final int fadeOutStart;

    protected ToxicBlastParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {

        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0;
        // ~5 seconds lifetime (100 ticks), with some variation
        this.lifetime = 90 + level.random.nextInt(21); // 90-110 ticks
        this.fadeOutStart = (int) (this.lifetime * 0.7);

        // expanding smoke cloud
        this.startSize = 0.375f + level.random.nextFloat() * 0.25f;
        this.quadSize = this.startSize;

        // toxic green-yellow tint
        this.rCol = 0.3f + level.random.nextFloat() * 0.2f;
        this.gCol = 0.6f + level.random.nextFloat() * 0.2f;
        this.bCol = 0.1f + level.random.nextFloat() * 0.1f;

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        // start fully visible
        this.alpha = 0.8f;

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

        // slow down over time (explosion drag)
        this.xd *= 0.92;
        this.yd *= 0.92;
        this.zd *= 0.92;

        // slight upward drift for smoke
        this.yd += 0.005;

        // fade out in the last 35% of lifetime
        if (this.age >= this.fadeOutStart) {
            float progress = (float) (this.age - this.fadeOutStart) / (this.lifetime - this.fadeOutStart);
            this.alpha = 0.8f * (1.0f - progress);
        }

        // expand slightly over time
        float ageRatio = (float) this.age / this.lifetime;
        this.quadSize = this.startSize * (1.0f + ageRatio * 0.5f);

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
            return new ToxicBlastParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
