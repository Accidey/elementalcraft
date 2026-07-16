package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.iss.ISSCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedVisuals {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalVisualConfig.scorchedAuraEnabled) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(ScorchedHandler.NBT_SCORCHED_TICKS)) return;
        if (data.getInt(ScorchedHandler.NBT_SCORCHED_TICKS) <= 0) return;

        if (!data.contains(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER)) return;
        int sourceFirePower = data.getInt(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
        int threshold = ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold;
        if (sourceFirePower < threshold) return;

        if (ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold <= 0) return;

        if (entity.tickCount % 4 == 0 && entity.level() instanceof ServerLevel serverLevel) {
            drawAuraCircle(serverLevel, entity);
        }
    }

    private static void drawAuraCircle(ServerLevel serverLevel, LivingEntity source) {
        double radius = ElementalFireNatureReactionsConfig.scorchedAuraRadius;
        double centerX = source.getX();
        double centerY = source.getY() + 0.1;
        double centerZ = source.getZ();

        int points = Math.max(8, (int) (radius * 12));
        double step = Math.PI * 2 / points;
        double rotAngle = source.tickCount * 0.02;

        for (int i = 0; i < points; i++) {
            double angle = rotAngle + i * step;
            double px = centerX + Math.cos(angle) * radius;
            double pz = centerZ + Math.sin(angle) * radius;

            serverLevel.sendParticles(ISSCore.getFireParticle(),
                    px, centerY, pz,
                    1, 0.05, 0.05, 0.05, 0.01);
        }
    }
}
