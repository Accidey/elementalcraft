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

import com.xulai.elementalcraft.client.ModParticles;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import java.util.List;
import java.util.Random;

@SuppressWarnings("null")
public class EffectHelper {
    private static final Random RANDOM = new Random();

    private static final Vector3f SMOG_COLOR = new Vector3f(0.1f, 0.8f, 0.2f);
    private static final Vector3f STATIC_PURPLE_BLUE = new Vector3f(0.5f, 0.2f, 1.0f);
    private static final Vector3f PARALYSIS_YELLOW = new Vector3f(1.0f, 1.0f, 0.6f);
    private static final Vector3f TOXIC_GREEN = new Vector3f(0.1f, 0.8f, 0.2f);

    public static void playSporeContagion(Entity source, List<LivingEntity> targets, double radius) {
        if (!(source.level() instanceof ServerLevel level)) return;
        double circumference = 2 * Math.PI * radius;
        int ringPoints = (int) (circumference * 10);
        double angleStep = (Math.PI * 2) / ringPoints;
        double baseY = source.getY();
        for (int i = 0; i < ringPoints; i++) {
            double angle = angleStep * i;
            double x = source.getX() + Math.cos(angle) * radius;
            double z = source.getZ() + Math.sin(angle) * radius;
            if (RANDOM.nextFloat() < 0.5f) {
                Vector3f deepColor = new Vector3f(0.05f, 0.5f, 0.1f);
                double ox = (RANDOM.nextDouble() - 0.5) * 0.3;
                double oz = (RANDOM.nextDouble() - 0.5) * 0.3;
                level.sendParticles(new DustParticleOptions(deepColor, 2.0f),
                        x + ox, baseY + 0.1, z + oz, 1, 0, 0, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.ENTITY_EFFECT,
                        x, baseY + 0.2, z, 0, 0.2, 0.9, 0.2, 1.0);
            }
            if (RANDOM.nextFloat() < 0.2f) {
                double sporeY = baseY + RANDOM.nextDouble() * 1.5;
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        x, sporeY, z, 1, 0, 0, 0, 0.02);
            }
        }
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

    public static void playFreezeAmbient(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (RANDOM.nextFloat() < 0.8f) {
            int count = 2 + RANDOM.nextInt(3);
            for (int i = 0; i < count; i++) {
                double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8;
                double y = entity.getY() + entity.getBbHeight() * 0.5 + RANDOM.nextDouble() * entity.getBbHeight() * 0.5;
                double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8;
                level.sendParticles(ModParticles.FROST_SNOWFLAKE.get(), x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void playParalysisAmbient(Entity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!ElementalVisualConfig.paralysisVisualEnabled) return;

        int stacks = amplifier + 1;
        double radius = ElementalVisualConfig.paralysisSpiralRadius;
        double height = entity.getBbHeight();
        double rotationSpeed = ElementalVisualConfig.paralysisSpiralRotationSpeed;
        double riseSpeed = ElementalVisualConfig.paralysisSpiralRiseSpeed;
        int spiralCount = ElementalVisualConfig.paralysisSpiralBaseCount
                + ElementalVisualConfig.paralysisSpiralCountPerStack * stacks;
        if (spiralCount < 1) spiralCount = 1;

        int cycleTicks = Math.max(10, (int) (height / riseSpeed));
        double angleStep = (Math.PI * 2.0) / spiralCount;

        for (int s = 0; s < spiralCount; s++) {
            double angle = (entity.tickCount * rotationSpeed) + s * angleStep;
            double spiralY = ((entity.tickCount + s * (cycleTicks / spiralCount)) % cycleTicks) / (double) cycleTicks * height;
            double sx = entity.getX() + Math.cos(angle) * radius;
            double sz = entity.getZ() + Math.sin(angle) * radius;
            double sy = entity.getY() + spiralY;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    sx, sy, sz, 1, 0, riseSpeed, 0, 0);
        }

        if (RANDOM.nextFloat() < ElementalVisualConfig.paralysisDustChance) {
            int dustRange = ElementalVisualConfig.paralysisDustCountMax - ElementalVisualConfig.paralysisDustCountMin;
            int count = ElementalVisualConfig.paralysisDustCountMin + (dustRange > 0 ? RANDOM.nextInt(dustRange) : 0);
            float size = (float) ElementalVisualConfig.paralysisDustSize;
            for (int i = 0; i < count; i++) {
                double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * radius * 2;
                double y = entity.getY() + RANDOM.nextDouble() * height;
                double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * radius * 2;
                level.sendParticles(new DustParticleOptions(PARALYSIS_YELLOW, size),
                        x, y, z, 1, 0, riseSpeed * 1.5, 0, 0);
            }
        }

        if (RANDOM.nextFloat() < ElementalVisualConfig.paralysisSparkChance) {
            double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * radius;
            double y = entity.getY() + RANDOM.nextDouble() * height;
            double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * radius;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                    ElementalVisualConfig.paralysisSparkCount, 0.08, 0.08, 0.08, 0);
        }
    }

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



    public static void playToxicBlast(Level level, Vec3 pos, double radius) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        playToxicBlastSmokeFog(serverLevel, pos.x, pos.y, pos.z, radius);
    }

