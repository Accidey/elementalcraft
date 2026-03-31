package com.xulai.elementalcraft.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class StaticShockEffect extends MobEffect {

    public StaticShockEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFD700);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}