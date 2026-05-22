package com.xulai.elementalcraft.potion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class ParalysisEffect extends MobEffect {
    private static final String NBT_AI_DISABLED = "EC_AIDisabled";
    private static final String NBT_SHARED_ORIGINAL_NO_AI = "EC_SharedOriginalNoAI";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.level().isClientSide) {
            disableAI(pLivingEntity);

            if (pLivingEntity.isInWater()) {
                pLivingEntity.move(MoverType.SELF, new Vec3(0, -0.05, 0));
                pLivingEntity.setDeltaMovement(
                    pLivingEntity.getDeltaMovement().x, 0, pLivingEntity.getDeltaMovement().z);

                CompoundTag data = pLivingEntity.getPersistentData();
                int drownTimer = data.getInt("EC_DrownTimer") + 1;
                if (drownTimer >= 20) {
                    drownTimer = 0;
                    pLivingEntity.hurt(pLivingEntity.damageSources().drown(), 2.0F);
                }
                data.putInt("EC_DrownTimer", drownTimer);
            } else {
                pLivingEntity.getPersistentData().remove("EC_DrownTimer");
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        restoreAI(entity);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    private static void disableAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(NBT_AI_DISABLED)) return;
        if (!data.contains(NBT_SHARED_ORIGINAL_NO_AI)) {
            data.putBoolean(NBT_SHARED_ORIGINAL_NO_AI, mob.isNoAi());
        }
        mob.setNoAi(true);
        data.putBoolean(NBT_AI_DISABLED, true);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(NBT_AI_DISABLED)) return;
        data.remove(NBT_AI_DISABLED);
        if (entity.hasEffect(ModMobEffects.FREEZE.get())) return;
        boolean wasNoAi = data.getBoolean(NBT_SHARED_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(NBT_SHARED_ORIGINAL_NO_AI);
    }

}
