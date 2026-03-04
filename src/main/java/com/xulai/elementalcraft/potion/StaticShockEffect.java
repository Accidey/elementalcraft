// src/main/java/com/xulai/elementalcraft/potion/StaticShockEffect.java
package com.xulai.elementalcraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * StaticShockEffect
 * <p>
 * 中文说明：
 * 静电效果定义类。
 * 这是一个“标记型”效果，主要用于在游戏界面（GUI）上显示图标、名称和等级（如静电 I/II/III）。
 * 该效果本身不执行任何每 Tick 逻辑（applyEffectTick），实际的属性修正（伤害、层数变化）和
 * 机制处理（如周期性伤害、衰减）完全位于 {@link com.xulai.elementalcraft.event.StaticShockHandler} 中。
 * 此效果的持续时间和放大器由 {@link com.xulai.elementalcraft.event.StaticShockHandler} 动态同步，
 * 以准确反映实体的当前静电层数和剩余时间。
 * <p>
 * English Description:
 * Definition class for the Static Shock effect.
 * This is a "marker" effect, primarily used to display the icon, name, and level (e.g., Static Shock I/II/III) on the game GUI.
 * The effect itself does not execute any per-tick logic (applyEffectTick). The actual attribute modifiers (damage, stack changes)
 * and mechanics (periodic damage, decay) are handled entirely within {@link com.xulai.elementalcraft.event.StaticShockHandler}.
 * The duration and amplifier of this effect are dynamically synchronized by {@link com.xulai.elementalcraft.event.StaticShockHandler}
 * to accurately reflect the entity's current static shock stacks and remaining time.
 */
public class StaticShockEffect extends MobEffect {

    /**
     * 构造函数。
     * 定义效果类型为有害（HARMFUL），颜色为闪电黄（0xFFD700）。
     * <p>
     * Constructor.
     * Defines the effect type as HARMFUL and color as Lightning Yellow (0xFFD700).
     */
    public StaticShockEffect() {
        // Category: HARMFUL (有害)
        // Color: 0xFFD700 (金色 / Golden)
        super(MobEffectCategory.HARMFUL, 0xFFD700);
    }

    /**
     * 决定是否在每个 Tick 执行 applyEffect 逻辑。
     * 这里返回 false，因为我们的核心逻辑运行在 StaticShockHandler 的 LivingTickEvent 事件监听器中。
     * 此效果实例仅用于 UI 显示和持续时间倒计时。
     * <p>
     * Determines whether to run the applyEffect logic every tick.
     * Returns false here because our core logic runs in the StaticShockHandler's LivingTickEvent listener.
     * This effect instance is solely for UI display and duration countdown.
     *
     * @param duration  剩余时长 / Remaining duration
     * @param amplifier 效果等级 / Effect amplifier
     * @return false，表示不需要每 Tick 执行 / false, indicating no per-tick execution needed
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}