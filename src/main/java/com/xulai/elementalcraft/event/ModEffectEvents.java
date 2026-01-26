package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.potion.ModMobEffects; // 导入效果注册类
import com.xulai.elementalcraft.util.EffectHelper;   // 导入粒子工具类
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ModEffectEvents
 * <p>
 * 全局事件监听器，用于处理与状态效果相关的持续性逻辑。
 * 目前主要用于触发“易燃孢子”的视觉粒子效果。
 */
// 确保 modid 与你的主类一致 (elementalcraft)
@Mod.EventBusSubscriber(modid = "elementalcraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEffectEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // 性能优化：
        // 1. 仅在服务端运行 (EffectHelper 内部也会检查 ServerLevel，但这里提前拦截更省性能)
        // 2. 频率限制：每 2 tick 检查一次，不需要每刻都跑，减少计算量
        if (entity.level().isClientSide || entity.tickCount % 2 != 0) {
            return;
        }

        // 检查生物是否拥有“易燃孢子”效果
        // 对应 ModMobEffects.java 中的 SPORES 注册项
        if (entity.hasEffect(ModMobEffects.SPORES.get())) {
            
            // 如果有效果，则播放环境悬浮粒子
            EffectHelper.playSporeAmbient(entity);
            
        }
    }
}