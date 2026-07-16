package com.xulai.elementalcraft.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ElementDamageHelper {

    public static void applyDamage(LivingEntity target, float damage, DamageSource source) {
        if (target.level().isClientSide) return;
        if (damage <= 0) return;
        if (!target.isAlive()) return;
        if (target instanceof Player player && player.isCreative()) return;
        target.hurt(source, damage);
    }
}
