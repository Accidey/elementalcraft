// src/main/java/com/xulai/elementalcraft/sound/ModSounds.java
package com.xulai.elementalcraft.sound;

import com.xulai.elementalcraft.ElementalCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * ModSounds
 * <p>
 * 中文说明：
 * 模组自定义音效注册类。
 * 使用 Forge 的延迟注册机制将所有自定义音效注册到游戏中。
 * 当前包含：
 * - electric_zap：静电伤害触发时的电击音效（"滋滋"电流声）。
 * <p>
 * English Description:
 * Mod Custom Sound Events Registration Class.
 * Uses Forge's deferred register to register all custom sound events into the game.
 * Currently includes:
 * - electric_zap: Electric zap sound played when Static Shock damage is triggered ("buzzing" current sound).
 */
public final class ModSounds {

    /**
     * 音效的延迟注册器。
     * 用于管理模组内所有 {@link SoundEvent} 的注册项。
     * <p>
     * Deferred register for sound events.
     * Manages all {@link SoundEvent} registry entries within the mod.
     */
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ElementalCraft.MODID);

    /**
     * 静电电击音效。
     * 当静电效果触发伤害时播放，模拟电流声。
     * 对应的音效文件位于：assets/elementalcraft/sounds/electric_zap.ogg
     * 需要在 assets/elementalcraft/sounds.json 中定义：
     * {
     *   "electric_zap": {
     *     "sounds": ["elementalcraft:electric_zap"]
     *   }
     * }
     * <p>
     * Electric zap sound effect.
     * Played when Static Shock damage is triggered, simulating electric current sound.
     * Corresponding sound file located at: assets/elementalcraft/sounds/electric_zap.ogg
     * Must be defined in assets/elementalcraft/sounds.json:
     * {
     *   "electric_zap": {
     *     "sounds": ["elementalcraft:electric_zap"]
     *   }
     * }
     */
    public static final RegistryObject<SoundEvent> ELECTRIC_ZAP =
            SOUND_EVENTS.register("electric_zap",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ElementalCraft.MODID, "electric_zap")));

    /**
     * 将注册器关联到模组事件总线。
     * 必须在模组初始化阶段调用，以确保所有音效被正确注册。
     * <p>
     * Registers the deferred register to the mod event bus.
     * Must be called during the mod initialization phase to ensure all sound events are registered correctly.
     *
     * @param eventBus 模组事件总线 / Mod event bus
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

    // Private constructor to prevent instantiation
    private ModSounds() {}
}