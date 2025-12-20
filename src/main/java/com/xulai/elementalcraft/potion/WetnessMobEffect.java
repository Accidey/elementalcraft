// src/main/java/com/xulai/elementalcraft/potion/WetnessMobEffect.java
package com.xulai.elementalcraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * WetnessMobEffect
 *
 * 中文说明：
 * 潮湿效果定义类。
 * 这是一个“标记型”效果，主要用于在游戏界面（GUI）上显示图标、名称和等级（如潮湿 I/II/III）。
 * 实际的属性修正（抗性变化、火伤减免）和逻辑处理（饱食度消耗）位于 WetnessHandler 中，不依赖此处的 applyEffectTick。
 *
 * English description:
 * Definition class for the Wetness effect.
 * This is a "marker" effect, primarily used to display the icon, name, and level (e.g., Wetness I/II/III) on the game GUI.
 * The actual attribute modifiers (resistance changes, fire reduction) and logic (hunger consumption) are handled in WetnessHandler, independent of applyEffectTick here.
 */
public class WetnessMobEffect extends MobEffect {
    
    /**
     * 构造函数。
     * 定义效果类型为中性，颜色为深蓝色。
     *
     * Constructor.
     * Defines the effect type as Neutral and color as Dark Blue.
     */
    public WetnessMobEffect() {
        // Category: NEUTRAL (中性，通常显示为蓝色)
        // Color: 0x3366FF (类似水的深蓝色 / Deep Blue like water)
        super(MobEffectCategory.NEUTRAL, 0x3366FF);
    }

    /**
     * 决定是否在每个 Tick 执行 applyEffect 逻辑。
     * 这里返回 false，因为我们的核心逻辑运行在 WetnessHandler 的 LivingTickEvent 中。
     * 此效果仅用于 UI 显示和倒计时。
     *
     * Determines whether to run applyEffect logic every tick.
     * Returns false here because our core logic runs in WetnessHandler's LivingTickEvent.
     * This effect is solely for UI display and countdowns.
     *
     * @param duration 剩余时长 / Remaining duration
     * @param amplifier 效果等级 / Effect amplifier
     * @return false
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}