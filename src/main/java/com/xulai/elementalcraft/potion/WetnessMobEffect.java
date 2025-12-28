// src/main/java/com/xulai/elementalcraft/potion/WetnessMobEffect.java
package com.xulai.elementalcraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * WetnessMobEffect
 * <p>
 * 中文说明：
 * 潮湿效果定义类。
 * 这是一个“标记型”效果，主要用于在游戏界面（GUI）上显示图标、名称和等级（如潮湿 I/II/III）。
 * 该效果本身不执行任何每 Tick 逻辑（applyEffectTick），实际的属性修正（抗性变化、火伤减免）和
 * 机制处理（如饱食度消耗）完全位于 {@link com.xulai.elementalcraft.event.WetnessHandler} 中。
 * <p>
 * English Description:
 * Definition class for the Wetness effect.
 * This is a "marker" effect, primarily used to display the icon, name, and level (e.g., Wetness I/II/III) on the game GUI.
 * The effect itself does not execute any per-tick logic (applyEffectTick). The actual attribute modifiers (resistance changes, fire reduction)
 * and mechanics (e.g., hunger consumption) are handled entirely within {@link com.xulai.elementalcraft.event.WetnessHandler}.
 */
public class WetnessMobEffect extends MobEffect {

    /**
     * 构造函数。
     * 定义效果类型为中性，颜色为深蓝色。
     * <p>
     * Constructor.
     * Defines the effect type as Neutral and color as Deep Blue.
     */
    public WetnessMobEffect() {
        // Category: NEUTRAL (中性，通常用于非纯正面也非纯负面的状态)
        // Color: 0x3366FF (类似水的深蓝色 / Deep Blue like water)
        super(MobEffectCategory.NEUTRAL, 0x3366FF);
    }

    /**
     * 决定是否在每个 Tick 执行 applyEffect 逻辑。
     * 这里返回 false，因为我们的核心逻辑运行在 WetnessHandler 的 LivingTickEvent 事件监听器中。
     * 此效果实例仅用于 UI 显示和持续时间倒计时。
     * <p>
     * Determines whether to run the applyEffect logic every tick.
     * Returns false here because our core logic runs in the WetnessHandler's LivingTickEvent listener.
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