package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "elementalcraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEffectEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide || entity.tickCount % 2 != 0) {
            return;
        }

        if (entity.hasEffect(ModMobEffects.SPORES.get())) {
            EffectHelper.playSporeAmbient(entity);
        }
    }
    
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        
        if (entity.level().isClientSide) return;
        
        // 检查是否是易燃孢子效果
        if (event.getEffectInstance().getEffect() == ModMobEffects.SPORES.get()) {
            // 检查目标是否有灼烧状态
            if (entity.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                // 等待一帧，确保孢子效果已经完全应用到实体上
                // 使用延迟执行来确保效果已经存在
                entity.level().getServer().execute(() -> {
                    // 再次检查孢子效果是否存在
                    if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                        // 触发毒火爆燃
                        int stacks = event.getEffectInstance().getAmplifier() + 1;
                        
                        // 由于指令给予效果没有施加者，使用目标自己作为攻击者
                        ReactionHandler.triggerToxicBlastFromScorched(entity, stacks);
                    }
                });
            }
        }
    }
}
