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

        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            // Draw electric ring around the entity
            drawShockRing(serverLevel, entity, stacks);

            // Swirl electric particles around enemies inside the ring
            if (entity.tickCount % 8 == 0) {
                drawRingEnemyParticles(serverLevel, entity, stacks);
            }
        }
    }

    /**
     * 用静电粒子（THUNDER_SPARK_PERSISTENT + ELECTRIC_SPARK）画一个电网光环圈（跟随实体移动）
     */
    private static void drawShockRing(ServerLevel serverLevel, LivingEntity source, int stacks) {
        double range = getShockRange(stacks);
        double centerX = source.getX();
        double centerY = source.getY() + 0.1;
        double centerZ = source.getZ();

        // 根据范围动态调整粒子数量，保证圆的密度
        int points = Math.max(12, (int) (range * 8));
        double angleStep = Math.PI * 2 / points;

        // 让圆圈缓慢旋转
        double rotAngle = source.tickCount * 0.05;

        for (int i = 0; i < points; i++) {
            double angle = rotAngle + i * angleStep;
            double px = centerX + Math.cos(angle) * range;
            double pz = centerZ + Math.sin(angle) * range;

            // 交替使用两种粒子：自定义雷电火花 + 原版电火花
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

    /**
     * 光环范围内敌人身上的静电粒子环绕
     */
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

            // 圆形距离检测
            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range) continue;

            // 在目标周围生成电火花环绕
            double tCenterX = target.getX();
            double tCenterY = target.getY() + target.getBbHeight() * 0.5;
            double tCenterZ = target.getZ();
            double tRadius = 0.4 + target.getBbWidth() * 0.5;

            // 3个旋转粒子围绕目标
            for (int i = 0; i < 3; i++) {
                double angle = target.tickCount * 0.15 + i * (Math.PI * 2.0 / 3.0);
                double px = tCenterX + Math.cos(angle) * tRadius;
                double pz = tCenterZ + Math.sin(angle) * tRadius;
                double py = tCenterY + Math.sin(target.tickCount * 0.1 + i) * 0.2;

                if (i == 0) {
                    serverLevel.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                            px, py, pz,
                            1, 0, 0, 0, 0.005);
                } else {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            px, py, pz,
                            1, 0, 0, 0, 0.005);
                }
            }
        }
    }

    /**
     * 计算光环范围（与 StaticShockHandler.applySplashDamage 中的溅射范围计算保持一致）
     */
    private static double getShockRange(int stacks) {
        int baseRange = ElementalThunderFrostReactionsConfig.staticSplashBaseRange;
        int perStack = ElementalThunderFrostReactionsConfig.staticSplashRangePerStack;
        int maxRange = ElementalThunderFrostReactionsConfig.staticSplashMaxRange;
        int range = baseRange + (stacks - 1) * perStack;
        if (range > maxRange) range = maxRange;
        if (range < 1) range = 1;
        return range;
    }
}