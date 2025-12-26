// src/main/java/com/xulai/elementalcraft/potion/ModMobEffects.java
package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModMobEffects
 *
 * 中文说明：
 * 模组药水效果注册类。
 * 使用 DeferredRegister 注册所有的自定义状态效果（如潮湿）。
 *
 * English description:
 * Mod Mob Effects Registration Class.
 * Uses DeferredRegister to register all custom status effects (such as Wetness).
 */
public class ModMobEffects {
    // 药水效果的延迟注册器
    // Deferred Register for Mob Effects
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ElementalCraft.MODID);

    // 注册“潮湿”效果
    // Registers the "Wetness" effect
    public static final RegistryObject<MobEffect> WETNESS = 
        MOB_EFFECTS.register("wetness", WetnessMobEffect::new);

    /**
     * 将注册器注册到事件总线。
     * Registers the registry to the event bus.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}