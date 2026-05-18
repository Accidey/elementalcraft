package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockVisuals {

    private static final Random RANDOM = new Random();
    private static final String NBT_STATIC_STACKS = "ec_static_stacks";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!ElementalVisualConfig.staticShockAuraEnabled) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_STATIC_STACKS)) return;

        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) return;

        if (stacks < ElementalThunderFrostReactionsConfig.staticAuraThreshold) return;

        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            drawShockRing(serverLevel, entity, stacks);

            if (entity.tickCount % 8 == 0) {
                drawRingEnemyParticles(serverLevel, entity, stacks);
            }
        }

        if (entity.tickCount % 6 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            drawBodyParticles(serverLevel, entity);
        }
    }

    private static void drawShockRing(ServerLevel serverLevel, LivingEntity source, int stacks) {
        double range = getShockRange(stacks);
        double centerX = source.getX();
        double centerY = source.getY() + 0.1;
        double centerZ = source.getZ();

        int points = Math.max(6, (int) (range * 4));
        double angleStep = Math.PI * 2 / points;

        double rotAngle = source.tickCount * 0.05;

        for (int i = 0; i < points; i++) {
            double angle = rotAngle + i * angleStep;
            double px = centerX + Math.cos(angle) * range;
            double pz = centerZ + Math.sin(angle) * range;

            if (i % 2 == 0) {
                serverLevel.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                        px, centerY, pz,
                        1, 0.02, 0.05, 0.02, 0.005);
            } else {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        px, centerY, pz,
                        1, 0.02, 0.05, 0.02, 0.005);
            }
        }
    }

    private static void drawRingEnemyParticles(ServerLevel serverLevel, LivingEntity source, int stacks) {
        double range = getShockRange(stacks);

        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        java.util.List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (target.isDeadOrDying()) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range) continue;

            double tCenterX = target.getX();
            double tCenterY = target.getY() + target.getBbHeight() * 0.5;
            double tCenterZ = target.getZ();
            double tRadius = 0.4 + target.getBbWidth() * 0.5;

            double angle = target.tickCount * 0.15;
            double px = tCenterX + Math.cos(angle) * tRadius;
            double pz = tCenterZ + Math.sin(angle) * tRadius;
            double py = tCenterY + Math.sin(target.tickCount * 0.1) * 0.2;

            serverLevel.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                    px, py, pz, 1, 0, 0, 0, 0.005);

            double angle2 = target.tickCount * 0.15 + Math.PI;
            double px2 = tCenterX + Math.cos(angle2) * tRadius;
            double pz2 = tCenterZ + Math.sin(angle2) * tRadius;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    px2, py, pz2, 1, 0, 0, 0, 0.005);
        }
    }

    private static void drawBodyParticles(ServerLevel serverLevel, LivingEntity entity) {
        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.5;
        double cz = entity.getZ();
        double radius = 0.3 + entity.getBbWidth() * 0.3;
        int count = 1 + entity.level().random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = entity.tickCount * 0.2 + i * (Math.PI * 2.0 / count);
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + Math.sin(entity.tickCount * 0.15 + i * 1.5) * 0.25;
            serverLevel.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                    px, py, pz, 1, 0, 0, 0, 0.005);
        }
    }

    private static double getShockRange(int stacks) {
        return stacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange;
    }
}