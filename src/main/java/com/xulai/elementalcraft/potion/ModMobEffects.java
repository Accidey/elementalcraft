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
 * <p>
 * 中文说明：
 * 模组药水效果（状态效果）注册类。
 * 使用 Forge 的 DeferredRegister 机制将模组中所有的自定义状态效果注册到游戏中。
 * 包含：
 * 1. 潮湿 (Wetness): 基础环境状态。
 * 2. 易燃孢子 (Flammable Spores): 元素反应核心中介状态 (V1.5.0)。
 * <p>
 * English Description:
 * Mod Mob Effects (Status Effects) Registration Class.
 * Uses Forge's DeferredRegister mechanism to register all custom status effects in the mod into the game.
 * Includes:
 * 1. Wetness: Basic environmental status.
 * 2. Flammable Spores: Core intermediary status for elemental reactions (V1.5.0).
 */
public class ModMobEffects {

    /**
     * 药水效果的延迟注册器。
     * 用于管理模组内的所有 MobEffect 注册项。
     * <p>
     * Deferred Register for Mob Effects.
     * Manages all MobEffect registry entries within the mod.
     */
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ElementalCraft.MODID);

    /**
     * 注册“潮湿”效果。
     * 对应的实现类为 {@link WetnessMobEffect}。
     * <p>
     * Registers the "Wetness" effect.
     * The implementation class is {@link WetnessMobEffect}.
     */
    public static final RegistryObject<MobEffect> WETNESS =
            MOB_EFFECTS.register("wetness", WetnessMobEffect::new);

    /**
     * 注册“易燃孢子”效果 (V1.5.0)。
     * 对应的实现类为 {@link FlammableSporesEffect}。
     * <p>
     * Registers the "Flammable Spores" effect (V1.5.0).
     * The implementation class is {@link FlammableSporesEffect}.
     */
    public static final RegistryObject<MobEffect> SPORES =
            MOB_EFFECTS.register("flammable_spores", FlammableSporesEffect::new);

    /**
     * 将注册器关联到模组事件总线。
     * 必须在模组初始化阶段调用，以确保效果被正确注册。
     * <p>
     * Registers the registry to the mod event bus.
     * Must be called during the mod initialization phase to ensure effects are registered correctly.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}