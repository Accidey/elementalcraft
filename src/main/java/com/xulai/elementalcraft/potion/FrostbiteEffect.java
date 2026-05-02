package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 霜冻效果：
 * - 每 100 tick（5秒）造成一次细雪冰冻伤害
 * - 每层减少 10% 移动速度（通过 AttributeModifier）
 * - 与潮湿效果接触时触发冻结
 */
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
        // 每层减速 = frostbiteSpeedReductionPerStack（默认 -0.1 = -10%）
        // amplifier = stacks - 1，所以实际减速 = -speedReduction * (amplifier + 1)
        return -ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack * (amplifier + 1);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        // 每 100 tick（5秒）造成一次细雪冰冻伤害
        int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteDamageIntervalTicks;
        if (entity.tickCount % damageInterval == 0) {
            float damage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
            entity.hurt(entity.damageSources().freeze(), damage);

            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.3f, 1.8f);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // 每 tick 都执行（内部自行控制伤害间隔）
    }
}