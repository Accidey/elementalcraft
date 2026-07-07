package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.phys.Vec3;

public class FreezeEffect extends MobEffect {

    private static final String NBT_SHARED_ORIGINAL_NO_AI = "EC_SharedOriginalNoAI";

    public FreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0x00BFFF);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            entity.setTicksFrozen(300);
            disableAI(entity);

            int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteDamageIntervalTicks;
            if (entity.tickCount % damageInterval == 0) {
                float damage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
                ElementType element = ElementUtils.getConsistentAttackElement(entity);
                if (element == ElementType.FIRE) {
                    damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
                } else if (element == ElementType.NATURE) {
                    damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
                } else if (element == ElementType.THUNDER) {
                    damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageThunderMultiplier;
                } else if (element == ElementType.FROST) {
                    damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
                }
                entity.hurt(entity.damageSources().freeze(), damage);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            if (entity.isInWater()) {
                entity.move(MoverType.SELF, new Vec3(0, -0.1, 0));
                entity.setDeltaMovement(0, 0, 0);

                CompoundTag data = entity.getPersistentData();
                int drownTimer = data.getInt("EC_DrownTimer") + 1;
                if (drownTimer >= 20) {
                    drownTimer = 0;
                    entity.hurt(entity.damageSources().drown(), 2.0F);
                }
                data.putInt("EC_DrownTimer", drownTimer);
            } else {
                entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
                entity.getPersistentData().remove("EC_DrownTimer");
            }

            if (entity.tickCount % 5 == 0) {
                EffectHelper.playFreezeAmbient(entity);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        entity.setTicksFrozen(0);
        restoreAI(entity);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    private static void disableAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        if (!data.contains(NBT_SHARED_ORIGINAL_NO_AI)) {
            data.putBoolean(NBT_SHARED_ORIGINAL_NO_AI, mob.isNoAi());
        }
        mob.setNoAi(true);
        data.putBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED, true);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        data.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
        if (entity.hasEffect(ModMobEffects.PARALYSIS.get())) return;
        boolean wasNoAi = data.getBoolean(NBT_SHARED_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(NBT_SHARED_ORIGINAL_NO_AI);
    }
}