    public static void playToxicBlastSmokeFog(ServerLevel serverLevel, double x, double y, double z, double radius) {
        int totalParticles = (int) (radius * 24);
        int batches = 100;
        spawnSmokeBatch(serverLevel, x, y, z, radius, totalParticles, batches, 0);
    }

    private static void spawnSmokeBatch(ServerLevel serverLevel, double x, double y, double z,
                                         double radius, int totalParticles, int batches, int currentTick) {
        if (currentTick >= batches) return;
        int batchCount = totalParticles / batches;
        if (currentTick < totalParticles % batches) batchCount++;
        for (int i = 0; i < batchCount; i++) {
            double dx = (serverLevel.random.nextDouble() - 0.5) * radius * 1.5;
            double dy = serverLevel.random.nextDouble() * 1.5;
            double dz = (serverLevel.random.nextDouble() - 0.5) * radius * 1.5;
            double vx = dx * 0.015;
            double vy = 0.01 + serverLevel.random.nextDouble() * 0.02;
            double vz = dz * 0.015;
            serverLevel.sendParticles(ModParticles.TOXIC_BLAST.get(),
                    x + dx, y + dy, z + dz,
                    0, vx, vy, vz, 1.0);
        }
        final int nextTick = currentTick + 1;
        serverLevel.getServer().execute(() ->
                spawnSmokeBatch(serverLevel, x, y, z, radius, totalParticles, batches, nextTick));
    }

