package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
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
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-403C-8064-03E786A055C9");

    public FrostbiteEffect() {
        super(MobEffectCategory.HARMFUL, 0x66CCFF);
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                SPEED_MODIFIER_UUID.toString(),
                0.0,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ATTACK_SPEED_MODIFIER_UUID.toString(),
                0.0,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        double reduction = ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack;
        if (reduction <= 0) return 0.0;
        return Math.max(-reduction * (amplifier + 1), -0.9);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteDamageIntervalTicks;
        if (entity.tickCount % damageInterval == 0) {
            float baseDamage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
            float damage = baseDamage;
            ElementType element = ElementUtils.getConsistentAttackElement(entity);
            float elementMult = 1.0f;
            if (element == ElementType.FIRE) {
                elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
            } else if (element == ElementType.NATURE) {
                elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
            } else if (element == ElementType.THUNDER) {
                elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageThunderMultiplier;
            } else if (element == ElementType.FROST) {
                elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
            }
            damage *= elementMult;

            CompoundTag data = entity.getPersistentData();
            float lastDmg = data.getFloat(FrostbiteHandler.NBT_FROSTBITE_LAST_PERIODIC_DMG);
            if (data.getInt(FrostbiteHandler.NBT_FROSTBITE_PERIODIC_LOGGED) == 0 || damage != lastDmg) {
                DebugCommand.sendFrostbitePeriodicDamageLog(entity, baseDamage, element, elementMult, damage);
                data.putFloat(FrostbiteHandler.NBT_FROSTBITE_LAST_PERIODIC_DMG, damage);
                data.putInt(FrostbiteHandler.NBT_FROSTBITE_PERIODIC_LOGGED, 1);
            }

            entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.FROSTBITE_THERMAL_SHOCK), damage);

            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.3f, 1.8f);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        entity.setTicksFrozen(0);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}