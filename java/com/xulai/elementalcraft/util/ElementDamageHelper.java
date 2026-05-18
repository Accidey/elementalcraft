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

        float absorption = target.getAbsorptionAmount();
        if (absorption > 0) {
            float absorbed = Math.min(absorption, damage);
            target.setAbsorptionAmount(absorption - absorbed);
            damage -= absorbed;
        }

        if (damage <= 0) return;

        float newHealth = target.getHealth() - damage;
        target.setHealth(Math.max(newHealth, 0));

        if (newHealth <= 0 && !target.isDeadOrDying()) {
            target.die(source);
        }
    }
}
