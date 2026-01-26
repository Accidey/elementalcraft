// src/main/java/com/xulai/elementalcraft/util/EffectHelper.java
package com.xulai.elementalcraft.util;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/**
 * EffectHelper
 * <p>
 * 中文说明：
 * 视觉与音效辅助工具类。
 * 集中管理模组内的粒子特效生成与音效播放逻辑。
 * 包括：孢子传染、毒气爆燃、蒸汽升腾、生命汲取等视觉效果。
 * <p>
 * English Description:
 * Visual and Sound Effect Helper Utility.
 * Centralizes logic for particle generation and sound playback within the mod.
 * Includes: Spore contagion, toxic combustion, steam rising, life drain, and other visual effects.
 */
@SuppressWarnings("null")
public class EffectHelper {

    private static final Random RANDOM = new Random();

    // 颜色定义：毒雾绿 (R, G, B)
    // Color Definition: Toxic Green (R, G, B)
    private static final Vector3f SMOG_COLOR = new Vector3f(0.1f, 0.8f, 0.2f); 

    /**
     * 播放：孢子扩散传染特效 (Spore Contagion)。
     * <p>
     * 视觉组成：
     * 1. 沉积毒雾 (Stagnant Smog)：地面生成的绿色粉尘。
     * 2. 上升毒烟 (Rising Fumes)：向上飘散的绿色实体特效粒子。
     * 3. 悬浮孢子 (Floating Spores)：空气中盘旋的孢子颗粒。
     * 4. 传输链路 (Transmission Links)：连接传染源与被感染目标的粒子连线。
     * <p>
     * Plays: Spore Contagion Spread FX.
     * <p>
     * Visual Composition:
     * 1. Stagnant Smog: Green dust on the ground.
     * 2. Rising Fumes: Green entity effect particles drifting upwards.
     * 3. Floating Spores: Spore particles hovering in the air.
     * 4. Transmission Links: Particle lines connecting the source to infected targets.
     */
    public static void playSporeContagion(Entity source, List<LivingEntity> targets, double radius) {
        if (!(source.level() instanceof ServerLevel level)) return;

        // 1. 剧毒烟雾圈
        // 1. The Toxic Smog Ring
        
        double circumference = 2 * Math.PI * radius;
        int ringPoints = (int) (circumference * 10); 
        double angleStep = (Math.PI * 2) / ringPoints;
        double baseY = source.getY(); 

        for (int i = 0; i < ringPoints; i++) {
            double angle = angleStep * i;
            double x = source.getX() + Math.cos(angle) * radius;
            double z = source.getZ() + Math.sin(angle) * radius;

            // 沉积毒雾层
            // Stagnant Smog Layer
            if (RANDOM.nextFloat() < 0.5f) {
                Vector3f deepColor = new Vector3f(0.05f, 0.5f, 0.1f);
                double ox = (RANDOM.nextDouble() - 0.5) * 0.3;
                double oz = (RANDOM.nextDouble() - 0.5) * 0.3;
                level.sendParticles(new DustParticleOptions(deepColor, 2.0f), 
                        x + ox, baseY + 0.1, z + oz, 1, 0, 0, 0, 0);
            }

            // 上升毒烟层
            // Rising Fumes Layer
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.ENTITY_EFFECT, 
                        x, baseY + 0.2, z, 0, 0.2, 0.9, 0.2, 1.0);
            }

