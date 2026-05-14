package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FrostbiteEffect extends MobEffect {

    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-403C-8064-03E786A055C8");

    public FrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x66CCFF);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_UUID.toString(),
                0.0,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return -ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack * (amplifier + 1);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteDamageIntervalTicks;
        if (entity.tickCount % damageInterval == 0) {
            float damage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
            ElementType element = ElementUtils.getConsistentAttackElement(entity);
            if (element == ElementType.FIRE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
            } else if (element == ElementType.NATURE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
            } else if (element == ElementType.FROST) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
            }
            entity.hurt(entity.damageSources().freeze(), damage);

            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.3f, 1.8f);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; 
    }
}