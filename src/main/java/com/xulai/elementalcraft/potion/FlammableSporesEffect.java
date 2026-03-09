package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FlammableSporesEffect extends MobEffect {

    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public FlammableSporesEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E8B57);

        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID.toString(),
                -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);

        this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID.toString(),
                -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.level().isClientSide) {
            int damageInterval = ElementalFireNatureReactionsConfig.sporeDamageInterval;
            if (damageInterval <= 0) {
                damageInterval = 100;
            }
            
            if (pLivingEntity.tickCount % damageInterval == 0) {
                double damagePerStack = ElementalFireNatureReactionsConfig.sporePoisonDamage;

                if (damagePerStack > 0) {
                    float totalDamage = (float) damagePerStack;

                    pLivingEntity.hurt(pLivingEntity.damageSources().wither(), totalDamage);
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    @Override
    public double getAttributeModifierValue(int pAmplifier, AttributeModifier pModifier) {
        if (pModifier.getId().equals(SPEED_MODIFIER_UUID) || pModifier.getId().equals(ATTACK_SPEED_MODIFIER_UUID)) {
            double reductionPerStack = ElementalFireNatureReactionsConfig.sporeSpeedReduction;

            double totalReduction = -reductionPerStack * (pAmplifier + 1);

            return Math.max(totalReduction, -0.95);
        }

        return super.getAttributeModifierValue(pAmplifier, pModifier);
    }
}