            // 悬浮孢子层
            // Floating Spores Layer
            if (RANDOM.nextFloat() < 0.2f) {
                double sporeY = baseY + RANDOM.nextDouble() * 1.5;
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, 
                        x, sporeY, z, 1, 0, 0, 0, 0.02);
            }
        }

        // 2. 烟雾传输链路
        // 2. Smog Links
        
        for (LivingEntity target : targets) {
            Vec3 start = source.position().add(0, source.getBbHeight() * 0.5, 0); 
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);   
            
            double dist = start.distanceTo(end);
            int linePoints = (int) (dist * 4); 

            for (int j = 0; j <= linePoints; j++) {
                double t = (double) j / linePoints;
                double lx = Mth.lerp(t, start.x, end.x);
                double ly = Mth.lerp(t, start.y, end.y);
                double lz = Mth.lerp(t, start.z, end.z);

                if (j % 2 == 0) {
                    level.sendParticles(new DustParticleOptions(SMOG_COLOR, 0.8f), lx, ly, lz, 1, 0, 0, 0, 0);
                } else {
                    if (RANDOM.nextFloat() < 0.1f) {
                        level.sendParticles(ParticleTypes.ENTITY_EFFECT, lx, ly, lz, 0, 0.2, 0.9, 0.2, 1.0);
                    }
                }
            }
        }
    }

    /**
     * 播放：易燃孢子持续特效 (Flammable Spore Ambient)。
     * <p>
     * 效果：
     * 在生物身体周围随机生成绿色的悬浮孢子，模拟孢子附着状态。
     * <p>
     * Plays: Flammable Spore Ambient FX.
     * <p>
     * Effect:
     * Randomly spawns green floating spores around the entity to simulate spore attachment status.
     */
    public static void playSporeAmbient(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        
        if (RANDOM.nextFloat() < 0.4f) {
            double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 1.2;
            double y = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 1.2;

            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, 
                    x, y, z, 1, 0, 0, 0, 0.01);
            
            if (RANDOM.nextFloat() < 0.05f) {
                 level.sendParticles(ParticleTypes.ENTITY_EFFECT, 
                    x, y, z, 0, 0.2, 0.9, 0.2, 1.0);
            }
        }
    }

    /**
     * 播放：生命汲取特效 (Drain Effect)。
     * <p>
     * 效果：
     * 生成从目标流向攻击者的粒子流，包含堆肥粒子和气泡。
     * <p>
     * Plays: Life Drain FX.
     * <p>
     * Effect:
     * Creates a particle stream flowing from the target to the attacker, including composter particles and bubbles.
     */
    public static void playDrainEffect(Entity attacker, Entity target) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        Vec3 start = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 end = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);
        Vec3 diff = end.subtract(start);
        int points = (int) (diff.length() * 5);
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double x = Mth.lerp(t, start.x, end.x) + (RANDOM.nextGaussian() * 0.05);
            double y = Mth.lerp(t, start.y, end.y) + (RANDOM.nextGaussian() * 0.05);
            double z = Mth.lerp(t, start.z, end.z) + (RANDOM.nextGaussian() * 0.05);
            level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0, 0.05, 0, 0);
        }
    }

    /**
     * 播放：毒气爆燃特效 (Toxic Combustion Blast)。
     * <p>
     * 效果：
     * 1. 核心闪光：瞬间照亮爆炸中心。
     * 2. 毒气云：大量深绿色粉尘向四周喷射。
     * 3. 燃烧火焰：混合在毒气中的火焰粒子。
     * 4. 熔岩溅射：少量的熔岩颗粒残留。
     * <p>
     * Plays: Toxic Combustion Blast FX.
     * <p>
     * Effect:
     * 1. Core Flash: Instantly illuminates the explosion center.
     * 2. Toxic Cloud: Massive dark green dust spraying outwards.
     * 3. Combustion Flames: Fire particles mixed within the toxic gas.
     * 4. Lava Spatter: Small amount of residual lava particles.
     */
    public static void playToxicBlast(Level level, Vec3 pos, double radius) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1. 核心点火闪光
        // 1. Core Ignition Flash
        serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        // 2. 毒气爆燃云
        // 2. Toxic Combustion Cloud
        int particleCount = (int) (radius * 40); 
        Vector3f toxicGreen = new Vector3f(0.1f, 0.8f, 0.2f); 

        for (int i = 0; i < particleCount; i++) {
            double dx = (RANDOM.nextDouble() - 0.5) * 2.0 * radius;
            double dy = (RANDOM.nextDouble() - 0.5) * 2.0 * radius;
            double dz = (RANDOM.nextDouble() - 0.5) * 2.0 * radius;

            if (dx*dx + dy*dy + dz*dz > radius * radius) continue;

            double pX = pos.x + dx;
            double pY = pos.y + dy;
            double pZ = pos.z + dz;

            double vX = dx * 0.2; 
            double vY = dy * 0.2;
            double vZ = dz * 0.2;

            // 绿色毒烟
            // Green Toxic Smoke
            float scale = 2.0f + RANDOM.nextFloat();
            serverLevel.sendParticles(new DustParticleOptions(toxicGreen, scale), 
                    pX, pY, pZ, 0, vX, vY, vZ, 1.0);

            // 爆燃火焰
            // Combustion Flames
            if (RANDOM.nextFloat() < 0.4f) {
                serverLevel.sendParticles(ParticleTypes.FLAME, 
                        pX, pY, pZ, 0, vX * 1.5, vY * 1.5, vZ * 1.5, 0.5);
            }
        }
        
        // 3. 爆炸残留
        // 3. Residual Effect
        serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 8, 0.5, 0.5, 0.5, 0.2);
    }

    /**
     * 播放：音波冲击与震荡波特效 (Sonic Boom & Shockwave)。
     * <p>
     * 效果：
     * 播放监守者音波粒子，并生成一圈向外扩散的火焰烟雾震荡波。
     * <p>
     * Plays: Sonic Boom & Shockwave FX.
     * <p>
     * Effect:
     * Plays Warden sonic boom particles and generates a ring of fire/smoke shockwave expanding outwards.
     */
    public static void playWildfireEjection(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.getX(), center.getY() + center.getBbHeight() * 0.5, center.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
        playShockwave(center, radius);
    }

    /**
     * 播放：环形震荡波 (Ring Shockwave)。
     * <p>
     * 效果：
     * 生成一圈向外扩散的火焰和烟雾粒子。
     * <p>
     * Plays: Ring Shockwave FX.
     * <p>
     * Effect:
     * Generates a ring of fire and smoke particles expanding outwards.
     */
    public static void playShockwave(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;
        int points = (int) (radius * 12);
        double step = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + 0.2;
            double vx = Math.cos(angle) * 0.2;
            double vz = Math.sin(angle) * 0.2;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, vx, 0, vz, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, vx, 0.1, vz, 0.05);
        }
    }

    /**
     * 播放：通用音效 (Generic Sound Playback)。
     * <p>
     * Plays: Generic Sound Playback.
     */
    public static void playSound(Level level, Entity pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * 播放：蒸汽云持续特效 (Steam Cloud Tick FX)。
     * <p>
     * 效果：
     * 在蒸汽云范围内随机生成上升的烟雾。如果是高温蒸汽，额外生成火焰和熔岩粒子。
     * <p>
     * Plays: Steam Cloud Tick FX.
     * <p>
     * Effect:
     * Randomly spawns rising smoke within the steam cloud. If high heat, additionally spawns fire and lava particles.
     */
    public static void playSteamCloudTick(ServerLevel level, AreaEffectCloud cloud, boolean isHighHeat) {
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;
        int count = Math.max(1, (int) (radius * 0.8));
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY();
            double upSpeed = 0.05 + RANDOM.nextDouble() * 0.08;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, upSpeed, 0, 1.0);
            if (isHighHeat) {
                if (RANDOM.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, upSpeed * 0.8, 0, 0.5);
                if (RANDOM.nextFloat() < 0.05f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
            }
        }
    }

    /**
     * 播放：蒸汽爆发特效 (Steam Burst FX)。
     * <p>
     * 效果：
     * 生成大量烟雾爆发。如果是高温蒸汽，伴随爆炸声和火焰粒子。
     * <p>
     * Plays: Steam Burst FX.
     * <p>
     * Effect:
     * Generates a massive smoke burst. If high heat, accompanied by explosion sounds and fire particles.
     */
    public static void playSteamBurst(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        float volume = isHighHeat ? 0.8F : 0.6F;
        float pitch = isHighHeat ? 1.0F : 1.2F;
        if (isHighHeat && intensity >= 3) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, 0.8F);
        } else {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch);
        }
        int count = (int) (Math.max(1.0, radius) * (isHighHeat ? 20 : 10) * intensity);
        double speed = isHighHeat ? (0.05 + intensity * 0.02) : 0.05;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY() + RANDOM.nextDouble() * target.getBbHeight() + 0.2;
            if (isHighHeat) {
                if (RANDOM.nextFloat() < 0.2f) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.1, 0, speed);
                if (RANDOM.nextFloat() < 0.3f) level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                if (RANDOM.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
                if (intensity >= 3 && RANDOM.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.POOF, x, y, z, 0, 0, 0, 0, speed * 1.5);
            } else {
                if (RANDOM.nextBoolean()) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
            }
        }
    }
}