    public static void playWildfireEjection(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BONE_MEAL_USE, SoundSource.HOSTILE, 2.5F, 1.2F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BONE_MEAL_USE, SoundSource.HOSTILE, 2.5F, 1.3F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.BONE_MEAL_USE, SoundSource.HOSTILE, 2.5F, 1.4F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.HOSTILE, 1.5F, 1.0F);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.0F, 1.2F);
        playShockwave(center, radius);
    }

    public static void playShockwave(Entity center, double radius) {
        if (!(center.level() instanceof ServerLevel level)) return;
        int points = (int) (radius * 16);
        double step = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + 0.2;
            double vx = Math.cos(angle) * 0.25;
            double vz = Math.sin(angle) * 0.25;
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y + RANDOM.nextDouble() * 0.8, z, 2, vx * 0.5, 0.05, vz * 0.5, 0.02);
            level.sendParticles(new DustParticleOptions(SMOG_COLOR, 1.5f + RANDOM.nextFloat()), x, y, z, 1, vx, 0.1, vz, 0.0);
            if (RANDOM.nextFloat() < 0.4f) {
                level.sendParticles(ParticleTypes.ENTITY_EFFECT, x, y, z, 0, vx * 1.2, 0.8, vz * 1.2, 1.0);
            }
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, vx * 1.5, 0.1, vz * 1.5, 0.05);
            }
        }
    }

    public static void playSound(Level level, Entity pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void playStaticChargedCloudTick(ServerLevel level, AreaEffectCloud cloud) {
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;
        int count = Math.max(2, (int) (radius * 8.0));
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY() + RANDOM.nextDouble() * 2.5;
            if (RANDOM.nextFloat() < 0.6f) {
                level.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                        x, y, z, 1, 0, 0, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        x, y, z, 1, 0, 0, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.2f) {
                level.sendParticles(new DustParticleOptions(STATIC_PURPLE_BLUE, 1.0f),
                        x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void playFrostedCloudTick(ServerLevel level, AreaEffectCloud cloud) {
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;
        int count = Math.max(2, (int) (radius * 8.0));
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY() + RANDOM.nextDouble() * 2.5;
            if (RANDOM.nextFloat() < 0.6f) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0, 0.02, 0, 0.02);
            }
            if (RANDOM.nextFloat() < 0.2f) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 1, 0, 0.01, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ModParticles.FROST_SNOWFLAKE.get(), x, y, z, 0, 0, -0.01, 0, 0.02);
            }
        }
    }

    public static void playSteamCloudTick(ServerLevel level, AreaEffectCloud cloud, boolean isHighHeat) {
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;
        int count = Math.max(1, (int) (radius * 10.0));
        double heightCeiling = ElementalFireNatureReactionsConfig.steamCloudHeightCeiling;
        double baseSpeed = heightCeiling / 70.0;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY();
            double upSpeed = baseSpeed * (0.8 + RANDOM.nextDouble() * 0.4);
            if (isHighHeat) {
                level.sendParticles(ModParticles.STEAM_SMOKE.get(), x, y, z, 0, 0, upSpeed, 0, 1.0);
            } else {
                double particleY = cloud.getY() + RANDOM.nextDouble() * heightCeiling;
                level.sendParticles(ModParticles.STEAM_CLOUD.get(), x, particleY, z, 0, 0, upSpeed, 0, 1.0);
            }
            if (isHighHeat) {
                if (RANDOM.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, upSpeed * 0.8, 0, 0.5);
                if (RANDOM.nextFloat() < 0.05f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
            }
        }
    }

    public static void playSteamBurst(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        playSteamBurst(level, target.getX(), target.getY(), target.getZ(), target.getBbHeight(), radius, intensity, isHighHeat);
    }

    public static void playSteamBurst(ServerLevel level, double x, double y, double z, float height, float radius, int intensity, boolean isHighHeat) {
        level.playSound(null, x, y, z,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
        int count = Math.max(1, (int) (Math.max(1.0, radius) * 5 * intensity));
        double speed = 0.05;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double px = x + Math.cos(angle) * dist;
            double pz = z + Math.sin(angle) * dist;
            double py = y + RANDOM.nextDouble() * height + 0.2;
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 0, 0, 0.05, 0, speed * 0.5);
        }
    }

    public static void playStaticBurst(ServerLevel level, LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        double z = entity.getZ();
        for (int i = 0; i < 8; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    public static void playStaticShockParticles(ServerLevel level, LivingEntity entity) {
        double radius = entity.getBbWidth() * 0.8 + 0.5;
        for (int i = 0; i < 2 + RANDOM.nextInt(2); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double heightOffset = (RANDOM.nextDouble() - 0.5) * entity.getBbHeight() * 1.2;
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            double y = entity.getY() + entity.getBbHeight() / 2 + heightOffset;
            if (RANDOM.nextFloat() < 0.125f) {
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.02);
            } else {
                level.sendParticles(new DustParticleOptions(STATIC_PURPLE_BLUE, 1.2f),
                        x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void playStaticSplashParticles(ServerLevel level, LivingEntity entity) {
        for (int i = 0; i < 5; i++) {
            double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * 0.6;
            double y = entity.getY() + entity.getBbHeight() * 0.5 + (RANDOM.nextDouble() - 0.5) * 0.5;
            double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * 0.6;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnFreezeColdCloud(ServerLevel level, LivingEntity target, double radius, int duration) {
        int particleCount = (int) (radius * 20);
        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(RANDOM.nextDouble()) * radius;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY() + RANDOM.nextDouble() * target.getBbHeight() + 0.2;
            if (RANDOM.nextFloat() < 0.6f) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + RANDOM.nextDouble() * 0.5, z, 1, 0, 0.02, 0, 0.02);
            }
            if (RANDOM.nextFloat() < 0.25f) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 1, 0, 0.01, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.03, 0, 0.5);
            }
        }
        int ringPoints = (int) (radius * 8);
        double step = (Math.PI * 2) / ringPoints;
        for (int i = 0; i < ringPoints; i++) {
            double angle = step * i;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            double y = target.getY() + target.getBbHeight() * 0.3;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 1, 0, 0.02, 0, 0.01);
        }
    }

    public static void playStaticSplashParticles(ServerLevel level, LivingEntity source, LivingEntity target) {
        Vec3 start = source.position().add(0, source.getBbHeight() * 0.5, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double dist = start.distanceTo(end);
        int linePoints = (int) (dist * 8);
        if (linePoints < 2) linePoints = 2;
        for (int j = 0; j <= linePoints; j++) {
            double t = (double) j / linePoints;
            double lx = Mth.lerp(t, start.x, end.x);
            double ly = Mth.lerp(t, start.y, end.y);
            double lz = Mth.lerp(t, start.z, end.z);
            if (j % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, lx, ly, lz, 1, 0, 0, 0, 0.02);
            } else {
                if (RANDOM.nextFloat() < 0.3f) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, lx, ly, lz, 0, 0, 0, 0, 0.05);
                }
            }
        }
    }
}
