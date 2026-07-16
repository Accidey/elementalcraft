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
                level.sendParticles(ModParticles.frostSnowflake(), x, y, z, 1, 0, 0, 0, 0);
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
        Vec3 pos = center.position();
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BONE_MEAL_USE, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEEHIVE_WORK, SoundSource.HOSTILE, 1.0F, 1.1F);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.AZALEA_LEAVES_FALL, SoundSource.HOSTILE, 1.0F, 1.0F);
        spawnNatureAnimation(level, pos, radius, 0);
        spawnTornadoAnimation(level, pos, radius, 0);
    }

    private static void spawnNatureAnimation(ServerLevel level, Vec3 pos, double radius, int tick) {
        if (tick > 18) return;
        if (tick <= 3) {
            spawnNatureCore(level, pos);
        } else if (tick <= 10) {
            double progress = (tick - 4) / 6.0;
            double r = 0.5 + (radius - 0.5) * progress;
            spawnNatureRing(level, pos, r);
        } else if (tick == 11) {
            spawnNaturePulse(level, pos, radius);
        } else if (tick <= 15) {
            double breathe = radius + Math.sin((tick - 11) * 1.2) * 0.25;
            spawnNatureRing(level, pos, breathe);
            spawnNatureRing(level, pos, breathe * 0.6);
        } else {
            spawnNatureFade(level, pos, radius);
        }
        final int nextTick = tick + 1;
        level.getServer().execute(() -> spawnNatureAnimation(level, pos, radius, nextTick));
    }

    private static void spawnNatureCore(ServerLevel level, Vec3 pos) {
        for (int angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            double x = pos.x + Math.cos(rad) * 0.5;
            double z = pos.z + Math.sin(rad) * 0.5;
            if (RANDOM.nextFloat() < 0.5f) {
                level.sendParticles(new DustParticleOptions(SMOG_COLOR, 2.0f), x, pos.y + 0.08, z, 1, 0, 0, 0, 0);
            }
        }
        if (RANDOM.nextFloat() < 0.4f) {
            level.sendParticles(ParticleTypes.ENTITY_EFFECT, pos.x, pos.y + 0.5, pos.z, 0, 0.2, 0.9, 0.2, 1.0);
        }
        if (RANDOM.nextFloat() < 0.3f) {
            double x = pos.x + (RANDOM.nextDouble() - 0.5) * 0.5;
            double z = pos.z + (RANDOM.nextDouble() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.COMPOSTER, x, pos.y + 0.1, z, 1, 0, 0.02, 0, 0);
        }
    }

    private static void spawnNatureRing(ServerLevel level, Vec3 pos, double r) {
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            double x = pos.x + Math.cos(rad) * r;
            double z = pos.z + Math.sin(rad) * r;
            if (RANDOM.nextFloat() < 0.65f) {
                level.sendParticles(new DustParticleOptions(SMOG_COLOR, 1.5f), x, pos.y + 0.08, z, 1, 0, 0, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.12f) {
                level.sendParticles(ParticleTypes.ENTITY_EFFECT, x, pos.y + 0.2, z, 0, 0.2, 0.9, 0.2, 1.0);
            }
            if (RANDOM.nextFloat() < 0.08f) {
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, pos.y + RANDOM.nextDouble() * 1.5, z, 1, 0, 0, 0, 0.01);
            }
        }
    }

    private static void spawnNaturePulse(ServerLevel level, Vec3 pos, double radius) {
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            double x = pos.x + Math.cos(rad) * radius;
            double z = pos.z + Math.sin(rad) * radius;
            double vx = Math.cos(rad) * 0.12;
            double vz = Math.sin(rad) * 0.12;
            level.sendParticles(new DustParticleOptions(SMOG_COLOR, 2.5f), x, pos.y + 0.08, z, 0, vx, 0.02, vz, 1.0);
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ParticleTypes.ENTITY_EFFECT, x, pos.y + 0.15, z, 0, 0.2, 0.9, 0.2, 1.0);
            }
            if (RANDOM.nextFloat() < 0.15f) {
                level.sendParticles(ParticleTypes.COMPOSTER, x, pos.y + 0.1, z, 1, 0, 0.02, 0, 0);
            }
        }
        int inner = (int) (radius * 2);
        for (int i = 0; i < inner; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double d = RANDOM.nextDouble() * radius;
            level.sendParticles(ParticleTypes.ENTITY_EFFECT, pos.x + Math.cos(a) * d, pos.y + 0.1, pos.z + Math.sin(a) * d, 0, 0.2, 0.9, 0.2, 1.0);
        }
    }

    private static void spawnNatureFade(ServerLevel level, Vec3 pos, double radius) {
        int spore = 5 + RANDOM.nextInt(4);
        for (int i = 0; i < spore; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double d = RANDOM.nextDouble() * radius;
            double x = pos.x + Math.cos(a) * d;
            double z = pos.z + Math.sin(a) * d;
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, pos.y + RANDOM.nextDouble() * 1.5, z, 1, 0, 0.03, 0, 0.01);
        }
        if (RANDOM.nextFloat() < 0.3f) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double d = RANDOM.nextDouble() * radius;
            level.sendParticles(ParticleTypes.COMPOSTER, pos.x + Math.cos(a) * d, pos.y + 0.1, pos.z + Math.sin(a) * d, 1, 0, 0.02, 0, 0);
        }
        if (RANDOM.nextFloat() < 0.2f) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double d = RANDOM.nextDouble() * radius;
            level.sendParticles(ParticleTypes.ENTITY_EFFECT, pos.x + Math.cos(a) * d, pos.y + 0.2, pos.z + Math.sin(a) * d, 0, 0.2, 0.9, 0.2, 1.0);
        }
    }

    private static void spawnTornadoAnimation(ServerLevel level, Vec3 pos, double radius, int tick) {
        int expandTicks = 20;
        int maxTick = expandTicks + 10;
        if (tick > maxTick) return;
        double progress = Math.min((double) tick / expandTicks, 1.0);
        double currentR = 0.5 + (radius - 0.5) * progress;
        int totalHelices = 6;
        int layers = 6;
        double baseAngle = tick * 0.3;
        double coneH = Math.min(currentR * 1.5, 10.0);
        for (int h = 0; h < totalHelices; h++) {
            double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelices;
            for (int p = 0; p < layers; p++) {
                double t = p / (double)(layers - 1);
                double y = 0.1 + t * coneH;
                double r = currentR * (0.3 + 1.78 * (t - 0.556) * (t - 0.556));
                double x = pos.x + Math.cos(helixAngle) * r;
                double z = pos.z + Math.sin(helixAngle) * r;
                double tangSpeed = 0.15 + RANDOM.nextDouble() * 0.1;
                double outwardSpeed = 0.03 + RANDOM.nextDouble() * 0.02;
                double vx = -Math.sin(helixAngle) * tangSpeed + Math.cos(helixAngle) * outwardSpeed;
                double vz = Math.cos(helixAngle) * tangSpeed + Math.sin(helixAngle) * outwardSpeed;
                double vy = tick <= expandTicks ? 0.03 + RANDOM.nextDouble() * 0.02 : 0;
                level.sendParticles(ModParticles.CHERRY_BLOSSOM.get(), x, pos.y + y, z, 0, vx, vy, vz, 1.0);
            }
        }
        final int nextTick = tick + 1;
        level.getServer().execute(() -> spawnTornadoAnimation(level, pos, radius, nextTick));
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
                level.sendParticles(ModParticles.frostSnowflake(), x, y, z, 0, 0, -0.01, 0, 0.02);
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
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, upSpeed, 0, 1.0);
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

    public static void playFrostBurstRing(ServerLevel level, double x, double y, double z, double radius) {
        int points = (int) Math.max(8, radius * 12);
        double step = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + 0.1;
            level.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0.02, 0, 0.01);
            if (RANDOM.nextFloat() < 0.3f) {
                level.sendParticles(ModParticles.FROST_ICE_RUNE.get(), px, py, pz, 1, 0, 0.01, 0, 0);
            }
            if (RANDOM.nextFloat() < 0.2f) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, px, py, pz, 1, 0, 0.01, 0, 0);
            }
        }
    }
}
