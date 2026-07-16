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

@OnlyIn(Dist.CLIENT)
public class FrostIceRuneParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final int fadeInTicks;
    private final int fadeOutTicks;

    protected FrostIceRuneParticle(
            ClientLevel level,
            double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        this.sprites = sprites;
        this.hasPhysics = false;
        this.gravity = 0;


        this.lifetime = 10;
        this.fadeInTicks = 6;
        this.fadeOutTicks = 6;


        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;


        this.quadSize = 0.075f + level.random.nextFloat() * 0.0375f;


        this.xd = 0;
        this.yd = 1.0d;
        this.zd = 0;


        this.alpha = 0.0f;

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


        if (this.age < this.fadeInTicks) {

            this.alpha = (float) this.age / this.fadeInTicks;
        } else if (this.age >= this.lifetime - this.fadeOutTicks) {

            int ticksUntilEnd = this.lifetime - this.age;
            this.alpha = (float) ticksUntilEnd / this.fadeOutTicks;
        } else {

            this.alpha = 1.0f;
        }


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

            return new FrostIceRuneParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
