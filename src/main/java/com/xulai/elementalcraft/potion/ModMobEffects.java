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
 * 模组状态效果（药水效果）注册类。
 * 使用 Forge 的延迟注册机制将所有自定义状态效果注册到游戏中。
 * 当前包含：
 * - 潮湿 (Wetness)：基础环境状态，影响火焰伤害减免。
 * - 易燃孢子 (Flammable Spores)：元素反应核心中介状态，提供物理减伤与火焰易伤。
 * - 静电 (Static Shock)：雷霆属性特有的持续伤害状态，周期性造成随机伤害。
 * <p>
 * English Description:
 * Mod Status Effect (Potion Effect) Registration Class.
 * Uses Forge's deferred register to register all custom status effects into the game.
 * Currently includes:
 * - Wetness: Basic environmental status, affects fire damage reduction.
 * - Flammable Spores: Core intermediary status for elemental reactions, provides physical resistance and fire vulnerability.
 * - Static Shock: Thunder-attribute specific damage-over-time status, deals periodic random damage.
 */
public final class ModMobEffects {

    /**
     * 状态效果的延迟注册器。
     * 用于管理模组内所有 {@link MobEffect} 的注册项。
     * <p>
     * Deferred register for status effects.
     * Manages all {@link MobEffect} registry entries within the mod.
     */
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ElementalCraft.MODID);

    /**
     * 潮湿效果。
     * 对应的实现类为 {@link WetnessMobEffect}。
     * 该效果本身不执行逻辑，仅用于 GUI 显示，实际机制由 {@link com.xulai.elementalcraft.event.WetnessHandler} 处理。
     * <p>
     * Wetness effect.
     * The implementation class is {@link WetnessMobEffect}.
     * This effect itself does not execute logic, it is only for GUI display; actual mechanics are handled by {@link com.xulai.elementalcraft.event.WetnessHandler}.
     */
    public static final RegistryObject<MobEffect> WETNESS =
            MOB_EFFECTS.register("wetness", WetnessMobEffect::new);

    /**
     * 易燃孢子效果。
     * 对应的实现类为 {@link FlammableSporesEffect}。
     * 该效果提供物理减伤、火焰易伤和周期性伤害，是元素反应系统的核心中介状态。
     * <p>
     * Flammable Spores effect.
     * The implementation class is {@link FlammableSporesEffect}.
     * This effect provides physical resistance, fire vulnerability, and periodic damage, serving as the core intermediary status for the elemental reaction system.
     */
    public static final RegistryObject<MobEffect> SPORES =
            MOB_EFFECTS.register("flammable_spores", FlammableSporesEffect::new);

    /**
     * 静电效果。
     * 对应的实现类为 {@link StaticShockEffect}。
     * 该效果本身不执行逻辑，仅用于 GUI 显示层数和剩余时间，实际机制（触发、叠加、伤害、衰减）由 {@link com.xulai.elementalcraft.event.StaticShockHandler} 处理。
     * <p>
     * Static Shock effect.
     * The implementation class is {@link StaticShockEffect}.
     * This effect itself does not execute logic, it is only for GUI display of stacks and remaining time; actual mechanics (trigger, stacking, damage, decay) are handled by {@link com.xulai.elementalcraft.event.StaticShockHandler}.
     */
    public static final RegistryObject<MobEffect> STATIC_SHOCK =
            MOB_EFFECTS.register("static_shock", StaticShockEffect::new);

    /**
     * 将注册器关联到模组事件总线。
     * 必须在模组初始化阶段调用，以确保所有效果被正确注册。
     * <p>
     * Registers the deferred register to the mod event bus.
     * Must be called during the mod initialization phase to ensure all effects are registered correctly.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    // Private constructor to prevent instantiation
    private ModMobEffects() {}
}