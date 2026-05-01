package com.xulai.elementalcraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class WetnessMobEffect extends MobEffect {

    public WetnessMobEffect() {
        super(MobEffectCategory.NEUTRAL, 0x3366FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}