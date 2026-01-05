// src/main/java/com/xulai/elementalcraft/util/EffectHelper.java
package com.xulai.elementalcraft.util;

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

import java.util.Random;

/**
 * EffectHelper
 * <p>
 * 中文说明：
 * 视觉与音效辅助工具类。
 * 负责处理复杂的粒子数学计算（如贝塞尔曲线、圆环扩散、球体爆炸等）和音效播放。
 * 将视觉表现逻辑从核心业务逻辑中剥离，保持代码整洁。
 * <p>
 * English Description:
 * Visual and Sound Effect Helper Utility.
 * Handles complex particle math calculations (Bezier curves, ring diffusion, sphere explosions) and sound playback.
 * Decouples visual presentation logic from core business logic to keep code clean.
 */
public class EffectHelper {

    private static final Random RANDOM = new Random();

    /**
     * 播放：寄生吸取特效 (Target -> Attacker)。
     * 效果：粒子从目标位置呈流线型飞向攻击者，模拟能量吸取。
     * <p>
     * Play: Parasitic Drain FX.
     * Effect: Particles flow from target to attacker, simulating energy siphon.
     */
    public static void playDrainEffect(Entity attacker, Entity target) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        Vec3 start = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 end = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);
        Vec3 diff = end.subtract(start);
        double dist = diff.length();
        
        int points = (int) (dist * 5); // 每格 5 个粒子

        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            // 简单的线性插值，稍微带点随机偏移模拟“流体”的不稳定性
            // Linear interpolation with slight random offset to simulate fluid instability
            double x = Mth.lerp(t, start.x, end.x) + (RANDOM.nextGaussian() * 0.05);
            double y = Mth.lerp(t, start.y, end.y) + (RANDOM.nextGaussian() * 0.05);
            double z = Mth.lerp(t, start.z, end.z) + (RANDOM.nextGaussian() * 0.05);

            // 绿色生物质粒子
            level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 1, 0, 0, 0, 0);
            
            // 间歇性加入水泡粒子
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 1, 0, 0.05, 0, 0);
            }
        }
    }

    /**
     * 播放：毒火爆燃特效。
     * 效果：混合了火焰、黑烟和绿色毒气的爆炸效果。
     * <p>
     * Play: Toxic Blast FX.
     * Effect: Explosion mixed with fire, black smoke, and green toxic gas.
     */
    public static void playToxicBlast(Level level, Vec3 pos, double radius) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1. 核心爆炸烟雾
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

        // 2. 向四周扩散的毒气 (SNEEZE 是绿色的)
        int particleCount = (int) (radius * 20);
        serverLevel.sendParticles(ParticleTypes.SNEEZE, 
                pos.x, pos.y + 0.5, pos.z, 
                particleCount, radius / 2, radius / 2, radius / 2, 0.1);

        // 3. 散落的火星
        serverLevel.sendParticles(ParticleTypes.LAVA, 
                pos.x, pos.y + 0.5, pos.z, 
                10, 0.5, 0.5, 0.5, 0.2);
    }

    /**
     * 播放：野火喷射特效 (Wildfire Ejection)。
     * 效果：模拟 Warden 的声波爆炸 (SONIC_BOOM)，并伴随火焰冲击波。
     * <p>
     * Play: Wildfire Ejection FX.
     * Effect: Simulates Warden's Sonic Boom, accompanied by a fire shockwave.
     */
    public static void playWildfireEjection(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;

        // 1. 核心声波爆炸 (Sonic Boom)
        // 这里的 Y 轴位置取身体中心
        level.sendParticles(ParticleTypes.SONIC_BOOM, 
                center.getX(), center.getY() + center.getBbHeight() * 0.5, center.getZ(), 
                1, 0, 0, 0, 0);

        // 2. 播放 Warden 声波轰鸣
        level.playSound(null, center.getX(), center.getY(), center.getZ(), 
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);

        // 3. 伴随火焰扩散圆环
        playShockwave(center, radius);
    }

    /**
     * 播放：孢子扩散传染特效 (Spore Contagion)。
     * 效果：以传染源为中心，向外扩散的一圈绿色孢子云。
     * <p>
     * Play: Spore Contagion FX.
     * Effect: A ring of green spore cloud expanding outward from the source.
     */
    public static void playSporeContagion(Entity source, double radius) {
        if (!(source.level() instanceof ServerLevel level)) return;

        // 数量随半径增加，确保视觉密度
        int count = (int) (radius * 25);
        
        // 使用 SPORE_BLOSSOM_AIR (大且明显的绿色空气孢子)
        // speed 设为 0.05 让它们有轻微的扩散动态，dx/dy/dz 设置为半径的一半形成云团
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, 
                source.getX(), source.getY() + source.getBbHeight() * 0.5, source.getZ(), 
                count, radius * 0.5, 0.5, radius * 0.5, 0.05);
        
        // 辅以落下的孢子增加层次感
        level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                source.getX(), source.getY() + source.getBbHeight(), source.getZ(),
                count / 3, radius * 0.4, 0.5, radius * 0.4, 0.02);
    }

    /**
     * 播放：通用冲击波特效。
     * 效果：以实体为中心，快速扩散的火焰/烟雾圆环。
     * <p>
     * Play: General Shockwave FX.
     * Effect: A rapidly expanding ring of fire/smoke centered on the entity.
     */
    public static void playShockwave(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;

        int points = (int) (radius * 12); // 圆周上的粒子数量
        double step = (Math.PI * 2) / points;

        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + 0.2;

            // 向外扩散的速度向量
            double vx = Math.cos(angle) * 0.2;
            double vz = Math.sin(angle) * 0.2;

            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, vx, 0, vz, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, vx, 0.1, vz, 0.05);
        }
    }

    /**
     * 播放：孢子附着特效。
     * 效果：少量绿色孢子在头顶盘旋。
     * <p>
     * Play: Spore Status FX.
     * Effect: Green spores hovering around the head.
     */
    public static void playSporeAmbient(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        
        // 使用 WARPED_SPORE (下界诡然菌孢子)
        level.sendParticles(ParticleTypes.WARPED_SPORE, 
                entity.getX(), entity.getEyeY(), entity.getZ(), 
                2, 0.3, 0.3, 0.3, 0.01);
    }

    /**
     * 播放音效的简便方法。
     * <p>
     * Helper method to play sound.
     */
    public static void playSound(Level level, Entity pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ======================== Steam Reaction Effects / 蒸汽反应特效 ========================

    /**
     * 播放：蒸汽云持续特效（Tick）。
     * 效果：在云的范围内随机生成上升的烟雾粒子，高温时伴有火星。
     * <p>
     * Play: Steam Cloud Tick FX.
     * Effect: Randomly spawns rising smoke particles within the cloud radius, with sparks if high heat.
     *
     * @param level 服务端世界 / Server Level
     * @param cloud 蒸汽云实体 / Steam Cloud Entity
     * @param isHighHeat 是否为高温蒸汽 / Is High Heat
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
                if (RANDOM.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, upSpeed * 0.8, 0, 0.5);
                }
                if (RANDOM.nextFloat() < 0.05f) {
                    level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * 播放：蒸汽爆发特效。
     * 效果：一次性的烟雾和声音爆发，高温时更加剧烈。
     * <p>
     * Play: Steam Burst FX.
     * Effect: One-time burst of smoke and sound, more intense if high heat.
     *
     * @param level 服务端世界 / Server Level
     * @param target 目标实体 / Target Entity
     * @param radius 爆发半径 / Burst Radius
     * @param intensity 强度等级 / Intensity Level
     * @param isHighHeat 是否为高温蒸汽 / Is High Heat
     */
    public static void playSteamBurst(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        float volume = isHighHeat ? 0.8F : 0.6F;
        float pitch = isHighHeat ? 1.0F : 1.2F;

        if (isHighHeat && intensity >= 3) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, 0.8F);
        } else {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch);
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
                if (RANDOM.nextFloat() < 0.2f)
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.1, 0, speed);
                if (RANDOM.nextFloat() < 0.3f)
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                if (RANDOM.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);

                if (intensity >= 3 && RANDOM.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.POOF, x, y, z, 0, 0, 0, 0, speed * 1.5);
                }
            } else {
                if (RANDOM.nextBoolean()) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                }
            }
        }
    }
}