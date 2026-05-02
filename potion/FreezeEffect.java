package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.event.FrostbiteHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/**
 * 冻结效果 - 与麻痹相同的机制：
 * - 通过 setNoAi 禁用 Mob AI（自动禁止移动和攻击）
 * - 每 tick 设置速度为0（防止物理推力）
 * - 效果结束时恢复 AI
 */
public class FreezeEffect extends MobEffect {

    public FreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0x00BFFF); // 冰蓝色
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            // 禁用 Mob AI（与麻痹相同的方式）
            disableAI(entity);
            // 每 tick 强制停止移动
            entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        // 恢复 AI
        restoreAI(entity);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // 每 tick 都执行
    }

    private static void disableAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        data.putBoolean(FrostbiteHandler.NBT_FREEZE_ORIGINAL_NO_AI, mob.isNoAi());
        mob.setNoAi(true);
        data.putBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED, true);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        boolean wasNoAi = data.getBoolean(FrostbiteHandler.NBT_FREEZE_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(FrostbiteHandler.NBT_FREEZE_ORIGINAL_NO_AI);
        data.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
    }
}