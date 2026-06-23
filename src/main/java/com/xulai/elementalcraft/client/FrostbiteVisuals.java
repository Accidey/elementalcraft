package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FrostbiteVisuals {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalVisualConfig.frostbiteAuraEnabled) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(FrostbiteHandler.NBT_FROSTBITE_STACKS)) return;

        int stacks = data.getInt(FrostbiteHandler.NBT_FROSTBITE_STACKS);
        if (stacks <= 0) return;

        boolean isFrozen = FrostbiteHandler.isFrozen(entity);
        boolean hasAura = ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && stacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold;

        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            if (hasAura) {
                double centerX = entity.getX();
                double centerY = entity.getY() + entity.getBbHeight() * 0.5;
                double centerZ = entity.getZ();

                int particleCount = stacks * 2;
                double spread = 0.3 + stacks * 0.1;

                if (isFrozen) {
                    for (int i = 0; i < particleCount * 2; i++) {
                        double angle = RANDOM.nextDouble() * Math.PI * 2;
                        double radius = 0.5 + RANDOM.nextDouble() * 0.3;
                        double px = centerX + Math.cos(angle) * radius;
                        double pz = centerZ + Math.sin(angle) * radius;
                        double py = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0, 0, 0.02);
                    }

                    if (entity.tickCount % 8 == 0) {
                        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                                centerX, centerY, centerZ,
                                10, 0.4, 0.3, 0.4, 0.05);
                        serverLevel.sendParticles(ModParticles.frostSnowflake(),
                                centerX, centerY, centerZ,
                                3, 0.3, 0.2, 0.3, 0);
                    }
                } else {
                    for (int i = 0; i < particleCount; i++) {
                        double angle = (entity.tickCount * 0.1 + i * (Math.PI * 2 / particleCount)) % (Math.PI * 2);
                        double radius = 0.3 + RANDOM.nextDouble() * 0.2;
                        double px = centerX + Math.cos(angle) * radius;
                        double pz = centerZ + Math.sin(angle) * radius;
                        double py = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0, 0, 0.01);
                    }

                    if (stacks >= 3 && RANDOM.nextFloat() < 0.3f) {
                        double px = centerX + (RANDOM.nextDouble() - 0.5) * spread;
                        double pz = centerZ + (RANDOM.nextDouble() - 0.5) * spread;
                        double py = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
                        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                                px, py, pz, 1, 0, -0.02, 0, 0);
                    }
                }

                drawAuraCircle(serverLevel, entity, stacks);

                if (entity.tickCount % 8 == 0) {
                    drawAuraEnemyParticles(serverLevel, entity, stacks);
                }
            }
        }
    }

    private static void drawAuraCircle(ServerLevel serverLevel, LivingEntity source, int stacks) {
        double range = FrostbiteHandler.getAuraRange(stacks);
        double centerX = source.getX();
        double centerY = source.getY() + 0.1;
        double centerZ = source.getZ();

        int points = Math.min(80, Math.max(16, (int) (range * 12)));
        double angleStep = Math.PI * 2 / points;

        double rotAngle = source.tickCount * 0.02;

        double prevPx = centerX + Math.cos(rotAngle) * range;
        double prevPz = centerZ + Math.sin(rotAngle) * range;

        for (int i = 0; i < points; i++) {
            double angle = rotAngle + (i + 1) * angleStep;
            double px = centerX + Math.cos(angle) * range;
            double pz = centerZ + Math.sin(angle) * range;

            double dx = px - prevPx;
            double dz = pz - prevPz;
            int segPoints = Math.max(2, (int) (Math.sqrt(dx * dx + dz * dz) * 2));
            for (int s = 0; s < segPoints; s++) {
                double t = (double) s / segPoints;
                double ix = prevPx + dx * t;
                double iz = prevPz + dz * t;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        ix, centerY, iz,
                        1, 0.01, 0.03, 0.01, 0.003);
            }

            prevPx = px;
            prevPz = pz;
        }
    }

    private static void drawAuraEnemyParticles(ServerLevel serverLevel, LivingEntity source, int stacks) {
        double range = FrostbiteHandler.getAuraRange(stacks);

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

            for (int i = 0; i < 3; i++) {
                double angle = target.tickCount * 0.15 + i * (Math.PI * 2.0 / 3.0);
                double px = tCenterX + Math.cos(angle) * tRadius;
                double pz = tCenterZ + Math.sin(angle) * tRadius;
                double py = tCenterY + Math.sin(target.tickCount * 0.1 + i) * 0.2;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        px, py, pz,
                        1, 0, 0, 0, 0.005);
            }
        }
    }
}
